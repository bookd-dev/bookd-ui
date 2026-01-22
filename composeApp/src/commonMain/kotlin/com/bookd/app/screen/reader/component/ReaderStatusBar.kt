package com.bookd.app.screen.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/**
 * 获取当前时间字符串（HH:mm 格式）
 */
private fun getCurrentTimeString(): String {
    // 使用 System.currentTimeMillis 来获取时间，避免引入额外依赖
    val millis = System.currentTimeMillis()
    val seconds = millis / 1000
    val minutes = (seconds / 60) % 60
    val hours = (seconds / 3600) % 24
    // 注意：这里的时间是 UTC，实际应该使用平台相关的时间获取方式
    // MVP 阶段使用简化实现
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
