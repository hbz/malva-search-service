package org.xbib.malva.extensions.pli.entities

import groovy.util.logging.Log4j2

import java.security.SecureRandom

/**
 *
 */
@Log4j2
class Service implements Comparable<Service> {

    static final Random random = new SecureRandom()

    static final Service EMPTY = new Service([:], '0', 9, 9)

    Map<String, Object> map

    String id

    Integer regionPriority

    Integer institutionPriority

    Integer servicePriority

    String priorityRandomKey

    Service(Map<String, Object> map, String id, Integer regionPriority, Integer institutionPriority) {
        this.map = map
        this.id = id
        this.regionPriority = regionPriority ? regionPriority : 9
        this.institutionPriority = institutionPriority ? institutionPriority : 9
        this.servicePriority = map.containsKey("priority") ? map.get("priority") as Integer : 9
        priorityRandomKey = "${this.regionPriority}${this.institutionPriority}${this.servicePriority}${random.nextInt(10)}"
    }

    boolean isInterLibrary() {
        map.containsKey("type") && map.get("type").toString().contains("interlibrary")
    }

    @Override
    int compareTo(Service o) {
        id <=> o.id
    }

}
