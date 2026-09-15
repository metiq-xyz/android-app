package xyz.metiq.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.metiq.R
import xyz.metiq.ui.theme.LocalMetiqColors
import kotlin.math.abs

internal data class NoiseColor(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val noiseTitleRes: Int,
)

internal data class AmbientSound(
    val id: String,
    @param:StringRes val labelRes: Int,
    val iconVector: ImageVector? = null,
    @param:DrawableRes val iconResId: Int? = null,
)

internal data class BinauralBand(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val feelRes: Int,
    val glyph: String,
    val beatHz: Float,
)

internal data class MixPreset(
    val id: String,
    @param:StringRes val labelRes: Int,
    val layers: Map<String, Float>,
)

internal val NOISE_COLORS = listOf(
    NoiseColor("pink", R.string.color_pink, R.string.noise_title_pink),
    NoiseColor("brown", R.string.color_brown, R.string.noise_title_brown),
    NoiseColor("white", R.string.color_white, R.string.noise_title_white),
    NoiseColor("grey", R.string.color_grey, R.string.noise_title_grey),
)
internal val NOISE_IDS = NOISE_COLORS.map { it.id }.toSet()

internal val AMBIENT_SOUNDS = listOf(
    AmbientSound("seawaves", R.string.ambient_seawaves, iconVector = Icons.Outlined.Waves),
    AmbientSound("rain", R.string.ambient_rain, iconVector = Icons.Outlined.WaterDrop),
    AmbientSound("fire", R.string.ambient_fire, iconVector = Icons.Outlined.LocalFireDepartment),
    AmbientSound("birds", R.string.ambient_birds, iconResId = R.drawable.ic_ambient_birds),
    AmbientSound("cafe", R.string.ambient_cafe, iconVector = Icons.Outlined.Storefront),
    AmbientSound("wind", R.string.ambient_wind, iconVector = Icons.Outlined.Air),
    AmbientSound("crickets", R.string.ambient_crickets, iconVector = Icons.Outlined.Grass),
    AmbientSound("stream", R.string.ambient_stream, iconVector = Icons.Outlined.Water),
)
internal val AMBIENT_IDS = AMBIENT_SOUNDS.map { it.id }.toSet()

internal const val BINAURAL_CARRIER_HZ = 216f

internal val BINAURAL_BANDS = listOf(
    BinauralBand("delta", R.string.binaural_delta, R.string.binaural_delta_feel, "δ", 2f),
    BinauralBand("theta", R.string.binaural_theta, R.string.binaural_theta_feel, "θ", 6f),
    BinauralBand("alpha", R.string.binaural_alpha, R.string.binaural_alpha_feel, "α", 10f),
    BinauralBand("beta", R.string.binaural_beta, R.string.binaural_beta_feel, "β", 18f),
    BinauralBand("gamma", R.string.binaural_gamma, R.string.binaural_gamma_feel, "γ", 40f),
)
internal val BINAURAL_BAND_IDS = BINAURAL_BANDS.map { it.id }.toSet()

internal val PREMADE_MIXES = listOf(
    MixPreset("cabin", R.string.mix_cabin, mapOf("fire" to 0.8f, "rain" to 0.45f)),
    MixPreset("beach", R.string.mix_beach, mapOf("seawaves" to 0.7f, "birds" to 0.5f)),
    MixPreset("bar", R.string.mix_bar, mapOf("cafe" to 0.8f, "rain" to 0.35f)),
)

@Composable
internal fun noiseColorFor(id: String): Color {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "pink" -> tokens.noisePink
        "brown" -> tokens.noiseBrown
        "grey" -> tokens.noiseGrey
        else -> tokens.noiseWhite
    }
}

@Composable
internal fun ambientAccentFor(id: String): Color {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "rain" -> tokens.ambientRain
        "fire" -> tokens.ambientFire
        "birds" -> tokens.ambientBirds
        "cafe" -> tokens.ambientCafe
        "wind" -> tokens.ambientWind
        "crickets" -> tokens.ambientCrickets
        "stream" -> tokens.ambientStream
        else -> tokens.ambientSeawaves
    }
}

@Composable
internal fun binauralAccentFor(id: String): Color {
    val tokens = LocalMetiqColors.current
    return when (id) {
        "delta" -> tokens.binauralDelta
        "theta" -> tokens.binauralTheta
        "beta" -> tokens.binauralBeta
        "gamma" -> tokens.binauralGamma
        else -> tokens.binauralAlpha
    }
}

internal fun mixMatches(active: Map<String, Float>, layers: Map<String, Float>): Boolean {
    if (active.size != layers.size) return false
    return layers.all { (id, v) -> active[id]?.let { abs(it - v) < 0.01f } == true }
}
