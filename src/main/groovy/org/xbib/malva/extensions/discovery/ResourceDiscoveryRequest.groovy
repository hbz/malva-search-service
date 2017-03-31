package org.xbib.malva.extensions.discovery

import groovy.transform.builder.Builder
import org.xbib.content.XContentBuilder
import org.xbib.cql.CQLParser
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator

/**
 */
@Builder
class ResourceDiscoveryRequest {

    String query

    String filter

    Map<String, Collection<String>> breadcrumbs

    Integer from

    Integer size

    XContentBuilder sort

    String facetLimit

    String facetSort

    String generatedQuery

    ResourceDiscoveryRequest validate() {
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator()
        if (from != null) {
            generator.setFrom(from)
        }
        if (size != null) {
            generator.setSize(size)
        }
        this.generatedQuery = generateQuery(generator)
        this
    }

    private String generateQuery(ElasticsearchQueryGenerator generator) {
        if (!query || query.trim().length() == 0 || query.equals(".")) {
            return  "{\"query\":{\"match_all\":{}}}"
        }
        if (sort != null) {
            generator.setSort(sort)
        }
        if (filter != null) {
            generator.filter(filter)
        }
        if (breadcrumbs != null) {
            for (Map.Entry<String, Collection<String>> me : breadcrumbs.entrySet()) {
                String key = me.getKey()
                if (key.startsWith("or.")) {
                    generator.orfilter(key.substring(3), me.getValue())
                }
                if (key.startsWith("and.")) {
                    generator.andfilter(key.substring(4), me.getValue())
                }
            }
        }
        if (facetLimit != null) {
            generator.facet(facetLimit, facetSort)
        }
        CQLParser parser = new CQLParser(query)
        parser.parse()
        parser.getCQLQuery().accept(generator)
        return generator.getSourceResult()
    }
}
