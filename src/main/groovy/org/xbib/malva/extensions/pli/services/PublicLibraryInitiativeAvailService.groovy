package org.xbib.malva.extensions.pli.services

import groovy.util.logging.Log4j2
import org.elasticsearch.action.get.GetAction
import org.elasticsearch.action.get.GetRequestBuilder
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.get.MultiGetAction
import org.elasticsearch.action.get.MultiGetItemResponse
import org.elasticsearch.action.get.MultiGetRequestBuilder
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.search.ClearScrollAction
import org.elasticsearch.action.search.ClearScrollRequestBuilder
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollAction
import org.elasticsearch.action.search.SearchScrollRequestBuilder
import org.elasticsearch.client.ElasticsearchClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortBuilders
import org.xbib.content.settings.Settings
import org.xbib.malva.Webapp
import org.xbib.malva.extensions.elasticsearch.ElasticsearchExtension
import org.xbib.malva.extensions.pli.PublicLibraryInitiativeParameters
import org.xbib.malva.extensions.pli.PublicLibraryInitiativeRequest
import org.xbib.malva.extensions.pli.PublicLibraryInitiativeResponse
import org.xbib.malva.extensions.pli.entities.Library
import org.xbib.malva.extensions.pli.entities.Service
import org.xbib.malva.util.MultiMap

@Log4j2
class PublicLibraryInitiativeAvailService implements PublicLibraryInitiativeParameters {

    Settings settings

    ElasticsearchClient client

    String manifestationsIndex

    String manifestationsType

    String partsIndex

    String partsType

    String holdingsIndex

    String holdingsType

    String chronoIndex

    String chronoType

    String servicesIndex

    String servicesType

    long scrollMillis

    Settings regionSettings

    Map<String, Integer> regionOrder

    Map<String, Collection<String>> formatByLibrary = [:]

    Map<String, String> libraryMarker

    List<String> withLibrary

    List<String> withoutLibrary

    List<String> withCarrierType

    List<String> withoutCarrierType

    List<String> withRegion

    List<String> withoutRegion

    List<String> withType

    List<String> withoutType

    List<String> withMode

    List<String> withoutMode

    List<String> withDistribution

    List<String> withoutDistribution

    PublicLibraryInitiativeAvailService(Settings settings, Webapp webapp) {
        this.settings = settings
        ElasticsearchExtension elasticsearchExtension = webapp.extensions().get('elasticsearch') as ElasticsearchExtension
        this.client = elasticsearchExtension.elasticsearchService.resources.get(webapp).client
        String defaultIndex = "efl"
        this.manifestationsIndex = settings.get("manifestations.index", defaultIndex)
        this.manifestationsType = settings.get("manifestations.type", "manifestations")
        this.partsIndex = settings.get("manifestations.index", defaultIndex)
        this.partsType = settings.get("manifestations.type", "parts")
        this.holdingsIndex = settings.get("holdings.index", defaultIndex)
        this.holdingsType = settings.get("holdings.type", "holdings")
        this.chronoIndex = settings.get("chrono.index.", defaultIndex)
        this.chronoType = settings.get("chrono.type", "chrono")
        this.servicesIndex = settings.get("services.index", defaultIndex)
        this.servicesType = settings.get("services.type", "services")
        this.scrollMillis = settings.getAsTime("scrolltimeout",
                org.xbib.content.util.unit.TimeValue.timeValueSeconds(60)).millis()
        this.regionOrder = [:]
        this.regionSettings = settings.getAsSettings("regions")
        regionSettings.asMap.keySet().eachWithIndex { entry, i ->
            regionOrder.put(entry, i)
        }
    }

