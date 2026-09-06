package com.autodrive.app.core.sync.domain

import kotlinx.coroutines.flow.Flow

interface SyncConnectivity {
    val isConnected: Flow<Boolean>
    fun isConnectedNow(): Boolean
}
