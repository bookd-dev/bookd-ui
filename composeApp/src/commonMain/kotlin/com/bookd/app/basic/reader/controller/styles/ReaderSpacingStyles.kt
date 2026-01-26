package com.bookd.app.basic.reader.controller.styles

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ReaderSettings

class ReaderSpacingStyles(private val settings: ReaderSettings) {

    /**
     * Heading 的顶部间距
     */
    fun getHeadingTopSpacing(level: Int): Int {
        // 标题级别越高，顶部间距越大
        val baseSpacing = settings.paragraphSpacing
        return when (level) {
            1 -> (baseSpacing * 2.5).toInt()
            2 -> (baseSpacing * 2f).toInt()
            3 -> (baseSpacing * 1.5).toInt()
            else -> baseSpacing
        }
    }

    /**
     * Heading 的底部间距
     */
    fun getHeadingBottomSpacing(level: Int): Int {
        val baseSpacing = settings.paragraphSpacing
        return when (level) {
            1 -> (baseSpacing * 1.5).toInt()
            2 -> baseSpacing
            else -> (baseSpacing * 0.8).toInt()
        }
    }

    /**
     * 获取行间距 spacing, 单位px
     */
    fun getLineSpacingPx(density: Density): Int {
        val baseSpacing = settings.paragraphSpacing
        return with(density) { baseSpacing.dp.roundToPx() }
    }

    /**
     * 获取内容宽度
     */
    fun getContentWidth(width: Int, density: Density): Int {
        val horizontalSpacing = settings.marginHorizontal
        return width - with(density) { (horizontalSpacing * 2).dp.roundToPx() }
    }

    /**
     * 获取内容高度
     */
    fun getContentHeight(height: Int, density: Density): Int {
        val verticalSpacing = settings.marginVertical
        return height - with(density) { (verticalSpacing * 2).dp.roundToPx() }
    }
}