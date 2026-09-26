package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MyClipSyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as MyClipSyncApp
        MainViewModel.Factory(app, app.container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.reconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestInitialPermissions()

        setContent {
            MyClipSyncTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshImeStatus()
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    BackHandler(enabled = selectedTab != MainTab.SYNC) {
        viewModel.setTab(MainTab.SYNC)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkNavyBackground,
                        DarkNavySurface,
                        Color(0xFF070E1A)
                    )
                )
            )
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Adaptive Expanded Layout (Tablets / Foldables / Desktop)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                NavigationRail(
                    containerColor = GlassSurface,
                    contentColor = Color.White,
                    modifier = Modifier
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(0.dp))
                        .testTag("navigation_rail")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MyClipSync",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = CyanPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    NavRailTabItem(
                        selected = selectedTab == MainTab.SYNC,
                        icon = Icons.Default.Sync,
                        label = "Sync",
                        onClick = { viewModel.setTab(MainTab.SYNC) },
                        testTag = "rail_tab_sync"
                    )
                    NavRailTabItem(
                        selected = selectedTab == MainTab.HISTORY,
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = { viewModel.setTab(MainTab.HISTORY) },
                        testTag = "rail_tab_history"
                    )
                    NavRailTabItem(
                        selected = selectedTab == MainTab.SETUP,
                        icon = Icons.Default.Bolt,
                        label = "Setup",
                        onClick = { viewModel.setTab(MainTab.SETUP) },
                        testTag = "rail_tab_setup"
                    )
                    NavRailTabItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { viewModel.setTab(MainTab.SETTINGS) },
                        testTag = "rail_tab_settings"
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    TabContent(
                        selectedTab = selectedTab,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Mobile Compact Layout with Bottom Navigation Bar
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    LiquidGlassBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            ) { innerPadding ->
                TabContent(
                    selectedTab = selectedTab,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: MainTab,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tabTransition",
        modifier = modifier
    ) { tab ->
        when (tab) {
            MainTab.SYNC -> DashboardScreen(
                viewModel = viewModel,
                onNavigateTab = { viewModel.setTab(it) }
            )
            MainTab.HISTORY -> HistoryScreen(
                viewModel = viewModel
            )
            MainTab.SETUP -> SetupScreen(
                viewModel = viewModel
            )
            MainTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun LiquidGlassBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x280D1A30))
            .border(1.dp, Color(0x3500E5FF), RoundedCornerShape(24.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            NavBarTabItem(
                selected = selectedTab == MainTab.SYNC,
                icon = Icons.Default.Sync,
                label = "Sync",
                onClick = { onTabSelected(MainTab.SYNC) },
                testTag = "nav_tab_sync"
            )
            NavBarTabItem(
                selected = selectedTab == MainTab.HISTORY,
                icon = Icons.Default.History,
                label = "History",
                onClick = { onTabSelected(MainTab.HISTORY) },
                testTag = "nav_tab_history"
            )
            NavBarTabItem(
                selected = selectedTab == MainTab.SETUP,
                icon = Icons.Default.Bolt,
                label = "Setup",
                onClick = { onTabSelected(MainTab.SETUP) },
                testTag = "nav_tab_setup"
            )
            NavBarTabItem(
                selected = selectedTab == MainTab.SETTINGS,
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = { onTabSelected(MainTab.SETTINGS) },
                testTag = "nav_tab_settings"
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavBarTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyanPrimary,
            selectedTextColor = CyanPrimary,
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = "Navigate to $label tab" }
    )
}

@Composable
private fun NavRailTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = CyanPrimary,
            selectedTextColor = CyanPrimary,
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = "Navigate to $label tab" }
    )
}
