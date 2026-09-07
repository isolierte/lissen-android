package org.grakovne.lissen.ui.screens.player.composable

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircleFilled
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.common.withHaptic
import org.grakovne.lissen.ui.extensions.formatTime
import org.grakovne.lissen.ui.extensions.spokenDuration
import org.grakovne.lissen.ui.screens.player.composable.common.provideForwardIcon
import org.grakovne.lissen.ui.screens.player.composable.common.provideReplayIcon
import org.grakovne.lissen.viewmodel.PlayerViewModel
import org.grakovne.lissen.viewmodel.SettingsViewModel

@Composable
fun TrackControlComposable(
  viewModel: PlayerViewModel,
  settingsViewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val isPlaying by viewModel.isPlaying.collectAsState()
  val currentTrackIndex by viewModel.currentChapterIndex.collectAsState()
  val currentTrackPosition by viewModel.currentChapterPosition.collectAsState()
  val currentTrackDuration by viewModel.currentChapterDuration.collectAsState()

  val seekTime by settingsViewModel.seekTime.collectAsState()

  val book by viewModel.book.collectAsState()
  val chapters = book?.chapters ?: emptyList()

  val view: View = LocalView.current

  var sliderPosition by remember { mutableDoubleStateOf(0.0) }
  var isDragging by remember { mutableStateOf(false) }

  LaunchedEffect(currentTrackPosition, currentTrackIndex, currentTrackDuration) {
    if (!isDragging) {
      sliderPosition = currentTrackPosition
    }
  }

  val chapterTitle =
    remember(currentTrackIndex, book) {
      book?.chapters?.getOrNull(currentTrackIndex)?.title.orEmpty()
    }

  val durationFloat = currentTrackDuration.toFloat()
  val fraction =
    if (durationFloat > 0f) {
      (sliderPosition.toFloat() / durationFloat).coerceIn(0f, 1f)
    } else {
      0f
    }

  Column(
    modifier =
      modifier
        .testTag("trackControls")
        .fillMaxWidth()
        .padding(horizontal = 6.dp),
  ) {
    val positionLabel = stringResource(R.string.a11y_playback_position)
    val spokenPosition =
      stringResource(
        R.string.a11y_position_of,
        spokenDuration(sliderPosition.toInt()),
        spokenDuration(currentTrackDuration.toInt()),
      )

    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      // 正在播放的章节名称，点击可打开章节列表
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { viewModel.requestChapterList() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = chapterTitle,
          style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          tint = colorScheme.onBackground.copy(alpha = 0.4f),
          modifier = Modifier.size(20.dp),
        )
      }

      ChapterSeekBar(
        valuePercent = fraction,
        onSeek = { f ->
          isDragging = true
          sliderPosition = (f * durationFloat).toDouble()
        },
        onSeekFinished = {
          isDragging = false
          viewModel.seekTo(sliderPosition)
        },
        contentDescription = positionLabel,
        stateDescription = spokenPosition,
      )
    }

    Box(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .offset(y = (-4).dp)
              .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = sliderPosition.toInt().formatTime(true),
            style = typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {},
          )
          Text(
            text =
              maxOf(0.0, currentTrackDuration - sliderPosition)
                .toInt()
                .formatTime(true),
            style = typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {},
          )
        }
      }

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            withHaptic(view) { viewModel.previousTrack() }
          },
          enabled = true,
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.a11y_previous_track),
            tint = colorScheme.onBackground,
            modifier = Modifier.size(42.dp),
          )
        }

        IconButton(
          onClick = { withHaptic(view) { viewModel.rewind() } },
        ) {
          Icon(
            imageVector = provideReplayIcon(seekTime),
            contentDescription = stringResource(R.string.a11y_rewind_seconds, seekTime.rewind),
            tint = colorScheme.onBackground,
            modifier = Modifier.size(52.dp),
          )
        }

        IconButton(
          onClick = { withHaptic(view) { viewModel.togglePlayPause() } },
          modifier = Modifier.size(72.dp),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
            contentDescription = if (isPlaying) stringResource(R.string.a11y_pause) else stringResource(R.string.a11y_play),
            tint = colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
          )
        }

        IconButton(
          onClick = { withHaptic(view) { viewModel.forward() } },
        ) {
          Icon(
            imageVector = provideForwardIcon(seekTime),
            contentDescription = stringResource(R.string.a11y_fast_forward_seconds, seekTime.forward),
            tint = colorScheme.onBackground,
            modifier = Modifier.size(52.dp),
          )
        }

        IconButton(
          onClick = {
            if (currentTrackIndex < chapters.size - 1) {
              withHaptic(view) { viewModel.nextTrack() }
            }
          },
          enabled = currentTrackIndex < chapters.size - 1,
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.a11y_next_track),
            tint =
              if (currentTrackIndex < chapters.size - 1) {
                colorScheme.onBackground
              } else {
                colorScheme.onBackground.copy(
                  alpha = 0.3f,
                )
              },
            modifier = Modifier.size(42.dp),
          )
        }
      }
    }
  }
}

