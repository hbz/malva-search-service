package org.xbib.webapp.extensions.oai

import com.ctc.wstx.stax.WstxInputFactory
import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import org.apache.xalan.processor.TransformerFactoryImpl
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollAction
import org.elasticsearch.action.search.SearchScrollRequestBuilder
import org.elasticsearch.client.ElasticsearchClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.xbib.common.settings.Settings
import org.xbib.common.xcontent.XContentBuilder
import org.xbib.common.xcontent.json.JsonXContent
import org.xbib.common.xcontent.xml.XmlXContent
import org.xbib.common.xcontent.xml.XmlXParams
import org.xbib.webapp.Constants
import org.xbib.util.PathUriResolver
import org.xbib.xml.namespace.XmlNamespaceContext

import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.Result
import javax.xml.transform.Templates
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.URIResolver
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.sax.TransformerHandler
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.nio.file.Paths
import java.time.Instant

import static org.xbib.common.xcontent.XContentService.jsonBuilder

class OAIService {

    private final static NS_URI = "http://xbib.org/ns/oai/elasticsearch/source/1.0/"

    private final Settings settings

    private final SAXTransformerFactory transformerFactory

    private final QName source

    private final XmlFactory xmlFactory

    private final ElasticsearchClient client

    OAIService(Settings settings, ElasticsearchClient client) {
        this.settings = settings
        this.client = client
        this.transformerFactory = createTransformerFactory() as SAXTransformerFactory
        URI uri = URI.create(settings.get(Constants.WEBAPP_HOME_PARAMETER))
        URIResolver uriResolver = new PathUriResolver(Paths.get(uri))
        transformerFactory.setURIResolver(uriResolver)
        this.source = new QName(NS_URI, "source")
        this.xmlFactory = createXmlFactory(createXMLInputFactory(), createXMLOutputFactory())
    }

    Map<String,Object> identify(String endpoint, IdentifyRequest identifyRequest) throws Exception {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
        searchRequestBuilder.setIndices(endpoint)
                .setSize(1)
                .addSort(SortBuilders.fieldSort("_timestamp").order(SortOrder.ASC))
                .addField('_timestamp')
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet(15000L)
        XContentBuilder builder = jsonBuilder()
        if (searchResponse.hits.hits.length == 1) {
            builder.field("earliesttimestamp", searchResponse.hits.hits[0].field('_timestamp').value)
        }
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }

    Map<String,Object> getRecord(String endpoint, GetRecordRequest getRecordRequest) throws Exception {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
        searchRequestBuilder.setIndices(endpoint)
                .setQuery(QueryBuilders.termQuery('_uid', getRecordRequest.type + '#' + getRecordRequest.id))
                .setSize(1)
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet(15000L)
        XContentBuilder builder = jsonBuilder()
        if (searchResponse.hits.hits.length == 1) {
            buildResponse(builder, getRecordRequest, searchResponse, "identifier", endpoint)
        } else {
            throw new OAIException('idDoesNotExist')
        }
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }

    Map<String,Object> listIdentifiers(String endpoint, ListIdentifiersRequest listIdentifiersRequest) throws Exception {
        SearchResponse searchResponse
        int scrollSize = settings.getAsInt("scrollsize", 1000)
        long scrollMillis = settings.getAsTime("scrolltimeout", org.xbib.common.unit.TimeValue.timeValueSeconds(60)).millis()
        if (listIdentifiersRequest.resumptionToken == null) {
            ResumptionToken resumptionToken = ResumptionToken.newToken()
            listIdentifiersRequest.setResumptionToken(resumptionToken)
            SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
            searchRequestBuilder.setIndices(endpoint)
                    .setQuery(listIdentifiersRequest.elasticsearchQuery)
                    .setSize(scrollSize)
                    .setScroll(TimeValue.timeValueMillis(scrollMillis))
                    .addSort(SortBuilders.fieldSort("_timestamp").order(SortOrder.ASC))
                    .addField('_timestamp')
            searchResponse = searchRequestBuilder.execute().actionGet(15000L)
        } else {
            searchResponse = new SearchScrollRequestBuilder(client, SearchScrollAction.INSTANCE)
                    .setScrollId(listIdentifiersRequest.resumptionToken.searchResponse.getScrollId())
                    .setScroll(TimeValue.timeValueMillis(scrollMillis))
                    .execute().actionGet()
            long cursor = listIdentifiersRequest.resumptionToken.getCursor()
            listIdentifiersRequest.resumptionToken.setCursor(cursor + searchResponse.hits.hits.length)
        }
        listIdentifiersRequest.resumptionToken.setSearchResponse(searchResponse)
        listIdentifiersRequest.resumptionToken.setExpireAt(Instant.now().plusSeconds(60))
        listIdentifiersRequest.resumptionToken.setCompleteListSize(searchResponse.hits.totalHits)
        XContentBuilder builder = jsonBuilder()
        builder.startObject()
        buildResponseIdentifiers(builder, searchResponse)
        builder.startObject("resumptiontoken")
                .field('key', listIdentifiersRequest.resumptionToken.key)
                .field('expirationDate', listIdentifiersRequest.resumptionToken.expireAt)
                .field('completeListSize', listIdentifiersRequest.resumptionToken.completeListSize)
                .field('cursor', listIdentifiersRequest.resumptionToken.cursor)
                .endObject()
        builder.endObject()
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }

