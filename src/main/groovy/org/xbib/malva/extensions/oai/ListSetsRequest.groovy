package org.xbib.malva.extensions.oai

import groovy.transform.builder.Builder

@Builder
class ListSetsRequest {

    String path

    String stylesheet

    String defaultStylesheet

    ResumptionToken resumptionToken

}
