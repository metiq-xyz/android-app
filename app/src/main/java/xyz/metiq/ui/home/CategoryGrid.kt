package xyz.metiq.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

private val CATEGORY_ORB_SIZE: Dp = 112.dp
private val CATEGORY_CELL_WIDTH: Dp = 134.dp
private val CATEGORY_ICON_SIZE: Dp = 36.dp
private val CATEGORY_BORDER_WIDTH: Dp = 1.5.dp
private val CATEGORY_ACTIVE_RING_WIDTH: Dp = 2.dp
private val CATEGORY_ACTIVE_RING_GAP: Dp = 4.dp
private val STATUS_PILL_HEIGHT: Dp = 24.dp
private val STATUS_PILL_RING: Dp = 3.dp
private const val CATEGORY_ANIM_MS = 300

internal enum class SoundCategory(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    NOISE(R.string.category_noise, R.drawable.ic_category_noise),
    AMBIENT(R.string.category_ambient, R.drawable.ic_category_ambient),
    BINAURAL(R.string.category_binaural, R.drawable.ic_category_binaural),
}

internal fun SoundCategory.soundIds(): Set<String> = when (this) {
    SoundCategory.NOISE -> NOISE_IDS
    SoundCategory.AMBIENT -> AMBIENT_IDS
    SoundCategory.BINAURAL -> BINAURAL_BAND_IDS
}

@Composable
internal fun SoundCategory.accent(): Color {
    val tokens = LocalMetiqColors.current
    return when (this) {
        SoundCategory.NOISE -> tokens.categoryNoise
        SoundCategory.AMBIENT -> tokens.categoryAmbient
        SoundCategory.BINAURAL -> tokens.categoryBinaural
    }
}

@Composable
internal fun CategoryGrid(
    levels: Map<String, Float>,
    onSelect: (SoundCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SoundCategory.entries.chunked(2).forEach { row ->
            Row {
                row.forEach { category ->
                    CategoryOrb(
                        category = category,
                        activeCount = category.soundIds().count { it in levels },
                        onClick = { onSelect(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryOrb(
    category: SoundCategory,
    activeCount: Int,
    onClick: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val label = stringResource(category.labelRes)
    val active = activeCount > 0
    val accent = category.accent()
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(CATEGORY_ANIM_MS, easing = FastOutSlowInEasing),
        label = "categoryActive",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) tokens.accentShade else tokens.textPrimary,
        animationSpec = tween(CATEGORY_ANIM_MS),
        label = "categoryIconTint",
    )

    Column(
        modifier = Modifier.width(CATEGORY_CELL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(CATEGORY_ORB_SIZE)
                .height(CATEGORY_ORB_SIZE + STATUS_PILL_HEIGHT / 2),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(CATEGORY_ORB_SIZE)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        val border = CATEGORY_BORDER_WIDTH.toPx()
                        if (progress < 1f) {
                            drawCircle(
                                color = tokens.subtleFill,
                                radius = r - border / 2f,
                                alpha = 1f - progress,
                                style = Stroke(border),
                            )
                        }
                        if (progress > 0f) {
                            drawCircle(color = accent, radius = r, alpha = progress)
                            val ring = CATEGORY_ACTIVE_RING_WIDTH.toPx()
                            val to = r + CATEGORY_ACTIVE_RING_GAP.toPx() + ring / 2f
                            drawCircle(
                                color = accent,
                                radius = r + (to - r) * progress,
                                alpha = progress,
                                style = Stroke(ring),
                            )
                        }
                    }
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(category.iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(CATEGORY_ICON_SIZE),
                )
            }
            StatusPill(
                text = if (active) {
                    pluralStringResource(R.plurals.category_active_count, activeCount, activeCount)
                } else {
                    stringResource(R.string.ambient_off)
                },
                active = active,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -STATUS_PILL_RING),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = Inter, fontSize = 17.sp),
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val shape = RoundedCornerShape(100.dp)
    val fill by animateColorAsState(
        targetValue = if (active) accent else tokens.subtleFill.compositeOver(tokens.foreground),
        animationSpec = tween(CATEGORY_ANIM_MS),
        label = "pillFill",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) tokens.accentShade else tokens.textSecondary,
        animationSpec = tween(CATEGORY_ANIM_MS),
        label = "pillText",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.foreground)
            .padding(STATUS_PILL_RING),
    ) {
        Box(
            modifier = Modifier
                .height(STATUS_PILL_HEIGHT)
                .clip(shape)
                .background(fill)
                .animateContentSize(tween(CATEGORY_ANIM_MS))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = textColor,
                maxLines = 1,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
            )
        }
    }
}
