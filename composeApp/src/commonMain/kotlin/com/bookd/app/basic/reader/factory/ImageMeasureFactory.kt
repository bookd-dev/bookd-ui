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
        // 计算alt文本的高度
        val altTextHeight = calculateAltTextHeight(element.alt)
        
        // 根据是否有已使用内容，选择不同的计算策略
        return if (usedHeight > 0) {
            calculateForUsedHeight(element, altTextHeight, availableHeight)
        } else {
            calculateForEmptyPage(element, altTextHeight, availableHeight)
        }
    }
    
    /**
     * 处理页面已有内容的情况 (usedH > 0)
     */
    private fun calculateForUsedHeight(
        element: ContentElement.Image, 
        altTextHeight: Int, 
        availableHeight: Int
    ): MeasureResult {
        // 页面可用高度为 remainingH = availableH - lineSpacing - altMeasureH - altSpacing
        val remainingHeight = availableHeight - lineSpacing - altTextHeight - imageToAltSpacing
        if (remainingHeight <= 0) {
            // 剩余空间不够，直接换页
            return MeasureResult.NEXT
        }
        
        // 获取图片信息
        val (imageWidth, imageHeight, aspectRatio) = getImageInfo(element, remainingHeight)
        
        // 1. 如果 imageH < remainingH，页面满足图片需求
        if (imageHeight <= remainingHeight) {
            val totalHeight = imageHeight + imageToAltSpacing + altTextHeight
            return MeasureResult(totalHeight, false, 0)
        }
        
        // 2. 如果 imageH > remainingH，尝试按比例缩放
        val scaledWidth = (remainingHeight * aspectRatio).toInt()
        if (scaledWidth >= imageMinScaleWidth) {
            // 新的宽度满足最小宽度要求，使用缩放后的尺寸
            val totalHeight = remainingHeight + imageToAltSpacing + altTextHeight
            return MeasureResult(totalHeight, false, 0)
        }
        
        // 缩放后宽度太小，换页
        return MeasureResult(0, true, 0)
    }
    
    /**
     * 处理页面空白的情况 (usedH = 0)
     */
    private fun calculateForEmptyPage(
        element: ContentElement.Image, 
        altTextHeight: Int, 
        availableHeight: Int
    ): MeasureResult {
        // 页面可用高度为 remainingH = availableH - altMeasureH - altSpacing (不需要减行间距)
        val remainingHeight = availableHeight - altTextHeight - imageToAltSpacing
        if (remainingHeight <= 0) {
            // 剩余空间不够，直接跳过, 不应该存在
            return MeasureResult.SKIP
        }
        
        // 获取图片信息
        val (imageWidth, imageHeight, aspectRatio) = getImageInfo(element, remainingHeight)
        
        // 1. 如果 imageH <= remainingH
        if (imageHeight <= remainingHeight) {
            if (imageHeight <= contentWidth) {
                // 图片高度小于等于页面宽度，直接使用
                val totalHeight = imageHeight + imageToAltSpacing + altTextHeight
                return MeasureResult(totalHeight, false, 0)
            } else {
                // 图片高度大于页面宽度，按比例缩放到页面宽度
                val scaledHeight = (contentWidth / aspectRatio).toInt()
                val totalHeight = scaledHeight + imageToAltSpacing + altTextHeight
                return MeasureResult(totalHeight, false, 0)
            }
        }
        
        // 2. 如果 imageH > remainingH，尝试按比例缩放
        val scaledWidth = (remainingHeight * aspectRatio).toInt()
        if (scaledWidth <= contentWidth) {
            // 缩放后的宽度小于等于页面宽度，使用缩放后的尺寸
            val totalHeight = remainingHeight + imageToAltSpacing + altTextHeight
            return MeasureResult(totalHeight, false, 0)
        } else {
            // 缩放后的宽度仍然大于页面宽度，按页面宽度再缩放一次
            val scaledHeight = (contentWidth / aspectRatio).toInt()
            val totalHeight = scaledHeight + imageToAltSpacing + altTextHeight
            return MeasureResult(totalHeight, false, 0)
        }
    }
    
    /**
     * 计算alt文本的显示高度
     */
    private fun calculateAltTextHeight(alt: String?): Int {
        if (alt.isNullOrEmpty()) {
            return 0
        }
        
        val textLayoutResult = textMeasurer.measure(
            text = alt,
            style = styleController.textStyles.imageAlternateTextStyle,
            constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = contentWidth
            )
        )
        
        return textLayoutResult.size.height
    }
    
    /**
     * 获取图片的宽度、高度和宽高比
     *
     * 1. 宽、高、宽高比，直接返回
     * 2. 宽、高，计算出宽高比返回
     * 3. 宽 or 高、宽高比，计算出另外一个边返回
     * 4. 只有宽高比, 用contentW计算出高度，返回
     * 5. 什么都没有，用剩余高度+contentW返回
     */
    private fun getImageInfo(image: ContentElement.Image, availableHeight: Int): Triple<Int, Int, Double> {
        // 优先级1: 宽高， 宽高比都有
        if (image.width != null && image.height != null && image.aspectRatio != null) {
            return Triple(image.width, image.height, image.aspectRatio)
        }

        // 优先级2: 如果同时有宽高，直接使用
        if (image.width != null && image.height != null) {
            val aspectRatio = image.width.toDouble() / image.height
            return Triple(image.width, image.height, aspectRatio)
        }

        // 优先级3: 宽高有任意一个，宽高比有
        if ((image.width != null || image.height != null) && (image.aspectRatio != null)) {
            val width: Int = when {
                image.width != null -> image.width
                image.height != null -> (image.height / image.aspectRatio).toInt()
                else -> 0
            }
            val height: Int = when {
                image.height != null -> image.height
                image.width != null -> (image.width / image.aspectRatio).toInt()
                else -> 0
            }
            return Triple(width, height, image.aspectRatio)
        }

        // 优先级4: 只有宽高比
        if (image.aspectRatio != null) {
            val (width, height) = if (image.aspectRatio > 1) {
                // 横屏图片：基于宽度计算高度
                contentWidth to (contentWidth / image.aspectRatio).toInt()
            } else {
                // 竖屏图片：基于高度计算宽度
                (availableHeight * image.aspectRatio).toInt() to availableHeight
            }
            return Triple(width, height, image.aspectRatio)
        }

        //优先级5: 用contentWidth + availableHeight
        val width = contentWidth
        val aspectRatio = width.toDouble() / availableHeight
        return Triple(width, availableHeight, aspectRatio)
    }
}