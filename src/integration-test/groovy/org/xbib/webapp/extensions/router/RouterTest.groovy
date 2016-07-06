package org.xbib.webapp.extensions.router

import groovy.util.logging.Log4j2
import org.junit.Test
import org.xbib.common.settings.Settings
import org.xbib.webapp.bootstrap.WebappServer
import org.xbib.webapp.request.path.PathDecoder

import java.nio.charset.StandardCharsets

@Log4j2
class RouterTest {

    @Test
    void testRouter() {
        WebappServer webappServer = new WebappServer()
        try {
            Settings settings = Settings.settingsBuilder()
                    .put('webapp.uri', 'http://127.0.0.1:9500')
                    .put('webapp.extension.router.type', RouterExtension.class.getName())
                    .put('elasticsearch.transport.enabled', 'true')
                    .put('elasticsearch.transport.cluster', 'zbn')
                    .put('elasticsearch.transport.host', 'zephyros:9300')
                    .putArray('library.router.regions.NRW.regions', ["NRW", "BAY", "BAW", "SAX", "NIE", "HAM", "SAA", "THU", "HES", "BER"] as String[])
                    .build()
            webappServer.run(settings)
            RouterExtension router = webappServer.webappService().extensions().get('router') as RouterExtension
            PathDecoder decoder = new PathDecoder('/_lp/', StandardCharsets.UTF_8)
            decoder.parse('base_region=NRW&id=1207143&year=2009')
            def params = decoder.params()
            def routerRequestBuilder = RouterRequest.builder()
                    .settings(settings.getAsSettings('library.router'))
                    .region(params.containsKey(RouterExtension.BASE_REGION) ? params.getString(RouterExtension.BASE_REGION) :'nrw')
                    .limit(params.getInteger(RouterExtension.LIMIT, 10))
                    .identifier(params.getString(RouterExtension.ID))
                    .year(params.getInteger(RouterExtension.YEAR, -1))
                    .baseInstitution(params.getString(RouterExtension.BASE_INSTITUTION))
                    .withCarrier(params.getAll(RouterExtension.WITH_CARRIER))
                    .withoutCarrier(params.getAll(RouterExtension.WITHOUT_CARRIER))
                    .withRegion(params.getAll(RouterExtension.WITH_REGION))
                    .withoutRegion(params.getAll(RouterExtension.WITHOUT_REGION))
                    .withInstitution(params.getAll(RouterExtension.WITH_INSTITUTION))
                    .withoutInstitution(params.getAll(RouterExtension.WITHOUT_INSTITUTION))
                    .withType(params.getAll(RouterExtension.WITH_TYPE))
                    .withoutType(params.getAll(RouterExtension.WITHOUT_TYPE))
                    .withMode(params.getAll(RouterExtension.WITH_MODE))
                    .withoutMode(params.getAll(RouterExtension.WITHOUT_MODE))
                    .withDistribution(params.getAll(RouterExtension.WITH_DISTRIBUTION))
                    .withoutDistribution(params.getAll(RouterExtension.WITHOUT_DISTRIBUTION))

            Map<String, Object> map = router.execute(routerRequestBuilder.build().validate())
            log.info("route={}", map)
        } catch (Throwable t) {
            log.error(t.getMessage() as String, t);
        } finally {
            webappServer.shutdown()
        }
    }
}


