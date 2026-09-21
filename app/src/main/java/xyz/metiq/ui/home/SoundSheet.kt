package xyz.metiq.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors

private val SHEET_HEADER_PADDING: Dp = 20.dp
private val SHEET_ROW_START_PADDING: Dp = 20.dp
private val SHEET_ROW_END_PADDING: Dp = 24.dp
private val SHEET_DIVIDER_TOP_PADDING: Dp = 24.dp
private val SHEET_LIST_TOP_PADDING: Dp = 20.dp
private val SHEET_ROW_SPACING: Dp = 12.dp
private val ORB_BOX_SIZE: Dp = 60.dp
private const val SHEET_MAX_VISIBLE_ROWS = 5.5f
private val SHEET_BODY_MAX_HEIGHT: Dp = SHEET_DIVIDER_TOP_PADDING + DividerDefaults.Thickness +
    SHEET_LIST_TOP_PADDING + (ORB_BOX_SIZE + SHEET_ROW_SPACING) * SHEET_MAX_VISIBLE_ROWS
private val ORB_SIZE: Dp = 54.dp
private val BADGE_SIZE: Dp = 24.dp
private val BADGE_RING: Dp = 2.dp
private val SLIDER_TRACK_HEIGHT: Dp = 8.dp
private val SLIDER_THUMB_SIZE: Dp = 20.dp
private val SLIDER_THUMB_BORDER: Dp = 2.dp
private val ORB_RING_WIDTH: Dp = 1.5.dp
private val ORB_BORDER_WIDTH: Dp = 1.dp
private const val ACTIVATE_ANIM_MS = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSheet(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    notice: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    contentEndPadding: Dp = SHEET_ROW_END_PADDING,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalMetiqColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.foreground,
        scrimColor = tokens.scrim.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = tokens.textPrimary.copy(alpha = 0.8f)) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SHEET_HEADER_PADDING, end = SHEET_HEADER_PADDING, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = tokens.textPrimary,
                    style = TextStyle(fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = tokens.textSecondary,
                        style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, tokens.subtleFill, RoundedCornerShape(12.dp))
                    .clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.sheet_close_cd),
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = SHEET_BODY_MAX_HEIGHT),
        ) {
            if (notice != null) {
                Box(
                    modifier = Modifier
                        .padding(start = SHEET_ROW_START_PADDING, end = SHEET_ROW_START_PADDING, top = 24.dp),
                ) {
                    notice()
                }
            }
            HorizontalDivider(
                color = tokens.divider,
                modifier = Modifier.padding(top = SHEET_DIVIDER_TOP_PADDING),
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = SHEET_ROW_START_PADDING,
                        end = contentEndPadding,
                        top = SHEET_LIST_TOP_PADDING,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(SHEET_ROW_SPACING),
                content = content,
            )
            footer?.invoke()
        }
    }
}

@Composable
internal fun SheetNotice(
    icon: ImageVector,
    text: String,
) {
    val tokens = LocalMetiqColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.cellBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 18.sp),
        )
    }
}

