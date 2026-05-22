package com.bookd.app.screen.bookshelf.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.add_to_bookshelves_title
import app.composeapp.generated.resources.bookshelf_create
import app.composeapp.generated.resources.bookshelf_delete
import app.composeapp.generated.resources.bookshelf_delete_confirm
import app.composeapp.generated.resources.bookshelf_delete_info
import app.composeapp.generated.resources.bookshelf_delete_note
import app.composeapp.generated.resources.bookshelf_description
import app.composeapp.generated.resources.bookshelf_edit
import app.composeapp.generated.resources.bookshelf_name
import app.composeapp.generated.resources.bookshelf_system_default_readonly
import app.composeapp.generated.resources.cancel
import app.composeapp.generated.resources.confirm
import app.composeapp.generated.resources.move_to_bookshelf_description
import app.composeapp.generated.resources.move_to_bookshelf_title
import app.composeapp.generated.resources.no_available_bookshelves
import app.composeapp.generated.resources.no_other_bookshelves
import app.composeapp.generated.resources.remove_from_all_message
import app.composeapp.generated.resources.remove_from_all_title
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BookWithProgress
import org.jetbrains.compose.resources.stringResource

// ============================================================================
// 通用组件
// ============================================================================

/**
 * 书架选择列表的选择模式
 */
enum class BookshelfSelectionMode {
    /** 多选模式（使用 Checkbox） */
    MULTIPLE,
    /** 单选模式（使用 RadioButton） */
    SINGLE
}

/**
 * 通用书架选择列表组件
 * 
 * 支持加载状态、空状态、多选和单选两种模式
 * 
 * @param bookshelves 可选的书架列表
 * @param isLoading 是否正在加载
 * @param isDisabled 是否禁用交互（如正在更新时）
 * @param emptyMessage 空状态提示文本
 * @param selectionMode 选择模式（多选/单选）
 * @param isSelected 判断书架是否被选中的函数
 * @param onSelect 选择书架的回调
 */
@Composable
fun BookshelfSelectionList(
    bookshelves: List<Bookshelf>,
    isLoading: Boolean,
    isDisabled: Boolean,
    emptyMessage: String,
    selectionMode: BookshelfSelectionMode,
    isSelected: (Bookshelf) -> Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxWidth().heightIn(min = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        bookshelves.isEmpty() -> {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
        }
        else -> {
            LazyColumn(modifier = modifier) {
                items(
                    items = bookshelves,
                    key = { it.id },
                    contentType = { "bookshelf_selection_item" }
                ) { bookshelf ->
                    BookshelfSelectionItem(
                        bookshelf = bookshelf,
                        isSelected = isSelected(bookshelf),
                        selectionMode = selectionMode,
                        enabled = !isDisabled,
                        onSelect = { onSelect(bookshelf.id) }
                    )
                }
            }
        }
    }
}

/**
 * 书架选择项
 * 
 * @param bookshelf 书架信息
 * @param isSelected 是否被选中
 * @param selectionMode 选择模式
 * @param enabled 是否可交互
 * @param onSelect 选择回调
 */
@Composable
private fun BookshelfSelectionItem(
    bookshelf: Bookshelf,
    isSelected: Boolean,
    selectionMode: BookshelfSelectionMode,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选择控件
        when (selectionMode) {
            BookshelfSelectionMode.MULTIPLE -> {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    enabled = enabled
                )
            }
            BookshelfSelectionMode.SINGLE -> {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    enabled = enabled
                )
            }
        }
        
        // 书架信息
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        ) {
            Text(
                text = bookshelf.name,
                style = MaterialTheme.typography.bodyLarge
            )
            bookshelf.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// 书架管理对话框
// ============================================================================

/**
 * 创建书架对话框
 */
@Composable
fun CreateBookshelfDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val isNameValid = name.isNotBlank()
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(stringResource(Res.string.bookshelf_create))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.bookshelf_name)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.bookshelf_description)) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        onConfirm(name.trim(), description.trim().ifBlank { null })
                    },
                    enabled = isNameValid
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * 编辑书架对话框
 */
