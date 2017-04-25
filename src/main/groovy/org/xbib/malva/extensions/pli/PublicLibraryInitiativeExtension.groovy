package org.xbib.malva.extensions.pli

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import groovy.util.logging.Log4j2
import org.xbib.content.XContentBuilder
import org.xbib.content.json.JsonXContent
import org.xbib.content.settings.Settings
import org.xbib.content.xml.XmlXContent
import org.xbib.malva.MalvaBinding
import org.xbib.malva.MalvaExtension
import org.xbib.malva.Webapp
import org.xbib.malva.extensions.pli.services.PublicLibraryInitiativeAvailService
import org.xbib.malva.util.MultiMap

/**
 *
 */
@Log4j2
class PublicLibraryInitiativeExtension implements MalvaExtension, PublicLibraryInitiativeParameters {

    String name

    Settings settings

    Webapp webapp

    @Inject
    PublicLibraryInitiativeExtension(@Assisted String name,
                                     @Assisted Settings settings,
                                     @Assisted Webapp webapp) {
        this.name = name
        this.settings = settings
        this.webapp = webapp
        log.info('{} {} started for web app {}', name, getClass().getName(), webapp.name())
    }

    @Override
    void prepareBinding(MalvaBinding binding) {
    }

    @Override
    String name() {
        name
    }

    @Override
    PublicLibraryInitiativeExtension object() {
        this
    }

    @Override
    void shutdown() {
    }

    PublicLibraryInitiativeResponse execute(MultiMap params) {
        log.debug("params={}", params)
        String baseService = params.getString(BASE_SERVICE)
        switch (baseService) {
            case "avail-v1":
                String baseRegion = params.getString (BASE_REGION)
                String baseLibrary = params.getString (BASE_LIBRARY)
                String id = params.getString (ID)
                String issn = params.getString (ISSN)
                Integer year = params.getInteger (YEAR, null)
                Collection < String > region = params.containsKey (REGION) ? params.getAll (REGION): null
                Collection < String > library = params.containsKey (LIBRARY) ? params.getAll (LIBRARY): null
                Collection < String > carriertype = params.containsKey (CARRIERTYPE) ? params.getAll (CARRIERTYPE): null
                Collection < String > type = params.containsKey (TYPE) ? params.getAll (TYPE): null
                Collection < String > mode = params.containsKey (MODE) ? params.getAll (MODE): null
                Collection < String > distribution = params.containsKey (DISTRIBUTION) ? params.getAll (DISTRIBUTION): null
                PublicLibraryInitiativeRequest request = PublicLibraryInitiativeRequest.builder ()
                        .baseService (baseService)
                        .baseRegion (baseRegion)
                        .baseLibrary (baseLibrary)
                        .id (id)
                        .issn (issn)
                        .year (year)
                        .region (region)
                        .library (library)
                        .carriertype (carriertype)
                        .type (type)
                        .mode (mode)
                        .distribution (distribution)
                        .build ()
                log.info("executing avail with request ${request}")
                return avail(request)
                break
            default:
                break
        }
        null
    }

    PublicLibraryInitiativeResponse avail(PublicLibraryInitiativeRequest libraryItemRequest) {
        PublicLibraryInitiativeAvailService availService = new PublicLibraryInitiativeAvailService(settings, webapp)
        try {
            availService.avail(libraryItemRequest)
        } catch (Exception e) {
            log.error(e.getMessage() as String, e)
            PublicLibraryInitiativeResponse errorResponse = new PublicLibraryInitiativeResponse()
            errorResponse.meta.error = e.getMessage() as String
            errorResponse
        }
    }

    Map<String,Object> getManifestation(String id) {
        PublicLibraryInitiativeAvailService availService = new PublicLibraryInitiativeAvailService(settings, webapp)
        availService.getDoc(settings.get("manifestations.index", "efl"),
                settings.get("manifestations.type", "manifestations"),
                id)
    }

    Map<String,Object> getPart(String id) {
        PublicLibraryInitiativeAvailService availService = new PublicLibraryInitiativeAvailService(settings, webapp)
        availService.getDoc(settings.get("parts.index", "efl"),
                settings.get("parts.type", "parts"),
                id)
    }

    Map<String,Object> getService(String id) {
        PublicLibraryInitiativeAvailService availService = new PublicLibraryInitiativeAvailService(settings, webapp)
        availService.getDoc(settings.get("services.index", "efl"),
                settings.get("services.type", "services"),
                id)
    }

    String asJson(PublicLibraryInitiativeResponse libraryItemResponse) {
        asXContent(JsonXContent.contentBuilder().prettyPrint(), libraryItemResponse)
    }

    String asXml(PublicLibraryInitiativeResponse libraryItemResponse) {
        asXContent(XmlXContent.contentBuilder().prettyPrint(), libraryItemResponse)
    }

    static String asXContent(XContentBuilder builder, PublicLibraryInitiativeResponse response) {
        builder.startObject()
                .field("meta").map(response.meta)
        if (!response.interlibrary.isEmpty()) {
            builder.field("interlibrary").map(response.interlibrary)
        }
        if (!response.noninterlibrary.isEmpty()) {
            builder.field("noninterlibrary").map(response.noninterlibrary)
        }
        builder.endObject()
                .string()
    }

    String formatGroup(List<Map> groups) {
        groups.collect { formatGroup(it) }.join(',')
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
}
