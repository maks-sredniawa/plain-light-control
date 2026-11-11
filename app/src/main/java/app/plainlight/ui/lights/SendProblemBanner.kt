package app.plainlight.ui.lights

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.plainlight.R
import app.plainlight.ble.SendResult
import app.plainlight.ui.common.WarningBanner
import app.plainlight.ui.common.startActivitySafely

@Composable
fun SendProblemBanner(
    problem: SendResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    when (problem) {
        null, SendResult.Sent -> Unit
        SendResult.PermissionMissing ->
            WarningBanner(
                title = stringResource(R.string.problem_permission_title),
                body = stringResource(R.string.problem_permission_body),
                actionLabel = stringResource(R.string.problem_permission_action),
                onAct = {
                    context.startActivitySafely(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                    onDismiss()
                },
                modifier = modifier,
            )
        SendResult.BluetoothOff ->
            WarningBanner(
                title = stringResource(R.string.problem_bluetooth_title),
                body = stringResource(R.string.problem_bluetooth_body),
                actionLabel = stringResource(R.string.problem_bluetooth_action),
                onAct = {
                    context.startActivitySafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    onDismiss()
                },
                modifier = modifier,
            )
        SendResult.AdvertisingUnsupported ->
            WarningBanner(
                title = stringResource(R.string.problem_unsupported_title),
                body = stringResource(R.string.problem_unsupported_body),
                actionLabel = stringResource(R.string.action_close),
                onAct = onDismiss,
                modifier = modifier,
            )
        is SendResult.Failed ->
            WarningBanner(
                title = stringResource(R.string.problem_failed_title),
                body = stringResource(R.string.problem_failed_body),
                actionLabel = stringResource(R.string.action_close),
                onAct = onDismiss,
                modifier = modifier,
            )
    }
}
