package io.github.nfdz.cryptool.ui.encryption

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.nfdz.cryptool.shared.core.constant.AppUrl
import io.github.nfdz.cryptool.shared.encryption.entity.MessageSource
import io.github.nfdz.cryptool.ui.*
import io.github.nfdz.cryptool.ui.R

@Composable
@Preview
private fun SourcePickerPreview() {
    AppTheme {
        SourcePicker(router = EmptyRouter) {}
    }
}

@Composable
fun SourcePicker(modifier: Modifier = Modifier, router: Router, onPick: (MessageSource) -> Unit) {
    var showSmsDialog by remember { mutableStateOf(false) }
    if (showSmsDialog) {
        SmsSourceDialog { source ->
            showSmsDialog = false
            if (source != null) {
                onPick(source)
            }
        }
    }
    var showFileDialog by remember { mutableStateOf(false) }
    if (showFileDialog) {
        FileSourceDialog { source ->
            showFileDialog = false
            if (source != null) {
                onPick(source)
            }
        }
    }
    var showLanDialog by remember { mutableStateOf(false) }
    if (showLanDialog) {
        LanSourceDialog { source ->
            showLanDialog = false
            if (source != null) {
                onPick(source)
            }
        }
    }
    LanSourceEffect()
    val sources = listOf(
        SourceOptionEntry(
            title = stringResource(R.string.encryption_source_manual_title),
            description = stringResource(R.string.encryption_source_manual_description),
            icon = Icons.Filled.TouchApp,
        ) {
            onPick(MessageSource.Manual)
        },
        SourceOptionEntry(
            title = stringResource(R.string.encryption_source_lan_title),
            description = stringResource(R.string.encryption_source_lan_description),
            icon = Icons.Filled.Lan,
            disabled = router.supportAdvancedFeatures().not()
        ) {
            if (router.supportAdvancedFeatures().not()) return@SourceOptionEntry
            showLanDialog = true
        },
        SourceOptionEntry(
            title = stringResource(R.string.encryption_source_file_title),
            description = stringResource(R.string.encryption_source_file_description),
            icon = Icons.Filled.Task,
            disabled = router.supportAdvancedFeatures().not()
        ) {
            if (router.supportAdvancedFeatures().not()) return@SourceOptionEntry
            showFileDialog = true
        },
        SourceOptionEntry(
            title = stringResource(R.string.encryption_source_sms_title),
            description = stringResource(R.string.encryption_source_sms_description),
            icon = Icons.Filled.Message,
            disabled = router.supportAdvancedFeatures().not() || BuildConfig.SMS_FEATURE.not(),
            disabledReason = if (BuildConfig.SMS_FEATURE.not()) stringResource(R.string.encryption_source_sms_not_available) else null
        ) {
            if (BuildConfig.SMS_FEATURE) {
                if (router.supportAdvancedFeatures().not()) return@SourceOptionEntry
                showSmsDialog = true
            } else {
                router.navigateToUrl(AppUrl.googlePlayLimitation)
            }
        },
    )
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            SourcePickerGrid(modifier, sources)
        }
        else -> {
            SourcePickerColumn(modifier, sources)
        }
    }
}

@Composable
private fun SourcePickerColumn(modifier: Modifier, sources: List<SourceOptionEntry>) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sources.forEach {
            SourceOption(
                modifier = Modifier.weight(1f),
                entry = it,
            )
        }
    }
}

@Composable
private fun SourcePickerGrid(modifier: Modifier, sources: List<SourceOptionEntry>) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val sourcesRows = sources.chunked(2)
        sourcesRows.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach {
                    SourceOption(
                        modifier = Modifier.weight(1f),
                        entry = it,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceOption(
    modifier: Modifier,
    entry: SourceOptionEntry,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = entry.onPick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
        Icon(
            entry.icon,
            modifier = Modifier
                .size(55.dp)
                .disabledAlpha(entry.disabled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(entry.title, Modifier.disabledAlpha(entry.disabled), style = MaterialTheme.typography.titleLarge)
            if (entry.disabledReason == null) {
                Text(
                    entry.description,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .disabledAlpha(entry.disabled),
                )
            } else {
                Text(
                    entry.disabledReason,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

private fun Modifier.disabledAlpha(disabled: Boolean): Modifier {
    return if (disabled) {
        alpha(0.4f)
    } else this
}


private class SourceOptionEntry(
    val disabled: Boolean = false,
    val disabledReason: String? = null,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onPick: () -> Unit,
)