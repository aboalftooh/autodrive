package com.autodrive.app.feature.chat.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.feature.chat.data.ChatMediaTransferProcessor
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class RetryFailedMessagesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: AutoDriveDatabase,
    private val chatRepository: ChatRepository,
    private val mediaTransferProcessor: ChatMediaTransferProcessor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        mediaTransferProcessor.processCurrentScope()
        val failed = db.chatMessageDao().getByStatus("FAILED")
        if (failed.isEmpty()) return Result.success()

        var anyError = false
        failed.forEach { msg ->
            val result = chatRepository.retrySend(msg.id)
            if (result is com.autodrive.app.core.common.result.Result.Error) anyError = true
        }

        return if (anyError) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "retry_failed_messages"
        private const val IMMEDIATE_WORK_NAME = "chat_media_transfer_now"

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<RetryFailedMessagesWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request,
            )
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RetryFailedMessagesWorker>(
                repeatInterval = 10,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
