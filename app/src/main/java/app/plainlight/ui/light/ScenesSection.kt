package app.plainlight.ui.light

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.plainlight.R
import app.plainlight.data.Scene
import app.plainlight.data.ScenePreset
import app.plainlight.theme.Spacing

@Composable
internal fun ScenesSection(
    scenes: List<Scene>,
    onApply: (Scene) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (Scene) -> Unit,
) {
    var saving by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(horizontal = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
    ) {
        Text(stringResource(R.string.light_scenes), style = MaterialTheme.typography.titleMedium)
        for (scene in scenes) {
            val label = sceneLabel(scene)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onApply(scene) }, modifier = Modifier.weight(1f)) { Text(label) }
                IconButton(onClick = { deletingId = scene.id }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.light_scene_delete, label))
                }
            }
        }
        TextButton(onClick = { saving = true }) { Text(stringResource(R.string.light_scene_save)) }
        Text(
            stringResource(R.string.light_scene_save_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (saving) {
        NameDialog(
            title = stringResource(R.string.light_scene_save),
            label = stringResource(R.string.light_scene_name),
            initial = "",
            onConfirm = {
                saving = false
                onSave(it)
            },
            onDismiss = { saving = false },
        )
    }

    val deleting = scenes.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.light_scene_delete_confirm_title, sceneLabel(deleting))) },
            text = { Text(stringResource(R.string.light_scene_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingId = null
                    onDelete(deleting)
                }) { Text(stringResource(R.string.light_scene_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun sceneLabel(scene: Scene): String =
    scene.name ?: when (scene.preset) {
        ScenePreset.EVENING -> stringResource(R.string.light_scene_evening)
        ScenePreset.READING -> stringResource(R.string.light_scene_reading)
        ScenePreset.NIGHT_LIGHT -> stringResource(R.string.light_scene_night_light)
        null -> ""
    }

@Composable
internal fun NameDialog(
    title: String,
    label: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
