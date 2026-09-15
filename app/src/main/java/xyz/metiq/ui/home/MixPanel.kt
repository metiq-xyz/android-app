package xyz.metiq.ui.home

import android.icu.text.ListFormatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.launch
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

private val PANEL_PADDING: Dp = 16.dp
private val PLAY_BUTTON_SIZE: Dp = 42.dp
private val ACTION_HEIGHT: Dp = 36.dp
private val CHIP_HEIGHT: Dp = 32.dp
private const val MARQUEE_PAUSE_MS = 2000
private val WAVE_BAR_IDLE = floatArrayOf(0.5f, 1f, 0.5f)

@Composable
internal fun MixPanel(
    quickMixes: List<String>,
    mixTitle: String?,
    soundNames: List<String>,
    playing: Boolean,
    saveEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSaveMix: () -> Unit,
    onOpenMix: () -> Unit,
    onQuickMix: (Int) -> Unit,
    activeQuickMixes: Set<Int>,
    deletableQuickMixes: Int,
    onDeleteQuickMix: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val hasSounds = soundNames.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.cellBackground)
            .padding(top = PANEL_PADDING, bottom = PANEL_PADDING),
    ) {
        NowPlayingRow(
            mixTitle = mixTitle,
            soundNames = soundNames,
            playing = playing,
            onPlayPause = onPlayPause,
            modifier = Modifier.padding(horizontal = PANEL_PADDING),
        )
        Spacer(Modifier.height(PANEL_PADDING))
        Row(
            modifier = Modifier.padding(horizontal = PANEL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaveMixAction(
                enabled = saveEnabled,
                onClick = onSaveMix,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(ACTION_HEIGHT)
                    .alpha(if (hasSounds) 1f else tokens.disabledAlpha)
                    .clip(CircleShape)
                    .border(1.5.dp, tokens.subtleFill, CircleShape)
                    .clickable(enabled = hasSounds, onClick = onOpenMix),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = stringResource(R.string.mix_panel_open_cd),
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(
            color = tokens.divider,
            modifier = Modifier.padding(vertical = PANEL_PADDING),
        )
        Text(
            text = stringResource(R.string.mix_panel_quick_mixes),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
            modifier = Modifier.padding(horizontal = PANEL_PADDING),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = PANEL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(quickMixes.size) { index ->
                QuickMixChip(
                    label = quickMixes[index],
                    active = index in activeQuickMixes,
                    onClick = { onQuickMix(index) },
                    onDelete = if (index < deletableQuickMixes) {
                        { onDeleteQuickMix(index) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun NowPlayingRow(
    mixTitle: String?,
    soundNames: List<String>,
    playing: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val hasSounds = soundNames.isNotEmpty()
    val showPause = hasSounds && playing

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(PLAY_BUTTON_SIZE)
                .alpha(if (hasSounds) 1f else tokens.disabledAlpha)
                .clip(CircleShape)
                .background(tokens.logo)
                .clickable(enabled = hasSounds, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (showPause) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (showPause) R.string.mix_panel_pause_cd else R.string.mix_panel_play_cd
                ),
                tint = tokens.accentShade,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = when {
                    !hasSounds -> stringResource(R.string.mix_panel_empty_title)
                    else -> mixTitle ?: stringResource(R.string.mix_now_playing)
                },
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = Inter, fontSize = 18.sp),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaveBars(
                    animating = showPause,
                    color = tokens.logo,
                    modifier = Modifier.size(width = 12.dp, height = 14.dp),
                )
                Spacer(Modifier.width(8.dp))
                val locale = LocalConfiguration.current.locales[0]
                Text(
                    text = if (hasSounds) {
                        remember(soundNames, locale) { ListFormatter.getInstance(locale).format(soundNames) }
                    } else {
                        stringResource(R.string.mix_panel_empty_subtitle)
                    },
                    color = tokens.textSecondary,
                    maxLines = 1,
                    style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = MARQUEE_PAUSE_MS,
                            repeatDelayMillis = MARQUEE_PAUSE_MS,
                        ),
                )
            }
        }
    }
}

@Composable
private fun SaveMixAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val container by animateColorAsState(
        targetValue = if (enabled) tokens.textPrimary else tokens.subtleFill,
        animationSpec = tween(300),
        label = "saveMixContainer",
    )
    val content by animateColorAsState(
        targetValue = if (enabled) tokens.foreground else tokens.textPrimary,
        animationSpec = tween(300),
        label = "saveMixContent",
    )

    Row(
        modifier = modifier
            .height(ACTION_HEIGHT)
            .alpha(if (enabled) 1f else tokens.disabledAlpha)
            .clip(RoundedCornerShape(100.dp))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.mix_save_chip),
            color = content,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.BookmarkAdd,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun QuickMixChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val tokens = LocalMetiqColors.current
    val container by animateColorAsState(
        targetValue = if (active) tokens.textPrimary else tokens.subtleFill,
        animationSpec = tween(300),
        label = "quickMixContainer",
    )
    val content by animateColorAsState(
        targetValue = if (active) tokens.foreground else tokens.textPrimary,
        animationSpec = tween(300),
        label = "quickMixContent",
    )

    Row(
        modifier = Modifier
            .height(CHIP_HEIGHT)
            .clip(RoundedCornerShape(100.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = if (onDelete != null) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = content,
            maxLines = 1,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp),
        )
        if (onDelete != null) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.mix_delete_confirm),
                tint = content.copy(alpha = tokens.accentIconAlpha),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
                    .padding(1.dp),
            )
        }
    }
}

@Composable
private fun WaveBars(animating: Boolean, color: Color, modifier: Modifier = Modifier) {
    val bars = remember { WAVE_BAR_IDLE.map { Animatable(it) } }

    LaunchedEffect(animating) {
        bars.forEachIndexed { index, bar ->
            launch {
                if (!animating) {
                    bar.animateTo(WAVE_BAR_IDLE[index], tween(300, easing = FastOutSlowInEasing))
                    return@launch
                }
                val random = Random(index)
                while (true) {
                    bar.animateTo(
                        targetValue = 0.25f + random.nextFloat() * 0.75f,
                        animationSpec = tween(220 + random.nextInt(260), easing = FastOutSlowInEasing),
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (bars.size * 2 - 1)
        bars.forEachIndexed { index, bar ->
            val height = size.height * bar.value
            drawLine(
                color = color,
                start = Offset(barWidth * (index * 2 + 0.5f), (size.height - height) / 2f + barWidth / 2f),
                end = Offset(barWidth * (index * 2 + 0.5f), (size.height + height) / 2f - barWidth / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

