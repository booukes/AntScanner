package dev.antworks.antscanner.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

class StorageManager(private val context: Context) {

    /**
     * Zapisuje zeskanowany PDF z cache do folderu Pobrane (Downloads).
     * Zwraca Uri docelowego pliku, jeśli się uda, lub null, jeśli coś się spierdoli.
     */
    fun savePdfToDownloads(sourceUri: Uri, fileName: String): Uri? {
        Timber.d("Zaczynamy zapis PDF-a: \$fileName do folderu Downloads...")

        val contentResolver = context.contentResolver
        // Zabezpieczenie przed brakiem rozszerzenia
        val pdfName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"

        // Przygotowujemy metadane dla MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, pdfName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            // Od API 29+ wrzucamy elegancko prosto w Pobrane
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // Prosimy system o utworzenie pustego pliku w Downloads
        val destinationUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (destinationUri == null) {
            Timber.e("Kurwa no, MediaStore zwrócił null przy tworzeniu pliku.")
            return null
        }

        return try {
            // Otwieramy strumienie: jeden czyta z cache, drugi ładuje do Downloads
            val inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            val outputStream: OutputStream? = contentResolver.openOutputStream(destinationUri)

            if (inputStream != null && outputStream != null) {
                inputStream.copyTo(outputStream)
                Timber.d("Zapis zakończony sukcesem! Plik leży w Downloads jako \$pdfName")
            } else {
                Timber.e("Nie udało się otworzyć strumieni danych.")
            }

            // Zawsze zamykamy drzwi za sobą
            inputStream?.close()
            outputStream?.close()

            destinationUri
        } catch (e: Exception) {
            Timber.e(e, "Spierdoliło się podczas kopiowania pliku PDF.")
            null
        }
    }
}