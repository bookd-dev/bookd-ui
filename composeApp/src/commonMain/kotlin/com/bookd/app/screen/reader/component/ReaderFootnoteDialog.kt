package com.bookd.app.screen.reader.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextStyle

/**
 * 脚注弹窗组件
 */
@Composable
fun ReaderFootnoteDialog(
    footnote: ContentElement.Footnote,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("注释 ${footnote.footnoteSpan?.text ?: ""}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 渲染脚注内容（支持富文本）
                val annotatedString = buildAnnotatedString {
                    footnote.contentSpans.forEach { span ->
                        val isBold = span.styles.contains(TextStyle.BOLD)
                        val isItalic = span.styles.contains(TextStyle.ITALIC)
                        val hasUnderline = span.styles.contains(TextStyle.UNDERLINE)
                        val hasStrikethrough = span.styles.contains(TextStyle.STRIKETHROUGH)
                        
                        withStyle(
                            SpanStyle(
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = when {
                                    hasUnderline && hasStrikethrough -> TextDecoration.combine(
                                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                                    )
                                    hasUnderline -> TextDecoration.Underline
                                    hasStrikethrough -> TextDecoration.LineThrough
                                    else -> null
                                }
                            )
                        ) {
                            append(span.text)
                        }
                    }
                }
                
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
