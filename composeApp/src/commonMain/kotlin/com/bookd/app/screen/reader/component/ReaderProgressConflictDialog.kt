package com.bookd.app.screen.reader.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.LocalReadingProgress
import com.bookd.app.data.model.ReadingProgressResponse

/**
 * 阅读进度冲突对话框
 */
@Composable
fun ReaderProgressConflictDialog(
    localProgress: LocalReadingProgress?,
    remoteProgress: ReadingProgressResponse?,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("检测到云端阅读进度")
        },
        text = {
            Column {
                Text(
                    text = "发现本地和云端的阅读进度不一致，请选择要使用的进度：",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 本地进度
                if (localProgress != null) {
                    ProgressInfoRow(
                        label = "本地进度",
                        chapterIndex = localProgress.chapterIndex,
                        progress = localProgress.progress,
                        lastReadAt = formatTimestamp(localProgress.lastReadAt)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 云端进度
                if (remoteProgress != null) {
                    ProgressInfoRow(
                        label = "云端进度",
                        chapterIndex = remoteProgress.currentPage,
                        progress = remoteProgress.progress,
                        lastReadAt = remoteProgress.lastReadAt
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onUseLocal) {
                    Text("保持本地")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onUseRemote) {
                    Text("使用云端")
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun ProgressInfoRow(
    label: String,
    chapterIndex: Int,
    progress: Double,
    lastReadAt: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "第 ${chapterIndex + 1} 章 (${(progress * 100).toInt()}%)",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "最后阅读: $lastReadAt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 格式化时间戳为可读字符串
 */
private fun formatTimestamp(timestamp: Long): String {
    // 简单格式化，MVP 阶段使用简化实现
    val seconds = timestamp / 1000
    val minutes = (seconds / 60) % 60
    val hours = (seconds / 3600) % 24
    val days = seconds / 86400
    
    // 计算大致日期（从 1970-01-01 开始）
    val totalDays = days.toInt()
    var year = 1970
    var remainingDays = totalDays
    
    while (true) {
        val daysInYear = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }
    
    val daysInMonths = intArrayOf(31, if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (i in daysInMonths.indices) {
        if (remainingDays < daysInMonths[i]) break
        remainingDays -= daysInMonths[i]
        month++
    }
    val day = remainingDays + 1
    
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
            "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
