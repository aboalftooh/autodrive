from pathlib import Path

p = Path("feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/BalanceRepositoryImpl.kt")
s = p.read_text(encoding="utf-8")

replacements = [
    (
        "import com.autodrive.app.core.database.AutoDriveDatabase\n",
        "import com.autodrive.app.core.database.AutoDriveDatabase\n"
        "import com.autodrive.app.core.database.entities.MarketerBalanceEntity\n",
    ),
    (
        "import com.autodrive.app.core.network.dto.RequestWithdrawalParams\n",
        "import com.autodrive.app.core.network.dto.RequestWithdrawalParams\n"
        "import com.autodrive.app.core.network.serialization.BigDecimalSerializer\n",
    ),
    (
        "import io.github.jan.supabase.postgrest.rpc\n",
        "import io.github.jan.supabase.postgrest.rpc\n"
        "import io.github.jan.supabase.postgrest.query.Columns\n",
    ),
    (
        "import kotlinx.coroutines.flow.Flow\n",
        "import kotlinx.coroutines.flow.Flow\n"
        "import kotlinx.coroutines.flow.channelFlow\n"
        "import kotlinx.coroutines.flow.collect\n",
    ),
    (
        "import kotlinx.coroutines.flow.map\n",
        "import kotlinx.coroutines.flow.map\n"
        "import kotlinx.coroutines.launch\n",
    ),
    (
        "import java.time.Instant\n",
        "import java.math.BigDecimal\n"
        "import java.time.Instant\n",
    ),
]

for old, new in replacements:
    if new in s:
        continue
    if old not in s:
        raise SystemExit(f"balance import target missing: {old!r}")
    s = s.replace(old, new, 1)

old_observer = '''    override fun observeBalance(userId: String): Flow<MarketerBalance> {\n        if (userId.isBlank()) return flowOf(MarketerBalance(Money.ZERO, ""))\n        return db.marketerBalanceDao().observe(userId)\n            .map { it?.toDomain() ?: MarketerBalance(Money.ZERO, "") }\n            .flowOn(Dispatchers.IO)\n    }\n\n'''

new_observer = '''    override fun observeBalance(userId: String): Flow<MarketerBalance> {\n        if (userId.isBlank()) return flowOf(MarketerBalance(Money.ZERO, ""))\n\n        // Room stays the UI cache, but every observer heals it from the canonical booked balance.\n        // The refresh runs concurrently so cached/offline UI is not blocked by the network.\n        return channelFlow {\n            launch(Dispatchers.IO) {\n                runCatching { refreshCanonicalBalanceCache(userId) }\n            }\n            db.marketerBalanceDao().observe(userId)\n                .map { it?.toDomain() ?: MarketerBalance(Money.ZERO, "") }\n                .collect { send(it) }\n        }.flowOn(Dispatchers.IO)\n    }\n\n    private suspend fun refreshCanonicalBalanceCache(userId: String) {\n        val scope = SyncScope.from(sessionReader.currentSession()) ?: return\n        if (scope.userId != userId) return\n\n        val row = supabase.client.postgrest["marketer_balance"]\n            .select(Columns.ALL) {\n                filter {\n                    eq("client_id", scope.clientId)\n                    eq("org_id", scope.orgId)\n                }\n                limit(1)\n            }\n            .decodeList<CanonicalBalanceRowDto>()\n            .singleOrNull() ?: return\n\n        check(row.clientId == scope.clientId && row.orgId == scope.orgId) { "REMOTE_SCOPE_MISMATCH" }\n        check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }\n\n        db.marketerBalanceDao().upsert(\n            MarketerBalanceEntity(\n                id = row.id,\n                userId = scope.userId,\n                clientId = row.clientId,\n                balance = row.balance,\n                pendingWithdrawal = BigDecimal.ZERO,\n                updatedAt = row.updatedAt,\n            )\n        )\n    }\n\n'''

if new_observer not in s:
    if old_observer not in s:
        raise SystemExit("observeBalance patch target missing")
    s = s.replace(old_observer, new_observer, 1)

marker = '@Serializable\nprivate data class CancelPendingWithdrawalsParams('
dto = '''@Serializable\nprivate data class CanonicalBalanceRowDto(\n    val id: String,\n    @SerialName("client_id") val clientId: String,\n    @SerialName("org_id") val orgId: String,\n    @Serializable(with = BigDecimalSerializer::class)\n    val balance: BigDecimal,\n    @SerialName("updated_at") val updatedAt: String,\n)\n\n'''

if dto not in s:
    if marker not in s:
        raise SystemExit("balance DTO insertion target missing")
    s = s.replace(marker, dto + marker, 1)

p.write_text(s, encoding="utf-8")
print("Canonical balance cache healing patch: PASS")
