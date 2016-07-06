yieldUnescaped '<!DOCTYPE html>'
html(lang:'en') {
  head {
    title('500 - Server error')
    meta(charset: 'utf-8')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
    link(rel: 'stylesheet', href: stringOf { url('bower_components/bootstrap/dist/css/bootstrap.min.css') })
  }
  body {
    div(class: 'container') {
      h1('Server error')
      p(class: 'responseStatus') {
        yield "Status ${stringOf { responseStatus } }"
      }
      p(class: 'exception') {
        yield "Exception ${stringOf { exception } }"
      }
      p(class: 'exceptionMessage') {
        yield "Exception message ${stringOf { exceptionMessage } }"
      }
      pre {
        code(class: 'trace') {
          StringWriter s = new StringWriter()
          org.codehaus.groovy.runtime.StackTraceUtils.printSanitizedStackTrace(exception, new PrintWriter(s))
          yield "${s.toString()}"
        }
      }
    }
  }
}
