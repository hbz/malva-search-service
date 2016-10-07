package org.xbib.webapp.extensions.router

import groovy.transform.builder.Builder
import groovy.util.logging.Log4j2
import org.xbib.content.settings.Settings

@Builder
@Log4j2
class RouterRequest {

    Settings settings
    Settings regionSettings
    String index
    String manifestationsType
    String volumesType
    String holdingsType
    String shelfType
    String servicesType
    String identifier
    Integer year
    Integer limit
    Map<String, Integer> regionOrder
    String region
    String baseInstitution
    Collection<String> withRegion
    Collection<String> withoutRegion
    Collection<String> withInstitution
    Collection<String> withoutInstitution
    Collection<String> withCarrier
    Collection<String> withoutCarrier
    Collection<String> withType
    Collection<String> withoutType
    Collection<String> withMode
    Collection<String> withoutMode
    Collection<String> withDistribution
    Collection<String> withoutDistribution
    Map<String, String> institutionMarker
    Map<String, Collection<String>> carrierByInstitution = [:]

    RouterRequest validate() {
        this.index = settings.get("library.router.index.name", "ezdb")
        this.manifestationsType = settings.get("library.router.index.type.manifestations", "manifestations")
        this.volumesType = settings.get("library.router.index.type.volumes", "volumes")
        this.holdingsType = settings.get("library.router.index.type.holdings", "holdings")
        this.shelfType = settings.get("library.router.index.type.shelf", "shelf")
        this.servicesType = settings.get("library.router.index.type.services", "services")
        this.limit = settings.getAsInt("library.router.limit", 10)
        this.regionOrder = [:]
        this.regionSettings = settings.getAsSettings("library.router.regions")
        this.region = region
        if (region == null) {
            // default region order, use keySet order
            regionSettings.asMap.keySet().eachWithIndex { entry, i ->
                regionOrder.put(entry, i)
            }
            return this
        }
        Settings settings = regionSettings != null ? regionSettings.getAsSettings(region) : null
        if (settings != null) {
            settings.getAsArray('regions').eachWithIndex { entry, i ->
                regionOrder.put(entry, i)
            }
            (settings.getAsStructuredMap().get('servicerestrictions') as Map<String, Collection<String>>).each { k,v ->
                carrierByInstitution.put(k, v)
            }
            institutionMarker(RouterParameters.PRIORITY, settings.getAsArray('priority') as Collection<String>)
        }
        if (baseInstitution != null && !baseInstitution.isEmpty()) {
            this.baseInstitution = baseInstitution
            institutionMarker("baseInstitution", [baseInstitution])
        }
        this.identifier = identifier != null ? identifier.trim().toLowerCase().replaceAll("\\-", "") : null
        this.withRegion = !isEmpty(withRegion) ? new LinkedHashSet(withRegion) : null
        this.withoutRegion = !isEmpty(withoutRegion) ? new LinkedHashSet(withoutRegion) : null
        this.withInstitution = !isEmpty(withInstitution) ? new HashSet(withInstitution) : null
        this.withoutInstitution = !isEmpty(withoutInstitution) ? new HashSet(withoutInstitution) : null
        this.withCarrier = !isEmpty(withCarrier) ? new HashSet(withCarrier) : null
        this.withoutCarrier = !isEmpty(withoutCarrier) ? new HashSet(withoutCarrier) : null
        if (identifier != null && withCarrier != null) {
            carrierByInstitution.put(identifier, new HashSet(withCarrier))
        }
        if (carrierByInstitution != null) {
            for (Map.Entry<String, Collection<String>> entry : carrierByInstitution.entrySet()) {
                carrierByInstitution.put(entry.getKey(), entry.getValue())
            }
        }
        this.withType = !isEmpty(withType) ? new HashSet(withType) : null
        this.withoutType = !isEmpty(withoutType) ? new HashSet(withoutType) : null
        this.withMode = !isEmpty(withMode) ? new HashSet(withMode) : null
        this.withoutMode = !isEmpty(withoutMode) ? new HashSet(withoutMode) : null
        this.withDistribution = !isEmpty(withDistribution) ? new HashSet(withDistribution) : null
        this.withoutDistribution = !isEmpty(withoutDistribution) ? new HashSet(withoutDistribution) : null

        if (regionOrder.isEmpty()) {
            log.warn("no region_order found for {} in {}", region, settings.asStructuredMap)
        }

        this
    }


    void institutionMarker(String marker, Collection<String> institutions) {
        if (marker == null || marker.isEmpty()) {
            return
        }
        if (institutionMarker == null) {
            institutionMarker = [:]
        }
        if (!isEmpty(institutions)) {
            institutions.findAll { institution ->
                !institutionMarker.containsKey(institution)
            }.each { institution ->
                institutionMarker.put(institution, marker)
            }
        }
    }

    private static boolean isEmpty(Collection<String> collection) {
        collection == null || 
                collection.isEmpty() || 
                !collection.iterator().hasNext() || 
                collection.iterator().next() == null || 
                collection.iterator().next().isEmpty()
    }

    String toString() {
        "index=${index?:''} holdingsType=${holdingsType?:''} shelfType=${shelfType?:''} servicesType=${servicesType?:''} " +
                "identifier=${identifier?:''} year=${year?:''} " +
                "baseInstitution=${baseInstitution?:''} region=${region?:''} " +
                "regionOrder=${regionOrder?:''} withRegion=${withRegion?:''} withoutRegion=${withoutRegion?:''} " +
                "withInstitution=${withInstitution?:''} withoutInstitution=${withoutInstitution?:''} " +
                "withType=${withType?:''} withoutType=${withoutType?:''} " +
                "withMode=${withMode?:''} withoutMode=${withoutMode?:''} " +
                "withCarrier=${withCarrier?:''} withoutCarrier=${withoutCarrier?:''} " +
                "withDistribution=${withDistribution?:''} withoutDistribution=${withoutDistribution?:''} " +
                "institutionMarker=${institutionMarker?:''} " +
                "settings=${settings.getAsMap()}"
    }
}
