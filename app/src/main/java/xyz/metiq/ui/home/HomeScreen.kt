package xyz.metiq.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import xyz.metiq.BuildConfig
import xyz.metiq.CustomMix
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_CUSTOM_MIXES
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.StartupScreen
import xyz.metiq.ThemePreference
import xyz.metiq.ui.FEEDBACK_URL
import xyz.metiq.ui.LicensesScreen
import xyz.metiq.ui.SUPPORT_URL
import xyz.metiq.ui.SettingsScreen
import xyz.metiq.ui.components.ParticleField
import xyz.metiq.ui.components.RatePromptDialog
import xyz.metiq.ui.openStoreRating
import xyz.metiq.ui.openUrl
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

private val CARD_INSET: Dp = 20.dp
private const val PARTICLE_FADE_MS = 400
private const val PARTICLE_TOTAL = 90
private const val RATE_PROMPT_DELAY_MS = 1500L
private val CONTENT_HORIZONTAL_PADDING: Dp = 12.dp

@Composable
fun HomeScreen(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onDynamicColors: (Boolean) -> Unit,
    onWarmth: (Float) -> Unit,
    onFadeSeconds: (Float) -> Unit,
    onTimerFadeSeconds: (Float) -> Unit,
    onRequestAudioFocus: (Boolean) -> Unit,
    onThemePreference: (ThemePreference) -> Unit,
    onStartupScreen: (StartupScreen) -> Unit,
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
    var openCategory by remember {
        mutableStateOf(
            when (settings.startupScreen) {
                StartupScreen.HOME -> null
                StartupScreen.NOISE -> SoundCategory.NOISE
                StartupScreen.AMBIENT -> SoundCategory.AMBIENT
                StartupScreen.BINAURAL -> SoundCategory.BINAURAL
            }
        )
    }
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
    
    LaunchedEffect(binder, nowPlaying) {
        binder?.setActiveLabel(nowPlaying)
    }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val particlesOn = settings.particlesEnabled && playback.playing && !mixEmpty &&
            lifecycleState.isAtLeast(Lifecycle.State.STARTED)

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
                    particlesOn = particlesOn,
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
                onParticlesEnabled = onParticlesEnabled,
                onDynamicColors = onDynamicColors,
                onWarmth = onWarmth,
                onWarmthPreview = { w -> binder?.engine?.setWarmth(w) },
                onFadeSeconds = onFadeSeconds,
                onTimerFadeSeconds = onTimerFadeSeconds,
                onRequestAudioFocus = onRequestAudioFocus,
                onThemePreference = onThemePreference,
                onStartupScreen = onStartupScreen,
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
    particlesOn: Boolean,
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
                .weight(1f)
                .fillMaxWidth(),
        ) {
            MixParticles(
                levels = levels,
                visible = particlesOn,
                modifier = Modifier.matchParentSize(),
            )
            SoundsCardContent(
                onOpenSettings = onOpenSettings,
                onSelectCategory = onSelectCategory,
                levels = levels,
                timerEnabled = timerEnabled,
                timerRemainingSeconds = timerRemainingSeconds,
                onOpenTimer = onOpenTimer,
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

@Composable
private fun SoundsCardContent(
    onOpenSettings: () -> Unit,
    onSelectCategory: (SoundCategory) -> Unit,
    levels: Map<String, Float>,
    timerEnabled: Boolean,
    timerRemainingSeconds: Long?,
    onOpenTimer: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Column(modifier = Modifier.fillMaxSize()) {
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
    }
}

private data class ParticleLayer(val color: Color, val count: Int, val seed: Long)

/**
 * Flow-field dots behind the category orbs, one layer per sound in the mix, tinted like its orb.
 * The last layers are kept while fading out so the dots don't vanish the instant playback stops.
 */
@Composable
private fun MixParticles(levels: Map<String, Float>, visible: Boolean, modifier: Modifier = Modifier) {
    val tokens = LocalMetiqColors.current
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = PARTICLE_FADE_MS),
        label = "particleFade",
    )
    var layers by remember { mutableStateOf(emptyList<ParticleLayer>()) }
    if (visible && levels.isNotEmpty()) {
        val perLayer = (PARTICLE_TOTAL / levels.size).coerceAtLeast(1)
        layers = levels.keys.sorted().map { id ->
            val color = when {
                id == "white" -> tokens.textPrimary
                NOISE_COLORS.any { it.id == id } -> noiseColorFor(id)
                id in BINAURAL_BAND_IDS -> lerp(binauralAccentFor(id), tokens.accentHighlight, 0.40f)
                else -> lerp(ambientAccentFor(id), tokens.accentHighlight, 0.40f)
            }
            ParticleLayer(color = color, count = perLayer, seed = id.hashCode().toLong())
        }
    }
    if (alpha > 0f) {
        Box(modifier = modifier) {
            layers.forEach { layer ->
                ParticleField(
                    color = layer.color,
                    count = layer.count,
                    seed = layer.seed,
                    intensity = alpha,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(name = "Home · Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Home · Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HomeScreenPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        HomeScreen(
            settings = DEFAULT_SETTINGS,
            onParticlesEnabled = {},
            onDynamicColors = {},
            onWarmth = {},
            onFadeSeconds = {},
            onTimerFadeSeconds = {},
            onRequestAudioFocus = {},
            onThemePreference = {},
            onStartupScreen = {},
            onBinauralVolume = {},
            onBinauralBand = {},
            onTimerPresets = {},
            onCustomMixes = {},
            onLanguageTag = {},
        )
    }
}
