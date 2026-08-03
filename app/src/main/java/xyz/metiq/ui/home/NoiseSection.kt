package xyz.metiq.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.metiq.R
import xyz.metiq.audio.PlaybackService
import xyz.metiq.ui.components.WaveRings
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.NoisePalette

internal data class NoiseColor(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val noiseTitleRes: Int,
)

internal val NOISE_COLORS = listOf(
    NoiseColor("pink", R.string.color_pink, R.string.noise_title_pink),
    NoiseColor("brown", R.string.color_brown, R.string.noise_title_brown),
    NoiseColor("white", R.string.color_white, R.string.noise_title_white),
    NoiseColor("grey", R.string.color_grey, R.string.noise_title_grey),
)
internal val NOISE_IDS = NOISE_COLORS.map { it.id }.toSet()

@Composable
internal fun paletteFor(id: String): NoisePalette {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "pink" -> tokens.noisePink
        "brown" -> tokens.noiseBrown
        "white" -> tokens.noiseWhite
        "grey" -> tokens.noiseGrey
        else -> tokens.noiseWhite
    }
}

@Composable
internal fun ColorGrid(
    activeId: String?,
    wavesOn: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NOISE_COLORS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { color ->
                    val active = activeId == color.id
                    ColorButton(
                        color = color,
                        active = active,
                        waveOn = active && wavesOn,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: NoiseColor,
    active: Boolean,
    waveOn: Boolean,
    onSelect: (String) -> Unit,
) {
    val palette = paletteFor(color.id)
    Box(
        modifier = Modifier.size(BUTTON_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        WaveRings(
            color = palette.wave,
            baseWidth = BUTTON_HEIGHT,
            active = waveOn,
            modifier = Modifier
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .size(BUTTON_HEIGHT * WAVE_OVERSHOOT),
        )
        ColorCircle(
            palette = palette,
            active = active,
            contentDescription = stringResource(color.labelRes),
            onClick = { onSelect(color.id) },
        )
    }
}

@Composable
private fun ColorCircle(
    palette: NoisePalette,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "circleBorder",
    )
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(BUTTON_HEIGHT)
            .clip(shape)
            .background(palette.fill)
            .border(borderWidth, palette.fill, shape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
    )
}

internal suspend fun selectColor(
    id: String,
    current: String?,
    binder: PlaybackService.EngineBinder?,
    setActive: (String?) -> Unit,
    sync: () -> Unit,
) {
    val b = binder ?: return
    if (current == id) {
        setActive(null)
        b.engine.stopLayer(id)
        sync()
        return
    }
    setActive(id)
    sync()
    b.engine.switchTo(id, "audio/noise/$id.ogg")
}
