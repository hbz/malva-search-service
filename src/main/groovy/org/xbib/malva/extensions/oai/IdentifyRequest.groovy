package org.xbib.malva.extensions.oai

import groovy.transform.builder.Builder

@Builder
class IdentifyRequest {

    String path

    String stylesheet

    String defaultStylesheet

    void validate() {
        // do nothing
    }
}
