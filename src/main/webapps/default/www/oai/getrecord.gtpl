'oai:GetRecord' {
    if (map.records) {
        rec = map.records[0]
        'oai:record' {
          'oai:header' {
             'oai:identifier' oai.normalizeIndexName(rec.index) + "/${rec.type}/${rec.id}"
             'oai:datestamp' rec.timestamp
          }
          'oai:metadata' {
            yieldUnescaped "${rec.recorddata}"
          }
        }
    }
}
