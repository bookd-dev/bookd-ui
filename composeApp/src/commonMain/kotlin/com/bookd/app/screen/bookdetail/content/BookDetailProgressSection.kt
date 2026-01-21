package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ReadingProgressResponse

/**
 * 阅读进度区域
 */
@Composable
fun BookDetailProgressSection(
    progress: ReadingProgressResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "阅读进度",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress.progress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 页码信息
            if (progress.totalPages != null && progress.totalPages > 0) {
                Text(
                    text = "第 ${progress.currentPage} 页 / 共 ${progress.totalPages} 页",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 上次阅读时间
            Text(
                text = "上次阅读: ${formatLastReadTime(progress.lastReadAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 格式化上次阅读时间
 */
private fun formatLastReadTime(isoTime: String): String {
    // 简单处理，实际项目中可使用 kotlinx-datetime 进行格式化
    return try {
        isoTime.substringBefore('T').replace('-', '/')
    } catch (e: Exception) {
        isoTime
    }
}