    PublicLibraryInitiativeResponse avail(PublicLibraryInitiativeRequest request) {
        PublicLibraryInitiativeResponse response = new PublicLibraryInitiativeResponse()
        validate(request)
        MultiMap<String, Map<String, Object>> multiMap = [:]
        GetRequestBuilder getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                .setIndex(manifestationsIndex).setType(manifestationsType)
                .setId(request.id)
        GetResponse getResponse = getRequestBuilder.execute().actionGet()
        boolean isInManifestations = false
        if (getResponse.isExists()) {
            isInManifestations = true
            Map<String, Object> source = getResponse.source
            if (source.containsKey("green")) {
                response.meta.put("green", source.get("green"))
                if (source.containsKey("greeninfo")) {
                    response.meta.put("greeninfo", source.get("greeninfo"))
                }
            }
            if (source.containsKey("openaccess")) {
                response.meta.put("openaccess", source.get("openaccess"))
            }
            if (source.containsKey("links")) {
                response.meta.put("links", source.get("links"))
            }
        }
        if (!isInManifestations) {
            // nothing found in manifestation, search in monograph parts
            getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(partsIndex).setType(partsType)
                    .setId(request.id)
            getResponse = getRequestBuilder.execute().actionGet()
            if (getResponse.isExists()) {
                List<String> serviceIds = getResponse.source.get("service") as List
                toServices(serviceIds, multiMap)
                toResult(request, response, toLibraries(multiMap))
            } else {
                // nothing found, search in parts for id, but also  limit to volumeissue (part), year (firstdate)
                QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                QueryBuilder idQuery = QueryBuilders.matchQuery('id', request.id)
                QueryBuilder parentQuery = QueryBuilders.matchQuery('parent', request.id)
                queryBuilder.should(idQuery).should(parentQuery).minimumNumberShouldMatch(1)
                QueryBuilder partQuery = request.volumeissue ? QueryBuilders.matchQuery('volumeissue', request.volumeissue) : null
                if (partQuery) {
                    queryBuilder = queryBuilder.must(partQuery)
                }
                QueryBuilder dateQuery = request.year ? QueryBuilders.matchQuery('firstdate', request.year) : null
                if (dateQuery) {
                    queryBuilder = queryBuilder.must(dateQuery)
                }
                fetchServicesFrom(partsIndex, partsType, queryBuilder, multiMap)
                toResult(request, response, toLibraries(multiMap))
            }
        } else if (request.year && request.year > 0) {
            String chronoId = request.id + "." + request.year
            getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                    .setIndex(chronoIndex).setType(chronoType)
                    .setId(chronoId)
            getResponse = getRequestBuilder.execute().actionGet()
            if (getResponse.isExists()) {
                List<String> servicesIds = getResponse.source.get("service") as List
                toServices(servicesIds, multiMap)
                toResult(request, response, toLibraries(multiMap))
            } else {
                chronoId = request.id + ".-1"
                getRequestBuilder = new GetRequestBuilder(client, GetAction.INSTANCE)
                        .setIndex(chronoIndex).setType(chronoType)
                        .setId(chronoId)
                getResponse = getRequestBuilder.execute().actionGet()
                if (getResponse.isExists()) {
                    List<String> servicesIds = getResponse.source.get("service") as List
                    toServices(servicesIds, multiMap)
                    toResult(request, response, toLibraries(multiMap))
                }
            }
        } else {
            // search for parent in holdings, put out everything
            QueryBuilder queryBuilder = QueryBuilders.matchQuery("parent", request.id)
            fetchServicesFrom(holdingsIndex, holdingsType, queryBuilder, multiMap)
            toResult(request, response, toLibraries(multiMap))
        }
        response
    }

    private void fetchServicesFrom(String index, String type, QueryBuilder queryBuilder,
                                  MultiMap multiMap) {
        SearchRequestBuilder searchRequest = new SearchRequestBuilder(client, SearchAction.INSTANCE)
                .setIndices(index)
                .setTypes(type)
                .setQuery(queryBuilder)
                .setScroll(TimeValue.timeValueMillis(scrollMillis))
                .setSize(10)
                .addSort(SortBuilders.fieldSort("_doc"))
        SearchResponse searchResponse = searchRequest.execute().actionGet()
        while (searchResponse.hits.hits.length > 0) {
            for (SearchHit hit : searchResponse.hits) {
                List<String> serviceIDs = hit.source.get("service") as List
                toServices(serviceIDs, multiMap)
            }
            searchResponse = new SearchScrollRequestBuilder(client, SearchScrollAction.INSTANCE,
                    searchResponse.getScrollId())
                    .setScroll(TimeValue.timeValueMillis(scrollMillis))
                    .execute().actionGet()
        }
        ClearScrollRequestBuilder clearScrollRequestBuilder = new ClearScrollRequestBuilder(client,
                ClearScrollAction.INSTANCE)
                .addScrollId(searchResponse.getScrollId())
        clearScrollRequestBuilder.execute().actionGet()
    }

