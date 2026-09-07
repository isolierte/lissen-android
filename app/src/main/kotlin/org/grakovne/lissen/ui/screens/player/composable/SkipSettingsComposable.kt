package org.grakovne.lissen.ui.screens.player.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.domain.BookSkipSettings
import org.grakovne.lissen.ui.components.LissenModalBottomSheet
import org.grakovne.lissen.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipSettingsComposable(
  playerViewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
) {
  val skipSettings by playerViewModel.skipSettings.collectAsState()
  val currentChapterPosition by playerViewModel.currentChapterPosition.collectAsState()
  val book by playerViewModel.book.collectAsState()

  val current = skipSettings ?: BookSkipSettings()
  var enabled by remember { mutableStateOf(current.enabled) }
  var introSeconds by remember { mutableIntStateOf(current.introSkipSeconds ?: 0) }
  var outroSeconds by remember { mutableIntStateOf(current.outroSkipSeconds ?: 0) }
  var introInput by remember { mutableStateOf(if (introSeconds > 0) introSeconds.toString() else "") }
  var outroInput by remember { mutableStateOf(if (outroSeconds > 0) outroSeconds.toString() else "") }

  val presets = listOf(5, 10, 15, 30)

  fun applySettings() {
    playerViewModel.saveSkipSettings(
      BookSkipSettings(
        enabled = enabled,
        introSkipSeconds = introSeconds.takeIf { it > 0 },
        outroSkipSeconds = outroSeconds.takeIf { it > 0 },
      ),
    )
  }

  LissenModalBottomSheet(
    onDismissRequest = onDismissRequest,
    containerColor = colorScheme.surface,
    scrollable = false,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      // Title
      Text(
        text = stringResource(R.string.skip_settings_title),
        style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 16.dp),
      )

      // Master toggle
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
            ) {
              enabled = !enabled
              applySettings()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.skip_settings_enable_title),
            style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            text = stringResource(R.string.skip_settings_enable_description),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = enabled,
          onCheckedChange = {
            enabled = it
            applySettings()
          },
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Intro Section
      SkipSection(
        title = stringResource(R.string.skip_settings_intro_title),
        description = stringResource(R.string.skip_settings_intro_description),
        seconds = introSeconds,
        inputText = introInput,
        currentChapterPosition = currentChapterPosition,
        presets = presets,
        onValueChange = { introInput = it },
        onApplyInput = {
          val parsed = introInput.toIntOrNull() ?: 0
          introSeconds = parsed
          introInput = if (parsed > 0) parsed.toString() else ""
          applySettings()
        },
        onPresetClick = {
          introSeconds = it
          introInput = it.toString()
          applySettings()
        },
        onSetFromPosition = {
          val pos = currentChapterPosition.toInt()
          introSeconds = pos
          introInput = if (pos > 0) pos.toString() else ""
          applySettings()
        },
        onClear = {
          introSeconds = 0
          introInput = ""
          applySettings()
        },
        enabled = enabled,
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Outro Section
      SkipSection(
        title = stringResource(R.string.skip_settings_outro_title),
        description = stringResource(R.string.skip_settings_outro_description),
        seconds = outroSeconds,
        inputText = outroInput,
        currentChapterPosition = null, // No "set from position" for outro
        presets = presets,
        onValueChange = { outroInput = it },
        onApplyInput = {
          val parsed = outroInput.toIntOrNull() ?: 0
          outroSeconds = parsed
          outroInput = if (parsed > 0) parsed.toString() else ""
          applySettings()
        },
        onPresetClick = {
          outroSeconds = it
          outroInput = it.toString()
          applySettings()
        },
        onSetFromPosition = null,
        onClear = {
          outroSeconds = 0
          outroInput = ""
          applySettings()
        },
        enabled = enabled,
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun SkipSection(
  title: String,
  description: String,
  seconds: Int,
  inputText: String,
  currentChapterPosition: Double?,
  presets: List<Int>,
  onValueChange: (String) -> Unit,
  onApplyInput: () -> Unit,
  onPresetClick: (Int) -> Unit,
  onSetFromPosition: (() -> Unit)?,
  onClear: () -> Unit,
  enabled: Boolean,
) {
  val focusManager = LocalFocusManager.current

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.5f),
  ) {
    Text(
      text = title,
      style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
      text = description,
      style = typography.bodySmall,
      color = colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    // Preset buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      presets.forEach { preset ->
        FilledTonalButton(
          onClick = { if (enabled) onPresetClick(preset) },
          modifier = Modifier.size(48.dp),
          shape = CircleShape,
          enabled = enabled,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor =
                if (seconds == preset) colorScheme.primary else colorScheme.surfaceContainer,
              contentColor =
                if (seconds == preset) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            ),
          contentPadding = PaddingValues(0.dp),
        ) {
          Text(
            text = stringResource(R.string.skip_settings_preset_seconds, preset),
            style =
              if (seconds == preset) {
                typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              } else {
                typography.labelSmall
              },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Set from current position (for intro only)
      if (onSetFromPosition != null && currentChapterPosition != null) {
        IconButton(
          onClick = { if (enabled) onSetFromPosition() },
          enabled = enabled,
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = stringResource(R.string.skip_settings_set_from_current),
            tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Manual input + current value display
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { value ->
          if (enabled) {
            onValueChange(value.filter { it.isDigit() })
          }
        },
        label = { Text(stringResource(R.string.skip_settings_manual_hint)) },
        placeholder = { Text(stringResource(R.string.skip_settings_manual_placeholder)) },
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
          ),
        keyboardActions =
          KeyboardActions(
            onDone = {
              onApplyInput()
              focusManager.clearFocus()
            },
          ),
        singleLine = true,
        enabled = enabled,
        modifier =
          Modifier
            .weight(1f)
            .height(56.dp),
      )

      if (seconds > 0) {
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
          onClick = { if (enabled) onClear() },
          enabled = enabled,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = colorScheme.errorContainer,
              contentColor = colorScheme.onErrorContainer,
            ),
        ) {
          Text(stringResource(R.string.skip_settings_clear), style = typography.labelMedium)
        }
      }
    }
  }
}
