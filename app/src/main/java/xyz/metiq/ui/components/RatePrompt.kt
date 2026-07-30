package xyz.metiq.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatePromptBanner(
    showFeedback: Boolean,
    message: String,
    rateLabel: String,
    onRate: () -> Unit,
    onFeedback: () -> Unit,
    onDonate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMetiqColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it != SwipeToDismissBoxValue.Settled },
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) onDismiss()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.cellBackground)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = tokens.ratingStar,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.rate_prompt_title),
                        color = tokens.textPrimary,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        color = tokens.textSecondary,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.rate_prompt_dismiss_cd),
                        tint = tokens.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionPill(
                    label = rateLabel,
                    background = tokens.textPrimary,
                    foreground = tokens.background,
                    onClick = onRate,
                )
                if (showFeedback) {
                    ActionPill(
                        label = stringResource(R.string.rate_prompt_feedback_cta),
                        background = tokens.subtleFill,
                        foreground = tokens.textPrimary,
                        onClick = onFeedback,
                    )
                }
                ActionPill(
                    label = stringResource(R.string.rate_prompt_donate_cta),
                    background = tokens.subtleFill,
                    foreground = tokens.textPrimary,
                    onClick = onDonate,
                )
            }
        }
    }
}

@Preview(name = "Rate prompt · Play", showBackground = true, backgroundColor = 0xFF222121)
@Composable
private fun RatePromptBannerPlayPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        RatePromptBanner(
            showFeedback = false,
            message = "Rate on Play Store or donate to keep it free.",
            rateLabel = "Rate",
            onRate = {},
            onFeedback = {},
            onDonate = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Rate prompt · F-Droid", showBackground = true, backgroundColor = 0xFF222121)
@Composable
private fun RatePromptBannerFdroidPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        RatePromptBanner(
            showFeedback = true,
            message = "Star us on GitHub, tell us what you miss, or donate.",
            rateLabel = "Star",
            onRate = {},
            onFeedback = {},
            onDonate = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = foreground,
            style = TextStyle(
                fontFamily = Inter,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
