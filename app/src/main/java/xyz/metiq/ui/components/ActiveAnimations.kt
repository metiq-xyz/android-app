package xyz.metiq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.metiq.ui.theme.LocalMetiqColors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private const val WAVE_PERIOD_SEC = 1.2f
private const val WAVE_RING_COUNT = 3
private const val WAVE_STAGGER_SEC = WAVE_PERIOD_SEC / WAVE_RING_COUNT
private const val WAVE_REACH = 0.4f

@Composable
fun WaveRings(
    color: Color,
    active: Boolean,
    baseWidth: Dp,
    modifier: Modifier = Modifier,
    baseHeight: Dp = baseWidth,
    cornerRadius: Dp? = null,
) {
    val maxAlpha = LocalMetiqColors.current.waveMaxAlpha
    var clock by remember { mutableFloatStateOf(0f) }
    var emitStart by remember { mutableStateOf<Float?>(null) }
    var emitEnd by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(active) {
        if (active) {
            if (emitStart == null) {
                clock = 0f
                emitStart = 0f
            }
            emitEnd = null
        } else {
            if (emitStart == null) return@LaunchedEffect
            emitEnd = clock
        }
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) clock += (now - last) / 1e9f
                last = now
            }
            val end = emitEnd
            if (end != null && clock - end >= WAVE_PERIOD_SEC) {
                emitStart = null
                emitEnd = null
                break
            }
        }
    }

    Canvas(modifier = modifier) {
        val start = emitStart ?: return@Canvas
        val emitUntil = emitEnd ?: clock
        val center = Offset(size.width / 2f, size.height / 2f)
        val bw = baseWidth.toPx()
        val bh = baseHeight.toPx()
        val corner = cornerRadius?.toPx()
        val firstK = maxOf(0, ceil((clock - WAVE_PERIOD_SEC - start) / WAVE_STAGGER_SEC).toInt())
        val lastK = floor((emitUntil - start) / WAVE_STAGGER_SEC).toInt()
        for (k in firstK..lastK) {
            val age = clock - (start + k * WAVE_STAGGER_SEC)
            if (age < 0f || age >= WAVE_PERIOD_SEC) continue
            val p = age / WAVE_PERIOD_SEC
            val scale = 1f + p * WAVE_REACH
            val ringColor = color.copy(alpha = (1f - p) * maxAlpha)
            if (corner == null) {
                drawCircle(color = ringColor, radius = (bw / 2f) * scale, center = center)
            } else {
                val w = bw * scale
                val h = bh * scale
                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(corner * scale, corner * scale),
                )
            }
        }
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    val radiusDp: Float,
    val alpha: Float,
    val speed: Float,
)

private fun newParticle(rng: Random, baseAlpha: Float, alphaJitter: Float): Particle = Particle(
    x = rng.nextFloat(),
    y = rng.nextFloat(),
    radiusDp = 1.2f + rng.nextFloat() * 1.2f,
    alpha = baseAlpha + rng.nextFloat() * alphaJitter,
    speed = 0.012f + rng.nextFloat() * 0.018f,
)

private fun flowAngle(x: Float, y: Float, t: Float): Float {
    val a = sin(x * 1.7f + t * 0.14f) + cos(y * 1.3f - t * 0.09f)
    val b = cos(x * 1.0f - t * 0.07f) + sin(y * 2.1f + t * 0.16f)
    return (a + b) * PI.toFloat() * 0.5f
}

private fun Particle.update(dt: Float, t: Float) {
    val angle = flowAngle(x, y, t)
    x = ((x + cos(angle) * speed * dt) % 1f + 1f) % 1f
    y = ((y + sin(angle) * speed * dt) % 1f + 1f) % 1f
}

@Composable
fun ParticleField(
    color: Color,
    modifier: Modifier = Modifier,
    count: Int = 90,
    seed: Long = 0xC0FFEE,
    intensity: Float = 1f,
) {
    val tokens = LocalMetiqColors.current
    val rng = remember(seed) { Random(seed) }
    val particles = remember(count, seed) {
        List(count) { newParticle(rng, tokens.particleBaseAlpha, tokens.particleAlphaJitter) }
    }
    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        var elapsed = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                elapsed += dt
                particles.forEach { it.update(dt, elapsed) }
                frameTime = now
            }
        }
    }
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION") frameTime
        particles.forEach { p ->
            drawCircle(
                color = color.copy(alpha = p.alpha * intensity),
                radius = p.radiusDp.dp.toPx(),
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
    }
}
