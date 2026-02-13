package com.example.views.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.views.data.DiscoveredRelay
import com.example.views.data.RelayDiscoveryEvent
import com.example.views.data.RelayMonitorAnnouncement
import com.example.views.data.RelayType
import com.example.views.relay.RelayConnectionStateMachine
import com.example.views.relay.TemporarySubscriptionHandle
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * NIP-66 Relay Discovery and Liveness Monitoring.
 *
 * Fetches kind 30166 (relay discovery) events from relay monitors to build
 * a catalog of discovered relays with their types, supported NIPs, latency,
 * and other metadata. Also fetches kind 10166 (monitor announcements) to
 * discover active monitors.
 *
 * The `T` tag on kind 30166 events provides the relay type from the official
 * nomenclature (Search, PublicOutbox, PublicInbox, etc.), replacing any need
 * for hardcoded relay lists or heuristic guessing.
 */
object Nip66RelayDiscoveryRepository {

    private const val TAG = "Nip66Discovery"
    private const val KIND_RELAY_DISCOVERY = 30166
    private const val KIND_MONITOR_ANNOUNCEMENT = 10166
    private const val FETCH_TIMEOUT_MS = 12_000L
    private const val CACHE_PREFS = "nip66_discovery_cache"
    private const val CACHE_KEY_RELAYS = "discovered_relays"
    private const val CACHE_KEY_MONITORS = "known_monitors"
    private const val CACHE_KEY_TIMESTAMP = "last_fetch"
    private const val CACHE_EXPIRY_MS = 6 * 60 * 60 * 1000L // 6 hours

    /** Well-known relays where NIP-66 monitors publish kind 30166 events. */
    val MONITOR_RELAYS = listOf(
        "wss://relay.nostr.watch",
        "wss://history.nostr.watch",
        "wss://relay.damus.io",
        "wss://nos.lol",
        "wss://relay.nostr.band"
    )

