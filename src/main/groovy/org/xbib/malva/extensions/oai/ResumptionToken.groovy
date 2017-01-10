package org.xbib.malva.extensions.oai

import org.elasticsearch.action.search.SearchResponse

import java.time.Instant

/**
 *
 * @param <T>
 */
class ResumptionToken<T> {

    private final static LRUCache<UUID, ResumptionToken> cache = new LRUCache<>(1000)

    private final UUID uuid

    private T value
    
    private long completeListSize
    
    private long cursor

    private Instant expirationDate

    private SearchResponse searchResponse

    private ResumptionToken() {
        this.uuid = UUID.randomUUID()
        this.cursor = 0
        this.value = null
        cache.put(uuid, this)
    }
    
    static ResumptionToken newToken() {
         new ResumptionToken()
    }
    
    static ResumptionToken get(UUID key) {
        cache.get(key)
    }
    
    UUID getKey() {
        uuid
    }

    ResumptionToken setValue(T value) {
        this.value = value
        this
    }
    
    T getValue() {
        value
    }
    
    ResumptionToken setExpireAt(Instant expireAt) {
        this.expirationDate = expireAt
        this
    }

    Instant getExpireAt() {
        expirationDate
    }
    
    ResumptionToken setCompleteListSize(long completeListSize) {
        this.completeListSize = completeListSize
        this
    }
    
    long getCompleteListSize() {
        completeListSize
    }
    
    ResumptionToken setCursor(long cursor) {
        this.cursor = cursor
        this
    }
    
    long getCursor() {
        cursor
    }

    ResumptionToken setSearchResponse(SearchResponse searchResponse) {
        this.searchResponse = searchResponse
        this
    }

    SearchResponse getSearchResponse() {
        searchResponse
    }
    
    @Override
    String toString() {
        value != null ? value.toString() : null
    }

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int cacheSize

        LRUCache(int cacheSize) {
            super(16, 0.75f, true)
            this.cacheSize = cacheSize
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() >= cacheSize
        }
    }
}
