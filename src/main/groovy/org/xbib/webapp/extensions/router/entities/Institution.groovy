package org.xbib.webapp.extensions.router.entities

class Institution extends LinkedHashMap<String, Object> implements Comparable<Institution> {

    String isil

    Integer priority

    Map<String, Region> regions = [:]

    Set<String> carrier = []

    List<Service> services = []

    List<Service> otherServices = []

    Region firstRegion

    Service firstService

    private Institution() {
    }

    static class Builder {
        Map<String, Integer> regionPriorities
        Institution institution = new Institution()
        List<Service> services = []
        List<Service> otherServices = []

        Builder institution(Institution inst) {
            institution.isil = inst.isil
            institution.priority = inst.priority as Integer
            institution.regions = inst.regions as  Map<String, Region>
            institution.carrier = inst.carrier as Set<String>
            institution.services = inst.services as List<Service>
            institution.otherServices = inst.otherServices as List<Service>
            institution.firstRegion = inst.firstRegion as Region
            institution.firstService = inst.firstService as Service
            this
        }

        Builder regionPriorities(Map<String, Integer> regionPriorities) {
            this.regionPriorities = regionPriorities
            this
        }

        Builder isil(String isil) {
            institution.setISIL(isil)
            this
        }

        Builder carrier(String carrier) {
            institution.getCarrier().add(carrier)
            this
        }

        Builder service(Map<String, Object> map) {
            Service service = new Service(map, map.get('_id').toString(), institution.getPriority())
            String regionStr = service.get("region")
            if (regionStr == null) {
                regionStr = 'X'
            }
            Integer prio = regionPriorities != null && regionPriorities.containsKey(regionStr) ?
                    regionPriorities.get(regionStr) : 0
            Region region = new Region(regionStr, prio)
            institution.addRegion(region)
            service.setRegionPriority(prio)
            if (service.isForInterLibraryCopy()) {
                services.add(service)
            } else {
                otherServices.add(service)
            }
            this
        }

        Institution build() {
            institution.setServices(services)
            institution.setOtherServices(otherServices)
            if ("DE-ALL".equals(institution.getISIL())) {
                // free resource
                Region region = new Region("ALL", 0)
                institution.addRegion(region)
            }
            // set region "X" if region is still undefined
            if (institution.getRegion() == null) {
                Region region = new Region("X", 0)
                institution.addRegion(region)
            } else {
                institution.put("region", institution.getRegions().keySet())
            }
            institution
        }
    }

    Service firstService() {
        firstService
    }

    void addRegion(Region newRegion) {
        if (firstRegion == null) {
            firstRegion = newRegion
        }
        regions.put(newRegion.getName(), newRegion)
    }

    Region getRegion() {
        firstRegion
    }

    Map<String,Region> getRegions() {
        regions
    }

    boolean isCarrierAllowed(String carrier) {
        this.carrier.isEmpty() || this.carrier.contains(carrier)
    }

    void setServices(List<Service> serviceList) {
        if (serviceList == null) {
            throw new IllegalArgumentException("no null list allowed")
        }
        this.services = serviceList
        put("service", services);
        put("servicecount", services.size());
        // reset first service
        this.firstService = !getServices().isEmpty() ? getServices().iterator().next() :
                !getOtherServices().isEmpty() ? getOtherServices().iterator().next() :
                        new Service([:], 'unknown', 9)
    }

    List<Service> getServices() {
        services
    }

    void setOtherServices(List<Service> otherServiceList) {
        if (otherServiceList == null) {
            throw new IllegalArgumentException("no null list allowed")
        }
        this.otherServices = otherServiceList
        put("otherservice", otherServices);
        put("otherservicecount", otherServices.size());
        // reset first service
        this.firstService = !getServices().isEmpty() ? getServices().iterator().next() :
                !getOtherServices().isEmpty() ? getOtherServices().iterator().next() :
                        new Service([:], 'unknown', 9)
    }

    List<Service> getOtherServices() {
        otherServices
    }

    Set<String> getCarrier() {
        carrier
    }

    void setISIL(String isil) {
        this.isil = isil
        put("isil", isil)
    }

    String getISIL() {
        isil
    }

    Institution setMarker(String marker) {
        if (marker != null) {
            put(marker, true)
        }
        this
    }

    boolean getMarker(String marker) {
        containsKey(marker)
    }

    Integer getPriority() {
        containsKey("priority") ? priority : 9
    }

    @Override
    int compareTo(Institution o) {
        firstService().compareTo(o.firstService())
    }

    static class PriorityComparator implements Comparator<Institution> {

        @Override
        int compare(Institution o1, Institution o2) {
            String k1 = o1.firstService().getPriorityKey()
            String k2 = o2.firstService().getPriorityKey()
            k1.compareTo(k2)
        }
    }
}
