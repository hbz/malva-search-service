package org.xbib.malva

import geb.Browser
import geb.Page
import org.junit.Test
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkUtils

class RootPage extends Page {
    static url = '/'
}

class SRUPage extends Page {
    static url = '/sru/hbz'
}

class SimpleWebAppTest {

    @Test
    void simpleWebappTest() {

        NetworkUtils.configureSystemProperties()

        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(getClass().getResourceAsStream('/test-config.json'))
            Browser.drive {
                to RootPage
                assert page instanceof RootPage
                to SRUPage
                assert page instanceof SRUPage
                Thread.sleep(3000L)
            }.quit()
        } finally {
            webappServer.shutdown()
        }
    }
}
