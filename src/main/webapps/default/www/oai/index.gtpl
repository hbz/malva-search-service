responseType = 'text/xml'
namespace = 'http://www.openarchives.org/OAI/2.0/'
schemaLocation = 'http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd'
responseHeader."Content-Type" = "${responseType}; charset=utf-8"
map = oai.execute(path, params)
xmlDeclaration()
'oai:OAI-PMH'('xmlns:oai': namespace,
                             'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                             'xsi:schemaLocation': schemaLocation) {
  'oai:responseDate' java.time.Instant.now()
  'oai:request'(verb: params.verb, from: params.from, until: params.until,
       set: params.set, metadataPrefix: params.metadataPrefix ) {
       url('')
  }
  if (params.verb == 'Identify') {
      include template 'oai/identify.tpl'
  }
  if (params.verb == 'GetRecord') {
      include template 'oai/getrecord.tpl'
  }
  if (params.verb == 'ListRecords') {
      include template 'oai/listrecords.tpl'
  }
  if (params.verb == 'ListIdentifiers') {
      include template 'oai/listrecords.tpl'
  }
}