@Composable
internal fun SoundLevelRow(
    label: String,
    orbColor: Color,
    trackColor: Color,
    level: Float?,
    onToggle: () -> Unit,
    onLevel: (Float) -> Unit,
    onLevelSettled: () -> Unit,
    detail: String? = null,
    dimmed: Boolean = false,
    showBadge: Boolean = true,
    orbBorder: Color? = null,
    icon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val tokens = LocalMetiqColors.current
    val active = level != null
    val rowAlpha by animateFloatAsState(
        targetValue = if (dimmed) tokens.disabledAlpha else 1f,
        animationSpec = tween(ACTIVATE_ANIM_MS),
        label = "soundRowAlpha",
    )
    val ringProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(ACTIVATE_ANIM_MS, easing = FastOutSlowInEasing),
        label = "soundRing",
    )
    val valueColor by animateColorAsState(
        targetValue = if (active) orbColor else tokens.textSecondary,
        animationSpec = tween(ACTIVATE_ANIM_MS),
        label = "soundValueColor",
    )

    Row(
        modifier = Modifier.graphicsLayer { alpha = rowAlpha },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ORB_BOX_SIZE)
                .drawBehind {
                    if (ringProgress <= 0f) return@drawBehind
                    val stroke = ORB_RING_WIDTH.toPx()
                    val from = ORB_SIZE.toPx() / 2f - stroke / 2f
                    val to = size.minDimension / 2f - stroke / 2f
                    drawCircle(
                        color = orbColor,
                        radius = from + (to - from) * ringProgress,
                        alpha = ringProgress,
                        style = Stroke(stroke),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(ORB_SIZE)
                    .clip(CircleShape)
                    .background(orbColor)
                    .then(
                        if (orbBorder != null) {
                            Modifier.border(ORB_BORDER_WIDTH, orbBorder, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .toggleable(
                        value = active,
                        enabled = active || !dimmed,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    )
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                icon?.invoke(lerp(orbColor, tokens.accentShade, 0.55f))
            }
            SoundBadge(
                active = active,
                visible = showBadge && (active || !dimmed),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = (-4).dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = label,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = Inter, fontSize = 17.sp),
                    modifier = Modifier.alignByBaseline(),
                )
                if (detail != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = detail,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelSlider(
                    label = label,
                    level = level,
                    fillColor = trackColor,
                    thumbColor = orbColor,
                    onLevel = onLevel,
                    onLevelSettled = onLevelSettled,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (level != null) {
                        stringResource(R.string.sound_level_percent, (level * 100).roundToInt())
                    } else {
                        stringResource(R.string.ambient_off)
                    },
                    color = valueColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    style = TextStyle(fontFamily = Inter, fontSize = 17.sp),
                    modifier = Modifier.widthIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun SoundBadge(active: Boolean, visible: Boolean, modifier: Modifier = Modifier) {
    val tokens = LocalMetiqColors.current
    val checkScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "badgeCheckScale",
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(ACTIVATE_ANIM_MS, easing = FastOutSlowInEasing),
        label = "badgeScale",
    )

    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .graphicsLayer {
                scaleX = badgeScale
                scaleY = badgeScale
            }
            .background(tokens.foreground, CircleShape)
            .padding(BADGE_RING),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(tokens.cellBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val scale = checkScale.coerceAtLeast(0f)
                    scaleX = scale
                    scaleY = scale
                }
                .background(tokens.logo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = tokens.accentShade,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun LevelSlider(
    label: String,
    level: Float?,
    fillColor: Color,
    thumbColor: Color,
    onLevel: (Float) -> Unit,
    onLevelSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val active = level != null
    val fillAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(ACTIVATE_ANIM_MS),
        label = "sliderFillAlpha",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sliderThumbScale",
    )
    val lastLevel = remember { mutableFloatStateOf(level ?: 0f) }

    SideEffect { if (level != null) lastLevel.floatValue = level }

    Canvas(
        modifier = modifier
            .height(SLIDER_THUMB_SIZE)
            .semantics {
                contentDescription = label
                if (level != null) {
                    progressBarRangeInfo = ProgressBarRangeInfo(level, 0f..1f)
                    setProgress { target ->
                        onLevel(target.coerceIn(0f, 1f))
                        onLevelSettled()
                        true
                    }
                }
            }
            .pointerInput(active, rtl) {
                if (!active) return@pointerInput
                detectTapGestures { offset ->
                    onLevel(levelAt(offset.x, size.width.toFloat(), SLIDER_THUMB_SIZE.toPx() / 2f, rtl))
                    onLevelSettled()
                }
            }
            .pointerInput(active, rtl) {
                if (!active) return@pointerInput
                detectHorizontalDragGestures(onDragEnd = onLevelSettled) { change, _ ->
                    change.consume()
                    onLevel(levelAt(change.position.x, size.width.toFloat(), SLIDER_THUMB_SIZE.toPx() / 2f, rtl))
                }
            },
    ) {
        val trackH = SLIDER_TRACK_HEIGHT.toPx()
        val cy = size.height / 2f
        val corner = CornerRadius(trackH / 2f, trackH / 2f)
        drawRoundRect(
            color = tokens.subtleFill,
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size(size.width, trackH),
            cornerRadius = corner,
        )
        if (fillAlpha <= 0f && thumbScale <= 0f) return@Canvas
        val shown = level ?: lastLevel.floatValue
        val r = SLIDER_THUMB_SIZE.toPx() / 2f
        val ltrX = r + shown * (size.width - 2f * r)
        val cx = if (rtl) size.width - ltrX else ltrX
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(if (rtl) cx else 0f, cy - trackH / 2f),
            size = Size(if (rtl) size.width - cx else cx, trackH),
            cornerRadius = corner,
            alpha = fillAlpha,
        )
        val thumbRadius = (r - SLIDER_THUMB_BORDER.toPx()) * thumbScale.coerceAtLeast(0f)
        if (thumbRadius > 0f) {
            drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(cx, cy))
        }
    }
}

private fun levelAt(x: Float, width: Float, thumbRadius: Float, rtl: Boolean): Float {
    val ltr = ((x - thumbRadius) / (width - 2f * thumbRadius)).coerceIn(0f, 1f)
    return if (rtl) 1f - ltr else ltr
}
