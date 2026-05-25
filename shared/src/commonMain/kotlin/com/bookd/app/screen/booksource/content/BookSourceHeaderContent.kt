package com.bookd.app.screen.booksource.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.BookSource
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.refresh
import app.composeapp.generated.resources.search
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookSourceHeaderContent(
    sources: List<BookSource>,
    pagerState: PagerState,
    isCollapsed: Boolean,
    isRefreshingAll: Boolean = false,
    onBookSourceChange: (Int) -> Unit = {},
    onRefreshAllClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    // 第一行：折叠时显示数据源 Tabs，右侧显示刷新和搜索按钮
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
                sources = sources,
                pagerState = pagerState,
                onClick = onBookSourceChange
            )
        }

        if (!isCollapsed) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = onRefreshAllClick,
                enabled = !isRefreshingAll
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(Res.string.refresh))
            }

            TextButton(
                onClick = onSearchClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(Res.string.search))
            }
        }
    }

    // 第二行：数据源 Tabs (展开时显示)
    AnimatedVisibility(visible = !isCollapsed) {
        BookSourceTabList(
            sources = sources,
            pagerState = pagerState,
            onClick = onBookSourceChange,
        )
    }
}

@Composable
private fun BookSourceTabList(
    sources: List<BookSource>,
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onClick: (Int) -> Unit = {}
) {
    if (sources.isEmpty()) return
    
    PrimaryScrollableTabRow(
        selectedTabIndex = pagerState.currentPage.coerceIn(0, sources.size - 1),
        modifier = modifier.height(40.dp),
        divider = {},
        edgePadding = 0.dp,
    ) {
        sources.forEachIndexed { index, source ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { onClick(index) },
            ) {
                Text(
                    text = source.name,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}
