package com.example.newssyncworkmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class NewsSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_USER_TOKEN) ?: "guest_token"
        Log.d("NewsSyncWorker", "Початок синхронізації новин для токена: $token")

        delay(3000)

        val outputData = Data.Builder()
            .putString(KEY_RESULT, "Успіх: завантажено 15 новин")
            .build()

        Log.d("NewsSyncWorker", "Синхронізація завершена успішно")
        return Result.success(outputData)
    }

    companion object {
        const val KEY_USER_TOKEN = "user_token"
        const val KEY_RESULT = "result"
    }
}