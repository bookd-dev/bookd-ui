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
import kotlin.math.abs
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
    // 用 rememberUpdatedState 持有最新的 adjacentChapters 引用，再通过 derivedStateOf 读取它。
    // 这样 derivedStateOf 能追踪到 State 的变化而自动重算，同时不会像 remember(key) 那样
    // 使依赖它作为 key 的 LaunchedEffect 重启，从而切断「新章节加入 → orderedChapterIndices 变化
    // → LaunchedEffect 重启 → chapterAnchors 重算 → itemChapterMap 重建 → 循环」链路。
    //
    // 注意：不能直接用 remember { derivedStateOf { adjacentChapters.keys.sorted() } }，
    // 因为 adjacentChapters 是 Composable 函数参数（val），derivedStateOf lambda 捕获的
    // 是首次组合时的值引用，后续重组时参数虽然更新但 lambda 内的闭包不会刷新。
    val adjacentChaptersState by rememberUpdatedState(adjacentChapters)
    val orderedChapterIndices: List<Int> by remember {
        derivedStateOf {
            val indices = adjacentChaptersState.keys.sorted()
            indices
        }
    }

    // 每章独立构建 elements（预插入章节标题）
    // 使用 derivedStateOf 而非 remember(key)，确保与 orderedChapterIndices 在同一个 snapshot
    // 帧内同步更新。避免「orderedChapterIndices 已包含新章节但 chapterElements 还是旧的」
    // 导致 pageAnchors 加载协程因 elements == null 而跳过新章节的时序竞争 bug。
    val chapterElements: Map<Int, List<ContentElement>> by remember {
        derivedStateOf {
            val result = adjacentChaptersState.mapValues { (_, chapter) ->
                buildList {
                    chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
                    addAll(chapter.elements)
                }
            }
            result.entries.sortedBy { it.key }.forEach { (idx, elems) ->
                logD(tag = "Reader") { "[ScrollMode] chapterElements[$idx] size=${elems.size} title=${adjacentChaptersState[idx]?.title}" }
            }
            result
        }
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

            // pageAnchors 不绑定任何 key：生命周期跟随 Composable，避免 adjacentChapters 变化时整体清空导致闪烁
            var chapterAnchors by remember {
                mutableStateOf<Map<Int, List<PageAnchor>>>(emptyMap())
            }

            // anchor 持久加载协程：只随 engine 变化重建（engine 变化意味着布局/设置改变，所有 anchors 需重算）。
            // 不随 adjacentChapters 变化重建，避免「adjacentChapters 新增章节
            // → LaunchedEffect 重启 → 正在计算的 anchor 被取消 → itemChapterMap 短暂丢失条目
            // → 切换检测误触发」链路。
            //
            // 内部用 snapshotFlow 监听「需要加载的章节列表」变化：
            //   - adjacentChapters 新增章节时，snapshotFlow 推送新的 indicesToLoad
            //   - 章节被裁剪时（窗口收缩），同步清理 chapterAnchors 中的过期条目
            //   - 已有 anchors 的章节直接跳过，保证增量性
            LaunchedEffect(engine) {
                snapshotFlow {
                    // 需要加载的章节：在 orderedChapterIndices 中、尚无 anchor、且 elements 已就绪。
                    // 额外检查 chapterElements[it] != null 有两个作用：
                    //   1. 安全性：避免在 elements 尚未就绪时发射无效的 indicesToLoad
                    //   2. 响应性：因为 chapterElements 是 derivedStateOf（State），snapshotFlow
                    //      会追踪它的读取。当新章节的 elements 就绪时，snapshotFlow 自动重新
                    //      评估并发射新的 indicesToLoad，从而触发加载。
                    orderedChapterIndices.filter { chapterAnchors[it] == null && chapterElements[it] != null }
                }.collect { indicesToLoad ->
                    // 先清理已不在窗口中的过期 anchor（窗口收缩场景）
                    val validKeys = orderedChapterIndices.toSet()
                    val stale = chapterAnchors.keys - validKeys
                    if (stale.isNotEmpty()) {
                        chapterAnchors = chapterAnchors.filterKeys { it in validKeys }
                        logD(tag = "Reader") { "[ScrollMode] pageAnchors 清理过期章节 $stale" }
                    }

                    if (indicesToLoad.isEmpty()) return@collect
                    logD(tag = "Reader") { "[ScrollMode] 开始加载 pageAnchors needCount=${indicesToLoad.size}" }
                    for (idx in indicesToLoad) {
                        // 每次迭代前再次确认该章节仍在窗口内（可能在加载过程中被裁剪）
                        if (idx !in orderedChapterIndices) continue
                        val elements = chapterElements[idx] ?: continue
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
                            logD(tag = "Reader") { "[ScrollMode] pageAnchors[$idx] 命中缓存 pages=${cached.size}" }
                        } else {
                            logD(tag = "Reader") { "[ScrollMode] pageAnchors[$idx] 开始计算 elements=${elements.size}" }
                            val t0 = Clock.System.now().toEpochMilliseconds()
                            val computed = withContext(Dispatchers.Default) {
                                engine.calculatePageAnchors(elements)
                            }
                            val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                            logD(tag = "Reader") { "[ScrollMode] pageAnchors[$idx] 计算完成 pages=${computed.size} 耗时 ${elapsedMs}ms" }
                            chapterAnchors = chapterAnchors + (idx to computed)
                            repository.savePageAnchorsCache(cacheKey, computed)
                        }
                    }
                    logD(tag = "Reader") { "[ScrollMode] pageAnchors 加载完成 keys=${chapterAnchors.keys.sorted()}" }
                }
            }

            // 构建"全局 item 索引 → (chapterIndex, localAnchorIndex)"映射表
            // 用于滚动追踪时反向查找章节归属
            // top-spacer 占 index 0，本映射从 index 1 对应的第一个 chapter-header 开始
            // chapter-header item 用 localAnchorIndex = -1 表示
            // 用 derivedStateOf 而非 remember(key)：derivedStateOf 在 orderedChapterIndices 或
            // chapterAnchors 任一变化时自动重算，但它本身是 State 而不是 LaunchedEffect 的 key，
            // 不会打断任何协程 Effect，彻底切断映射表重建 → 章节检测触发 → 切换循环的链路。
            val itemChapterMap: List<Pair<Int, Int>> by remember {
                derivedStateOf {
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

            // 章节切换检测：屏幕中心点 + 到底正向推进策略
            //
            // 核心规则：屏幕垂直中心点落在哪个章节的 item 上，那就是当前章。
            // 通过解析 LazyColumn item 的 key 字符串（格式 "$chapterIdx-$pageIndex"）获取章节号。
            //
            // 正向推进：当 bottom-spacer 可见（到底了）时，取中心点章节与屏幕最大可见章节的较大值，
            // 确保多个短章节不足一屏时仍能触发加载下一章。
            //
            // 回退保护：回退（向更小章节号切换）只依赖中心点，不依赖 maxVisible，
            // 防止 recompose 导致的 visibleItemsInfo 抖动引发 正向→回退→正向 死循环。
            //
            // top-spacer/bottom-spacer 的 key 不符合 "$chapterIdx-$pageIndex" 格式，
            // toIntOrNull() 返回 null，自动过滤。
            //
            // 面积策略：选择屏幕上占据可见面积最大的章节作为当前章节。
            // 相比中心点策略，面积策略在布局抖动时更稳定（新章节刚加载时面积很小）。
            //
            // 冷却机制：章节切换后 800ms 内只允许正向推进，不允许回退。
            // 原因：切换触发 loadAdjacentChaptersForPager 加载新章节 → 新章节的 pageAnchors
            // 就绪后 LazyColumn items 急剧增长 → visibleItemsInfo 短暂抖动 →
            // 形成 6→7→6→7 循环。冷却期给布局足够时间稳定。
            LaunchedEffect(listState) {
                var lastSwitchTimeMs = 0L

                snapshotFlow {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo.map { item ->
                        VisibleItemInfo(key = item.key, offset = item.offset, size = item.size)
                    }
                    detectChapterByMaxVisibleArea(
                        visibleItems = visibleItems,
                        viewportStartOffset = layoutInfo.viewportStartOffset,
                        viewportHeight = layoutInfo.viewportSize.height,
                        currentChapterIndex = currentChapterIndexState
                    )
                }
                    .distinctUntilChanged()
                    .debounce(300)
                    .collect { newChapterIndex ->
                        val current = currentChapterIndexState
                        if (newChapterIndex != current) {
                            val now = Clock.System.now().toEpochMilliseconds()
                            val elapsed = now - lastSwitchTimeMs
                            val isRetreat = newChapterIndex < current

                            // 冷却期内只允许正向推进
                            if (isRetreat && elapsed < 800) {
                                logD(tag = "Reader") {
                                    "[ScrollMode] 章节切换冷却中，忽略回退 $current → $newChapterIndex (${elapsed}ms)"
                                }
                                return@collect
                            }

                            lastSwitchTimeMs = now
                            logD(tag = "Reader") {
                                "[ScrollMode] 章节切换检测 $current → $newChapterIndex"
                            }
                            onCurrentChapterChanged(newChapterIndex)
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
                    item(key = topSpacerKey) {
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
                            key = { _, anchor -> "$chapterIdx-${anchor.pageIndex}" }
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

                    item(key = bottomSpacerKey) {
                        Spacer(modifier = Modifier.height(verticalMarginDp))
                    }
                }
            }
        }
    }
}

/**
 * 可见 item 的简化表示，用于章节切换检测算法。
 * 解耦 LazyListItemInfo，便于单元测试。
 */
internal data class VisibleItemInfo(
    val key: Any?,
    val offset: Int,
    val size: Int
)

/**
 * 章节切换检测纯算法：占据可见面积最大的章节 + 到底正向推进策略。
 *
 * 相比之前的「屏幕中心点」策略，面积策略在布局抖动时更稳定：
 * - 新章节刚加载时在 viewport 内面积很小，不会立即抢走"最大面积"
 * - 多短章节同屏时，面积最大的才是用户真正在看的
 *
 * @param visibleItems   当前可见的 item 列表（从 LazyListLayoutInfo.visibleItemsInfo 转换）
 * @param viewportStartOffset  viewport 在内容坐标系中的起始偏移
 * @param viewportHeight       viewport 高度
 * @param currentChapterIndex  当前章节索引
 * @return 建议切换到的章节索引
 */
internal fun detectChapterByMaxVisibleArea(
    visibleItems: List<VisibleItemInfo>,
    viewportStartOffset: Int,
    viewportHeight: Int,
    currentChapterIndex: Int
): Int {
    if (visibleItems.isEmpty()) return currentChapterIndex

    val viewportEnd = viewportStartOffset + viewportHeight

    // 步骤1：按章节分组，计算每个章节在 viewport 内的可见面积
    val chapterAreas = mutableMapOf<Int, Int>()
    for (item in visibleItems) {
        val chapterIdx = (item.key as? String)?.substringBefore('-')?.toIntOrNull()
            ?: continue  // 跳过 spacer
        val itemEnd = item.offset + item.size
        // clamp 到 viewport 范围
        val visibleStart = maxOf(item.offset, viewportStartOffset)
        val visibleEnd = minOf(itemEnd, viewportEnd)
        val visibleArea = maxOf(0, visibleEnd - visibleStart)
        chapterAreas[chapterIdx] = (chapterAreas[chapterIdx] ?: 0) + visibleArea
    }

    // 步骤2：没有有效章节 item（全是 spacer 或 anchors 未就绪） → 保持当前
    if (chapterAreas.isEmpty()) return currentChapterIndex

    // 步骤3：选面积最大的章节；面积相同时优先选更接近当前章节的（减少不必要切换）
    val maxAreaChapter = chapterAreas.entries
        .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }
            .thenBy { abs(it.key - currentChapterIndex) })
        .first().key

    // 步骤4：到底时正向推进兜底（短章节不足一屏场景）
    val isAtBottom = visibleItems.any { it.key == "bottom-spacer" }
    val maxVisibleChapter = if (isAtBottom) chapterAreas.keys.maxOrNull() else null

    // 步骤5：正向推进取较大值；回退依赖面积策略
    val candidate = maxOf(maxAreaChapter, maxVisibleChapter ?: maxAreaChapter)

    return if (candidate > currentChapterIndex) candidate else maxAreaChapter
}

private const val topSpacerKey = "top-spacer"
private const val bottomSpacerKey = "bottom-spacer"