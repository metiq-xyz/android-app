package xyz.metiq

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class CustomMix(
    val name: String,
    val layers: Map<String, Float>,
)

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class Settings(
    val particlesEnabled: Boolean,
    val wavesEnabled: Boolean,
    val timerPresetsSeconds: List<Long>,
    val languageTag: String?,
    val customMixes: List<CustomMix>,
    val warmth: Float,
    val fadeSeconds: Float,
    val timerFadeSeconds: Float,
    val requestAudioFocus: Boolean,
    val themePreference: ThemePreference,
    val binauralVolume: Float,
    val binauralBand: String?,
)

val DEFAULT_SETTINGS = Settings(
    particlesEnabled = true,
    wavesEnabled = true,
    timerPresetsSeconds = listOf(
        15L * 60, 30L * 60, 45L * 60, 60L * 60
    ),
    languageTag = null,
    customMixes = emptyList(),
    warmth = 0f,
    fadeSeconds = 0.5f,
    timerFadeSeconds = 2f,
    requestAudioFocus = false,
    themePreference = ThemePreference.SYSTEM,
    binauralVolume = 0.5f,
    binauralBand = null,
)

const val MAX_TIMER_PRESETS = 4
const val MAX_CUSTOM_MIXES = 6
const val MAX_CUSTOM_MIX_NAME_LENGTH = 24
const val MAX_FADE_SECONDS = 30f

fun clampFadeSeconds(value: Float): Float {
    val rounded = Math.round(value.coerceIn(0f, MAX_FADE_SECONDS) * 10f) / 10f
    return rounded.coerceIn(0f, MAX_FADE_SECONDS)
}

// One mix per line as "name|id:volume,id:volume". The name is sanitized of the
// separator characters on save, so a plain split round-trips safely.
private fun encodeCustomMixes(mixes: List<CustomMix>): String =
    mixes.take(MAX_CUSTOM_MIXES).joinToString("\n") { mix ->
        val name = mix.name.replace('\n', ' ').replace('|', ' ')
            .trim().take(MAX_CUSTOM_MIX_NAME_LENGTH)
        val layers = mix.layers.entries.joinToString(",") { (id, vol) ->
            "$id:${vol.coerceIn(0f, 1f)}"
        }
        "$name|$layers"
    }

private val LEGACY_LAYER_IDS = mapOf("thunderstorm" to "rain")

private fun decodeCustomMixes(encoded: String): List<CustomMix> =
    encoded.lineSequence().mapNotNull { line ->
        val sep = line.indexOf('|')
        if (sep <= 0) return@mapNotNull null
        val name = line.substring(0, sep).trim()
        val layers = line.substring(sep + 1).split(',').mapNotNull { entry ->
            val id = entry.substringBefore(':').takeIf { it.isNotBlank() }
                ?.let { LEGACY_LAYER_IDS[it] ?: it }
            val vol = entry.substringAfter(':', "").toFloatOrNull()
            if (id != null && vol != null && vol > 0f) id to vol.coerceIn(0f, 1f) else null
        }.toMap()
        if (name.isNotEmpty() && layers.isNotEmpty()) CustomMix(name, layers) else null
    }.take(MAX_CUSTOM_MIXES).toList()

val SUPPORTED_LANGUAGE_TAGS: List<String> = listOf("en", "it", "es", "fr", "pt")

// Rate-prompt policy: only nudge once the user has clearly stuck around, and
// re-nudge sparingly after a swipe-away so it never feels nagging.
private const val RATE_MIN_LAUNCHES = 4
private const val RATE_MIN_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000 // 3 days since first launch
private const val RATE_REPROMPT_MILLIS = 21L * 24 * 60 * 60 * 1000 // 3 weeks after a dismissal

private val Context.dataStore by preferencesDataStore(name = "metiq_settings")

