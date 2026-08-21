package com.bookd.app.screen.bookdetail.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.Bookshelf

/**
 * 所在书架区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailShelvesSection(
    bookshelves: List<Bookshelf>,
    onRemoveFromBookshelf: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "所在书架",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bookshelves.forEach { bookshelf ->
                FilterChip(
                    selected = true,
                    onClick = { /* 可以跳转到书架详情 */ },
                    label = { Text(bookshelf.name) },
                    trailingIcon = if (!bookshelf.isSystemDefault) {
                        {
                            IconButton(
                                onClick = { onRemoveFromBookshelf(bookshelf.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "从此书架移除",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else null
                )
            }
        }
    }
}
