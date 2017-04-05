package org.xbib.malva.extensions.sru

import groovy.util.logging.Log4j2
import org.junit.Test
import org.xbib.content.settings.Settings
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkUtils
import org.xbib.malva.request.path.PathDecoder

import java.nio.charset.StandardCharsets

@Log4j2
class SRUTest {

    @Test
    void testSearchRetrieve() {

        // add net.hostname to system properties
        NetworkUtils.configureSystemProperties();

        WebappServer webappServer = new WebappServer()
        try {
            Settings settings = Settings.settingsBuilder()
                    .put('webapp.profile', 'zbn')
                    .put('webapp.home', 'src/main/webapps')
                    .put('webapp.networkclass', 'LOCAL')
                    .put('webapp.uri', 'http://${net.hostname}:9500')
                    .put('webapp.extension.sru.type', SRUExtension.class.getName())
                    .put('webapp.extension.elasticsearch.transport.enabled', 'true')
                    .put('webapp.extension.elasticsearch.transport.cluster', 'zbn')
                    .put('webapp.extension.elasticsearch.transport.host', 'zephyros:9300')
                    .build()
            webappServer.run(settings)
            //decoder.parse('index=hbzfix&version=2.0&operation=searchRetrieve&query=linux&recordSchema=mods&extraRequestData=holdings&facetLimit=10:dc.type')


            SRUExtension sru = webappServer.webappService.webapps().get('default').extensions().get('sru') as SRUExtension
            PathDecoder decoder = new PathDecoder('/sru/hbz/', StandardCharsets.UTF_8)
            decoder.parse('index=hbz&version=2.0&operation=searchRetrieve&query=linux&recordSchema=json&extraRequestData=holdings')
            Map<String,Object> result = sru.execute(decoder.path(), decoder.params(), true)
            log.info("sru={}", result)
        } catch (Throwable t) {
            log.error(t.getMessage() as String, t)
        } finally {
            webappServer.shutdown()
        }
    }
}
