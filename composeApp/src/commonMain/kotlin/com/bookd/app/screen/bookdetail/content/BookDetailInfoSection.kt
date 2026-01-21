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
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.chapters_value
import app.composeapp.generated.resources.chapters_label
import app.composeapp.generated.resources.detail_info
import app.composeapp.generated.resources.image_count_label
import app.composeapp.generated.resources.images_value
import app.composeapp.generated.resources.isbn_label
import app.composeapp.generated.resources.publisher_label
import app.composeapp.generated.resources.word_count_label
import app.composeapp.generated.resources.word_count_value
import app.composeapp.generated.resources.word_count_wan
import app.composeapp.generated.resources.word_count_yi
import com.bookd.app.data.model.Book
import org.jetbrains.compose.resources.stringResource

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
                text = stringResource(Res.string.detail_info),
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
                InfoRow(label = stringResource(Res.string.publisher_label), value = book.publisher)
            }
            
            // ISBN
            if (!book.isbn.isNullOrBlank()) {
                InfoRow(label = stringResource(Res.string.isbn_label), value = book.isbn)
            }
            
            // 章节数
            if (book.chapterCount > 0) {
                InfoRow(
                    label = stringResource(Res.string.chapters_label), 
                    value = stringResource(Res.string.chapters_value, book.chapterCount)
                )
            }
            
            // 总字数
            if (book.totalWordCount > 0) {
                InfoRow(
                    label = stringResource(Res.string.word_count_label), 
                    value = formatWordCount(book.totalWordCount)
                )
            }
            
            // 图片数
            if (book.totalImageCount > 0) {
                InfoRow(
                    label = stringResource(Res.string.image_count_label), 
                    value = stringResource(Res.string.images_value, book.totalImageCount)
                )
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
@Composable
private fun formatWordCount(count: Int): String {
    return when {
        count < 10000 -> stringResource(Res.string.word_count_value, count)
        count < 100000000 -> {
            val wan = count / 10000.0
            val formatted = ((wan * 10).toLong() / 10.0).toString()
            stringResource(Res.string.word_count_wan, formatted)
        }
        else -> {
            val yi = count / 100000000.0
            val formatted = ((yi * 10).toLong() / 10.0).toString()
            stringResource(Res.string.word_count_yi, formatted)
        }
    }
}
