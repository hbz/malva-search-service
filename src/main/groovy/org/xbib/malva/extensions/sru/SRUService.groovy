package org.xbib.malva.extensions.sru

import com.ctc.wstx.stax.WstxInputFactory
import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import groovy.util.logging.Log4j2
import org.apache.xalan.processor.TransformerFactoryImpl
import org.elasticsearch.action.search.*
import org.elasticsearch.client.ElasticsearchClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.xbib.content.XContentBuilder
import org.xbib.content.json.JsonXContent
import org.xbib.content.resource.XmlNamespaceContext
import org.xbib.content.settings.Settings
import org.xbib.content.xml.XmlXContent
import org.xbib.content.xml.XmlXParams
import org.xbib.content.xml.util.XMLUtil
import org.xbib.malva.MalvaConstants
import org.xbib.util.PathUriResolver

import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.*
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.sax.TransformerHandler
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.nio.file.Paths

import static org.xbib.content.json.JsonXContent.contentBuilder

/**
 * SRU service.
 */
@Log4j2
class SRUService implements MalvaConstants {

    private final static NS_URI = "http://xbib.org/ns/sru/elasticsearch/source/1.0/";

    private final Settings settings

    private final ElasticsearchClient client

    private final SAXTransformerFactory transformerFactory

    private final QName source

    private final QName holdings

    private final XmlFactory xmlFactory

    SRUService(Settings settings, ElasticsearchClient client) {
        this.settings = settings
        this.client = client
        this.source = new QName(NS_URI, "source")
        this.holdings = new QName(NS_URI, "holdings")
        this.xmlFactory = createXmlFactory(createXMLInputFactory(), createXMLOutputFactory())
        this.transformerFactory = createTransformerFactory(settings) as SAXTransformerFactory
    }

    Map<String,Object> searchRetrieve(String endpoint, SearchRetrieveRequest searchRetrieveRequest, boolean xml)
            throws Exception {
        searchRetrieve(endpoint, searchRetrieveRequest, contentBuilder(), xml)
    }

    Map<String,Object> searchRetrieve(String endpoint,
                          SearchRetrieveRequest searchRetrieveRequest,
                          XContentBuilder builder, boolean xml) throws Exception {
        XContentBuilder logmsg = contentBuilder()
        logmsg.startObject()
                .field("type", "request")
                .field("endpoint", endpoint)
                .field("version", searchRetrieveRequest.version)
                .field("operation", SRUConstants.SEARCH_RETRIEVE_OPERATION)
                .fieldIfNotNull("recordSchema", searchRetrieveRequest.recordSchema)
                .fieldIfNotNull("extraRequestData", searchRetrieveRequest.extraRequestData)
                .field("cql", searchRetrieveRequest.cqlQuery)
                .fieldIfNotNull("facetLimit", searchRetrieveRequest.facetLimit)
                .endObject()
        log.info("{}", logmsg.string())
        if (client == null) {
            throw new IllegalStateException("no Elasticsearch client")
        }
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
        searchRequestBuilder.setIndices(endpoint)
        String elasticsearchQuery = searchRetrieveRequest.getElasticsearchQuery()
        searchRequestBuilder.setExtraSource(elasticsearchQuery)
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet(15000L)
        logmsg = contentBuilder()
        logmsg.startObject()
                .field("type", "response")
                .field("endpoint", endpoint)
                .field("version", searchRetrieveRequest.version)
                .field("operation", SRUConstants.SEARCH_RETRIEVE_OPERATION)
                .fieldIfNotNull("recordSchema", searchRetrieveRequest.recordSchema)
                .fieldIfNotNull("extraRequestData", searchRetrieveRequest.extraRequestData)
                .field("cql", searchRetrieveRequest.cqlQuery)
                .field("elasticsearchQuery", elasticsearchQuery)
                .field("hits", searchResponse.hits.totalHits)
                .field("took", searchResponse.tookInMillis)
                .endObject()
        log.info("{}", logmsg.string())
        String body = buildResponse(builder, endpoint, xml,
                searchRetrieveRequest, searchResponse, "identifier", endpoint)
        JsonXContent.jsonContent().createParser(body).mapOrderedAndClose()
    }

