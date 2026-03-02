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
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.progress_conflict_title
import app.composeapp.generated.resources.progress_conflict_message
import app.composeapp.generated.resources.reader_local_progress
import app.composeapp.generated.resources.reader_remote_progress
import app.composeapp.generated.resources.use_local_progress
import app.composeapp.generated.resources.use_remote_progress
import app.composeapp.generated.resources.local_progress_info
import app.composeapp.generated.resources.remote_progress_info
import app.composeapp.generated.resources.reader_last_read_at
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            Text(stringResource(Res.string.progress_conflict_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.progress_conflict_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 本地进度
                if (localProgress != null) {
                    ProgressInfoRow(
                        label = stringResource(Res.string.reader_local_progress),
                        progressText = stringResource(Res.string.local_progress_info, localProgress.chapterIndex + 1, (localProgress.progress * 100).toInt()),
                        lastReadAt = formatTimestamp(localProgress.lastReadAt)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 云端进度
                if (remoteProgress != null) {
                    ProgressInfoRow(
                        label = stringResource(Res.string.reader_remote_progress),
                        progressText = stringResource(Res.string.remote_progress_info, remoteProgress.currentPage + 1, (remoteProgress.progress * 100).toInt()),
                        lastReadAt = remoteProgress.lastReadAt
                    )
                }
                }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onUseLocal) {
                    Text(stringResource(Res.string.use_local_progress))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onUseRemote) {
                    Text(stringResource(Res.string.use_remote_progress))
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun ProgressInfoRow(
    label: String,
    progressText: String,
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
            text = progressText,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(Res.string.reader_last_read_at, lastReadAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 格式化时间戳为可读字符串
 */
private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${(local.month.ordinal + 1).toString().padStart(2, '0')}-${local.day.toString().padStart(2, '0')} " +
           "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
