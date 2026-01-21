package com.bookd.app.screen.bookshelf.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.add_to_bookshelves
import app.composeapp.generated.resources.book_detail
import app.composeapp.generated.resources.chapters_count
import app.composeapp.generated.resources.move_to_bookshelf
import app.composeapp.generated.resources.reading_progress
import app.composeapp.generated.resources.remove_from_all_bookshelves
import coil3.compose.AsyncImage
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.model.BookWithProgress
import com.bookd.app.ui.theme.FormatEpub
import com.bookd.app.ui.theme.FormatMobi
import com.bookd.app.ui.theme.FormatPdf
import com.bookd.app.ui.theme.FormatTxt
import org.jetbrains.compose.resources.stringResource

/**
 * 书籍操作菜单类型
 */
enum class BookMenuAction {
    /** 查看书籍详情 */
    Detail,
    /** 添加到其他书架（书籍未加入的） */
    AddToBookshelves,
    /** 移动到书架（从当前书架移除并添加到其他书架） */
    MoveToBookshelf,
    /** 从所有书架移除 */
    RemoveFromAll
}

/**
 * 书架书籍列表项（列表模式）
 * 
 * 显示书籍封面、标题、作者、格式、阅读进度等信息
 * 点击打开阅读器，三点图标显示更多操作
 */
@Composable
fun BookshelfBookListItem(
    bookWithProgress: BookWithProgress,
    showMoveToBookshelf: Boolean = true,
    onClick: () -> Unit = {},
    onMenuAction: (BookMenuAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val book = bookWithProgress.book
    val progress = bookWithProgress.progress
    val progressPercent = progress?.let { (it.progress * 100).toInt() } ?: 0
    
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面图片
        BookCover(
            coverUrl = book.coverPath,
            title = book.title,
            modifier = Modifier.size(width = 80.dp, height = 110.dp)
        )
        
        // 书籍信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 标题
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 阅读进度文本
            Text(
                text = stringResource(Res.string.reading_progress, progressPercent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 作者
            book.author?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部信息行：格式 + 章节数 + 三点菜单
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 格式标签
                FormatTag(format = book.format)
                
                // 章节数
                if (book.chaptersCount > 0) {
                    Text(
                        text = stringResource(Res.string.chapters_count, book.chaptersCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 三点菜单（右下角）
                BookContextMenu(
                    showMenu = showMenu,
                    showMoveToBookshelf = showMoveToBookshelf,
                    onShowMenu = { showMenu = true },
                    onDismissMenu = { showMenu = false },
                    onMenuAction = { action ->
                        showMenu = false
                        onMenuAction(action)
                    }
                )
            }
        }
    }
}

/**
 * 书架书籍卡片（瀑布流/网格模式）
 * 
 * 显示封面、书名、阅读进度、作者
 * 点击打开阅读器，三点图标显示更多操作
 */
@Composable
fun BookshelfBookGridItem(
    bookWithProgress: BookWithProgress,
    showMoveToBookshelf: Boolean = true,
    onClick: () -> Unit = {},
    onMenuAction: (BookMenuAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val book = bookWithProgress.book
    val progress = bookWithProgress.progress
    val progressPercent = progress?.let { (it.progress * 100).toInt() } ?: 0
    
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 封面
        BookCover(
            coverUrl = book.coverPath,
            title = book.title,
            modifier = Modifier.size(width = 100.dp, height = 140.dp)
        )
        
        // 书名
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 阅读进度文本
        Text(
            text = stringResource(Res.string.reading_progress, progressPercent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 底部行：作者 + 三点菜单
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 作者
            Text(
                text = book.author ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 三点菜单（右下角）
            BookContextMenu(
                showMenu = showMenu,
                showMoveToBookshelf = showMoveToBookshelf,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onMenuAction = { action ->
                    showMenu = false
                    onMenuAction(action)
                }
            )
        }
    }
}

/**
 * 书籍上下文菜单（三点图标 + 下拉菜单）
 */
@Composable
private fun BookContextMenu(
    showMenu: Boolean,
    showMoveToBookshelf: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuAction: (BookMenuAction) -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        // 三点图标
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .noRippleClickable { onShowMenu() },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 下拉菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissMenu
        ) {
            // 书籍详情
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.book_detail)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { onMenuAction(BookMenuAction.Detail) }
            )
            
            // 添加到书架
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.add_to_bookshelves)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LibraryAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { onMenuAction(BookMenuAction.AddToBookshelves) }
            )
            
            // 移动到书架（系统默认书架不显示此选项）
            if (showMoveToBookshelf) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.move_to_bookshelf)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { onMenuAction(BookMenuAction.MoveToBookshelf) }
                )
            }
            
            // 从所有书架移除
            DropdownMenuItem(
                text = { 
                    Text(
                        text = stringResource(Res.string.remove_from_all_bookshelves),
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { onMenuAction(BookMenuAction.RemoveFromAll) }
            )
        }
    }
}

/**
 * 书籍封面
 */
@Composable
private fun BookCover(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // 无封面占位
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式标签
 */
@Composable
private fun FormatTag(
    format: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (format.lowercase()) {
        "epub" -> FormatEpub
        "pdf" -> FormatPdf
        "txt" -> FormatTxt
        "mobi" -> FormatMobi
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = format.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor
        )
    }
}
