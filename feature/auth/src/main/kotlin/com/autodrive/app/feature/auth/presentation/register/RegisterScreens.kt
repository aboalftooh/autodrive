package com.autodrive.app.feature.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.WorkshopSpecialties
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveStepIndicator
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionOption
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton

// ═══════════════════════════════════════════════
// شاشة 5 — البيانات الأساسية
// ═══════════════════════════════════════════════
@Composable
fun BasicInfoScreen(
    accountType: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val saved by viewModel.saved.collectAsState()
    val isWorkshop = accountType == "WORKSHOP_OWNER"
    val step = if (isWorkshop) 1 else 1
    val totalSteps = if (isWorkshop) 3 else 2

    var fullName    by remember { mutableStateOf(viewModel.fullName) }
    var phone       by remember { mutableStateOf(viewModel.phone.ifBlank { viewModel.verifiedPhone }) }
    var bankName    by remember { mutableStateOf(viewModel.bankName) }
    var bankAccount by remember { mutableStateOf(viewModel.bankAccount) }
    var workshopName by remember { mutableStateOf(viewModel.workshopName) }
    var specialty    by remember { mutableStateOf(viewModel.specialty) }
    var address      by remember { mutableStateOf(viewModel.address) }
    var workers     by remember { mutableStateOf(viewModel.workersCount) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }



    val focusPhone       = remember { FocusRequester() }
    val focusWorkshop    = remember { FocusRequester() }
    val focusAddress     = remember { FocusRequester() }
    val focusBankName    = remember { FocusRequester() }
    val focusBankAccount = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoDriveIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "رجوع",
                onClick = onBack,
            )
            Spacer(Modifier.width(8.dp))
            AutoDriveStepIndicator(currentStep = step, totalSteps = totalSteps)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            if (isWorkshop) "استكمال بيانات الورشة" else "استكمال بيانات المسوّق",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary
        )
        Spacer(Modifier.height(4.dp))
        Text("تُدخل مرة واحدة فقط — يمكن تعديلها من الملف الشخصي",
            style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)

        Spacer(Modifier.height(32.dp))

        AutoDriveTextField(
            value         = fullName,
            onValueChange = { fullName = it; viewModel.fullName = it },
            label         = if (isWorkshop) "اسم صاحب الورشة" else "الاسم",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusPhone.requestFocus() })
        )
        Spacer(Modifier.height(16.dp))
        AutoDriveTextField(
            value           = phone,
            onValueChange   = { phone = it; viewModel.phone = it },
            label           = "رقم الهاتف",
            modifier        = Modifier.focusRequester(focusPhone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                if (isWorkshop) focusWorkshop.requestFocus() else focusBankName.requestFocus()
            })
        )
        Spacer(Modifier.height(16.dp))
        if (isWorkshop) {
            AutoDriveTextField(
                value           = workshopName,
                onValueChange   = { workshopName = it; viewModel.workshopName = it },
                label           = "اسم الورشة",
                modifier        = Modifier.focusRequester(focusWorkshop),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusAddress.requestFocus() })
            )
            Spacer(Modifier.height(16.dp))
            AutoDriveSelectionField(
                selected = specialty.takeIf(String::isNotBlank)?.let { AutoDriveSelectionOption(it, it) },
                options = WorkshopSpecialties.labels.map { AutoDriveSelectionOption(it, it) },
                label = "التخصص",
                onSelected = { option -> specialty = option.label; viewModel.specialty = option.label },
            )
            Spacer(Modifier.height(16.dp))
            AutoDriveTextField(
                value = workers,
                onValueChange = { workers = it; viewModel.workersCount = it },
                label = "عدد العمال",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(16.dp))
            AutoDriveTextField(
                value           = address,
                onValueChange   = { address = it; viewModel.address = it },
                label           = "العنوان",
                modifier        = Modifier.focusRequester(focusAddress),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusBankName.requestFocus() })
            )
            Spacer(Modifier.height(16.dp))
        }
        AutoDriveTextField(
            value           = bankName,
            onValueChange   = { bankName = it; viewModel.bankName = it },
            label           = "اسم البنك",
            modifier        = Modifier.focusRequester(focusBankName),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusBankAccount.requestFocus() })
        )
        Spacer(Modifier.height(16.dp))
        AutoDriveTextField(
            value           = bankAccount,
            onValueChange   = { bankAccount = it; viewModel.bankAccount = it },
            label           = "رقم الحساب البنكي",
            modifier        = Modifier.focusRequester(focusBankAccount),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* إغلاق الكيبورد */ })
        )

        errorMsg?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(40.dp))

        val canProceed = fullName.isNotBlank() &&
            phone.isNotBlank() &&
            bankName.isNotBlank() &&
            bankAccount.isNotBlank() &&
            (!isWorkshop || (workshopName.isNotBlank() && specialty.isNotBlank() && address.isNotBlank()))

        if (isWorkshop) {
            AutoDrivePrimaryButton(
                text = "التالي ←",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.accountType = accountType
                    viewModel.saveDraft()
                    onContinue()
                },
                enabled = canProceed,
            )
        } else {
            AutoDrivePrimaryButton(
                text = "التالي ←",
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.accountType = accountType; viewModel.saveDraft(); onContinue() },
                enabled = canProceed
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════
// شاشة 6 — بيانات الورشة
// ═══════════════════════════════════════════════
@Composable
fun WorkshopInfoScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val saved by viewModel.saved.collectAsState()

    var workshopName by remember { mutableStateOf("") }
    var specialty    by remember { mutableStateOf("") }
    var workers      by remember { mutableStateOf("") }
    var address      by remember { mutableStateOf("") }
    var errorMsg     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saved) {
        if (saved is Result.Success) onSaved()
        if (saved is Result.Error) errorMsg = (saved as Result.Error).message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoDriveIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "رجوع",
                onClick = onBack,
            )
            Spacer(Modifier.width(8.dp))
            AutoDriveStepIndicator(currentStep = 2, totalSteps = 3)
        }

        Spacer(Modifier.height(32.dp))

        Text("بيانات الورشة", style = MaterialTheme.typography.headlineLarge, color = AutoDriveText.Primary)
        Spacer(Modifier.height(4.dp))
        Text("تُدخل مرة واحدة فقط — يمكن تعديلها لاحقاً",
            style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)

        Spacer(Modifier.height(32.dp))

        AutoDriveTextField(value = workshopName, onValueChange = { workshopName = it; viewModel.workshopName = it }, label = "اسم الورشة")
        Spacer(Modifier.height(16.dp))
        AutoDriveSelectionField(
            selected = specialty.takeIf(String::isNotBlank)?.let { AutoDriveSelectionOption(it, it) },
            options = WorkshopSpecialties.labels.map { AutoDriveSelectionOption(it, it) },
            label = "التخصص",
            onSelected = { option -> specialty = option.label; viewModel.specialty = option.label },
        )
        Spacer(Modifier.height(16.dp))
        AutoDriveTextField(value = workers, onValueChange = { workers = it; viewModel.workersCount = it }, label = "عدد العمال")
        Spacer(Modifier.height(16.dp))
        AutoDriveTextField(value = address, onValueChange = { address = it; viewModel.address = it }, label = "العنوان (المدينة — الحي)")

        errorMsg?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(40.dp))

        AutoDrivePrimaryButton(
            text = "حفظ والدخول ←",
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.saveWorkshopOwner() },
            enabled = workshopName.isNotBlank(),
            loading = saved is Result.Loading
        )
        Spacer(Modifier.height(24.dp))
    }
}