    private val JSON = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t -> Log.e(TAG, "Coroutine failed: ${t.message}", t) })

    /** All discovered relays keyed by normalized URL. */
    private val _discoveredRelays = MutableStateFlow<Map<String, DiscoveredRelay>>(emptyMap())
    val discoveredRelays: StateFlow<Map<String, DiscoveredRelay>> = _discoveredRelays.asStateFlow()

    /** Known relay monitors (pubkeys that publish kind 30166). */
    private val _monitors = MutableStateFlow<List<RelayMonitorAnnouncement>>(emptyList())
    val monitors: StateFlow<List<RelayMonitorAnnouncement>> = _monitors.asStateFlow()

    /** Whether a fetch is currently in progress. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Whether we've completed at least one fetch. */
    private val _hasFetched = MutableStateFlow(false)
    val hasFetched: StateFlow<Boolean> = _hasFetched.asStateFlow()

    @Volatile
    private var fetchHandle: TemporarySubscriptionHandle? = null
    @Volatile
    private var monitorFetchHandle: TemporarySubscriptionHandle? = null
    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Initialize with context for persistent caching.
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            loadFromDisk()
        }
    }

    /**
     * Get the NIP-66 relay type(s) for a given relay URL, or empty set if unknown.
     */
    fun getRelayTypes(relayUrl: String): Set<RelayType> {
        return _discoveredRelays.value[normalizeUrl(relayUrl)]?.types ?: emptySet()
    }

    /**
     * Check if a relay is categorized as a Search/Indexer relay by NIP-66 monitors.
     */
    fun isSearchRelay(relayUrl: String): Boolean {
        return _discoveredRelays.value[normalizeUrl(relayUrl)]?.isSearch == true
    }

    /**
     * Get all discovered relays of a specific type.
     */
    fun getRelaysByType(type: RelayType): List<DiscoveredRelay> {
        return _discoveredRelays.value.values.filter { type in it.types }
    }

    /**
     * Fetch relay discovery events (kind 30166) from the given relays.
     * These are typically fetched from well-known aggregator relays or the
     * user's own relay set. The events are published by relay monitors.
     *
     * @param discoveryRelays Relays to query for kind 30166 events
     * @param monitorPubkeys Optional: specific monitor pubkeys to trust.
     *                       If empty, accepts events from any monitor.
     */
    fun fetchRelayDiscovery(
        discoveryRelays: List<String> = emptyList(),
        monitorPubkeys: List<String> = emptyList()
    ) {
        // Always include well-known NIP-66 monitor relays — user cache relays
        // typically don't carry kind 30166 events.
        val allRelays = (MONITOR_RELAYS + discoveryRelays).distinct()
        if (allRelays.isEmpty()) {
            Log.w(TAG, "No relays to fetch discovery events from")
            return
        }

        // Skip if cache is fresh
        val lastFetch = prefs?.getLong(CACHE_KEY_TIMESTAMP, 0L) ?: 0L
        if (_hasFetched.value && System.currentTimeMillis() - lastFetch < CACHE_EXPIRY_MS) {
            Log.d(TAG, "Discovery cache is fresh (${_discoveredRelays.value.size} relays), skipping fetch")
            return
        }

        fetchHandle?.cancel()
        _isLoading.value = true

        scope.launch {
            try {
                Log.d(TAG, "Fetching kind $KIND_RELAY_DISCOVERY from ${allRelays.size} relays")

                val rawEvents = mutableListOf<Event>()

                val filter = if (monitorPubkeys.isNotEmpty()) {
                    Filter(
                        kinds = listOf(KIND_RELAY_DISCOVERY),
                        authors = monitorPubkeys,
                        limit = 500
                    )
                } else {
                    Filter(
                        kinds = listOf(KIND_RELAY_DISCOVERY),
                        limit = 500
                    )
                }

                val handle = RelayConnectionStateMachine.getInstance()
                    .requestTemporarySubscription(allRelays, filter) { event ->
                        if (event.kind == KIND_RELAY_DISCOVERY) {
                            synchronized(rawEvents) { rawEvents.add(event) }
                        }
                    }
                fetchHandle = handle

                delay(FETCH_TIMEOUT_MS)
                handle.cancel()
                fetchHandle = null

                val events = synchronized(rawEvents) { rawEvents.toList() }
                Log.d(TAG, "Received ${events.size} kind-$KIND_RELAY_DISCOVERY events")

                if (events.isNotEmpty()) {
                    val parsed = events.mapNotNull { parseDiscoveryEvent(it) }
                    val aggregated = aggregateDiscoveryEvents(parsed)
                    _discoveredRelays.value = aggregated
                    saveToDisk(aggregated)
                    Log.d(TAG, "Discovered ${aggregated.size} relays from ${parsed.size} monitor events")
                }

                _hasFetched.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch relay discovery: ${e.message}", e)
                _isLoading.value = false
                _hasFetched.value = true
            }
        }
    }

    /**
     * Fetch relay monitor announcements (kind 10166) to discover active monitors.
     */
    fun fetchMonitors(discoveryRelays: List<String>) {
        if (discoveryRelays.isEmpty()) return
        monitorFetchHandle?.cancel()

        scope.launch {
            try {
                Log.d(TAG, "Fetching kind $KIND_MONITOR_ANNOUNCEMENT from ${discoveryRelays.size} relays")

                val rawEvents = mutableListOf<Event>()
                val filter = Filter(
                    kinds = listOf(KIND_MONITOR_ANNOUNCEMENT),
                    limit = 50
                )

                val handle = RelayConnectionStateMachine.getInstance()
                    .requestTemporarySubscription(discoveryRelays, filter) { event ->
                        if (event.kind == KIND_MONITOR_ANNOUNCEMENT) {
                            synchronized(rawEvents) { rawEvents.add(event) }
                        }
                    }
                monitorFetchHandle = handle

                delay(8_000L)
                handle.cancel()
                monitorFetchHandle = null

                val events = synchronized(rawEvents) { rawEvents.toList() }
                val monitors = events.mapNotNull { parseMonitorAnnouncement(it) }
                    .distinctBy { it.pubkey }
                _monitors.value = monitors
                Log.d(TAG, "Discovered ${monitors.size} relay monitors")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch monitors: ${e.message}", e)
            }
        }
    }

    // ── Parsing ──

    /**
     * Parse a kind 30166 event into a RelayDiscoveryEvent.
     */
    private fun parseDiscoveryEvent(event: Event): RelayDiscoveryEvent? {
        val dTag = event.tags.firstOrNull { it.size >= 2 && it[0] == "d" }?.get(1)
        if (dTag.isNullOrBlank()) return null

        val relayUrl = dTag.trim()
        val types = mutableListOf<RelayType>()
        val nips = mutableListOf<Int>()
        val requirements = mutableListOf<String>()
        val topics = mutableListOf<String>()
        var network: String? = null
        var rttOpen: Int? = null
        var rttRead: Int? = null
        var rttWrite: Int? = null
        var geohash: String? = null
        // l-tag metadata (nostr.watch monitors publish these)
        var countryCode: String? = null
        var isp: String? = null
        var asNumber: String? = null
        var asName: String? = null

        event.tags.forEach { tag ->
            if (tag.size < 2) return@forEach
            when (tag[0]) {
                "T" -> RelayType.fromTag(tag[1])?.let { types.add(it) }
                "N" -> tag[1].toIntOrNull()?.let { nips.add(it) }
                "R" -> requirements.add(tag[1])
                "n" -> network = tag[1]
                "t" -> topics.add(tag[1])
                "g" -> geohash = tag[1]
                "rtt-open" -> rttOpen = tag[1].toIntOrNull()
                "rtt-read" -> rttRead = tag[1].toIntOrNull()
                "rtt-write" -> rttWrite = tag[1].toIntOrNull()
                "l" -> {
                    // l-tags carry labeled metadata: ["l", value, namespace]
                    if (tag.size >= 3) {
                        val value = tag[1]
                        val ns = tag[2]
                        when {
                            ns == "countryCode" || ns.contains("countryCode") -> countryCode = value.uppercase()
                            ns.contains("isp") -> isp = value
                            ns == "host.as" -> asNumber = value
                            ns == "host.asn" -> asName = value
                        }
                    }
                }
            }
        }

        return RelayDiscoveryEvent(
            relayUrl = relayUrl,
            monitorPubkey = event.pubKey,
            createdAt = event.createdAt,
            relayTypes = types,
            supportedNips = nips,
            requirements = requirements,
            network = network,
            rttOpen = rttOpen,
            rttRead = rttRead,
            rttWrite = rttWrite,
            topics = topics,
            geohash = geohash,
            nip11Content = event.content.takeIf { it.isNotBlank() },
            countryCode = countryCode,
            isp = isp,
            asNumber = asNumber,
            asName = asName
        )
    }

    /**
     * Parse a kind 10166 event into a RelayMonitorAnnouncement.
     */
    private fun parseMonitorAnnouncement(event: Event): RelayMonitorAnnouncement? {
        var frequency = 3600
        val checks = mutableListOf<String>()
        val timeouts = mutableMapOf<String, Int>()
        var geohash: String? = null

        event.tags.forEach { tag ->
            if (tag.size < 2) return@forEach
            when (tag[0]) {
                "frequency" -> tag[1].toIntOrNull()?.let { frequency = it }
                "c" -> checks.add(tag[1])
                "g" -> geohash = tag[1]
                "timeout" -> {
                    if (tag.size >= 3) {
                        val testType = tag[1]
                        tag[2].toIntOrNull()?.let { timeouts[testType] = it }
                    }
                }
            }
        }

        return RelayMonitorAnnouncement(
            pubkey = event.pubKey,
            frequencySeconds = frequency,
            checks = checks,
            timeouts = timeouts,
            geohash = geohash
        )
    }

    // ── Aggregation ──

    /**
     * Aggregate multiple monitor events for the same relay into a single DiscoveredRelay.
     * Takes the union of types, NIPs, requirements, and averages RTT values.
     */
    private fun aggregateDiscoveryEvents(
        events: List<RelayDiscoveryEvent>
    ): Map<String, DiscoveredRelay> {
        return events.groupBy { normalizeUrl(it.relayUrl) }
            .mapValues { (url, relayEvents) ->
                val types = relayEvents.flatMap { it.relayTypes }.toSet()
                val nips = relayEvents.flatMap { it.supportedNips }.toSet()
                val reqs = relayEvents.flatMap { it.requirements }.toSet()
                val topics = relayEvents.flatMap { it.topics }.toSet()
                val network = relayEvents.mapNotNull { it.network }.firstOrNull()
                val nip11 = relayEvents.mapNotNull { it.nip11Content }.firstOrNull()
                val lastSeen = relayEvents.maxOf { it.createdAt }
                val monitorPubkeys = relayEvents.map { it.monitorPubkey }.distinct().toSet()

                // l-tag metadata: take first non-null from any monitor
                val countryCode = relayEvents.mapNotNull { it.countryCode }.firstOrNull()
                val ispValue = relayEvents.mapNotNull { it.isp }.firstOrNull()

                val avgRttOpen = relayEvents.mapNotNull { it.rttOpen }.takeIf { it.isNotEmpty() }
                    ?.let { it.sum() / it.size }
                val avgRttRead = relayEvents.mapNotNull { it.rttRead }.takeIf { it.isNotEmpty() }
                    ?.let { it.sum() / it.size }
                val avgRttWrite = relayEvents.mapNotNull { it.rttWrite }.takeIf { it.isNotEmpty() }
                    ?.let { it.sum() / it.size }

                // Parse NIP-11 JSON content for structured fields
                var software: String? = null
                var version: String? = null
                var relayName: String? = null
                var description: String? = null
                var icon: String? = null
                var banner: String? = null
                var paymentRequired = false
                var authRequired = false
                var restrictedWrites = false
                var hasNip11 = false
                var operatorPubkey: String? = null
                var nip11Nips = emptySet<Int>()

                if (nip11 != null) {
                    try {
                        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(nip11)
                        val obj = parsed as? kotlinx.serialization.json.JsonObject
                        if (obj != null && obj.isNotEmpty()) {
                            hasNip11 = true
                            software = obj["software"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            version = obj["version"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            relayName = obj["name"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            description = obj["description"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            icon = obj["icon"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            banner = obj["banner"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            operatorPubkey = obj["pubkey"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }

                            val limitation = obj["limitation"] as? kotlinx.serialization.json.JsonObject
                            if (limitation != null) {
                                paymentRequired = (limitation["payment_required"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                                authRequired = (limitation["auth_required"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                                restrictedWrites = (limitation["restricted_writes"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                            }

                            nip11Nips = obj["supported_nips"]
                                ?.let { it as? kotlinx.serialization.json.JsonArray }
                                ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
                                ?.toSet() ?: emptySet()
                        }
                    } catch (_: Exception) { /* malformed NIP-11 JSON */ }
                }

                // Merge NIPs from N tags and NIP-11 content
                val allNips = if (nips.isNotEmpty()) nips + nip11Nips else nip11Nips

                // Fallback heuristic: infer type from supported NIPs when T tags are absent
                val effectiveTypes = if (types.isEmpty()) {
                    val inferred = mutableSetOf<RelayType>()
                    if (allNips.isNotEmpty()) {
                        if (50 in allNips) inferred.add(RelayType.SEARCH)
                        if (65 in allNips && (1 in allNips || 2 in allNips)) inferred.add(RelayType.PUBLIC_OUTBOX)
                        if (4 in allNips || 44 in allNips) inferred.add(RelayType.PUBLIC_INBOX)
                        if (96 in allNips) inferred.add(RelayType.BLOB)
                        if (inferred.isEmpty() && (1 in allNips || 2 in allNips)) inferred.add(RelayType.PUBLIC_OUTBOX)
                    }
                    inferred
                } else types

                DiscoveredRelay(
                    url = url,
                    types = effectiveTypes,
                    supportedNips = allNips,
                    requirements = reqs,
                    network = network,
                    avgRttOpen = avgRttOpen,
                    avgRttRead = avgRttRead,
                    avgRttWrite = avgRttWrite,
                    topics = topics,
                    monitorCount = monitorPubkeys.size,
                    lastSeen = lastSeen,
                    nip11Json = nip11,
                    software = software,
                    version = version,
                    name = relayName,
                    description = description,
                    icon = icon,
                    banner = banner,
                    paymentRequired = paymentRequired,
                    authRequired = authRequired,
                    restrictedWrites = restrictedWrites,
                    hasNip11 = hasNip11,
                    operatorPubkey = operatorPubkey,
                    countryCode = countryCode,
                    isp = ispValue,
                    seenByMonitors = monitorPubkeys
                )
            }
    }

    // ── Persistence ──

    private fun saveToDisk(relays: Map<String, DiscoveredRelay>) {
        try {
            // Serialize as a simple list of relay entries
            val entries = relays.values.map { relay ->
                mapOf(
                    "url" to relay.url,
                    "types" to relay.types.map { it.tag },
                    "nips" to relay.supportedNips.toList(),
                    "reqs" to relay.requirements.toList(),
                    "network" to (relay.network ?: ""),
                    "rttOpen" to (relay.avgRttOpen ?: -1),
                    "rttRead" to (relay.avgRttRead ?: -1),
                    "rttWrite" to (relay.avgRttWrite ?: -1),
                    "topics" to relay.topics.toList(),
                    "monitors" to relay.monitorCount,
                    "lastSeen" to relay.lastSeen,
                    "software" to (relay.software ?: ""),
                    "version" to (relay.version ?: ""),
                    "name" to (relay.name ?: ""),
                    "description" to (relay.description ?: ""),
                    "icon" to (relay.icon ?: ""),
                    "banner" to (relay.banner ?: ""),
                    "paymentRequired" to relay.paymentRequired,
                    "authRequired" to relay.authRequired,
                    "restrictedWrites" to relay.restrictedWrites,
                    "hasNip11" to relay.hasNip11,
                    "operatorPubkey" to (relay.operatorPubkey ?: ""),
                    "countryCode" to (relay.countryCode ?: ""),
                    "isp" to (relay.isp ?: ""),
                    "seenByMonitors" to relay.seenByMonitors.toList()
                )
            }
            val json = JSON.encodeToString(entries)
            prefs?.edit()
                ?.putString(CACHE_KEY_RELAYS, json)
                ?.putLong(CACHE_KEY_TIMESTAMP, System.currentTimeMillis())
                ?.apply()
            Log.d(TAG, "Saved ${relays.size} discovered relays to disk")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save discovery cache: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromDisk() {
        try {
            val json = prefs?.getString(CACHE_KEY_RELAYS, null) ?: return
            val lastFetch = prefs?.getLong(CACHE_KEY_TIMESTAMP, 0L) ?: 0L
            if (System.currentTimeMillis() - lastFetch > CACHE_EXPIRY_MS) {
                Log.d(TAG, "Discovery cache expired, will re-fetch")
                return
            }

            val entries: List<Map<String, Any>> = JSON.decodeFromString(json)
            val relays = mutableMapOf<String, DiscoveredRelay>()

            for (entry in entries) {
                val url = entry["url"] as? String ?: continue
                val typeStrings = (entry["types"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val types = typeStrings.mapNotNull { RelayType.fromTag(it) }.toSet()
                val nips = (entry["nips"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet() ?: emptySet()
                val reqs = (entry["reqs"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                val network = (entry["network"] as? String)?.takeIf { it.isNotBlank() }
                val rttOpen = (entry["rttOpen"] as? Number)?.toInt()?.takeIf { it >= 0 }
                val rttRead = (entry["rttRead"] as? Number)?.toInt()?.takeIf { it >= 0 }
                val rttWrite = (entry["rttWrite"] as? Number)?.toInt()?.takeIf { it >= 0 }
                val topics = (entry["topics"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                val monitors = (entry["monitors"] as? Number)?.toInt() ?: 0
                val lastSeen = (entry["lastSeen"] as? Number)?.toLong() ?: 0L

                val softwareVal = (entry["software"] as? String)?.takeIf { it.isNotBlank() }
                val versionVal = (entry["version"] as? String)?.takeIf { it.isNotBlank() }
                val nameVal = (entry["name"] as? String)?.takeIf { it.isNotBlank() }
                val descriptionVal = (entry["description"] as? String)?.takeIf { it.isNotBlank() }
                val iconVal = (entry["icon"] as? String)?.takeIf { it.isNotBlank() }
                val bannerVal = (entry["banner"] as? String)?.takeIf { it.isNotBlank() }
                val paymentRequired = (entry["paymentRequired"] as? Boolean) ?: false
                val authRequired = (entry["authRequired"] as? Boolean) ?: false
                val restrictedWrites = (entry["restrictedWrites"] as? Boolean) ?: false
                val hasNip11 = (entry["hasNip11"] as? Boolean) ?: false
                val operatorPubkey = (entry["operatorPubkey"] as? String)?.takeIf { it.isNotBlank() }
                val countryCode = (entry["countryCode"] as? String)?.takeIf { it.isNotBlank() }
                val ispVal = (entry["isp"] as? String)?.takeIf { it.isNotBlank() }
                val seenByMonitors = (entry["seenByMonitors"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()

                relays[url] = DiscoveredRelay(
                    url = url,
                    types = types,
                    supportedNips = nips,
                    requirements = reqs,
                    network = network,
                    avgRttOpen = rttOpen,
                    avgRttRead = rttRead,
                    avgRttWrite = rttWrite,
                    topics = topics,
                    monitorCount = monitors,
                    lastSeen = lastSeen,
                    software = softwareVal,
                    version = versionVal,
                    name = nameVal,
                    description = descriptionVal,
                    icon = iconVal,
                    banner = bannerVal,
                    paymentRequired = paymentRequired,
                    authRequired = authRequired,
                    restrictedWrites = restrictedWrites,
                    hasNip11 = hasNip11,
                    operatorPubkey = operatorPubkey,
                    countryCode = countryCode,
                    isp = ispVal,
                    seenByMonitors = seenByMonitors
                )
            }

            if (relays.isNotEmpty()) {
                _discoveredRelays.value = relays
                _hasFetched.value = true
                Log.d(TAG, "Loaded ${relays.size} discovered relays from disk cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load discovery cache: ${e.message}")
        }
    }

    /**
     * Clear all cached data (e.g. on logout).
     */
    fun clear() {
        fetchHandle?.cancel()
        monitorFetchHandle?.cancel()
        fetchHandle = null
        monitorFetchHandle = null
        _discoveredRelays.value = emptyMap()
        _monitors.value = emptyList()
        _hasFetched.value = false
        _isLoading.value = false
        prefs?.edit()?.clear()?.apply()
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().removeSuffix("/").lowercase()
    }
}
