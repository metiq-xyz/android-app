package xyz.metiq.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.CustomMix
import xyz.metiq.MAX_CUSTOM_MIX_NAME_LENGTH
import xyz.metiq.R
import xyz.metiq.ui.components.WaveRings
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import kotlin.math.abs

internal const val AMBIENT_DEFAULT_VOLUME = 0.7f
internal const val AMBIENT_PARTICLE_TOTAL = 90
private const val MIXER_OFF_WIDTH_FRACTION = 0.42f
private val MIXER_THUMB_SIZE: Dp = 16.dp
private val MIXER_TRACK_HEIGHT: Dp = 6.dp
private val MIXER_PILL_HEIGHT: Dp = 26.dp
private val MIXER_OFF_LABEL_PADDING: Dp = 10.dp
private val MIX_EDGE_FADE_WIDTH: Dp = 24.dp

internal data class AmbientSound(
    val id: String,
    @param:StringRes val labelRes: Int,
    val iconVector: ImageVector? = null,
    @param:DrawableRes val iconResId: Int? = null,
)

internal data class MixPreset(
    val id: String,
    @param:StringRes val labelRes: Int,
    val layers: Map<String, Float>,
)

internal val AMBIENT_SOUNDS = listOf(
    AmbientSound("seawaves", R.string.ambient_seawaves, iconVector = Icons.Outlined.Waves),
    AmbientSound("rain", R.string.ambient_rain, iconVector = Icons.Outlined.WaterDrop),
    AmbientSound("fire", R.string.ambient_fire, iconVector = Icons.Outlined.LocalFireDepartment),
    AmbientSound("birds", R.string.ambient_birds, iconResId = R.drawable.ic_ambient_birds),
    AmbientSound("cafe", R.string.ambient_cafe, iconVector = Icons.Outlined.Storefront),
    AmbientSound("wind", R.string.ambient_wind, iconVector = Icons.Outlined.Air),
)
internal val AMBIENT_IDS = AMBIENT_SOUNDS.map { it.id }.toSet()

internal val PREMADE_MIXES = listOf(
    MixPreset("cabin", R.string.mix_cabin, mapOf("fire" to 0.8f, "rain" to 0.45f)),
    MixPreset("beach", R.string.mix_beach, mapOf("seawaves" to 0.7f, "birds" to 0.5f)),
    MixPreset("bar", R.string.mix_bar, mapOf("cafe" to 0.8f, "rain" to 0.35f)),
)

@Composable
internal fun ambientAccentFor(id: String): Color {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "seawaves" -> tokens.ambientSeawaves
        "rain" -> tokens.ambientRain
        "fire" -> tokens.ambientFire
        "birds" -> tokens.ambientBirds
        "cafe" -> tokens.ambientCafe
        "wind" -> tokens.ambientWind
        else -> tokens.ambientSeawaves
    }
}

internal fun mixMatches(active: Map<String, Float>, layers: Map<String, Float>): Boolean {
    if (active.keys != layers.keys) return false
    return active.all { (id, v) -> abs(v - (layers[id] ?: return false)) < 0.01f }
}

@Composable
internal fun SaveMixDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_save_title), fontFamily = Inter) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_CUSTOM_MIX_NAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.mix_save_name_hint), fontFamily = Inter) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim().replace('|', ' ')) },
            ) { Text(stringResource(R.string.mix_save_confirm), fontFamily = Inter) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
            }
        },
    )
}

@Composable
internal fun MixPresets(
    customMixes: List<CustomMix>,
    activeMix: Map<String, Float>,
    onApply: (Map<String, Float>) -> Unit,
    onStop: () -> Unit,
    onDelete: (CustomMix) -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val expanded =
                    constraints.maxWidth + (CONTENT_HORIZONTAL_PADDING * 2).roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = expanded, maxWidth = expanded)
                )
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fade = MIX_EDGE_FADE_WIDTH.toPx()
                if (scroll.value > 0) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent, 1f to tokens.scrim,
                            startX = 0f, endX = fade,
                        ),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
                if (scroll.value < scroll.maxValue) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to tokens.scrim, 1f to Color.Transparent,
                            startX = size.width - fade, endX = size.width,
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
            .horizontalScroll(scroll)
            .padding(horizontal = CONTENT_HORIZONTAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        customMixes.forEach { mix ->
            key(mix.name) {
                val active = mixMatches(activeMix, mix.layers)
                MixChip(
                    label = mix.name,
                    active = active,
                    onClick = { if (active) onStop() else onApply(mix.layers) },
                    trailingIcon = Icons.Outlined.Delete,
                    onTrailingClick = { onDelete(mix) },
                )
            }
        }
        if (customMixes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(tokens.divider),
            )
        }
        PREMADE_MIXES.forEach { preset ->
            val active = mixMatches(activeMix, preset.layers)
            MixChip(
                label = stringResource(preset.labelRes),
                active = active,
                onClick = { if (active) onStop() else onApply(preset.layers) },
            )
        }
    }
}

@Composable
private fun MixChip(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val tokens = LocalMetiqColors.current
    val background = if (active) tokens.textPrimary else tokens.cellBackground
    val content = if (active) tokens.background else tokens.textPrimary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = content,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
        if (trailingIcon != null && onTrailingClick != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = stringResource(R.string.mix_delete_confirm),
                tint = content.copy(alpha = tokens.accentIconAlpha),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTrailingClick),
            )
        }
    }
}

