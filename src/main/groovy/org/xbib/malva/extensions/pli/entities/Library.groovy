package org.xbib.malva.extensions.pli.entities
/**
 *
 */
class Library implements Comparable<Library> {

    Map<String, Object> map = [:]

    String isil

    Integer priority

    Region region

    List<String> groups = []

    Set<String> format = []

    Set<Service> interlibraryServices = new TreeSet<>(new Service.PriorityComparator())

    Set<Service> nonInterlibraryServices = new TreeSet(new Service.PriorityComparator())

    Service firstService

    static class Builder {
        Library library = new Library()
        Map<String, Integer> regionOrder

        Builder isil(String isil) {
            library.isil = isil
            this
        }

        Builder groups(List<String> groups) {
            library.groups = groups
            this
        }

        Builder priority(Integer priority) {
            library.priority = priority
            this
        }

        Builder format(String format) {
            library.format.add(format)
            this
        }

        Builder regionOrder(Map<String, Integer> regionOrder) {
            this.regionOrder = regionOrder
            this
        }

        Builder service(String id, Map<String, Object> map) {
            String regionStr = map.containsKey("region") ? map.get("region") : 'X'
            Integer regionPriority = regionOrder && regionOrder.containsKey(regionStr) ?
                    regionOrder.get(regionStr) : 0
            library.region = new Region(regionStr, regionPriority)
            map.librarygroups = library.groups
            map.librarypriority = library.priority
            Service service = new Service(map, id, regionPriority)
            if (service.isInterLibrary()) {
                library.interlibraryServices.add(service)
            } else {
                library.nonInterlibraryServices.add(service)
            }
            this
        }

        Library build() {
            library.activateInterlibraryServices(library.interlibraryServices)
            library.activateNonInterlibraryService(library.nonInterlibraryServices)
            if ("DE-ALL" == library.isil) {
                library.region = new Region("ALL", 0)
            }
            if (!library.region) {
                library.region = new Region("X", 0)
            }
            library
        }
    }

    boolean isCarrierTypeAllowed(String format) {
        this.format.isEmpty() || this.format.contains(format)
    }

    void activateInterlibraryServices(Set<Service> serviceList) {
        interlibraryServices = serviceList
        if (!interlibraryServices.isEmpty()) {
            map.put("interlibraryservice", interlibraryServices.collect { it.map })
        }
        this.firstService = !interlibraryServices.isEmpty() ? ++interlibraryServices.iterator() :
                !nonInterlibraryServices.isEmpty() ? ++nonInterlibraryServices.iterator() : Service.EMPTY
    }

    void activateNonInterlibraryService(Set<Service> serviceList) {
        nonInterlibraryServices = serviceList
        if (!nonInterlibraryServices.isEmpty()) {
            map.put("noninterlibraryservice", nonInterlibraryServices.collect { it.map })
        }
        this.firstService = !interlibraryServices.isEmpty() ? ++interlibraryServices.iterator() :
                !nonInterlibraryServices.isEmpty() ? ++nonInterlibraryServices.iterator() : Service.EMPTY
    }

    Library setMarker(String marker) {
        if (marker != null) {
            map.put(marker, true)
        }
        this
    }

    boolean getMarker(String marker) {
        map.containsKey(marker)
    }

    @Override
    int compareTo(Library o) {
        isil <=> o.isil
    }

    static class PriorityRandomComparator implements Comparator<Library> {

        @Override
        int compare(Library l1, Library l2) {
            l1.firstService.key <=> l2.firstService.key
        }
    }
}
