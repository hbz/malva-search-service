package org.xbib.webapp.extensions.oai

import groovy.transform.builder.Builder
import org.elasticsearch.index.query.QueryBuilder

import java.time.Instant

import static org.elasticsearch.index.query.QueryBuilders.*

@Builder
class ListIdentifiersRequest  {

    String path

    String stylesheet

    String defaultStylesheet

    ResumptionToken resumptionToken

    String set

    String metadataPrefix

    Instant from

    Instant until

    QueryBuilder elasticsearchQuery

    QueryBuilder getElasticsearchQuery() {
        if (elasticsearchQuery == null) {
            this.elasticsearchQuery = createElasticsearchQuery()
        }
        elasticsearchQuery
    }

    private QueryBuilder createElasticsearchQuery() {
        QueryBuilder queryBuilder = boolQuery().must(matchAllQuery())
        if (from != null) {
            queryBuilder.filter(rangeQuery('_timestamp').from(from.toString()))
        }
        if (until != null) {
            queryBuilder.filter(rangeQuery('_timestamp').to(until.toString()))
        }
        queryBuilder
    }

    void validate() {
        if (metadataPrefix == null) {
            throw new OAIException('badArgument')
        }
    }

}
