package app.plainlight.ui.addlight

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.plainlight.R
import app.plainlight.ble.canAdvertise
import app.plainlight.theme.Spacing
import app.plainlight.ui.appViewModelExtras
import app.plainlight.ui.common.DetailScaffold
import app.plainlight.ui.lights.SendProblemBanner

@Composable
fun AddLightScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddLightViewModel = viewModel(factory = AddLightViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultName = stringResource(R.string.add_light_name_default)
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.step) { if (state.step == AddLightStep.DONE) onDone() }

    val askPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.permissionGranted() else viewModel.permissionDenied()
        }
    val pair = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canAdvertise(context)) {
            askPermission.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            viewModel.pair()
        }
    }

    DetailScaffold(title = stringResource(R.string.lights_add), onBack = onDone, modifier = modifier) { inner ->
        Column(
            modifier = inner.verticalScroll(rememberScrollState()).padding(bottom = Spacing.block),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            SendProblemBanner(state.problem, onDismiss = viewModel::dismissProblem)
            Column(
                modifier = Modifier.padding(horizontal = Spacing.base),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_light_name)) },
                    placeholder = { Text(defaultName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (state.step) {
                    AddLightStep.INSTRUCTIONS -> {
                        if (state.permissionJustGranted) {
                            Body(stringResource(R.string.add_light_after_permission))
                        } else if (state.attempt > 0) {
                            Body(stringResource(R.string.add_light_retry))
                        }
                        Body(stringResource(R.string.add_light_step_power))
                        Body(stringResource(R.string.add_light_step_pair))
                        Button(
                            onClick = pair,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.add_light_pair)) }
                    }
                    AddLightStep.PAIRING -> {
                        CircularProgressIndicator()
                        Body(stringResource(R.string.add_light_pairing))
                    }
                    AddLightStep.CHECK -> {
                        Body(stringResource(R.string.add_light_check))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
                            Button(
                                onClick = { viewModel.confirm(name.ifBlank { defaultName }) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.add_light_yes))
                            }
                            OutlinedButton(onClick = viewModel::deny, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.add_light_no))
                            }
                        }
                    }
                    AddLightStep.FAILED -> {
                        Body(stringResource(R.string.add_light_failed))
                        Button(onClick = viewModel::restart, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.add_light_try_again))
                        }
                    }
                    AddLightStep.SAVING, AddLightStep.DONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}
