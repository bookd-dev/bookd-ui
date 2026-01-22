package com.bookd.app.screen.reader.content.element

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

/**
 * 引用块渲染组件
 */
@Composable
fun QuoteView(
    quote: ContentElement.Quote,
    settings: ReaderSettings,
    onFootnoteClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp)
    ) {
        // 左侧竖线
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 引用内容（复用段落渲染，字体稍小，无首行缩进）
        ParagraphView(
            paragraph = ContentElement.Paragraph(quote.spans),
            settings = settings.copy(
                fontSize = settings.fontSize - 1,
                firstLineIndent = false
            ),
            onFootnoteClick = onFootnoteClick,
            onLinkClick = onLinkClick,
            modifier = Modifier.weight(1f)
        )
    }
}
