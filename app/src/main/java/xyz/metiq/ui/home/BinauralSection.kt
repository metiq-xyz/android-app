package xyz.metiq.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.components.ParticleField
import xyz.metiq.ui.components.WaveRings
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

internal const val BINAURAL_CARRIER_HZ = 216f
private val EXTENDED_FAB_CORNER: Dp = 16.dp

internal data class BinauralBand(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val feelRes: Int,
    val glyph: String,
    val beatHz: Float,
)

internal val BINAURAL_BANDS = listOf(
    BinauralBand("delta", R.string.binaural_delta, R.string.binaural_delta_feel, "δ", 2f),
    BinauralBand("theta", R.string.binaural_theta, R.string.binaural_theta_feel, "θ", 6f),
    BinauralBand("alpha", R.string.binaural_alpha, R.string.binaural_alpha_feel, "α", 10f),
    BinauralBand("beta", R.string.binaural_beta, R.string.binaural_beta_feel, "β", 18f),
    BinauralBand("gamma", R.string.binaural_gamma, R.string.binaural_gamma_feel, "γ", 40f),
)

@Composable
internal fun binauralAccentFor(id: String): Color {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "delta" -> tokens.binauralDelta
        "theta" -> tokens.binauralTheta
        "alpha" -> tokens.binauralAlpha
        "beta" -> tokens.binauralBeta
        "gamma" -> tokens.binauralGamma
        else -> tokens.binauralAlpha
    }
}

@Composable
internal fun BinauralFab(band: BinauralBand?, wavesOn: Boolean, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    val accent = band?.let { binauralAccentFor(it.id) }
    val container by animateColorAsState(
        targetValue = accent?.let { lerp(it, tokens.accentHighlight, 0.40f) } ?: tokens.cellBackground,
        animationSpec = tween(durationMillis = 300),
        label = "binauralFabColor",
    )
    val contentColor = accent?.let { lerp(it, tokens.accentShade, 0.55f) } ?: tokens.textPrimary
    val density = LocalDensity.current
    var fabSize by remember { mutableStateOf(DpSize.Zero) }
    Box(contentAlignment = Alignment.Center) {
        if (fabSize.width > 0.dp) {
            WaveRings(
                color = container,
                active = wavesOn,
                baseWidth = fabSize.width,
                baseHeight = fabSize.height,
                cornerRadius = EXTENDED_FAB_CORNER,
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .size(fabSize.width * WAVE_OVERSHOOT, fabSize.height * WAVE_OVERSHOOT),
            )
        }
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = container,
            contentColor = contentColor,
            modifier = Modifier.onSizeChanged {
                fabSize = with(density) { DpSize(it.width.toDp(), it.height.toDp()) }
            },
        ) {
            if (band != null) {
                Text(
                    text = band.glyph,
                    style = TextStyle(fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Hearing,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.binaural_fab_label),
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BinauralSheet(
    activeBand: String?,
    wavesOn: Boolean,
    particlesOn: Boolean,
    volume: Float,
    onTap: (String) -> Unit,
    onVolume: (Float) -> Unit,
    onVolumeSettled: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.foreground,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
      Box {
        val particleAlpha by animateFloatAsState(
            targetValue = if (particlesOn) 1f else 0f,
            animationSpec = tween(durationMillis = PARTICLE_FADE_MS),
            label = "binauralParticleFade",
        )
        var particleFill by remember { mutableStateOf(Color.Unspecified) }
        if (activeBand != null) {
            particleFill = lerp(binauralAccentFor(activeBand), tokens.accentHighlight, 0.40f)
        }
        if (particleAlpha > 0f && particleFill != Color.Unspecified) {
            ParticleField(
                color = particleFill,
                count = 28,
                seed = 0xB1AA,
                intensity = particleAlpha,
                modifier = Modifier.matchParentSize(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CONTENT_HORIZONTAL_PADDING)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BinauralHeadphoneNotice()
            Spacer(Modifier.height(20.dp))
            BinauralGrid(activeBand = activeBand, wavesOn = wavesOn, onTap = onTap)
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(visible = activeBand != null) {
                val accent = activeBand?.let { binauralAccentFor(it) } ?: tokens.textPrimary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.binaural_volume),
                        color = tokens.textSecondary,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    )
                    Spacer(Modifier.height(10.dp))
                    MixerControl(
                        active = true,
                        volume = volume,
                        accent = accent,
                        onVolume = onVolume,
                        onVolumeSettled = onVolumeSettled,
                    )
                }
            }
        }
      }
    }
}

@Composable
private fun BinauralHeadphoneNotice() {
    val tokens = LocalMetiqColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.cellBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Headphones,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.binaural_headphone_notice),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
        )
    }
}

@Composable
private fun BinauralGrid(
    activeBand: String?,
    wavesOn: Boolean,
    onTap: (String) -> Unit,
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
            BINAURAL_BANDS.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { band ->
                        BinauralOrb(
                            modifier = Modifier.width(cellWidth),
                            orbSize = cellWidth.coerceAtMost(BUTTON_HEIGHT),
                            band = band,
                            wavesOn = wavesOn && activeBand == band.id,
                            onTap = { onTap(band.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BinauralOrb(
    modifier: Modifier = Modifier,
    orbSize: Dp = BUTTON_HEIGHT,
    band: BinauralBand,
    wavesOn: Boolean,
    onTap: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val label = stringResource(band.labelRes)
    val accent = binauralAccentFor(band.id)
    val orbFill = lerp(accent, tokens.accentHighlight, 0.40f)
    val glyphTint = lerp(accent, tokens.accentShade, 0.55f)
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
                baseWidth = orbSize,
                active = wavesOn,
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
                Text(
                    text = band.glyph,
                    color = glyphTint,
                    style = TextStyle(fontFamily = Inter, fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(band.feelRes),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
            textAlign = TextAlign.Center,
        )
    }
}
