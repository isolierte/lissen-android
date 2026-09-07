package org.grakovne.lissen.ui.screens.player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.grakovne.lissen.R
import org.grakovne.lissen.domain.LibraryType
import org.grakovne.lissen.ui.components.LissenModalBottomSheet
import org.grakovne.lissen.ui.screens.player.composable.common.provideNowPlayingTitle
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.PlayerViewModel

/**
 * Full-screen chapter list sheet, opened from the "chapters" nav bar item.
 *
 * Slides up from the bottom, reusing the same popup style as the Downloads and
 * Playback Speed sheets. Provides ascending/descending sort and a button to
 * locate the currently playing chapter; on open it auto-scrolls there.
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

  // 排序状态：正序/倒序
  var reversed by remember { mutableStateOf(false) }
  var locateTick by remember { mutableIntStateOf(0) }

  val displayedChapters =
    remember(chapters, reversed) {
      if (reversed) chapters.reversed() else chapters
    }

  val currentTrackId =
    remember(currentTrackIndex, chapters) {
      chapters.getOrNull(currentTrackIndex)?.id
    }

  // 当前章节在当前显示(可能倒序)列表里的位置
  val currentDisplayIndex =
    remember(displayedChapters, currentTrackId) {
      displayedChapters.indexOfFirst { it.id == currentTrackId }
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

  // 打开时自动定位到当前播放章节；切换排序或点定位按钮时重新定位
  LaunchedEffect(currentDisplayIndex, locateTick) {
    if (currentDisplayIndex >= 0 && displayedChapters.isNotEmpty()) {
      listState.animateScrollToItem(currentDisplayIndex)
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
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = provideNowPlayingTitle(libraryType, context),
          style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = colorScheme.primary,
          modifier = Modifier.weight(1f),
        )

        IconButton(
          onClick = { locateTick++ },
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = stringResource(R.string.chapter_list_locate_current),
            tint = colorScheme.primary,
          )
        }

        IconButton(
          onClick = { reversed = !reversed },
        ) {
          Icon(
            imageVector = if (reversed) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
            contentDescription =
              stringResource(
                if (reversed) {
                  R.string.chapter_list_sort_descending
                } else {
                  R.string.chapter_list_sort_ascending
                },
              ),
            tint = colorScheme.onSurface,
          )
        }
      }

      HorizontalDivider()
      Text(
        text =
          stringResource(
            if (reversed) {
              R.string.chapter_list_sort_descending
            } else {
              R.string.chapter_list_sort_ascending
            },
          ),
        style = typography.labelMedium,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
      )

      LazyColumn(
        state = listState,
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth(),
      ) {
        itemsIndexed(
          displayedChapters,
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

          if (index < displayedChapters.size - 1) {
            HorizontalDivider(
              thickness = 1.dp,
              modifier = Modifier.padding(start = 24.dp, end = 4.dp),
            )
          }
        }
      }
    }
  }
}