    private List<Library> toLibraries(MultiMap<String, Map<String, Object>> map) {
        List<Library> libraries = []
        if (!map) {
            return libraries
        }
        map.keySet().each { isil ->
            Library.Builder builder = new Library.Builder()
            builder.regionOrder(regionOrder)
            builder.isil(isil)
            if (formatByLibrary) {
                formatByLibrary.get(isil)?.each { format ->
                    builder.format(format)
                }
            }
            map.getAll(isil).each { Map<String, Object> service ->
                builder.service(service.get('_id') as String, service)
            }
            libraries.add(builder.build())
        }
        libraries
    }

    private void toResult(PublicLibraryInitiativeRequest request,
                          PublicLibraryInitiativeResponse response,
                          List<Library> listOfLibraries) {
        withLibrary = request.library ? request.library.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_library") as List<String>
        withoutLibrary = request.library ? request.library.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_library") as List<String>
        withCarrierType = request.carriertype ? request.carriertype.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_carrier") as List<String>
        withoutCarrierType = request.carriertype ? request.carriertype.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_carrier") as List<String>
        withRegion = request.region && request.region != "*" ? request.region.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_region") as List<String>
        withoutRegion = request.region && request.region != "*" ? request.region.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_region") as List<String>
        withType = request.type ? request.type.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_type") as List<String>
        withoutType = request.type ? request.type.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_type") as List<String>
        withMode = request.mode ? request.mode.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_mode") as List<String>
        withoutMode = request.mode ? request.mode.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_mode") as List<String>
        withDistribution = request.distribution ? request.distribution.findAll { !it.startsWith(('-')) }
                .collect { it.startsWith('+') ? it.substring(1) : it } :
                settings.getAsArray("with_distribution") as List<String>
        withoutDistribution = request.distribution ? request.distribution.findAll { it.startsWith(('-')) }
                .collect { it.substring(1) } :
                settings.getAsArray("without_distribution") as List<String>
        List<Library> filteredListOfLibraries = listOfLibraries.findAll { library ->
            includeLibrary(library) && excludeLibrary(library)
        }
        filteredListOfLibraries = filteredListOfLibraries.findAll { library ->
            int numberOfServicesBeforeFilter = library.interlibraryServices.size()
            List<Service> filteredInterlibraryServices = library.interlibraryServices.findAll { service ->
                includeCarrierType(service) && excludeCarrierType(service) &&
                includeCarrierType(library, service) && excludeCarrierType(library, service) &&
                includeRegion(service) && excludeRegion(service) &&
                includeLibrary(service) && excludeLibrary(service) &&
                includeType(service) && excludeType(service) &&
                includeMode(service) && excludeMode(service) &&
                includeDistribution(service) && excludeDistribution(service)
            }
            // remove library if their services were all removed
            if (numberOfServicesBeforeFilter > 0 && filteredInterlibraryServices.isEmpty()) {
                return false
            }
            // update new interlibrary services
            if (!filteredInterlibraryServices.isEmpty()) {
                library.setInterlibraryServices(filteredInterlibraryServices)
            }
            // search all filtered interlibrary services for non-used services by finding the difference set
            Set<Service> diff = new HashSet<>()
            diff.addAll(library.interlibraryServices)
            diff.removeAll(filteredInterlibraryServices)
            List<Service> nonInterlibraryServices = library.nonInterlibraryServices
            if (!diff.isEmpty()) {
                nonInterlibraryServices.addAll(diff)
            }
            if (!nonInterlibraryServices.isEmpty()) {
                library.setNonInterlibraryServices(nonInterlibraryServices)
            }
            true
        }
        List<Library> markedListOfLibraries = filteredListOfLibraries.collect { library ->
            library.setMarker(getMarker(library))
        }
        response.meta.possession = request.baseLibrary && markedListOfLibraries.find { library ->
            library.isil == request.baseLibrary
        }
        Map<String, List<Library>> allregions = markedListOfLibraries.groupBy { library ->
            library.region.name
        }
        // remove "null" region (region not set = unconfigured library)
        List<Library> unconfiguredLibraries = allregions.remove(null)
        if (unconfiguredLibraries) {
            log.warn('unconfigured libraries = {}', unconfiguredLibraries)
        }
        response.meta.interlibrarybyregions = allregions.collectEntries { k, v ->
            [ k, toPriority(v).findAll { it.map.containsKey('interlibraryservice') }.collect { it.isil } ]
        }.findAll { k, v -> !v.isEmpty() }
        response.meta.noninterlibrarybyregions = allregions.collectEntries { k, v ->
            [ k, v.findAll { it.map.containsKey('noninterlibraryservice') }.collect { it.isil } ]
        }.findAll { k, v -> !v.isEmpty() }
        // global
        List<Library> interlibrary = toPriority(markedListOfLibraries).findAll {
            it.map.containsKey('interlibraryservice')
        }
        List<Library> noninterlibrary = markedListOfLibraries.findAll {
            it.map.containsKey('noninterlibraryservice')
        }
        response.interlibrary = interlibrary.collectEntries { [ (it.isil): it.map.interlibraryservice ] }
        response.noninterlibrary = noninterlibrary.collectEntries { [ (it.isil): it.map.noninterlibraryservice ] }
        // compact
        response.meta.interlibrary = interlibrary.collect { it.isil }
        response.meta.noninterlibrary = noninterlibrary.collect { it.isil }
    }

