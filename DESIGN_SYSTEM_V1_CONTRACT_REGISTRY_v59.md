# DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59

- schema/session: 59
- source: `AutoDrive-v58(3).zip` / `f867f1b3fae63d586172b52e12106dfcc6a9307c826b1a182eddd43374db84ee`
- frozen records: 48 = 31 components + 14 patterns + 3 screens
- authority: current v58 code + V07 set; historical docs are references, not current conformance proof.

## DS-CMP-001 — AutoDrivePrimaryButton
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` (definition line 70)
- **publicApi/signature summary:** `fun AutoDrivePrimaryButton( text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null, highlighted: Boolean = false, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, loading, onClick; typed state: AutoDrivePrimaryButton
- **foundation tokens used/required:** AutoDriveBrand.Primary, AutoDriveMotion.fast, AutoDriveRadius.MediumShape, AutoDriveSurface.Overlay, AutoDriveText.Disabled, AutoDriveText.OnBrand
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WelcomeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-002 — AutoDriveSecondaryButton
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` (definition line 102)
- **publicApi/signature summary:** `fun AutoDriveSecondaryButton( text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, loading, onClick; typed state: AutoDriveSecondaryButton
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-003 — AutoDriveTextButton
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` (definition line 125)
- **publicApi/signature summary:** `fun AutoDriveTextButton( text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, tone: AutoDriveTextButtonTone = AutoDriveTextButtonTone.Neutral, icon: ImageVector? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, loading, tone, onClick; typed state: AutoDriveTextButton, AutoDriveTextButtonTone
- **foundation tokens used/required:** AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveRadius.MediumShape, AutoDriveSpace.SM, AutoDriveStatus.Error, AutoDriveText.Disabled, AutoDriveText.Secondary, AutoDriveTextButtonTone.Destructive, AutoDriveTextButtonTone.Neutral, AutoDriveTextButtonTone.Primary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-004 — AutoDriveIconButton
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` (definition line 155)
- **publicApi/signature summary:** `fun AutoDriveIconButton( icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean = false, loading: Boolean = false, tone: AutoDriveIconButtonTone = AutoDriveIconButtonTone.Neutral, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, loading, selected, tone, onClick; typed state: AutoDriveIconButton, AutoDriveIconButtonTone
- **foundation tokens used/required:** AutoDriveBorder.Strong, AutoDriveBrand.Active, AutoDriveIconSize.MD, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveStatus.Error, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-005 — AutoDriveFab
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt` (definition line 187)
- **publicApi/signature summary:** `fun AutoDriveFab( onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Add, contentDescription: String, loading: Boolean = false, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: loading, onClick; typed state: AutoDriveFab
- **foundation tokens used/required:** AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveIconSize.LG, AutoDriveIconSize.MD, AutoDriveMotion.fast, AutoDriveText.OnBrand
- **semantics responsibility:** Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-006 — AutoDriveTextField
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt` (definition line 46)
- **publicApi/signature summary:** `fun AutoDriveTextField( value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", supportingText: String? = null, errorText: String? = null, enabled: Boolean = true, readOnly: Boolean = false, singleLine: Boolean = true, maxLines: Int = if (singleLine) 1 else 5, leadingIcon: ImageVector? = null, trailingContent: (@Composable () -> Unit)? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, )`
- **allowed slots/variants:** trailingContent
- **state model:** parameters: enabled, readOnly; typed state: AutoDriveTextField
- **foundation tokens used/required:** AutoDriveBorderColor.Default, AutoDriveBrand.Primary, AutoDriveIconSize.SM, AutoDriveRadius.MediumShape, AutoDriveStatus.Error, AutoDriveStatus.Error.copy, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-007 — AutoDriveSearchField
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt` (definition line 111)
- **publicApi/signature summary:** `fun AutoDriveSearchField( value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, enabled: Boolean = true, searching: Boolean = false, onClear: () -> Unit = { onValueChange("") }, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled; typed state: AutoDriveSearchField
- **foundation tokens used/required:** AutoDriveBorder.Strong, AutoDriveBorderColor.Default, AutoDriveBrand.Primary, AutoDriveIconSize.MD, AutoDriveIconSize.SM, AutoDriveIconSize.TouchTarget, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-008 — AutoDriveNumericField
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt` (definition line 162)
- **publicApi/signature summary:** `fun AutoDriveNumericField( value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", supportingText: String? = null, errorText: String? = null, enabled: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled; typed state: AutoDriveNumericField
- **foundation tokens used/required:** AutoDriveTextField
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-009 — AutoDriveSelectionField
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt` (definition line 194)
- **publicApi/signature summary:** `fun AutoDriveSelectionField( selected: AutoDriveSelectionOption?, options: List<AutoDriveSelectionOption>, label: String, onSelected: (AutoDriveSelectionOption) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String = "", )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, selected; typed state: AutoDriveSelectionField, AutoDriveSelectionOption
- **foundation tokens used/required:** AutoDriveBorderColor.Default, AutoDriveBrand.Active, AutoDriveBrand.Primary, AutoDriveRadius.MediumShape, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-010 — AutoDriveCard
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt` (definition line 39)
- **publicApi/signature summary:** `fun AutoDriveCard( modifier: Modifier = Modifier, state: AutoDriveCardState = AutoDriveCardState.Default, selectedAccent: AutoDriveAccent = AutoDriveAccent.Active, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit, )`
- **allowed slots/variants:** content
- **state model:** parameters: state, onClick; typed state: AutoDriveAccent, AutoDriveCard, AutoDriveCardState
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveOpacity.High, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSurface.Base
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-011 — AutoDriveMetricCard
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt` (definition line 59)
- **publicApi/signature summary:** `fun AutoDriveMetricCard( label: String, value: String, modifier: Modifier = Modifier, supportingText: String? = null, accent: AutoDriveAccent? = null, icon: ImageVector? = null, onClick: (() -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: accent, onClick; typed state: AutoDriveAccent, AutoDriveMetricCard
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.SM, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSpace.X3L, AutoDriveSpace.X6L, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/reports/ReportStatTile.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-012 — AutoDriveHighlightCard
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt` (definition line 87)
- **publicApi/signature summary:** `fun AutoDriveHighlightCard( accent: AutoDriveAccent, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit, )`
- **allowed slots/variants:** content
- **state model:** parameters: accent, onClick; typed state: AutoDriveAccent, AutoDriveHighlightCard
- **foundation tokens used/required:** AutoDriveBorder.Accent, AutoDriveOpacity.High, AutoDriveRadius.ExtraLargeShape, AutoDriveSpace.XL, AutoDriveSurface.Raised
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-013 — AutoDriveAlertCard
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/containers/ContainerComponents.kt` (definition line 103)
- **publicApi/signature summary:** `fun AutoDriveAlertCard( title: String, body: String, tone: AutoDriveStatusTone, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Info, action: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** action
- **state model:** parameters: tone; typed state: AutoDriveAlertCard, AutoDriveStatusTone
- **foundation tokens used/required:** AutoDriveBorder.Accent, AutoDriveIconSize.MD, AutoDriveOpacity.High, AutoDriveRadius.LargeShape, AutoDriveSpace.LG, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-014 — AutoDriveBottomNavigation
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt` (definition line 57)
- **publicApi/signature summary:** `fun AutoDriveBottomNavigation( items: List<AutoDriveNavigationItem>, selectedItemId: String, onItemClick: (AutoDriveNavigationItem) -> Unit, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(topStart = AutoDriveRadius.X2L, topEnd = AutoDriveRadius.X2L), contentHeight: Dp = 72.dp, centerAction: (@Composable () -> Unit)? = null, centerActionAfterIndex: Int = 1, )`
- **allowed slots/variants:** centerAction
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveBottomNavigation, AutoDriveNavigationItem, AutoDriveRadius
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveSpace.SM, AutoDriveSpace.X6L, AutoDriveSurface.Base
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-015 — AutoDriveTopHeader
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt` (definition line 131)
- **publicApi/signature summary:** `fun AutoDriveTopHeader( title: String, modifier: Modifier = Modifier, subtitle: String? = null, leadingContent: (@Composable () -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null, titleContent: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** leadingContent, actions, titleContent
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveTopHeader
- **foundation tokens used/required:** AutoDriveSpace.LG, AutoDriveSpace.MD, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-016 — AutoDriveBackHeader
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt` (definition line 155)
- **publicApi/signature summary:** `fun AutoDriveBackHeader( title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailingAction: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** trailingAction
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveBackHeader
- **foundation tokens used/required:** AutoDriveIconSize.MD, AutoDriveIconSize.TouchTarget, AutoDriveRadius.PillShape, AutoDriveSpace.SM, AutoDriveText.Primary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-017 — AutoDriveBadge
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 50)
- **publicApi/signature summary:** `fun AutoDriveBadge( modifier: Modifier = Modifier, count: Int? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: count; typed state: AutoDriveBadge
- **foundation tokens used/required:** AutoDriveBrand.Primary, AutoDriveRadius.PillShape, AutoDriveSpace.XS, AutoDriveText.OnBrand
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-018 — AutoDriveStatusChip
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 73)
- **publicApi/signature summary:** `fun AutoDriveStatusChip( text: String, tone: AutoDriveStatusTone, modifier: Modifier = Modifier, icon: ImageVector? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: tone; typed state: AutoDriveStatusChip, AutoDriveStatusTone
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.XS, AutoDriveOpacity.Muted, AutoDriveOpacity.Tint, AutoDriveRadius.PillShape, AutoDriveSpace.MD, AutoDriveSpace.XS, AutoDriveStatusTone.Neutral
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-019 — AutoDriveSnackbarContent
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 99)
- **publicApi/signature summary:** `fun AutoDriveSnackbarContent( message: String, modifier: Modifier = Modifier, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** action
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveSnackbarContent
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveIconSize.SM, AutoDriveRadius.MediumShape, AutoDriveSpace.LG, AutoDriveSpace.SM, AutoDriveSurface.Overlay, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-020 — AutoDriveDialog
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 124)
- **publicApi/signature summary:** `fun AutoDriveDialog( title: String, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, body: String? = null, tone: AutoDriveDialogTone = AutoDriveDialogTone.Information, content: (@Composable () -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, )`
- **allowed slots/variants:** content, actions
- **state model:** parameters: tone; typed state: AutoDriveDialog, AutoDriveDialogTone
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveOpacity.High, AutoDriveRadius.ExtraLargeShape, AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveSpace.X2L, AutoDriveStatus.Error.copy, AutoDriveSurface.Raised, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-021 — AutoDriveBottomSheet
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 163)
- **publicApi/signature summary:** `fun AutoDriveBottomSheet( onDismissRequest: () -> Unit, title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit, )`
- **allowed slots/variants:** content
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveBottomSheet
- **foundation tokens used/required:** AutoDriveRadius.PillShape, AutoDriveRadius.X2L, AutoDriveSpace.LG, AutoDriveSpace.XL, AutoDriveSurface.Raised, AutoDriveText.Disabled, AutoDriveText.Primary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-022 — AutoDriveLoadingState
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 191)
- **publicApi/signature summary:** `fun AutoDriveLoadingState( modifier: Modifier = Modifier, variant: AutoDriveLoadingVariant = AutoDriveLoadingVariant.Content, label: String? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: variant; typed state: AutoDriveLoadingState, AutoDriveLoadingVariant
- **foundation tokens used/required:** AutoDriveBorder.Strong, AutoDriveBrand.Primary, AutoDriveSpace.MD, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-023 — AutoDriveEmptyState
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt` (definition line 209)
- **publicApi/signature summary:** `fun AutoDriveEmptyState( title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Inbox, action: (@Composable () -> Unit)? = null, centered: Boolean = true, )`
- **allowed slots/variants:** action
- **state model:** parameters: centered; typed state: AutoDriveEmptyState
- **foundation tokens used/required:** AutoDriveIconSize.Hero, AutoDriveSpace.MD, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-024 — AutoDriveAvatar
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 62)
- **publicApi/signature summary:** `fun AutoDriveAvatar( name: String, modifier: Modifier = Modifier, size: AutoDriveAvatarSize = AutoDriveAvatarSize.Default, accent: AutoDriveAccent = AutoDriveAccent.Active, imageContent: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** imageContent
- **state model:** parameters: accent; typed state: AutoDriveAccent, AutoDriveAvatar, AutoDriveAvatarSize
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveOpacity.Muted, AutoDriveOpacity.Subtle
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-025 — AutoDriveListRow
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 89)
- **publicApi/signature summary:** `fun AutoDriveListRow( title: String, modifier: Modifier = Modifier, supportingText: String? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, selected: Boolean = false, enabled: Boolean = true, onClick: (() -> Unit)? = null, )`
- **allowed slots/variants:** leading, trailing
- **state model:** parameters: enabled, selected, onClick; typed state: AutoDriveListRow
- **foundation tokens used/required:** AutoDriveBrand.Active.copy, AutoDriveOpacity.Tint, AutoDriveSpace.LG, AutoDriveSpace.MD, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-026 — AutoDriveSectionHeader
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 120)
- **publicApi/signature summary:** `fun AutoDriveSectionHeader( title: String, modifier: Modifier = Modifier, subtitle: String? = null, action: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** action
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveSectionHeader
- **foundation tokens used/required:** AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-027 — AutoDriveDivider
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 136)
- **publicApi/signature summary:** `fun AutoDriveDivider(modifier: Modifier = Modifier)`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: stateless/content-driven; typed state: AutoDriveDivider
- **foundation tokens used/required:** AutoDriveBorder.Thin, AutoDriveBorderColor.Default
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-028 — AutoDriveStatValue
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 141)
- **publicApi/signature summary:** `fun AutoDriveStatValue( value: String, modifier: Modifier = Modifier, size: AutoDriveStatSize = AutoDriveStatSize.Medium, accent: AutoDriveAccent? = null, unit: String? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: accent; typed state: AutoDriveAccent, AutoDriveStatSize, AutoDriveStatValue
- **foundation tokens used/required:** AutoDriveSpace.SM, AutoDriveStatXL, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt`, `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-029 — AutoDriveStatusIndicator
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 164)
- **publicApi/signature summary:** `fun AutoDriveStatusIndicator( tone: AutoDriveStatusTone, contentDescription: String, modifier: Modifier = Modifier, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: tone; typed state: AutoDriveStatusIndicator, AutoDriveStatusTone
- **foundation tokens used/required:** MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- **semantics responsibility:** Requires caller-provided contentDescription and owns interactive state/role through the underlying DS control; v65 verifies semantics quality.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-030 — AutoDriveStepIndicator
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 175)
- **publicApi/signature summary:** `fun AutoDriveStepIndicator( currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: currentStep, totalSteps; typed state: AutoDriveStepIndicator
- **foundation tokens used/required:** AutoDriveBorderColor.Default, AutoDriveBrand.Active, AutoDriveBrand.Active.copy, AutoDriveMotion.emphasized, AutoDriveOpacity.Medium, AutoDriveRadius.PillShape, AutoDriveSpace.SM
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt`, `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-CMP-031 — AutoDriveInstrumentNumber
- **kind:** COMPONENT
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt` (definition line 235)
- **publicApi/signature summary:** `fun AutoDriveInstrumentNumber( text: String, tone: AutoDriveInstrumentTone, modifier: Modifier = Modifier, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: tone; typed state: AutoDriveInstrumentNumber, AutoDriveInstrumentTone
- **foundation tokens used/required:** AutoDriveBrand.Active, AutoDriveBrand.Secondary, AutoDriveInstrument.Caution, AutoDriveInstrument.Empty, AutoDriveInstrument.Full, AutoDriveInstrument.Good, AutoDriveInstrument.Low, AutoDriveInstrumentTone.Active, AutoDriveInstrumentTone.Caution, AutoDriveInstrumentTone.Empty, AutoDriveInstrumentTone.Full, AutoDriveInstrumentTone.Good, AutoDriveInstrumentTone.Low, AutoDriveInstrumentTone.Secondary, AutoDriveSpace.SM
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** NONE
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-001 — ScreenHeader
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt` (definition line 15)
- **publicApi/signature summary:** `fun ScreenHeader( title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, context: (@Composable () -> Unit)? = null, titleContent: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** trailing, context, titleContent
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.LG, AutoDriveSpace.SM
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-002 — DashboardHero
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/dashboard/DashboardHero.kt` (definition line 20)
- **publicApi/signature summary:** `fun DashboardHero( modifier: Modifier = Modifier, accent: AutoDriveAccent = AutoDriveAccent.Primary, label: String? = null, heroContent: @Composable () -> Unit, supportingContent: (@Composable () -> Unit)? = null, action: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** heroContent, supportingContent, action
- **state model:** parameters: accent; typed state: AutoDriveAccent
- **foundation tokens used/required:** AutoDriveSpace.LG, AutoDriveText.Secondary
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-003 — MetricSummary
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/metrics/MetricSummary.kt` (definition line 25)
- **publicApi/signature summary:** `fun MetricSummary( items: List<MetricSummaryItem>, modifier: Modifier = Modifier, title: String? = null, onItemClick: ((String) -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.MD
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** NONE
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-004 — ConversationItem
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/conversation/ConversationItem.kt` (definition line 22)
- **publicApi/signature summary:** `fun ConversationItem( title: String, preview: String, timestamp: String, onClick: () -> Unit, modifier: Modifier = Modifier, unreadCount: Int = 0, identityName: String = title, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: onClick; typed state: NONE
- **foundation tokens used/required:** AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-005 — TransactionRow
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt` (definition line 25)
- **publicApi/signature summary:** `fun TransactionRow( title: String, amount: String, metadata: String, tone: AutoDriveStatusTone, modifier: Modifier = Modifier, statusLabel: String? = null, onClick: (() -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: tone, onClick; typed state: AutoDriveStatusTone
- **foundation tokens used/required:** AutoDriveStatusChip
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-006 — PendingRequestCard
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/finance/FinancePatterns.kt` (definition line 49)
- **publicApi/signature summary:** `fun PendingRequestCard( title: String, amount: String, metadata: String, statusLabel: String, modifier: Modifier = Modifier, loading: Boolean = false, primaryActionLabel: String? = null, onPrimaryAction: (() -> Unit)? = null, secondaryActionLabel: String? = null, onSecondaryAction: (() -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: loading; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveStatusChip, AutoDriveStatusTone.Warning
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-007 — SettingsGroup
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt` (definition line 63)
- **publicApi/signature summary:** `fun SettingsGroup( title: String, items: List<SettingsGroupItem>, modifier: Modifier = Modifier, onItemClick: (String) -> Unit = {}, headerAction: (@Composable () -> Unit)? = null, )`
- **allowed slots/variants:** headerAction
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-008 — SettingsRow
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt` (definition line 36)
- **publicApi/signature summary:** `fun SettingsRow( label: String, modifier: Modifier = Modifier, value: String? = null, variant: SettingsRowVariant = SettingsRowVariant.Value, enabled: Boolean = true, statusTone: AutoDriveStatusTone = AutoDriveStatusTone.Neutral, onClick: (() -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: enabled, variant, onClick; typed state: AutoDriveStatusTone, SettingsRowVariant
- **foundation tokens used/required:** AutoDriveStatus.Error, AutoDriveStatusChip, AutoDriveText.Disabled, AutoDriveText.Primary, AutoDriveText.Secondary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PARTIAL
- **known drift IDs:** DS59-SETTINGS-001
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v63

## DS-PAT-009 — ReportStatTile
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/reports/ReportStatTile.kt` (definition line 12)
- **publicApi/signature summary:** `fun ReportStatTile( label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, supportingText: String? = null, accent: AutoDriveAccent? = null, icon: ImageVector? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: accent, onClick; typed state: AutoDriveAccent
- **foundation tokens used/required:** MaterialTheme/typed DS dependencies; no direct foundation token reference isolated
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** NONE
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-010 — MediaActionGroup
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/media/MediaActionGroup.kt` (definition line 34)
- **publicApi/signature summary:** `fun MediaActionGroup( state: MediaActionState, modifier: Modifier = Modifier, mediaLabel: String? = null, recordingTime: String? = null, onCamera: () -> Unit, onGallery: () -> Unit, onStartVoice: () -> Unit, onStopVoice: () -> Unit, onRemoveMedia: () -> Unit, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: state; typed state: MediaActionState
- **foundation tokens used/required:** AutoDriveSpace.MD, AutoDriveSpace.SM, AutoDriveStatusIndicator, AutoDriveStatusTone.Error, AutoDriveText.Primary
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-011 — SearchResultsList
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/search/SearchResultsList.kt` (definition line 17)
- **publicApi/signature summary:** `fun <T> SearchResultsList( query: String, onQueryChange: (String) -> Unit, placeholder: String, state: SearchResultsState, items: List<T>, emptyTitle: String, emptyBody: String, modifier: Modifier = Modifier, loadingLabel: String? = null, errorTitle: String = emptyTitle, errorBody: String = emptyBody, itemContent: @Composable (T) -> Unit, )`
- **allowed slots/variants:** itemContent
- **state model:** parameters: state, query; typed state: SearchResultsState
- **foundation tokens used/required:** AutoDriveSpace.LG
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** NONE
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-012 — EmptyScreen
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt` (definition line 23)
- **publicApi/signature summary:** `fun EmptyScreen( title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Inbox, actionLabel: String? = null, onAction: (() -> Unit)? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.LG
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-013 — ErrorScreen
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt` (definition line 37)
- **publicApi/signature summary:** `fun ErrorScreen( title: String, body: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.LG
- **semantics responsibility:** Owns presentation of the interactive control and enabled/selected/loading semantics exposed by its typed API; caller owns business meaning and callback.
- **minimum touch-target responsibility:** Interactive target responsibility applies; v65 verifies >=48dp effective hit target.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-PAT-014 — LoadingScreen
- **kind:** PATTERN
- **ownerPath:** `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/state/StateScreens.kt` (definition line 50)
- **publicApi/signature summary:** `fun LoadingScreen( modifier: Modifier = Modifier, label: String? = null, )`
- **allowed slots/variants:** NONE; parameters/variants only
- **state model:** parameters: stateless/content-driven; typed state: NONE
- **foundation tokens used/required:** AutoDriveSpace.LG
- **semantics responsibility:** Owns visual/presentation semantics for supplied content; caller owns domain meaning. No ViewModel/navigation ownership allowed by V07.
- **minimum touch-target responsibility:** NOT_APPLICABLE unless caller places it inside an interactive parent.
- **RTL responsibility:** Arabic RTL dedicated preview exists in V1 preview suite; directional content must remain layout-direction aware and use AutoMirrored where directional icons are owned.
- **current production consumers:** `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`, `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt`, `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt`, `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **current V58 conformance:** PASS
- **known drift IDs:** NONE
- **reference docs:** `docs/design-system/03_COMPONENT_SPEC.md`, `docs/design-system/04_PATTERNS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md`
- **reference preview/test/screenshot:** V07 dedicated Arabic RTL preview coverage; implementation source above.
- **behavior preservation notes:** Presentation-only ownership; no navigation, ViewModel, repository, session, or data ownership may move into this contract.
- **future repair session:** v64 consumer adoption / v65 a11y / v66 closure

## DS-SCR-HOME-001 — HOME_V1
- **kind:** SCREEN
- **ownerPath:** `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt`
- **publicApi/signature summary:** screen-level Compose contract rooted at `HomeScreen` on route `home`.
- **allowed slots/variants:** current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- **state model:** competitionAvailability DISABLED hides competition card; LOCKED/ACTIVE retained
- **foundation tokens used/required:** consume frozen V1 components/patterns/foundations; no new tokens in v59.
- **semantics responsibility:** screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- **minimum touch-target responsibility:** all interactive descendants require v65 effective-target verification.
- **RTL responsibility:** Arabic/RTL layout behavior must be preserved; no navigation/business behavior may depend on layout direction.
- **current production consumers:** route `home` via NavigationGraphs/AppNavigation.
- **current V58 conformance:** DRIFT
- **known drift IDs:** DS59-HOME-001, DS59-HOME-002, DS59-HOME-003, DS59-HOME-004, DS59-HOME-005, DS59-HOME-006, DS59-HOME-007, DS59-HOME-008
- **intendedV1Contract:** ScreenHeader + DashboardHero + AutoDriveInstrumentNumber + Dashboard width contract while preserving current Home behavior.
- **currentV58Implementation:** Local HomeHeader and local hero/style island; first-name brand emphasis exists locally; required V1 composition adoption is incomplete.
- **knownDrift:** DS59-HOME-001, DS59-HOME-002, DS59-HOME-003, DS59-HOME-004, DS59-HOME-005, DS59-HOME-006, DS59-HOME-007, DS59-HOME-008
- **behaviorThatMustNotChange:** navigation callbacks; competition availability branching; pump interaction lifecycle; ON_RESUME Dynamo refresh; unread count presentation
- **visualReferenceStatus:** current runtime capture blocked; existing PNGs are LEGACY_VISUAL_REFERENCE only.
- **reference docs:** `docs/design-system/05_SCREEN_SPECS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md` plus current v58 source.
- **reference preview/test/screenshot:** current static code/tool evidence; historical PNG only where named, not current-source provenance.
- **future repair session:** v61

## DS-SCR-REPORTS-001 — REPORTS_V1
- **kind:** SCREEN
- **ownerPath:** `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt`
- **publicApi/signature summary:** screen-level Compose contract rooted at `ActivityLogScreen` on route `activity_log?filter={filter}`.
- **allowed slots/variants:** current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- **state model:** ReportsLoadState.LOADING; ReportsLoadState.ERROR; ReportsLoadState.CONTENT; competition ACTIVE branch
- **foundation tokens used/required:** consume frozen V1 components/patterns/foundations; no new tokens in v59.
- **semantics responsibility:** screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- **minimum touch-target responsibility:** all interactive descendants require v65 effective-target verification.
- **RTL responsibility:** Arabic/RTL layout behavior must be preserved; no navigation/business behavior may depend on layout direction.
- **current production consumers:** route `activity_log?filter={filter}` via NavigationGraphs/AppNavigation.
- **current V58 conformance:** DRIFT
- **known drift IDs:** DS59-REPORTS-001, DS59-REPORTS-002, DS59-REPORTS-003
- **intendedV1Contract:** ScreenHeader + DashboardHero + ReportStatTile + Dashboard max-width + narrow fallback while preserving report data/behavior.
- **currentV58Implementation:** ScreenHeader and DashboardHero are used; ReportStatTile and responsive width/fallback contracts are absent.
- **knownDrift:** DS59-REPORTS-001, DS59-REPORTS-002, DS59-REPORTS-003
- **behaviorThatMustNotChange:** report calculations/data source; retry behavior; financial/invoice/competition navigation
- **visualReferenceStatus:** current runtime capture blocked; existing PNGs are LEGACY_VISUAL_REFERENCE only.
- **reference docs:** `docs/design-system/05_SCREEN_SPECS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md` plus current v58 source.
- **reference preview/test/screenshot:** current static code/tool evidence; historical PNG only where named, not current-source provenance.
- **future repair session:** v62

## DS-SCR-SETTINGS-001 — SETTINGS_V1
- **kind:** SCREEN
- **ownerPath:** `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
- **publicApi/signature summary:** screen-level Compose contract rooted at `ProfileScreen` on route `profile`.
- **allowed slots/variants:** current route callbacks/state inputs only; no route argument or business-flow redesign in v59-v66.
- **state model:** WORKSHOP_OWNER guard; section-based editor; destructive sign-out confirmation
- **foundation tokens used/required:** consume frozen V1 components/patterns/foundations; no new tokens in v59.
- **semantics responsibility:** screen composition must preserve callback/business ownership while exposing accessible DS controls; cross-cut a11y closure is v65.
- **minimum touch-target responsibility:** all interactive descendants require v65 effective-target verification.
- **RTL responsibility:** Arabic/RTL layout behavior must be preserved; no navigation/business behavior may depend on layout direction.
- **current production consumers:** route `profile` via NavigationGraphs/AppNavigation.
- **current V58 conformance:** PARTIAL
- **known drift IDs:** DS59-SETTINGS-001
- **intendedV1Contract:** SettingsGroup/SettingsRow and DS inputs/sheets/dialogs with destructive styling correctly applied while preserving section-specific editing.
- **currentV58Implementation:** ProfileScreen consumes SettingsGroup/SettingsRow and DS form/sheet/dialog components; SettingsRow destructive titleColor is computed but not applied.
- **knownDrift:** DS59-SETTINGS-001
- **behaviorThatMustNotChange:** section-specific save APIs; IBAN text behavior; weekly target local preference; WORKSHOP_OWNER guard; sign-out confirmation
- **visualReferenceStatus:** current runtime capture blocked; existing PNGs are LEGACY_VISUAL_REFERENCE only.
- **reference docs:** `docs/design-system/05_SCREEN_SPECS.md`, `docs/design-system/DESIGN_SYSTEM_V1.md` plus current v58 source.
- **reference preview/test/screenshot:** current static code/tool evidence; historical PNG only where named, not current-source provenance.
- **future repair session:** v63
