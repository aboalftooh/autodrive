package com.autodrive.app.feature.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveStepIndicator
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSelectionOption
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.model.account.WorkshopSpecialties

@Composable
fun BasicInfoScreen(
    accountType: String,
    onSubmitted: () -> Unit,
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val action by viewModel.action.collectAsState()
    val isWorkshop = accountType == "WORKSHOP_OWNER"
    val totalSteps = if (isWorkshop) 3 else 2

    var fullName by remember { mutableStateOf(viewModel.fullName) }
    val phone = viewModel.registrationPhone
    var bankName by remember { mutableStateOf(viewModel.bankName) }
    var bankAccount by remember { mutableStateOf(viewModel.bankAccount) }
    var workshopName by remember { mutableStateOf(viewModel.workshopName) }
    var specialty by remember { mutableStateOf(viewModel.specialty) }
    var address by remember { mutableStateOf(viewModel.address) }
    var workers by remember { mutableStateOf(viewModel.workersCount) }

    LaunchedEffect(accountType) {
        viewModel.accountType = accountType
    }
    LaunchedEffect(action) {
        when (action) {
            is RegistrationActionState.Submitted -> onSubmitted()
            RegistrationActionState.Completed -> onCompleted()
            else -> Unit
        }
    }

    val focusWorkshop = remember { FocusRequester() }
    val focusAddress = remember { FocusRequester() }
    val focusBankName = remember { FocusRequester() }
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
            AutoDriveStepIndicator(currentStep = 1, totalSteps = totalSteps)
        }

        Spacer(Modifier.height(32.dp))
        Text(
            if (isWorkshop) "بيانات الورشة" else "بيانات المسوّق",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (viewModel.isPhoneVerified) "أكمل بيانات حسابك" else "أدخل البيانات لإرسال طلب الانضمام",
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary
        )

        Spacer(Modifier.height(32.dp))
        AutoDriveTextField(
            value = fullName,
            onValueChange = { fullName = it; viewModel.fullName = it },
            label = if (isWorkshop) "اسم صاحب الورشة" else "الاسم",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                if (isWorkshop) focusWorkshop.requestFocus() else focusBankName.requestFocus()
            })
        )
        Spacer(Modifier.height(16.dp))

        AutoDriveTextField(
            value = phone,
            onValueChange = {},
            label = if (viewModel.isPhoneVerified) "رقم الهاتف الموثق" else "رقم الهاتف",
            supportingText = if (viewModel.isPhoneVerified) {
                "تم التحقق منه بواسطة OTP ولا يمكن تغييره من الملف الشخصي"
            } else {
                "سيتم التحقق منه بواسطة OTP بعد موافقة الإدارة"
            },
            readOnly = true,
        )
        Spacer(Modifier.height(16.dp))

        if (isWorkshop) {
            AutoDriveTextField(
                value = workshopName,
                onValueChange = { workshopName = it; viewModel.workshopName = it },
                label = "اسم الورشة",
                modifier = Modifier.focusRequester(focusWorkshop),
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
                value = address,
                onValueChange = { address = it; viewModel.address = it },
                label = "العنوان",
                modifier = Modifier.focusRequester(focusAddress),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusBankName.requestFocus() })
            )
            Spacer(Modifier.height(16.dp))
        }

        AutoDriveTextField(
            value = bankName,
            onValueChange = { bankName = it; viewModel.bankName = it },
            label = "اسم البنك",
            modifier = Modifier.focusRequester(focusBankName),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusBankAccount.requestFocus() })
        )
        Spacer(Modifier.height(16.dp))
        AutoDriveTextField(
            value = bankAccount,
            onValueChange = { bankAccount = it; viewModel.bankAccount = it },
            label = "رقم الحساب البنكي",
            modifier = Modifier.focusRequester(focusBankAccount),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )

        if (action is RegistrationActionState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = (action as RegistrationActionState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(40.dp))
        val canProceed = fullName.isNotBlank() &&
            phone.isNotBlank() &&
            bankName.isNotBlank() &&
            bankAccount.isNotBlank() &&
            (!isWorkshop || (workshopName.isNotBlank() && specialty.isNotBlank() && address.isNotBlank()))

        AutoDrivePrimaryButton(
            text = if (viewModel.isPhoneVerified) "إكمال التسجيل" else "إرسال طلب الانضمام",
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::submitOrComplete,
            enabled = canProceed && action !is RegistrationActionState.Loading,
            loading = action is RegistrationActionState.Loading,
        )
        Spacer(Modifier.height(24.dp))
    }
}
