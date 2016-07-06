package org.xbib.webapp.extensions.router.entities

class Region {

    String name

    Integer priority

    Region(String name, Integer priority) {
        this.name = name
        this.priority = priority
    }

    String getName() {
        name
    }

    Integer getPriority() {
        priority
    }
}
