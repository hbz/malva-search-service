package org.xbib.malva.extensions.oai

import groovy.transform.builder.Builder

@Builder
class ListMetadataFormatsRequest {

    String path

    String stylesheet

    String defaultStylesheet

    String identifier

}
