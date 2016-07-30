package org.xbib.webapp.extensions.oai

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import groovy.util.logging.Log4j2
import org.elasticsearch.client.ElasticsearchClient
import org.xbib.common.settings.Settings
import org.xbib.webapp.MultiMap
import org.xbib.webapp.Webapp
import org.xbib.webapp.WebappBinding
import org.xbib.webapp.WebappExtension

import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Log4j2
class OAIExtension implements WebappExtension, OAIConstants {

    private final String name

    private final Settings settings

    ElasticsearchClient client

    OAIService oaiService

    @Inject
    OAIExtension(@Assisted String name,
                 @Assisted Settings settings,
                 @Assisted Webapp webapp) {
        this.name = name
        this.settings = settings
        this.client = webapp.webappService().elasticsearchService().client()
        this.oaiService = new OAIService(settings, this.client)
        log.info('OAI extension started')
    }

    @Override
    void prepareBinding(WebappBinding binding) {
    }

    @Override
    String name() {
        name
    }

    @Override
    OAIExtension object() {
        this
    }

    static String stylesheet(MultiMap params) {
        String metadataPrefix = params.getString(METADATA_PREFIX_PARAMETER)
        String index = normalizeIndexName(params.getString('index', '_all')) as String
        'stylesheets/oai/' + metadataPrefix + '/' + index + '.xsl'
    }

    static String defaultStylesheet(MultiMap params) {
        String metadataPrefix = params.getString(METADATA_PREFIX_PARAMETER)
        'stylesheets/oai/' + metadataPrefix + '/' + '_all.xsl'
    }

    static String normalizeIndexName(String index) {
        // remove trailing digits (timestamp info)
        index.replaceAll('\\d+$','')
    }

    Map<String,Object> execute(String path, MultiMap params) {
        switch (params.getString(VERB_PARAMETER)) {
            case IDENTIFY:
                return identify(path, params)
            case GET_RECORD:
                return getrecord(path, params)
            case LIST_IDENTIFIERS:
                return listidentifiers(path, params)
            case LIST_RECORDS :
                return listrecords(path, params)
            case LIST_METADATA_FORMATS :
                return listmetadataformats(path, params)
            default:
                throw new IOException('unknown verb')
        }
    }

    Map<String,Object> identify(String path, MultiMap params) {
        def identifyRequestBuilder = IdentifyRequest.builder()
        oaiService.identify(params.get('index', '_all') as String, identifyRequestBuilder.build())
    }

    Map<String,Object> getrecord(String path, MultiMap params) {
        def getRecordRequestBuilder = GetRecordRequest.builder()
                .path(path)
                .stylesheet(stylesheet(params))
                .defaultStylesheet(defaultStylesheet(params))
                .identifier(params.getString('identifier'))
                .metadataPrefix(params.getString(METADATA_PREFIX_PARAMETER))
        oaiService.getRecord(params.get('index', '_all') as String, getRecordRequestBuilder.build())
    }

    Map<String,Object> listidentifiers(String path, MultiMap params) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("GMT"))
        def listIdentifiersRequestBuilder = ListIdentifiersRequest.builder()
                .path(path)
                .stylesheet(stylesheet(params))
                .defaultStylesheet(defaultStylesheet(params))
                .from(params.getInstant(FROM_PARAMETER, dateTimeFormatter))
                .until(params.getInstant(UNTIL_PARAMETER, dateTimeFormatter))
                .set(params.getString(SET_PARAMETER))
                .metadataPrefix(params.getString(METADATA_PREFIX_PARAMETER))
        if (params.get(RESUMPTION_TOKEN_PARAMETER)) {
            ResumptionToken resumptionToken = ResumptionToken.get(UUID.fromString(params.getString(RESUMPTION_TOKEN_PARAMETER)))
            if (resumptionToken == null) {
                throw new OAIException('badResumptionToken')
            }
            listIdentifiersRequestBuilder.resumptionToken(resumptionToken)
        }
        oaiService.listIdentifiers(params.get('index', '_all') as String, listIdentifiersRequestBuilder.build())
    }

    Map<String,Object> listrecords(String path, MultiMap params) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("GMT"))
        def listRecordsRequestBuilder = ListRecordsRequest.builder()
                .path(path)
                .stylesheet(stylesheet(params))
                .defaultStylesheet(defaultStylesheet(params))
                .from(params.getInstant(FROM_PARAMETER, dateTimeFormatter))
                .until(params.getInstant(UNTIL_PARAMETER, dateTimeFormatter))
                .set(params.getString(SET_PARAMETER))
                .metadataPrefix(params.getString(METADATA_PREFIX_PARAMETER))
        if (params.get(RESUMPTION_TOKEN_PARAMETER)) {
            ResumptionToken resumptionToken = ResumptionToken.get(UUID.fromString(params.getString(RESUMPTION_TOKEN_PARAMETER)))
            if (resumptionToken == null) {
                throw new OAIException('badResumptionToken')
            }
            listRecordsRequestBuilder.resumptionToken(resumptionToken)
        }
        oaiService.listRecords(params.get('index', '_all') as String, listRecordsRequestBuilder.build())
    }

    Map<String,Object> listmetadataformats(String path, MultiMap params) {
        def listMetadataFormatsRequestBuilder = ListMetadataFormatsRequest.builder()
        oaiService.listMetadataFormats(params.get('index', '_all') as String, listMetadataFormatsRequestBuilder.build())
    }

    @Override
    void shutdown() {
    }

}
