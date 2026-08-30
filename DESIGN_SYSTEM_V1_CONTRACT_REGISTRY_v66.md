# DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v66

- Freshly reconciled from current v65 source; v59 registry remains immutable.
- Runtime evidence remains blocked; contract source/API/consumer reconciliation is static.

## DS-CMP-001 — AutoDrivePrimaryButton
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt`
- publicApiFingerprint: `7e6ca2c59644219c0682337192ebd1eab394e0596154563416b5b521a39cea98`
- slots: NONE; parameters/variants only
- states: parameters: enabled, loading, onClick; typed state: AutoDrivePrimaryButton
- tokens: AutoDriveBrand.Primary, AutoDriveMotion.fast, AutoDriveRadius.MediumShape, AutoDriveSurface.Overlay, AutoDriveText.Disabled, AutoDriveText.OnBrand
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WelcomeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-002 — AutoDriveSecondaryButton
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt`
- publicApiFingerprint: `58c2e25cf63cbd950ca13397c240e00efc26934f75b74887f76afc331c84a257`
- slots: NONE; parameters/variants only
- states: parameters: enabled, loading, onClick; typed state: AutoDriveSecondaryButton
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-003 — AutoDriveTextButton
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt`
- publicApiFingerprint: `f6d64ea6d99a0f0aba73d788c5c1a26cfd9eda9d207fe5edbcac623398c0cd37`
- slots: NONE; parameters/variants only
- states: parameters: enabled, loading, tone, onClick; typed state: AutoDriveTextButton, AutoDriveTextButtonTone
- tokens: AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveRadius.MediumShape, AutoDriveSpace.SM, AutoDriveStatus.Error, AutoDriveText.Disabled, AutoDriveText.Secondary, AutoDriveTextButtonTone.Destructive, AutoDriveTextButtonTone.Neutral, AutoDriveTextButtonTone.Primary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-004 — AutoDriveIconButton
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt`
- publicApiFingerprint: `6c0805523c06fcd9477906d21efad09d83849c9a73bc49ca805344c80c5ee0ca`
- slots: NONE; parameters/variants only
- states: parameters: enabled, loading, selected, tone, onClick; typed state: AutoDriveIconButton, AutoDriveIconButtonTone
- tokens: AutoDriveBorder.Strong, AutoDriveBrand.Active, AutoDriveIconSize.MD, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveStatus.Error, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt`, `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt`, `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/AboutAppScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/PrivacyPolicyScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/OtpInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-005 — AutoDriveFab
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt`
- publicApiFingerprint: `156a47bbedf0efeade0bd5be1fa4788524113d9d92f26925b16135a33a2e551f`
- slots: NONE; parameters/variants only
- states: parameters: loading, onClick; typed state: AutoDriveFab
- tokens: AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveIconSize.LG, AutoDriveIconSize.MD, AutoDriveMotion.fast, AutoDriveText.OnBrand
- semantics: Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-006 — AutoDriveTextField
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt`
- publicApiFingerprint: `a7217f0e2d4d7584867651c9f24c04be6fa3ce77f8eede36712ce35ca7204a52`
- slots: trailingContent
- states: parameters: enabled, readOnly; typed state: AutoDriveTextField
- tokens: AutoDriveBorderColor.Default, AutoDriveBrand.Primary, AutoDriveIconSize.SM, AutoDriveRadius.MediumShape, AutoDriveStatus.Error, AutoDriveStatus.Error.copy, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-007 — AutoDriveSearchField
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt`
- publicApiFingerprint: `1a5bd552e7beb8f231b864861a60897e34050899adc00ecd19b5becb11966039`
- slots: NONE; parameters/variants only
- states: parameters: enabled; typed state: AutoDriveSearchField
- tokens: AutoDriveBorder.Strong, AutoDriveBorderColor.Default, AutoDriveBrand.Primary, AutoDriveIconSize.MD, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-008 — AutoDriveNumericField
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt`
- publicApiFingerprint: `13eaa7db0917882eaafa81073ee2c026602993b45fd7c3c2d9fdfe58dd69f3b8`
- slots: NONE; parameters/variants only
- states: parameters: enabled; typed state: AutoDriveNumericField
- tokens: AutoDriveTextField
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-009 — AutoDriveSelectionField
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt`
- publicApiFingerprint: `edc3cbcf94fbab511dbc0ecfc67683f12aef0152be3241792b5371fd89589aec`
- slots: NONE; parameters/variants only
- states: parameters: enabled, selected; typed state: AutoDriveSelectionField, AutoDriveSelectionOption
- tokens: AutoDriveBorderColor.Default, AutoDriveBrand.Active, AutoDriveBrand.Primary, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-010 — AutoDriveCard
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt`
- publicApiFingerprint: `b62e984a02ff650f3cdb84db1345ae77a27ab40030d38e49d2e836f2412dc17b`
- slots: content
- states: parameters: state, onClick; typed state: AutoDriveAccent, AutoDriveCard, AutoDriveCardState
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveOpacity.High, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSurface.Base
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-011 — AutoDriveMetricCard
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt`
- publicApiFingerprint: `a8aed97c089b4ec16247704f1c37358857182624061358bf483e873ea34bf121`
- slots: NONE; parameters/variants only
- states: parameters: accent, onClick; typed state: AutoDriveAccent, AutoDriveMetricCard
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.SM, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSpace.X3L, AutoDriveSpace.X6L, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/reports/ReportStatTile.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-012 — AutoDriveHighlightCard
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt`
- publicApiFingerprint: `0c3e9091cfe9bdcb46ee22548bdf767f17b281b188a96c60d345669c0765e70e`
- slots: content
- states: parameters: accent, onClick; typed state: AutoDriveAccent, AutoDriveHighlightCard
- tokens: AutoDriveBorder.Accent, AutoDriveOpacity.High, AutoDriveRadius.ExtraLargeShape, AutoDriveSpace.XL, AutoDriveSurface.Raised
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-013 — AutoDriveAlertCard
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt`
- publicApiFingerprint: `fc6905e9b2d1cba6286ea12ac246f79ec0068eb23024ab9eaff5c1b3629f3627`
- slots: action
- states: parameters: tone; typed state: AutoDriveAlertCard, AutoDriveStatusTone
- tokens: AutoDriveBorder.Accent, AutoDriveIconSize.MD, AutoDriveOpacity.High, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-014 — AutoDriveBottomNavigation
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt`
- publicApiFingerprint: `03d2ab13bc3d795ec2a427be9e982918d5c17579bc54ff6d6e150143d0d985cb`
- slots: centerAction
- states: parameters: stateless/content-driven; typed state: AutoDriveBottomNavigation, AutoDriveNavigationItem, AutoDriveRadius
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveSpace.SM, AutoDriveSpace.X6L, AutoDriveSurface.Base
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-015 — AutoDriveTopHeader
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt`
- publicApiFingerprint: `38aa0525d652a62aa2ad0772b8e46a41840de812aaeed833d485847e2f95fc28`
- slots: leadingContent, actions, titleContent
- states: parameters: stateless/content-driven; typed state: AutoDriveTopHeader
- tokens: AutoDriveSpace.LG, AutoDriveSpace.MD, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-016 — AutoDriveBackHeader
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt`
- publicApiFingerprint: `98b600b45ec2389549e2e31e68d8076a729f1d27fe4cd60db3e2a76479be9771`
- slots: trailingAction
- states: parameters: stateless/content-driven; typed state: AutoDriveBackHeader
- tokens: AutoDriveIconSize.MD, AutoDriveIconSize.TouchTarget, AutoDriveRadius.PillShape, AutoDriveSpace.SM, AutoDriveText.Primary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt`, `feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-017 — AutoDriveBadge
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `1cd3f409e2b138b340e4fe8a788ba92653b0052ce6634077057ca14ca0289c67`
- slots: NONE; parameters/variants only
- states: parameters: count; typed state: AutoDriveBadge
- tokens: AutoDriveBrand.Primary, AutoDriveRadius.PillShape, AutoDriveSpace.XS, AutoDriveText.OnBrand
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-018 — AutoDriveStatusChip
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `5506d8f8fb77cf2b8703654fb680ca6c1886a78a06a45eb418982da9a4c97af9`
- slots: NONE; parameters/variants only
- states: parameters: tone; typed state: AutoDriveStatusChip, AutoDriveStatusTone
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.XS, AutoDriveOpacity.Muted, AutoDriveOpacity.Tint, AutoDriveRadius.PillShape, AutoDriveSpace.MD, AutoDriveSpace.XS, AutoDriveStatusTone.Neutral
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-019 — AutoDriveSnackbarContent
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `ccfbe8827c7c92b6fa418cb442b4399b13cc2691a2e22fe70ac524c1ce8ec165`
- slots: action
- states: parameters: stateless/content-driven; typed state: AutoDriveSnackbarContent
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.SM, AutoDriveRadius.MediumShape, AutoDriveSpace.LG, AutoDriveSpace.SM, AutoDriveSurface.Overlay, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-020 — AutoDriveDialog
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `58fe85c246bb38878e688753483e55161d6e57657ad1a88d9a72c51e08fd1ab4`
- slots: content, actions
- states: parameters: tone; typed state: AutoDriveDialog, AutoDriveDialogTone
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveOpacity.High, AutoDriveRadius.ExtraLargeShape, AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveSpace.X2L, AutoDriveStatus.Error.copy, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-021 — AutoDriveBottomSheet
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `aee215cd183534028f98e5feca38f2aa593b0f5bc652f26ac64a3b7437eb4dc0`
- slots: content
- states: parameters: stateless/content-driven; typed state: AutoDriveBottomSheet
- tokens: AutoDriveRadius.PillShape, AutoDriveRadius.X2L, AutoDriveSpace.LG, AutoDriveSpace.XL, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-022 — AutoDriveLoadingState
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `593443030bce9968e8c5a872be857bf9287cc2ab98ca1047a9f3b0ca416b51d2`
- slots: NONE; parameters/variants only
- states: parameters: variant; typed state: AutoDriveLoadingState, AutoDriveLoadingVariant
- tokens: AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveSpace.MD, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatScreen.kt`, `feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-023 — AutoDriveEmptyState
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt`
- publicApiFingerprint: `382c532e472f75dfa61b11107b16c334259e618898ccafe389fd14b10a1d4da4`
- slots: action
- states: parameters: centered; typed state: AutoDriveEmptyState
- tokens: AutoDriveIconSize.Hero, AutoDriveSpace.MD, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-024 — AutoDriveAvatar
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `b2e8d7121e808d1c5e9009d6743467313b62668f572532ac9664c7710859e193`
- slots: imageContent
- states: parameters: accent; typed state: AutoDriveAccent, AutoDriveAvatar, AutoDriveAvatarSize
- tokens: AutoDriveBorder.Thin, AutoDriveOpacity.Muted, AutoDriveOpacity.Subtle
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-025 — AutoDriveListRow
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `6271fb74b52794b8febc8d17de1cca884f01e3e51090b60f1710a0518b5f5a66`
- slots: leading, trailing
- states: parameters: enabled, selected, onClick; typed state: AutoDriveListRow
- tokens: AutoDriveBrand.Active.copy, AutoDriveOpacity.Tint, AutoDriveSpace.LG, AutoDriveSpace.MD, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-026 — AutoDriveSectionHeader
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `be020ffd1bd121774e7954dcfb674ab82144e40ccd82867f3d54b993df192190`
- slots: action
- states: parameters: stateless/content-driven; typed state: AutoDriveSectionHeader
- tokens: AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-027 — AutoDriveDivider
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `e8fea9931593f4b32aef18b4e060284d5fde9f057da90a22c9b7049c3870564c`
- slots: NONE; parameters/variants only
- states: parameters: stateless/content-driven; typed state: AutoDriveDivider
- tokens: AutoDriveBorder.Thin, AutoDriveBorderColor.Default
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/info/presentation/PrivacyPolicyScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-028 — AutoDriveStatValue
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `4f18398e516388e64e4569bcae4009757fc3b0b52121c89da958952f3c995672`
- slots: NONE; parameters/variants only
- states: parameters: accent; typed state: AutoDriveAccent, AutoDriveStatSize, AutoDriveStatValue
- tokens: AutoDriveSpace.SM, AutoDriveStatXL, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-029 — AutoDriveStatusIndicator
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `40d5d3b870a35434cdf77fea3778c4b04cb9126a7abb3bdf14dac8623abd637d`
- slots: NONE; parameters/variants only
- states: parameters: tone; typed state: AutoDriveStatusIndicator, AutoDriveStatusTone
- tokens: MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- semantics: Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-030 — AutoDriveStepIndicator
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `9dc825634358937f6dcdddabd6aa61ef119b43b9701351e61d5e556aea739ab2`
- slots: NONE; parameters/variants only
- states: parameters: currentStep, totalSteps; typed state: AutoDriveStepIndicator
- tokens: AutoDriveBorderColor.Default, AutoDriveBrand.Active, AutoDriveBrand.Active.copy, AutoDriveMotion.emphasized, AutoDriveOpacity.Medium, AutoDriveRadius.PillShape, AutoDriveSpace.SM
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-CMP-031 — AutoDriveInstrumentNumber
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt`
- publicApiFingerprint: `d2fe607d92b7de19f4a4b2e1e3bc31353c8157d07f43a1411f4ab4a9e9de5197`
- slots: NONE; parameters/variants only
- states: parameters: tone; typed state: AutoDriveInstrumentNumber, AutoDriveInstrumentTone
- tokens: AutoDriveBrand.Active, AutoDriveBrand.Secondary, AutoDriveInstrument.Caution, AutoDriveInstrument.Empty, AutoDriveInstrument.Full, AutoDriveInstrument.Good, AutoDriveInstrument.Low, AutoDriveInstrumentTone.Active, AutoDriveInstrumentTone.Caution, AutoDriveInstrumentTone.Empty, AutoDriveInstrumentTone.Full, AutoDriveInstrumentTone.Good, AutoDriveInstrumentTone.Low, AutoDriveInstrumentTone.Secondary, AutoDriveSpace.SM
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-001 — ScreenHeader
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt`
- publicApiFingerprint: `884379aa5eb89b9c741790d8548bb4230d6e237cc38cff33fc9d39136440848b`
- slots: trailing, context, titleContent
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: AutoDriveSpace.LG, AutoDriveSpace.SM
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-002 — DashboardHero
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt`
- publicApiFingerprint: `98e26c8c1b7675245185cda15e188e8e07603d3943b65357a609fee5e8653a13`
- slots: heroContent, supportingContent, action
- states: parameters: accent; typed state: AutoDriveAccent
- tokens: AutoDriveSpace.LG, AutoDriveText.Secondary
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-003 — MetricSummary
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt`
- publicApiFingerprint: `d08d5b21b04ccc599ac93864153c44489cf3612544cd21178ed53eabccf964fa`
- slots: NONE; parameters/variants only
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: AutoDriveSpace.MD
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-004 — ConversationItem
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`
- publicApiFingerprint: `c7367f8fb4e8e7ebee85f1453cf037c6e6eafe8de41b761fee28e1768e678f21`
- slots: NONE; parameters/variants only
- states: parameters: onClick; typed state: NONE
- tokens: AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-005 — TransactionRow
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`
- publicApiFingerprint: `6782b7b218585fd5241ba5e82de5c8300dfa288762a964b2e300b9a6390c2b8f`
- slots: NONE; parameters/variants only
- states: parameters: tone, onClick; typed state: AutoDriveStatusTone
- tokens: AutoDriveStatusChip
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-006 — PendingRequestCard
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`
- publicApiFingerprint: `2898318a9c565020e77ec870ef8706b36d711f60acb0f1f4df8aeba42e20d239`
- slots: NONE; parameters/variants only
- states: parameters: loading; typed state: NONE
- tokens: AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveStatusChip, AutoDriveStatusTone.Warning
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-007 — SettingsGroup
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- publicApiFingerprint: `f7052cf375355c2e296ead420b312b8a291facacaab52914ea3c8b339c50f0a2`
- slots: headerAction
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-008 — SettingsRow
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- publicApiFingerprint: `a001a6feea36b54b5488c8600c93f5ee2e219a9944eef8acd5af4c2df19b07b9`
- slots: NONE; parameters/variants only
- states: parameters: enabled, variant, onClick; typed state: AutoDriveStatusTone, SettingsRowVariant
- tokens: AutoDriveStatus.Error, AutoDriveStatusChip, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-009 — ReportStatTile
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/reports/ReportStatTile.kt`
- publicApiFingerprint: `2f56be0ec12e5ddb331e686edd31a4d1b06004a30cd4bf03025be314806912e1`
- slots: NONE; parameters/variants only
- states: parameters: accent, onClick; typed state: AutoDriveAccent
- tokens: MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-010 — MediaActionGroup
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- publicApiFingerprint: `f73c15074dd2315bb41e649a9054cc243278ceaf7f7d46e0e0aac6a7ab40b6d7`
- slots: NONE; parameters/variants only
- states: parameters: state; typed state: MediaActionState
- tokens: AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveStatusIndicator, AutoDriveStatusTone.Error, AutoDriveText.Primary
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-011 — SearchResultsList
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`
- publicApiFingerprint: `c732c427aada1f1c2edce279ceae446d764cd6c22c56b3395d1440bcc2eb314a`
- slots: itemContent
- states: parameters: state, query; typed state: SearchResultsState
- tokens: AutoDriveSpace.LG
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-012 — EmptyScreen
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`
- publicApiFingerprint: `d2ff392e6ef75f85cbf44c6b1a4a6be1bab5486d201935b6a89a93587fb324b8`
- slots: NONE; parameters/variants only
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: AutoDriveSpace.LG
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-013 — ErrorScreen
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`
- publicApiFingerprint: `44a6a8b8c9c58d96a46b7d20f1dbbeaad21170727f426b4267e275f382e77680`
- slots: NONE; parameters/variants only
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: AutoDriveSpace.LG
- semantics: Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-PAT-014 — LoadingScreen
- ownerPath: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`
- publicApiFingerprint: `0a9e32333a008f1d1b70616a37b151c83697b2c4279c3df3d5efc6c67b62c38e`
- slots: NONE; parameters/variants only
- states: parameters: stateless/content-driven; typed state: NONE
- tokens: AutoDriveSpace.LG
- semantics: Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- knownConsumers: v59 registry
- currentConsumers: `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-SCR-HOME-001 — HOME_V1
- ownerPath: `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`
- publicApiFingerprint: `05938d4e69326c0fb4250c9ed6074c19e37b5257e99253815740b2c0aa7b3745`
- slots: current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- states: competitionAvailability DISABLED hides competition card; LOCKED/ACTIVE retained
- tokens: consume frozen V1 components/patterns/foundations; no new tokens in v59.
- semantics: screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- knownConsumers: v59 registry
- currentConsumers: NONE
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-SCR-REPORTS-001 — REPORTS_V1
- ownerPath: `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`
- publicApiFingerprint: `258078b62f4dc5de7c120a518f56c7eb9e5c9cfe1f443de7e3b2e01b2ab0fc60`
- slots: current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- states: ReportsLoadState.LOADING; ReportsLoadState.ERROR; ReportsLoadState.CONTENT; competition ACTIVE branch
- tokens: consume frozen V1 components/patterns/foundations; no new tokens in v59.
- semantics: screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- knownConsumers: v59 registry
- currentConsumers: NONE
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

## DS-SCR-SETTINGS-001 — SETTINGS_V1
- ownerPath: `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- publicApiFingerprint: `fe5eeb5726b876485f7631e9dc021ce2b00b0266bd5290024ccecce1d05cb220`
- slots: current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- states: WORKSHOP_OWNER guard; section-based editor; destructive sign-out confirmation
- tokens: consume frozen V1 components/patterns/foundations; no new tokens in v59.
- semantics: screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- knownConsumers: v59 registry
- currentConsumers: NONE
- runtimeEvidence: `BLOCKED_GRADLE_BOOTSTRAP`
- status: `MIGRATED`

