package io.github.nfdz.cryptool.ui.gatekeeper

import androidx.compose.material3.SnackbarHostState
import dev.testify.ComposableScreenshotRule
import dev.testify.annotation.ScreenshotInstrumentation
import io.github.nfdz.cryptool.shared.gatekeeper.viewModel.EmptyGatekeeperViewModel
import io.github.nfdz.cryptool.ui.DarkColorScheme
import io.github.nfdz.cryptool.ui.LightColorScheme
import io.github.nfdz.cryptool.ui.test.TestEntry
import org.junit.Rule
import org.junit.Test

class GatekeeperScreenScreenshotTest {

    @get:Rule
    val rule = ComposableScreenshotRule()

    @ScreenshotInstrumentation
    @Test
    fun lightAskCode() {
        rule.setCompose {
            TestEntry(colorScheme = LightColorScheme) {
                GatekeeperScreenContent(
                    snackbar = SnackbarHostState(),
                    supportAdvancedFeatures = true,
                    activity = null,
                    viewModel = EmptyGatekeeperViewModel,
                    hasCode = true,
                    canUseBiometricAccess = false,
                    loadingAccess = false,
                )
            }
        }.assertSame()
    }

    @ScreenshotInstrumentation
    @Test
    fun darkAskCode() {
        rule.setCompose {
            TestEntry(colorScheme = DarkColorScheme) {
                GatekeeperScreenContent(
                    snackbar = SnackbarHostState(),
                    supportAdvancedFeatures = true,
                    activity = null,
                    viewModel = EmptyGatekeeperViewModel,
                    hasCode = true,
                    canUseBiometricAccess = false,
                    loadingAccess = false,
                )
            }
        }.assertSame()
    }

    @ScreenshotInstrumentation
    @Test
    fun lightCreateCode() {
        rule.setCompose {
            TestEntry(colorScheme = LightColorScheme) {
                GatekeeperScreenContent(
                    snackbar = SnackbarHostState(),
                    supportAdvancedFeatures = true,
                    activity = null,
                    viewModel = EmptyGatekeeperViewModel,
                    hasCode = false,
                    canUseBiometricAccess = false,
                    loadingAccess = false,
                )
            }
        }.assertSame()
    }

    @ScreenshotInstrumentation
    @Test
    fun darkCreateCode() {
        rule.setCompose {
            TestEntry(colorScheme = DarkColorScheme) {
                GatekeeperScreenContent(
                    snackbar = SnackbarHostState(),
                    supportAdvancedFeatures = true,
                    activity = null,
                    viewModel = EmptyGatekeeperViewModel,
                    hasCode = false,
                    canUseBiometricAccess = false,
                    loadingAccess = false,
                )
            }
        }.assertSame()
    }
}