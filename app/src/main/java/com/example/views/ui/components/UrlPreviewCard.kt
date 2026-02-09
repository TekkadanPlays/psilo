package com.example.views.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.views.data.UrlPreviewInfo

/** Size for inline thumbnail in note cards (top-right). */
val URL_PREVIEW_THUMBNAIL_DP = 76.dp

/** Smaller thumbnail when used inside Kind1LinkEmbedBlock. */
private val KIND1_EMBED_THUMBNAIL_DP = 64.dp

/**
 * Small kind-1 link embed at top of note: headline, root domain, optional description, link with icon, thumbnail.
 * Transparent background; in thread view shows outline only for separation.
 *
 * @param expandDescriptionInThread When true (thread view), show more description lines.
 * @param inThreadView When true, draw outline only (no background); when false, transparent and borderless.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Kind1LinkEmbedBlock(
    previewInfo: UrlPreviewInfo,
    expandDescriptionInThread: Boolean = false,
    inThreadView: Boolean = false,
    onUrlClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    onUrlClick(previewInfo.url)
                    uriHandler.openUri(previewInfo.url)
                }
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = if (inThreadView) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: headline, root domain (no counts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (previewInfo.title.isNotEmpty()) {
                        Text(
                            text = previewInfo.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = previewInfo.rootDomain,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (previewInfo.imageUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(KIND1_EMBED_THUMBNAIL_DP)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = previewInfo.imageUrlFullPath,
                            contentDescription = previewInfo.title.ifEmpty { "Link preview" },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Body: description (1-2 lines in feed, more in thread) + link line
            if (previewInfo.description.isNotEmpty() || previewInfo.url.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (previewInfo.description.isNotEmpty()) {
                    Text(
                        text = previewInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expandDescriptionInThread) 6 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = previewInfo.rootDomain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Image-only URL preview thumbnail for note cards: square, rounded, clickable.
 * Use in top-right of note body; no title/siteName block.
 */
@Composable
fun UrlPreviewThumbnail(
    previewInfo: UrlPreviewInfo,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = URL_PREVIEW_THUMBNAIL_DP,
    onUrlClick: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    if (previewInfo.imageUrl.isEmpty()) return
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onUrlClick(previewInfo.url); uriHandler.openUri(previewInfo.url) }
    ) {
        AsyncImage(
            model = previewInfo.imageUrlFullPath,
            contentDescription = "Link preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Compact, borderless URL preview: edge-to-edge, minimal chrome.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UrlPreviewCard(
    previewInfo: UrlPreviewInfo,
    modifier: Modifier = Modifier,
    onUrlClick: (String) -> Unit = {},
    onUrlLongClick: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    onUrlClick(previewInfo.url)
                    uriHandler.openUri(previewInfo.url)
                },
                onLongClick = {
                    showMenu = true
                    onUrlLongClick(previewInfo.url)
                }
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (previewInfo.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = previewInfo.imageUrlFullPath,
                    contentDescription = previewInfo.title.ifEmpty { "Preview" },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (previewInfo.siteName.isNotEmpty()) {
                    Text(
                        text = previewInfo.siteName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (previewInfo.title.isNotEmpty()) {
                    Text(
                        text = previewInfo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = previewInfo.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(previewInfo.url)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy URL",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("URL Options") },
            text = { Text(previewInfo.url) },
            confirmButton = {
                TextButton(
                    onClick = {
                        uriHandler.openUri(previewInfo.url)
                        showMenu = false
                    }
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(previewInfo.url))
                        showMenu = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UrlPreviewCardPreview() {
    MaterialTheme {
        UrlPreviewCard(
            previewInfo = UrlPreviewInfo(
                url = "https://example.com",
                title = "Example Website",
                description = "Description.",
                imageUrl = "https://picsum.photos/400/200",
                siteName = "example.com"
            )
        )
    }
}
