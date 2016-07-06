package org.xbib.webapp.extensions.oai

import groovy.transform.builder.Builder
import org.elasticsearch.index.query.QueryBuilder

import java.time.Instant

import static org.elasticsearch.index.query.QueryBuilders.boolQuery
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery

@Builder
class ListRecordsRequest {

    String path

    String stylesheet

    String defaultStylesheet

    String set

    String metadataPrefix

    Instant from

    Instant until

    ResumptionToken resumptionToken

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


}
