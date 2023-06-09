package io.github.nfdz.cryptool.shared.gatekeeper.viewModel

import io.github.aakira.napier.Napier
import io.github.nfdz.cryptool.shared.core.export.ExportData
import io.github.nfdz.cryptool.shared.core.import.ImportData
import io.github.nfdz.cryptool.shared.gatekeeper.entity.TutorialInformation
import io.github.nfdz.cryptool.shared.gatekeeper.repository.GatekeeperRepository
import io.github.nfdz.cryptool.shared.platform.biometric.BiometricContext
import io.github.nfdz.cryptool.shared.platform.biometric.TooManyAttemptsException
import io.github.nfdz.cryptool.shared.platform.localization.LocalizedError

class GatekeeperViewModelImpl(
    private val repository: GatekeeperRepository,
    private val exportData: ExportData,
    private val importData: ImportData,
    private val localizedError: LocalizedError,
) : GatekeeperViewModelBase() {

    override val tag: String
        get() = "GatekeeperViewModel"

    override val initialState: GatekeeperState = refreshState()

    private var loadingAccess = false

    override suspend fun processAction(action: GatekeeperAction) {
        runCatching {
            when (action) {
                is GatekeeperAction.Create -> create(action.code, action.biometricEnabled)
                is GatekeeperAction.AccessWithCode -> accessWithCode(action.code)
                is GatekeeperAction.AccessWithBiometric -> accessWithBiometric(action.biometricContext)
                GatekeeperAction.Delete -> delete()
                is GatekeeperAction.AcknowledgeWelcome -> acknowledgeWelcome(action.welcomeTutorial)
                is GatekeeperAction.AcknowledgeLegacyMigration -> acknowledgeLegacyMigration(action)
                is GatekeeperAction.ChangeAccessCode -> changeAccessCode(action)
                is GatekeeperAction.ChangeBiometricAccess -> changeBiometricAccess(action)
                GatekeeperAction.CheckAccess -> checkAccess()
                GatekeeperAction.PushAccessValidity -> pushAccessValidity()
            }
        }.onFailure {
            Napier.e(tag = tag, message = "processAction: $action", throwable = it)
            emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperUnexpected))
        }
    }

    private fun refreshState(): GatekeeperState = GatekeeperState(
        isOpen = repository.isOpen(),
        hasCode = repository.hasCode(),
        canUseBiometricAccess = repository.canUseBiometricAccess(),
        welcome = repository.getWelcomeInformation(),
        canMigrateFromLegacy = repository.canMigrateFromLegacy(),
        loadingAccess = loadingAccess,
    )

    private suspend fun create(code: String, biometricEnabled: Boolean) {
        repository.setNewCode(code, biometricEnabled)
        emitNewState(refreshState())
    }

    private suspend fun accessWithCode(code: String) {
        loadingAccess = true
        emitNewState(refreshState())
        val validCode = runCatching {
            repository.validateCode(code)
        }.getOrNull()
        if (validCode != true) emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperInvalidAccessCode))

        loadingAccess = false
        emitNewState(refreshState())
    }

    private suspend fun accessWithBiometric(biometricContext: BiometricContext) {
        loadingAccess = true
        emitNewState(refreshState())
        val result = runCatching {
            repository.biometricAccess(biometricContext)
        }
        if (result.getOrNull() != true) {
            if (result.exceptionOrNull() is TooManyAttemptsException) {
                emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperBiometricTooManyAttempts))
            } else {
                emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperInvalidAccessCode))
            }
        }

        loadingAccess = false
        emitNewState(refreshState())
    }

    private fun delete() {
        repository.reset()
        emitNewState(refreshState())
    }

    private fun acknowledgeWelcome(welcomeTutorial: TutorialInformation?) {
        repository.acknowledgeWelcome(welcomeTutorial)
        emitNewState(refreshState())
    }

    private suspend fun acknowledgeLegacyMigration(action: GatekeeperAction.AcknowledgeLegacyMigration) {
        repository.acknowledgeWelcome(action.welcomeTutorial)
        if (action.migrateData) {
            repository.launchMigration()
        }
        emitNewState(refreshState())
    }

    private suspend fun changeAccessCode(action: GatekeeperAction.ChangeAccessCode) {
        val validCode = repository.validateCode(action.oldCode)
        if (!validCode) {
            emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperInvalidOldAccessCode))
            return
        }

        loadingAccess = true
        emitNewState(refreshState())

        val data = exportData.prepareDataDto()

        repository.reset()

        runCatching {
            repository.setNewCode(action.newCode, false)
            importData.consumeDataDto(data)
            emitSideEffect(GatekeeperEffect.ChangedCode)
        }.onFailure {
            emitSideEffect(GatekeeperEffect.Error(localizedError.gatekeeperChangeAccessCode))
        }

        loadingAccess = false
        emitNewState(refreshState())
    }

    private fun changeBiometricAccess(action: GatekeeperAction.ChangeBiometricAccess) {
        repository.setBiometricAccess(action.biometricEnabled)
        emitNewState(refreshState())
    }

    private fun checkAccess() {
        val anyChange = repository.checkAccessChange()
        if (anyChange) {
            emitNewState(refreshState())
        }
    }

    private fun pushAccessValidity() {
        repository.pushAccessValidity()
    }

}