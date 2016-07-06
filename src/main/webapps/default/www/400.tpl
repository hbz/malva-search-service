yieldUnescaped '<!DOCTYPE html>'
html(lang:'en') {
  head {
    title('400 - Bad request')
    meta(charset: 'utf-8')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
    link(rel: 'stylesheet', href: stringOf { url('bower_components/bootstrap/dist/css/bootstrap.min.css') })
  }
  body {
    div(class: 'container') {
      h1('Bad request')
      p(class: 'responseStatus') {
        yield "Status ${stringOf { responseStatus } }"
      }
    }
  }
}