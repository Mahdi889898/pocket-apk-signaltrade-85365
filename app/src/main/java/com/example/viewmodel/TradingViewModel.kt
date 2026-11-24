package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.TradingAiService
import com.example.data.MarketBundle
import com.example.data.MarketDataRepository
import com.example.model.AiTradingSignal
import com.example.model.AppTab
import com.example.model.CryptoPair
import com.example.model.MarketDataState
import com.example.model.SUPPORTED_CONTRACT_PAIRS
import com.example.model.ScannerUiState
import com.example.model.SignalUiState
import com.example.model.Timeframe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "TradingViewModel"

data class TradingUiState(
    val activeTab: AppTab = AppTab.SIGNALS,
    val selectedPair: CryptoPair = SUPPORTED_CONTRACT_PAIRS.first(),
    val selectedTimeframe: Timeframe = Timeframe.M15,
    val searchQuery: String = "",
    val bcGameReachable: Boolean? = null,
    val marketState: MarketDataState = MarketDataState.Loading,
    val signalState: SignalUiState = SignalUiState.Idle,
    val bcVisionSignalState: SignalUiState = SignalUiState.Idle,
    val scannerState: ScannerUiState = ScannerUiState.Idle,
    val signalHistory: List<AiTradingSignal> = emptyList(),
    val lastUpdatedMs: Long = 0L
)

