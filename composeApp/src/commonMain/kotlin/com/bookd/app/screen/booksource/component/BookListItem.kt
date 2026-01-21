package com.bookd.app.screen.booksource.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bookd.app.data.model.Book
import com.bookd.app.ui.theme.FormatEpub
import com.bookd.app.ui.theme.FormatMobi
import com.bookd.app.ui.theme.FormatPdf
import com.bookd.app.ui.theme.FormatTxt
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.chapters_count
import org.jetbrains.compose.resources.stringResource

/**
 * 书籍列表项
 * 
 * 显示书籍封面、标题、作者、格式等信息
 */
@Composable
fun BookListItem(
    book: Book,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
