package dev.antworks.antscanner

import android.app.Application
import dev.antworks.antscanner.data.AntRepository
import dev.antworks.antscanner.data.StorageManager
import dev.antworks.antscanner.data.db.AppDatabase
import dev.antworks.antscanner.data.prefs.UserPreferencesManager
import timber.log.Timber

class AntScannerApp : Application() {

    // Leniwa inicjalizacja - odpala się dopiero, gdy jest potrzebne
    val database by lazy { AppDatabase.getDatabase(this) }
    val storageManager by lazy { StorageManager(this) }
    val prefsManager by lazy { UserPreferencesManager(this) }

    // Główne Repozytorium, o które pruje się teraz MainActivity
    val repository by lazy { AntRepository(database.documentDao(), storageManager, prefsManager) }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("AntScannerApp zainicjalizowana. Zależności gotowe do wstrzyknięcia.")
    }
}