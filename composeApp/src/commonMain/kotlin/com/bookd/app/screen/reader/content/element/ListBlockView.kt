package com.bookd.app.screen.reader.content.element

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

/**
 * 列表块渲染组件
 */
@Composable
fun ListBlockView(
    list: ContentElement.ListBlock,
    settings: ReaderSettings,
    onFootnoteClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        list.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 16.dp)
            ) {
                // 列表标记
                Text(
                    text = if (list.ordered) "${index + 1}." else "•",
                    modifier = Modifier
                        .width(24.dp)
                        .padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = settings.fontSize.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 列表项内容
                ParagraphView(
                    paragraph = ContentElement.Paragraph(item.spans),
                    settings = settings.copy(firstLineIndent = false), // 列表项不缩进
                    onFootnoteClick = onFootnoteClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
