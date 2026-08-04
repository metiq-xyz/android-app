package xyz.metiq.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

@Composable
internal fun TimerFab(state: SleepTimerState, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = tokens.cellBackground,
        contentColor = tokens.textPrimary,
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (state.running) formatTimerClock(state.remainingSeconds)
            else stringResource(R.string.timer_label),
            style = if (state.running) {
                TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimerSheet(
    state: SleepTimerState,
    presetsSeconds: List<Long>,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.foreground,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CONTENT_HORIZONTAL_PADDING)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SleepTimer(
                state = state,
                presetsSeconds = presetsSeconds,
            )
        }
    }
}
