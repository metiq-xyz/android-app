package xyz.metiq.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.metiq.BuildConfig
import xyz.metiq.CustomMix
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_CUSTOM_MIXES
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.ThemePreference
import xyz.metiq.ui.FEEDBACK_URL
import xyz.metiq.ui.LicensesScreen
import xyz.metiq.ui.SUPPORT_URL
import xyz.metiq.ui.SettingsScreen
import xyz.metiq.ui.components.RatePromptDialog
import xyz.metiq.ui.openStoreRating
import xyz.metiq.ui.openUrl
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

private val CARD_INSET: Dp = 20.dp
private const val RATE_PROMPT_DELAY_MS = 1500L
private val CONTENT_HORIZONTAL_PADDING: Dp = 12.dp

@Composable
fun HomeScreen(
    settings: Settings,
    onDynamicColors: (Boolean) -> Unit,
    onWarmth: (Float) -> Unit,
    onFadeSeconds: (Float) -> Unit,
    onTimerFadeSeconds: (Float) -> Unit,
    onRequestAudioFocus: (Boolean) -> Unit,
    onThemePreference: (ThemePreference) -> Unit,
    onBinauralVolume: (Float) -> Unit,
    onBinauralBand: (String?) -> Unit,
    onTimerPresets: (List<Long>) -> Unit,
    onCustomMixes: (List<CustomMix>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    ratePromptVisible: Boolean = false,
    onRatePromptDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val tokens = LocalMetiqColors.current
    var showSettings by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var openCategory by remember { mutableStateOf<SoundCategory?>(null) }
    var showMixSheet by remember { mutableStateOf(false) }
    var showSaveMixDialog by remember { mutableStateOf(false) }
    var pendingMixDelete by remember { mutableStateOf<CustomMix?>(null) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var ratePromptReady by remember { mutableStateOf(false) }
    val playback = rememberMixPlayback(
        savedBinauralBand = settings.binauralBand,
        binauralVolume = settings.binauralVolume,
        onBinauralBand = onBinauralBand,
        onBinauralVolume = onBinauralVolume,
    )
    val binder = playback.binder
    val timer = rememberSleepTimerState(
        onStart = { seconds -> binder?.startSleepTimer(seconds) },
        onCancel = { binder?.cancelSleepTimer() },
    )

    LaunchedEffect(Unit) {
        delay(RATE_PROMPT_DELAY_MS)
        ratePromptReady = true
    }

    LaunchedEffect(binder) {
        val b = binder ?: return@LaunchedEffect
        // The service owns the countdown; when it ends it stops playback, and MixPlayback
        // clears the mix from the resulting idle state.
        b.timerRemainingSeconds.collect { remaining ->
            if (remaining != null) timer.syncFromService(remaining) else if (timer.running) timer.reset()
        }
    }

    LaunchedEffect(binder, settings.warmth) {
        binder?.engine?.setWarmth(settings.warmth)
    }

    LaunchedEffect(binder, settings.fadeSeconds) {
        binder?.engine?.setFadeMillis((settings.fadeSeconds * 1000f).toLong())
    }
    
    LaunchedEffect(binder, settings.timerFadeSeconds) {
        binder?.engine?.setTimerFadeMillis((settings.timerFadeSeconds * 1000f).toLong())
    }
    
    LaunchedEffect(binder, settings.requestAudioFocus) {
        binder?.setRequestAudioFocus(settings.requestAudioFocus)
    }

    val quickMixes = settings.customMixes.map { it.name } +
            PREMADE_MIXES.map { stringResource(it.labelRes) }
    val quickMixLayers = settings.customMixes.map { it.layers } + PREMADE_MIXES.map { it.layers }
    val activeQuickMixes = quickMixLayers.indices.filter { mixMatches(playback.levels, quickMixLayers[it]) }.toSet()
    val mixIsSaved = activeQuickMixes.isNotEmpty()
    val saveEnabled = playback.levels.isNotEmpty() && !mixIsSaved &&
            settings.customMixes.size < MAX_CUSTOM_MIXES
    val mixEmpty = playback.levels.isEmpty()
    
    LaunchedEffect(mixEmpty) {
        if (mixEmpty) {
            showMixSheet = false
            timer.reset()
        }
    }
    
    val mixTitle = activeQuickMixes.minOrNull()?.let { quickMixes[it] }
    val nowPlaying = mixLabel(playback.levels, mixTitle)
    val nowPlayingArgb = mixTint(playback.levels)?.toArgb()
    
    LaunchedEffect(binder, nowPlaying, nowPlayingArgb) {
        binder?.setActiveColor(nowPlaying, nowPlayingArgb)
    }

    BackHandler(enabled = showLicenses) { showLicenses = false }
    BackHandler(enabled = showSettings && !showLicenses) { showSettings = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            },
    ) {
        Scaffold(containerColor = tokens.background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                HomeCard(
                    quickMixes = quickMixes,
                    mixTitle = mixTitle,
                    onOpenSettings = { showSettings = true },
                    onSelectCategory = { openCategory = it },
                    levels = playback.levels,
                    playing = playback.playing,
                    onPlayPause = playback::togglePlayPause,
                    onQuickMix = { index ->
                        // Tapping the mix that's already on stops it, like the old chips did.
                        if (index in activeQuickMixes) playback.clear() else playback.applyMix(quickMixLayers[index])
                    },
                    activeQuickMixes = activeQuickMixes,
                    customMixCount = settings.customMixes.size,
                    onDeleteQuickMix = { pendingMixDelete = settings.customMixes[it] },
                    saveEnabled = saveEnabled,
                    onSaveMix = { showSaveMixDialog = true },
                    onOpenMix = { showMixSheet = true },
                    timerEnabled = !mixEmpty && playback.playing,
                    timerRemainingSeconds = timer.remainingSeconds.takeIf { timer.running },
                    onOpenTimer = { showTimerSheet = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val nothingElseOpen = !showSettings && !showLicenses && openCategory == null &&
                !showMixSheet && !showTimerSheet && !showSaveMixDialog && pendingMixDelete == null
        if (ratePromptVisible && ratePromptReady && nothingElseOpen) {
            RatePromptDialog(
                showFeedback = BuildConfig.SHOW_FEEDBACK_CTA,
                message = stringResource(R.string.rate_prompt_message, BuildConfig.STORE_NAME),
                rateLabel = stringResource(R.string.rate_prompt_cta),
                onRate = { openStoreRating(context) },
                onFeedback = { openUrl(context, FEEDBACK_URL) },
                onDonate = { openUrl(context, SUPPORT_URL) },
                onDismiss = onRatePromptDismiss,
            )
        }
        openCategory?.let { category ->
            CategorySheet(
                category = category,
                levels = playback.levels,
                onToggle = playback::toggle,
                onLevel = playback::setLevel,
                onLevelSettled = playback::settleLevel,
                onDismiss = { openCategory = null },
            )
        }
        if (showTimerSheet) {
            TimerSheet(
                state = timer,
                presetsSeconds = settings.timerPresetsSeconds,
                onDismiss = { showTimerSheet = false },
            )
        }
        if (showMixSheet && !mixEmpty) {
            MixSheet(
                title = mixTitle,
                levels = playback.levels,
                saveEnabled = saveEnabled,
                onSave = { showSaveMixDialog = true },
                onToggle = playback::toggle,
                onLevel = playback::setLevel,
                onLevelSettled = playback::settleLevel,
                onDismiss = { showMixSheet = false },
            )
        }
        if (showSaveMixDialog) {
            SaveMixDialog(
                onDismiss = { showSaveMixDialog = false },
                onSave = { name ->
                    val snapshot = playback.levels.filterValues { it > 0f }
                    if (snapshot.isNotEmpty()) {
                        val others = settings.customMixes
                            .filterNot { it.name.equals(name, ignoreCase = true) }
                        onCustomMixes((others + CustomMix(name, snapshot)).take(MAX_CUSTOM_MIXES))
                    }
                    showSaveMixDialog = false
                },
            )
        }
        pendingMixDelete?.let { mix ->
            DeleteMixDialog(
                mixName = mix.name,
                onDismiss = { pendingMixDelete = null },
                onConfirm = {
                    onCustomMixes(settings.customMixes - mix)
                    pendingMixDelete = null
                },
            )
        }
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            SettingsScreen(
                settings = settings,
                onDynamicColors = onDynamicColors,
                onWarmth = onWarmth,
                onWarmthPreview = { w -> binder?.engine?.setWarmth(w) },
                onFadeSeconds = onFadeSeconds,
                onTimerFadeSeconds = onTimerFadeSeconds,
                onRequestAudioFocus = onRequestAudioFocus,
                onThemePreference = onThemePreference,
                onTimerPresets = onTimerPresets,
                onLanguageTag = onLanguageTag,
                onBack = { showSettings = false },
                onOpenLicenses = { showLicenses = true },
            )
        }
        AnimatedVisibility(
            visible = showLicenses,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            LicensesScreen(onBack = { showLicenses = false })
        }
    }
}

@Composable
private fun HomeCard(
    quickMixes: List<String>,
    mixTitle: String?,
    onOpenSettings: () -> Unit,
    onSelectCategory: (SoundCategory) -> Unit,
    levels: Map<String, Float>,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onQuickMix: (Int) -> Unit,
    activeQuickMixes: Set<Int>,
    customMixCount: Int,
    onDeleteQuickMix: (Int) -> Unit,
    saveEnabled: Boolean,
    onSaveMix: () -> Unit,
    onOpenMix: () -> Unit,
    timerEnabled: Boolean,
    timerRemainingSeconds: Long?,
    onOpenTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(tokens.foreground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 14.dp, end = 14.dp)
                .height(48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_metiq),
                contentDescription = stringResource(R.string.app_name),
                colorFilter = ColorFilter.tint(tokens.logo),
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(32.dp),
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = CONTENT_HORIZONTAL_PADDING,
                        end = CONTENT_HORIZONTAL_PADDING,
                        top = 32.dp,
                        bottom = 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CategoryGrid(levels = levels, onSelect = onSelectCategory)
            }
            TimerButton(
                enabled = timerEnabled,
                remainingSeconds = timerRemainingSeconds,
                onClick = onOpenTimer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = CARD_INSET, bottom = CARD_INSET),
            )
        }
        MixPanel(
            quickMixes = quickMixes,
            mixTitle = mixTitle,
            soundNames = mixSoundNames(levels),
            playing = playing,
            saveEnabled = saveEnabled,
            onPlayPause = onPlayPause,
            onSaveMix = onSaveMix,
            onOpenMix = onOpenMix,
            onQuickMix = onQuickMix,
            activeQuickMixes = activeQuickMixes,
            deletableQuickMixes = customMixCount,
            onDeleteQuickMix = onDeleteQuickMix,
            modifier = Modifier.padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_INSET),
        )
    }
}

@Preview(name = "Home · Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Home · Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HomeScreenPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        HomeScreen(
            settings = DEFAULT_SETTINGS,
            onDynamicColors = {},
            onWarmth = {},
            onFadeSeconds = {},
            onTimerFadeSeconds = {},
            onRequestAudioFocus = {},
            onThemePreference = {},
            onBinauralVolume = {},
            onBinauralBand = {},
            onTimerPresets = {},
            onCustomMixes = {},
            onLanguageTag = {},
        )
    }
}
