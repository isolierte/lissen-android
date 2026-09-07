package org.grakovne.lissen.ui.screens.player.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.grakovne.lissen.domain.LibraryType
import org.grakovne.lissen.ui.components.LissenModalBottomSheet
import org.grakovne.lissen.ui.screens.player.composable.common.provideNowPlayingTitle
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.PlayerViewModel

/**
 * Full-screen chapter list sheet, opened from the "chapters" nav bar item.
 *
 * Slides up from the bottom, reusing the same popup style as the Downloads and
 * Playback Speed sheets. On open it auto-scrolls to the currently playing chapter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
  libraryType: LibraryType,
  cachingModelView: CachingModelView,
  playerViewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
) {
  val context = LocalContext.current

  val book by playerViewModel.book.collectAsState()
  val currentTrackIndex by playerViewModel.currentChapterIndex.collectAsState()
  val listState = rememberLazyListState()

  val bookId = book?.id ?: ""
  val chapters = book?.chapters ?: emptyList()

  val currentTrackId =
    remember(currentTrackIndex, chapters) {
      chapters.getOrNull(currentTrackIndex)?.id
    }

  val maxDuration =
    remember(chapters) {
      chapters.maxOfOrNull { it.duration } ?: 0.0
    }

  val cachedChapterIdsFlow =
    remember(bookId) {
      when (bookId.isEmpty()) {
        true -> flowOf(emptySet())
        false -> cachingModelView.provideCachedChapterIds(bookId).map { it.toSet() }
      }
    }
  val cachedChapterIds by cachedChapterIdsFlow.collectAsState(initial = emptySet())

  // 打开时自动定位到当前播放章节
  LaunchedEffect(currentTrackIndex, bookId) {
    if (currentTrackIndex in chapters.indices) {
      listState.animateScrollToItem(currentTrackIndex)
    }
  }

  LissenModalBottomSheet(
    onDismissRequest = onDismissRequest,
    containerColor = colorScheme.surface,
    scrollable = false,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxHeight()
          .fillMaxWidth(),
    ) {
      Text(
        text = provideNowPlayingTitle(libraryType, context),
        style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
      )

      HorizontalDivider()

      LazyColumn(
        state = listState,
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth(),
      ) {
        itemsIndexed(
          chapters,
          key = { _, chapter -> chapter.id },
        ) { index, chapter ->
          PlaylistItemComposable(
            track = chapter,
            isSelected = chapter.id == currentTrackId,
            onClick = { playerViewModel.setChapter(chapter) },
            modifier = Modifier.fillMaxWidth(),
            maxDuration = maxDuration,
            isCached = chapter.id in cachedChapterIds,
          )

          if (index < chapters.size - 1) {
            HorizontalDivider(
              thickness = 1.dp,
              modifier = Modifier.padding(start = 24.dp),
            )
          }
        }
      }
    }
  }
}