package com.bookd.app.screen.reader.content.element

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

/**
 * 标题渲染组件
 */
@Composable
fun HeadingView(
    heading: ContentElement.Heading,
    settings: ReaderSettings,
    modifier: Modifier = Modifier
) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        5 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    
    Text(
        text = heading.text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        style = style.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}
