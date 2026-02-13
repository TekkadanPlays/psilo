package com.example.views.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import com.example.views.ui.components.cutoutPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.views.cache.Nip11CacheManager
import com.example.views.data.DiscoveredRelay
import com.example.views.data.RelayInformation
import com.example.views.relay.LogType
import com.example.views.relay.RelayLogBuffer
import com.example.views.relay.RelayLogEntry
import com.example.views.repository.Nip66RelayDiscoveryRepository

/** Types we show by default (connection lifecycle + errors/notices). RECEIVED/EOSE/SENT are hidden unless verbose. */
private val RELEVANT_LOG_TYPES = setOf(
    LogType.CONNECTING,
    LogType.CONNECTED,
    LogType.DISCONNECTED,
    LogType.ERROR,
    LogType.NOTICE
)

/**
 * Relay detail screen: NIP-11 info header, NIP-66 discovery data, health stats, and activity log.
 * Logs display newest-first. Inspired by Amethyst's relay detail page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayLogScreen(
    relayUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nip11 = remember(context) { Nip11CacheManager.getInstance(context) }
    var relayInfo by remember(relayUrl) { mutableStateOf<RelayInformation?>(nip11.getCachedRelayInfo(relayUrl)) }
    LaunchedEffect(relayUrl) {
        nip11.getRelayInfo(relayUrl).let { info -> relayInfo = info }
    }

    // NIP-66 discovery data for this relay
    val discoveredRelays by Nip66RelayDiscoveryRepository.discoveredRelays.collectAsState()
    val discoveryData = remember(discoveredRelays, relayUrl) {
        val normalized = relayUrl.lowercase().trimEnd('/')
        discoveredRelays.values.firstOrNull { it.url.lowercase().trimEnd('/') == normalized }
    }

    val allLogs by RelayLogBuffer.getLogsForRelay(relayUrl).collectAsState()
    var showVerbose by remember { mutableStateOf(false) }
    // Newest-first log ordering
    val filteredLogs = remember(allLogs, showVerbose) {
        val logs = if (showVerbose) allLogs else allLogs.filter { it.type in RELEVANT_LOG_TYPES }
        logs.sortedByDescending { it.timestamp }
    }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = discoveryData?.name ?: relayInfo?.name?.take(24) ?: relayUrl.takeAfterLastSlash(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = relayUrl.removePrefix("wss://").removePrefix("ws://"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { RelayLogBuffer.clearLogsForRelay(relayUrl) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Clear logs")
                        }
                    },
                    windowInsets = WindowInsets(0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Relay Info Header ──
            item(key = "relay_header") {
                RelayInfoHeader(relayUrl = relayUrl, info = relayInfo, discovery = discoveryData)
            }

            // ── NIP-66 Discovery Data ──
            if (discoveryData != null) {
                item(key = "nip66_data") {
                    Nip66DataCard(discovery = discoveryData!!)
                }
            }

            // ── Health Stats ──
            item(key = "health_stats") {
                val healthMap by com.example.views.relay.RelayHealthTracker.healthByRelay.collectAsState()
                val health = healthMap[relayUrl]
                if (health != null && health.connectionAttempts > 0) {
                    HealthCard(
                        health = health,
                        relayUrl = relayUrl
                    )
                }
            }

            // ── Activity Log ──
            item(key = "activity_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredLogs.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = showVerbose,
                            onClick = { showVerbose = !showVerbose },
                            label = { Text("Verbose", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                item(key = "empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.History, null,
                                Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (allLogs.isEmpty()) "No activity yet" else "No connection or notice events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { "${it.timestamp}-${it.type}-${it.message.hashCode()}" }) { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

private fun String.takeAfterLastSlash(): String = substringAfterLast('/')

// ── Relay Info Header ──

@Composable
private fun RelayInfoHeader(
    relayUrl: String,
    info: RelayInformation?,
    discovery: DiscoveredRelay?
) {
    val infoImage = discovery?.icon ?: info?.icon ?: info?.image
    val relayName = discovery?.name ?: info?.name
    val description = discovery?.description ?: info?.description

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            // Banner (if available from NIP-66)
            discovery?.banner?.let { bannerUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bannerUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Relay icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (infoImage != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(infoImage)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Outlined.Router, null,
                                Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (relayName != null) {
                        Text(
                            text = relayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = relayUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = desc.take(200).let { if (desc.length > 200) "$it…" else it },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // NIP-11 metadata chips
                    val softwareName = info?.software ?: discovery?.softwareShort
                    if (softwareName != null || info?.supported_nips?.isNotEmpty() == true) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            softwareName?.let { sw ->
                                val label = sw.removePrefix("git+").removePrefix("https://github.com/")
                                    .removeSuffix(".git").substringAfterLast("/").take(20)
                                RelayDetailChip(label, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            info?.version?.takeIf { it.isNotBlank() }?.let { v ->
                                RelayDetailChip("v$v", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── NIP-66 Discovery Data Card ──

@Composable
private fun Nip66DataCard(discovery: DiscoveredRelay) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Discovery",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // Type tags
            if (discovery.types.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    discovery.types.forEach { type ->
                        RelayDetailChip(type.displayName, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RelayStatItem("Monitors", "${discovery.monitorCount}")
                discovery.avgRttOpen?.let { rtt ->
                    val rttColor = when {
                        rtt < 500 -> Color(0xFF66BB6A)
                        rtt < 1000 -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.error
                    }
                    RelayStatItem("Latency", "${rtt}ms", rttColor)
                } ?: RelayStatItem("Latency", "—")
                RelayStatItem("NIPs", "${discovery.supportedNips.size}")
                RelayStatItem("NIP-11", if (discovery.hasNip11) "Yes" else "No")
            }

            // Metadata row
            val hasMetadata = discovery.countryCode != null || discovery.softwareShort != null ||
                discovery.paymentRequired || discovery.authRequired
            if (hasMetadata) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    discovery.countryCode?.let { cc ->
                        val flag = countryCodeToFlag(cc)
                        val name = COUNTRY_NAMES[cc.uppercase()] ?: cc
                        RelayDetailChip("$flag $name", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    discovery.softwareShort?.let { sw ->
                        RelayDetailChip(sw, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    if (discovery.paymentRequired) {
                        RelayDetailChip("Payment Required", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    }
                    if (discovery.authRequired) {
                        RelayDetailChip("Auth Required", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (discovery.restrictedWrites) {
                        RelayDetailChip("Restricted Writes", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // Supported NIPs (compact display)
            if (discovery.supportedNips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Supported NIPs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = discovery.supportedNips.sorted().joinToString(", ") { "NIP-$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Health Card ──

@Composable
private fun HealthCard(
    health: com.example.views.relay.RelayHealthInfo,
    relayUrl: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Health",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RelayStatItem("Connections", "${health.connectionAttempts}")
                RelayStatItem("Failures", "${health.connectionFailures}",
                    if (health.connectionFailures > 0) MaterialTheme.colorScheme.error else null)
                val ratePercent = (health.failureRate * 100).toInt()
                RelayStatItem("Fail Rate", "$ratePercent%",
                    if (ratePercent > 30) MaterialTheme.colorScheme.error else null)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RelayStatItem("Avg Latency", if (health.avgLatencyMs > 0) "${health.avgLatencyMs}ms" else "—")
                RelayStatItem("Events", "${health.eventsReceived}")
                RelayStatItem("Consec. Fails", "${health.consecutiveFailures}",
                    if (health.consecutiveFailures > 2) MaterialTheme.colorScheme.error else null)
            }

            if (health.isFlagged || health.isBlocked) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusText = when {
                        health.isBlocked -> "Blocked"
                        health.isFlagged -> "Flagged — unreliable"
                        else -> ""
                    }
                    val statusColor = if (health.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (health.isBlocked) {
                        TextButton(
                            onClick = { com.example.views.relay.RelayHealthTracker.unblockRelay(relayUrl) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) { Text("Unblock", style = MaterialTheme.typography.labelSmall) }
                    } else if (health.isFlagged) {
                        TextButton(
                            onClick = { com.example.views.relay.RelayHealthTracker.blockRelay(relayUrl) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Block", style = MaterialTheme.typography.labelSmall) }
                        TextButton(
                            onClick = { com.example.views.relay.RelayHealthTracker.unflagRelay(relayUrl) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) { Text("Dismiss", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            if (health.lastError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Last error: ${health.lastError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Log Entry Row ──

@Composable
private fun LogEntryRow(entry: RelayLogEntry) {
    val (iconTint, label) = when (entry.type) {
        LogType.CONNECTING -> MaterialTheme.colorScheme.primary to "CONN"
        LogType.CONNECTED -> Color(0xFF66BB6A) to "OK"
        LogType.DISCONNECTED -> MaterialTheme.colorScheme.outline to "DISC"
        LogType.ERROR -> MaterialTheme.colorScheme.error to "ERR"
        LogType.NOTICE -> MaterialTheme.colorScheme.tertiary to "NOTE"
        LogType.SENT -> MaterialTheme.colorScheme.secondary to "SEND"
        LogType.RECEIVED -> MaterialTheme.colorScheme.onSurfaceVariant to "RECV"
        LogType.EOSE -> MaterialTheme.colorScheme.onSurfaceVariant to "EOSE"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            text = entry.formattedTime(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(68.dp)
        )
        // Type badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.width(42.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = iconTint,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        // Message
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Small Components ──

@Composable
private fun RelayStatItem(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RelayDetailChip(text: String, backgroundColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Convert ISO 3166-1 alpha-2 country code to flag emoji. */
private fun countryCodeToFlag(code: String): String {
    if (code.length != 2) return code
    val first = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private val COUNTRY_NAMES = mapOf(
    "AD" to "Andorra", "AE" to "UAE", "AF" to "Afghanistan", "AR" to "Argentina",
    "AT" to "Austria", "AU" to "Australia", "BE" to "Belgium", "BG" to "Bulgaria",
    "BR" to "Brazil", "CA" to "Canada", "CH" to "Switzerland", "CL" to "Chile",
    "CN" to "China", "CO" to "Colombia", "CZ" to "Czechia", "DE" to "Germany",
    "DK" to "Denmark", "EE" to "Estonia", "ES" to "Spain", "FI" to "Finland",
    "FR" to "France", "GB" to "United Kingdom", "GR" to "Greece", "HK" to "Hong Kong",
    "HR" to "Croatia", "HU" to "Hungary", "ID" to "Indonesia", "IE" to "Ireland",
    "IL" to "Israel", "IN" to "India", "IS" to "Iceland", "IT" to "Italy",
    "JP" to "Japan", "KR" to "South Korea", "LT" to "Lithuania", "LU" to "Luxembourg",
    "LV" to "Latvia", "MX" to "Mexico", "MY" to "Malaysia", "NL" to "Netherlands",
    "NO" to "Norway", "NZ" to "New Zealand", "PH" to "Philippines", "PL" to "Poland",
    "PT" to "Portugal", "RO" to "Romania", "RS" to "Serbia", "RU" to "Russia",
    "SA" to "Saudi Arabia", "SE" to "Sweden", "SG" to "Singapore", "SI" to "Slovenia",
    "SK" to "Slovakia", "TH" to "Thailand", "TR" to "Turkey", "TW" to "Taiwan",
    "UA" to "Ukraine", "US" to "United States", "VN" to "Vietnam", "ZA" to "South Africa"
)