    private static List<Library> toPriority(List<Library> libraries) {
        List<Library> librariesByPriority = []
        Map<Boolean, List<Library>> priorities = libraries.groupBy { library ->
                library.getMarker("priority")
        }
        if (priorities.containsKey(true)) {
            List<Library> withPriority = priorities.remove(true)
            withPriority.sort(new Library.PriorityRandomComparator())
            librariesByPriority.addAll(withPriority)
            List<Library> withoutPriority = priorities.remove(false)
            if (withoutPriority) {
                withoutPriority.sort(new Library.PriorityRandomComparator())
                librariesByPriority.addAll(withoutPriority)
            }
        } else if (priorities.containsKey(false)) {
            List<Library> withoutPriority = priorities.remove(false)
            withoutPriority.sort(new Library.PriorityRandomComparator())
            librariesByPriority.addAll(withoutPriority)
        }
        librariesByPriority
    }

    private void toServices(List<String> serviceIds, MultiMap<String, Map<String, Object>> multiMap) {
        if (!serviceIds) {
            return
        }
        MultiGetRequestBuilder multiGetRequestBuilder = new MultiGetRequestBuilder(client, MultiGetAction.INSTANCE)
        for (String serviceID : serviceIds) {
            multiGetRequestBuilder.add(servicesIndex, servicesType, serviceID)
        }
        MultiGetResponse multiGetResponse = multiGetRequestBuilder.execute().actionGet()
        for (MultiGetItemResponse multiGetItemResponse : multiGetResponse) {
            if (!multiGetItemResponse.isFailed()) {
                String id = multiGetItemResponse.response.id
                Map<String, Object> m = [:]
                if (multiGetItemResponse.response.source) {
                    m.putAll(multiGetItemResponse.response.source)
                    m.put("_id", id)
                    int pos1 = id.indexOf('(')
                    int pos2 = id.indexOf(')')
                    String isil = id.substring(pos1 + 1, pos2)
                    multiMap.put(isil, m)
                } else {
                    log.warn('no content for '+ multiGetItemResponse.response.id)
                }
            } else {
                log.warn('failed: ' + multiGetItemResponse.response.id, multiGetItemResponse.getFailure().failure)
            }
        }
    }

    Map<String, Object> getDoc(String index, String type, String id) {
        GetRequestBuilder getRequest = new GetRequestBuilder(client, GetAction.INSTANCE)
                .setIndex(index).setType(type).setId(id)
        GetResponse getResponse = getRequest.execute().actionGet()
        getResponse.isExists() ? getResponse.source : [:]
    }

    private boolean includeLibrary(Library library) {
        withLibrary.isEmpty() || withLibrary.contains(library.isil)
    }

    private boolean excludeLibrary(Library library) {
        withoutLibrary.isEmpty() || !withoutLibrary.contains(library.isil)
    }

    private boolean includeLibrary(Service service) {
        withLibrary.isEmpty() ||
                (service.map.containsKey("isil") && withLibrary.contains(service.map.get("isil")))
    }

    private boolean excludeLibrary(Service service) {
        withoutLibrary.isEmpty() ||
                (service.map.containsKey("isil") && !withoutLibrary.contains(service.map.get("isil")))
    }

    private boolean includeCarrierType(Service service) {
        String carrierType = service.map.get("carriertype") as String
        withCarrierType.isEmpty() || withCarrierType.contains(carrierType)
    }

    private boolean excludeCarrierType(Service service) {
        String carrierType = service.map.get("carriertype") as String
        withoutCarrierType.isEmpty() || !withoutCarrierType.contains(carrierType)
    }

