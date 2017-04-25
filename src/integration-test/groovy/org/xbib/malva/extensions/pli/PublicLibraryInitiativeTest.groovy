package org.xbib.malva.extensions.pli

import groovy.util.logging.Log4j2
import org.junit.Test
import org.xbib.malva.Request
import org.xbib.malva.Response
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkService
import org.xbib.malva.network.NetworkUtils

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 */
@Log4j2
class PublicLibraryInitiativeTest {

    @Test
    void test() {
        NetworkUtils.configureSystemProperties()
        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(getClass().getResourceAsStream('/test-config.json'))
            URI uri = webappServer.networkService.getURIs().get(0)
            NetworkService networkService = webappServer.networkService
            Request request = networkService.newRequest()
            request.path = "/pli/avail-v1/*/*"
            request.params = [
                    id: '21142889', //'9568360', // //'HT003289261' // '21142889', //1845482',  //
                    //issn: '1743-9329',
                    //id: '2273997x',
                    year: 2016,
                    region: '',
                    library: '',
                    mode: 'copy'
                    //library: ["DE-38M", "DE-61", "DE-38", "DE-465", "DE-464"],
                    //type: [ 'interlibrary' ],
                    //mode: [ 'copy' ],
                    //carriertype: [ 'volume' ]
                    //region: 'NRW'
            ]
            Response response = networkService.newResponse()
            networkService.dispatch(uri, 'default', request, response)
            log.info('response={}', response.bytesStreamOutput.bytes().toUtf8())
            assertEquals(200, response.responseStatus.statusCode)
            assertTrue(response.headers.'content-length' > 0)
        } finally {
            webappServer.shutdown()
        }
    }
}