@Composable
fun EditBookshelfDialog(
    bookshelf: Bookshelf,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, name: String?, description: String?) -> Unit
) {
    var name by remember(bookshelf) { mutableStateOf(bookshelf.name) }
    var description by remember(bookshelf) { mutableStateOf(bookshelf.description ?: "") }
    
    val isNameValid = name.isNotBlank()
    val hasChanges = name.trim() != bookshelf.name || 
                     description.trim() != (bookshelf.description ?: "")
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(stringResource(Res.string.bookshelf_edit))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.bookshelf_name)) },
                    singleLine = true,
                    enabled = !isLoading && !bookshelf.isSystemDefault,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (bookshelf.isSystemDefault) {
                    Text(
                        text = stringResource(Res.string.bookshelf_system_default_readonly),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.bookshelf_description)) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        val newName = if (name.trim() != bookshelf.name) name.trim() else null
                        val newDescription = if (description.trim() != (bookshelf.description ?: "")) {
                            description.trim().ifBlank { null }
                        } else {
                            null
                        }
                        onConfirm(bookshelf.id, newName, newDescription)
                    },
                    enabled = isNameValid && hasChanges
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * 删除书架确认对话框
 */
@Composable
fun DeleteBookshelfDialog(
    bookshelf: Bookshelf,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (id: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(stringResource(Res.string.bookshelf_delete))
        },
        text = {
            Column {
                Text(stringResource(Res.string.bookshelf_delete_confirm))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(Res.string.bookshelf_delete_info, bookshelf.name, bookshelf.bookCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (bookshelf.bookCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(Res.string.bookshelf_delete_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    onClick = { onConfirm(bookshelf.id) }
                ) {
                    Text(
                        text = stringResource(Res.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

// ============================================================================
// 书籍操作对话框
// ============================================================================

/**
 * 添加到书架对话框
 * 
 * 只显示书籍未加入的书架，允许用户多选后批量添加
 */
@Composable
fun AddToBookshelvesDialog(
    book: BookWithProgress,
    availableBookshelves: List<Bookshelf>,
    selectedBookshelves: Set<Int>,
    isLoading: Boolean,
    isUpdating: Boolean,
    onToggleBookshelf: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (bookId: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading && !isUpdating) onDismiss() },
        title = {
            Text(stringResource(Res.string.add_to_bookshelves_title))
        },
        text = {
            BookshelfSelectionList(
                bookshelves = availableBookshelves,
                isLoading = isLoading,
                isDisabled = isUpdating,
                emptyMessage = stringResource(Res.string.no_available_bookshelves),
                selectionMode = BookshelfSelectionMode.MULTIPLE,
                isSelected = { it.id in selectedBookshelves },
                onSelect = onToggleBookshelf
            )
        },
        confirmButton = {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                TextButton(
                    onClick = { onConfirm(book.book.id) },
                    enabled = !isLoading && selectedBookshelves.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading && !isUpdating
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * 移动到书架对话框
 * 
 * 单选目标书架，从当前书架移除并添加到目标书架
 */
@Composable
fun MoveToBookshelfDialog(
    book: BookWithProgress,
    availableBookshelves: List<Bookshelf>,
    selectedBookshelf: Int?,
    isLoading: Boolean,
    isUpdating: Boolean,
    onSelectBookshelf: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (bookId: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading && !isUpdating) onDismiss() },
        title = {
            Text(stringResource(Res.string.move_to_bookshelf_title))
        },
        text = {
            Column {
                // 提示文字
                Text(
                    text = stringResource(Res.string.move_to_bookshelf_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                BookshelfSelectionList(
                    bookshelves = availableBookshelves,
                    isLoading = isLoading,
                    isDisabled = isUpdating,
                    emptyMessage = stringResource(Res.string.no_other_bookshelves),
                    selectionMode = BookshelfSelectionMode.SINGLE,
                    isSelected = { it.id == selectedBookshelf },
                    onSelect = onSelectBookshelf
                )
            }
        },
        confirmButton = {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                TextButton(
                    onClick = { onConfirm(book.book.id) },
                    enabled = !isLoading && selectedBookshelf != null
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading && !isUpdating
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * 从所有书架移除确认对话框
 */
@Composable
fun RemoveFromAllDialog(
    book: BookWithProgress,
    isRemoving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (bookId: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isRemoving) onDismiss() },
        title = {
            Text(stringResource(Res.string.remove_from_all_title))
        },
        text = {
            Text(
                text = stringResource(Res.string.remove_from_all_message, book.book.title),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (isRemoving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                TextButton(
                    onClick = { onConfirm(book.book.id) }
                ) {
                    Text(
                        text = stringResource(Res.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRemoving
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