    Map<String,Object> listRecords(String endpoint, ListRecordsRequest listRecordsRequest) throws Exception {
        SearchResponse searchResponse
        int scrollSize = settings.getAsInt("scrollsize", 1000)
        long scrollMillis = settings.getAsTime("scrolltimeout", org.xbib.common.unit.TimeValue.timeValueSeconds(60)).millis()
        if (listRecordsRequest.resumptionToken == null) {
            ResumptionToken resumptionToken = ResumptionToken.newToken()
            listRecordsRequest.setResumptionToken(resumptionToken)
            SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
            searchRequestBuilder.setIndices(endpoint)
                    .setQuery(listRecordsRequest.elasticsearchQuery)
                    .setSize(scrollSize)
                    .setScroll(TimeValue.timeValueMillis(scrollMillis))
                    .addSort(SortBuilders.fieldSort("_timestamp").order(SortOrder.ASC))
                    .addField('_source')
                    .addField('_timestamp')
            searchResponse = searchRequestBuilder.execute().actionGet(15000L)
        } else {
            searchResponse = new SearchScrollRequestBuilder(client, SearchScrollAction.INSTANCE)
                    .setScrollId(listRecordsRequest.resumptionToken.searchResponse.getScrollId())
                    .setScroll(TimeValue.timeValueMillis(scrollMillis))
                    .execute().actionGet()
            listRecordsRequest.resumptionToken.setSearchResponse(searchResponse)
        }
        XContentBuilder builder = jsonBuilder()
        builder.startObject()
        buildResponse(builder, listRecordsRequest, searchResponse, "identifier", endpoint)
        builder.startObject("resumptiontoken")
                .field('key', listRecordsRequest.resumptionToken.key)
                .field('expirationDate', listRecordsRequest.resumptionToken.expireAt)
                .field('completeListSize', listRecordsRequest.resumptionToken.completeListSize)
                .field('cursor', listRecordsRequest.resumptionToken.cursor)
                .endObject()
        builder.endObject()
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }


    Map<String,Object> listMetadataFormats(String endpoint, ListMetadataFormatsRequest listMetadataFormatsRequest) throws Exception {
        XContentBuilder builder = jsonBuilder()
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }

    Map<String,Object> listSets(String endpoint, ListSetsRequest listMetadataFormatsRequest) throws Exception {
        XContentBuilder builder = jsonBuilder()
        JsonXContent.jsonXContent.createParser(builder.string()).mapOrderedAndClose()
    }

    private static void buildResponseIdentifiers(XContentBuilder builder, SearchResponse response) throws Exception {
        long total = response.hits.totalHits
        builder.field("total", total)
        if (response.hits.hits.length > 0) {
            builder.startArray("records")
            for (SearchHit hit : response.hits.hits) {
                builder.startObject()
                        .field("index", hit.getIndex())
                        .field("type", hit.getType())
                        .field("id", hit.getId())
                        .field("score", hit.getScore())
                builder.endObject()
            }
            builder.endArray()
        }
    }

    private void buildResponse(XContentBuilder builder,
                                 OAIRequest request, SearchResponse response,
                                 String key, String... values) throws Exception {
        long total = response.hits.totalHits
        builder.field("total", total)
        if (response.hits.hits.length > 0) {
            XmlNamespaceContext context = XmlNamespaceContext.newInstance()
            context.addNamespace("", NS_URI)
            XmlXParams xmlParams
            xmlParams = new XmlXParams(source, context, xmlFactory)
            builder.startArray("records")
            for (SearchHit hit : response.hits.hits) {
                builder.startObject()
                        .field("index", hit.getIndex())
                        .field("type", hit.getType())
                        .field("id", hit.getId())
                        .field("score", hit.getScore())
                XContentBuilder xmlBuilder = XmlXContent.contentBuilder(xmlParams)
                xmlBuilder.startObject()
                filter(xmlBuilder, hit.getSource(), key, values)
                xmlBuilder.endObject()
                builder.field("recorddata", request.getStylesheet() == null ? xmlBuilder.string() :
                        transform(request.getStylesheet(), request.getDefaultStylesheet(), xmlBuilder.string())
                )
                builder.endObject()
            }
            builder.endArray()
        }
    }

