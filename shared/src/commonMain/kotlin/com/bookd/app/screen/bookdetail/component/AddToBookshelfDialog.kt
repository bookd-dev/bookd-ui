package com.bookd.app.screen.bookdetail.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.add_to_bookshelves_title
import app.composeapp.generated.resources.books_count
import app.composeapp.generated.resources.cancel
import app.composeapp.generated.resources.confirm
import app.composeapp.generated.resources.no_bookshelves_create_first
import app.composeapp.generated.resources.select_bookshelves_hint
import com.bookd.app.data.model.Bookshelf
import org.jetbrains.compose.resources.stringResource

/**
 * 添加到书架对话框
 */
@Composable
fun AddToBookshelfDialog(
    allBookshelves: List<Bookshelf>,
    currentBookshelves: List<Bookshelf>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    // 已选中的书架 ID 列表
    val selectedIds = remember(allBookshelves, currentBookshelves) {
        mutableStateListOf<Int>().apply {
            // 默认选中当前已加入的书架
            addAll(currentBookshelves.map { it.id })
        }
    }
    
    // 过滤掉系统默认书架（通过收藏按钮管理）
    val availableBookshelves = allBookshelves.filter { !it.isSystemDefault }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.add_to_bookshelves_title))
        },
        text = {
            Column {
                if (availableBookshelves.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.no_bookshelves_create_first),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.select_bookshelves_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = availableBookshelves,
                            key = { it.id }
                        ) { bookshelf ->
                            BookshelfCheckItem(
                                bookshelf = bookshelf,
                                isChecked = selectedIds.contains(bookshelf.id),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedIds.contains(bookshelf.id)) {
                                            selectedIds.add(bookshelf.id)
                                        }
                                    } else {
                                        selectedIds.remove(bookshelf.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                TextButton(
                    onClick = { 
                        // 只传递新增的书架 ID
                        val currentIds = currentBookshelves.map { it.id }.toSet()
                        val newIds = selectedIds.filter { it !in currentIds }
                        if (newIds.isNotEmpty()) {
                            onConfirm(newIds)
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = availableBookshelves.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun BookshelfCheckItem(
    bookshelf: Bookshelf,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookshelf.name,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (!bookshelf.description.isNullOrBlank()) {
                Text(
                    text = bookshelf.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 书籍数量
        Text(
            text = stringResource(Res.string.books_count, bookshelf.bookCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
