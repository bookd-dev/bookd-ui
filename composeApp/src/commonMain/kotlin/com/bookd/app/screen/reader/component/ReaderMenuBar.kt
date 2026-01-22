package com.bookd.app.screen.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 阅读器底部菜单栏
 * 
 * 显示：
 * - 上一章/下一章按钮
 * - 章节进度滑块
 * - 目录按钮
 * - 设置按钮
 */
@Composable
fun ReaderMenuBar(
    currentChapter: Int,
    totalChapters: Int,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapterSeek: (Int) -> Unit,
    onTocClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(currentChapter) { 
        mutableFloatStateOf(currentChapter.toFloat()) 
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            // 章节进度滑块行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一章按钮
                TextButton(
                    onClick = onPreviousChapter,
                    enabled = hasPreviousChapter
                ) {
                    Text("上一章")
                }
                
                // 进度滑块
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        onChapterSeek(sliderPosition.toInt())
                    },
                    valueRange = 0f..(totalChapters - 1).coerceAtLeast(0).toFloat(),
                    steps = (totalChapters - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f)
                )
                
                // 下一章按钮
                TextButton(
                    onClick = onNextChapter,
                    enabled = hasNextChapter
                ) {
                    Text("下一章")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 底部按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 目录按钮
                TextButton(onClick = onTocClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("目录")
                }
                
                // 设置按钮
                TextButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("设置")
                }
            }
        }
    }
}
