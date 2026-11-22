package `in`.rahulja.vision202020.ui.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.ui.theme.VisionTheme

class AboutLibrariesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTheme = intent.getStringExtra(EXTRA_THEME)
            ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.OLED_BLACK
        val repository = PreferencesRepositoryImpl(applicationContext.dataStore)

        setContent {
            val preferences by repository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences(appTheme = initialTheme))

            VisionTheme(appTheme = preferences.appTheme) {
                val resources = LocalResources.current
                val librariesJson = remember {
                    resources.openRawResource(R.raw.aboutlibraries).use { it.readBytes() }
                }
                val libraries by produceLibraries(librariesJson)
                LibrariesContainer(
                    libraries = libraries,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    companion object {
        const val EXTRA_THEME = "extra_theme"
    }
}
