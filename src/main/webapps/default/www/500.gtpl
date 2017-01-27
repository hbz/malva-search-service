yieldUnescaped '<!DOCTYPE html>'
html(lang:'en') {
  head {
    title('500 - Server error')
    meta(charset: 'utf-8')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
    link(rel: 'stylesheet', href: stringOf { url('webjars/bootstrap/3.3.5/css/bootstrap.min.css') })
  }
  body {
    div(class: 'container') {
      h1('Server error')
      div(class: 'alert alert-danger') {
        p(class: 'responseStatus') {
          strong "Status"
          yield " ${stringOf { response.httpStatus } }"
        }
        p(class: 'exception') {
          strong "Exception"
          yield " ${stringOf { exception } }"
        }
        p(class: 'exceptionMessage') {
          strong "Exception message"
          yield " ${stringOf { exceptionMessage } }"
        }
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
