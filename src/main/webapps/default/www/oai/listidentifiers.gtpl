if (map.records) {
  'oai:ListIdentifiers' {
    map.records.each { rec ->
      'oai:header' {
         'oai:identifier' oai.normalizeIndexName(rec.index) + "/${rec.type}/${rec.id}"
         'oai:datestamp' rec.timestamp
      }
    }
    if (map.resumptiontoken) {
     'oai:resumptionToken'(expirationDate: map.resumptiontoken.expirationDate,
        completeListSize: map.resumptiontoken.completeListSize,
        cursor: map.resumptiontoken.cursor) map.resumptiontoken.key
    }
  }
} else {
  'oai:error'(code: 'noRecordsMatch')
}
