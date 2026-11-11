package app.plainlight.ui.light

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.plainlight.R
import app.plainlight.theme.Spacing
import app.plainlight.ui.appViewModelExtras
import app.plainlight.ui.common.DetailScaffold
import app.plainlight.ui.lights.SendProblemBanner

private const val MIN_BRIGHTNESS = 0.05f

@Composable
fun LightScreen(
    lightId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightViewModel =
        viewModel(key = lightId, factory = LightViewModel.Factory(lightId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.removed) { if (state.removed) onBack() }
    val light = state.light ?: return

    DetailScaffold(
        title = light.name,
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (!state.simpleMode) {
                IconButton(onClick = { renaming = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.light_rename))
                }
            }
        },
    ) { inner ->
        Column(
            modifier = inner.verticalScroll(rememberScrollState()).padding(bottom = Spacing.block),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            SendProblemBanner(state.problem, onDismiss = viewModel::dismissProblem)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                modifier = Modifier.padding(horizontal = Spacing.base),
            ) {
                Button(onClick = viewModel::switchOn, modifier = Modifier.weight(1f).heightIn(min = 72.dp)) {
                    Text(stringResource(R.string.lights_on), style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(onClick = viewModel::switchOff, modifier = Modifier.weight(1f).heightIn(min = 72.dp)) {
                    Text(stringResource(R.string.lights_off), style = MaterialTheme.typography.titleLarge)
                }
            }
            LabelledSlider(
                label = stringResource(R.string.light_brightness),
                value = state.setting.brightness,
                onChange = viewModel::previewBrightness,
                onDone = viewModel::sendBrightness,
                range = MIN_BRIGHTNESS..1f,
            )
            if (!state.simpleMode) {
                LabelledSlider(
                    label = stringResource(R.string.light_colour),
                    value = state.setting.coldness,
                    onChange = viewModel::previewColdness,
                    onDone = viewModel::sendColdness,
                    start = stringResource(R.string.light_warm),
                    end = stringResource(R.string.light_cool),
                )
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(Spacing.tight),
                ) {
                    FilledTonalButton(onClick = viewModel::remember, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.light_remember))
                    }
                    Text(
                        stringResource(R.string.light_remember_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScenesSection(
                    scenes = light.scenes,
                    onApply = viewModel::applyScene,
                    onSave = viewModel::saveScene,
                    onDelete = { viewModel.deleteScene(it.id) },
                )
                TimerSection(timerHours = state.timerHours, onSwitchOffIn = viewModel::switchOffIn)
                TextButton(onClick = { confirmRemove = true }, modifier = Modifier.padding(horizontal = Spacing.base)) {
                    Text(stringResource(R.string.light_remove))
                }
            }
        }
    }

    if (renaming) {
        NameDialog(
            title = stringResource(R.string.light_rename),
            label = stringResource(R.string.add_light_name),
            initial = light.name,
            onConfirm = {
                renaming = false
                viewModel.rename(it)
            },
            onDismiss = { renaming = false },
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.light_remove_confirm_title, light.name)) },
            text = { Text(stringResource(R.string.light_remove_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.remove()
                }) { Text(stringResource(R.string.light_remove_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmRemove = false },
                ) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
    start: String? = null,
    end: String? = null,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.base)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            valueRange = range,
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.outline),
        )
        if (start != null && end != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(start, style = MaterialTheme.typography.bodySmall)
                Text(end, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TimerSection(
    timerHours: Int?,
    onSwitchOffIn: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
    ) {
        Text(stringResource(R.string.light_timer), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            for (hours in 1..2) {
                OutlinedButton(onClick = { onSwitchOffIn(hours) }, modifier = Modifier.weight(1f)) {
                    Text(pluralStringResource(R.plurals.light_timer_off_in, hours, hours), textAlign = TextAlign.Center)
                }
            }
        }
        if (timerHours != null) {
            Text(
                pluralStringResource(R.plurals.light_timer_asked, timerHours, timerHours),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
