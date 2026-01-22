package com.bookd.app.screen.reader.content.element

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

/**
 * 脚注区块渲染组件
 * 
 * 用于渲染独立的脚注区块（通常脚注内容在弹窗中显示）
 */
@Composable
fun FootnoteView(
    footnote: ContentElement.Footnote,
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${footnote.id}]",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = (settings.fontSize - 2).sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(end = 4.dp)
        )
        
        // 脚注内容
        Column(modifier = Modifier.weight(1f)) {
            footnote.spans.forEach { span ->
                Text(
                    text = span.text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (settings.fontSize - 2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
