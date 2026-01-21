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
import com.bookd.app.data.model.Bookshelf

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
            Text("添加到书架")
        },
        text = {
            Column {
                if (availableBookshelves.isEmpty()) {
                    Text(
                        text = "暂无可用书架，请先创建书架",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "选择要添加到的书架",
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
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
            text = "${bookshelf.bookCount} 本",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
