package com.bookd.app.screen.bookshelf.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
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
import com.bookd.app.data.model.Bookshelf
import org.jetbrains.compose.resources.stringResource

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
