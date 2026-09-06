package com.autodrive.app.feature.chat.data.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMediaTransferScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueue() = RetryFailedMessagesWorker.enqueueNow(context)
}
