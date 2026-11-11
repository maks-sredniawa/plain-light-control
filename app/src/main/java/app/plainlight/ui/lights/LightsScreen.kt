package app.plainlight.ui.lights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.plainlight.R
import app.plainlight.data.Light
import app.plainlight.theme.Spacing
import app.plainlight.ui.appViewModelExtras
import app.plainlight.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightsScreen(
    onOpenSupport: () -> Unit,
    onOpenLight: (String) -> Unit,
    onAddLight: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightsViewModel =
        viewModel(factory = LightsViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_lights)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLight,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.lights_add)) },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            SendProblemBanner(state.problem, onDismiss = viewModel::dismissProblem)
            if (state.showTipAsk) {
                TipAskCard(
                    onTip = {
                        viewModel.answerTipAsk()
                        onOpenSupport()
                    },
                    onDecline = viewModel::answerTipAsk,
                    modifier = Modifier.padding(start = Spacing.base, end = Spacing.base, top = Spacing.base),
                )
            }
            if (state.lights.isEmpty()) {
                EmptyState(message = stringResource(R.string.lights_empty), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(Spacing.snug),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.lights, key = { it.id }) { light ->
                        LightCard(
                            light = light,
                            onOpen = { onOpenLight(light.id) },
                            onOn = { viewModel.switchOn(light) },
                            onOff = { viewModel.switchOff(light) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LightCard(
    light: Light,
    onOpen: () -> Unit,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(modifier = Modifier.padding(Spacing.base), verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
            Text(light.name, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.snug)) {
                Button(onClick = onOn, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.lights_on)) }
                OutlinedButton(
                    onClick = onOff,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.lights_off)) }
            }
        }
    }
}
