package xyz.metiq.ui.theme

import androidx.compose.ui.graphics.Color

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
    val noisePink: Color,
    val noiseBrown: Color,
    val noiseWhite: Color,
    val noiseGrey: Color,
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
    val ambientCrickets: Color,
    val ambientStream: Color,
    val categoryNoise: Color,
    val categoryAmbient: Color,
    val categoryBinaural: Color,
)

object MetiqColors {
    private val PinkFill = Color(0xFFFFC6F2)
    private val BrownFill = Color(0xFFA34E08)
    private val GreyFillDark = Color(0xFF9A9A9A)
    private val GreyFillLight = Color(0xFF6E6E6E)

    private val AmbientSeawaves = Color(0xFF3A7BD5)
    private val AmbientRain = Color(0xFF6C5CE7)
    private val AmbientFire = Color(0xFFE8662B)
    private val AmbientBirds = Color(0xFF4CAF7D)
    private val AmbientCafe = Color(0xFFB8862B)
    private val AmbientWind = Color(0xFF3AA6B9)
    private val AmbientCrickets = Color(0xFF8FAE3C)
    private val AmbientStream = Color(0xFF4FC3E8)

    private val CategoryNoise = Color(0xFFCAE7F3)
    private val CategoryAmbient = Color(0xFFDBF1B3)
    private val CategoryBinaural = Color(0xFFABAAFE)

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
        noisePink = PinkFill,
        noiseBrown = BrownFill,
        noiseWhite = Color.White,
        noiseGrey = GreyFillDark,
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
        ambientCrickets = AmbientCrickets,
        ambientStream = AmbientStream,
        categoryNoise = CategoryNoise,
        categoryAmbient = CategoryAmbient,
        categoryBinaural = CategoryBinaural,
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
        noisePink = PinkFill,
        noiseBrown = BrownFill,
        noiseWhite = Color.White,
        noiseGrey = GreyFillLight,
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
        ambientCrickets = AmbientCrickets,
        ambientStream = AmbientStream,
        categoryNoise = CategoryNoise,
        categoryAmbient = CategoryAmbient,
        categoryBinaural = CategoryBinaural,
    )
}
