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

        val (finalWidth, finalHeight) = if (currentY > 0) {
            if (imageHeight <= remainingHeight) {
                imageWidth to imageHeight
            } else {
                val scaledWidth = (availableHeight * aspectRatio).toInt()
                if (scaledWidth >= imageMinScaleWidth) {
                    scaledWidth to availableHeight
                } else {
                    imageWidth to imageHeight
                }
            }
        } else {
            if (imageHeight <= remainingHeight) {
                if (imageHeight <= contentWidth) {
                    imageWidth to imageHeight
                } else {
                    contentWidth to (contentWidth / aspectRatio).toInt()
                }
            } else {
                val scaledWidth = (availableHeight * aspectRatio).toInt()
                if (scaledWidth <= contentWidth) {
                    scaledWidth to availableHeight
                } else {
                    contentWidth to (contentWidth / aspectRatio).toInt()
                }
            }
        }


        return RenderCommand.Image(
            y = y,
            src = element.src,
            width = finalWidth,
            height = finalHeight,
            imageBitmap = null,
            altText = element.alt,
            altTextLayout = altTextLayout
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
        // 图片可用高度：不扣除 alt 文本高度，与 prerender() 保持一致
        val remainingHeight = availableHeight - imageToAltSpacing
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
}
