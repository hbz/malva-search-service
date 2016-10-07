package org.xbib.webapp.extensions.sru

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import groovy.util.logging.Log4j2
import org.elasticsearch.client.ElasticsearchClient
import org.xbib.content.settings.Settings
import org.xbib.webapp.Webapp
import org.xbib.webapp.WebappBinding
import org.xbib.webapp.WebappExtension
import org.xbib.util.MultiMap

@Log4j2
class SRUExtension implements WebappExtension, SRUConstants {

    private final String name

    private final Settings settings

    ElasticsearchClient client

    SRUService sruService

    @Inject
    SRUExtension(@Assisted String name,
                    @Assisted Settings settings,
                    @Assisted Webapp webapp) {
        this.name = name
        this.settings = settings
        this.client =  webapp.webappService().elasticsearchService().client()
        this.sruService = new SRUService(settings, this.client)
        log.info('SRU extension started')
    }

    @Override
    void prepareBinding(WebappBinding binding) {
    }

    @Override
    void shutdown() {
    }

    @Override
    String name() {
        name
    }

    @Override
    SRUExtension object() {
        this
    }

    static String normalizeIndexName(String index) {
        // remove trailing digits (timestamp info)
        index.replaceAll('\\d+$','')
    }

    static String version(MultiMap params) {
        params.get(VERSION_PARAMETER, '2.0') as String
    }

    static String recordSchema(MultiMap params) {
        params.get(RECORD_SCHEMA_PARAMETER) as String
    }

    static String responseType(MultiMap params) {
        String version = version(params)
        String responseType = null
        switch (version) {
            case '1.1' :
                responseType = 'text/xml'
                break
            case '1.2' :
                // see http://www.loc.gov/standards/sru/companionSpecs/transport.html
                // there is NO DEFINED response content type for SRU 1.2 !!!
                // we use text/xml
                responseType = 'text/xml'
                break
            case '2.0':
                responseType = 'application/sru+xml'
                break
        }
        responseType
    }

    static String stylesheet(MultiMap params) {
        String recordSchema = recordSchema(params)
        String index = normalizeIndexName(params.getString('index', '_all')) as String
        recordSchema ? 'stylesheets/' + recordSchema + '/' + index + '.xsl' : null
    }

    static String defaultStylesheet(MultiMap params) {
        String recordSchema = recordSchema(params)
        recordSchema ? 'stylesheets/' + recordSchema + '/' + '_all.xsl' : null
    }

    static String namespace(String version) {
        String namespace = 'http://docs.oasis-open.org/ns/search-ws/sruResponse'
        if( version == '1.2') {
            namespace = 'http://www.loc.gov/zing/srw/'
        }
        namespace
    }

    static String schemaLocation(String version) {
        String schemaLocation = 'http://docs.oasis-open.org/ns/search-ws/sruResponse http://www.loc.gov/standards/sru/sru-2-0/schemas/sruResponse.xsd'
        if( version == '1.2') {
            schemaLocation = 'http://www.loc.gov/zing/srw/ http://www.loc.gov/standards/sru/xmlFiles/srw-types.xsd'
        }
        schemaLocation
    }

    Map<String,Object> execute(String path, MultiMap params, boolean xml) {
        // map query from multiple parameters
        String cql = params.get("cql")
        if (cql == null) {
            cql = params.get(QUERY_PARAMETER)
        }
        // OpenSearch
        if (cql == null) {
            cql = params.get("q")
        }
        def searchRetrieveRequestBuilder = SearchRetrieveRequest.builder()
                .stylesheet(stylesheet(params))
                .defaultStylesheet(defaultStylesheet(params))
                .version(version(params))
                .path(path)
                .recordSchema(recordSchema(params))
                .cqlQuery(cql)
                .startRecord(params.get(START_RECORD_PARAMETER, 1) as Integer)
                .maximumRecords(params.get(MAXIMUM_RECORDS_PARAMETER, 10) as Integer)
                .filter(params.get(FILTER_PARAMETER) as String)
                .orfilter(params.findAll { it.key.toString().startsWith("filter.or.") })
                .andfilter(params.findAll { it.key.toString()startsWith("filter.and.") })
                .facetLimit(params.get(FACET_LIMIT_PARAMETER) as String)
                .facetCount(params.get(FACET_COUNT_PARAMETER) as String)
                .facetStart(params.get(FACET_START_PARAMETER) as String) // not supported
                .facetSort(params.get(FACET_SORT_PARAMETER) as String) // not supported
                .resultSetTTL(params.get(RESULT_SET_TTL_PARAMETER, 0) as Integer)
                .extraRequestData(params.get(EXTRA_REQUEST_DATA_PARAMETER) as String)
        sruService.searchRetrieve(params.get('index', '_all') as String,
                searchRetrieveRequestBuilder.build().validate(), xml)
    }

    Map<String,Object> execute(String path, SearchRetrieveRequest searchRetrieveRequest) {
        sruService.searchRetrieve(path, searchRetrieveRequest, true)
    }

}
