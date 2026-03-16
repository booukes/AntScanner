package dev.antworks.antscanner.scanner

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning

class AntScannerClient {

    /**
     * Zwraca gotowego do odpalenia klienta skanera.
     * Skonfigurowany sztywno, żeby Mama nie musiała niczego klikać ani się zastanawiać.
     */
    fun getClient(): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            // Wyłączamy grzebanie w galerii, skanujemy tylko na żywo z aparatu (mniej miejsc do pomyłki)
            .setGalleryImportAllowed(false)
            // Zgodnie z wytycznymi: max 10 stron na jeden plik
            .setPageLimit(10)
            // Interesuje nas tylko wypluty PDF, nie chcemy bawić się w luźne JPG-i
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            // Pełny tryb - daje zajebisty UI od Google'a z filtrami i kadrowaniem
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        return GmsDocumentScanning.getClient(options)
    }
}