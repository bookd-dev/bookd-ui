package com.bookd.app.screen.reader.content.element

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TextStyle as BookdTextStyle

/**
 * 段落渲染组件
 * 
 * 渲染富文本段落，支持：
 * - 首行缩进
 * - 文本样式（粗体、斜体、下划线、删除线、代码）
 * - 脚注标记
 * - 链接
 */
@Composable
fun ParagraphView(
    paragraph: ContentElement.Paragraph,
    settings: ReaderSettings,
    onFootnoteClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    val annotatedString = buildAnnotatedString {
        // 首行缩进：使用两个全角空格
        if (settings.firstLineIndent) {
            append("\u3000\u3000") // 两个全角空格
        }
        
        paragraph.spans.forEach { span ->
            val startIndex = length
            
            // 应用文本样式
            withStyle(
                style = buildSpanStyle(span, settings, onSurfaceColor, surfaceVariantColor, primaryColor)
            ) {
                append(span.text)
            }
            
            // 脚注标记：为包含 footnoteId 的文本添加可点击注解
            if (span.footnoteId != null) {
                addStringAnnotation(
                    tag = "footnote",
                    annotation = span.footnoteId,
                    start = startIndex,
                    end = startIndex + span.text.length
                )
            }
            
            // 链接注解
            if (span.link != null) {
                addStringAnnotation(
                    tag = "link",
                    annotation = span.link,
                    start = startIndex,
                    end = startIndex + span.text.length
                )
            }
        }
    }
    
    // 渲染可点击文本
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = TextStyle(
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineHeight).sp,
            letterSpacing = settings.letterSpacing.sp,
            textAlign = when (settings.textAlign) {
                "justify" -> TextAlign.Justify
                "left" -> TextAlign.Start
                "center" -> TextAlign.Center
                "right" -> TextAlign.End
                else -> TextAlign.Justify
            }
        ),
        onClick = { offset ->
            // 处理脚注点击
            annotatedString.getStringAnnotations("footnote", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onFootnoteClick(annotation.item)
                }
            
            // 处理链接点击
            annotatedString.getStringAnnotations("link", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
        }
    )
}

/**
 * 构建文本片段样式
 */
private fun buildSpanStyle(
    span: TextSpan,
    settings: ReaderSettings,
    textColor: Color,
    codeBackgroundColor: Color,
    primaryColor: Color
): SpanStyle {
    val isBold = span.styles.contains(BookdTextStyle.BOLD)
    val isItalic = span.styles.contains(BookdTextStyle.ITALIC)
    val isCode = span.styles.contains(BookdTextStyle.CODE)
    val hasLink = span.link != null
    val isFootnote = span.footnoteId != null
    
    return SpanStyle(
        fontSize = settings.fontSize.sp,
        fontWeight = if (isBold || isFootnote) FontWeight.Bold else FontWeight(settings.fontWeight),
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = buildTextDecoration(span.styles, hasLink),
        color = if (hasLink || isFootnote) primaryColor else textColor,
        letterSpacing = settings.letterSpacing.sp,
        fontFamily = if (isCode) FontFamily.Monospace else null,
        background = if (isCode) codeBackgroundColor else Color.Unspecified
    )
}

/**
 * 构建文本装饰
 */
private fun buildTextDecoration(styles: List<BookdTextStyle>, hasLink: Boolean): TextDecoration? {
    val decorations = buildList {
        if (styles.contains(BookdTextStyle.UNDERLINE) || hasLink) add(TextDecoration.Underline)
        if (styles.contains(BookdTextStyle.STRIKETHROUGH)) add(TextDecoration.LineThrough)
    }
    return if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
}
