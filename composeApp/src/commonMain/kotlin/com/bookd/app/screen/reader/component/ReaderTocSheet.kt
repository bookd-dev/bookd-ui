package com.bookd.app.screen.reader.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.TocItem
import com.bookd.app.data.vm.TocSortOrder
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.toc
import app.composeapp.generated.resources.no_bookmarks

import app.composeapp.generated.resources.bookmarks_count
import app.composeapp.generated.resources.toc_sort_asc
import app.composeapp.generated.resources.toc_sort_desc
import app.composeapp.generated.resources.toc_current_reading
import app.composeapp.generated.resources.toc_read_status_read
import app.composeapp.generated.resources.toc_read_status_reading
import app.composeapp.generated.resources.toc_read_status_unread
import app.composeapp.generated.resources.toc_word_count_wan
import app.composeapp.generated.resources.toc_word_count_qian
import app.composeapp.generated.resources.toc_word_count
import app.composeapp.generated.resources.toc_chapter_fallback
import app.composeapp.generated.resources.toc_delete_bookmark
import org.jetbrains.compose.resources.stringResource

/**
 * 目录/书签面板（BottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTocSheet(
    toc: List<TocItem>,
    bookmarks: List<BookmarkResponse>,
    currentChapterIndex: Int,
    sortOrder: TocSortOrder,
    onDismiss: () -> Unit,
    onTocItemClick: (Int) -> Unit,
    onBookmarkClick: (BookmarkResponse) -> Unit,
    onBookmarkDelete: (Int) -> Unit,
    onSortOrderToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .navigationBarsPadding()
        ) {
            // Tab 栏
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(Res.string.toc)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(Res.string.bookmarks_count, bookmarks.size)) }
                )
            }
            
            when (selectedTab) {
                0 -> TocContent(
                    toc = toc,
                    currentChapterIndex = currentChapterIndex,
                    sortOrder = sortOrder,
                    onTocItemClick = onTocItemClick,
                    onSortOrderToggle = onSortOrderToggle
                )
                1 -> BookmarkContent(
                    bookmarks = bookmarks,
                    toc = toc,
                    onBookmarkClick = onBookmarkClick,
                    onBookmarkDelete = onBookmarkDelete
                )
            }
        }
    }
}

/**
 * 目录内容
 */
@Composable
private fun TocContent(
    toc: List<TocItem>,
    currentChapterIndex: Int,
    sortOrder: TocSortOrder,
    onTocItemClick: (Int) -> Unit,
    onSortOrderToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 排序按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSortOrderToggle) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = if (sortOrder == TocSortOrder.ASC) stringResource(Res.string.toc_sort_asc) else stringResource(Res.string.toc_sort_desc)
                )
            }
            Text(
                text = if (sortOrder == TocSortOrder.ASC) stringResource(Res.string.toc_sort_asc) else stringResource(Res.string.toc_sort_desc),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        
        HorizontalDivider()
        
        // 目录列表
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = toc,
                key = { it.index }
            ) { item ->
                TocItemRow(
                    item = item,
                    isCurrentChapter = item.index == currentChapterIndex,
                    onClick = { onTocItemClick(item.index) }
                )
            }
        }
    }
}

/**
 * 目录项行
 */
@Composable
private fun TocItemRow(
    item: TocItem,
    isCurrentChapter: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (16 + item.level * 16).dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 章节标题
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrentChapter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 当前阅读标记
            if (isCurrentChapter) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = stringResource(Res.string.toc_current_reading),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 第二行：字数 · 阅读状态 · 进度
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 字数
            if (item.wordCount > 0) {
                Text(
                    text = when {
                        item.wordCount >= 10000 -> stringResource(Res.string.toc_word_count_wan, item.wordCount / 10000)
                        item.wordCount >= 1000 -> stringResource(Res.string.toc_word_count_qian, item.wordCount / 1000)
                        else -> stringResource(Res.string.toc_word_count, item.wordCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 阅读状态
            val statusText = when (item.readStatus) {
                "read" -> stringResource(Res.string.toc_read_status_read)
                "reading" -> stringResource(Res.string.toc_read_status_reading)
                else -> stringResource(Res.string.toc_read_status_unread)
            }
            val statusColor = when (item.readStatus) {
                "read" -> MaterialTheme.colorScheme.primary
                "reading" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
            
            // 进度
            if (item.readProgress > 0 && item.readStatus != "unread") {
                Text(
                    text = " · ${(item.readProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 书签内容
 */
@Composable
private fun BookmarkContent(
    bookmarks: List<BookmarkResponse>,
    toc: List<TocItem>,
    onBookmarkClick: (BookmarkResponse) -> Unit,
    onBookmarkDelete: (Int) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.no_bookmarks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = bookmarks,
            key = { it.id }
        ) { bookmark ->
            BookmarkRow(
                bookmark = bookmark,
                chapterTitle = findChapterTitle(toc, bookmark.chapterIndex, stringResource(Res.string.toc_chapter_fallback, bookmark.chapterIndex + 1)),
                onClick = { onBookmarkClick(bookmark) },
                onDelete = { onBookmarkDelete(bookmark.id) }
            )
        }
    }
}

/**
 * 书签行
 */
@Composable
private fun BookmarkRow(
    bookmark: BookmarkResponse,
    chapterTitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!bookmark.note.isNullOrEmpty()) {
                Text(
                    text = bookmark.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Text(
                text = bookmark.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.toc_delete_bookmark),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


/**
 * 查找章节标题
 */
private fun findChapterTitle(toc: List<TocItem>, index: Int, fallback: String): String {
    fun findInList(items: List<TocItem>): String? {
        for (item in items) {
            if (item.index == index) return item.title
            findInList(item.children)?.let { return it }
        }
        return null
    }
    return findInList(toc) ?: fallback
}
