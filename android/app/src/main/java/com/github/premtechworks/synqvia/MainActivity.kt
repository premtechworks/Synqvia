package com.github.premtechworks.synqvia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import com.github.premtechworks.synqvia.ui.util.TabLatencyTracker
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.ui.BottomBarGlassStyle
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.MainTab
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.ClipDetailOverlay
import com.github.premtechworks.synqvia.ui.components.LocalBottomBarHeight
import androidx.compose.runtime.DisposableEffect
import com.github.premtechworks.synqvia.ui.components.SynqviaMark
import com.github.premtechworks.synqvia.ui.isBlurSupported
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.LocalReduceMotion
import com.github.premtechworks.synqvia.ui.motion.bouncySpring
import com.github.premtechworks.synqvia.ui.motion.isSystemReduceMotionEnabled
import com.github.premtechworks.synqvia.ui.motion.rememberSynqviaHaptics
import com.github.premtechworks.synqvia.ui.motion.softSpring
import com.github.premtechworks.synqvia.ui.screens.DashboardScreen
import com.github.premtechworks.synqvia.ui.screens.HistoryScreen
import com.github.premtechworks.synqvia.ui.screens.SettingsScreen
import com.github.premtechworks.synqvia.ui.screens.SetupScreen
import com.github.premtechworks.synqvia.ui.CardGlassStyle
import com.github.premtechworks.synqvia.ui.GlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.rememberBottomBarGlassStyle
import com.github.premtechworks.synqvia.ui.rememberCardGlassStyle
import com.github.premtechworks.synqvia.ui.rememberGlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.synqviaGlass
import com.github.premtechworks.synqvia.ui.components.SynqviaLightDarkPreview
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.synqviaCardShadow
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.github.premtechworks.synqvia.ui.AppRoutes
import com.github.premtechworks.synqvia.ui.screens.ImeSettingsScreen
import com.github.premtechworks.synqvia.ui.screens.OnboardingScreen
import com.github.premtechworks.synqvia.ui.screens.PairScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as SynqviaApp
        MainViewModel.Factory(app, app.container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!viewModel.isUserStopped) {
            viewModel.reconnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isReady = false
        val startTime = android.os.SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            !isReady && (android.os.SystemClock.uptimeMillis() - startTime < 500L)
        }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            if (isSystemReduceMotionEnabled(this)) {
                splashScreenViewProvider.remove()
            } else {
                val alpha = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.ALPHA, 1f, 0f).apply {
                    duration = 150L
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                }
                alpha.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                    }
                })
                alpha.start()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        requestInitialPermissions()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentRoute by viewModel.currentRoute.collectAsState()

            SynqviaTheme(
                themeMode = themeMode
            ) {
                DisposableEffect(Unit) {
                    isReady = true
                    onDispose {}
                }
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "root_navigation"
                ) { route ->
                    when (route) {
                        AppRoutes.ONBOARDING -> {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onNavigateToPair = { viewModel.navigateTo(AppRoutes.PAIR) }
                            )
                        }
                        AppRoutes.PAIR -> {
                            BackHandler {
                                viewModel.navigateBack()
                            }
                            PairScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() },
                                onNavigateToMain = { viewModel.navigateTo(AppRoutes.MAIN) }
                            )
                        }
                        AppRoutes.IME_SETTINGS -> {
                            BackHandler {
                                viewModel.navigateBack()
                            }
                            ImeSettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        else -> {
                            MainAppContent(viewModel = viewModel)
                        }
                    }
                }
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
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val reduceMotionFollowSystem by viewModel.reduceMotionFollowSystem.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedback.collectAsState()
    val selectedClipForDetail by viewModel.selectedClipForDetail.collectAsState()

    val isBlurSupported = remember(context) { isBlurSupported(context) }
    val hazeState = rememberHazeState()
    val isSystemReduceMotion = remember(context) { isSystemReduceMotionEnabled(context) }
    val reduceMotion = reduceMotionFollowSystem && isSystemReduceMotion
    val appHaptics = rememberSynqviaHaptics(userEnabled = hapticFeedbackEnabled)

    // Hoisted scroll & list states across tab transitions
    val dashboardScrollState = rememberScrollState()
    val historyListState = rememberLazyListState()
    val setupScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    val undoDeleteEvent by viewModel.undoDeleteEvent.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(undoDeleteEvent) {
        undoDeleteEvent?.let { event ->
            coroutineScope.launch {
                val snackJob = launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.message} — Undo",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.clipId)
                    }
                }
                delay(6000L)
                snackJob.cancel()
                viewModel.clearUndoDeleteEvent()
            }
        }
    }

    BackHandler(enabled = selectedTab != MainTab.SYNC) {
        viewModel.setTab(MainTab.SYNC)
    }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalIsBlurSupported provides isBlurSupported,
        LocalReduceMotion provides reduceMotion,
        LocalAppHaptics provides appHaptics
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SynqviaTheme.colors.bgTop,
                            SynqviaTheme.colors.bgBottom
                        )
                    )
                )
        ) {
            val isWideScreen = maxWidth >= 600.dp
            val density = LocalDensity.current
            val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            var measuredBottomBarHeight by remember { mutableStateOf(56.dp + navBarBottomInset) }

            CompositionLocalProvider(
                LocalBottomBarHeight provides if (isWideScreen) 0.dp else measuredBottomBarHeight
            ) {
                if (isWideScreen) {
                    // Adaptive Expanded Layout (Tablets / Foldables / Desktop)
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavigationRail(
                            containerColor = SynqviaTheme.colors.surfaceInset,
                            contentColor = SynqviaTheme.colors.textPrimary,
                            modifier = Modifier
                                .border(1.dp, SynqviaTheme.colors.divider, RoundedCornerShape(0.dp))
                                .testTag("navigation_rail")
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SynqviaMark(
                                size = 32.dp,
                                contentDescription = "Synqvia",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            NavRailTabItem(
                                selected = selectedTab == MainTab.SYNC,
                                outlinedIcon = Icons.Outlined.Home,
                                filledIcon = Icons.Filled.Home,
                                label = "Sync",
                                onClick = {
                                    TabLatencyTracker.onTabClicked(MainTab.SYNC)
                                    viewModel.setTab(MainTab.SYNC)
                                },
                                testTag = "rail_tab_sync"
                            )
                            NavRailTabItem(
                                selected = selectedTab == MainTab.HISTORY,
                                outlinedIcon = Icons.Outlined.History,
                                filledIcon = Icons.Filled.History,
                                label = "History",
                                onClick = {
                                    TabLatencyTracker.onTabClicked(MainTab.HISTORY)
                                    viewModel.setTab(MainTab.HISTORY)
                                },
                                testTag = "rail_tab_history"
                            )
                            NavRailTabItem(
                                selected = selectedTab == MainTab.SETUP,
                                outlinedIcon = Icons.Outlined.Bolt,
                                filledIcon = Icons.Filled.Bolt,
                                label = "Setup",
                                onClick = {
                                    TabLatencyTracker.onTabClicked(MainTab.SETUP)
                                    viewModel.setTab(MainTab.SETUP)
                                },
                                testTag = "rail_tab_setup"
                            )
                            NavRailTabItem(
                                selected = selectedTab == MainTab.SETTINGS,
                                outlinedIcon = Icons.Outlined.Settings,
                                filledIcon = Icons.Filled.Settings,
                                label = "Settings",
                                onClick = {
                                    TabLatencyTracker.onTabClicked(MainTab.SETTINGS)
                                    viewModel.setTab(MainTab.SETTINGS)
                                },
                                testTag = "rail_tab_settings"
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            TabContent(
                                selectedTab = selectedTab,
                                viewModel = viewModel,
                                dashboardScrollState = dashboardScrollState,
                                historyListState = historyListState,
                                setupScrollState = setupScrollState,
                                settingsScrollState = settingsScrollState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Mobile Compact Layout: edge-to-edge root Box, tab content fills whole window
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Tab content fills whole window and scrolls behind bottom bar
                        TabContent(
                            selectedTab = selectedTab,
                            viewModel = viewModel,
                            dashboardScrollState = dashboardScrollState,
                            historyListState = historyListState,
                            setupScrollState = setupScrollState,
                            settingsScrollState = settingsScrollState,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Bottom bar is an overlay aligned to the bottom with frosted glass
                        SynqviaBottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.setTab(it) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { size ->
                                    val hDp = with(density) { size.height.toDp() }
                                    if (hDp > 0.dp) {
                                        measuredBottomBarHeight = hDp
                                    }
                                }
                        )

                        // Frosted Glass SnackbarHost floating above the bottom bar
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (measuredBottomBarHeight > 0.dp) measuredBottomBarHeight + 8.dp else 16.dp)
                                .padding(horizontal = 16.dp),
                            snackbar = { data ->
                                val cardGlassStyle = rememberCardGlassStyle()
                                val borderBrush = rememberGlassCardBorderBrush()
                                val colors = SynqviaTheme.colors
                                val isDark = SynqviaTheme.isDark
                                val snackbarShape = RoundedCornerShape(14.dp)

                                val snackbarModifier = if (isDark) {
                                    Modifier
                                        .clip(snackbarShape)
                                        .synqviaGlass(
                                            hazeState = hazeState,
                                            style = cardGlassStyle,
                                            isBlurSupported = isBlurSupported,
                                            fallbackColor = colors.surfaceHigh
                                        )
                                        .border(1.dp, borderBrush, snackbarShape)
                                } else {
                                    Modifier
                                        .synqviaCardShadow(elevation = 6.dp, shape = snackbarShape)
                                        .clip(snackbarShape)
                                        .background(Color(0xFF0B1B33).copy(alpha = 0.92f))
                                        .border(1.dp, Color(0xFF0B1B33).copy(alpha = 0.12f), snackbarShape)
                                }

                                val textColor = if (isDark) colors.textPrimary else Color.White
                                val actionColor = if (isDark) colors.primary else Color(0xFF9CC4FF)

                                Box(
                                    modifier = snackbarModifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = data.visuals.message,
                                            style = SynqviaType.Body,
                                            color = textColor
                                        )
                                        data.visuals.actionLabel?.let { actionLabel ->
                                            Text(
                                                text = actionLabel,
                                                style = SynqviaType.Headline.copy(color = actionColor),
                                                modifier = Modifier
                                                    .clickable { data.performAction() }
                                                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // IN-COMPOSITION overlay at the very top of the root (above headers and bottom bar)
                selectedClipForDetail?.let { clip ->
                    ClipDetailOverlay(
                        clip = clip,
                        onDismiss = { viewModel.selectClipForDetail(null) },
                        onCopy = { viewModel.copyToClipboardOnly(it) },
                        onShare = { text ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
                        },
                        onTogglePin = { id, pinned -> viewModel.togglePin(id, pinned) },
                        onDelete = { id -> viewModel.deleteClip(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: MainTab,
    viewModel: MainViewModel,
    dashboardScrollState: ScrollState,
    historyListState: LazyListState,
    setupScrollState: ScrollState,
    settingsScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 16.dp.roundToPx() }

    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            if (reduceMotion) {
                (fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120)))
                    .apply { targetContentZIndex = 1f }
            } else {
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                ) { slideOffsetPx * direction } + fadeIn(
                    animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                )
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                ) { -slideOffsetPx * direction } + fadeOut(
                    animationSpec = tween(durationMillis = 140, easing = DecelerateEasing)
                )
                (enter togetherWith exit).apply { targetContentZIndex = 1f }
            }
        },
        label = "tab_content_transition",
        modifier = modifier
    ) { currentTab ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    TabLatencyTracker.onTabDrawn(currentTab)
                }
        ) {
            when (currentTab) {
                MainTab.SYNC -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateTab = { viewModel.setTab(it) },
                    onOpenPair = { viewModel.navigateTo(AppRoutes.PAIR) },
                    scrollState = dashboardScrollState
                )
                MainTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    lazyListState = historyListState,
                    onNavigateTab = { viewModel.setTab(it) }
                )
                MainTab.SETUP -> SetupScreen(
                    viewModel = viewModel,
                    scrollState = setupScrollState,
                    onOpenImeSettings = { viewModel.navigateTo(AppRoutes.IME_SETTINGS) }
                )
                MainTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    scrollState = settingsScrollState
                )
            }
        }
    }
}


