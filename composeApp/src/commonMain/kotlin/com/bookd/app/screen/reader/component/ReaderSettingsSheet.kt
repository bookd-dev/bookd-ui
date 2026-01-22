package com.bookd.app.screen.reader.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.PageAnimationType
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.model.ReaderSettings

/**
 * 阅读器设置面板（BottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Double) -> Unit,
    onParagraphSpacingChange: (Int) -> Unit,
    onMarginHorizontalChange: (Int) -> Unit,
    onMarginVerticalChange: (Int) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onPageAnimationTypeChange: (PageAnimationType) -> Unit,
    onFirstLineIndentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 标题
            Text(
                text = "阅读器设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 字体大小
            SettingSliderRow(
                label = "字体大小",
                value = settings.fontSize.toFloat(),
                valueRange = 12f..32f,
                valueText = "${settings.fontSize}",
                onValueChange = { onFontSizeChange(it.toInt()) }
            )
            
            // 行间距
            SettingSliderRow(
                label = "行间距",
                value = settings.lineHeight.toFloat(),
                valueRange = 1.0f..2.5f,
                valueText = String.format("%.1f", settings.lineHeight),
                onValueChange = { onLineHeightChange(it.toDouble()) }
            )
            
            // 段落间距
            SettingSliderRow(
                label = "段落间距",
                value = settings.paragraphSpacing.toFloat(),
                valueRange = 0f..32f,
                valueText = "${settings.paragraphSpacing}",
                onValueChange = { onParagraphSpacingChange(it.toInt()) }
            )
            
            // 页面边距（水平）
            SettingSliderRow(
                label = "水平边距",
                value = settings.marginHorizontal.toFloat(),
                valueRange = 8f..48f,
                valueText = "${settings.marginHorizontal}",
                onValueChange = { onMarginHorizontalChange(it.toInt()) }
            )
            
            // 页面边距（垂直）
            SettingSliderRow(
                label = "垂直边距",
                value = settings.marginVertical.toFloat(),
                valueRange = 16f..80f,
                valueText = "${settings.marginVertical}",
                onValueChange = { onMarginVerticalChange(it.toInt()) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // 阅读模式
            Text(
                text = "阅读模式",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = settings.pageMode == PageMode.SCROLL,
                    onClick = { onPageModeChange(PageMode.SCROLL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("竖屏滚动")
                }
                SegmentedButton(
                    selected = settings.pageMode == PageMode.PAGE,
                    onClick = { onPageModeChange(PageMode.PAGE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("横屏翻页")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 翻页动画（仅翻页模式）
            if (settings.pageMode == PageMode.PAGE) {
                Text(
                    text = "翻页动画",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = settings.pageAnimationType == PageAnimationType.NATIVE,
                        onClick = { onPageAnimationTypeChange(PageAnimationType.NATIVE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("原生动画")
                    }
                    SegmentedButton(
                        selected = settings.pageAnimationType == PageAnimationType.REALISTIC,
                        onClick = { /* MVP 阶段禁用 */ },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        enabled = false // MVP 阶段禁用
                    ) {
                        Text("仿真动画")
                    }
                }
                
                Text(
                    text = "仿真动画即将推出",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 首行缩进开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "首行缩进",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = settings.firstLineIndent,
                    onCheckedChange = onFirstLineIndentChange
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 设置滑块行
 */
@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
