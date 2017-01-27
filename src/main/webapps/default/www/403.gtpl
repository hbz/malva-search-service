yieldUnescaped '<!DOCTYPE html>'
html(lang:'en') {
  head {
    title('403 - Forbidden')
    meta(charset: 'utf-8')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
    link(rel: 'stylesheet', href: stringOf { url('webjars/bootstrap/3.3.5/css/bootstrap.min.css') })
  }
  body {
    div(class: 'container') {
      h1('Forbidden')
    }
  }
}