private object Keys {
    val PARTICLES_ENABLED = booleanPreferencesKey("particles_enabled")
    val WAVES_ENABLED = booleanPreferencesKey("waves_enabled")
    val WARMTH = floatPreferencesKey("warmth")
    val FADE_SECONDS = floatPreferencesKey("fade_seconds")
    val TIMER_FADE_SECONDS = floatPreferencesKey("timer_fade_seconds")
    val REQUEST_AUDIO_FOCUS = booleanPreferencesKey("request_audio_focus")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    val BINAURAL_VOLUME = floatPreferencesKey("binaural_volume")
    val BINAURAL_BAND = stringPreferencesKey("binaural_band")
    val TIMER_PRESETS = stringPreferencesKey("timer_presets")
    val CUSTOM_MIXES = stringPreferencesKey("custom_mixes")
    val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    val RATE_FIRST_LAUNCH = longPreferencesKey("rate_first_launch_millis")
    val RATE_LAUNCH_COUNT = intPreferencesKey("rate_launch_count")
    val RATE_LAST_PROMPT = longPreferencesKey("rate_last_prompt_millis")
}

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val flow: Flow<Settings> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs.toSettings() }

    suspend fun setParticlesEnabled(enabled: Boolean) {
        store.edit { it[Keys.PARTICLES_ENABLED] = enabled }
    }

    suspend fun setWavesEnabled(enabled: Boolean) {
        store.edit { it[Keys.WAVES_ENABLED] = enabled }
    }

    suspend fun setWarmth(warmth: Float) {
        store.edit { it[Keys.WARMTH] = warmth.coerceIn(0f, 1f) }
    }

    suspend fun setFadeSeconds(seconds: Float) {
        store.edit { it[Keys.FADE_SECONDS] = clampFadeSeconds(seconds) }
    }

    suspend fun setTimerFadeSeconds(seconds: Float) {
        store.edit { it[Keys.TIMER_FADE_SECONDS] = clampFadeSeconds(seconds) }
    }

    suspend fun setRequestAudioFocus(enabled: Boolean) {
        store.edit { it[Keys.REQUEST_AUDIO_FOCUS] = enabled }
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        store.edit { it[Keys.THEME_PREFERENCE] = preference.name }
    }

    suspend fun setBinauralVolume(volume: Float) {
        store.edit { it[Keys.BINAURAL_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setBinauralBand(band: String?) {
        store.edit {
            if (band == null) it.remove(Keys.BINAURAL_BAND)
            else it[Keys.BINAURAL_BAND] = band
        }
    }

    suspend fun setTimerPresetsSeconds(presets: List<Long>) {
        val capped = presets.take(MAX_TIMER_PRESETS).filter { it > 0L }
        store.edit { it[Keys.TIMER_PRESETS] = capped.joinToString(",") }
    }

    suspend fun setCustomMixes(mixes: List<CustomMix>) {
        store.edit { it[Keys.CUSTOM_MIXES] = encodeCustomMixes(mixes) }
    }

    suspend fun setLanguageTag(tag: String?) {
        store.edit {
            if (tag == null) it.remove(Keys.LANGUAGE_TAG)
            else it[Keys.LANGUAGE_TAG] = tag
        }
        applyLanguageTag(tag)
    }

    // Recomputed on every emission (each launch increment) against the wall clock.
    val ratePromptVisible: Flow<Boolean> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs ->
        val now = System.currentTimeMillis()
        val firstLaunch = prefs[Keys.RATE_FIRST_LAUNCH] ?: now
        val launchCount = prefs[Keys.RATE_LAUNCH_COUNT] ?: 0
        val lastPrompt = prefs[Keys.RATE_LAST_PROMPT] ?: 0L
        val minLaunches = if (BuildConfig.DEBUG) 0 else RATE_MIN_LAUNCHES
        val minAgeMillis = if (BuildConfig.DEBUG) 0L else RATE_MIN_AGE_MILLIS
        launchCount >= minLaunches &&
            now - firstLaunch >= minAgeMillis &&
            (lastPrompt == 0L || now - lastPrompt >= RATE_REPROMPT_MILLIS)
    }

    suspend fun registerLaunch() {
        val now = System.currentTimeMillis()
        store.edit { prefs ->
            if (prefs[Keys.RATE_FIRST_LAUNCH] == null) prefs[Keys.RATE_FIRST_LAUNCH] = now
            prefs[Keys.RATE_LAUNCH_COUNT] = (prefs[Keys.RATE_LAUNCH_COUNT] ?: 0) + 1
        }
    }

    // Dismissing or acting on the note snoozes it until the reprompt interval, so it
    // always resurfaces later rather than being permanently opted out.
    suspend fun snoozeRatePrompt() {
        store.edit { it[Keys.RATE_LAST_PROMPT] = System.currentTimeMillis() }
    }

    private fun Preferences.toSettings(): Settings {
        val particles = this[Keys.PARTICLES_ENABLED] ?: DEFAULT_SETTINGS.particlesEnabled
        val waves = this[Keys.WAVES_ENABLED] ?: DEFAULT_SETTINGS.wavesEnabled
        val warmth = (this[Keys.WARMTH] ?: DEFAULT_SETTINGS.warmth).coerceIn(0f, 1f)
        val presets = this[Keys.TIMER_PRESETS]?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?.filter { it > 0L }?.take(MAX_TIMER_PRESETS)?.ifEmpty { null }
            ?: DEFAULT_SETTINGS.timerPresetsSeconds
        val languageTag = this[Keys.LANGUAGE_TAG]?.takeIf { it in SUPPORTED_LANGUAGE_TAGS }
        val customMixes = this[Keys.CUSTOM_MIXES]?.let(::decodeCustomMixes).orEmpty()
        val fadeSeconds = this[Keys.FADE_SECONDS]?.let(::clampFadeSeconds)
            ?: DEFAULT_SETTINGS.fadeSeconds
        val timerFadeSeconds = this[Keys.TIMER_FADE_SECONDS]?.let(::clampFadeSeconds)
            ?: DEFAULT_SETTINGS.timerFadeSeconds
        val requestAudioFocus = this[Keys.REQUEST_AUDIO_FOCUS]
            ?: DEFAULT_SETTINGS.requestAudioFocus
        val themePreference = this[Keys.THEME_PREFERENCE]
            ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: DEFAULT_SETTINGS.themePreference
        val binauralVolume = (this[Keys.BINAURAL_VOLUME] ?: DEFAULT_SETTINGS.binauralVolume)
            .coerceIn(0f, 1f)
        val binauralBand = this[Keys.BINAURAL_BAND]
        return Settings(
            particlesEnabled = particles,
            wavesEnabled = waves,
            timerPresetsSeconds = presets,
            languageTag = languageTag,
            customMixes = customMixes,
            warmth = warmth,
            fadeSeconds = fadeSeconds,
            timerFadeSeconds = timerFadeSeconds,
            requestAudioFocus = requestAudioFocus,
            themePreference = themePreference,
            binauralVolume = binauralVolume,
            binauralBand = binauralBand,
        )
    }
}

fun applyLanguageTag(tag: String?) {
    val locales = if (tag.isNullOrBlank()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
