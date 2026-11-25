package `in`.rahulja.vision202020.ui.breakui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Single way to open [BreakActivity], shared by the break notification, tile and widget. */
object BreakLauncher {
    private const val REQUEST_CODE = 5

    fun intent(context: Context): Intent = Intent(context, BreakActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    fun pendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        intent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