class TradingViewModel(
    private val marketRepository: MarketDataRepository = MarketDataRepository(),
    private val aiService: TradingAiService = TradingAiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    private var currentBundle: MarketBundle? = null
    private var loadJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        probeBcGameConnection()
        loadSelectedMarket(resetSignal = true)
        startLiveRefreshLoop()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectPair(pair: CryptoPair) {
        if (_uiState.value.selectedPair.symbol == pair.symbol) return
        _uiState.update {
            it.copy(
                selectedPair = pair,
                searchQuery = "",
                signalState = SignalUiState.Idle
            )
        }
        loadSelectedMarket(resetSignal = true)
    }

    fun selectCustomSymbol(rawInput: String) {
        val cleaned = rawInput.trim().uppercase().replace("/USDT", "").replace("USDT", "")
            .filter { it.isLetterOrDigit() }
        if (cleaned.length < 2) return
        val existing = SUPPORTED_CONTRACT_PAIRS.find { it.baseAsset == cleaned }
        val pair = existing ?: CryptoPair(baseAsset = cleaned, displayNameFa = cleaned)
        selectPair(pair)
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (_uiState.value.selectedTimeframe == timeframe) return
        _uiState.update {
            it.copy(
                selectedTimeframe = timeframe,
                signalState = SignalUiState.Idle
            )
        }
        loadSelectedMarket(resetSignal = true)
    }

    fun refreshMarketNow() {
        probeBcGameConnection()
        loadSelectedMarket(resetSignal = false)
    }

    fun analyzeCurrentMarketWithAi() {
        val bundle = currentBundle
        if (bundle == null) {
            _uiState.update {
                it.copy(
                    signalState = SignalUiState.Error("ابتدا دیتای زنده بازار دریافت شود، سپس تحلیل هوشمند را اجرا کنید.")
                )
            }
            return
        }
        val timeframe = _uiState.value.selectedTimeframe
        viewModelScope.launch {
            _uiState.update { it.copy(signalState = SignalUiState.Loading) }
            try {
                val signal = aiService.analyzeLiveContractMarket(bundle, timeframe)
                _uiState.update { state ->
                    state.copy(
                        signalState = SignalUiState.Success(signal),
                        signalHistory = listOf(signal) + state.signalHistory.take(29)
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI signal generation timed out", e)
                _uiState.update {
                    it.copy(
                        signalState = SignalUiState.Error(
                            "زمان تحلیل هوش مصنوعی به پایان رسید (۶۰ ثانیه). لطفاً دوباره تلاش کنید."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI signal analysis failed", e)
                _uiState.update {
                    it.copy(
                        signalState = SignalUiState.Error(
                            e.localizedMessage ?: "خطا در دریافت تحلیل از هوش مصنوعی."
                        )
                    )
                }
            }
        }
    }

    fun analyzeBcGameWebViewSnapshot(
        bitmap: Bitmap,
        pageUrl: String,
        pageTitle: String,
        extractedPageText: String
    ) {
        val fallbackPair = _uiState.value.selectedPair.displayPair
        viewModelScope.launch {
            _uiState.update { it.copy(bcVisionSignalState = SignalUiState.Loading) }
            try {
                val signal = aiService.analyzeBcGameTerminalScreenshot(
                    screenshot = bitmap,
                    pageUrl = pageUrl,
                    pageTitle = pageTitle,
                    extractedPageText = extractedPageText,
                    fallbackPairDisplay = fallbackPair
                )
                _uiState.update { state ->
                    state.copy(
                        bcVisionSignalState = SignalUiState.Success(signal),
                        signalHistory = listOf(signal) + state.signalHistory.take(29)
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "BC.Game vision AI timed out", e)
                _uiState.update {
                    it.copy(
                        bcVisionSignalState = SignalUiState.Error(
                            "زمان تحلیل تصویری به پایان رسید. لطفاً دوباره اسکن کنید."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "BC.Game vision AI failed", e)
                _uiState.update {
                    it.copy(
                        bcVisionSignalState = SignalUiState.Error(
                            e.localizedMessage ?: "خطا در تحلیل تصویری صفحه BC.Game."
                        )
                    )
                }
            }
        }
    }

    fun dismissBcVisionOverlay() {
        _uiState.update { it.copy(bcVisionSignalState = SignalUiState.Idle) }
    }

    fun runMultiCoinAiScanner() {
        val timeframe = _uiState.value.selectedTimeframe
        val scanPairs = SUPPORTED_CONTRACT_PAIRS.take(8)
        viewModelScope.launch {
            _uiState.update { it.copy(scannerState = ScannerUiState.Loading) }
            try {
                val bundles = scanPairs.map { pair ->
                    async {
                        runCatching {
                            marketRepository.fetchMarketBundle(pair, timeframe, limit = 50)
                        }.getOrNull()
                    }
                }.awaitAll().filterNotNull()

                if (bundles.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            scannerState = ScannerUiState.Error(
                                "دریافت دیتای زنده ارزها ناموفق بود. اتصال اینترنت را بررسی کنید."
                            )
                        )
                    }
                    return@launch
                }

                val scannedSignals = aiService.scanMarketBatch(bundles, timeframe)
                if (scannedSignals.isEmpty()) {
                    _uiState.update {
                        it.copy(scannerState = ScannerUiState.Error("سیگنالی از اسکنر دریافت نشد."))
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            scannerState = ScannerUiState.Success(
                                items = scannedSignals,
                                scannedAtMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Batch AI scanner timed out", e)
                _uiState.update {
                    it.copy(
                        scannerState = ScannerUiState.Error("زمان اسکن بازار به پایان رسید. دوباره تلاش کنید.")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Batch AI scanner failed", e)
                _uiState.update {
                    it.copy(
                        scannerState = ScannerUiState.Error(
                            e.localizedMessage ?: "خطا در اجرای اسکنر هوشمند."
                        )
                    )
                }
            }
        }
    }

    fun inspectPairFromScanner(pair: CryptoPair) {
        selectPair(pair)
        selectTab(AppTab.SIGNALS)
    }

    private fun probeBcGameConnection() {
        viewModelScope.launch {
            val reachable = marketRepository.checkBcGameReachability()
            _uiState.update { it.copy(bcGameReachable = reachable) }
        }
    }

    private fun loadSelectedMarket(resetSignal: Boolean) {
        loadJob?.cancel()
        val pair = _uiState.value.selectedPair
        val timeframe = _uiState.value.selectedTimeframe
        loadJob = viewModelScope.launch {
            if (currentBundle == null || resetSignal) {
                _uiState.update { it.copy(marketState = MarketDataState.Loading) }
            }
            try {
                val bundle = marketRepository.fetchMarketBundle(pair, timeframe)
                currentBundle = bundle
                _uiState.update {
                    it.copy(
                        marketState = MarketDataState.Ready(
                            ticker = bundle.ticker,
                            candles = bundle.candles,
                            indicators = bundle.indicators
                        ),
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load market bundle", e)
                _uiState.update {
                    it.copy(
                        marketState = MarketDataState.Error(
                            e.localizedMessage ?: "خطا در اتصال به دیتای بازار."
                        )
                    )
                }
            }
        }
    }

    private fun startLiveRefreshLoop() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                if (_uiState.value.activeTab == AppTab.SIGNALS) {
                    val pair = _uiState.value.selectedPair
                    val timeframe = _uiState.value.selectedTimeframe
                    try {
                        val bundle = marketRepository.fetchMarketBundle(pair, timeframe)
                        if (_uiState.value.selectedPair.symbol == pair.symbol &&
                            _uiState.value.selectedTimeframe == timeframe
                        ) {
                            currentBundle = bundle
                            _uiState.update {
                                it.copy(
                                    marketState = MarketDataState.Ready(
                                        ticker = bundle.ticker,
                                        candles = bundle.candles,
                                        indicators = bundle.indicators
                                    ),
                                    lastUpdatedMs = System.currentTimeMillis()
                                )
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Silent live tick refresh skipped: ${e.message}")
                    }
                }
            }
        }
    }
}
