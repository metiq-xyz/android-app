package xyz.metiq.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.metiq.BuildConfig
import xyz.metiq.DEFAULT_SETTINGS
import xyz.metiq.MAX_TIMER_PRESETS
import xyz.metiq.R
import xyz.metiq.Settings
import xyz.metiq.ThemePreference
import xyz.metiq.clampFadeSeconds
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme
import xyz.metiq.ui.theme.Inter
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val SECTION_HORIZONTAL_PADDING = 16.dp
private val SETTING_TITLE_GAP = 4.dp
private val SETTING_ACTION_GAP = 12.dp
private val SECTION_TITLE_SIZE = 17.sp
private val SETTING_TITLE_SIZE = 15.sp
private val SETTING_DESCRIPTION_SIZE = 12.sp
private val SETTING_DESCRIPTION_LINE_HEIGHT = 15.sp

private data class LanguageOption(
    val tag: String?,
    @param:StringRes val labelRes: Int,
)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption(null, R.string.settings_language_system),
    LanguageOption("en", R.string.settings_language_english),
    LanguageOption("it", R.string.settings_language_italian),
    LanguageOption("es", R.string.settings_language_spanish),
    LanguageOption("fr", R.string.settings_language_french),
    LanguageOption("pt", R.string.settings_language_portuguese),
    LanguageOption("pl", R.string.settings_language_polish),
    LanguageOption("zh", R.string.settings_language_chinese),
)

private const val GH_SPONSORS_URL = "https://github.com/sponsors/metiq-xyz"

