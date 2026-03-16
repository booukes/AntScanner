package dev.antworks.antscanner.data

import android.net.Uri
import dev.antworks.antscanner.data.db.DocumentDao
import dev.antworks.antscanner.data.db.DocumentEntity
import dev.antworks.antscanner.data.prefs.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class AntRepository(
    private val documentDao: DocumentDao,
    private val storageManager: StorageManager,
    private val prefsManager: UserPreferencesManager
) {
    // --------------------------------------------------------
    // SEKCJA: DANE MASYWNE (Dokumenty)
    // --------------------------------------------------------

    // Lista dokumentów odświeżająca się sama (Flow)
    val allDocumentsFlow: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    /**
     * Główna funkcja biznesowa: najpierw fizyczny zapis, potem rekord w bazie.
     * Jak plik się spierdoli, to do bazy nie leci żaden śmieć.
     */
    suspend fun processAndSaveScan(tempUri: Uri, fileName: String): Boolean {
        Timber.d("Rozpoczynam proces zapisu skanu: $fileName")

        // 1. Zapisz fizycznie do folderu Pobrane
        val finalUri = storageManager.savePdfToDownloads(tempUri, fileName)

        return if (finalUri != null) {
            // 2. Jak się udało, wbijamy to do bazy danych
            val entity = DocumentEntity(
                name = fileName,
                uriString = finalUri.toString()
            )
            documentDao.insertDocument(entity)
            Timber.d("Sukces! Dokument $fileName wylądował bezpiecznie na dysku i w bazie.")
            true
        } else {
            Timber.e("Kurwa no, zapis fizyczny padł. Przerywam operację, baza nietknięta.")
            false
        }
    }

    // --------------------------------------------------------
    // SEKCJA: ONBOARDING I PREFERENCJE
    // --------------------------------------------------------

    val momNameFlow: Flow<String?> = prefsManager.momNameFlow
    val hasSeenOnboardingFlow: Flow<Boolean> = prefsManager.hasSeenOnboardingFlow

    suspend fun saveMomName(name: String) {
        prefsManager.saveMomName(name)
    }
}