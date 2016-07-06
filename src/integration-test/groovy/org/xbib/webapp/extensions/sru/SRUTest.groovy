package org.xbib.webapp.extensions.sru

import groovy.util.logging.Log4j2
import org.junit.Test
import org.xbib.common.settings.Settings
import org.xbib.webapp.bootstrap.WebappServer
import org.xbib.webapp.request.path.PathDecoder

import java.nio.charset.StandardCharsets

@Log4j2
class SRUTest {

    @Test
    void testSearchRetrieve() {
        WebappServer webappServer = new WebappServer()
        try {
            Settings settings = Settings.settingsBuilder()
                    .put('webapp.uri', 'http://127.0.0.1:9500')
                    .put('webapp.extension.sru.type', SRUExtension.class.getName())
                    .put('elasticsearch.transport.enabled', 'true')
                    .put('elasticsearch.transport.cluster', 'zbn')
                    .put('elasticsearch.transport.host', 'zephyros:9300')
                    .build()
            webappServer.run(settings)
            SRUExtension sru = webappServer.webappService().extensions().get('sru') as SRUExtension
            PathDecoder decoder = new PathDecoder('/_sru/', StandardCharsets.UTF_8)
            //decoder.parse('index=hbzfix&version=2.0&operation=searchRetrieve&query=linux&recordSchema=mods&extraRequestData=holdings&facetLimit=10:dc.type')
            decoder.parse('index=hbz&version=1.2&operation=searchRetrieve&query=linux&extraRequestData=holdings')
            Map<String,Object> result = sru.execute(decoder.path(), decoder.params(), true)
            log.info("sru={}", result)
        } catch (Throwable t) {
            log.error(t.getMessage() as String, t)
        } finally {
            webappServer.shutdown()
        }
    }
}
