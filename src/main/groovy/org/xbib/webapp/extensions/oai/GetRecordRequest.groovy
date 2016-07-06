package org.xbib.webapp.extensions.oai

import groovy.transform.builder.Builder

@Builder
class GetRecordRequest {

    String path

    String stylesheet

    String defaultStylesheet

    String identifier

    String metadataPrefix

    String getIndex() {
        def (index, type, id) = identifier.split('/')
        index
    }

    String getType() {
        def (index, type, id) = identifier.split('/')
        type
    }

    String getId() {
        def (index, type, id) = identifier.split('/')
        id
    }

    void validate() {
        if (identifier == null) {
            throw new OAIException('badArgument')
        }
        if (metadataPrefix == null) {
            throw new OAIException('badArgument')
        }

    }
}
