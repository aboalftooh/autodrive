#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUPPORT="$ROOT/scripts/static-v00-support"
OUT="$ROOT/build/v00-static"
mkdir -p "$OUT"

command -v kotlinc >/dev/null 2>&1 || {
  echo "ERROR: kotlinc is required" >&2
  exit 1
}
command -v java >/dev/null 2>&1 || {
  echo "ERROR: java is required" >&2
  exit 1
}

KOTLIN_HOME="$(cd "$(dirname "$(command -v kotlinc)")/.." && pwd)"
COROUTINES_JAR="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
[[ -f "$COROUTINES_JAR" ]] || {
  echo "ERROR: missing $COROUTINES_JAR" >&2
  exit 1
}

cd "$ROOT"

# 1) Pure core Kotlin compilation.
mapfile -t CORE_SRC < <(find core/model/src/main/kotlin core/common/src/main/kotlin -name '*.kt' | sort)
kotlinc -cp "$COROUTINES_JAR" "${CORE_SRC[@]}" \
  -d "$OUT/core-base.jar" 2>&1 | tee "$OUT/core-base-compile.log"

# 2) Independent behavior suite compilation and execution.
BEHAVIOR_SRC=(
  "$SUPPORT/stubs-v13/org/junit/JUnitStubs.kt"
  "$SUPPORT/stubs-v13/javax/inject/InjectStubs.kt"
  "$SUPPORT/stubs-v13/kotlinx/coroutines/test/TestStubs.kt"
  "$SUPPORT/stubs-v13/com/autodrive/app/core/observability/AppLogger.kt"
  "$SUPPORT/stubs-v13/com/autodrive/app/core/database/entities/PendingOperationEntity.kt"
  "$SUPPORT/stubs-v13/com/autodrive/app/core/database/dao/PendingOperationDao.kt"
  core/model/src/main/kotlin/com/autodrive/app/core/model/money/Money.kt
  core/observability/src/main/kotlin/com/autodrive/app/core/observability/SensitiveDataRedactor.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/diagnostics/SyncDiagnostics.kt
  core/session/src/main/kotlin/com/autodrive/app/core/session/domain/CurrentSession.kt
  core/session/src/main/kotlin/com/autodrive/app/core/session/domain/SessionReader.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/RealtimeConnectionObserver.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/RealtimeController.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/SyncConnectivity.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/SyncCoordinator.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncEngine.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncStepExecutor.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/OutboxRetryPolicy.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/PendingOperationProcessor.kt
  core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeEventPolicy.kt
  feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/validation/SudanPhoneNumber.kt
  feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatMediaErrorMapper.kt
  feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/domain/model/CommissionModels.kt
  feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/domain/model/Invoice.kt
  feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/domain/CommissionCalculator.kt
  app/src/main/kotlin/com/autodrive/app/feature/reports/domain/repository/InvoiceDetailRepository.kt
  app/src/main/kotlin/com/autodrive/app/feature/reports/domain/usecase/GetInvoiceDetailsUseCase.kt
  feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/domain/model/NotificationModels.kt
  app/src/main/kotlin/com/autodrive/app/navigation/AppDestinations.kt
  app/src/main/kotlin/com/autodrive/app/navigation/NotificationDestinationResolver.kt
  app/src/test/kotlin/com/autodrive/app/core/model/money/MoneyTest.kt
  app/src/test/kotlin/com/autodrive/app/core/observability/SensitiveDataRedactorTest.kt
  app/src/test/kotlin/com/autodrive/app/core/session/domain/CurrentSessionTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinatorTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/data/SyncStepExecutorTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/outbox/OutboxRetryPolicyTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/outbox/OutboxSensitiveErrorTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/outbox/PendingOperationProcessorTest.kt
  app/src/test/kotlin/com/autodrive/app/core/sync/realtime/RealtimeEventPolicyTest.kt
  app/src/test/kotlin/com/autodrive/app/feature/auth/domain/validation/SudanPhoneNumberTest.kt
  app/src/test/kotlin/com/autodrive/app/feature/chat/data/ChatMediaErrorMapperTest.kt
  app/src/test/kotlin/com/autodrive/app/feature/commission/domain/CommissionCalculatorTest.kt
  app/src/test/kotlin/com/autodrive/app/feature/reports/domain/usecase/GetInvoiceDetailsUseCaseTest.kt
  app/src/test/kotlin/com/autodrive/app/navigation/NotificationDestinationResolverTest.kt
  "$SUPPORT/BehaviorRunner.kt"
)
kotlinc -cp "$COROUTINES_JAR" "${BEHAVIOR_SRC[@]}" \
  -include-runtime -d "$OUT/behavior-v00.jar" 2>&1 | tee "$OUT/behavior-compile.log"
java -cp "$OUT/behavior-v00.jar:$COROUTINES_JAR" BehaviorRunnerKt \
  2>&1 | tee "$OUT/behavior-tests.log"

# 3) Architecture suite compilation and execution.
ARCH_SRC=(
  "$SUPPORT/stubs-v13/org/junit/JUnitStubs.kt"
  app/src/test/kotlin/com/autodrive/app/architecture/*.kt
  "$SUPPORT/ArchitectureRunner.kt"
)
kotlinc "${ARCH_SRC[@]}" -include-runtime -d "$OUT/architecture-v00.jar" \
  2>&1 | tee "$OUT/architecture-compile.log"
java -jar "$OUT/architecture-v00.jar" 2>&1 | tee "$OUT/architecture-tests.log"

# 4) Repository static rules.
python3 tools/verify_modules_v13.py | tee "$OUT/module-checks.log"
python3 tools/verify_package_v12.py | tee "$OUT/package-checks.log"
python3 tools/verify_room_v10.py | tee "$OUT/room-checks.log"
python3 tools/verify_observability_v11.py | tee "$OUT/security-checks.log"
python3 tools/verify_cleanup_v14.py | tee "$OUT/cleanup-checks.log"

echo "v00 static verification: PASS"
