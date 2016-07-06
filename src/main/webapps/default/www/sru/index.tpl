responseType = sru.responseType(params)
version = sru.version(params)
namespace = sru.namespace(version)
schemaLocation = sru.schemaLocation(version)
responseHeader."Content-Type" = "${responseType}; charset=utf-8"
map = sru.execute(path, params, true)
xmlDeclaration()
'sru:searchRetrieveResponse'('xmlns:sru': namespace,
                             'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                             'xsi:schemaLocation': schemaLocation) {
  'sru:version' version
  'sru:numberOfRecords' map.total
  if (map.records) {
    'sru:records' {
      map.records.each { rec ->
        'sru:record' {
          'sru:recordSchema' "${rec.recordschema}"
          'sru:recordPacking' "${rec.recordpacking}"
          'sru:recordData' {
            yieldUnescaped "${rec.recorddata}"
          }
          'sru:recordIdentifier' sru.normalizeIndexName(rec.index) + "/${rec.type}/${rec.id}"
        }
      }
    }
    if (version == '2.0' && map.facets) {
      'sru:facetedResults'('xmlns:facet':'http://docs.oasis-open.org/ns/search-ws/facetedResults',
                           'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
                           'xsi:schemaLocation':'http://docs.oasis-open.org/ns/search-ws/facetedResults http://www.loc.gov/standards/sru/sru-2-0/schemas/facetedResults.xsd') {
        'facet:facets' {
          map.facets.each { facet ->
            'facet:facet' {
              'facet:index' "${facet.name}"
              'facet:terms' {
                facet.buckets.each { bucket ->
                  'facet:term' {
                    'facet:actualTerm' "${bucket.term}"
                    'facet:requestUrl' "${bucket.requestUrl}"
                    'facet:count' "${bucket.count}"
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}