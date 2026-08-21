package com.bookd.app.screen.bookshelf.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import app.composeapp.generated.resources.bookshelf_delete
import app.composeapp.generated.resources.bookshelf_edit
import app.composeapp.generated.resources.grid_view
import app.composeapp.generated.resources.list_view
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.structure.BookshelfMenu
import org.jetbrains.compose.resources.stringResource

/**
 * 书架页面 Header
 * 
 * 包含：
 * - 书架 Tab 列表（长按可编辑/删除）
 * - 设置菜单（切换视图模式、添加书架、网络配置）
 */
@Composable
fun BookshelfHeaderContent(
    bookshelves: List<Bookshelf>,
    pagerState: PagerState,
    isGridMode: Boolean,
    onBookshelfChange: (Int) -> Unit = {},
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
    onEditBookshelf: (Bookshelf) -> Unit = {},
    onDeleteBookshelf: (Bookshelf) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    // 单行布局：左侧书架 Tabs，右侧设置菜单
    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 左侧：书架 Tab 列表
        BookshelfTabList(
            bookshelves = bookshelves,
            pagerState = pagerState,
            modifier = Modifier.weight(1f),
            onClick = onBookshelfChange,
            onEditBookshelf = onEditBookshelf,
            onDeleteBookshelf = onDeleteBookshelf
        )

        if (bookshelves.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        }


        // 右侧：设置菜单
        Box(modifier = Modifier.padding(end = 16.dp)) {
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
                    // 根据当前视图模式动态显示文本和图标
                    val (text, icon) = if (entry == BookshelfMenu.ToggleViewMode) {
                        if (isGridMode) {
                            stringResource(Res.string.list_view) to Icons.AutoMirrored.Outlined.ViewList
                        } else {
                            stringResource(Res.string.grid_view) to Icons.Outlined.GridView
                        }
                    } else {
                        stringResource(entry.text) to entry.icon
                    }
                    
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        leadingIcon = icon?.let {
                            {
                                Icon(
                                    imageVector = it,
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

/**
 * 书架 Tab 列表
 * 
 * 点击三点图标显示编辑/删除菜单
 */
@Composable
private fun BookshelfTabList(
    bookshelves: List<Bookshelf>,
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onClick: (Int) -> Unit = {},
    onEditBookshelf: (Bookshelf) -> Unit = {},
    onDeleteBookshelf: (Bookshelf) -> Unit = {},
) {
    if (bookshelves.isEmpty()) return
    
    // 当前显示上下文菜单的书架 ID
    var contextMenuBookshelfId by remember { mutableStateOf<Int?>(null) }
    
    PrimaryScrollableTabRow(
        selectedTabIndex = pagerState.currentPage.coerceIn(0, bookshelves.size - 1),
        modifier = modifier.height(40.dp),
        divider = {},
        edgePadding = 16.dp,
    ) {
        bookshelves.forEachIndexed { index, bookshelf ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { onClick(index) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(text = bookshelf.name)
                    // 显示书籍数量
                    if (bookshelf.bookCount > 0) {
                        Text(
                            text = "(${bookshelf.bookCount})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 三点图标按钮 + 下拉菜单（仅非系统默认书架显示）
                    if (!bookshelf.isSystemDefault) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .noRippleClickable { contextMenuBookshelfId = bookshelf.id },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // 上下文菜单
                            DropdownMenu(
                                expanded = contextMenuBookshelfId == bookshelf.id,
                                onDismissRequest = { contextMenuBookshelfId = null }
                            ) {
                                // 编辑
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.bookshelf_edit)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        contextMenuBookshelfId = null
                                        onEditBookshelf(bookshelf)
                                    }
                                )
                                
                                // 删除
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = stringResource(Res.string.bookshelf_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        contextMenuBookshelfId = null
                                        onDeleteBookshelf(bookshelf)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