private fun themeLabelRes(preference: ThemePreference): Int = when (preference) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onWavesEnabled: (Boolean) -> Unit = {},
    onDynamicColors: (Boolean) -> Unit = {},
    onWarmth: (Float) -> Unit,
    onWarmthPreview: (Float) -> Unit = {},
    onFadeSeconds: (Float) -> Unit = {},
    onTimerFadeSeconds: (Float) -> Unit = {},
    onRequestAudioFocus: (Boolean) -> Unit = {},
    onThemePreference: (ThemePreference) -> Unit = {},
    onTimerPresets: (List<Long>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Scaffold(
        containerColor = tokens.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_cd),
                            tint = tokens.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.background,
                ),
            )
        },
    ) { padding ->
        SettingsContent(
            settings = settings,
            onParticlesEnabled = onParticlesEnabled,
            onWavesEnabled = onWavesEnabled,
            onDynamicColors = onDynamicColors,
            onWarmth = onWarmth,
            onWarmthPreview = onWarmthPreview,
            onFadeSeconds = onFadeSeconds,
            onTimerFadeSeconds = onTimerFadeSeconds,
            onRequestAudioFocus = onRequestAudioFocus,
            onThemePreference = onThemePreference,
            onTimerPresets = onTimerPresets,
            onLanguageTag = onLanguageTag,
            onOpenLicenses = onOpenLicenses,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    onParticlesEnabled: (Boolean) -> Unit,
    onWavesEnabled: (Boolean) -> Unit = {},
    onWarmth: (Float) -> Unit,
    onTimerPresets: (List<Long>) -> Unit,
    onLanguageTag: (String?) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    onWarmthPreview: (Float) -> Unit = {},
    onFadeSeconds: (Float) -> Unit = {},
    onTimerFadeSeconds: (Float) -> Unit = {},
    onRequestAudioFocus: (Boolean) -> Unit = {},
    onThemePreference: (ThemePreference) -> Unit = {},
    onDynamicColors: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section(stringResource(R.string.settings_section_sound)) {
            WarmthRow(
                warmth = settings.warmth,
                onWarmth = onWarmth,
                onWarmthPreview = onWarmthPreview,
            )
            FadeNumberRow(
                label = stringResource(R.string.settings_fade_label),
                description = stringResource(R.string.settings_fade_description),
                seconds = settings.fadeSeconds,
                onCommit = onFadeSeconds,
            )
            FadeNumberRow(
                label = stringResource(R.string.settings_timer_fade_label),
                description = stringResource(R.string.settings_timer_fade_description),
                seconds = settings.timerFadeSeconds,
                onCommit = onTimerFadeSeconds,
            )
            ToggleRow(
                label = stringResource(R.string.settings_request_focus_label),
                description = stringResource(R.string.settings_request_focus_description),
                checked = settings.requestAudioFocus,
                onToggle = onRequestAudioFocus,
            )
        }
        Section(stringResource(R.string.settings_section_appearance)) {
            DropdownPickerRow(
                label = stringResource(R.string.settings_theme_label),
                options = ThemePreference.entries,
                current = settings.themePreference,
                labelFor = { stringResource(themeLabelRes(it)) },
                onPick = onThemePreference,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleRow(
                    label = stringResource(R.string.settings_dynamic_colors_label),
                    description = stringResource(R.string.settings_dynamic_colors_description),
                    checked = settings.dynamicColorsEnabled,
                    onToggle = onDynamicColors,
                )
            }
            ToggleRow(
                label = stringResource(R.string.settings_particles_label),
                description = stringResource(R.string.settings_particles_description),
                checked = settings.particlesEnabled,
                onToggle = onParticlesEnabled,
            )
            ToggleRow(
                label = stringResource(R.string.settings_waves_label),
                description = stringResource(R.string.settings_waves_description),
                checked = settings.wavesEnabled,
                onToggle = onWavesEnabled,
            )
        }
        Section(stringResource(R.string.settings_section_language)) {
            DropdownPickerRow(
                label = stringResource(R.string.settings_language_label),
                options = LANGUAGE_OPTIONS,
                current = LANGUAGE_OPTIONS.first { it.tag == settings.languageTag },
                labelFor = { stringResource(it.labelRes) },
                onPick = { onLanguageTag(it.tag) },
            )
        }
        Section(stringResource(R.string.settings_section_timer_presets)) {
            TimerPresetsEditor(
                presetsSeconds = settings.timerPresetsSeconds,
                onChange = onTimerPresets,
            )
        }
        Section(stringResource(R.string.settings_section_support)) {
            LinkRow(
                label = stringResource(R.string.settings_rate_label, BuildConfig.STORE_NAME),
                onClick = { openStoreRating(context) },
            )
            LinkRow(
                label = stringResource(R.string.settings_donate_kofi_label),
                description = stringResource(R.string.settings_donate_description),
                onClick = { openUrl(context, KOFI_URL) },
            )
            LinkRow(
                label = stringResource(R.string.settings_donate_github_label),
                onClick = { openUrl(context, GH_SPONSORS_URL) },
            )
        }
        Section(stringResource(R.string.settings_section_about)) {
            LabeledValue(stringResource(R.string.settings_about_version), version)
            LinkRow(
                label = stringResource(R.string.settings_about_open_licenses),
                onClick = onOpenLicenses,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.foreground)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            color = tokens.textPrimary,
            modifier = Modifier.padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 12.dp),
            style = TextStyle(
                fontFamily = Inter,
                fontSize = SECTION_TITLE_SIZE,
                fontWeight = FontWeight.Bold,
            ),
        )
        content()
    }
}

