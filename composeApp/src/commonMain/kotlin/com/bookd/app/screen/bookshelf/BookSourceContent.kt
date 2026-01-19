package com.bookd.app.screen.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Composable
fun BookshelfSourceContent(
    page: Int,
    scrollState: LazyListState,
    tabRowHeightPx: Float,
    tabRowOffset: Float,
    onTabRowOffsetChanged: (Float) -> Unit,
) {
    // 使用 rememberUpdatedState 确保闭包中使用最新值
    val currentTabRowOffset by rememberUpdatedState(tabRowOffset)
    val currentOnChanged by rememberUpdatedState(onTabRowOffsetChanged)

    // 每个 page 独立的 NestedScrollConnection
    val nestedScrollConnection = remember(page) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // 向上滚动（delta < 0）：先折叠 Tab，再滚动列表
                if (delta < 0 && currentTabRowOffset > -tabRowHeightPx) {
                    val newOffset = (currentTabRowOffset + delta).coerceAtLeast(-tabRowHeightPx)
                    val consumed = newOffset - currentTabRowOffset
                    currentOnChanged(newOffset)
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y

                // 向下滚动（delta > 0）：列表滚动完后展开 Tab（包括惯性滚动）
                if (delta > 0 && currentTabRowOffset < 0) {
                    // 检查列表是否已经在顶部
                    val atTop = scrollState.firstVisibleItemIndex == 0 &&
                            scrollState.firstVisibleItemScrollOffset == 0
                    if (atTop) {
                        val newOffset = (currentTabRowOffset + delta).coerceAtMost(0f)
                        val consumedOffset = newOffset - currentTabRowOffset
                        currentOnChanged(newOffset)
                        return Offset(0f, consumedOffset)
                    }
                }

                return Offset.Zero
            }
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.nestedScroll(nestedScrollConnection)
    ) {
        items(50) { i ->
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(color = Color.Red)
            ) {
                Text(text = "Book$i")
            }
        }
    }
}