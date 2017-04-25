package org.xbib.malva.extensions.pli

/**
 */
class PublicLibraryInitiativeResponse {

    Map<String, Object> meta = [:]

    Map<String, Object> interlibrary = [:]

    Map<String, Object> noninterlibrary = [:]

    Map<String, Object> asMap() {
        [
                meta: meta,
                interlibrary: interlibrary,
                noninterlibrary: noninterlibrary
        ].findAll { k, v -> v }
    }

    String toString() {
        asMap().toString()
    }
}
