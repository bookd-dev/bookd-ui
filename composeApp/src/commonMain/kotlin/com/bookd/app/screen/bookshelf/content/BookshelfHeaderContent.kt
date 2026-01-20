package com.bookd.app.screen.bookshelf.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
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
    pagerState: PagerState,
    isCollapsed: Boolean,
    isBookshelfVisible: Boolean,
    onBookSourceChange: (Int) -> Unit = {},
    onBookshelfClick: () -> Unit = {},
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        // 第一行：书架按钮 (始终显示), 书籍源(折叠显示), 设置网络(始终显示)
        // 折叠时，数据源 Tabs 移到这一行

        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BooklistMore,
                contentDescription = stringResource(Res.string.booklist),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (isBookshelfVisible) 90f else 0f)
                    .noRippleClickable { onBookshelfClick() },
            )

            // 折叠时显示数据源选择
            AnimatedVisibility(
                visible = isCollapsed,
                modifier = Modifier.weight(1f),
            ) {
                BookSourceTabList(
                    pagerState = pagerState,
                    onClick = onBookSourceChange
                )
            }

            if (!isCollapsed) {
                Spacer(modifier = Modifier.weight(1f))
            }

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

        // 第二行：数据源 Tabs (展开时显示)
        AnimatedVisibility(visible = !isCollapsed) {
            BookSourceTabList(
                pagerState = pagerState,
                onClick = onBookSourceChange,
            )
        }
    }
}

@Composable
private fun BookSourceTabList(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onClick: (Int) -> Unit = {}
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier.height(40.dp),
        divider = {},
        edgePadding = 0.dp,  // 移除默认的边距
    ) {
        repeat(30) { index ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { onClick(index) },
            ) {
                Text(
                    text = "数据源$index",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}