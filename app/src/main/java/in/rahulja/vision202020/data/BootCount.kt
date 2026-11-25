package `in`.rahulja.vision202020.data

import android.content.Context
import android.provider.Settings

/** Boot counter that scopes a session snapshot to one boot; -1 if the ROM does not expose it. */
fun Context.currentBootCount(): Int = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
