package org.xbib.webapp.extensions.router

import groovy.util.logging.Log4j2
import org.elasticsearch.action.get.GetAction
import org.elasticsearch.action.get.GetRequestBuilder
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.ElasticsearchClient
import org.xbib.webapp.extensions.router.entities.Institution
import org.xbib.webapp.extensions.router.entities.Service

/**
 * Algorithm for routing loan/copy requests to libraries.
 * Finds the right order of supplying libraries for intermediating the request.
 */
@Log4j2
class RouterService  {

    Map<String, Object> execute(ElasticsearchClient client, RouterRequest request) throws IOException {
        if (request == null) {
            throw new IOException("no request given")
        }
        Map<String, Object> response = [:]
        // ZDB-ID?
        if (request.identifier == null) {
            return response
        }
        List<Map<String, Object>> results = []
        if (request.year != null && request.year > 0) {
            Map<String, Object> result = [:]
            String id = request.getIdentifier()
            GetRequestBuilder getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(request.index)
                    .setType(request.manifestationsType)
                    .setId(id)
            GetResponse getResponse = getRequestBuilder.execute().actionGet(5000L)
            if (getResponse.isExists()) {
                result.putAll(getResponse.getSourceAsMap())
            }
            id = request.identifier + "." + request.year
            getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(request.index)
                    .setType(request.shelfType)
                    .setId(id)
            getResponse = getRequestBuilder.execute().actionGet(5000L)
            if (getResponse.isExists()) {
                Map<String, Object> source = getResponse.getSourceAsMap()
                Map<String, Object> m = filter(request,
                        toInstitutions(request, expandServices(client, request, source.get("institution") as List)))
                m.putAll(source)
                result.putAll(m)
            }
            results.add(result)
        } else {
            Map<String, Object> result = [:]
            String id = request.identifier
            GetRequestBuilder getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(request.index)
                    .setType(request.manifestationsType)
                    .setId(id)
            GetResponse getResponse = getRequestBuilder.execute().actionGet(5000L)
            if (getResponse.isExists()) {
                result.putAll(getResponse.getSourceAsMap())
            }
            getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(request.index)
                    .setType(request.holdingsType)
                    .setId(id)
            getResponse = getRequestBuilder.execute().actionGet(5000L)
            if (getResponse.isExists()) {
                Map<String, Object> source = getResponse.getSourceAsMap()
                Map<String, Object> m = filter(request,
                        toInstitutions(request, expandServices(client, request, source.get("institution") as List)))
                m.putAll(source)
                result.putAll(m)
            }
            results.add(result)
        }
        response.put("results", results)
        response
    }

