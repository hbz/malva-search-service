package org.xbib.webapp.extensions.router.entities

class Service extends LinkedHashMap<String, Object> implements Comparable<Service> {

    private final String id

    private Integer servicePriority

    private Integer institutionPriority

    private Integer regionPriority

    Service(Map<String, Object> map, String id, Integer institutionPriority) {
        super(map)
        this.id = id
        this.servicePriority = (Integer) get("priority")
        if (servicePriority == null) {
            this.servicePriority = 9
        }
        this.institutionPriority = institutionPriority
        if (institutionPriority == null) {
            this.institutionPriority = 9
        }
        regionPriority = 9
    }

    String getId() {
        id
    }

    Integer getPriority() {
        servicePriority
    }

    void setRegionPriority(Integer regionPriority) {
        this.regionPriority = regionPriority
    }

    Integer getRegionPriority() {
        regionPriority
    }

    Integer getInstitutionPriority() {
        institutionPriority
    }

    boolean isForInterLibraryCopy() {
        String type = containsKey("type") ? get("type").toString() : "";
        String mode = containsKey("mode") ? get("mode").toString() : "";
        type.contains("interlibrary") && mode.contains("copy")
    }

    String getPriorityKey() {
        "" + getRegionPriority() + getInstitutionPriority()  + getPriority() + getId()
    }

    @Override
    int compareTo(Service o) {
        getPriorityKey().compareTo(o.getPriorityKey())
    }

}
