package xyz.metiq.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.IBinder
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.metiq.BuildConfig
import xyz.metiq.CustomMix
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_CUSTOM_MIXES
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.ThemePreference
import xyz.metiq.audio.BINAURAL_ID
import xyz.metiq.audio.PlaybackService
import xyz.metiq.ui.FEEDBACK_URL
import xyz.metiq.ui.KOFI_URL
import xyz.metiq.ui.LicensesScreen
import xyz.metiq.ui.SettingsContent
import xyz.metiq.ui.openStoreRating
import xyz.metiq.ui.openUrl
import xyz.metiq.ui.components.LocalWaveAnimationEnabled
import xyz.metiq.ui.components.ParticleField
import xyz.metiq.ui.components.RatePromptBanner
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

private val FAB_INSET: Dp = 6.dp
private val FAB_SHADOW_PAD: Dp = 12.dp

private data class AmbientParticleField(val color: Color, val count: Int, val seed: Long)

private enum class HomeTab { NOISE, AMBIENT, SETTINGS }

@Composable
fun HomeScreen(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onWavesEnabled: (Boolean) -> Unit,
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
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var showLicenses by remember { mutableStateOf(false) }
    var binder by remember { mutableStateOf<PlaybackService.EngineBinder?>(null) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var startJob by remember { mutableStateOf<Job?>(null) }
    val pagerState = rememberPagerState(initialPage = HomeTab.NOISE.ordinal, pageCount = { HomeTab.entries.size })
    val tab = HomeTab.entries[pagerState.currentPage]
    val ambientLevels = remember { mutableStateMapOf<String, Float>() }
    var binauralBandId by remember { mutableStateOf<String?>(null) }
    var binauralVolume by remember { mutableStateOf(settings.binauralVolume) }
    val latestSettings by rememberUpdatedState(settings)
    var showSaveMixDialog by remember { mutableStateOf(false) }
    var pendingMixDelete by remember { mutableStateOf<CustomMix?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var showBinauralSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }

    val noiseTitleById = NOISE_COLORS.associate { it.id to stringResource(it.noiseTitleRes) }
    val resolvedTokens = LocalMetiqColors.current
    val noiseArgbById = remember(resolvedTokens) {
        mapOf(
            "pink" to resolvedTokens.noisePink.fill.toArgb(),
            "brown" to resolvedTokens.noiseBrown.fill.toArgb(),
            "white" to resolvedTokens.noiseWhite.fill.toArgb(),
            "grey" to resolvedTokens.noiseGrey.fill.toArgb(),
        )
    }

    val timer = rememberSleepTimerState(
        onFinished = {
            val c = controller
            val b = binder
            if (c != null && b != null) {
                b.engine.stopAllTimerFade()
                b.setActiveColor(null, null)
                c.stop()
                activeId = null
                ambientLevels.clear()
                binauralBandId = null
            }
        },
    )

    DisposableEffect(Unit) {
        var controllerFuture: ListenableFuture<MediaController>? = null
        var connectedController: MediaController? = null
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }

        fun releaseController() {
            controllerFuture?.let { f ->
                if (connectedController == null) MediaController.releaseFuture(f)
            }
            controllerFuture = null
            connectedController?.removeListener(listener)
            connectedController?.release()
            connectedController = null
            controller = null
        }

        fun connectController() {
            releaseController()
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            controllerFuture = future
            future.addListener({
                if (controllerFuture !== future) return@addListener // superseded by a reconnect
                val c = future.get()
                connectedController = c
                controller = c
                playing = c.isPlaying
                c.addListener(listener)
            }, ContextCompat.getMainExecutor(context))
        }

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, b: IBinder?) {
                val bound = b as PlaybackService.EngineBinder
                binder = bound
                val ids = bound.engine.activeLayerIds()
                val existingNoise = ids.firstOrNull { it in NOISE_IDS }
                if (existingNoise != null && activeId == null) {
                    activeId = existingNoise
                }
                ids.filter { it in AMBIENT_IDS }.forEach { aid ->
                    if (!ambientLevels.containsKey(aid)) {
                        ambientLevels[aid] = bound.engine.layerVolume(aid) ?: AMBIENT_DEFAULT_VOLUME
                    }
                }
                if (ambientLevels.isNotEmpty() && activeId == null) {
                    scope.launch { pagerState.scrollToPage(HomeTab.AMBIENT.ordinal) }
                }
                if (BINAURAL_ID in ids && binauralBandId == null) {
                    binauralBandId = latestSettings.binauralBand
                    bound.engine.layerVolume(BINAURAL_ID)?.let { binauralVolume = it }
                }
                connectController()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
            }
        }
        val bindIntent = Intent(
            context, PlaybackService::class.java
        ).setAction(PlaybackService.ENGINE_BIND_ACTION)
        context.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE)

        onDispose {
            releaseController()
            context.unbindService(conn)
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
    LaunchedEffect(settings.binauralVolume) {
        binauralVolume = settings.binauralVolume
    }
    LaunchedEffect(binder, binauralVolume) {
        binder?.engine?.setLayerVolume(BINAURAL_ID, binauralVolume)
    }

    val activeColor = activeId?.let { id -> NOISE_COLORS.firstOrNull { it.id == id } }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val onNoiseTab = tab == HomeTab.NOISE
    val particlesOn =
        onNoiseTab && activeColor != null && playing && settings.particlesEnabled &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val ambientWavesOn =
        !onNoiseTab && playing &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val ambientParticlesOn =
        !onNoiseTab && playing && settings.particlesEnabled &&
                ambientLevels.isNotEmpty() &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val wavesOn =
        onNoiseTab && activeColor != null && playing &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val binauralWavesOn =
        binauralBandId != null && playing &&
                lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val timerEnabled =
        (activeId != null || ambientLevels.isNotEmpty() || binauralBandId != null) && playing
    val tokens = LocalMetiqColors.current
    val ambientNowPlaying = stringResource(R.string.ambient_now_playing)
    val premadeMixNames = PREMADE_MIXES.associate { it.id to stringResource(it.labelRes) }
    val binauralNotiByBand = BINAURAL_BANDS.associate { it.id to stringResource(R.string.binaural_noti_label, it.glyph) }
    val binauralTileArgb = tokens.binauralAlpha.toArgb()
    val ambientTileArgb = tokens.ambientSeawaves.toArgb()

    val syncSession: () -> Unit = {
        val b = binder
        val c = controller
        if (b != null && c != null) {
            val noise = activeId?.let { aid -> NOISE_COLORS.firstOrNull { it.id == aid } }
            val bedLabel: String? = when {
                noise != null -> noiseTitleById[noise.id]
                ambientLevels.isNotEmpty() -> {
                    val activeMix = ambientLevels.filterValues { it > 0f }
                    PREMADE_MIXES.firstOrNull { mixMatches(activeMix, it.layers) }?.let { premadeMixNames[it.id] }
                        ?: settings.customMixes.firstOrNull { mixMatches(activeMix, it.layers) }?.name
                        ?: ambientNowPlaying
                }
                else -> null
            }
            val bedArgb: Int? = when {
                noise != null -> noiseArgbById[noise.id]
                ambientLevels.isNotEmpty() -> ambientTileArgb
                else -> null
            }
            val binauralPart = binauralBandId?.let { binauralNotiByBand[it] }
            val label = when {
                bedLabel != null && binauralPart != null -> "$bedLabel + $binauralPart"
                bedLabel != null -> bedLabel
                else -> binauralPart
            }
            if (label != null) {
                b.setActiveColor(label, bedArgb ?: binauralTileArgb)
            } else {
                b.setActiveColor(null, null)
            }
            if (activeId != null || ambientLevels.isNotEmpty() || binauralBandId != null) {
                b.requestAudioFocusNow()
                c.play()
            } else {
                c.stop()
                timer.reset()
            }
        }
    }

    val applyMix: (Map<String, Float>) -> Unit = { layers ->
        val b = binder
        val c = controller
        val valid = layers.filterKeys { it in AMBIENT_IDS }.filterValues { it > 0f }
        if (b != null && c != null && valid.isNotEmpty()) {
            activeId?.let { b.engine.stopLayer(it) }
            activeId = null
            ambientLevels.keys.toList().forEach { b.engine.stopLayer(it) }
            ambientLevels.clear()
            timer.reset()
            valid.forEach { (id, vol) -> ambientLevels[id] = vol }
            syncSession()
            valid.forEach { (id, vol) ->
                scope.launch {
                    runCatching { b.engine.startLayer(id, "audio/ambient/$id.ogg", vol) }
                }
            }
        }
    }

    val activateAmbient: (String, Float) -> Unit = { id, vol ->
        val b = binder
        val c = controller
        if (b != null && c != null) {
            activeId?.let { noise ->
                b.engine.stopLayer(noise)
                activeId = null
            }
            if (ambientLevels.isEmpty()) timer.reset()
            ambientLevels[id] = vol
            syncSession()
            scope.launch {
                runCatching { b.engine.startLayer(id, "audio/ambient/$id.ogg", vol) }
            }
        }
    }

    val disableAmbient: (String) -> Unit = { id ->
        val b = binder
        if (b != null) {
            b.engine.stopLayer(id)
            ambientLevels.remove(id)
            syncSession()
        }
    }

    val stopAmbient: () -> Unit = {
        val b = binder
        if (b != null) {
            ambientLevels.keys.toList().forEach { b.engine.stopLayer(it) }
            ambientLevels.clear()
            syncSession()
        }
    }

    val tapBinaural: (String) -> Unit = { id ->
        val b = binder
        val c = controller
        if (b != null && c != null) {
            val band = BINAURAL_BANDS.firstOrNull { it.id == id }
            if (band != null) {
                if (binauralBandId == id) {
                    if (!playing) {
                        c.play()
                    } else {
                        binauralBandId = null
                        b.engine.stopBinaural()
                        syncSession()
                    }
                } else {
                    val firstStart = binauralBandId == null
                    binauralBandId = id
                    onBinauralBand(id)
                    b.engine.setBinaural(BINAURAL_CARRIER_HZ, band.beatHz)
                    if (firstStart) b.engine.startBinaural(binauralVolume)
                    syncSession()
                }
            }
        }
    }

    val tapAmbient: (String) -> Unit = { id ->
        if (ambientLevels.containsKey(id)) {
            if (!playing) controller?.play() else disableAmbient(id)
        } else {
            activateAmbient(id, AMBIENT_DEFAULT_VOLUME)
        }
    }

    BackHandler(enabled = showLicenses) { showLicenses = false }
    BackHandler(enabled = !showLicenses && tab != HomeTab.NOISE) {
        scope.launch { pagerState.animateScrollToPage(HomeTab.NOISE.ordinal) }
    }

    CompositionLocalProvider(LocalWaveAnimationEnabled provides settings.wavesEnabled) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = tokens.background,
        bottomBar = {
            val playingTabs: Map<HomeTab, Color> = buildMap {
                if (playing) {
                    when {
                        activeId != null -> put(
                            HomeTab.NOISE,
                            activeId?.let { noiseArgbById[it] }?.let { Color(it) } ?: tokens.textPrimary,
                        )
                        ambientLevels.isNotEmpty() -> put(HomeTab.AMBIENT, Color(ambientTileArgb))
                    }
                }
            }
            MetiqBottomBar(
                selected = tab,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it.ordinal) } },
                playingTabs = playingTabs,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showLicenses && tab != HomeTab.SETTINGS,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                Column(
                    modifier = Modifier.padding(end = FAB_INSET, bottom = FAB_INSET),
                    horizontalAlignment = Alignment.End,
                ) {
                    AnimatedVisibility(
                        visible = timerEnabled,
                        modifier = Modifier.offset(x = FAB_SHADOW_PAD),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Box(modifier = Modifier.padding(FAB_SHADOW_PAD)) {
                            TimerFab(
                                state = timer,
                                onClick = { showTimerSheet = true },
                            )
                        }
                    }
                    BinauralFab(
                        band = BINAURAL_BANDS.firstOrNull { it.id == binauralBandId },
                        wavesOn = binauralWavesOn,
                        onClick = { showBinauralSheet = true },
                    )
                }
            }
        },
    ) { padding ->
      Box(
          modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                  detectTapGestures { focusManager.clearFocus() }
              },
      ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(
                visible = ratePromptVisible && !showLicenses && tab != HomeTab.SETTINGS,
                enter = expandVertically() + slideInVertically { -it } + fadeIn(),
                exit = shrinkVertically() + slideOutVertically { -it } + fadeOut(),
            ) {
                RatePromptBanner(
                    showFeedback = BuildConfig.SHOW_FEEDBACK_CTA,
                    message = stringResource(R.string.rate_prompt_message, BuildConfig.STORE_NAME),
                    rateLabel = stringResource(R.string.rate_prompt_cta),
                    onRate = { openStoreRating(context) },
                    onFeedback = { openUrl(context, FEEDBACK_URL) },
                    onDonate = { openUrl(context, KOFI_URL) },
                    onDismiss = onRatePromptDismiss,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = HomeTab.entries.size - 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.NOISE -> TabCard(
                        onHelp = { showHelp = true },
                        particles = {
                            val particleAlpha by animateFloatAsState(
                                targetValue = if (particlesOn) 1f else 0f,
                                animationSpec = tween(durationMillis = PARTICLE_FADE_MS),
                                label = "noiseParticleFade",
                            )
                            var particleFill by remember { mutableStateOf(Color.Unspecified) }
                            if (activeColor != null) particleFill = paletteFor(activeColor.id).fill
                            if (particleAlpha > 0f && particleFill != Color.Unspecified) {
                                ParticleField(
                                    color = particleFill,
                                    intensity = particleAlpha,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        },
                    ) {
                        ColorGrid(
                            activeId = activeId,
                            wavesOn = wavesOn,
                            onSelect = { id ->
                                if (id in NOISE_IDS) {
                                    if (activeId == id && !playing) {
                                        controller?.play()
                                    } else {
                                        if (activeId != id) timer.reset()
                                        ambientLevels.clear()
                                        startJob?.cancel()
                                        startJob = scope.launch {
                                            selectColor(id, activeId, binder, { activeId = it }, syncSession)
                                        }
                                    }
                                }
                            },
                        )
                    }

                    HomeTab.AMBIENT -> TabCard(
                        onHelp = { showHelp = true },
                        particles = {
                            val particleAlpha by animateFloatAsState(
                                targetValue = if (ambientParticlesOn) 1f else 0f,
                                animationSpec = tween(durationMillis = PARTICLE_FADE_MS),
                                label = "ambientParticleFade",
                            )
                            var particleFields by remember { mutableStateOf(emptyList<AmbientParticleField>()) }
                            if (ambientParticlesOn) {
                                val activeAmbient = AMBIENT_SOUNDS.filter { ambientLevels.containsKey(it.id) }
                                val perField = (AMBIENT_PARTICLE_TOTAL / activeAmbient.size).coerceAtLeast(1)
                                particleFields = activeAmbient.map { sound ->
                                    AmbientParticleField(
                                        color = lerp(ambientAccentFor(sound.id), tokens.accentHighlight, 0.40f),
                                        count = perField,
                                        seed = sound.id.hashCode().toLong(),
                                    )
                                }
                            }
                            if (particleAlpha > 0f) {
                                particleFields.forEach { field ->
                                    ParticleField(
                                        color = field.color,
                                        count = field.count,
                                        seed = field.seed,
                                        intensity = particleAlpha,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        },
                    ) {
                        val activeMix = ambientLevels.filterValues { it > 0f }
                        val mixIsSaved = PREMADE_MIXES.any { mixMatches(activeMix, it.layers) } ||
                                settings.customMixes.any { mixMatches(activeMix, it.layers) }
                        MixPresets(
                            customMixes = settings.customMixes,
                            activeMix = activeMix,
                            onApply = applyMix,
                            onStop = stopAmbient,
                            onDelete = { pendingMixDelete = it },
                        )
                        Spacer(Modifier.height(24.dp))
                        AmbientGrid(
                            levels = ambientLevels,
                            wavesOn = ambientWavesOn,
                            onTap = tapAmbient,
                            onVolume = { id, v ->
                                ambientLevels[id] = v
                                binder?.engine?.setLayerVolume(id, v)
                            },
                            onVolumeSettled = { id ->
                                if ((ambientLevels[id] ?: 0f) <= 0f) disableAmbient(id)
                            },
                        )
                        Spacer(Modifier.height(24.dp))
                        SaveMixButton(
                            enabled = activeMix.isNotEmpty() && !mixIsSaved &&
                                    settings.customMixes.size < MAX_CUSTOM_MIXES,
                            onClick = { showSaveMixDialog = true },
                        )
                    }

                    HomeTab.SETTINGS -> Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            color = tokens.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        SettingsContent(
                            settings = settings,
                            onParticlesEnabled = onParticlesEnabled,
                            onWavesEnabled = onWavesEnabled,
                            onDynamicColors = onDynamicColors,
                            onWarmth = onWarmth,
                            onWarmthPreview = { w -> binder?.engine?.setWarmth(w) },
                            onFadeSeconds = onFadeSeconds,
                            onTimerFadeSeconds = onTimerFadeSeconds,
                            onRequestAudioFocus = onRequestAudioFocus,
                            onThemePreference = onThemePreference,
                            onTimerPresets = onTimerPresets,
                            onLanguageTag = onLanguageTag,
                            onOpenLicenses = { showLicenses = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        pendingMixDelete?.let { mix ->
            AlertDialog(
                onDismissRequest = { pendingMixDelete = null },
                title = { Text(stringResource(R.string.mix_delete_title), fontFamily = Inter) },
                text = {
                    Text(
                        stringResource(R.string.mix_delete_message, mix.name),
                        fontFamily = Inter,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onCustomMixes(settings.customMixes - mix)
                        pendingMixDelete = null
                    }) { Text(stringResource(R.string.mix_delete_confirm), fontFamily = Inter) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingMixDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
                    }
                },
            )
        }
        if (showSaveMixDialog) {
            SaveMixDialog(
                onDismiss = { showSaveMixDialog = false },
                onSave = { name ->
                    val snapshot = ambientLevels.filterValues { it > 0f }
                    if (snapshot.isNotEmpty()) {
                        val others = settings.customMixes
                            .filterNot { it.name.equals(name, ignoreCase = true) }
                        onCustomMixes((others + CustomMix(name, snapshot)).take(MAX_CUSTOM_MIXES))
                    }
                    showSaveMixDialog = false
                },
            )
        }
        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text(stringResource(R.string.help_title), fontFamily = Inter) },
                text = {
                    Text(
                        stringResource(
                            if (tab == HomeTab.AMBIENT) R.string.ambient_helper
                            else R.string.home_helper
                        ),
                        fontFamily = Inter,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) {
                        Text(stringResource(R.string.dialog_ok), fontFamily = Inter)
                    }
                },
            )
        }
        if (showBinauralSheet) {
            BinauralSheet(
                activeBand = binauralBandId,
                wavesOn = binauralWavesOn,
                particlesOn = binauralWavesOn && settings.particlesEnabled,
                volume = binauralVolume,
                onTap = tapBinaural,
                onVolume = { binauralVolume = it },
                onVolumeSettled = { onBinauralVolume(binauralVolume) },
                onDismiss = { showBinauralSheet = false },
            )
        }
        if (showTimerSheet) {
            TimerSheet(
                state = timer,
                presetsSeconds = settings.timerPresetsSeconds,
                onDismiss = { showTimerSheet = false },
            )
        }
      }
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
}

@Composable
private fun TabCard(
    onHelp: () -> Unit,
    particles: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(tokens.foreground),
    ) {
        particles()
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = CONTENT_HORIZONTAL_PADDING, end = CONTENT_HORIZONTAL_PADDING, top = 20.dp),
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
                onClick = onHelp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.help_cd),
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = CONTENT_HORIZONTAL_PADDING, end = CONTENT_HORIZONTAL_PADDING, bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun MetiqBottomBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    playingTabs: Map<HomeTab, Color>,
) {
    val tokens = LocalMetiqColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = tokens.background,
        selectedTextColor = tokens.textPrimary,
        unselectedIconColor = tokens.textSecondary,
        unselectedTextColor = tokens.textSecondary,
        indicatorColor = tokens.textPrimary,
    )
    NavigationBar(containerColor = tokens.background) {
        NavigationBarItem(
            selected = selected == HomeTab.NOISE,
            onClick = { onSelect(HomeTab.NOISE) },
            icon = {
                TabIcon(Icons.Outlined.GraphicEq, HomeTab.NOISE in playingTabs, playingTabs[HomeTab.NOISE] ?: tokens.textPrimary)
            },
            label = { Text(stringResource(R.string.tab_noise), fontFamily = Inter) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == HomeTab.AMBIENT,
            onClick = { onSelect(HomeTab.AMBIENT) },
            icon = {
                TabIcon(Icons.Outlined.Waves, HomeTab.AMBIENT in playingTabs, playingTabs[HomeTab.AMBIENT] ?: tokens.textPrimary)
            },
            label = { Text(stringResource(R.string.tab_ambient), fontFamily = Inter) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == HomeTab.SETTINGS,
            onClick = { onSelect(HomeTab.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_settings), fontFamily = Inter) },
            colors = itemColors,
        )
    }
}

@Composable
private fun TabIcon(vector: ImageVector, playing: Boolean, accent: Color) {
    Box {
        Icon(vector, contentDescription = null)
        if (playing) {
            val pulse by rememberInfiniteTransition(label = "playingDot")
                .animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "playingDotAlpha",
                )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .size(6.dp)
                    .graphicsLayer { alpha = pulse }
                    .background(accent, CircleShape)
            )
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
            onWavesEnabled = {},
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
