package io.github.nfdz.cryptool.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import io.github.nfdz.cryptool.shared.encryption.viewModel.EncryptionEffect
import io.github.nfdz.cryptool.shared.encryption.viewModel.EncryptionViewModel
import io.github.nfdz.cryptool.shared.gatekeeper.viewModel.GatekeeperEffect
import io.github.nfdz.cryptool.shared.gatekeeper.viewModel.GatekeeperViewModel
import io.github.nfdz.cryptool.shared.message.viewModel.MessageEffect
import io.github.nfdz.cryptool.shared.message.viewModel.MessageViewModel
import io.github.nfdz.cryptool.ui.extension.showSnackbarAsync
import org.koin.core.context.GlobalContext

@Composable
fun AppMessagesEffect(snackbar: SnackbarHostState) {
    EncryptionSideEffect(snackbar)
    MessageSideEffect(snackbar)
    GatekeeperSideEffect(snackbar)
}

@Composable
private fun EncryptionSideEffect(
    snackbar: SnackbarHostState,
    encryptionViewModel: EncryptionViewModel = GlobalContext.get().get()
) {
    val context = LocalContext.current
    val effect = encryptionViewModel.observeSideEffect().collectAsState(null).value ?: return
    LaunchedEffect(effect) {
        when (effect) {
            is EncryptionEffect.Error -> {
                val retryAction = effect.retry
                if (retryAction != null) {
                    val result =
                        snackbar.showSnackbar(effect.message, actionLabel = context.getString(R.string.snackbar_retry))
                    if (result == SnackbarResult.ActionPerformed) {
                        encryptionViewModel.dispatch(retryAction)
                    }
                } else {
                    snackbar.showSnackbarAsync(effect.message)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun MessageSideEffect(
    snackbar: SnackbarHostState,
    messageViewModel: MessageViewModel = GlobalContext.get().get()
) {
    val context = LocalContext.current
    val effect = messageViewModel.observeSideEffect().collectAsState(null).value ?: return
    LaunchedEffect(effect) {
        when (effect) {
            is MessageEffect.Event -> snackbar.showSnackbarAsync(effect.message)
            is MessageEffect.Error -> {
                val retryAction = effect.retry
                if (retryAction != null) {
                    val result =
                        snackbar.showSnackbar(effect.message, actionLabel = context.getString(R.string.snackbar_retry))
                    if (result == SnackbarResult.ActionPerformed) {
                        messageViewModel.dispatch(retryAction)
                    }
                } else {
                    snackbar.showSnackbarAsync(effect.message)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun GatekeeperSideEffect(
    snackbar: SnackbarHostState,
    gatekeeperViewModel: GatekeeperViewModel = GlobalContext.get().get()
) {
    val effect = gatekeeperViewModel.observeSideEffect().collectAsState(null).value ?: return
    LaunchedEffect(effect) {
        when (effect) {
            is GatekeeperEffect.Error -> snackbar.showSnackbarAsync(effect.message)
            else -> Unit
        }
    }
}