/**
 * Bottom navigation bar with real frosted glass material (blur 24dp, tint #0B1426 @ 72%),
 * 0.5dp top hairline (white 10%), and ONE shared selection pill sliding with soft spring.
 */
@Composable
fun SynqviaBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isBlurSupported = LocalIsBlurSupported.current
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalAppHaptics.current

    val colors = SynqviaTheme.colors

    val navBarGlassModifier = Modifier.synqviaGlass(
        hazeState = hazeState,
        style = rememberBottomBarGlassStyle(),
        isBlurSupported = isBlurSupported,
        fallbackColor = colors.navBarBg
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(navBarGlassModifier)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Top hairline: 0.5dp white 10% in dark, 1dp #E1E9F5 in light
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (SynqviaTheme.isDark) 0.5.dp else 1.dp)
                .background(if (SynqviaTheme.isDark) Color.White.copy(alpha = 0.10f) else colors.outline)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = 4
            val tabWidth = totalWidth / tabCount
            val pillWidth = 64.dp
            val density = LocalDensity.current

            // ONE shared selection pill that slides between items with the soft spring
            val tabWidthPx = with(density) { tabWidth.toPx() }
            val pillWidthPx = with(density) { pillWidth.toPx() }
            val targetOffsetXPx = tabWidthPx * selectedTab.ordinal + (tabWidthPx - pillWidthPx) / 2f

            val pillTranslationX = remember { Animatable(targetOffsetXPx) }
            LaunchedEffect(targetOffsetXPx) {
                if (reduceMotion) {
                    pillTranslationX.snapTo(targetOffsetXPx)
                } else {
                    pillTranslationX.animateTo(
                        targetValue = targetOffsetXPx,
                        animationSpec = softSpring()
                    )
                }
            }

            // Shared sliding selection pill
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(40.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = pillTranslationX.value
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.navPillBg)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarTabItem(
                    selected = selectedTab == MainTab.SYNC,
                    outlinedIcon = Icons.Outlined.Home,
                    filledIcon = Icons.Filled.Home,
                    label = "Sync",
                    onClick = {
                        if (selectedTab != MainTab.SYNC) {
                            haptics.tick()
                            TabLatencyTracker.onTabClicked(MainTab.SYNC)
                            onTabSelected(MainTab.SYNC)
                        }
                    },
                    testTag = "nav_tab_sync",
                    modifier = Modifier.weight(1f)
                )
                NavBarTabItem(
                    selected = selectedTab == MainTab.HISTORY,
                    outlinedIcon = Icons.Outlined.History,
                    filledIcon = Icons.Filled.History,
                    label = "History",
                    onClick = {
                        if (selectedTab != MainTab.HISTORY) {
                            haptics.tick()
                            TabLatencyTracker.onTabClicked(MainTab.HISTORY)
                            onTabSelected(MainTab.HISTORY)
                        }
                    },
                    testTag = "nav_tab_history",
                    modifier = Modifier.weight(1f)
                )
                NavBarTabItem(
                    selected = selectedTab == MainTab.SETUP,
                    outlinedIcon = Icons.Outlined.Bolt,
                    filledIcon = Icons.Filled.Bolt,
                    label = "Setup",
                    onClick = {
                        if (selectedTab != MainTab.SETUP) {
                            haptics.tick()
                            TabLatencyTracker.onTabClicked(MainTab.SETUP)
                            onTabSelected(MainTab.SETUP)
                        }
                    },
                    testTag = "nav_tab_setup",
                    modifier = Modifier.weight(1f)
                )
                NavBarTabItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    outlinedIcon = Icons.Outlined.Settings,
                    filledIcon = Icons.Filled.Settings,
                    label = "Settings",
                    onClick = {
                        if (selectedTab != MainTab.SETTINGS) {
                            haptics.tick()
                            TabLatencyTracker.onTabClicked(MainTab.SETTINGS)
                            onTabSelected(MainTab.SETTINGS)
                        }
                    },
                    testTag = "nav_tab_settings",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarTabItem(
    selected: Boolean,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val colors = SynqviaTheme.colors

    // Outlined -> filled icon swap with a scale bounce (0.85 -> 1.12 -> 1)
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            if (!reduceMotion) {
                iconScale.snapTo(0.85f)
                iconScale.animateTo(1.12f, tween(120, easing = DecelerateEasing))
                iconScale.animateTo(1.0f, bouncySpring())
            } else {
                iconScale.snapTo(1f)
            }
        } else {
            iconScale.snapTo(1f)
        }
    }

    // Label color crossfades over 150ms
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.textSecondary,
        animationSpec = tween(durationMillis = 150),
        label = "tab_label_color"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.textSecondary,
        animationSpec = tween(durationMillis = 150),
        label = "tab_icon_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .semantics { contentDescription = "Navigate to $label tab" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .height(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) filledIcon else outlinedIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = if (selected) SynqviaType.Overline.copy(fontWeight = FontWeight.SemiBold) else SynqviaType.Overline.copy(fontWeight = FontWeight.Medium),
                color = labelColor
            )
        }
    }
}

