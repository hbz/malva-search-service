package org.xbib.malva.extensions.pli.entities

import groovy.util.logging.Log4j2

import java.security.SecureRandom

/**
 *
 */
@Log4j2
class Service implements Comparable<Service> {

    static final Random random = new SecureRandom()

    static final Service EMPTY = new Service([:], '0', 9)

    Map<String, Object> map

    String id

    Integer regionPriority

    Integer libraryPriority

    Integer servicePriority

    String key

    List<String> groups

    Service(Map<String, Object> map, String id, Integer regionPriority) {
        this.map = map
        this.id = id
        this.regionPriority = regionPriority ? regionPriority : 9
        this.libraryPriority = map.containsKey('librarypriority') ? map.get('librarypriority') as Integer : 9
        this.servicePriority = map.containsKey("priority") ? map.get("priority") as Integer : 9
        this.groups = map.containsKey('librarygroups') ? map.get('librarygroups') as List<String> : []
        this.key = "${this.regionPriority}${this.libraryPriority}${this.servicePriority}${random.nextInt(10)}"
    }

    boolean isInterLibrary() {
        map.containsKey("type") && map.get("type").toString().contains("interlibrary")
    }

    @Override
    int compareTo(Service o) {
        id <=> o.id
    }

    static class PriorityComparator implements Comparator<Service> {

        @Override
        int compare(Service s1, Service s2) {
            s1.key <=> s2.key
        }
    }
}
