package com.nerdsaladstudios.tribaltranslatev2.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfRenderHelper {

    private const val TAG = "PdfRenderHelper"
    private const val PDF_ASSET_PATH = "pdfs/NIPUN_Class2_Hindi_Santali_Bilingual_Worksheets.pdf"

    fun renderPdfPages(context: Context): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            val cacheFile = File(context.cacheDir, "bilingual_worksheet.pdf")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open(PDF_ASSET_PATH).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            fileDescriptor = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)

            val pageCount = renderer.pageCount
            Log.d(TAG, "Opened PDF with $pageCount pages")

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF: ${e.message}", e)
        } finally {
            try { renderer?.close() } catch (e: Exception) {}
            try { fileDescriptor?.close() } catch (e: Exception) {}
        }

        return bitmaps
    }
}
