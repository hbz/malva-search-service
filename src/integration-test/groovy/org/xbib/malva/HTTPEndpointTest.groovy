package org.xbib.malva

import groovy.util.logging.Log4j2
import org.elasticsearch.common.io.Streams
import org.junit.Test
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkService
import org.xbib.malva.network.NetworkUtils

import java.nio.charset.StandardCharsets

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 */
@Log4j2
class HTTPEndpointTest {

    @Test
    void testEndpointWithHttpURLConnection() {
        NetworkUtils.configureSystemProperties()
        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(getClass().getResourceAsStream('/test-config.json'))
            URI uri = webappServer.networkService.getURIs().get(0)
            URL base = uri.toURL()
            URL url = new URL(base, "/sru/de-468-introx-/?version=2.0&operation=searchRetrieve&recordSchema=json&extraRequestData=holdings&query=%28.%29+sortby+dc.date%2Fsort.descending&facetLimit=21%3Adc.type%2C21%3Adc.format%2C21%3Aintrox.subject%2C21%3Adc.language%2C100%3Acollection&startRecord=1&maximumRecords=20")
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")
            connection.setDoInput(true)
            StringWriter response = new StringWriter()
            Streams.copy(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8), response)
            log.info(response)
            assertEquals(200, connection.getResponseCode())
        } finally {
            webappServer.shutdown()
        }
    }

    @Test
    void testEndpointWithRequestResponse() {
        NetworkUtils.configureSystemProperties()
        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(getClass().getResourceAsStream('/test-config.json'))
            URI uri = webappServer.networkService.getURIs().get(0)
            NetworkService networkService = webappServer.networkService
            Request request = networkService.newRequest()
            request.path = "/sru/de-468-introx-/"
            request.params = [ version: '2.0',
                               operation: 'searchRetrieve',
                               recordSchema: 'json',
                               extraRequestData: 'holdings',
                               query: '(.) sortby dc.date/sort.descending',
                               filter: 'introx.subject=MeSH',
                               facetLimit: '21:dc.type,21:dc.format,21:introx.subject,21:dc.language,1:collection',
                               startRecord: 1,
                               maximumRecords: 20
            ]
            Response response = networkService.newResponse()
            networkService.dispatch(uri, 'default', request, response)
            log.info('response={}', response.bytesStreamOutput.bytes().toUtf8())
            assertEquals(200, response.responseStatus.statusCode)
            assertTrue(response.headers.'content-length' > 0)
            log.info(response)
        } finally {
            webappServer.shutdown()
        }
    }
}
