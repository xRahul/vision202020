package `in`.rahulja.vision202020.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import `in`.rahulja.vision202020.R

/** Spoken form such as "5 minutes 30 seconds remaining", for TalkBack content descriptions. */
@Composable
fun spokenTimeRemaining(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    val time = when {
        minutes == 0 -> pluralStringResource(R.plurals.a11y_seconds, seconds, seconds)

        seconds == 0 -> pluralStringResource(R.plurals.a11y_minutes, minutes, minutes)

        else -> pluralStringResource(R.plurals.a11y_minutes, minutes, minutes) + " " +
            pluralStringResource(R.plurals.a11y_seconds, seconds, seconds)
    }
    return stringResource(R.string.a11y_time_remaining, time)
}
