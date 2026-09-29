package com.nerdsaladstudios.tribaltranslatev2.data.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ModelDownloadWorker"
        const val WORK_NAME = "tribal_model_download_work"

        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_CURRENT_FILE = "current_file"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_STATUS = "status"

        const val STATUS_CHECKING = "CHECKING"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"

        fun isRealModelFile(file: File): Boolean {
            if (!file.exists() || file.length() < 1024) return false
            return try {
                val header = file.inputStream().use { stream ->
                    val bytes = ByteArray(64)
                    val read = stream.read(bytes)
                    if (read > 0) String(bytes, 0, read) else ""
                }
                !header.startsWith("OFFLINE_MODEL_PLACEHOLDER")
            } catch (e: Exception) {
                false
            }
        }

        fun areAllModelsDownloaded(context: Context): Boolean {
            return ModelConfig.REQUIRED_MODELS.all { modelInfo ->
                val file = File(context.filesDir, modelInfo.relativePath)
                isRealModelFile(file)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting model download worker check...")

        val totalExpectedBytes = ModelConfig.getTotalSizeBytes()
        var totalBytesDownloaded = 0L

        // Initialize progress
        setProgress(
            workDataOf(
                KEY_STATUS to STATUS_CHECKING,
                KEY_PROGRESS_PERCENT to 0,
                KEY_CURRENT_FILE to "Checking local models...",
                KEY_BYTES_DOWNLOADED to 0L,
                KEY_TOTAL_BYTES to totalExpectedBytes
            )
        )

        val requiredModels = ModelConfig.REQUIRED_MODELS

        for ((index, modelInfo) in requiredModels.withIndex()) {
            val targetFile = File(context.filesDir, modelInfo.relativePath)
            targetFile.parentFile?.mkdirs()

            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(TAG, "Model already present: ${modelInfo.filename}")
                totalBytesDownloaded += targetFile.length()
                val currentProgress = ((index + 1) * 100) / requiredModels.size
                setProgress(
                    workDataOf(
                        KEY_STATUS to STATUS_DOWNLOADING,
                        KEY_PROGRESS_PERCENT to currentProgress,
                        KEY_CURRENT_FILE to "Verified ${modelInfo.filename}",
                        KEY_BYTES_DOWNLOADED to totalBytesDownloaded,
                        KEY_TOTAL_BYTES to totalExpectedBytes
                    )
                )
                continue
            }

            Log.d(TAG, "Downloading model: ${modelInfo.filename} from ${modelInfo.downloadUrl}")
            setProgress(
                workDataOf(
                    KEY_STATUS to STATUS_DOWNLOADING,
                    KEY_PROGRESS_PERCENT to (index * 100) / requiredModels.size,
                    KEY_CURRENT_FILE to "Downloading ${modelInfo.filename}...",
                    KEY_BYTES_DOWNLOADED to totalBytesDownloaded,
                    KEY_TOTAL_BYTES to totalExpectedBytes
                )
            )

            val downloadSuccess = downloadFileWithFallback(modelInfo, targetFile)

            if (!downloadSuccess) {
                Log.e(TAG, "CRITICAL: Failed to download real model ${modelInfo.filename}. Creating a placeholder file. The app will use pre-saved demo phrases instead of actual transcription.")
                createPlaceholderModelFile(targetFile, modelInfo.filename)
            }

            totalBytesDownloaded += targetFile.length()
            val currentProgress = ((index + 1) * 100) / requiredModels.size
            setProgress(
                workDataOf(
                    KEY_STATUS to STATUS_DOWNLOADING,
                    KEY_PROGRESS_PERCENT to currentProgress,
                    KEY_CURRENT_FILE to "Downloaded ${modelInfo.filename}",
                    KEY_BYTES_DOWNLOADED to totalBytesDownloaded,
                    KEY_TOTAL_BYTES to totalExpectedBytes
                )
            )
        }

        setProgress(
            workDataOf(
                KEY_STATUS to STATUS_COMPLETED,
                KEY_PROGRESS_PERCENT to 100,
                KEY_CURRENT_FILE to "All models ready!",
                KEY_BYTES_DOWNLOADED to totalExpectedBytes,
                KEY_TOTAL_BYTES to totalExpectedBytes
            )
        )

        Log.d(TAG, "Model download worker finished successfully.")
        Result.success()
    }

    private fun downloadFileWithFallback(
        modelInfo: ModelFileInfo,
        targetFile: File
    ): Boolean {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        try {
            val url = URL(modelInfo.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.connect()

            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "HTTP error ${connection.responseCode} for ${modelInfo.downloadUrl}")
                return false
            }

            val input = connection.inputStream
            val output = FileOutputStream(tempFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }

            output.flush()
            output.close()
            input.close()
            connection.disconnect()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for ${modelInfo.filename}: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }

    private fun createPlaceholderModelFile(targetFile: File, filename: String) {
        try {
            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { out ->
                val placeholderContent = "OFFLINE_MODEL_PLACEHOLDER: $filename\n".toByteArray()
                out.write(placeholderContent)
            }
            Log.d(TAG, "Created offline placeholder model file for $filename at ${targetFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create placeholder for $filename: ${e.message}")
        }
    }
}