    /**
     * Filter out unwanted elements from Elasticsearch document.
     * Remove all "xbib" keys, and remove ISILs if they are set in "identifier" and are not the index
     *
     * @param builder the builder
     * @param map Elasticsearch document
     * @return the filtered Elasticsearch document
     */
    private static boolean filter(XContentBuilder builder, Map<String,Object> map, String key, String... values) {
        boolean hasContent = false
        for (Map.Entry<String,Object> me : map.entrySet()) {
            if ("xbib".equals(me.key)) {
                continue
            }
            if (key == null) {
                builder.field(me.key, me.value)
                hasContent = true
                continue
            }
            if (me.value instanceof Map) {
                Map m = (Map)me.value
                boolean b = false
                if (m.containsKey(key)) {
                    for (String v : values) {
                        String s = m.get(key).toString()
                        // main ISIL or not?
                        if (s.indexOf("-") < s.lastIndexOf("-")) {
                            if (s.startsWith(v)) {
                                b = true
                            }
                        } else if (v.equals(s)) {
                            b = true
                        }
                    }
                } else {
                    b = true
                }
                if (b) {
                    builder.field(me.key)
                    builder.map(m)
                    hasContent = true
                }
            } else if (me.value instanceof List) {
                builder.startArray(me.key)
                List list = (List)me.value
                for (Object o : list) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>)o
                        boolean b = false
                        if (m.containsKey(key)) {
                            for (String v : values) {
                                String s = m.get(key).toString()
                                // main ISIL or not?
                                if (s.indexOf("-") < s.lastIndexOf("-")) {
                                    if (s.startsWith(v)) {
                                        b = true
                                    }
                                } else if (v.equals(s)) {
                                    b = true
                                }
                            }
                        } else {
                            b = true
                        }
                        if (b) {
                            builder.map(m)
                            hasContent = true
                        }
                    } else {
                        builder.value(o)
                        hasContent = true
                    }
                }
                builder.endArray()
            } else {
                builder.field(me.key, me.value)
                hasContent = true
            }
        }
        return hasContent
    }

    private String transform(String stylesheet, String defaultStylesheet, String renderedTemplate)
            throws TransformerException {
        StreamSource xslSource
        try {
            xslSource = transformerFactory.getURIResolver().resolve(stylesheet, null) as StreamSource
        } catch (TransformerException e1) {
            // styleheet does not exist
            try {
                xslSource = transformerFactory.getURIResolver().resolve(defaultStylesheet, null) as StreamSource
            } catch (TransformerException e2) {
                // recordSchema not configured
                return renderedTemplate
            }
        }
        StringWriter sw = new StringWriter()
        try {
            Templates templates = transformerFactory.newTemplates(xslSource)
            // we need a new transformer handler for every transformation
            TransformerHandler handler = transformerFactory.newTransformerHandler(templates)
            Result result = new StreamResult(sw)
            handler.setResult(result)
            transformerFactory.newTransformer().transform(new StreamSource(new StringReader(renderedTemplate)),
                    new SAXResult(handler))
        } finally {
            xslSource.getReader().close()
        }
        sw.toString()
    }

    private static TransformerFactory createTransformerFactory() {
        new TransformerFactoryImpl()
    }

    private static XmlFactory createXmlFactory(XMLInputFactory inputFactory, XMLOutputFactory outputFactory) {
        XmlFactory xmlFactory = new XmlFactory(inputFactory, outputFactory)
        xmlFactory.configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true)
        xmlFactory
    }

    private static XMLInputFactory createXMLInputFactory() {
        XMLInputFactory inputFactory = new WstxInputFactory()
        inputFactory.setProperty("javax.xml.stream.isNamespaceAware", Boolean.TRUE)
        inputFactory.setProperty("javax.xml.stream.isValidating", Boolean.FALSE)
        inputFactory.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE)
        inputFactory.setProperty("javax.xml.stream.isReplacingEntityReferences", Boolean.FALSE)
        inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE)
        inputFactory
    }

    private static XMLOutputFactory createXMLOutputFactory() {
        XMLOutputFactory outputFactory = new WstxOutputFactory()
        outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.FALSE)
        outputFactory
    }
}
