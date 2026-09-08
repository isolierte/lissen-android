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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.domain.BookSkipSettings
import org.grakovne.lissen.ui.components.LissenModalBottomSheet
import org.grakovne.lissen.ui.components.slider.CommonSlider
import org.grakovne.lissen.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipSettingsComposable(
  playerViewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
) {
  val skipSettings by playerViewModel.skipSettings.collectAsState()
  val currentChapterPosition by playerViewModel.currentChapterPosition.collectAsState()

  val current = skipSettings ?: BookSkipSettings()
  var enabled by remember { mutableStateOf(current.enabled) }
  var introSeconds by remember { mutableIntStateOf(current.introSkipSeconds ?: 0) }
  var outroSeconds by remember { mutableIntStateOf(current.outroSkipSeconds ?: 0) }

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
    containerColor = colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Title (matches sleep timer page style)
      Text(
        text = stringResource(R.string.skip_settings_title),
        style = typography.bodyLarge,
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

      // ── Intro Section: seconds slider (same style as sleep timer) ──
      SkipSecondsSection(
        title = stringResource(R.string.skip_settings_intro_title),
        seconds = introSeconds,
        enabled = enabled,
        showLocate = true,
        locateContentDescription = stringResource(R.string.skip_settings_set_from_current),
        onLocate = {
          if (enabled) {
            val pos = currentChapterPosition.toInt().coerceIn(0, MAX_SKIP_SECONDS)
            introSeconds = pos
            applySettings()
          }
        },
        onChange = { value ->
          if (enabled) {
            introSeconds = value
            applySettings()
          }
        },
      )

      Spacer(modifier = Modifier.height(16.dp))

      // ── Outro Section ──
      SkipSecondsSection(
        title = stringResource(R.string.skip_settings_outro_title),
        seconds = outroSeconds,
        enabled = enabled,
        showLocate = false,
        locateContentDescription = "",
        onLocate = {},
        onChange = { value ->
          if (enabled) {
            outroSeconds = value
            applySettings()
          }
        },
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SkipSecondsSection(
  title: String,
  seconds: Int,
  enabled: Boolean,
  showLocate: Boolean,
  locateContentDescription: String,
  onLocate: () -> Unit,
  onChange: (Int) -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.5f),
  ) {
    // Section title row: title on the left, optional locate button on the right
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.weight(1f),
      )

      if (showLocate) {
        IconButton(
          onClick = { onLocate() },
          enabled = enabled,
          modifier = Modifier.size(40.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = locateContentDescription,
            tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // Main time adjustment: sliding bar, identical to the sleep timer page
    SkipSecondsSlider(
      seconds = seconds,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
      onChange = onChange,
    )

    // Quick presets (circle buttons, same as sleep timer page)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      SkipSecondsPresets.forEach { value ->
        val selected =
          when (value) {
            null -> seconds == 0
            else -> seconds == value
          }

        FilledTonalButton(
          onClick = {
            val target =
              when (value) {
                null -> 0
                else -> value
              }
            onChange(target)
          },
          modifier = Modifier.size(56.dp),
          shape = CircleShape,
          enabled = enabled,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = if (selected) colorScheme.primary else colorScheme.surfaceContainer,
              contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            ),
          contentPadding = PaddingValues(0.dp),
        ) {
          if (value == null) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          } else {
            Text(
              text = value.toString(),
              style =
                if (selected) {
                  typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                  typography.labelMedium
                },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

/**
 * Slider for picking a whole number of seconds (0..[MAX_SKIP_SECONDS]).
 * Mirrors the SleepTimerSlider UI (large centered value, tick scale, drag).
 */
@Composable
private fun SkipSecondsSlider(
  seconds: Int,
  modifier: Modifier = Modifier,
  onChange: (Int) -> Unit,
) {
  val range = 0..MAX_SKIP_SECONDS
  val headerTemplate = stringResource(R.string.skip_settings_seconds_value)

  CommonSlider(
    internalValue = seconds.coerceIn(range),
    range = range,
    formatHeader = { value -> headerTemplate.format(value.roundToInt()) },
    formatIndex = { it },
    labeledIndexes = (5..MAX_SKIP_SECONDS step 5).toList(),
    modifier = modifier,
    onUpdate = { value -> onChange(value.roundToInt().coerceIn(range)) },
  )
}

private val SkipSecondsPresets =
  listOf(
    null, // 0 = off / clear
    5,
    10,
    15,
    30,
  )

private const val MAX_SKIP_SECONDS = 120
