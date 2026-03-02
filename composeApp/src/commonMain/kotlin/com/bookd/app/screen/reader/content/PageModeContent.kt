package com.bookd.app.screen.reader.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bookd.app.basic.extension.logD
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.repository.ReaderRepository
import com.bookd.app.screen.reader.component.ReaderPageCanvas
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.reader_chapter_no_content
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageModeContent(
    bookId: Int,
    chapters: Map<Int, ChapterContent>,
    currentChapterIndex: Int,
    pagerSlideDirection: Int,
    settings: ReaderSettings,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    onPageChanged: (pageIndex: Int) -> Unit,
    onChapterChanged: (chapterIndex: Int, direction: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapters.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.reader_chapter_no_content),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val sortedChapterIndices = remember(chapters.keys) { chapters.keys.sorted() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current

        val repository: ReaderRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()

        // 每个章节通过独立 @Composable 函数异步加载，避免主线程 DB 阻塞
        // 使用 buildList + for 循环，确保 @Composable 调用顺序稳定
        val chapterPageDataList = buildList {
            for (chapterIndex in sortedChapterIndices) {
                val chapter = chapters[chapterIndex] ?: continue
                val data = rememberChapterPageData(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    chapter = chapter,
                    settings = settings,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    density = density,
                    textMeasurer = textMeasurer,
                    repository = repository
                )
                if (data != null) add(data)
            }
        }

        val chapterPageDataWithOffset = remember(chapterPageDataList) {
            var offset = 0
            chapterPageDataList.map { data ->
                val updated = data.copy(pageIndexInChapterOffset = offset)
                offset += data.anchors.size
                updated
            }
        }

        val allPages = remember(chapterPageDataWithOffset) {
            chapterPageDataWithOffset.flatMap { chapterData ->
                chapterData.anchors.mapIndexed { index, anchor ->
                    PageInfo(
                        chapterIndex = chapterData.chapterIndex,
                        pageIndexInChapter = index,
                        anchor = anchor,
                        nextAnchor = chapterData.anchors.getOrNull(index + 1),
                        elements = chapterData.elements,
                        engine = chapterData.engine
                    )
                }
            }
        }

        val initialPageIndex = remember(chapterPageDataWithOffset, currentChapterIndex) {
            chapterPageDataWithOffset.firstOrNull { it.chapterIndex == currentChapterIndex }?.pageIndexInChapterOffset ?: 0
        }

        val pagerState = rememberPagerState(
            initialPage = initialPageIndex.coerceIn(0, (allPages.size - 1).coerceAtLeast(0))
        ) { allPages.size }

        var lastReportedChapterIndex by remember { mutableIntStateOf(currentChapterIndex) }

        LaunchedEffect(pagerState.currentPage, allPages) {
            val currentPage = allPages.getOrNull(pagerState.currentPage)
            if (currentPage != null) {
                onPageChanged(currentPage.pageIndexInChapter)
                if (currentPage.chapterIndex != lastReportedChapterIndex) {
                    val direction = if (currentPage.chapterIndex > lastReportedChapterIndex) 1 else -1
                    lastReportedChapterIndex = currentPage.chapterIndex
                    onChapterChanged(currentPage.chapterIndex, direction)
                }
            }
        }

        LaunchedEffect(currentChapterIndex, pagerSlideDirection, allPages) {
            val targetPageIndex = if (pagerSlideDirection == -1) {
                allPages.indexOfLast { it.chapterIndex == currentChapterIndex }
            } else {
                allPages.indexOfFirst { it.chapterIndex == currentChapterIndex }
            }
            if (targetPageIndex >= 0 && targetPageIndex != pagerState.currentPage) {
                pagerState.scrollToPage(targetPageIndex)
            }
            lastReportedChapterIndex = currentChapterIndex
        }

        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(allPages.size) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Final)
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                        if (up != null && !up.isConsumed) {
                            val tapPosition = up.position
                            val width = containerSize.width.toFloat()
                            val leftBoundary = width / 3f
                            val rightBoundary = width * 2f / 3f
                            when {
                                tapPosition.x < leftBoundary -> {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }
                                tapPosition.x > rightBoundary -> {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage < allPages.size - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                                else -> onToggleMenu()
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val page = allPages.getOrNull(pageIndex)
                if (page != null) {
                    val renderCommands = remember(
                        page.anchor,
                        page.nextAnchor,
                        page.elements,
                        page.engine
                    ) {
                        val t0 = Clock.System.now().toEpochMilliseconds()
                        val cmds = page.engine.prepareRenderCommands(page.anchor, page.nextAnchor, page.elements)
                        val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
                        logD(tag = "Reader") { "[PageMode] 绘制准备 pageIndex=${page.pageIndexInChapter} cmds=${cmds.size} 耗时 ${elapsedMs}ms" }
                        cmds
                    }
                    ReaderPageCanvas(
                        renderCommands = renderCommands,
                        pageAnchor = page.anchor,
                        nextPageAnchor = page.nextAnchor,
                        elements = page.elements,
                        readerEngine = page.engine,
                        onLinkClick = onLinkClick,
                        onFootnoteClick = { footnoteId ->
                            val footnote = page.elements.filterIsInstance<ContentElement.Footnote>()
                                .firstOrNull { it.footnoteId == footnoteId }
                            if (footnote != null) onFootnoteClick(footnote)
                        },
                        onImageClick = onImageClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { maxHeight.toDp() })
                    )
                }
            }
        }
    }
}

