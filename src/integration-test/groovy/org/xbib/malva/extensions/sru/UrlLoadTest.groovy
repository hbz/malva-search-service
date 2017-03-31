package org.xbib.malva.extensions.sru

import groovy.util.logging.Log4j2
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static org.junit.Assert.assertTrue

@Log4j2
class UrlLoadTest {

    @Ignore
    @Test
    void testLoad() {
        try {
            List<URL> urls = [
                    new URL("https://index.hbz-nrw.de/_sru3/hbz?version=2.0&operation=searchRetrieve&query=linux&recordSchema=json&extraRequestData=holdings"),
                    new URL("https://index.hbz-nrw.de/_sru3/hbz?version=2.0&operation=searchRetrieve&query=unix&recordSchema=mods&extraRequestData=holdings"),
                    new URL("https://index.hbz-nrw.de/_sru3/hbz?version=2.0&operation=searchRetrieve&query=solaris"),
            ]

            ExecutorService executor = Executors.newFixedThreadPool(8)
            int max = 1000
            CountDownLatch latch = new CountDownLatch(max)
            (1..max).each {
                log.info("submitting ${it}")
                executor.submit(new Runnable() {
                    @Override
                    void run() {
                        try {
                            InputStream inputStream = pickRandom(urls).openStream()
                            assertTrue(inputStream.bytes.length > 0)
                            inputStream.close()
                        } catch (Throwable t) {
                            log.error(t.getMessage() as String, t)
                        } finally {
                            latch.countDown()
                        }
                    }
                })
            }
            latch.await()
            executor.shutdown()
            log.info("done")
        } catch (Throwable t) {
            log.error(t.getMessage() as String, t)
        }
    }

    private final static Random random = new Random()

    private static URL pickRandom(List<URL> urls) {
        urls.get(random.nextInt(2))
    }
}
