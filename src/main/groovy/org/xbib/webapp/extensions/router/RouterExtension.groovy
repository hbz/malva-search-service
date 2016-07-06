package org.xbib.webapp.extensions.router

import groovy.util.logging.Log4j2
import org.elasticsearch.client.ElasticsearchClient
import org.xbib.common.Strings
import org.xbib.common.settings.Settings
import org.xbib.common.xcontent.xml.XmlXContent
import org.xbib.webapp.Webapp
import org.xbib.webapp.WebappBinding
import org.xbib.webapp.WebappExtension
import org.xbib.webapp.MultiMap

import static org.xbib.common.xcontent.XContentService.jsonBuilder

@Log4j2
class RouterExtension implements WebappExtension, RouterParameters {

    Settings settings

    ElasticsearchClient client

    RouterService routerService

    @Override
    void webapp(Webapp webapp) {
        this.settings = webapp.settings()
        this.client = webapp.webappService().elasticsearchService().client()
        this.routerService = new RouterService()
        log.info('Router extension started')
    }

    @Override
    void prepareBinding(WebappBinding binding) {
    }

    @Override
    String name() {
        'router'
    }

    @Override
    RouterExtension object() {
        this
    }

    Map<String,Object> execute(MultiMap params) {
        execute(settings, params)
    }

    Map<String,Object> execute(Settings settings, MultiMap params) {
        def routerRequestBuilder = RouterRequest.builder()
                .settings(settings)
                .region(params.getString(BASE_REGION, 'NRW'))
                .limit(params.getInteger(LIMIT, -1))
                .identifier(params.getString(ID))
                .baseInstitution(params.getString(BASE_INSTITUTION))
                .withCarrier(params.getAll(WITH_CARRIER))
                .withoutCarrier(params.getAll(WITHOUT_CARRIER))
                .withRegion(params.getAll(WITH_REGION))
                .withoutRegion(params.getAll(WITHOUT_REGION))
                .withInstitution(params.getAll(WITH_INSTITUTION))
                .withoutInstitution(params.getAll(WITHOUT_INSTITUTION))
                .withType(params.getAll(WITH_TYPE))
                .withoutType(params.getAll(WITHOUT_TYPE))
                .withMode(params.getAll(WITH_MODE))
                .withoutMode(params.getAll(WITHOUT_MODE))
                .withDistribution(params.getAll(WITH_DISTRIBUTION))
                .withoutDistribution(params.getAll(WITHOUT_DISTRIBUTION))
        if (params.containsKey(YEAR) && !Strings.isEmpty(params.get(YEAR) as String)) {
            routerRequestBuilder.year(params.getInteger(YEAR, -1))
        }
        routerService.execute(client, routerRequestBuilder.build().validate())
    }

    Map<String,Object> execute(RouterRequest routerRequest) {
        routerService.execute(client, routerRequest)
    }

    Map<String,Object> getManifestation(String id) {
        routerService.getManifestation(client,
                settings.get("library.router.index.name", "ezdb"),
                settings.get("library.router.index.type.manifestations", "manifestations"),
                id)
    }

    Map<String,Object> getService(String id) {
        routerService.getService(client,
                settings.get("library.router.index.name", "ezdb"),
                settings.get("library.router.index.type.services", "services"),
                id)
    }

    String asJson(Map<String,Object> map) {
        jsonBuilder().map(map).string()
    }

    String asXml(Map<String,Object> map) {
        XmlXContent.contentBuilder().map(map).string()
    }

    String formatGroup(Map group) {
        if (group.enddate) {
            if (group.endvolume) {
                if (group.beginvolume) {
                    return "${group.beginvolume}.${group.begindate} - ${group.endvolume}.${group.enddate}"
                } else {
                    return "${group.begindate} - ${group.endvolume}.${group.enddate}"
                }
            } else {
                if (group.beginvolume) {
                    return "${group.beginvolume}.${group.begindate} - ${group.enddate}"
                } else {
                    return "${group.begindate} - ${group.enddate}"
                }
            }
        } else {
            if (group.open) {
                if (group.beginvolume) {
                    return "${group.beginvolume}.${group.begindate} -"
                } else {
                    return "${group.begindate} -"
                }
            } else {
                if (group.beginvolume) {
                    return "${group.beginvolume}.${group.begindate}"
                } else {
                    return "${group.begindate}"
                }
            }
        }
    }

    @Override
    void shutdown() {
    }

}