    private String buildResponse(XContentBuilder builder, String index, boolean xml,
                                 SearchRetrieveRequest request, SearchResponse response,
                                 String key, String... values) throws Exception {
        builder.startObject()
        long total = response.hits.totalHits
        builder.field("total", total)
        if (response.hits.hits) {
            String relatedIndex = request.extraRequestData ?: 'holdings'
            XmlNamespaceContext context = XmlNamespaceContext.newInstance()
            context.addNamespace("", NS_URI)
            XmlXParams xmlParams

            if ("1.1".equals(request.version) || "1.2".equals(request.version)) {
                context.addNamespace("sru", SRUConstants.SRW_NS_URI)
            } else {
                context.addNamespace("sru", SRUConstants.SEARCH_RETRIEVE_NS_URI)
            }
            xmlParams = new XmlXParams(source, context, xmlFactory)
            List<XContentBuilder> logmsgs = new LinkedList<>()
            builder.startArray("records")
            for (SearchHit hit : response.hits.hits) {
                builder.startObject()
                    .field("index", hit.getIndex())
                    .field("type", hit.getType())
                    .field("id", hit.getId())
                    .field("score", hit.getScore())
                    .fieldIfNotNull("recordschema", request.recordSchema ?: 'mods')
                    .field("recordpacking", settings.get("webapp.sru.searchretrieve.recordPacking", "xml"))
                if (xml) {
                    XContentBuilder xmlBuilder = XmlXContent.contentBuilder(xmlParams)
                    xmlBuilder.startObject()
                    filter(xmlBuilder, hit.getSource(), key, values)
                    fetchRelatedDocs(xmlParams, xmlBuilder, xml, logmsgs, index, hit.getIndex(), relatedIndex, hit.getId(), key, values)
                    xmlBuilder.endObject()
                    builder.field("recorddata", request.getStylesheet() == null ? xmlBuilder.string() :
                            transform(request.getStylesheet(), request.getDefaultStylesheet(), xmlBuilder.string()))
                } else {
                    builder.startObject("recorddata")
                    builder.startObject("source")
                    filter(builder, hit.getSource(), key, values)
                    builder.endObject()
                    fetchRelatedDocs(xmlParams, builder, xml, logmsgs, index, hit.getIndex(), relatedIndex, hit.getId(), key, values)
                    builder.endObject()
                }
                builder.endObject()
            }
            builder.endArray()
            if (!logmsgs.isEmpty()) {
                XContentBuilder logBuilder = contentBuilder()
                logBuilder.startObject()
                logBuilder.field("type", "related")
                logBuilder.field("index", index)
                logBuilder.fieldIfNotNull("recordschema", request.recordSchema)
                logBuilder.startArray("related")
                logBuilder.copy(logmsgs)
                logBuilder.endArray()
                log.info("{}", logBuilder.string())
            }
        }
        if (response.getAggregations()) {
            String query = request.cqlQuery
            builder.startArray('facets')
            for (Aggregation aggregation : response.getAggregations()) {
                if (aggregation instanceof StringTerms) {
                    StringTerms stringTerms = (StringTerms) aggregation
                    builder.startObject()
                    builder.field("name", stringTerms.name)
                    builder.startArray("buckets")
                    for (Terms.Bucket bucket : stringTerms.buckets) {
                        builder.startObject()
                        builder.field("term", bucket.key)
                        builder.field("count", bucket.docCount)
                        String newFilter = stringTerms.getName() + "=\"" + URLEncoder.encode(bucket.getKey().toString(), "UTF-8") + "\""
                        String filter = request.filter ? request.filter + " and " + newFilter : newFilter
                        String path = request.path <<
                                "?operation=searchRetrieve" <<
                                (request.version ? '&version=' << request.version : '') <<
                                (query ? "&query=" << query : '')  <<
                                (filter ? "&filter=" << filter : '') <<
                                (request.recordSchema ? '&recordSchema=' << request.recordSchema : '') <<
                                (request.extraRequestData ? '&extraRequestData=' << request.extraRequestData : '') <<
                                (request.startRecord ? '&startRecord=' << request.startRecord : '') <<
                                (request.maximumRecords ? '&maximumRecords=' << request.maximumRecords : '' ) <<
                                (request.facetLimit ? '&facetLimit=' << request.facetLimit : '')
                        builder.field("requestUrl", XMLUtil.escape(path))
                        builder.endObject()
                    }
                    builder.endArray()
                    builder.endObject()
                } else if (aggregation instanceof LongTerms) {
                    LongTerms longTerms = aggregation as LongTerms
                    builder.startObject()
                    builder.field("name", longTerms.name)
                    builder.startArray("buckets")
                    for (Terms.Bucket bucket : longTerms.getBuckets()) {
                        builder.startObject()
                        builder.field("term", bucket.getKey())
                        builder.field("count", bucket.getDocCount())
                        String newFilter = longTerms.getName() + "=\"" + URLEncoder.encode(bucket.getKey().toString(), "UTF-8") + "\""
                        String filter = request.filter ? request.filter + " and " + newFilter : newFilter
                        String path = request.path <<
                                "?operation=searchRetrieve" <<
                                (request.version ? '&version=' << request.version : '') <<
                                (query ? "&query=" << query : '') <<
                                (filter ? "&filter=" << filter : '') <<
                                (request.recordSchema ? '&recordSchema=' << request.recordSchema : '') <<
                                (request.extraRequestData ? '&extraRequestData=' << request.extraRequestData : '') <<
                                (request.startRecord ? '&startRecord=' << request.startRecord : '') <<
                                (request.maximumRecords ? '&maximumRecords=' << request.maximumRecords : '' ) <<
                                (request.facetLimit ? '&facetLimit=' << request.facetLimit : '')
                        builder.field("requestUrl", XMLUtil.escape(path))
                        builder.endObject()
                    }
                    builder.endArray()
                    builder.endObject()
                }
            }
            builder.endArray()
        }
        builder.string()
    }

