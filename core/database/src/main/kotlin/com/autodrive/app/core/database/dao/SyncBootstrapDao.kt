package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.SyncBootstrapStateEntity
import com.autodrive.app.core.database.entities.SyncBootstrapStagingEntity
import com.autodrive.app.core.database.entities.SyncReconciliationStateEntity

@Dao
interface SyncBootstrapDao {
    @Query(
        """
        SELECT * FROM sync_bootstrap_state
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND stream=:stream
        LIMIT 1
        """,
    )
    suspend fun getState(userId: String, clientId: String, orgId: String, stream: String): SyncBootstrapStateEntity?

    @Upsert
    suspend fun upsertState(state: SyncBootstrapStateEntity)

    @Upsert
    suspend fun upsertStaging(rows: List<SyncBootstrapStagingEntity>)

    @Query(
        """
        SELECT * FROM sync_bootstrap_staging
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND bootstrap_id=:bootstrapId
        ORDER BY entity_type, entity_id
        """,
    )
    suspend fun getStaging(
        userId: String,
        clientId: String,
        orgId: String,
        bootstrapId: String,
    ): List<SyncBootstrapStagingEntity>

    @Query(
        """
        SELECT COUNT(*) FROM sync_bootstrap_staging
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND bootstrap_id=:bootstrapId
        """,
    )
    suspend fun countStaging(userId: String, clientId: String, orgId: String, bootstrapId: String): Int

    @Query(
        """
        DELETE FROM sync_bootstrap_staging
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND bootstrap_id=:bootstrapId
        """,
    )
    suspend fun deleteStaging(userId: String, clientId: String, orgId: String, bootstrapId: String)

    @Query(
        """
        DELETE FROM sync_bootstrap_staging
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
        """,
    )
    suspend fun deleteStagingForScope(userId: String, clientId: String, orgId: String)

    @Query(
        """
        DELETE FROM sync_bootstrap_state
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
        """,
    )
    suspend fun deleteStateForScope(userId: String, clientId: String, orgId: String)
}

@Dao
interface SyncReconciliationStateDao {
    @Query(
        """
        SELECT * FROM sync_reconciliation_state
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND stream=:stream
        LIMIT 1
        """,
    )
    suspend fun get(
        userId: String,
        clientId: String,
        orgId: String,
        stream: String,
    ): SyncReconciliationStateEntity?

    @Upsert
    suspend fun upsert(state: SyncReconciliationStateEntity)

    @Query(
        """
        DELETE FROM sync_reconciliation_state
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
        """,
    )
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String)
}
