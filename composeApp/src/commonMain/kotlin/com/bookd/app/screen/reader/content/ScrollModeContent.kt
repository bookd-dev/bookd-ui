package com.bookd.app.screen.reader.content

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * 滚动模式内容组件
 * 
 * 垂直滚动浏览章节内容，支持：
 * - 滚动位置追踪
 * - 滚动到指定位置
 * - 触底加载下一章
 * - 中间 1/3 区域点击切换菜单
 */
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
    // 收集章节中的所有脚注
    val footnotes = chapter.elements.filterIsInstance<ContentElement.Footnote>()
        .associateBy { it.footnoteId }
    
    // 容器尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 追踪滚动位置
    LaunchedEffect(listState) {
        snapshotFlow {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            firstVisibleItemIndex to firstVisibleItemScrollOffset
        }
            .debounce(300) // 防抖 300ms
            .collect { (paragraphIndex, scrollOffset) ->
                onScrollPositionChanged(paragraphIndex, scrollOffset)
            }
    }
    
    // 检测是否滚动到底部或顶部
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            Triple(totalItems, lastVisibleItem, firstVisibleItem)
        }.collect { (totalItems, lastVisibleItem, firstVisibleItem) ->
            // 滚动到底部
            if (totalItems > 0 && lastVisibleItem >= totalItems - 1) {
                onReachEnd()
            }
            // 滚动到顶部
            if (firstVisibleItem == 0 && listState.firstVisibleItemScrollOffset == 0) {
                onReachStart()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // 滚动模式只检测中间 1/3 区域点击切换菜单
                    val zoneWidth = containerSize.width / 3f
                    if (offset.x >= zoneWidth && offset.x <= zoneWidth * 2) {
                        onToggleMenu()
                    }
                    // 左右区域在滚动模式下不做处理，让用户自然滚动
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                horizontal = settings.marginHorizontal.dp,
                vertical = settings.marginVertical.dp
            )
        ) {
            // 章节标题（第一项）
            if (chapter.title != null) {
                item(key = "${chapter.index}_title") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = chapter.title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = (settings.fontSize + 6).sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (settings.fontSize + 6).sp * settings.lineHeight
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            // 内容元素
            itemsIndexed(
                items = chapter.elements,
                key = { index, _ -> "${chapter.index}_$index" }
            ) { index, element ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = settings.paragraphSpacing.dp)
                ) {
                    ContentElementView(
                        element = element,
                        settings = settings,
                        onImageClick = onImageClick,
                        onFootnoteClick = { footnoteId ->
                            footnotes[footnoteId]?.let { onFootnoteClick(it) }
                        },
                        onLinkClick = onLinkClick
                    )
                }
            }
        }
    }
}
