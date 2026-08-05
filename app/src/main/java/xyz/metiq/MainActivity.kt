package xyz.metiq

import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import xyz.metiq.audio.PcmStore
import xyz.metiq.ui.home.HomeScreen
import xyz.metiq.ui.theme.MetiqTheme

private const val SPLASH_MAX_HOLD_MS = 2000L

class MainActivity : AppCompatActivity() {
    private var systemConfig by mutableStateOf<Configuration?>(null)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemConfig = Configuration(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashShownAt = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !PcmStore.noiseReady &&
                SystemClock.uptimeMillis() - splashShownAt < SPLASH_MAX_HOLD_MS
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as MetiqApp
        setContent {
            val settings by app.settings.flow.collectAsState(initial = DEFAULT_SETTINGS)
            val ratePromptVisible by app.settings.ratePromptVisible.collectAsState(initial = false)
            val scope = rememberCoroutineScope()
            val repo = app.settings
            val darkTheme = when (settings.themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            LaunchedEffect(darkTheme) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val baseConfig = systemConfig
            val localizedContext = remember(settings.languageTag, baseConfig) {
                val config = Configuration(baseConfig ?: resources.configuration)
                if (settings.languageTag != null) {
                    config.setLocales(LocaleList.forLanguageTags(settings.languageTag))
                }
                createConfigurationContext(config)
            }
            val localizedConfig = localizedContext.resources.configuration
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides
                    if (localizedConfig.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    },
            ) {
            MetiqTheme(darkTheme = darkTheme) {
                HomeScreen(
                    settings = settings,
                    onParticlesEnabled = { scope.launch { repo.setParticlesEnabled(it) } },
                    onWavesEnabled = { scope.launch { repo.setWavesEnabled(it) } },
                    onWarmth = { scope.launch { repo.setWarmth(it) } },
                    onFadeSeconds = { scope.launch { repo.setFadeSeconds(it) } },
                    onTimerFadeSeconds = { scope.launch { repo.setTimerFadeSeconds(it) } },
                    onRequestAudioFocus = { scope.launch { repo.setRequestAudioFocus(it) } },
                    onThemePreference = { scope.launch { repo.setThemePreference(it) } },
                    onBinauralVolume = { scope.launch { repo.setBinauralVolume(it) } },
                    onBinauralBand = { scope.launch { repo.setBinauralBand(it) } },
                    onTimerPresets = { scope.launch { repo.setTimerPresetsSeconds(it) } },
                    onCustomMixes = { scope.launch { repo.setCustomMixes(it) } },
                    onLanguageTag = { scope.launch { repo.setLanguageTag(it) } },
                    ratePromptVisible = ratePromptVisible,
                    onRatePromptDismiss = { scope.launch { repo.snoozeRatePrompt() } },
                )
            }
            }
        }
    }
}
