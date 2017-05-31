import org.xbib.malva.extensions.pli.PublicLibraryInitiativeResponse
yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
  head {
      title('Verfügbarkeitsrecherche')
      meta(charset: 'utf-8')
      meta('http-equiv': 'X-UA=Compatible', content: 'IE=edge')
      meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
      link(rel: 'stylesheet', href: contextPath('webjars/bootstrap/3.3.5/css/bootstrap.min.css'))
  }
  body {
    div(class: 'container') {
      div(class: 'row') {
        div(class: 'col-md-12') {
          h2 'Verfügbarkeitsrecherche'
          form(class: 'form-horizontal', method: 'post', role: 'form') {
            fieldset {
              legend 'Medien für die Fernleihe'
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'id', class: 'control-label pull-right') {
                    yield 'Katalog-ID'
                  }
                }
                div(class: 'col-md-9') {
                  input(type: 'text', class: 'form-control input-md pull-left', placeholder: 'ZDB-ID, hbz-ID ...',
                         id: 'id', name: 'id', value: stringOf { params.getString('id') })
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'issn', class: 'control-label pull-right') {
                    yield 'ISSN'
                  }
                }
                div(class: 'col-md-9') {
                  input(type: 'text', class: 'form-control input-md pull-left', placeholder: 'ISSN',
                         id: 'issn', name: 'issn', value: stringOf { params.getString('issn') })
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'year', class: 'control-label pull-right') {
                      yield 'Erscheinungsjahr'
                  }
                }
                div(class: 'col-md-9') {
                  input(type: 'text', class: 'form-control', placeholder: 'Jahr',
                     id: 'year', name: 'year', value: stringOf { params.getString('year') })
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'region', class: 'control-label pull-right') {
                        yield 'Region'
                  }
                }
                div(class: 'col-md-9') {
                  select(class: 'form-control', id: 'region', name: 'region') {
                    ['alle':'',
                         'Leihverkehrsregion NRW':'NRW',
                         'Leihverkehrsregion BAY':'BAY',
                         'Leihverkehrsregion BER':'BER',
                         'Leihverkehrsregion BAW':'BAW',
                         'Leihverkehrsregion GBV':'GBV',
                         'Leihverkehrsregion HES':'HES',
                         'Leihverkehrsregion SAX':'SAX'
                    ].each { k,v ->
                      if (params.getString('region') == v) {
                         option(value: v, selected: 'selected') { yield k }
                      } else {
                         option(value: v) { yield k }
                      }
                    }
                  }
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'library', class: 'control-label pull-right') {
                        yield 'Bibliothek'
                  }
                }
                div(class: 'col-md-9') {
                  input(type: 'text', class: 'form-control', placeholder: 'ISIL',
                         id: 'library', name: 'library', value: params.getString('library') )
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'mode', class: 'control-label pull-right') {
                        yield 'Medium'
                  }
                }
                div(class: 'col-md-9') {
                  select(class: 'form-control', id: 'carriertype', name: 'carriertype') {
                    ['alle Werte':'',
                         'gedruckte Ressource':'volume',
                         'Online-Ressource':'online resource'
                    ].each { k,v ->
                      if (params.getString('carriertype') == v) {
                         option(value: v, selected: 'selected') { yield k }
                      } else {
                         option(value: v) { yield k }
                      }
                    }
                  }
                }
              }
              div(class: 'form-group col-md-12 pull-left') {
                div(class: 'col-md-3') {
                  label(for: 'mode', class: 'control-label pull-right') {
                        yield 'Fernleihmodus'
                  }
                }
                div(class: 'col-md-9') {
                  select(class: 'form-control', id: 'mode', name: 'mode') {
                    ['alle Werte':'',
                         'Leihe':'loan',
                         'Kopie':'copy',
                         'nicht erlaubt':'none'
                    ].each { k,v ->
                      if (params.getString('mode') == v) {
                         option(value: v, selected: 'selected') { yield k }
                      } else {
                         option(value: v) { yield k }
                      }
                    }
                  }
                }
              }
              div(class: 'form-group col-md-12') {
                div(class: 'col-md-3') {
                  label(for: 'submit', class: 'xs-label')
                }
                div(class: 'col-md-9') {
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
      if (params.getString('id') || params.getString('issn')) {

        params.base_service = 'avail-v1'
        params.base_region = params.region?:'*'
        params.base_library = params.library?:'*'
        params.type = 'interlibrary'

        PublicLibraryInitiativeResponse response = pli.execute(params)

        p "${response.meta}"

        def carriertypes = [
               'volume': 'gedruckte Ressource',
               'online resource': 'Online-Ressource',
               'computer disc': 'Datenträger für Computer'
        ]
        def modes = [
               'copy': 'Kopie',
               'loan': 'Leihe',
               'none': 'nicht erlaubt'
        ]
        def dists = [
                'none': 'unbeschränkt',
                'unrestricted': 'unbeschränkt',
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
            if (response.meta.interlibrarybyregions) {
              div(class: 'panel panel-default') {
                div(class: 'panel-heading') {
                  yield "Gefunden wurden Fernleihnachweise in ${response.meta.interlibrarybyregions.size()?:'keine'} Regionen"
                }
                div(class: 'panel-body') {
                  dl(class: 'dl-horizontal') {
                    response.meta.interlibrarybyregions.each { entry ->
                      dt "${entry.key}"
                      dd "${entry.value.join(', ')}"
                    }
                  }
                }
              }
            }
          }
        }

        div(class: 'row') {
          div(class: 'col-md-12') {
            if (response.meta.noninterlibrarybyregions) {
              div(class: 'panel panel-default') {
                div(class: 'panel-heading') {
                  yield "Gefunden wurden andere Nachweise in ${response.meta.noninterlibrarybyregions.size()?:'keine'} Regionen"
                }
                div(class: 'panel-body') {
                  dl(class: 'dl-horizontal') {
                    response.meta.noninterlibrarybyregions.each { entry ->
                      dt "${entry.key}"
                      dd "${entry.value.join(', ')}"
                    }
                  }
                }
              }
            }
          }
        }

        div(class: 'row') {
          div(class: 'col-md-12') {
              p "Gefunden wurden ${response.interlibrary.size()?:'keine'} Bibliotheken mit Fernleihnachweisen"
              strong stringOf { response.meta.eonly ? 'Ressource ist nur elektronisch' : '' }
              strong stringOf { response.meta.openaccess ? 'Ressource is als Open Access verfügbar' : '' }
              strong stringOf { response.meta.green ? "Ressource ist für Bibliotheken frei zugänglich" : '' }
              dl(class: 'dl-horizontal') {
                if (response.meta.links) {
                  response.meta.links.each { link ->
                    dt stringOf { link.nonpublicnote?:'Link' }
                    dd {
                      a(target: '_blank', href: link.uri ) {
                        yield link.uri
                      }
                    }
                  }
                }
                response.interlibrary.each { isil, services ->
                  services.each { s ->
                    div(class: 'panel panel-default') {
                      div(class: 'panel-heading') {
                        yield "LP ${s.priority}"
                      }
                      div(class: 'panel-body') {
                        dt 'Bibliothek'
                        dd "${s.region} / ${s.name} / ${s.isil}"
                        dt 'eindeutiger Bezeichner'
                        dd  "${s._id}"
                        dt 'Medium'
                        dd carriertypes."$s.carriertype"
                        [s.mode].flatten().each { m ->
                          dt 'Fernleihe'
                          dd modes."$m"
                        }
                        if (s.distribution) {
                          [s.distribution].flatten().each { d ->
                            dt 'Einschränkung'
                            dd dists."$d"
                          }
                        }
                        if (s.comment) {
                          dt 'Kommentar'
                          dd s.comment
                        }
                        if (s.info) {
                          if (s.info.license) {
                            dt 'Lizenz'
                            dd scopes."$s.info.license.scope"
                            if (s.info.license.charge) {
                              dt 'Lizenz kostenpflichtig'
                              dd charges."$s.info.license.charge"
                            }
                            if (s.info.license.readme) {
                              [s.info.license.readme].flatten().each { r ->
                                dt 'Readme-Datei'
                                dd {
                                  a(target: '_blank', href: r ) { yield r }
                                }
                              }
                            }
                          }
                          if (s.info.textualholdings) {
                            [s.info.textualholdings].flatten().each { t ->
                              dt 'Bestandsnachweis'
                              dd { yield t }
                            }
                          }
                          if (s.info.location) {
                            s.info.location.each { l ->
                              if (l) {
                                dt 'Standort/Signatur'
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
                            [s.info.links].flatten().each { link ->
                                dt link.nonpublicnote
                                dd {
                                  a(target: '_blank', href: link.uri ) { yield link.uri }
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
  }
}