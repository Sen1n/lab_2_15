package com.example.newssyncworkmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NewsSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application.applicationContext)

    private val _syncStatus = MutableStateFlow("Очікування запуску...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _nightSyncStatus = MutableStateFlow("Нічна синхронізація вимкнена")
    val nightSyncStatus: StateFlow<String> = _nightSyncStatus.asStateFlow()

    private var observeJob: Job? = null

    fun syncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(NewsSyncWorker.KEY_USER_TOKEN, "abc123")
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<NewsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueue(oneTimeRequest)

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(oneTimeRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    _syncStatus.value = when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> "ENQUEUED: Очікує інтернет-з'єднання..."
                        WorkInfo.State.RUNNING -> "RUNNING: Виконується синхронізація..."
                        WorkInfo.State.SUCCEEDED -> {
                            val resultMessage = workInfo.outputData.getString(NewsSyncWorker.KEY_RESULT)
                                ?: "Синхронізовано"
                            "SUCCEEDED: $resultMessage"
                        }
                        WorkInfo.State.FAILED -> "FAILED: Помилка під час синхронізації"
                        WorkInfo.State.BLOCKED -> "BLOCKED: Задача заблокована"
                        WorkInfo.State.CANCELLED -> "CANCELLED: Задачу скасовано"
                    }
                }
            }
        }
    }

    fun enableNightSync() {
        val nightConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val inputData = Data.Builder()
            .putString(NewsSyncWorker.KEY_USER_TOKEN, "abc123")
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(nightConstraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NIGHT_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        _nightSyncStatus.value = "Нічна синхронізація активна (кожні 15 хв, лише Wi-Fi + зарядка)"
    }

    companion object {
        const val NIGHT_SYNC_WORK_NAME = "night_news_sync"
    }
}