package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 操作按钮区域
 */
@Composable
fun BookDetailActionSection(
    inDefaultBookshelf: Boolean,
    onStartReading: () -> Unit,
    onToggleDefaultBookshelf: () -> Unit,
    onAddToBookshelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 开始阅读按钮
        Button(
            onClick = onStartReading,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始阅读")
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 收藏按钮（添加/移除默认书架）
        IconButton(onClick = onToggleDefaultBookshelf) {
            Icon(
                imageVector = if (inDefaultBookshelf) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (inDefaultBookshelf) "从书架移除" else "添加到书架",
                tint = if (inDefaultBookshelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 添加到其他书架按钮
        FilledTonalButton(onClick = onAddToBookshelf) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("书架")
        }
    }
}