    private static Map<String, Object> filter(RouterRequest request, List<Institution> instlist) {
        List<Institution> institutions = instlist.findAll { inst ->
            includeInstitution(request, inst) &&
            excludeInstitution(request, inst)
        }
        institutions = institutions.findAll { inst ->
            int numberOfServicesBeforeFilter = inst.getServices().size()
            List<Service> services = inst.getServices().findAll { service ->
                includeCarrier(request, inst, service) &&
                excludeCarrier(request, inst, service) &&
                includeRegion(request, service) &&
                excludeRegion(request, service) &&
                includeInstitution(request, service) &&
                excludeInstitution(request, service) &&
                includeType(request, service) &&
                excludeType(request, service) &&
                includeMode(request, service) &&
                excludeMode(request, service) &&
                includeDistribution(request, service) &&
                excludeDistribution(request, service)
            }
            // remove institution if their services were all removed
            if (numberOfServicesBeforeFilter > 0 && services.isEmpty()) {
                return false
            }
            Collections.sort(services)
            // update new services
            inst.setServices(services)
            // find all filtered services by evaluating the difference set
            Set<Service> diff = new HashSet<>()
            diff.addAll(inst.getServices())
            diff.removeAll(services)
            List<Service> other = inst.getOtherServices()
            if (!diff.isEmpty()) {
                other.addAll(diff)
            }
            if (!other.isEmpty()) {
                Collections.sort(other)
                inst.setOtherServices(other)
            }
            true
        }
        Collections.shuffle(institutions) // a bit of random here, we will sort later
        Map<String, Object> result = [:]
        List<Institution> insts = []
        boolean hasBase
        // group by region?
        if (isEmpty(request.region)) {
            // do not group by region
            List<Institution> instList = institutions.collect { inst ->
                inst.setMarker(getMarker(request, inst))
            }
            hasBase = request.baseInstitution &&
                    instList.find { inst -> inst.getISIL().equals(request.baseInstitution) }
            Map<Boolean, List<Institution>> priorities = institutions.groupBy { inst ->
                inst.getMarker("priority")
            }
            if (priorities.containsKey(true)) {
                List<Institution> l1 = priorities.remove(true)
                Collections.sort(l1, new Institution.PriorityComparator())
                List<Institution> l2 = priorities.remove(false)
                if (l2 == null) {
                    l2 = []
                }
                insts.addAll(l1)
                insts.addAll(l2) // hmmm
            } else if (priorities.containsKey(false)) {
                List<Institution> l1 = priorities.remove(false)
                Collections.sort(l1, new Institution.PriorityComparator())
                insts.addAll(l1)
            }
        } else {
            // group by region
            List<Institution> instList = institutions.collect { inst ->
                inst.setMarker(getMarker(request, inst))
            }
            Map<String, List<Institution>> regions = instList.groupBy { inst ->
                inst.getRegion().getName()
            }
            // now delete regions that are not configured in region_order request parameter
            regions = regions.findAll { k, v ->
                request.regionOrder.containsKey(k)
            }
            String baseRegion = request.region
            // find our region
            List<Institution> ourregion = regions.containsKey(baseRegion) ? regions.remove(baseRegion) : []
            // find our institution (the base institution)
            hasBase = request.baseInstitution != null &&
                    instList.find { inst -> inst.getISIL().equals(request.baseInstitution) }
            // priority/non-priority insts
            Map<Boolean, List<Institution>> priorities = ourregion.groupBy { inst ->
                inst.getMarker("priority")
            }
            if (priorities.containsKey(true)) {
                List<Institution> l1 = priorities.remove(true)
                Collections.sort(l1, new Institution.PriorityComparator())
                List<Institution> l2 = priorities.remove(false)
                if (l2 == null) {
                    l2 = []
                }
                insts.addAll(l1)
                insts.addAll(l2)
            } else if (priorities.containsKey(false)) {
                List<Institution> l1 = priorities.remove(false)
                Collections.sort(l1, new Institution.PriorityComparator())
                insts.addAll(l1)
            }
        }
        result.put("hasbase", hasBase)
        result.put("institutions", insts)
        result.put("count", insts.size())
        result
    }

    private static List<Institution> toInstitutions(RouterRequest request, Map<String, Object> map)
            throws IOException {
        List<Institution> institutions = []
        map.each { isil, servicesResult ->
            Institution.Builder builder = new Institution.Builder()
            builder.isil(isil)
            if (request.carrierByInstitution) {
                request.carrierByInstitution.get(isil)?.each { carrier ->
                    builder.carrier(carrier)
                }
            }
            builder.regionPriorities(request.regionOrder)
            servicesResult.each { service ->
                builder.service(service as Map<String, Object>)
            }
            institutions.add(builder.build())
        }
        institutions
    }

    private static Map<String,Object> expandServices(ElasticsearchClient client, RouterRequest request, List<Map<String,Object>> maps) {
        Map<String,Object> isils = [:]
        maps.each { map ->
            String isil = map.get("isil")
            List<String> serviceIds = (List<String>) map.get("service")
            List<Map<String,Object>> servicesResult = []
            serviceIds.each { serviceId ->
                String serviceIdStr = serviceId.toString()
                GetRequestBuilder getRequest = new GetRequestBuilder(client, GetAction.INSTANCE)
                        .setIndex(request.index)
                        .setType(request.servicesType)
                        .setId(serviceIdStr)
                GetResponse getResponse = getRequest.execute().actionGet(5000L)
                if (getResponse.exists) {
                    Map<String,Object> m = [:]
                    m.putAll(getResponse.getSourceAsMap())
                    m.put("_id", serviceIdStr)
                    servicesResult.add(m)
                }
            }
            isils.put(isil, servicesResult)
        }
        isils
    }

    static Map<String, Object> getManifestation(ElasticsearchClient client, String index, String type, String id) throws IOException {
        GetRequestBuilder getRequest = new GetRequestBuilder(client, GetAction.INSTANCE)
                .setIndex(index).setType(type).setId(id)
        GetResponse getResponse = getRequest.execute().actionGet(5000L)
        getResponse.isExists() ? getResponse.getSourceAsMap() : [:]
    }