    private static boolean includeCarrierType(Library library, Service service) {
        library.isCarrierTypeAllowed(service.map.get("carriertype") as String)
    }

    private static boolean excludeCarrierType(Library library, Service service) {
        library.isCarrierTypeAllowed(service.map.get("carriertype") as String)
    }

    private boolean includeRegion(Service service) {
        withRegion.isEmpty() ||
                (service.map.containsKey("region") && withRegion.contains(service.map.get("region")))
    }

    private boolean excludeRegion(Service service) {
        withoutRegion.isEmpty() ||
                (service.map.containsKey("region") && !withoutRegion.contains(service.map.get("region")))
    }

    private boolean includeType(Service service) {
        String type = (String) service.map.get("type")
        withType.isEmpty() ||
                (service.map.containsKey("type") && withType.contains(type))
    }

    private boolean excludeType(Service service) {
        String type = (String) service.map.get("type")
        withoutType.isEmpty() ||
                (service.map.containsKey("type") && !withoutType.contains(type))
    }

    private boolean includeMode(Service service) {
        checkValueInSet(service.map.get("mode"), withMode)
    }

    private boolean excludeMode(Service service) {
        checkValueNotInSet(service.map.get("mode"), withoutMode)
    }

    private boolean includeDistribution(Service service) {
        checkValueInSet(service.map.get("distribution"), withDistribution)
    }

    private boolean excludeDistribution(Service service) {
        checkValueNotInSet(service.map.get("distribution"), withoutDistribution)
    }

    private String getMarker(Library library) {
        libraryMarker?.get(library.isil)
    }

    private void validate(PublicLibraryInitiativeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("no request given")
        }
        if (request.issn && !request.id) {
            QueryBuilder queryBuilder = QueryBuilders.matchQuery("identifiers.issn", request.issn.replace('-', '').trim())
            SearchRequestBuilder searchRequest = new SearchRequestBuilder(client, SearchAction.INSTANCE)
                    .setIndices(manifestationsIndex).setTypes(manifestationsType)
                    .setQuery(queryBuilder)
                    .setSize(1)
            SearchResponse searchResponse = searchRequest.execute().actionGet()
            if (searchResponse.hits.hits.length > 0) {
                SearchHit hit = searchResponse.hits.hits[0]
                request.id = hit.id
            }
            if (!request.id) {
                throw new IllegalArgumentException("unable to resolve issn " + request.issn)
            }
        }
        if (!request.id) {
            throw new IllegalArgumentException("no identifier found")
        }
        request.id = request.id.trim().toLowerCase().replaceAll("\\-", "")
        Settings settings = regionSettings ? regionSettings.getAsSettings(request.baseRegion) : null
        if (settings) {
            settings.getAsArray('regions').eachWithIndex { entry, i ->
                regionOrder.put(entry, i)
            }
            (settings.getAsStructuredMap().get('restrictions') as Map<String, Collection<String>>).each { k,v ->
                formatByLibrary.put(k, v)
            }
            libraryMarker('priority', settings.getAsArray('priorities') as Collection<String>)
        }
        if (regionOrder.isEmpty()) {
            log.warn("no region order defined, request region={}", request.baseRegion)
        } else {
            log.trace('region order = {}', regionOrder)
        }
        if (request.baseLibrary) {
            libraryMarker("baseLibrary", [request.baseLibrary])
        }
    }

    void libraryMarker(String marker, Collection<String> libraries) {
        if (!marker || marker.isEmpty()) {
            return
        }
        if (!libraryMarker) {
            libraryMarker = [:]
        }
        if (libraries) {
            libraries.findAll { library ->
                !libraryMarker.containsKey(library)
            }.each { library ->
                libraryMarker.put(library, marker)
            }
        }
    }

    private static boolean checkValueInSet(Object o, Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return true
        }
        if (o == null) {
            return false
        }
        if (!(o instanceof Collection)) {
            o = Collections.singleton(o)
        }
        for (Object value : (Collection) o) {
            if (collection.contains(value)) {
                return true
            }
        }
        false
    }

    private static boolean checkValueNotInSet(Object o, Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return true
        }
        if (o == null) {
            return false
        }
        if (!(o instanceof Collection)) {
            o = Collections.singleton(o)
        }
        boolean b = true
        for (Object value : (Collection) o) {
            b = b && !collection.contains(value.toString())
        }
        b
    }
}
