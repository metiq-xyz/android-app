package xyz.metiq.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import kotlin.time.Duration.Companion.seconds

internal const val ALPHA_ANIM_MS = 300

internal enum class TimerField {
    HOURS, MINUTES, SECONDS
}

private fun formatDecimal(n: Int): String = n.toString().padStart(2, '0')
private fun hoursFor(seconds: Long): Int = (seconds / 3600L).toInt()
private fun minutesFor(seconds: Long): Int = ((seconds / 60L) % 60L).toInt()
private fun secondsFor(seconds: Long): Int = (seconds % 60L).toInt()

fun formatTimerClock(seconds: Long): String {
    val h = hoursFor(seconds)
    val m = minutesFor(seconds)
    val s = secondsFor(seconds)
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// Held by the screen so playback changes can call reset(); the countdown itself is
// driven by rememberSleepTimerState below.
@Stable
class SleepTimerState {
    var remainingSeconds by mutableLongStateOf(0L)
        private set
    var running by mutableStateOf(false)
        private set
    internal var editField by mutableStateOf<TimerField?>(null)
        private set
    internal var editBuffer by mutableStateOf("")
        private set

    fun reset() {
        remainingSeconds = 0L
        running = false
        editField = null
        editBuffer = ""
    }

    internal fun toggleRunning() {
        if (running) {
            running = false
            remainingSeconds = 0L
        } else if (remainingSeconds > 0L) {
            running = true
        }
    }

    internal fun selectPreset(seconds: Long) {
        remainingSeconds = seconds
        running = true
        editField = null
        editBuffer = ""
    }

    internal fun beginEdit(field: TimerField) {
        // Commit any field already being edited before switching to the new one.
        editField?.let { commitField(it) }
        editBuffer = ""
        editField = field
    }

    internal fun onBufferChange(value: String) {
        editBuffer = value
    }

    internal fun commitField(field: TimerField) {
        if (editField == field && editBuffer.isNotBlank()) {
            val parsed = editBuffer.toIntOrNull() ?: 0
            val maxVal = if (field == TimerField.HOURS) 99 else 59
            val clamped = parsed.coerceIn(0, maxVal)
            val h = if (field == TimerField.HOURS) clamped else hoursFor(remainingSeconds)
            val m = if (field == TimerField.MINUTES) clamped else minutesFor(remainingSeconds)
            val s = if (field == TimerField.SECONDS) clamped else secondsFor(remainingSeconds)
            remainingSeconds = h * 3600L + m * 60L + s
        }
        if (editField == field) {
            editField = null
            editBuffer = ""
        }
    }

    internal fun tick() {
        if (remainingSeconds > 0L) remainingSeconds -= 1L
    }
}

// onFinished fires once when a running timer reaches zero (the screen stops playback
// there), after which the timer resets.
@Composable
fun rememberSleepTimerState(onFinished: () -> Unit): SleepTimerState {
    val state = remember { SleepTimerState() }
    val latestOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(state.running) {
        if (!state.running) return@LaunchedEffect
        while (state.running && state.remainingSeconds > 0L) {
            delay(1.seconds)
            if (state.running) state.tick()
        }
        if (state.running && state.remainingSeconds == 0L) {
            latestOnFinished()
            state.reset()
        }
    }
    return state
}

@Composable
fun SleepTimer(
    state: SleepTimerState,
    presetsSeconds: List<Long>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presetsSeconds.forEach { seconds ->
                PresetChip(
                    modifier = Modifier.weight(1f, fill = false),
                    label = presetLabel(seconds),
                    enabled = !state.running,
                    onClick = {
                        if (!state.running) state.selectPreset(seconds)
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerCell(
                label = stringResource(R.string.timer_hours),
                liveValue = hoursFor(state.remainingSeconds),
                isEditing = state.editField == TimerField.HOURS,
                editBuffer = state.editBuffer,
                onBeginEdit = { if (!state.running) state.beginEdit(TimerField.HOURS) },
                onBufferChange = state::onBufferChange,
                onCommit = { state.commitField(TimerField.HOURS) },
                enabled = !state.running,
            )
            TimerCell(
                label = stringResource(R.string.timer_minutes),
                liveValue = minutesFor(state.remainingSeconds),
                isEditing = state.editField == TimerField.MINUTES,
                editBuffer = state.editBuffer,
                onBeginEdit = { if (!state.running) state.beginEdit(TimerField.MINUTES) },
                onBufferChange = state::onBufferChange,
                onCommit = { state.commitField(TimerField.MINUTES) },
                enabled = !state.running,
            )
            TimerCell(
                label = stringResource(R.string.timer_seconds),
                liveValue = secondsFor(state.remainingSeconds),
                isEditing = state.editField == TimerField.SECONDS,
                editBuffer = state.editBuffer,
                onBeginEdit = { if (!state.running) state.beginEdit(TimerField.SECONDS) },
                onBufferChange = state::onBufferChange,
                onCommit = { state.commitField(TimerField.SECONDS) },
                enabled = !state.running,
            )
        }
        Spacer(Modifier.height(16.dp))
        StartStopButton(
            running = state.running,
            enabled = state.running || state.remainingSeconds > 0L,
            onClick = state::toggleRunning,
        )
    }
}

@Composable
private fun presetLabel(seconds: Long): String {
    val totalMinutes = (seconds / 60L).toInt()
    val hours = totalMinutes / 60
    val remainder = totalMinutes % 60
    return when {
        hours > 0 && remainder == 0 -> stringResource(R.string.timer_preset_hours, hours)
        hours > 0 -> stringResource(R.string.timer_preset_hours_minutes, hours, remainder)
        else -> stringResource(R.string.timer_preset_minutes, totalMinutes)
    }
}

@Composable
private fun StartStopButton(running: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    val buttonAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else tokens.disabledAlpha,
        animationSpec = tween(durationMillis = ALPHA_ANIM_MS),
        label = "startStopAlpha",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .alpha(buttonAlpha)
            .background(tokens.textPrimary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(if (running) R.string.timer_stop else R.string.timer_start),
            color = tokens.foreground,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun TimerCell(
    modifier: Modifier = Modifier,
    label: String,
    liveValue: Int,
    isEditing: Boolean,
    editBuffer: String,
    onBeginEdit: () -> Unit,
    onBufferChange: (String) -> Unit,
    onCommit: () -> Unit,
    enabled: Boolean,
) {
    val tokens = LocalMetiqColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                // Cap must precede fillMaxWidth: size modifiers respect incoming
                // constraints, so the reverse order silently drops the cap.
                .widthIn(max = 96.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.cellBackground)
                .clickable(enabled = enabled && !isEditing) { onBeginEdit() },
            contentAlignment = Alignment.Center,
        ) {
            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                var hadFocus by remember { mutableStateOf(false) }
                BasicTextField(
                    value = editBuffer,
                    onValueChange = { txt ->
                        if (txt.length <= 2 && txt.all { it.isDigit() }) onBufferChange(txt)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Inter,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(tokens.textPrimary),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) hadFocus = true
                            else if (hadFocus) onCommit()
                        },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            } else {
                Text(
                    text = formatDecimal(liveValue),
                    color = tokens.textPrimary,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
        )
    }
}

@Composable
private fun PresetChip(modifier: Modifier = Modifier, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tokens = LocalMetiqColors.current
    val chipAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else tokens.disabledAlpha,
        animationSpec = tween(durationMillis = ALPHA_ANIM_MS),
        label = "presetChipAlpha",
    )
    Box(
        modifier = modifier
            .alpha(chipAlpha)
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.cellBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
