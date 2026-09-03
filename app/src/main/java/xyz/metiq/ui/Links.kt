package xyz.metiq.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import xyz.metiq.BuildConfig

const val KOFI_URL = "https://ko-fi.com/metiq"
const val FEEDBACK_URL =
    "https://github.com/metiq-xyz/android-app/issues/new?template=feedback.yml"

private fun viewIntent(url: String): Intent =
    Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(viewIntent(url))
    }
}

fun openUrlWithFallback(context: Context, primary: String, fallback: String) {
    val ok = runCatching { context.startActivity(viewIntent(primary)) }.isSuccess
    if (!ok) runCatching { context.startActivity(viewIntent(fallback)) }
}

fun openStoreRating(context: Context) {
    openUrlWithFallback(
        context,
        BuildConfig.STORE_RATE_URL,
        BuildConfig.STORE_RATE_FALLBACK_URL,
    )
}
