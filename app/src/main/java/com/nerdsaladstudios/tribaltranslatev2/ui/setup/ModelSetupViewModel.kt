package com.nerdsaladstudios.tribaltranslatev2.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nerdsaladstudios.tribaltranslatev2.data.download.ModelConfig
import com.nerdsaladstudios.tribaltranslatev2.data.download.ModelDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelSetupUiState(
    val progressPercent: Int = 0,
    val currentFile: String = "Initializing...",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = ModelConfig.getTotalSizeBytes(),
    val status: String = "Preparing download...",
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class ModelSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSetupUiState())
    val uiState: StateFlow<ModelSetupUiState> = _uiState.asStateFlow()

    fun checkAndStartDownload(context: Context) {
        viewModelScope.launch {
            if (ModelDownloadWorker.areAllModelsDownloaded(context)) {
                _uiState.update {
                    it.copy(
                        progressPercent = 100,
                        currentFile = "All models verified locally",
                        status = "Complete",
                        isCompleted = true
                    )
                }
                return@launch
            }

            val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .addTag(ModelDownloadWorker.WORK_NAME)
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                ModelDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )

            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo == null) return@collect

                val progress = workInfo.progress
                val percent = progress.getInt(ModelDownloadWorker.KEY_PROGRESS_PERCENT, 0)
                val currentFile = progress.getString(ModelDownloadWorker.KEY_CURRENT_FILE) ?: "Downloading..."
                val bytesDownloaded = progress.getLong(ModelDownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
                val totalBytes = progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, ModelConfig.getTotalSizeBytes())
                val statusStr = progress.getString(ModelDownloadWorker.KEY_STATUS) ?: "Downloading"

                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.update {
                            it.copy(
                                progressPercent = 100,
                                currentFile = "All model files ready!",
                                status = "Completed",
                                isCompleted = true,
                                errorMessage = null
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        _uiState.update {
                            it.copy(
                                status = "Download Failed",
                                errorMessage = "Failed to download model files. Tap to retry or use offline placeholders."
                            )
                        }
                    }
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        _uiState.update {
                            it.copy(
                                progressPercent = percent,
                                currentFile = currentFile,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                status = statusStr,
                                isCompleted = false
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun retryDownload(context: Context) {
        _uiState.update { ModelSetupUiState() }
        checkAndStartDownload(context)
    }
}
