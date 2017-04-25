package org.xbib.malva

import org.junit.Ignore
import org.junit.Test
import org.xbib.malva.bootstrap.WebappServer
import org.xbib.malva.network.NetworkUtils

import java.util.concurrent.CountDownLatch

/**
 */
@Ignore
class RunWebappServer {

    @Test
    void testWebappServer() {
        CountDownLatch latch = new CountDownLatch(1)
        NetworkUtils.configureSystemProperties()
        WebappServer webappServer = new WebappServer()
        try {
            webappServer.run(getClass().getResourceAsStream('/test-config.json'))
            try {
                // wait forever
                latch.await()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                // bail out
            }
        } finally {
            webappServer.shutdown()
        }
    }
}
