package org.xbib.malva.extensions.sru

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import groovy.util.logging.Log4j2
import org.elasticsearch.client.ElasticsearchClient
import org.xbib.content.settings.Settings
import org.xbib.malva.MalvaExtension
import org.xbib.malva.MalvaBinding
import org.xbib.malva.Webapp
import org.xbib.malva.elasticsearch.ElasticsearchService
import org.xbib.malva.util.MultiMap

/**
 *
 */
@Log4j2
class SRUExtension implements MalvaExtension, SRUConstants {

    String name

    Settings settings

    Webapp webapp

    ElasticsearchService elasticsearchService

    SRUService sruService

    @Inject
    SRUExtension(@Assisted String name,
                    @Assisted Settings settings,
                    @Assisted Webapp webapp,
                    ElasticsearchService elasticsearchService) {
        this.name = name
        this.settings = settings
        this.webapp = webapp
        this.elasticsearchService = elasticsearchService
        log.info('SRU extension started, Elasticsearch service = {}', elasticsearchService)
    }

    @Override
    void prepareBinding(MalvaBinding binding) {
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

    Map<String,Object> execute(String path, MultiMap params, boolean xml) {
        // map query from multiple parameters
        String cql = params.getString("cql")
        if (cql == null) {
            cql = params.getString(QUERY_PARAMETER)
        }
        // OpenSearch
        if (cql == null) {
            cql = params.getString("q")
        }
        def searchRetrieveRequestBuilder = SearchRetrieveRequest.builder()
                .stylesheet(stylesheet(params))
                .defaultStylesheet(defaultStylesheet(params))
                .version(version(params))
                .path(path)
                .recordSchema(recordSchema(params))
                .cqlQuery(cql)
                .startRecord(params.getInteger(START_RECORD_PARAMETER, 1))
                .maximumRecords(params.getInteger(MAXIMUM_RECORDS_PARAMETER, 10))
                .filter(params.getString(FILTER_PARAMETER))
                .orfilter(params.findAll { it.key.toString().startsWith("filter.or.") })
                .andfilter(params.findAll { it.key.toString()startsWith("filter.and.") })
                .facetLimit(params.getString(FACET_LIMIT_PARAMETER))
                .facetCount(params.getString(FACET_COUNT_PARAMETER))
                .facetStart(params.getString(FACET_START_PARAMETER)) // not supported yet
                .facetSort(params.getString(FACET_SORT_PARAMETER)) // not supported yet
                .resultSetTTL(params.getInteger(RESULT_SET_TTL_PARAMETER, 0))
                .extraRequestData(extraRequestData(params))

        execute(params.getString('index', '_all') as String,
                searchRetrieveRequestBuilder.build().validate(), xml)
    }

    Map<String,Object> execute(String path, SearchRetrieveRequest searchRetrieveRequest) {
        execute(path, searchRetrieveRequest, true)
    }

    Map<String,Object> execute(String path, SearchRetrieveRequest searchRetrieveRequest, boolean xml) {
        if (sruService == null) {
            ElasticsearchClient client = elasticsearchService.resources.get(webapp).client
            log.info('the Elastiscearch client is: {}', client)
            this.sruService = new SRUService(webapp.settings(), client)
        }
        sruService.searchRetrieve(path, searchRetrieveRequest, xml)
    }

    static String normalizeIndexName(String index) {
        // remove trailing digits (timestamp info)
        index.replaceAll('\\d+$','')
    }

    static String version(MultiMap params) {
        params.getString(VERSION_PARAMETER, '2.0')
    }

    static String recordSchema(MultiMap params) {
        params.getString(RECORD_SCHEMA_PARAMETER, 'mods')
    }

    static String extraRequestData(MultiMap params) {
        params.getString(EXTRA_REQUEST_DATA_PARAMETER, 'holdings')
    }

    static String responseType(MultiMap params) {
        String version = version(params)
        String responseType = null
        switch (version) {
            case '1.1' :
                // nothing found about SRU 1.1 response type
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
        String index = normalizeIndexName(params.getString('index', '_all'))
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
}
