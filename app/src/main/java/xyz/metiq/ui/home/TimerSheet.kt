package xyz.metiq.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

private val TIMER_BUTTON_SIZE: Dp = 52.dp

@Composable
internal fun TimerButton(
    enabled: Boolean,
    remainingSeconds: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val running = remainingSeconds != null
    val alpha by animateFloatAsState(
        targetValue = if (enabled || running) 1f else tokens.disabledAlpha,
        animationSpec = tween(ALPHA_ANIM_MS),
        label = "timerButtonAlpha",
    )

    Row(
        modifier = modifier
            .height(TIMER_BUTTON_SIZE)
            .widthIn(min = TIMER_BUTTON_SIZE)
            .alpha(alpha)
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.subtleFill.compositeOver(tokens.cellBackground))
            .clickable(enabled = enabled || running, onClick = onClick)
            .animateContentSize()
            .padding(horizontal = if (running) 16.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = stringResource(R.string.timer_label),
            tint = tokens.textPrimary,
            modifier = Modifier.size(24.dp),
        )
        if (remainingSeconds != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTimerClock(remainingSeconds),
                color = tokens.textPrimary,
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Tabular digits keep the pill from jittering as the seconds tick.
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

@Composable
internal fun TimerSheet(
    state: SleepTimerState,
    presetsSeconds: List<Long>,
    onDismiss: () -> Unit,
) {
    SoundSheet(
        title = stringResource(R.string.timer_label),
        subtitle = null,
        onDismiss = onDismiss,
        contentEndPadding = 20.dp,
        footer = {
            TimerStartStopButton(
                state = state,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        },
    ) {
        SleepTimer(state = state, presetsSeconds = presetsSeconds)
    }
}
