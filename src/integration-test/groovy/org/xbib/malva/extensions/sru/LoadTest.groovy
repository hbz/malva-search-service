package org.xbib.malva.extensions.sru

import groovy.util.logging.Log4j2
import org.junit.Ignore
import org.junit.Test
import org.xbib.content.settings.Settings
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkUtils
import org.xbib.malva.request.path.PathDecoder
import org.xbib.malva.util.MultiMap

import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Log4j2
class LoadTest {

    @Ignore
    @Test
    void testLoad() {

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
            SRUExtension sru = webappServer.webappService.webapps().get('default').extensions().get('sru') as SRUExtension
            PathDecoder decoder = new PathDecoder('/sru/hbz/', StandardCharsets.UTF_8)
            decoder.parse('index=hbz&version=2.0&operation=searchRetrieve&query=linux&extraRequestData=holdings')

            final String path = decoder.path()
            final MultiMap map = decoder.params()

            ExecutorService executor = Executors.newFixedThreadPool(20)

            CountDownLatch latch = new CountDownLatch(1000)
            (1..1000).each {
                log.info("submitting ${it}")
                executor.submit(new Runnable() {
                    @Override
                    void run() {
                        try {
                            sru.execute(path, map, true)
                        } catch (Throwable t) {
                            log.error(t.getMessage() as String, t)
                        } finally {
                            latch.countDown()
                        }
                    }
                })
            }
            latch.await()
            executor.shutdown()
            log.info("done")
        } catch (Throwable t) {
            log.error(t.getMessage() as String, t)
        } finally {
            webappServer.shutdown()
        }
    }
}
