package com.bookd.app.screen.bookshelf.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.booklist
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.structure.BookshelfMenu
import com.bookd.app.ui.icons.BooklistMore
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookshelfHeaderContent(
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(Res.string.settings),
                modifier = Modifier.size(24.dp).noRippleClickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                BookshelfMenu.entries.forEach { entry ->
                    val text = stringResource(entry.text)
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        leadingIcon = entry.icon?.let {
                            {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = text,
                                    modifier = Modifier.size(entry.iconSize)
                                )
                            }
                        },
                        onClick = {
                            onMenuClick(entry)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
