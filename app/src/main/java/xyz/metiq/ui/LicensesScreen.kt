package xyz.metiq.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.metiq.R
import xyz.metiq.ui.theme.LocalMetiqColors
import xyz.metiq.ui.theme.MetiqTheme
import xyz.metiq.ui.theme.Inter
import androidx.compose.ui.tooling.preview.Preview

private data class LicenseEntry(val titleRes: Int, val bodyRes: Int)

private val LICENSES = listOf(
    LicenseEntry(R.string.licenses_inter_title, R.string.licenses_inter_body),
    LicenseEntry(R.string.licenses_apache2_title, R.string.licenses_apache2_body),
    LicenseEntry(R.string.licenses_kotlin_title, R.string.licenses_kotlin_body),
    LicenseEntry(R.string.licenses_sounds_title, R.string.licenses_sounds_body),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val tokens = LocalMetiqColors.current
    Scaffold(
        containerColor = tokens.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.licenses_title),
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_cd),
                            tint = tokens.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.licenses_intro),
                color = tokens.textSecondary,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 18.sp),
            )
            Spacer(Modifier.height(4.dp))
            LICENSES.forEach { entry ->
                LicenseCard(entry)
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: LicenseEntry) {
    val tokens = LocalMetiqColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.foreground)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(entry.titleRes),
            color = tokens.textPrimary,
            style = TextStyle(
                fontFamily = Inter,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(entry.bodyRes),
            color = tokens.textSecondary,
            style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 18.sp),
        )
    }
}

@Preview(name = "Licenses — dark", showBackground = true, backgroundColor = 0xFF111010)
@Composable
private fun LicensesScreenDarkPreview() {
    MetiqTheme(darkTheme = isSystemInDarkTheme()) {
        LicensesScreen(onBack = {})
    }
}
