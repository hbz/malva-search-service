package org.xbib.malva.extensions.pli

import groovy.transform.builder.Builder

@Builder
class PublicLibraryInitiativeRequest {

    String baseService

    String baseRegion

    String baseLibrary

    String id

    String issn

    Integer year

    String volumeissue

    Collection<String> region

    Collection<String> library

    Collection<String> carriertype

    Collection<String> type

    Collection<String> mode

    Collection<String> distribution

    Map<String, Object> asMap() {
        [
                baseService: baseService,
                baseRegion: baseRegion,
                baseLibrary: baseLibrary,
                id: id,
                issn: issn,
                year: year,
                volumeissue: volumeissue,
                region: region,
                library: library,
                carriertype: carriertype,
                type: type,
                mode: mode,
                distribution: distribution
        ].findAll { k, v -> v }
    }

    String toString() {
        asMap().toString()
    }
}
