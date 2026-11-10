package app.royalreader.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global app preferences, persisted via DataStore.
 **/
data class AppSettings(
    val reducedAnimations: Boolean = false,
    val autoBrightness: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("app_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store: DataStore<Preferences> = context.settingsDataStore

    private object Keys {
        val REDUCED_ANIMATIONS = booleanPreferencesKey("reduced_animations")
        val AUTO_BRIGHTNESS = booleanPreferencesKey("auto_brightness")
    }

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            reducedAnimations = prefs[Keys.REDUCED_ANIMATIONS] ?: false,
            autoBrightness = prefs[Keys.AUTO_BRIGHTNESS] ?: false,
        )
    }

    suspend fun setReducedAnimations(enabled: Boolean) {
        store.edit { it[Keys.REDUCED_ANIMATIONS] = enabled }
    }

    suspend fun setAutoBrightness(enabled: Boolean) {
        store.edit { it[Keys.AUTO_BRIGHTNESS] = enabled }
    }
}
