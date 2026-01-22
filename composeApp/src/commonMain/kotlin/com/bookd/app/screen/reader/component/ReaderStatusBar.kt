package com.bookd.app.screen.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.basic.extension.getCurrentTimeString
import kotlinx.coroutines.delay

/**
 * 阅读器底部状态栏
 * 
 * 显示：
 * - 当前页/总页数
 * - 当前时间
 * 
 * 注意：电量显示需要平台特定实现，MVP 阶段暂不实现
 */
@Composable
fun ReaderStatusBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }
    
    // 每分钟更新时间
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(60_000) // 1 分钟
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 页码
            Text(
                text = "${currentPage + 1}/$totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 时间
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

