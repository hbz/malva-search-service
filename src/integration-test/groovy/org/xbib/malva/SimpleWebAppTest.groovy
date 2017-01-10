package org.xbib.malva

import geb.Browser
import geb.Page
import org.junit.Test
import org.xbib.content.settings.Settings
import org.xbib.content.settings.SettingsLoaderService
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkUtils

import static org.xbib.content.settings.Settings.settingsBuilder

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
                to SRUPage
                assert page instanceof SRUPage
                Thread.sleep(3000L)
            }.quit()
        } finally {
            webappServer.shutdown()
        }
    }
}