    static Map<String, Object> getService(ElasticsearchClient client, String index, String type, String id) throws IOException {
        GetRequestBuilder getRequest = new GetRequestBuilder(client, GetAction.INSTANCE)
                .setIndex(index).setType(type).setId(id)
        GetResponse getResponse = getRequest.execute().actionGet(5000L)
        getResponse.isExists() ? getResponse.getSourceAsMap() : [:]
    }

    private static boolean includeCarrier(RouterRequest request, Institution institution, Service service) {
        String carriertype = (String) service.get("carriertype")
        Set<String> set = request.withCarrier
        institution.isCarrierAllowed(carriertype) &&
                (set == null || (carriertype != null && set.contains(carriertype)))
    }

    private static boolean excludeCarrier(RouterRequest request, Institution institution, Service service) {
        String carriertype = (String) service.get("carriertype")
        Set<String> set = request.withoutCarrier
        institution.isCarrierAllowed(carriertype) &&
                (set == null || (carriertype != null && !set.contains(carriertype)))
    }


    private static boolean includeRegion(RouterRequest request, Service service) {
        request.withRegion == null ||
                (service.containsKey("region") && request.withRegion.contains(service.get("region")))
    }

    private static boolean excludeRegion(RouterRequest request, Service service) {
        request.withoutRegion == null ||
                (service.containsKey("region") && !request.withoutRegion.contains(service.get("region")))
    }

    private static boolean includeInstitution(RouterRequest request, Service service) {
        request.withInstitution == null ||
                (service.containsKey("isil") && request.withInstitution.contains(service.get("isil")))
    }

    private static boolean excludeInstitution(RouterRequest request, Service service) {
        String isil = (String) service.get("isil")
        request.withoutInstitution == null ||
                (service.containsKey("isil") && !request.withoutInstitution.contains(isil))
    }

    private static boolean includeInstitution(RouterRequest request, Institution institution) {
        String isil = institution.getISIL()
        request.withInstitution == null || request.withInstitution.contains(isil)
    }

    private static boolean excludeInstitution(RouterRequest request, Institution institution) {
        String isil = institution.getISIL()
        request.withoutInstitution == null || !request.withoutInstitution.contains(isil)
    }

    private static boolean includeType(RouterRequest request, Service service) {
        String type = (String) service.get("type")
        request.withType == null ||
                (service.containsKey("type") && request.withType.contains(type))
    }

    private static boolean excludeType(RouterRequest request, Service service) {
        String type = (String) service.get("type")
        request.withoutType == null ||
                (service.containsKey("type") && !request.withoutType.contains(type))
    }

    private static boolean includeMode(RouterRequest request, Service service) {
        checkValueInSet(service.get("mode"), request.withMode)
    }

    private static boolean excludeMode(RouterRequest request, Service service) {
        checkValueNotInSet(service.get("mode"), request.withoutMode)
    }

    private static boolean includeDistribution(RouterRequest request, Service service) {
        checkValueInSet(service.get("distribution"), request.withDistribution)
    }

    private static boolean excludeDistribution(RouterRequest request, Service service) {
        checkValueNotInSet(service.get("distribution"), request.withoutDistribution)
    }

    private static String getMarker(RouterRequest request, Institution institution) {
        request.institutionMarker != null ?
                request.institutionMarker.get(institution.getISIL()) : null
    }

    private static boolean isEmpty(String s) {
        s == null || s.isEmpty()
    }

    private static boolean checkValueInSet(Object o, Collection<String> collection) {
        if (o == null || collection == null) {
            return true
        }
        if (!(o instanceof Collection)) {
            o = Collections.singleton(o)
        }
        for (Object value : (Collection) o) {
            if (collection.contains(value.toString())) {
                return true
            }
        }
        return false
    }

    private static boolean checkValueNotInSet(Object o, Collection<String> collection) {
        if (o == null || collection == null) {
            return true
        }
        if (!(o instanceof Collection)) {
            o = Collections.singleton(o)
        }
        boolean b = true
        for (Object value : (Collection) o) {
            b = b && !collection.contains(value.toString())
        }
        return b
    }
}
