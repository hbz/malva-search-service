package org.xbib.malva.extensions.discovery

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import groovy.util.logging.Log4j2
import org.xbib.content.settings.Settings
import org.xbib.malva.MalvaBinding
import org.xbib.malva.MalvaExtension
import org.xbib.malva.Webapp
import org.xbib.malva.elasticsearch.ElasticsearchService

/**
 */
@Log4j2
class DiscoveryExtension implements MalvaExtension {

    String name

    Settings settings

    Webapp webapp

    ElasticsearchService elasticsearchService

    @Inject
    DiscoveryExtension(@Assisted String name,
                 @Assisted Settings settings,
                 @Assisted Webapp webapp,
                 ElasticsearchService elasticsearchService) {
        this.name = name
        this.settings = settings
        this.webapp = webapp
        this.elasticsearchService = elasticsearchService
        log.info('{} disocvery extension started', name)
    }

    @Override
    void prepareBinding(MalvaBinding binding) {
    }

    @Override
    void shutdown() {
    }

    @Override
    String name() {
        name
    }

    @Override
    DiscoveryExtension object() {
        this
    }

}
