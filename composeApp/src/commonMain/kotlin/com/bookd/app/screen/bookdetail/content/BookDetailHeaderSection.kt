package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.file_size_bytes
import app.composeapp.generated.resources.file_size_gb
import app.composeapp.generated.resources.file_size_kb
import app.composeapp.generated.resources.file_size_mb
import coil3.compose.AsyncImage
import com.bookd.app.data.model.Book
import org.jetbrains.compose.resources.stringResource

/**
 * 书籍详情头部区域
 * 
 * 显示封面、标题、作者信息
 */
@Composable
fun BookDetailHeaderSection(
    book: Book,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // 封面图片
        AsyncImage(
            model = book.coverPath,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 120.dp, height = 160.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 书籍信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 标题
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 作者
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 格式
            Text(
                text = book.format.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 文件大小
            Text(
                text = formatFileSize(book.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 格式化文件大小
 * 
 * 使用 Kotlin 跨平台兼容的方式格式化
 */
@Composable
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> stringResource(Res.string.file_size_bytes, bytes.toInt())
        bytes < 1024 * 1024 -> stringResource(Res.string.file_size_kb, (bytes / 1024).toInt())
        bytes < 1024 * 1024 * 1024 -> stringResource(Res.string.file_size_mb, bytes / (1024.0 * 1024.0))
        else -> stringResource(Res.string.file_size_gb, bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
