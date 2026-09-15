package xyz.metiq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import xyz.metiq.R
import xyz.metiq.ui.theme.Inter
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme

@Composable
fun RatePromptDialog(
    showFeedback: Boolean,
    message: String,
    rateLabel: String,
    onRate: () -> Unit,
    onFeedback: () -> Unit,
    onDonate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        RatePromptCard(
            showFeedback = showFeedback,
            message = message,
            rateLabel = rateLabel,
            onRate = {
                onRate()
                onDismiss()
            },
            onFeedback = {
                onFeedback()
                onDismiss()
            },
            onDonate = {
                onDonate()
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun RatePromptCard(
    showFeedback: Boolean,
    message: String,
    rateLabel: String,
    onRate: () -> Unit,
    onFeedback: () -> Unit,
    onDonate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(tokens.foreground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .background(tokens.subtleFill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = tokens.ratingStar,
                    modifier = Modifier.size(28.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.rate_prompt_dismiss_cd),
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.rate_prompt_title),
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 20.sp),
        )
        Spacer(Modifier.height(24.dp))
        DialogButton(
            label = stringResource(R.string.rate_prompt_donate_cta),
            background = tokens.textPrimary,
            foreground = tokens.foreground,
            onClick = onDonate,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogButton(
                label = rateLabel,
                background = tokens.subtleFill,
                foreground = tokens.textPrimary,
                onClick = onRate,
                modifier = Modifier.weight(1f),
            )
            if (showFeedback) {
                DialogButton(
                    label = stringResource(R.string.rate_prompt_feedback_cta),
                    background = tokens.subtleFill,
                    foreground = tokens.textPrimary,
                    onClick = onFeedback,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            maxLines = 1,
            style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Preview(name = "Rate prompt · Play", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun RatePromptPlayPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        RatePromptCard(
            showFeedback = false,
            message = "Rate on Play Store or donate to keep it free.",
            rateLabel = "Rate",
            onRate = {},
            onFeedback = {},
            onDonate = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Rate prompt · F-Droid", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun RatePromptFdroidPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        RatePromptCard(
            showFeedback = true,
            message = "Star us on GitHub, tell us what you miss, or donate.",
            rateLabel = "Star",
            onRate = {},
            onFeedback = {},
            onDonate = {},
            onDismiss = {},
        )
    }
}
