package com.autodrive.app.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sessionReader: SessionReader,
    private val draftStore: RegistrationDraftStore
) : ViewModel() {

    var accountType: String = draftStore.accountType
    var fullName = draftStore.fullName
    var bankName = draftStore.bankName
    var bankAccount = draftStore.bankAccount
    var workshopName = draftStore.workshopName
    var specialty = draftStore.specialty
    var workersCount = draftStore.workersCount
    var address = draftStore.address

    val verifiedPhone: String
        get() = sessionReader.currentSession().phone.orEmpty()

    fun saveDraft() {
        draftStore.accountType = accountType
        draftStore.fullName = fullName.trim()
        // Identity invariant: profile phone must always be the phone proven by OTP.
        // Never trust an editable draft/UI value for this field.
        draftStore.phone = verifiedPhone.trim()
        draftStore.bankName = bankName.trim()
        draftStore.bankAccount = bankAccount.trim()
        draftStore.workshopName = workshopName.trim()
        draftStore.specialty = specialty.trim()
        draftStore.workersCount = workersCount.trim()
        draftStore.address = address.trim()
    }
}