    private void fetchRelatedDocs(XmlXParams xmlXParams, XContentBuilder builder, boolean xml, List<XContentBuilder> logmsgs,
                                  String index, String hitIndex, String relatedIndex, String uid, String key, String... values) {
        if (relatedIndex == null) {
            return
        }
        String relatedHitIndex = hitIndex.replaceAll('\\d{8,}$','') << relatedIndex
        try {
            SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
                    .setIndices(relatedHitIndex)
                    .setQuery(QueryBuilders.termQuery("xbib.uid", uid))
                    .setSize(100) // should be reasonable to avoid most scrolls
                    .setScroll(TimeValue.timeValueMillis(1000))
            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet(15000L)
            long took = searchResponse.tookInMillis
            long totalHits = searchResponse.hits.totalHits
            boolean open = false
            if (totalHits > 0) {
                builder.startArray(relatedIndex)
                open = true
            }
            while (searchResponse.getHits().getHits().length > 0) {
                SearchHits hits = searchResponse.hits
                took += searchResponse.tookInMillis
                for (SearchHit hit : hits) {
                    XmlXParams holdingsXmlXParams = new XmlXParams(holdings, xmlXParams.namespaceContext, xmlFactory)
                    XContentBuilder hitBuilder = xml ? XmlXContent.contentBuilder(holdingsXmlXParams) : contentBuilder()
                    hitBuilder.startObject()
                    hitBuilder.field("index", xml ? XMLUtil.escape(hit.getIndex()) : hit.getIndex())
                    hitBuilder.field("type", xml ? XMLUtil.escape(hit.getType()) : hit.getType())
                    hitBuilder.field("id", xml ? XMLUtil.escape(hit.getId()) : hit.getId())
                    hitBuilder.startObject("source")
                    // skip filtering if hit is "local" i.e. hit index looks like requested index
                    boolean isLocal = isLocal(hit.getIndex(), index)
                    boolean hasContent = filter(hitBuilder, hit.getSource(), isLocal ? null : key, values)
                    hitBuilder.endObject()
                    hitBuilder.endObject()
                    // skip hits where the content is completely filtered out
                    if (hasContent) {
                        builder.copy(hitBuilder)
                    }
                }
                searchResponse = new SearchScrollRequestBuilder(client, SearchScrollAction.INSTANCE)
                        .setScrollId(searchResponse.getScrollId())
                        .setScroll(TimeValue.timeValueMillis(1000))
                        .execute().actionGet(15000L)
            }
            if (open) {
                builder.endArray()
            }
            XContentBuilder logBuilder = contentBuilder()
            logBuilder.startObject()
                    .field("index", relatedHitIndex)
                    .field("uid", uid)
                    .field("hits", totalHits)
                    .field("took", took)
                    .endObject()
            logmsgs.add(logBuilder)
        } catch (IndexNotFoundException e) {
            log.warn(relatedHitIndex + ": " + e.getMessage())
        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }
    }

    private static boolean isLocal(String index, String otherIndex) {
        return index.replaceAll('\\d{8,}$','').startsWith(otherIndex.replaceAll('\\d{8,}$',''))
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

    private static TransformerFactory createTransformerFactory(Settings settings) {
        TransformerFactory transformerFactory = new TransformerFactoryImpl()
        URI uri = URI.create(settings.get(HOME_URI_PARAMETER))
        URIResolver uriResolver = new PathUriResolver(Paths.get(uri))
        transformerFactory.setURIResolver(uriResolver)
        transformerFactory
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