@Composable
private fun <T> DropdownPickerRow(
    label: String,
    options: List<T>,
    current: T,
    labelFor: @Composable (T) -> String,
    onPick: (T) -> Unit,
    leadingFor: (@Composable (T) -> Unit)? = null,
) {
    val tokens = LocalMetiqColors.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
        )
        Spacer(Modifier.width(SETTING_ACTION_GAP))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingFor != null) {
                    leadingFor(current)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = labelFor(current),
                    color = tokens.textSecondary,
                    style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = tokens.foreground,
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = labelFor(opt),
                                style = TextStyle(fontFamily = Inter, fontSize = 16.sp),
                            )
                        },
                        leadingIcon = leadingFor?.let { { it(opt) } },
                        onClick = {
                            onPick(opt)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(textColor = tokens.textPrimary),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = tokens.textPrimary,
                style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
            )
            if (description != null) {
                Spacer(Modifier.height(SETTING_TITLE_GAP))
                Text(
                    text = description,
                    color = tokens.textSecondary,
                    style = TextStyle(fontFamily = Inter, fontSize = SETTING_DESCRIPTION_SIZE, lineHeight = SETTING_DESCRIPTION_LINE_HEIGHT),
                )
            }
        }
        Spacer(Modifier.width(SETTING_ACTION_GAP))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.background,
                checkedTrackColor = tokens.textPrimary,
                uncheckedThumbColor = tokens.textSecondary,
                uncheckedTrackColor = tokens.background,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

private val WARMTH_TRACK_HEIGHT = 6.dp
private val WARMTH_THUMB_SIZE = 16.dp
private val WARMTH_PREVIEW_THROTTLE = 30.milliseconds

@Composable
private fun WarmthRow(
    warmth: Float,
    onWarmth: (Float) -> Unit,
    onWarmthPreview: (Float) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    var local by remember { mutableFloatStateOf(warmth) }
    var interacting by remember { mutableStateOf(false) }
    LaunchedEffect(warmth) { local = warmth }
    LaunchedEffect(Unit) {
        snapshotFlow { local }.collect { v ->
            if (interacting) onWarmthPreview(v)
            delay(WARMTH_PREVIEW_THROTTLE)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_warmth_label),
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
        )
        Spacer(Modifier.height(SETTING_TITLE_GAP))
        Text(
            text = stringResource(R.string.settings_warmth_description),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = SETTING_DESCRIPTION_SIZE, lineHeight = SETTING_DESCRIPTION_LINE_HEIGHT),
        )
        Spacer(Modifier.height(12.dp))
        ThinSlider(
            value = local,
            onValue = { interacting = true; local = it },
            onValueSettled = { interacting = false; onWarmth(local) },
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_warmth_bright),
                color = tokens.textSecondary,
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
            )
            Text(
                text = stringResource(R.string.settings_warmth_warm),
                color = tokens.textSecondary,
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
            )
        }
    }
}

// Formats a fade value without a trailing ".0" (e.g. "5" not "5.0", but "0.3").
private fun formatFadeSeconds(seconds: Float): String {
    val s = clampFadeSeconds(seconds)
    return if (s == s.toLong().toFloat()) s.toLong().toString() else s.toString()
}

