package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.Tag

/**
 * 标签区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailTagsSection(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onTagClick: (Tag) -> Unit = {}
) {
    Column(modifier = modifier) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onTagClick(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
    }
}