@Composable
private fun NavRailTabItem(
    selected: Boolean,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val haptics = LocalAppHaptics.current
    val reduceMotion = LocalReduceMotion.current
    val colors = SynqviaTheme.colors
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && !reduceMotion) {
            iconScale.snapTo(0.85f)
            iconScale.animateTo(1.12f, tween(120, easing = DecelerateEasing))
            iconScale.animateTo(1.0f, bouncySpring())
        } else {
            iconScale.snapTo(1f)
        }
    }

    NavigationRailItem(
        selected = selected,
        onClick = {
            if (!selected) {
                haptics.tick()
                onClick()
            }
        },
        icon = {
            Icon(
                imageVector = if (selected) filledIcon else outlinedIcon,
                contentDescription = label,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
            )
        },
        label = {
            Text(
                text = label,
                style = if (selected) SynqviaType.Overline.copy(fontWeight = FontWeight.SemiBold) else SynqviaType.Overline.copy(fontWeight = FontWeight.Medium)
            )
        },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            unselectedIconColor = colors.textSecondary,
            unselectedTextColor = colors.textSecondary,
            indicatorColor = colors.navPillBg
        ),
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = "Navigate to $label tab" }
    )
}

@SynqviaLightDarkPreview
@Composable
fun PreviewSynqviaBottomNavBar() {
    SynqviaTheme {
        SynqviaBottomNavBar(
            selectedTab = MainTab.SYNC,
            onTabSelected = {}
        )
    }
}
