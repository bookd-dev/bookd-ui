package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.Book

/**
 * 书籍详细信息区域
 */
@Composable
fun BookDetailInfoSection(
    book: Book,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 描述
            if (!book.description.isNullOrBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            // 出版社
            if (!book.publisher.isNullOrBlank()) {
                InfoRow(label = "出版社", value = book.publisher)
            }
            
            // ISBN
            if (!book.isbn.isNullOrBlank()) {
                InfoRow(label = "ISBN", value = book.isbn)
            }
            
            // 章节数
            if (book.chapterCount > 0) {
                InfoRow(label = "章节数", value = "${book.chapterCount} 章")
            }
            
            // 总字数
            if (book.totalWordCount > 0) {
                InfoRow(label = "总字数", value = formatWordCount(book.totalWordCount))
            }
            
            // 图片数
            if (book.totalImageCount > 0) {
                InfoRow(label = "图片数", value = "${book.totalImageCount} 张")
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.7f)
        )
    }
}

/**
 * 格式化字数
 */
private fun formatWordCount(count: Int): String {
    return when {
        count < 10000 -> "$count 字"
        count < 100000000 -> String.format("%.1f 万字", count / 10000.0)
        else -> String.format("%.1f 亿字", count / 100000000.0)
    }
}
