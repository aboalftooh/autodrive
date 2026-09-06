package com.autodrive.app.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.data.AutoDriveAvatar
import com.autodrive.app.core.designsystem.components.data.AutoDriveAvatarSize
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBottomSheet
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialogTone
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveSnackbarContent
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveNumericField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionOption
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.settings.SettingsGroup
import com.autodrive.app.core.designsystem.patterns.settings.SettingsGroupItem
import com.autodrive.app.core.designsystem.patterns.settings.SettingsRow
import com.autodrive.app.core.designsystem.patterns.settings.SettingsRowVariant
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.model.account.WorkshopSpecialties
import com.autodrive.app.core.model.money.Money
import java.util.Locale

@Composable
fun ProfileScreen(
    onNavigateHome: () -> Unit,
    onNavigateRecent: () -> Unit,
    onNavigateAchievements: () -> Unit,
    onSignedOut: () -> Unit,
    onAddClick: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    onNavigatePrivacy: () -> Unit = {},
    onNavigateFaq: () -> Unit = {},
    unreadMessages: Int = 0,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    if (state.showSignOutConfirmDialog) {
        AutoDriveDialog(
            title = "تسجيل الخروج",
            body = "هل أنت متأكد من تسجيل الخروج؟",
            tone = AutoDriveDialogTone.Destructive,
            onDismissRequest = viewModel::dismissSignOutDialog,
            actions = {
                AutoDriveTextButton("إلغاء", viewModel::dismissSignOutDialog)
                AutoDriveTextButton(
                    "تسجيل الخروج",
                    viewModel::signOut,
                    tone = AutoDriveTextButtonTone.Destructive,
                )
            },
        )
    }

    state.user?.let { user ->
        when (state.editingSection) {
            ProfileEditSection.ACCOUNT -> AccountEditSheet(
                user = user,
                isSaving = state.isSaving,
                error = state.saveError,
                onDismiss = { if (!state.isSaving) viewModel.cancelEditing() },
                onSave = viewModel::saveAccount,
            )

            ProfileEditSection.PAYOUT -> PayoutEditSheet(
                user = user,
                isSaving = state.isSaving,
                error = state.saveError,
                onDismiss = { if (!state.isSaving) viewModel.cancelEditing() },
                onSave = viewModel::savePayout,
            )

            ProfileEditSection.WORKSHOP -> if (user.accountType == AccountType.WORKSHOP_OWNER) {
                WorkshopEditSheet(
                    user = user,
                    isSaving = state.isSaving,
                    error = state.saveError,
                    onDismiss = { if (!state.isSaving) viewModel.cancelEditing() },
                    onSave = viewModel::saveWorkshop,
                )
            }

            ProfileEditSection.WEEKLY_TARGET -> WeeklyTargetSheet(
                target = state.weeklyTarget,
                onDismiss = viewModel::cancelEditing,
                onChange = viewModel::setWeeklyTarget,
            )

            null -> Unit
        }
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        bottomBar = {
            AutoDriveBottomNavigation(
                items = profileRootItems(unreadMessages),
                selectedItemId = "settings",
                onItemClick = { item ->
                    when (item.id) {
                        "home" -> onNavigateHome()
                        "messages" -> onNavigateRecent()
                        "achievements" -> onNavigateAchievements()
                    }
                },
                centerAction = {
                    AutoDriveFab(
                        onClick = onAddClick,
                        contentDescription = "محادثة جديدة",
                        icon = Icons.Rounded.Add,
                    )
                },
            )
        },
        snackbarHost = {
            state.successMessage?.let {
                AutoDriveSnackbarContent(it, Modifier.padding(AutoDriveSpace.LG))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AutoDriveContentWidth.Readable)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.X2L),
            ) {
                ScreenHeader(title = "الإعدادات")

                val user = state.user
                if (user == null) {
                    LoadingScreen(
                        label = "جاري تحميل بيانات الحساب",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    IdentityBlock(user)

                    SettingsGroup(
                        title = "الحساب الشخصي",
                        items = listOf(
                            SettingsGroupItem("name", "الاسم", user.fullName),
                            SettingsGroupItem("phone", "الهاتف", user.phone),
                        ),
                        headerAction = {
                            AutoDriveTextButton(
                                text = "تعديل",
                                onClick = { viewModel.startEditing(ProfileEditSection.ACCOUNT) },
                            )
                        },
                        modifier = Modifier.padding(horizontal = AutoDriveSpace.LG),
                    )

                    SettingsGroup(
                        title = "بيانات استلام العمولة",
                        items = listOf(
                            SettingsGroupItem("bank", "البنك", user.bankName ?: "—"),
                            SettingsGroupItem("account", "الحساب / IBAN", user.bankAccount ?: "—"),
                        ),
                        headerAction = {
                            AutoDriveTextButton(
                                text = "تعديل",
                                onClick = { viewModel.startEditing(ProfileEditSection.PAYOUT) },
                            )
                        },
                        modifier = Modifier.padding(horizontal = AutoDriveSpace.LG),
                    )

                    if (user.accountType == AccountType.WORKSHOP_OWNER) {
                        SettingsGroup(
                            title = "بيانات الورشة",
                            items = listOf(
                                SettingsGroupItem("workshop", "اسم الورشة", user.workshopName ?: "—"),
                                SettingsGroupItem("specialty", "التخصص", user.specialty ?: "—"),
                                SettingsGroupItem("workers", "عدد العمال", user.workersCount?.toString() ?: "—"),
                                SettingsGroupItem("address", "العنوان", user.address ?: "—"),
                            ),
                            headerAction = {
                                AutoDriveTextButton(
                                    text = "تعديل",
                                    onClick = { viewModel.startEditing(ProfileEditSection.WORKSHOP) },
                                )
                            },
                            modifier = Modifier.padding(horizontal = AutoDriveSpace.LG),
                        )
                    }

                    SettingsGroup(
                        title = "الأهداف والتخصيص",
                        items = listOf(
                            SettingsGroupItem(
                                id = "weekly_target",
                                label = "الهدف الأسبوعي",
                                value = formatMoney(state.weeklyTarget),
                                variant = SettingsRowVariant.Editable,
                            )
                        ),
                        onItemClick = { id ->
                            if (id == "weekly_target") {
                                viewModel.startEditing(ProfileEditSection.WEEKLY_TARGET)
                            }
                        },
                        modifier = Modifier.padding(horizontal = AutoDriveSpace.LG),
                    )

                    SettingsGroup(
                        title = "المساعدة والمعلومات",
                        items = listOf(
                            SettingsGroupItem("about", "عن التطبيق", "AutoDrive v1.0.0", SettingsRowVariant.Navigation),
                            SettingsGroupItem("privacy", "سياسة الخصوصية", variant = SettingsRowVariant.Navigation),
                            SettingsGroupItem("faq", "الأسئلة الشائعة", variant = SettingsRowVariant.Navigation),
                        ),
                        onItemClick = { id ->
                            when (id) {
                                "about" -> onNavigateAbout()
                                "privacy" -> onNavigatePrivacy()
                                "faq" -> onNavigateFaq()
                            }
                        },
                        modifier = Modifier.padding(horizontal = AutoDriveSpace.LG),
                    )

                    SettingsRow(
                        label = "تسجيل الخروج",
                        variant = SettingsRowVariant.Destructive,
                        onClick = viewModel::requestSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AutoDriveSpace.LG)
                            .padding(bottom = AutoDriveSpace.X2L),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentityBlock(user: AutoDriveUser) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AutoDriveSpace.LG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
    ) {
        AutoDriveAvatar(
            name = user.fullName,
            size = AutoDriveAvatarSize.Hero,
            accent = AutoDriveAccent.Active,
        )
        Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.headlineSmall,
                color = AutoDriveText.Primary,
            )
            FormatUtils.formatJoinDate(user.createdAt).takeIf(String::isNotEmpty)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDriveText.Secondary,
                )
            }
        }
    }
}

