package org.xbib.malva.extensions.pli

/**
 */
class PublicLibraryInitiativeResponse {

    Map<String, Object> meta = [:]

    Map<String, Object> docs = [:]

    Map<String, Object> interlibrary = [:]

    Map<String, Object> moreinterlibrary = [:]

    Map<String, Object> noninterlibrary = [:]

    Map<String, Object> asMap() {
        [
                meta: meta,
                docs: docs,
                interlibrary: interlibrary,
                moreinterlibrary: moreinterlibrary,
                noninterlibrary: noninterlibrary
        ].findAll { k, v -> v }
    }

    String toString() {
        asMap().toString()
    }
}