@Composable
internal fun SaveMixButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else tokens.disabledAlpha,
        animationSpec = tween(durationMillis = ALPHA_ANIM_MS),
        label = "saveMixAlpha",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.cellBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.mix_save_chip),
            color = tokens.textPrimary.copy(alpha = contentAlpha),
            style = TextStyle(
                fontFamily = Inter,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = tokens.textPrimary.copy(alpha = contentAlpha),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun AmbientGrid(
    levels: Map<String, Float>,
    wavesOn: Boolean,
    onTap: (String) -> Unit,
    onVolume: (String, Float) -> Unit,
    onVolumeSettled: (String) -> Unit,
) {
    val columns = 3
    val spacing = 8.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = ((maxWidth - spacing * (columns - 1)) / columns).coerceAtMost(AMBIENT_CELL_WIDTH)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AMBIENT_SOUNDS.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { sound ->
                        val level = levels[sound.id]
                        AmbientOrb(
                            modifier = Modifier.width(cellWidth),
                            orbSize = cellWidth.coerceAtMost(BUTTON_HEIGHT),
                            sound = sound,
                            active = level != null,
                            wavesOn = wavesOn,
                            volume = level ?: AMBIENT_DEFAULT_VOLUME,
                            onTap = { onTap(sound.id) },
                            onVolume = { onVolume(sound.id, it) },
                            onVolumeSettled = { onVolumeSettled(sound.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientOrb(
    modifier: Modifier = Modifier,
    orbSize: Dp = BUTTON_HEIGHT,
    sound: AmbientSound,
    active: Boolean,
    wavesOn: Boolean,
    volume: Float,
    onTap: () -> Unit,
    onVolume: (Float) -> Unit,
    onVolumeSettled: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val label = stringResource(sound.labelRes)
    val accent = ambientAccentFor(sound.id)
    val orbFill = lerp(accent, tokens.accentHighlight, 0.40f)
    val iconTint = lerp(accent, tokens.accentShade, 0.55f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(orbSize),
            contentAlignment = Alignment.Center,
        ) {
            WaveRings(
                color = orbFill,
                diameter = orbSize,
                active = active && wavesOn,
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .size(orbSize * WAVE_OVERSHOOT),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(orbFill)
                    .clickable(onClick = onTap)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                val iconModifier = Modifier.size(34.dp)
                when {
                    sound.iconVector != null -> Icon(
                        imageVector = sound.iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = iconModifier,
                    )

                    sound.iconResId != null -> Icon(
                        painter = painterResource(sound.iconResId),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = iconModifier,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MixerControl(
            active = active,
            volume = volume,
            accent = accent,
            onVolume = onVolume,
            onVolumeSettled = onVolumeSettled,
        )
    }
}

@Composable
internal fun MixerControl(
    active: Boolean,
    volume: Float,
    accent: Color,
    onVolume: (Float) -> Unit,
    onVolumeSettled: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val trackColor = tokens.cellBackground
    val thumbColor = lerp(accent, tokens.accentHighlight, 0.40f)
    val offLabel = stringResource(R.string.ambient_off)
    val offLabelStyle = TextStyle(fontFamily = Inter, fontSize = 12.sp)
    val textMeasurer = rememberTextMeasurer()
    val t by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "mixerMorph",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (active) 200 else 120,
            delayMillis = if (active) 140 else 0,
        ),
        label = "mixerThumb",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MIXER_THUMB_SIZE)
            .padding(horizontal = 4.dp)
            .pointerInput(active) {
                if (!active) return@pointerInput
                detectTapGestures { offset ->
                    onVolume((offset.x / size.width).coerceIn(0f, 1f))
                    onVolumeSettled()
                }
            }
            .pointerInput(active) {
                if (!active) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { onVolumeSettled() },
                ) { change, _ ->
                    change.consume()
                    onVolume((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val cy = size.height / 2f
            val barH = MIXER_PILL_HEIGHT.toPx() +
                (MIXER_TRACK_HEIGHT.toPx() - MIXER_PILL_HEIGHT.toPx()) * t
            val labelW = textMeasurer.measure(offLabel, offLabelStyle).size.width +
                MIXER_OFF_LABEL_PADDING.toPx() * 2f
            val offW = maxOf(MIXER_OFF_WIDTH_FRACTION * w, labelW)
            val barW = offW + (w - offW) * t
            val barLeft = (w - barW) / 2f
            val corner = CornerRadius(barH / 2f, barH / 2f)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(barLeft, cy - barH / 2f),
                size = Size(barW, barH),
                cornerRadius = corner,
            )
            val thumbR = MIXER_THUMB_SIZE.toPx() / 2f
            val thumbX = (barLeft + volume * barW).coerceIn(barLeft + thumbR, barLeft + barW - thumbR)
            if (t > 0f) {
                drawRoundRect(
                    color = accent.copy(alpha = t),
                    topLeft = Offset(barLeft, cy - barH / 2f),
                    size = Size(thumbX - barLeft, barH),
                    cornerRadius = corner,
                )
            }
            val r = thumbR * thumbScale
            if (r > 0f) {
                drawCircle(color = thumbColor, radius = r, center = Offset(thumbX, cy))
            }
        }
        if (t < 0.999f) {
            Text(
                text = offLabel,
                color = tokens.textPrimary.copy(alpha = tokens.disabledAlpha * (1f - t)),
                style = offLabelStyle,
            )
        }
    }
}
