package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.model.AppTab
import com.example.ui.screens.BcGameTerminalScreen
import com.example.ui.screens.ScannerHistoryScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.theme.AiCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SignalTradeTheme
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.TradingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            SignalTradeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SignalTradeApp()
                }
            }
        }
    }
}

@Composable
private fun SignalTradeApp(
    viewModel: TradingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Return to primary Signals screen when pressing system back on secondary tabs
    BackHandler(enabled = state.activeTab != AppTab.SIGNALS) {
        viewModel.selectTab(AppTab.SIGNALS)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        if (useNavigationRail) {
            Scaffold(
                containerColor = ObsidianBackground,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = tab == state.activeTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AiCyan,
                                    selectedTextColor = AiCyan,
                                    indicatorColor = ObsidianSurfaceElevated,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ActiveTabContent(state = state, viewModel = viewModel)
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = ObsidianBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = tab == state.activeTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AiCyan,
                                    selectedTextColor = AiCyan,
                                    indicatorColor = Color(0xFF0C4A6E).copy(alpha = 0.55f),
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ActiveTabContent(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    state: com.example.viewmodel.TradingUiState,
    viewModel: TradingViewModel
) {
    when (state.activeTab) {
        AppTab.SIGNALS -> SignalsScreen(
            state = state,
            onSelectPair = viewModel::selectPair,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSubmitCustomSymbol = viewModel::selectCustomSymbol,
            onSelectTimeframe = viewModel::selectTimeframe,
            onRefreshMarket = viewModel::refreshMarketNow,
            onAnalyzeWithAi = viewModel::analyzeCurrentMarketWithAi,
            onOpenBcGameTerminal = { viewModel.selectTab(AppTab.BC_TERMINAL) }
        )

        AppTab.BC_TERMINAL -> BcGameTerminalScreen(
            visionSignalState = state.bcVisionSignalState,
            onAnalyzeSnapshot = viewModel::analyzeBcGameWebViewSnapshot,
            onDismissOverlay = viewModel::dismissBcVisionOverlay
        )

        AppTab.SCANNER_HISTORY -> ScannerHistoryScreen(
            scannerState = state.scannerState,
            signalHistory = state.signalHistory,
            timeframeLabel = state.selectedTimeframe.label,
            onRunScanner = viewModel::runMultiCoinAiScanner,
            onInspectPair = viewModel::inspectPairFromScanner
        )
    }
}

@Composable
private fun tabLabel(tab: AppTab): String = when (tab) {
    AppTab.SIGNALS -> stringResource(R.string.nav_signals)
    AppTab.BC_TERMINAL -> stringResource(R.string.nav_bc_terminal)
    AppTab.SCANNER_HISTORY -> stringResource(R.string.nav_scanner)
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.SIGNALS -> if (selected) Icons.Filled.CandlestickChart else Icons.Outlined.CandlestickChart
    AppTab.BC_TERMINAL -> if (selected) Icons.Filled.Language else Icons.Outlined.Language
    AppTab.SCANNER_HISTORY -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
}
