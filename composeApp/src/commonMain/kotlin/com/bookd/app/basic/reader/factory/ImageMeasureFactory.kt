package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.data.model.ContentElement


/**
 * 图片测量工厂，它符合如下原则:
 * 行高: lineSpacing
 * 页面宽度、剩余高度、已使用高度: contentW、availableH、usedH
 * alt 文本测量高度、文本和图片之间的间隔: altMeasureH、altSpacing (altSpacing=lineSpacing/2)
 * 图片宽高、宽高比: imageW, imageH, imageAspectRatio
 * 图片有个最小宽度: imageMinW (只在页面非以当前图片为开始是使用, imageMinW = contentW * 2 / 3)
 *
 *
 * # usedH > 0:
 * 页面可用高度为 remainingH = availableH - lineSpacing - altMeasureH - altSpacing, 如果 remainingH <= 0，直接换页面
 * 1. 如果 imageH < remainingH, 页面满足图片需求
 * 2. 如果 imageH > remainingH, 尝试用 remainingH + imageAspectRatio 计算出新的 imageW
 *    - 如果新的imageH >= imageMinW， 当前页面满足图片需求
 *    - 如果新的imageH < imageMinW, 页面不满足需求换到下一页
 *
 * # usedH = 0
 * 页面可用高度为 remainingH = availableH - altMeasureH - altSpacing (不需要减行间距)
 * 1. 如果 imageH <= remainingH
 *    - 如果 imageH <= contentW, 页面满足图片需求
 *    - 如果 imageH > contentW, 图片按照 contentW + imageAspectRatio 重新计算 imageH, 用 contentW + 新imageH 作为图片占用大小
 * 2. 如果 imageH > remainingH, 尝试用 remainingH + imageAspectRatio 计算出新的 imageW
 *    - 如果新的 imageW <= contentW, 页面满足图片需求
 *    - 如果新的 imageW > contentW, 用 contentW + imageAspectRatio 重新计算出新的 imageH, 用 contentW + 新imageH 作为图片占用大小
 *
 * # 特殊情况 1. 图片宽 or 高无法获取，宽高比可以获取，用 contentW / remainingH + 宽高比计算占用
 * # 特殊情况 2. 图片只有宽 and 高，自己计算宽高比，再走上面 used > 0 / used = 0 的逻辑
 * # 特殊清空 3. 图片宽、高、宽高比都无法获取, 用 remainingH + contentW 作为图片占用
 */
class ImageMeasureFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density
) : IContentElementFactory<ContentElement.Image> {

    // 行间距
    private val lineSpacing = styleController.spacingStyles.getLineSpacingPx(density)
    
    // 图片和alt文本的间距，使用行间距的一半
    private val imageToAltSpacing = lineSpacing / 2

    // 图片最小允许按照比例缩放的宽度, 目前是内容宽度的 2/3
    private val imageMinScaleWidth = (contentWidth * 2 / 3f).toInt()

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Image,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {

    }
}