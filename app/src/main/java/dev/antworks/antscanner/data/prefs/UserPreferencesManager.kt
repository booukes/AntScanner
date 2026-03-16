package dev.antworks.antscanner.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

// Rozszerzenie na Context, żeby DataStore był singletonem (odpala się raz i siedzi w pamięci)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "antscanner_prefs")

class UserPreferencesManager(private val context: Context) {

    companion object {
        // Klucze, pod którymi trzymamy dane
        val MOM_NAME_KEY = stringPreferencesKey("mom_name")
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    }

    // Wyciągamy imię. Używamy Flow, więc UI zareaguje od razu, jak tylko to odczyta.
    val momNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MOM_NAME_KEY]
    }

    // Flaga sprawdzająca, czy Mama już podała imię (żeby nie męczyć jej tym ekranem drugi raz)
    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    // Funkcja odpalana po wpisaniu imienia na ekranie Welcome
    suspend fun saveMomName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[MOM_NAME_KEY] = name
            preferences[HAS_SEEN_ONBOARDING_KEY] = true
        }
        Timber.d("Imię zapisane: $name. Onboarding zaliczony. Można skanować!")
    }
}