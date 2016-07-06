yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
  head {
      title('Verfügbarkeitsrecherche')
      meta(charset: 'utf-8')
      meta('http-equiv': 'X-UA=Compatible', content: 'IE=edge')
      meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
      link(rel: 'icon', href: url('dist/img/elasticsearch-icon.png'))
      link(rel: 'stylesheet', href: url('bower_components/bootstrap/dist/css/bootstrap.min.css'))
      link(rel: 'stylesheet', href: url('bower_components/font-awesome/css/font-awesome.min.css'))
        style(type: 'text/css') {
           yieldUnescaped '''
        /*<![CDATA[*/
        /*]]>*/
          '''
        }
  }
  body {
    div(class: 'container') {
      div(class: 'row') {
        div(class: 'col-md-12') {
          h2 'Nachweise aus ZDB/EZB'
          form(class: 'form-horizontal', method: 'post', role: 'form') {
            fieldset {
              legend 'Verfügbarkeit von Medien für die Kopienfernleihe'
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'id', class: 'control-label pull-right') {
                    yield 'ZDB-ID'
                  }
                }
                div(class: 'col-xs-9') {
                  input(type: 'text', class: 'form-control input-md pull-left', placeholder: 'ZDB-ID hier eintragen',
                         id: 'id', name: 'id', value: stringOf { params.getString('id') })
                }
              }
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'year', class: 'control-label pull-right') {
                      yield 'Jahrgang'
                  }
                }
                div(class: 'col-xs-9') {
                  input(type: 'text', class: 'form-control', placeholder: 'Jahrgang hier eintragen',
                     id: 'year', name: 'year', value: stringOf { params.getString('year') })
                }
              }
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'with_institution', class: 'control-label pull-right') {
                        yield 'Institution'
                  }
                }
                div(class: 'col-xs-9') {
                  input(type: 'text', class: 'form-control', placeholder: 'ISIL hier eintragen (optional)',
                         id: 'with_institution', name: 'with_institution', value: stringOf { params.getString('with_institution') })
                }
              }
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'base_region', class: 'control-label pull-right') {
                        yield 'Region'
                  }
                }
                div(class: 'col-xs-9') {
                  select(class: 'form-control', id: 'base_region', name: 'base_region') {
                    ['alle':'',
                         'NRW':'NRW',
                         'BAY':'BAY',
                         'BER':'BER',
                         'BAW':'BAW',
                         'HAM':'HAM',
                         'HES':'HES',
                         'NIE':'NIE',
                         'SAA':'SAA',
                         'SAX':'SAX',
                         'THU':'THU'
                    ].each { k,v ->
                      if (params.getString('base_region') == v) {
                         option(value: v, selected: 'selected') { yield k }
                      } else {
                         option(value: v) { yield k }
                      }
                    }
                  }
                }
              }
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'with_mode', class: 'control-label pull-right') {
                        yield 'Fernleihe'
                  }
                }
                div(class: 'col-xs-9') {
                  select(class: 'form-control', id: 'with_mode', name: 'with_mode') {
                    ['alle Werte':'',
                         'Leihe':'loan',
                         'Kopie':'copy',
                         'nicht erlaubt':'none'
                    ].each { k,v ->
                      if (params.getString('with_mode') == v) {
                         option(value: v, selected: 'selected') { yield k }
                      } else {
                         option(value: v) { yield k }
                      }
                    }
                  }
                }
              }
              div(class: 'form-group col-xs-12') {
                div(class: 'col-xs-3') {
                  label(for: 'submit', class: 'xs-label')
                }
                div(class: 'col-xs-9') {
                  button(type: 'submit', id: 'submit', class: 'btn btn-default') {
                    i(class: 'fa fa-search')
                    yield " Suche"
                  }
                }
              }
            }
          }
        }
      }
      if (params.getString('id')) {
        vars.".result" = router.execute(params)
        def carriertypes = [
               'volume': 'Band',
               'online resource': 'Online-Ressource',
               'computer disc': 'Datenträger für Computer'
        ]
        def modes = [
               'copy': 'Kopie',
               'loan': 'Leihe',
               'none': 'nicht erlaubt'
        ]
        def dists = [
                'unrestricted': 'unbeschränkt',
                'none': 'unbeschränkt',
                'domestic': 'nur Inland',
                'postal': 'nur Postversand',
                'electronic': 'auch elektronisch an Nutzer'
        ]
        def scopes = [
                'solitary': 'Einzellizenz',
                'national': 'Nationallizenz',
                'consortial': 'Konsortiallizenz'
        ]
        def charges = [
                'yes': 'ja',
                'no': 'nein',
                'no-with-print': 'kostenlos mit Druckausgabe'
        ]
        div(class: 'row') {
          div(class: 'col-md-12') {
            vars.".result".results.each { res ->
              h3 stringOf { res.titlecomponents.join(' ; ') }
              if (res.firstdate) {
                p "$res.publishedat $res.publishedby (${res.firstdate} - ${res.lastdate?:''})"
              }
              strong stringOf { res.eonly ? 'nur elektronisch' : '' }
              strong stringOf { res.openaccess ? ' Open Access' : '' }
              p "Gefunden wurden ${res.count?:'keine'} Institutionen"
              all = res.institutions.find { it.isil == 'DE-ALL' }
              if (all) {
                strong "Diese Ressource ist für Bibliotheken frei zugänglich"
              }
              dl(class: 'dl-horizontal') {
                if (res.links) {
                  res.links.each { link ->
                    dt stringOf { link.nonpublicnote?:'Link' }
                    dd { a(target: '_blank', href: stringOf { link.uri } ) { yield stringOf { link.uri } } }
                  }
                }
                res.institutions.each { inst ->
                  services = [] 
                  services.addAll(inst.service)
                  services.addAll(inst.otherservice)
                  services.each { s ->
                    hr(style: 'width:100%; color: black; height: 1px; background-color:black')
                    dt('Lieferposition')
                    dd stringOf { 'LP' + s.priority }
                    if (s.name) {
                      dt('Bibliothek')
                      dd stringOf { '' + s.region + ' / ' + s.name }
                    }
                    dt('eindeutiger Bezeichner')
                    dd { a(href: "service?id=" + s.getId()) {
                            yield stringOf { s.getId() } }
                    }
                    dt('Medium')
                    dd { yield stringOf { carriertypes."$s.carriertype" } }
                    if (s.mode) {
                      (s.mode instanceof List ? s.mode : [s.mode]).each { m ->
                        dt('Fernleihe')
                        dd stringOf { modes."$m" }
                      }
                    }
                    if (s.distribution) {
                      (s.distribution instanceof List ? s.distribution : [s.distribution]).each { d ->
                        dt('Einschränkung')
                        dd stringOf { dists."$d" }
                      }
                    }
                    if (s.comment) {
                      dt('Kommentar')
                      dd stringOf { s.comment }
                    }
                    if (s.info) {
                      if (s.info.license) {
                        dt('Lizenz')
                        dd stringOf { scopes."$s.info.license.scope" }
                        if (s.info.license.charge) {
                          dt('Lizenz kostenpflichtig')
                          dd stringOf { charges."$s.info.license.charge" }
                        }
                        if (s.info.license.readme) {
                          (s.info.license.readme instanceof List ? s.info.license.readme : [s.info.license.readme]).each { r ->
                            dt('Readme-Datei')
                            dd { a(target: '_blank', href: stringOf { r } ) { yield stringOf { r } } }
                          }
                        }
                      }
                      if (s.info.textualholdings) {
                        (s.info.textualholdings instanceof List ? s.info.textualholdings : [s.info.textualholdings]).each { t ->
                          dt('Bestandsnachweis')
                          dd stringOf { t }
                        }
                      }
                      if (s.info.location) {
                        s.info.location.each { l ->
                          if (l) {
                            dt('Standort/Signatur')
                            dd stringOf {
                              l.callnumber ? l.callnumber :
                              l.collection ? l.collection :
                              l.publicnote ? l.publicnote :
                              l
                            }
                          }
                        }
                      }
                      if (s.info.links) {
                        (s.info.links instanceof List ? s.info.links : [s.info.links]).each { link ->
                            dt stringOf { link.nonpublicnote }
                            dd { a(target: '_blank', href: stringOf { link.uri } ) { yield stringOf { link.uri } } }
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
    }
  }
}