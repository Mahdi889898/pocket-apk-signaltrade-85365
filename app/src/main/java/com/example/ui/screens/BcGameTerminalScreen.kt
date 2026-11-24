package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.BC_GAME_CONTRACT_URL
import com.example.model.SignalUiState
import com.example.ui.components.AiSignalResultCard
import com.example.ui.theme.AiCyan
import com.example.ui.theme.BuyEmeraldBright
import com.example.ui.theme.MonoTextStyles
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SellCoral
import com.example.ui.theme.SellCoralBright
import com.example.ui.theme.SellCoralContainer
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BcGameTerminalScreen(
    visionSignalState: SignalUiState,
    onAnalyzeSnapshot: (Bitmap, String, String, String) -> Unit,
    onDismissOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(BC_GAME_CONTRACT_URL) }
    var currentTitle by remember { mutableStateOf("BC.Game Contract Trading") }
    var loadProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    fun captureAndAnalyzeWebView() {
        val wv = webViewRef ?: return
        if (wv.width <= 0 || wv.height <= 0) return

        val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        wv.draw(canvas)

        val safeUrl = wv.url ?: currentUrl
        val safeTitle = wv.title ?: currentTitle

        // Safely read visible page text snippet to enrich Gemini vision analysis
        wv.evaluateJavascript(
            "(function(){try{return (document.body && document.body.innerText) ? document.body.innerText.substring(0, 1400) : '';}catch(e){return '';}})();"
        ) { jsResult ->
            val cleanedText = jsResult
                ?.trim()
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                .orEmpty()
            onAnalyzeSnapshot(bitmap, safeUrl, safeTitle, cleanedText)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Browser Control Bar connected to https://bc.game/trading/contract
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = canGoBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("webview_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_web_back),
                    tint = if (canGoBack) TextPrimaryDark else TextSecondaryDark.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = canGoForward,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("webview_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_web_forward),
                    tint = if (canGoForward) TextPrimaryDark else TextSecondaryDark.copy(alpha = 0.4f)
                )
            }

            // URL Pill (LTR)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = BuyEmeraldBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = currentUrl.removePrefix("https://"),
                        style = MonoTextStyles.microAxis,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("webview_reload_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_web_reload),
                    tint = AiCyan
                )
            }

            IconButton(
                onClick = { webViewRef?.loadUrl(BC_GAME_CONTRACT_URL) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("webview_home_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.cd_web_home),
                    tint = BuyEmeraldBright
                )
            }
        }

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = AiCyan,
                trackColor = ObsidianSurface
            )
        }

        // Main WebView Container + Floating AI Scan Result Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bc_game_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#070B14"))
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                mediaPlaybackRequiresUserGesture = true
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Keep HTTPS navigation inside embedded terminal
                                    return !(url.startsWith("https://") || url.startsWith("http://"))
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (!url.isNullOrBlank()) currentUrl = url
                                    currentTitle = view?.title ?: currentTitle
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) currentTitle = title
                                }
                            }

                            loadUrl(BC_GAME_CONTRACT_URL)
                            webViewRef = this
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                    }
                )
            }

            // Expandable AI Vision Overlay over BC.Game page
            androidx.compose.animation.AnimatedVisibility(
                visible = visionSignalState !is SignalUiState.Idle,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(ObsidianBackground.copy(alpha = 0.96f))
                        .border(1.dp, AiCyan.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.source_bc_vision_ai),
                            style = MaterialTheme.typography.titleSmall,
                            color = AiCyan
                        )
                        IconButton(
                            onClick = onDismissOverlay,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dismiss_bc_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_close_overlay),
                                tint = TextSecondaryDark
                            )
                        }
                    }

                    when (visionSignalState) {
                        is SignalUiState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = AiCyan, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.btn_scanning_webview),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        is SignalUiState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SellCoralContainer.copy(alpha = 0.5f))
                                    .border(1.dp, SellCoral, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = SellCoralBright
                                )
                                Text(
                                    text = visionSignalState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        is SignalUiState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                AiSignalResultCard(
                                    signal = visionSignalState.signal,
                                    onOpenBcGameTerminal = null
                                )
                            }
                        }

                        SignalUiState.Idle -> Unit
                    }
                }
            }
        }

        // Docked AI Chart Vision Action Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isScanning = visionSignalState is SignalUiState.Loading
            Button(
                onClick = { captureAndAnalyzeWebView() },
                enabled = !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("scan_bc_game_webview_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AiCyan,
                    contentColor = Color(0xFF061524)
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF061524)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_scanning_webview),
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_scan_webview_ai),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