/**
 * A custom, thicker seek bar with a rounded track, a filled primary portion and
 * a thumb. Supports tap-to-seek and horizontal drag.
 */
@Composable
private fun ChapterSeekBar(
  valuePercent: Float,
  onSeek: (Float) -> Unit,
  onSeekFinished: () -> Unit,
  contentDescription: String,
  stateDescription: String,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val trackHeight = with(density) { 10.dp.toPx() }
  val thumbRadius = with(density) { 11.dp.toPx() }
  val thumbInset = with(density) { 3.5.dp.toPx() }

  // Capture colors in composable scope (MaterialTheme.colorScheme is @Composable)
  val trackColor = colorScheme.surfaceVariant
  val fillColor = colorScheme.primary
  val thumbInnerColor = colorScheme.surface

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(30.dp)
        .pointerInput(Unit) {
          fun seekAt(x: Float) {
            val w = size.width.toFloat()
            if (w > 0f) {
              onSeek((x / w).coerceIn(0f, 1f))
            }
          }
          detectTapGestures { offset ->
            seekAt(offset.x)
            onSeekFinished()
          }
        }
        .pointerInput(Unit) {
          fun seekAt(x: Float) {
            val w = size.width.toFloat()
            if (w > 0f) {
              onSeek((x / w).coerceIn(0f, 1f))
            }
          }
          detectDragGestures(
            onDragStart = { offset -> seekAt(offset.x) },
            onDrag = { change, _ ->
              change.consume()
              seekAt(change.position.x)
            },
            onDragEnd = { onSeekFinished() },
            onDragCancel = { onSeekFinished() },
          )
        }
        .semantics {
          this.contentDescription = contentDescription
          this.stateDescription = stateDescription
        }
        .drawBehind {
          val barWidth = size.width
          val barCenterY = size.height / 2f
          val progress = valuePercent.coerceIn(0f, 1f)
          val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

          // background track
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, barCenterY - trackHeight / 2f),
            size = Size(barWidth, trackHeight),
            cornerRadius = corner,
          )

          // filled progress
          val fillWidth = barWidth * progress
          if (fillWidth > 0f) {
            drawRoundRect(
              color = fillColor,
              topLeft = Offset(0f, barCenterY - trackHeight / 2f),
              size = Size(fillWidth, trackHeight),
              cornerRadius = corner,
            )
          }

          // thumb
          val thumbX = barWidth * progress
          drawCircle(
            color = fillColor,
            radius = thumbRadius,
            center = Offset(thumbX, barCenterY),
          )
          drawCircle(
            color = thumbInnerColor,
            radius = thumbRadius - thumbInset,
            center = Offset(thumbX, barCenterY),
          )
        },
  )
}
