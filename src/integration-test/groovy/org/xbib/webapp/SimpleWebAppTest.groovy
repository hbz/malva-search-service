package org.xbib.webapp

import geb.Browser
import geb.Page
import org.junit.Test
import org.xbib.content.settings.Settings
import org.xbib.content.settings.SettingsLoaderService
import org.xbib.webapp.bootstrap.WebappServer

import static org.xbib.content.settings.Settings.settingsBuilder

class RootPage extends Page {
    static url = '/'
}

class LicensePriorityPage extends Page {
    static url = '/lp'
}

class SimpleWebAppTest {

    @Test
    void simpleWebappTest() {
        InputStream inputStream = getClass().getResourceAsStream('/test-config.json')
        Settings settings = settingsBuilder()
                .put(SettingsLoaderService.loaderFromResource('.json').load(inputStream.text))
                .replacePropertyPlaceholders()
                .build()
        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(settings)
            Browser.drive {
                to RootPage
                assert page instanceof RootPage
                to LicensePriorityPage
                assert page instanceof LicensePriorityPage
                Thread.sleep(3000L)
            }.quit()
        } finally {
            webappServer.shutdown()
        }
    }
}
