package com.parthjivani.ledgerly.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = LedgerlyRepository.getInstance(applicationContext)
            
            // Check if Supabase credentials exist
            val token = repository.getSetting("supabase_access_token", "")
            if (token.isNotEmpty()) {
                SupabaseClient.accessToken = token
            }
            val refreshToken = repository.getSetting("supabase_refresh_token", "")
            if (refreshToken.isNotEmpty()) {
                SupabaseClient.refreshToken = refreshToken
            }

            if (!SupabaseClient.isConfigured) {
                return@withContext Result.success()
            }

            val success = repository.pushAllPendingToSupabase()
            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ledgerly_offline_sync_work"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
