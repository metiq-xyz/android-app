package xyz.metiq.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import xyz.metiq.BuildConfig

const val KOFI_URL = "https://ko-fi.com/metiq"
const val FEEDBACK_URL =
    "https://github.com/metiq-xyz/android-app/issues/new?template=feedback.yml"

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

fun openUrlWithFallback(context: Context, primary: String, fallback: String) {
    val intent = Intent(Intent.ACTION_VIEW, primary.toUri())
    val ok = runCatching { context.startActivity(intent) }.isSuccess
    if (!ok) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, fallback.toUri())) }
}

fun openStoreRating(context: Context) {
    openUrlWithFallback(
        context,
        BuildConfig.STORE_RATE_URL,
        BuildConfig.STORE_RATE_FALLBACK_URL,
    )
}