@Composable
private fun AccountEditSheet(
    user: AutoDriveUser,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var fullName by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.fullName) }
    var phone by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.phone) }
    val focusManager = LocalFocusManager.current

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "الحساب الشخصي",
    ) {
        AutoDriveTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "الاسم الكامل",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !isSaving,
        )
        AutoDriveTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "الهاتف",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = !isSaving,
        )
        SaveError(error)
        AutoDrivePrimaryButton(
            text = "حفظ",
            onClick = { onSave(fullName, phone) },
            enabled = !isSaving,
            loading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PayoutEditSheet(
    user: AutoDriveUser,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var bankName by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.bankName ?: "") }
    var bankAccount by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.bankAccount ?: "") }
    val focusManager = LocalFocusManager.current

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "بيانات استلام العمولة",
    ) {
        AutoDriveTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = "اسم البنك",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !isSaving,
        )
        AutoDriveTextField(
            value = bankAccount,
            onValueChange = { bankAccount = it },
            label = "رقم الحساب / IBAN",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = !isSaving,
        )
        SaveError(error)
        AutoDrivePrimaryButton(
            text = "حفظ",
            onClick = { onSave(bankName, bankAccount) },
            enabled = !isSaving,
            loading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WorkshopEditSheet(
    user: AutoDriveUser,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var workshopName by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.workshopName ?: "") }
    var specialty by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.specialty ?: "") }
    var workersCount by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.workersCount?.toString() ?: "") }
    var address by rememberSaveable(user.userId, user.id) { androidx.compose.runtime.mutableStateOf(user.address ?: "") }
    val focusManager = LocalFocusManager.current
    val options = WorkshopSpecialties.labels.map { AutoDriveSelectionOption(it, it) }
    val selected = options.firstOrNull { it.label == specialty }

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "بيانات الورشة",
    ) {
        AutoDriveTextField(
            value = workshopName,
            onValueChange = { workshopName = it },
            label = "اسم الورشة",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !isSaving,
        )
        AutoDriveSelectionField(
            selected = selected,
            options = options,
            label = "التخصص",
            onSelected = { specialty = it.label },
            enabled = !isSaving,
        )
        AutoDriveNumericField(
            value = workersCount,
            onValueChange = { workersCount = it },
            label = "عدد العمال",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !isSaving,
        )
        AutoDriveTextField(
            value = address,
            onValueChange = { address = it },
            label = "العنوان",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = !isSaving,
        )
        SaveError(error)
        AutoDrivePrimaryButton(
            text = "حفظ",
            onClick = { onSave(workshopName, specialty, workersCount, address) },
            enabled = !isSaving,
            loading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeeklyTargetSheet(
    target: Money,
    onDismiss: () -> Unit,
    onChange: (Money) -> Unit,
) {
    val step = Money.of(50_000L)
    val minimum = Money.of(100_000L)
    val maximum = Money.of(5_000_000L)

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "الهدف الأسبوعي",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AutoDrivePrimaryButton(
                text = "−",
                onClick = { onChange(target - step) },
                enabled = target > minimum,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMoney(target),
                style = MaterialTheme.typography.titleLarge,
                color = AutoDriveText.Primary,
                modifier = Modifier.weight(2f),
            )
            AutoDrivePrimaryButton(
                text = "+",
                onClick = { onChange(target + step) },
                enabled = target < maximum,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "هدف شخصي لعرض تقدمك في الشاشة الرئيسية، ولا يؤثر على ترتيب المسابقة.",
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary,
        )
    }
}

@Composable
private fun SaveError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun formatMoney(value: Money): String =
    String.format(Locale.US, "%,.0f", value.amount)

private fun profileRootItems(unreadMessages: Int) = listOf(
    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),
    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),
    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),
)
