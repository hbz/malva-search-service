if (request.headers?.'accept' == 'application/xml') {
  response.headers."content-type" = 'application/xml; charset=utf-8'
  yieldUnescaped pli.asXml(pli.execute(params))
} else {
  response.headers."content-type" = 'application/json; charset=utf-8'
  yieldUnescaped pli.asJson(pli.execute(params))
}