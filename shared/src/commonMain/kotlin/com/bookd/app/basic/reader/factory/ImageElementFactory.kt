package com.bookd.app.basic.reader.factory

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement


/**
 * 图片测量工厂。
 *
 * 图片会按原始宽高比收敛到内容区域宽度和当前页剩余高度内。页面已有内容时，如果
 * 图片必须缩得小于最小可读宽度，则推迟到下一页渲染。
 */
class ImageElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) : IContentMeasureFactory<ContentElement.Image, RenderCommand.Image> {

    // 行间距
    private val lineSpacing = styleController.sizeStyles.getLineSpacingPx(density)
    
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
            calculateForUsedHeight(elements, element, altTextHeight, usedHeight, availableHeight)
        } else {
            calculateForEmptyPage(element, altTextHeight, availableHeight)
        }
    }

    override fun prerender(
        elements: List<ContentElement>,
        element: ContentElement.Image,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): RenderCommand.Image {
        var y = currentY

        if (shouldAddTopSpacing(elements, element, y)) {
            y += lineSpacing
        }

        val altTextLayout = if (!element.alt.isNullOrEmpty()) {
            textMeasurer.measure(
                text = element.alt,
                style = styleController.textStyles.imageAlternateTextStyle,
                constraints = Constraints(maxWidth = contentWidth)
            )
        } else {
            null
        }

        val altTextHeight = altTextLayout?.size?.height ?: 0
        // 图片可用高度：不扣除 alt 文本高度，避免 alt 文本过长时压缩图片尺寸
        // measure() 阶段已保证分页正确，prerender() 只需按最佳尺寸渲染图片
        val remainingHeight = contentHeight - y - imageToAltSpacing
        val availableHeight = if (remainingHeight > 0) remainingHeight else 1

        val (imageWidth, imageHeight, aspectRatio) = getImageInfo(element, availableHeight)
        val (finalWidth, finalHeight) = fitImageWithinBounds(
            width = imageWidth,
            height = imageHeight,
            aspectRatio = aspectRatio,
            maxWidth = contentWidth,
            maxHeight = availableHeight
        )


        return RenderCommand.Image(
            y = y,
            src = element.src,
            width = finalWidth,
            height = finalHeight,
            imageBitmap = null,
            altText = element.alt,
            altTextLayout = altTextLayout,
            altSpacing = if (altTextLayout != null) imageToAltSpacing else 0
        )
    }

    override fun draw(
        drawScope: DrawScope,
        imagePainters: Map<String, AsyncImagePainter>,
        command: RenderCommand.Image
    ) {
        var y = command.y.toFloat()
        val imageOffsetX = (contentWidth - command.width) / 2f

        // 绘制图片：优先使用 imagePainters（Coil AsyncImagePainter），回退到 imageBitmap
        val painter = imagePainters[command.src]
        if (painter != null) {
            drawScope.translate(left = imageOffsetX, top = y) {
                with(painter) {
                    draw(Size(command.width.toFloat(), command.height.toFloat()))
                }
            }
            y += command.height.toFloat()
        } else if (command.imageBitmap != null) {
            drawScope.drawImage(
                image = command.imageBitmap,
                dstOffset = IntOffset(imageOffsetX.toInt(), command.y),
                dstSize = IntSize(command.width, command.height)
            )
            y += command.height.toFloat()
        }

        // 绘制 alt 文本
        if (command.altTextLayout != null) {
            drawScope.drawText(
                textLayoutResult = command.altTextLayout,
                topLeft = Offset((contentWidth - command.altTextLayout.size.width) / 2f, y + imageToAltSpacing),
            )
        }
    }

    /**
     * 处理页面已有内容的情况 (usedH > 0)
     */
    private fun calculateForUsedHeight(
        elements: List<ContentElement>,
        element: ContentElement.Image, 
        altTextHeight: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {

        // 图片可用高度：不扣除 alt 文本高度，与 prerender() 保持一致
        // alt 文本高度只在计算 totalHeight 时加回
        val remainingHeight = availableHeight -
                imageToAltSpacing -
                if (shouldAddTopSpacing(elements, element, usedHeight)) {
                    lineSpacing
                } else {
                    0
                }
        if (remainingHeight <= 0) {
            // 剩余空间不够，直接换页
            return MeasureResult.NEXT
        }
        
        val (imageWidth, imageHeight, aspectRatio) = getImageInfo(element, remainingHeight)
        val (finalWidth, finalHeight) = fitImageWithinBounds(
            width = imageWidth,
            height = imageHeight,
            aspectRatio = aspectRatio,
            maxWidth = contentWidth,
            maxHeight = remainingHeight
        )
        
        // 1. 如果 imageH < remainingH，页面满足图片需求。宽度仍需受内容区域约束。
        if (imageHeight <= remainingHeight) {
            val totalHeight = finalHeight + imageToAltSpacing + altTextHeight
            return MeasureResult(totalHeight, false, 0)
        }
        
        // 2. 如果 imageH > remainingH，尝试按比例缩放
        if (finalWidth >= imageMinScaleWidth) {
            // 新的宽度满足最小宽度要求，使用缩放后的尺寸
            val totalHeight = finalHeight + imageToAltSpacing + altTextHeight
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
        // 图片可用高度：不扣除 alt 文本高度，与 prerender() 保持一致
        val remainingHeight = availableHeight - imageToAltSpacing
        if (remainingHeight <= 0) {
            // 剩余空间不够，直接跳过, 不应该存在
            return MeasureResult.SKIP
        }
        
        val (imageWidth, imageHeight, aspectRatio) = getImageInfo(element, remainingHeight)
        val (_, finalHeight) = fitImageWithinBounds(
            width = imageWidth,
            height = imageHeight,
            aspectRatio = aspectRatio,
            maxWidth = contentWidth,
            maxHeight = remainingHeight
        )
        val totalHeight = finalHeight + imageToAltSpacing + altTextHeight
        return MeasureResult(totalHeight, false, 0)
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
            constraints = Constraints(
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

    private fun fitImageWithinBounds(
        width: Int,
        height: Int,
        aspectRatio: Double,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val safeMaxWidth = maxWidth.coerceAtLeast(1)
        val safeMaxHeight = maxHeight.coerceAtLeast(1)
        val safeAspectRatio = when {
            aspectRatio.isFinite() && aspectRatio > 0.0 -> aspectRatio
            else -> safeWidth.toDouble() / safeHeight
        }
        val scale = minOf(
            safeMaxWidth.toDouble() / safeWidth,
            safeMaxHeight.toDouble() / safeHeight,
            1.0
        )
        val fittedWidth = (safeWidth * scale).toInt().coerceIn(1, safeMaxWidth)
        val fittedHeight = (fittedWidth / safeAspectRatio).toInt().coerceIn(1, safeMaxHeight)
        return fittedWidth to fittedHeight
    }
}
