package com.bookd.app.screen.reader.content

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.bookd.app.basic.extension.logD
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.extension.getCommandHeight
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.repository.ReaderRepository
import com.bookd.app.screen.reader.component.ReaderPageCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.time.Clock


/**
 * 滚动阅读模式内容组件
 *
 * 将 adjacentChapters（上一章 + 当前章 + 下一章）拼接在同一个 LazyColumn 中，
 * 用户滚动到章末自动无缝接续下一章，无需任何手动触发。
 *
 * 进度追踪：当某章内容在屏幕上的可见像素占比超过一半时，回调 onCurrentChapterChanged。
 */
@OptIn(FlowPreview::class)
@Composable
fun ScrollModeContent(
    bookId: Int,
    currentChapterIndex: Int,
    adjacentChapters: Map<Int, ChapterContent>,
    settings: ReaderSettings,
    listState: LazyListState = rememberLazyListState(),
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    onScrollPositionChanged: (chapterIndex: Int, paragraphIndex: Int, scrollOffset: Int) -> Unit,
    onCurrentChapterChanged: (chapterIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository: ReaderRepository = koinInject()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val densityValue = density.density

    // [日志] 入参快照：收到了哪些章节、各章 elements 数量
    logD(tag = "Reader") {
        val chapterSummary = adjacentChapters.entries
            .sortedBy { it.key }
            .joinToString { "ch${it.key}(${it.value.elements.size}elems)" }
        "[ScrollMode] 入参 currentChapterIndex=$currentChapterIndex adjacentChapters=[$chapterSummary]"
    }

    // 按章节顺序排列（adjacentChapters 中所有章节，已按 key 升序），不依赖 currentChapterIndex
    // 这样 currentChapterIndex 变化时不会触发 orderedChapterIndices 的重算，也不会重建 itemChapterMap
    val orderedChapterIndices: List<Int> = remember(adjacentChapters) {
        val indices = adjacentChapters.keys.sorted()
        logD(tag = "Reader") { "[ScrollMode] orderedChapterIndices=$indices" }
        indices
    }

    // 每章独立构建 elements（预插入章节标题）
    val chapterElements: Map<Int, List<ContentElement>> = remember(adjacentChapters) {
        val result = adjacentChapters.mapValues { (_, chapter) ->
            buildList {
                chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
                addAll(chapter.elements)
            }
        }
        result.entries.sortedBy { it.key }.forEach { (idx, elems) ->
            logD(tag = "Reader") { "[ScrollMode] chapterElements[$idx] size=${elems.size} title=${adjacentChapters[idx]?.title}" }
        }
        result
    }

    // 每章独立的脚注点击 handler（避免捕获过时的 elements 引用）
    val footnoteHandlers: Map<Int, (String) -> Unit> = remember(chapterElements) {
        chapterElements.mapValues { (_, elements) ->
            { footnoteId: String ->
                val footnote = elements.filterIsInstance<ContentElement.Footnote>()
                    .firstOrNull { it.footnoteId == footnoteId }
                if (footnote != null) onFootnoteClick(footnote)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (!constraints.isZero) {
            // [日志] viewport 尺寸确认
            logD(tag = "Reader") {
                "[ScrollMode] viewport ${constraints.maxWidth}x${constraints.maxHeight} density=$densityValue"
            }
            // 三章共用一个 engine（只依赖 settings / constraints / density，与章节内容无关）
            val engine = remember(textMeasurer, settings, constraints, densityValue) {
                ReaderEngine(
                    textMeasurer,
                    density,
                    Constraints.fixed(constraints.maxWidth, constraints.maxHeight),
                    settings
                )
            }

            val verticalMarginDp = remember(engine) {
                with(density) { engine.marginVerticalPx.toDp() }
            }

            // 每章独立计算 pageAnchors，加载完一章立即更新 UI，不等三章全部完成
            var chapterAnchors by remember(chapterElements) {
                mutableStateOf<Map<Int, List<PageAnchor>>>(emptyMap())
            }

            LaunchedEffect(chapterElements, engine, constraints, densityValue) {
                logD(tag = "Reader") { "[ScrollMode] 开始加载 pageAnchors chapterCount=${chapterElements.size}" }
                for ((idx, elements) in chapterElements) {
                    val cacheKey = repository.buildPageAnchorCacheKey(
                        bookId = bookId,
                        chapterIndex = idx,
                        settings = settings,
                        viewportWidth = constraints.maxWidth,
                        viewportHeight = constraints.maxHeight,
                        density = densityValue,
                        elements = elements
                    )
                    val cached = repository.getPageAnchorsCache(cacheKey)
                    if (cached != null) {
                        repository.updateLastAccessedAt(cacheKey)
                        chapterAnchors = chapterAnchors + (idx to cached)
                        logD(tag = "Reader") {
                            "[ScrollMode] pageAnchors[$idx] 命中缓存 pages=${cached.size}"
                        }
                    } else {
                        logD(tag = "Reader") { "[ScrollMode] pageAnchors[$idx] 开始计算 elements=${elements.size}" }
                        val t0 = Clock.System.now().toEpochMilliseconds()
                        val computed = withContext(Dispatchers.Default) {
                            engine.calculatePageAnchors(elements)
                        }
                        val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                        logD(tag = "Reader") {
                            "[ScrollMode] pageAnchors[$idx] 计算完成 pages=${computed.size} 耗时 ${elapsedMs}ms"
                        }
                        chapterAnchors = chapterAnchors + (idx to computed)
                        repository.savePageAnchorsCache(cacheKey, computed)
                    }
                }
                logD(tag = "Reader") {
                    "[ScrollMode] 全部 pageAnchors 加载完成 keys=${chapterAnchors.keys.sorted()}"
                }
            }

            // 构建"全局 item 索引 → (chapterIndex, localAnchorIndex)"映射表
            // 用于滚动追踪时反向查找章节归属
            // top-spacer 占 index 0，本映射从 index 1 对应的第一个 chapter-header 开始
            // chapter-header item 用 localAnchorIndex = -1 表示
            val itemChapterMap: List<Pair<Int, Int>> = remember(orderedChapterIndices, chapterAnchors) {
                buildList {
                    for (idx in orderedChapterIndices) {
                        val anchors = chapterAnchors[idx]
                        if (anchors == null) {
                            logD(tag = "Reader") { "[ScrollMode] itemChapterMap: ch$idx 的 pageAnchors 尚未就绪，跳过" }
                            continue
                        }
                        anchors.indices.forEach { anchorIdx ->
                            add(idx to anchorIdx)               // anchor items
                        }
                    }
                }.also { map ->
                    logD(tag = "Reader") {
                        val summary = orderedChapterIndices.joinToString { idx ->
                            val count = map.count { it.first == idx && it.second >= 0 }
                            "ch$idx:${count}pages"
                        }
                        "[ScrollMode] itemChapterMap 构建完成 totalItems=${map.size} [$summary]"
                    }
                }
            }

            // 用 rememberUpdatedState 持有最新的映射表和当前章节索引，
            // 避免将它们作为 LaunchedEffect 的 key：
            //   - itemChapterMap 每次 adjacentChapters 变化（章节加载/裁剪）都会重建，
            //     若作为 key 会导致 debounce 不断被打断，snapshotFlow 反复重启，形成循环。
            //   - currentChapterIndex 同理：每次章节切换回调都会更新它，若作为 key 同样循环。
            // rememberUpdatedState 保证 snapshotFlow lambda 始终读取到最新值，
            // 而 Effect 本身只随 listState 变化重建（listState 生命周期与 Composable 相同）。
            val itemChapterMapState by rememberUpdatedState(itemChapterMap)
            val currentChapterIndexState by rememberUpdatedState(currentChapterIndex)

            // 进度追踪：300ms 防抖，上报当前可见的章节 + anchor 位置
            LaunchedEffect(listState) {
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }
                    .debounce(300)
                    .collect { (globalIndex, scrollOffset) ->
                        // index 0 = top-spacer，内容从 index 1 开始，映射时偏移 -1
                        val mapIndex = (globalIndex - 1).coerceAtLeast(0)
                        val (chapterIdx, localAnchorIdx) = itemChapterMapState.getOrNull(mapIndex)
                            ?: return@collect
                        val paragraphIndex = localAnchorIdx.coerceAtLeast(0)
                        onScrollPositionChanged(chapterIdx, paragraphIndex, scrollOffset)
                    }
            }

            // 章节切换检测：统计每章在屏幕上的可见像素，占比最大的即为"当前章"
            LaunchedEffect(listState) {
                snapshotFlow {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val viewportStart = listState.layoutInfo.viewportStartOffset
                    val viewportEnd = listState.layoutInfo.viewportEndOffset
                    if (visibleItems.isEmpty() || viewportEnd <= viewportStart) {
                        return@snapshotFlow currentChapterIndexState
                    }

                    val chapterVisiblePx = mutableMapOf<Int, Int>()
                    for (itemInfo in visibleItems) {
                        val mapIndex = (itemInfo.index - 1).coerceAtLeast(0)
                        val (chIdx, _) = itemChapterMapState.getOrNull(mapIndex) ?: continue
                        val visibleTop = maxOf(itemInfo.offset, viewportStart)
                        val visibleBottom = minOf(itemInfo.offset + itemInfo.size, viewportEnd)
                        val px = (visibleBottom - visibleTop).coerceAtLeast(0)
                        chapterVisiblePx[chIdx] = (chapterVisiblePx[chIdx] ?: 0) + px
                    }

                    chapterVisiblePx.maxByOrNull { it.value }?.key ?: currentChapterIndexState
                }
                    .distinctUntilChanged()
                    .debounce(300)
                    .collect { dominantChapterIndex ->
                        if (dominantChapterIndex != currentChapterIndexState) {
                            logD(tag = "Reader") {
                                "[ScrollMode] 章节切换检测 $currentChapterIndexState → $dominantChapterIndex"
                            }
                            onCurrentChapterChanged(dominantChapterIndex)
                        }
                    }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // 滚动模式只响应中间 1/3 区域点击（切换菜单）
                            val zoneWidth = constraints.maxWidth / 3f
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
                    item(key = "top-spacer") {
                        Spacer(modifier = Modifier.height(verticalMarginDp))
                    }

                    for (chapterIdx in orderedChapterIndices) {
                        val elements = chapterElements[chapterIdx]
                        if (elements == null) {
                            logD(tag = "Reader") { "[ScrollMode] LazyColumn: ch$chapterIdx 的 elements 为 null，跳过渲染" }
                            continue
                        }
                        val safeAnchors = chapterAnchors[chapterIdx] ?: run {
                            logD(tag = "Reader") { "[ScrollMode] LazyColumn: ch$chapterIdx 的 pageAnchors 尚未就绪，渲染空列表" }
                            emptyList()
                        }
                        val footnoteHandler = footnoteHandlers[chapterIdx] ?: {}

                        itemsIndexed(
                            items = safeAnchors,
                            key = { _, anchor -> "$chapterIdx-$anchor" }
                        ) { index, anchor ->
                            val nextAnchor = safeAnchors.getOrNull(index + 1)

                            val renderCommands = remember(anchor, nextAnchor, elements, engine) {
                                val t0 = Clock.System.now().toEpochMilliseconds()
                                val cmds = engine.prepareRenderCommands(anchor, nextAnchor, elements)
                                val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                                logD(tag = "Reader") {
                                    "[ScrollMode] 绘制准备 chapterIndex=$chapterIdx anchorIndex=$index cmds=${cmds.size} 耗时 ${elapsedMs}ms"
                                }
                                cmds
                            }

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
                                onLinkClick = onLinkClick,
                                onFootnoteClick = footnoteHandler,
                                onImageClick = onImageClick,
                                verticalOffset = 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(contentHeightDp)
                            )
                        }
                    }

                    item(key = "bottom-spacer") {
                        Spacer(modifier = Modifier.height(verticalMarginDp))
                    }
                }
            }
        }
    }
}