/**
 * 异步加载单章节分页数据。
 * 先查本地 DB 缓存（suspend，不阻塞主线程），未命中则在 Default 调度器上执行测量计算。
 * 加载完成前返回 null，外层 mapNotNull 会跳过该章节，完成后触发重组。
 */
@Composable
private fun rememberChapterPageData(
    bookId: Int,
    chapterIndex: Int,
    chapter: ChapterContent,
    settings: ReaderSettings,
    maxWidth: Int,
    maxHeight: Int,
    density: Density,
    textMeasurer: TextMeasurer,
    repository: ReaderRepository
): ChapterPageData? {
    val elements = remember(chapterIndex, chapter, settings) {
        buildList {
            chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
            addAll(chapter.elements)
        }
    }
    val engine = remember(chapterIndex, elements, settings, maxWidth, maxHeight, density) {
        ReaderEngine(textMeasurer, density, Constraints.fixed(maxWidth, maxHeight), settings)
    }
    val cacheKey = remember(chapterIndex, elements, settings, maxWidth, maxHeight, density) {
        repository.buildPageAnchorCacheKey(
            bookId = bookId,
            chapterIndex = chapterIndex,
            settings = settings,
            viewportWidth = maxWidth,
            viewportHeight = maxHeight,
            density = density.density,
            elements = elements
        )
    }

    var anchors by remember(cacheKey) { mutableStateOf<List<PageAnchor>?>(null) }

    LaunchedEffect(cacheKey) {
        // 1. 异步读缓存（不阻塞主线程）
        val cached = repository.getPageAnchorsCache(cacheKey)
        if (cached != null) {
            repository.updateLastAccessedAt(cacheKey)
            anchors = cached
            return@LaunchedEffect
        }
        // 2. 缓存未命中：在后台线程执行耗时计算
        val t0 = Clock.System.now().toEpochMilliseconds()
        val computed = withContext(Dispatchers.Default) { engine.calculatePageAnchors(elements) }
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - t0
        logD(tag = "Reader") { "[PageMode] 分页测量 chapter=$chapterIndex elements=${elements.size} pages=${computed.size} 耗时 ${elapsedMs}ms" }
        anchors = computed
        repository.savePageAnchorsCache(cacheKey, computed)
    }

    val currentAnchors = anchors ?: return null
    return ChapterPageData(chapterIndex, elements, engine, currentAnchors, 0)
}

private data class ChapterPageData(
    val chapterIndex: Int,
    val elements: List<ContentElement>,
    val engine: ReaderEngine,
    val anchors: List<PageAnchor>,
    val pageIndexInChapterOffset: Int
)

private data class PageInfo(
    val chapterIndex: Int,
    val pageIndexInChapter: Int,
    val anchor: PageAnchor,
    val nextAnchor: PageAnchor?,
    val elements: List<ContentElement>,
    val engine: ReaderEngine
)
