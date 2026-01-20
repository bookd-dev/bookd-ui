package com.bookd.app.screen.booksource.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.unit.dp
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.structure.BookSourceMenu
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookSourceHeaderContent(
    pagerState: PagerState,
    isCollapsed: Boolean,
    onBookSourceChange: (Int) -> Unit = {},
    onMenuClick: (entry: BookSourceMenu) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    // 第一行：折叠时显示数据源 Tabs，右侧显示搜索菜单
    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp).noRippleClickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                BookSourceMenu.entries.forEach { entry ->
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
