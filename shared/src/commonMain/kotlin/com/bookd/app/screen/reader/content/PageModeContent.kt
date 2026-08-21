package com.bookd.app.screen.reader.content

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.bookd.app.basic.extension.logD
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.controller.styles.ReaderThemeColors
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.repository.ReaderRepository
import com.bookd.app.screen.reader.ReaderParagraphSelection
import com.bookd.app.screen.reader.resolveReaderFootnote
import com.bookd.app.screen.reader.component.ReaderPageCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.time.Clock

/**
 * 翻页阅读模式内容组件。
 *
 * 将已加载的相邻章节展开为横向 pager 中的页面条目，分页锚点和滚动模式共用
 * ReaderRepository 的 PageAnchor 缓存语义。
 */
@Composable
fun PageModeContent(
    bookId: Int,
    currentChapterIndex: Int,
    currentPageIndex: Int,
    adjacentChapters: Map<Int, ChapterContent>,
    settings: ReaderSettings,
    scrollRequest: ReaderScrollRequest?,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (ReaderParagraphSelection) -> Unit,
    onScrollRequestCompleted: (chapterIndex: Int, anchorId: String?, paragraphIndex: Int, scrollOffset: Int) -> Unit,
    onPagePositionChanged: (pageIndex: Int) -> Unit,
    onPagerChapterChanged: (chapterIndex: Int, direction: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository: ReaderRepository = koinInject()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val densityValue = density.density
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val readerThemeColors = remember(
        colorScheme.background,
        colorScheme.onBackground,
        colorScheme.onSurfaceVariant,
        colorScheme.surfaceVariant,
        colorScheme.outline,
        colorScheme.outlineVariant,
    ) {
        ReaderThemeColors(
            background = colorScheme.background,
            content = colorScheme.onBackground,
            secondaryContent = colorScheme.onSurfaceVariant,
            surfaceVariant = colorScheme.surfaceVariant,
            outline = colorScheme.outline,
            outlineVariant = colorScheme.outlineVariant,
        )
    }

    val adjacentChaptersState by rememberUpdatedState(adjacentChapters)
    val orderedChapterIndices: List<Int> by remember {
        derivedStateOf { adjacentChaptersState.keys.sorted() }
    }
    val chapterElements: Map<Int, List<ContentElement>> by remember {
        derivedStateOf {
            adjacentChaptersState.mapValues { (_, chapter) ->
                buildReaderChapterElements(chapter)
            }
        }
    }
    val footnoteHandlers: Map<Int, (String) -> Unit> = remember(chapterElements) {
        chapterElements.mapValues { (_, elements) ->
            { footnoteId: String ->
                val footnote = resolveReaderFootnote(elements, footnoteId)
                if (footnote != null) onFootnoteClick(footnote)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(readerThemeColors.background)) {
        if (!constraints.isZero) {
            val engine = remember(textMeasurer, settings, constraints, densityValue, readerThemeColors) {
                ReaderEngine(
                    textMeasurer,
                    density,
                    Constraints.fixed(constraints.maxWidth, constraints.maxHeight),
                    settings,
                    readerThemeColors,
                )
            }
            var chapterAnchors by remember(engine) {
                mutableStateOf<Map<Int, List<PageAnchor>>>(emptyMap())
            }

            LaunchedEffect(engine) {
                snapshotFlow {
                    orderedChapterIndices.filter {
                        chapterAnchors[it] == null && chapterElements[it] != null
                    }
                }.collect { indicesToLoad ->
                    val validKeys = orderedChapterIndices.toSet()
                    val stale = chapterAnchors.keys - validKeys
                    if (stale.isNotEmpty()) {
                        chapterAnchors = chapterAnchors.filterKeys { it in validKeys }
                        logD(tag = "Reader") { "[PageMode] pageAnchors 清理过期章节 $stale" }
                    }

                    if (indicesToLoad.isEmpty()) return@collect
                    for (chapterIndex in indicesToLoad) {
                        if (chapterIndex !in orderedChapterIndices) continue
                        val elements = chapterElements[chapterIndex] ?: continue
                        val cacheKey = repository.buildPageAnchorCacheKey(
                            bookId = bookId,
                            chapterIndex = chapterIndex,
                            settings = settings,
                            viewportWidth = constraints.maxWidth,
                            viewportHeight = constraints.maxHeight,
                            density = densityValue,
                            elements = elements
                        )
                        val cached = repository.getPageAnchorsCache(cacheKey)
                        if (cached != null) {
                            repository.updateLastAccessedAt(cacheKey)
                            chapterAnchors = chapterAnchors + (chapterIndex to cached)
                            logD(tag = "Reader") {
                                "[PageMode] pageAnchors[$chapterIndex] 命中缓存 pages=${cached.size}"
                            }
                        } else {
                            val t0 = Clock.System.now().toEpochMilliseconds()
                            val computed = withContext(Dispatchers.Default) {
                                engine.calculatePageAnchors(elements)
                            }
                            val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                            chapterAnchors = chapterAnchors + (chapterIndex to computed)
                            repository.savePageAnchorsCache(cacheKey, computed)
                            logD(tag = "Reader") {
                                "[PageMode] pageAnchors[$chapterIndex] 计算完成 pages=${computed.size} 耗时 ${elapsedMs}ms"
                            }
                        }
                    }
                }
            }

            val pageEntries: List<PageModeEntry> by remember {
                derivedStateOf {
                    buildPageModeEntries(
                        orderedChapterIndices = orderedChapterIndices,
                        chapterElements = chapterElements,
                        chapterAnchors = chapterAnchors
                    )
                }
            }
            val pagerState = rememberPagerState { pageEntries.size.coerceAtLeast(1) }
            val pageEntriesState by rememberUpdatedState(pageEntries)
            val currentChapterIndexState by rememberUpdatedState(currentChapterIndex)

            LaunchedEffect(pageEntries, currentChapterIndex, currentPageIndex, scrollRequest) {
                if (scrollRequest != null) return@LaunchedEffect
                val exactTarget = findPageModeEntryIndex(
                    entries = pageEntries,
                    chapterIndex = currentChapterIndex,
                    pageIndex = currentPageIndex
                )
                val chapterTarget = findFirstChapterEntryIndex(pageEntries, currentChapterIndex)
                val targetPage = when {
                    exactTarget >= 0 -> exactTarget
                    chapterTarget >= 0 -> chapterTarget
                    else -> 0
                }
                if (
                    targetPage != pagerState.currentPage &&
                    targetPage in 0 until pagerState.pageCount &&
                    !pagerState.isScrollInProgress
                ) {
                    pagerState.scrollToPage(targetPage)
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        val entry = pageEntriesState.getOrNull(page) as? ContentPageEntry
                            ?: return@collect
                        val current = currentChapterIndexState
                        if (entry.chapterIndex != current) {
                            val direction = pageModeChapterDirection(current, entry.chapterIndex)
                            onPagerChapterChanged(entry.chapterIndex, direction)
                        }
                        onPagePositionChanged(entry.pageIndex)
                    }
            }

            LaunchedEffect(pageEntries, currentChapterIndex, pagerState.currentPage) {
                val entry = pageEntries.getOrNull(pagerState.currentPage) as? ContentPageEntry
                    ?: return@LaunchedEffect
                if (entry.chapterIndex != currentChapterIndex) {
                    val direction = pageModeChapterDirection(currentChapterIndex, entry.chapterIndex)
                    onPagerChapterChanged(entry.chapterIndex, direction)
                }
                onPagePositionChanged(entry.pageIndex)
            }

            LaunchedEffect(scrollRequest, pageEntries, chapterElements, chapterAnchors) {
                val request = scrollRequest ?: return@LaunchedEffect
                val target = resolvePageModeScrollTarget(
                    entries = pageEntries,
                    chapterElements = chapterElements,
                    chapterAnchors = chapterAnchors,
                    request = request,
                ) ?: return@LaunchedEffect

                if (
                    target.pagerIndex != pagerState.currentPage &&
                    target.pagerIndex in 0 until pagerState.pageCount
                ) {
                    pagerState.scrollToPage(target.pagerIndex)
                }
                onPagePositionChanged(target.pageIndex)
                onScrollRequestCompleted(
                    target.chapterIndex,
                    target.anchorId,
                    target.paragraphIndex,
                    0,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pagerState, pageEntries.size, constraints.maxWidth) {
                        detectTapGestures { offset ->
                            val zoneWidth = constraints.maxWidth / 3f
                            when {
                                offset.x < zoneWidth && pagerState.currentPage > 0 -> {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                                offset.x > zoneWidth * 2 &&
                                    pagerState.currentPage < pagerState.pageCount - 1 -> {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                                offset.x >= zoneWidth && offset.x <= zoneWidth * 2 -> {
                                    onToggleMenu()
                                }
                            }
                        }
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    key = { page ->
                        pageEntriesState.getOrNull(page)?.key ?: "empty:$page"
                    },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (val entry = pageEntries.getOrNull(page)) {
                        is ContentPageEntry -> {
                            val renderCommands = remember(entry, engine) {
                                val t0 = Clock.System.now().toEpochMilliseconds()
                                val commands = engine.prepareRenderCommands(
                                    startAnchor = entry.anchor,
                                    endAnchor = entry.nextAnchor,
                                    elements = entry.elements
                                )
                                val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                                logD(tag = "Reader") {
                                    "[PageMode] 绘制准备 chapterIndex=${entry.chapterIndex} pageIndex=${entry.pageIndex} cmds=${commands.size} 耗时 ${elapsedMs}ms"
                                }
                                commands
                            }
                            val footnoteHandler = footnoteHandlers[entry.chapterIndex] ?: {}
                            ReaderPageCanvas(
                                renderCommands = renderCommands,
                                pageAnchor = entry.anchor,
                                nextPageAnchor = entry.nextAnchor,
                                elements = entry.elements,
                                readerEngine = engine,
                                onLinkClick = onLinkClick,
                                onFootnoteClick = footnoteHandler,
                                onImageClick = onImageClick,
                                onParagraphLongClick = { anchorId, paragraphIndex ->
                                    onParagraphLongClick(
                                        ReaderParagraphSelection(
                                            chapterIndex = entry.chapterIndex,
                                            anchorId = anchorId,
                                            paragraphIndex = paragraphIndex,
                                            scrollOffset = 0,
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is LoadingPageEntry,
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
