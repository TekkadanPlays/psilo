package com.example.views.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSidebar(
    drawerState: DrawerState,
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                SidebarContent(
                    onItemClick = { itemId ->
                        onItemClick(itemId)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        },
        modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun SidebarContent(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        // User profile section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary // Use theme color
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "John Doe",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = "@johndoe",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Navigation items using proper NavigationDrawerItem
        getModernMenuItems().forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.title) },
                selected = false,
                icon = { Icon(item.icon, contentDescription = item.title) },
                badge = item.badge?.let { { Text(it) } },
                onClick = { onItemClick(item.id) }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}


private data class ModernSidebarMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val badge: String? = null,
    val hasArrow: Boolean = false
)

private fun getModernMenuItems(): List<ModernSidebarMenuItem> = listOf(
    ModernSidebarMenuItem("home", "Home", Icons.Default.Home),
    ModernSidebarMenuItem("bookmarks", "Bookmarks", Icons.Default.Star),
    ModernSidebarMenuItem("lists", "Lists", Icons.AutoMirrored.Filled.List),
    ModernSidebarMenuItem("bug_report", "Report a Bug", Icons.Outlined.BugReport)
)

@Preview(showBackground = true)
@Composable
fun ModernSidebarPreview() {
    MaterialTheme {
        ModernSidebar(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            onItemClick = {}
        ) {
            // Empty content for preview
        }
    }
}
