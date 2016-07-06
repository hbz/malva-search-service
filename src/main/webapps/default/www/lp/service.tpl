yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
  head {
      title('Online-Deckblatt')
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
          h2 'Nachweis aus ZDB/EZB'
          form(class: 'form-horizontal', method: 'post', role: 'form') {
            fieldset {
              legend 'Service-Eintrag für die Kopienfernleihe'
              div(class: 'form-group col-xs-12 pull-left') {
                div(class: 'col-xs-3') {
                  label(for: 'id', class: 'control-label pull-right') {
                    yield 'Service-ID'
                  }
                }
                div(class: 'col-xs-9') {
                  input(type: 'text', class: 'form-control input-md pull-left', placeholder: 'Service-ID hier eintragen',
                         id: 'id', name: 'id', value: stringOf { params.getString('id') })
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
        s = router.getService(params.getString('id'))
        def carriertypes = [
               'volume': 'Band',
               'online resource': 'Online-Ressource',
               'computer disc': 'Datenträger für Computer'
        ]
        def modes = [ 'copy': 'Kopie', 'loan': 'Leihe', 'none': 'nicht erlaubt' ]
        def dists = [ 'none': 'unbeschränkt', 'unrestricted': 'unbeschränkt', 'domestic': 'nur Inland', 'postal': 'nur Postversand', 'electronic': 'auch elektronisch an Nutzer']
        def scopes = [ 'solitary': 'Einzellizenz', 'national': 'Nationallizenz', 'consortial': 'Konsortiallizenz']
        def charges = [ 'yes': 'ja', 'no': 'nein', 'no-with-print': 'kostenlos mit Druckausgabe']
        div(class: 'row') {
          div(class: 'col-md-12') {
              dl(class: 'dl-horizontal') {
                dt('Lieferposition')
                dd stringOf { 'LP' + s.priority }
                if (s.name) { 
                  dt('Bibliothek')
                  dd stringOf { '' + s.region + ' / ' + s.name }
                }
                dt('eindeutiger Bezeichner')
                dd stringOf { params.getString('id') }
                s.parents.each { p ->
                  dt('zugehöriger Titel')
                  dd {
                    a(target: '_blank', href: stringOf { "index?id=${p}" } ) { yield stringOf { p } }
                  }
                }
                dt('Medium')
                dd {
                  yield stringOf { carriertypes."$s.carriertype" }
                }
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
                      dt('Standort/Signatur')
                      dd stringOf {
                        l.callnumber ? l.callnumber :
                        l.collection ? l.collection :
                        l.publicnote ? l.publicnote :
                        l
                      }
                    }
                  }
                  if (s.info.links) {
                    s.info.links.each { link ->
                      dt stringOf { link.nonpublicnote }
                      dd { a(target: '_blank', href: stringOf { link.uri } ) { yield stringOf { link.uri } } }
                    }
                  }
                }
              }
          }
        }
        div(class: 'row') {
          div(class: 'col-md-12') { 
            hr(style: 'width:100%; color: black; height: 1px; background-color:black')
          }
        }
        s.parent.each { parent ->
          m = router.getManifestation(parent)
          div(class: 'row') {
            div(class: 'col-md-12') {
               h3 "dazugehöriger Titel"
               dl(class: 'dl-horizontal') {
               dt('ID') 
               dd "$m.identifierForTheManifestation"
               dt('Titel') 
               dd "$m.title"
               dt('Verlag, Ort, erschienen')
               l = m.lastdate ? '' + m.lastdate : ''
               dd "$m.publishedby, $m.publishedat, $m.firstdate - $l"
               dt('Ressourcentyp')
               dd "$m.mediatype - $m.contenttype - $m.carriertype"
                  if (m.links) {
                    m.links.each { link ->
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