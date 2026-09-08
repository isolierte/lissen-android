package org.grakovne.lissen.ui.screens.player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.grakovne.lissen.R
import org.grakovne.lissen.domain.LibraryType
import org.grakovne.lissen.domain.PlayingChapter
import org.grakovne.lissen.ui.components.LissenModalBottomSheet
import org.grakovne.lissen.ui.screens.player.composable.common.provideNowPlayingTitle
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.PlayerViewModel

/**
 * Full-screen chapter list sheet, opened from the "chapters" nav bar item.
 *
 * Slides up from the bottom, reusing the same popup style as the Downloads and
 * Playback Speed sheets. Provides:
 *  - a search field matching chapter ordinal (序号) or chapter title,
 *  - ascending/descending sort,
 *  - a "locate current chapter" button.
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

  var reversed by remember { mutableStateOf(false) }
  var locateTick by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }

  // 序号 = 章节在书中的原始顺序（1-based）。排序反转不改变章节序号。
  val itemsWithOrdinal: List<Pair<Int, PlayingChapter>> =
    remember(chapters) {
      chapters.mapIndexed { index, chapter -> (index + 1) to chapter }
    }

  // 应用倒序（仅影响排列，不影响序号）
  val orderedItems: List<Pair<Int, PlayingChapter>> =
    remember(itemsWithOrdinal, reversed) {
      if (reversed) itemsWithOrdinal.reversed() else itemsWithOrdinal
    }

  // 搜索过滤：匹配 序号字符串 或 章节名称（忽略大小写，包含即中）
  val displayedItems: List<Pair<Int, PlayingChapter>> =
    remember(orderedItems, searchQuery) {
      val q = searchQuery.trim().lowercase()
      if (q.isEmpty()) {
        orderedItems
      } else {
        orderedItems.filter { (ordinal, chapter) ->
          ordinal.toString().lowercase().contains(q) ||
            chapter.title.lowercase().contains(q)
        }
      }
    }

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

  // 打开/切排序/点定位 → 滚到当前播放章节；搜索时不做自动定位
  LaunchedEffect(currentTrackId, reversed, locateTick, searchQuery) {
    if (searchQuery.isNotBlank()) return@LaunchedEffect
    val idx = displayedItems.indexOfFirst { it.second.id == currentTrackId }
    if (idx >= 0 && displayedItems.isNotEmpty()) {
      listState.animateScrollToItem(idx)
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

      // 搜索框：可按 序号 或 章节名 搜索
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier =
            Modifier
              .weight(1f)
              .height(52.dp),
          placeholder = { Text(stringResource(R.string.chapter_list_search_hint)) },
          leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.a11y_clear))
              }
            }
          },
          singleLine = true,
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions =
            KeyboardActions(
              onSearch = { /* live filter is enough */ },
            ),
        )
      }

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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
      )

      LazyColumn(
        state = listState,
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth(),
      ) {
        itemsIndexed(
          displayedItems,
          key = { _, (_, chapter) -> chapter.id },
        ) { index, (ordinal, chapter) ->
          PlaylistItemComposable(
            track = chapter,
            isSelected = chapter.id == currentTrackId,
            onClick = { playerViewModel.setChapter(chapter) },
            modifier = Modifier.fillMaxWidth(),
            maxDuration = maxDuration,
            isCached = chapter.id in cachedChapterIds,
            ordinal = ordinal,
          )

          if (index < displayedItems.size - 1) {
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