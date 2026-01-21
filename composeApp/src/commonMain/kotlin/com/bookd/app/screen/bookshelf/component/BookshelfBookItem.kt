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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.chapters_count
import app.composeapp.generated.resources.reading_progress
import coil3.compose.AsyncImage
import com.bookd.app.data.model.BookWithProgress
import org.jetbrains.compose.resources.stringResource

/**
 * 书架书籍列表项（列表模式）
 * 
 * 显示书籍封面、标题、作者、格式、阅读进度等信息
 */
@Composable
fun BookshelfBookListItem(
    bookWithProgress: BookWithProgress,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val book = bookWithProgress.book
    val progress = bookWithProgress.progress
    val progressPercent = progress?.let { (it.progress * 100).toInt() } ?: 0
    
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
            
            // 底部信息行：格式 + 章节数
            Row(
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
            }
        }
    }
}

/**
 * 书架书籍卡片（瀑布流/网格模式）
 * 
 * 显示封面、书名、阅读进度、作者
 */
@Composable
fun BookshelfBookGridItem(
    bookWithProgress: BookWithProgress,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val book = bookWithProgress.book
    val progress = bookWithProgress.progress
    val progressPercent = progress?.let { (it.progress * 100).toInt() } ?: 0
    
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
        
        // 作者
        book.author?.let { author ->
            Text(
                text = author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
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
        "epub" -> Color(0xFF4CAF50)
        "pdf" -> Color(0xFFE91E63)
        "txt" -> Color(0xFF2196F3)
        "mobi" -> Color(0xFFFF9800)
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
