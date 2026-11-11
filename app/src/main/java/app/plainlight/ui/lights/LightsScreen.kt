package app.plainlight.ui.lights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.plainlight.theme.Spacing
import app.plainlight.ui.appViewModelExtras
import app.plainlight.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightsScreen(
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightsViewModel =
        viewModel(factory = LightsViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_lights)) }) },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
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
            EmptyState(
                message = stringResource(R.string.lights_empty),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
