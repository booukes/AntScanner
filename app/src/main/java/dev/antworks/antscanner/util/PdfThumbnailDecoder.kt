package dev.antworks.antscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil.decode.DataSource
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Customowy dekoder dla biblioteki Coil.
 * Bierze URI PDF-a i zamienia jego pierwszą stronę w Bitmapę, którą Coil potrafi wyświetlić.
 */
class PdfThumbnailDecoder(
    private val context: Context,
    private val source: SourceResult,
    private val options: Options
) : Decoder {

    override suspend fun decode(): coil.decode.DecodeResult? = withContext(Dispatchers.IO) {
        try {
            // Coil daje nam dostęp do pliku w cache
            val file = source.source.file().toFile()
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

            // Używamy natywnego renderera Androida
            val renderer = PdfRenderer(fileDescriptor)

            if (renderer.pageCount > 0) {
                // Otwieramy pierwszą stronę (indeks 0)
                val page = renderer.openPage(0)

                // Tworzymy pustą bitmapę o proporcjach strony A4 (mniej więcej)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)

                // Renderujemy zawartość strony na bitmapę
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Zamykamy stronę i renderer, żeby nie wyciekała pamięć
                page.close()
                renderer.close()
                fileDescriptor.close()

                // Zwracamy gotową bitmapę opakowaną w format zrozumiały dla Coil
                coil.decode.DecodeResult(
                    drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                    isSampled = false
                )
            } else {
                renderer.close()
                fileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Nie udało się wyrenderować miniatury PDF.")
            null
        }
    }

    // Fabryka dla Coila, żeby wiedział, kiedy użyć tego dekodera
    class Factory(private val context: Context) : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: coil.ImageLoader): Decoder? {
            // Sprawdzamy, czy to w ogóle jest PDF (na podstawie MIME type)
            val isPdf = result.mimeType == "application/pdf"
            return if (isPdf) PdfThumbnailDecoder(context, result, options) else null
        }
    }
}