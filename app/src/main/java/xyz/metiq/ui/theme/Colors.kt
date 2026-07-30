package xyz.metiq.ui.theme

import androidx.compose.ui.graphics.Color

data class NoisePalette(val fill: Color, val onFill: Color, val wave: Color)

data class MetiqColorTokens(
    val background: Color,
    val foreground: Color,
    val cellBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val subtleFill: Color,
    val sliderActiveFill: Color,
    val scrim: Color,
    val ratingStar: Color,
    val logo: Color,
    val accentHighlight: Color,
    val accentShade: Color,
    val disabledAlpha: Float,
    val accentIconAlpha: Float,
    val waveMaxAlpha: Float,
    val particleBaseAlpha: Float,
    val particleAlphaJitter: Float,
    val noisePink: NoisePalette,
    val noiseBrown: NoisePalette,
    val noiseWhite: NoisePalette,
    val noiseGrey: NoisePalette,
    val binauralDelta: Color,
    val binauralTheta: Color,
    val binauralAlpha: Color,
    val binauralBeta: Color,
    val binauralGamma: Color,
    val ambientSeawaves: Color,
    val ambientRain: Color,
    val ambientFire: Color,
    val ambientBirds: Color,
    val ambientCafe: Color,
    val ambientWind: Color,
)

object MetiqColors {
    private val PinkFill = Color(0xFFFFC6F2)
    private val BrownFill = Color(0xFFA34E08)
    private val GreyFill = Color(0xFF565656)

    private val AmbientSeawaves = Color(0xFF3A7BD5)
    private val AmbientRain = Color(0xFF6C5CE7)
    private val AmbientFire = Color(0xFFE8662B)
    private val AmbientBirds = Color(0xFF4CAF7D)
    private val AmbientCafe = Color(0xFFB8862B)
    private val AmbientWind = Color(0xFF3AA6B9)

    private val BinauralDelta = Color(0xFF7B6CF6)
    private val BinauralTheta = Color(0xFF4C7BE8)
    private val BinauralAlpha = Color(0xFF2FA9A0)
    private val BinauralBeta = Color(0xFFCB9A2E)
    private val BinauralGamma = Color(0xFFDF6478)

    val Dark = MetiqColorTokens(
        background = Color(0xFF111010),
        foreground = Color(0xFF222121),
        cellBackground = Color(0xFF2E2C2D),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.50f),
        divider = Color.White.copy(alpha = 0.08f),
        subtleFill = Color.White.copy(alpha = 0.12f),
        sliderActiveFill = Color.White.copy(alpha = 0.55f),
        scrim = Color.Black,
        ratingStar = Color(0xFFFFC65A),
        logo = Color(0xFFDBF1B3),
        accentHighlight = Color.White,
        accentShade = Color.Black,
        disabledAlpha = 0.5f,
        accentIconAlpha = 0.7f,
        waveMaxAlpha = 0.9f,
        particleBaseAlpha = 0.2f,
        particleAlphaJitter = 0.4f,
        noisePink = NoisePalette(PinkFill, Color.Black, wave = PinkFill),
        noiseBrown = NoisePalette(BrownFill, Color.White, wave = BrownFill),
        noiseWhite = NoisePalette(Color.White, Color.Black, wave = Color.White),
        noiseGrey = NoisePalette(GreyFill, Color.White, wave = GreyFill),
        binauralDelta = BinauralDelta,
        binauralTheta = BinauralTheta,
        binauralAlpha = BinauralAlpha,
        binauralBeta = BinauralBeta,
        binauralGamma = BinauralGamma,
        ambientSeawaves = AmbientSeawaves,
        ambientRain = AmbientRain,
        ambientFire = AmbientFire,
        ambientBirds = AmbientBirds,
        ambientCafe = AmbientCafe,
        ambientWind = AmbientWind,
    )

    val Light = MetiqColorTokens(
        background = Color(0xFFE5E7EB),
        foreground = Color(0xFFF5F7FA),
        cellBackground = Color(0xFFECEEF3),
        textPrimary = Color(0xFF111827),
        textSecondary = Color.Black.copy(alpha = 0.50f),
        divider = Color.Black.copy(alpha = 0.08f),
        subtleFill = Color.Black.copy(alpha = 0.12f),
        sliderActiveFill = Color.Black.copy(alpha = 0.55f),
        scrim = Color.Black,
        ratingStar = Color(0xFFFFC65A),
        logo = Color(0xFFADC08B),
        accentHighlight = Color.White,
        accentShade = Color.Black,
        disabledAlpha = 0.5f,
        accentIconAlpha = 0.7f,
        waveMaxAlpha = 0.9f,
        particleBaseAlpha = 0.2f,
        particleAlphaJitter = 0.4f,
        noisePink = NoisePalette(PinkFill, Color.Black, wave = PinkFill),
        noiseBrown = NoisePalette(BrownFill, Color.White, wave = BrownFill),
        noiseWhite = NoisePalette(Color.White, Color.Black, wave = Color(0xFFDCDFE7)),
        noiseGrey = NoisePalette(GreyFill, Color.White, wave = GreyFill),
        binauralDelta = BinauralDelta,
        binauralTheta = BinauralTheta,
        binauralAlpha = BinauralAlpha,
        binauralBeta = BinauralBeta,
        binauralGamma = BinauralGamma,
        ambientSeawaves = AmbientSeawaves,
        ambientRain = AmbientRain,
        ambientFire = AmbientFire,
        ambientBirds = AmbientBirds,
        ambientCafe = AmbientCafe,
        ambientWind = AmbientWind,
    )
}
