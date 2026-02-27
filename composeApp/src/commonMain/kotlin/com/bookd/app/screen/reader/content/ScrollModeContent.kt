package com.bookd.app.screen.reader.content

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.extension.getCommandHeight
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.screen.reader.component.ReaderPageCanvas
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun ScrollModeContent(
    chapter: ChapterContent,
    settings: ReaderSettings,
    listState: LazyListState = rememberLazyListState(),
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    onScrollPositionChanged: (paragraphIndex: Int, scrollOffset: Int) -> Unit,
    onReachEnd: () -> Unit,
    onReachStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 预插入章节标题到元素列表头部
    val elements = remember(chapter) {
        buildList {
            chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
            addAll(chapter.elements)
        }
    }

    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (screenSize == IntSize.Zero) {
                    screenSize = size
                }
            }
    ) {
        if (screenSize != IntSize.Zero) {
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current

            // 缓存 Engine 实例，避免每次重组都重新测量
            val engine = remember(elements, settings, screenSize, density) {
                ReaderEngine(textMeasurer, density, Constraints.fixed(screenSize.width, screenSize.height), settings)
            }

            // 滚动模式垂直 margin（仅用于首尾留白）
            val verticalMarginDp = remember(engine) {
                with(density) { engine.marginVerticalPx.toDp() }
            }

            val pageAnchors = remember(engine, elements) {
                engine.calculatePageAnchors(elements)
            }

            // 300ms 防抖滚动追踪
            LaunchedEffect(listState) {
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }
                    .debounce(300)
                    .collect { (paragraphIndex, scrollOffset) ->
                        onScrollPositionChanged(paragraphIndex, scrollOffset)
                    }
            }

            // 边界检测
            LaunchedEffect(listState) {
                snapshotFlow {
                    val layoutInfo = listState.layoutInfo
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                    Triple(totalItems, lastVisibleItem, firstVisibleItem)
                }.collect { (totalItems, lastVisibleItem, firstVisibleItem) ->
                    if (totalItems > 0 && lastVisibleItem >= totalItems - 1) {
                        onReachEnd()
                    }
                    if (firstVisibleItem == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        onReachStart()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // 滚动模式只响应中间 1/3 区域点击（切换菜单）
                            val zoneWidth = screenSize.width / 3f
                            if (offset.x >= zoneWidth && offset.x <= zoneWidth * 2) {
                                onToggleMenu()
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 顶部留白（替代 Canvas 内的 marginVertical）
                    item(key = "top-spacer") {
                        Spacer(modifier = Modifier.height(verticalMarginDp))
                    }

                    itemsIndexed(pageAnchors, key = { index, _ -> index }) { index, anchor ->
                        val nextAnchor = pageAnchors.getOrNull(index + 1)
                        val renderCommands = remember(anchor, nextAnchor, elements, engine) {
                            engine.prepareRenderCommands(anchor, nextAnchor, elements)
                        }

                        // 计算本页实际内容高度（px）
                        val contentHeightDp = remember(renderCommands) {
                            val totalPx = if (renderCommands.isEmpty()) 0 else {
                                val lastCmd = renderCommands.last()
                                lastCmd.y + getCommandHeight(lastCmd)
                            }
                            with(density) { totalPx.toDp() }
                        }

                        ReaderPageCanvas(
                            renderCommands = renderCommands,
                            pageAnchor = anchor,
                            nextPageAnchor = nextAnchor,
                            elements = elements,
                            readerEngine = engine,
                            onLinkClick = {},
                            onFootnoteClick = {},
                            onImageClick = { _, _ -> },
                            verticalOffset = 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(contentHeightDp)
                        )
                    }

                    // 底部留白
                    item(key = "bottom-spacer") {
                        Spacer(modifier = Modifier.height(verticalMarginDp))
                    }
            }
        }
    }
}
}