@Composable
private fun FadeNumberRow(
    label: String,
    description: String,
    seconds: Float,
    onCommit: (Float) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val focusManager = LocalFocusManager.current
    // Buffer resets whenever the committed value changes (incl. clamping on commit).
    var text by remember(seconds) { mutableStateOf(formatFadeSeconds(seconds)) }
    val commit: () -> Unit = {
        focusManager.clearFocus()
        val parsed = clampFadeSeconds(text.toFloatOrNull() ?: seconds)
        text = formatFadeSeconds(parsed)
        if (parsed != seconds) onCommit(parsed)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = tokens.textPrimary,
                    style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
                )
                Spacer(Modifier.height(SETTING_TITLE_GAP))
                Text(
                    text = description,
                    color = tokens.textSecondary,
                    style = TextStyle(fontFamily = Inter, fontSize = SETTING_DESCRIPTION_SIZE, lineHeight = SETTING_DESCRIPTION_LINE_HEIGHT),
                )
            }
            Spacer(Modifier.width(SETTING_ACTION_GAP))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.background)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { txt ->
                        // Digits and a single decimal separator, max 4 chars ("30" or "0.3").
                        if (txt.length <= 4 && txt.all { it.isDigit() || it == '.' } &&
                            txt.count { it == '.' } <= 1
                        ) {
                            text = txt
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Inter,
                        fontSize = 15.sp,
                        color = tokens.textPrimary,
                    ),
                    cursorBrush = SolidColor(tokens.textPrimary),
                    modifier = Modifier.onFocusChanged { if (!it.isFocused) commit() },
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_fade_unit),
                color = tokens.textSecondary,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun ThinSlider(
    value: Float,
    onValue: (Float) -> Unit,
    onValueSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val inactive = tokens.subtleFill
    val activeFill = tokens.sliderActiveFill
    val thumbColor = tokens.textPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WARMTH_THUMB_SIZE)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValue((offset.x / size.width).coerceIn(0f, 1f))
                    onValueSettled()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { onValueSettled() },
                ) { change, _ ->
                    change.consume()
                    onValue((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val cy = size.height / 2f
            val barH = WARMTH_TRACK_HEIGHT.toPx()
            val corner = CornerRadius(barH / 2f, barH / 2f)
            val thumbR = WARMTH_THUMB_SIZE.toPx() / 2f
            val thumbX = (value * w).coerceIn(thumbR, w - thumbR)
            drawRoundRect(
                color = inactive,
                topLeft = Offset(0f, cy - barH / 2f),
                size = Size(w, barH),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = activeFill,
                topLeft = Offset(0f, cy - barH / 2f),
                size = Size(thumbX, barH),
                cornerRadius = corner,
            )
            drawCircle(color = thumbColor, radius = thumbR, center = Offset(thumbX, cy))
        }
    }
}

@Composable
private fun TimerPresetsEditor(
    presetsSeconds: List<Long>,
    onChange: (List<Long>) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val focusManager = LocalFocusManager.current
    val buffers = remember(presetsSeconds) {
        mutableStateOf(
            List(MAX_TIMER_PRESETS) { idx ->
                presetsSeconds.getOrNull(idx)?.let { (it / 60L).toString() } ?: ""
            }
        )
    }
    val commit: () -> Unit = {
        focusManager.clearFocus()
        val newPresets = buffers.value.mapNotNull { it.toLongOrNull()?.takeIf { v -> v > 0 } }
            .map { it * 60L }
        if (newPresets != presetsSeconds) onChange(newPresets)
    }
    Column(
        modifier = Modifier.padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buffers.value.forEachIndexed { idx, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "#${idx + 1}",
                    color = tokens.textSecondary,
                    style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                    modifier = Modifier.width(28.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.background)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { txt ->
                            if (txt.length <= 4 && txt.all { it.isDigit() }) {
                                val updated = buffers.value.toMutableList()
                                updated[idx] = txt
                                buffers.value = updated
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = Inter,
                            fontSize = 15.sp,
                            color = tokens.textPrimary,
                        ),
                        cursorBrush = SolidColor(tokens.textPrimary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_timer_unit_min),
                    color = tokens.textSecondary,
                    style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(tokens.textPrimary)
                .clickable { commit() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_timer_save),
                color = tokens.foreground,
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
            )
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = SETTING_TITLE_SIZE),
        )
        if (description != null) {
            Spacer(Modifier.height(SETTING_TITLE_GAP))
            Text(
                text = description,
                color = tokens.textSecondary,
                style = TextStyle(fontFamily = Inter, fontSize = SETTING_DESCRIPTION_SIZE, lineHeight = SETTING_DESCRIPTION_LINE_HEIGHT),
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    val tokens = LocalMetiqColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SECTION_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = tokens.textSecondary,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp),
        )
        Text(
            text = value,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, textAlign = TextAlign.End),
        )
    }
}

@Preview(name = "Settings · Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Settings · Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingsScreenPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        SettingsScreen(
            settings = DEFAULT_SETTINGS,
            onParticlesEnabled = {},
            onWarmth = {},
            onTimerPresets = {},
            onLanguageTag = {},
            onBack = {},
            onOpenLicenses = {},
        )
    }
}
