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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.screen.reader.component.ReaderPageCanvas
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageModeContent(
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
                text = "本章无内容",
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

        val chapterPageDataList = sortedChapterIndices.mapNotNull { chapterIndex ->
            val chapter = chapters[chapterIndex] ?: return@mapNotNull null
            val elements = remember(chapterIndex, chapter, settings) {
                buildList {
                    chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
                    addAll(chapter.elements)
                }
            }
            val engine = remember(chapterIndex, elements, settings, maxWidth, maxHeight, density) {
                ReaderEngine(textMeasurer, density, Constraints.fixed(maxWidth, maxHeight), settings)
            }
            val anchors = remember(engine, elements) {
                val result = engine.calculatePageAnchors(elements)
                println("[PageModeContent] chapter=$chapterIndex maxWidth=$maxWidth maxHeight=$maxHeight elements=${elements.size} pages=${result.size}")
                result
            }
            ChapterPageData(
                chapterIndex = chapterIndex,
                elements = elements,
                engine = engine,
                anchors = anchors,
                pageIndexInChapterOffset = 0
            )
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

        val coroutineScope = rememberCoroutineScope()
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
                        page.engine.prepareRenderCommands(page.anchor, page.nextAnchor, page.elements)
                    }
                    ReaderPageCanvas(
                        renderCommands = renderCommands,
                        pageAnchor = page.anchor,
                        nextPageAnchor = page.nextAnchor,
                        elements = page.elements,
                        readerEngine = page.engine,
                        onLinkClick = {},
                        onFootnoteClick = {},
                        onImageClick = { _, _ -> },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { maxHeight.toDp() })
                    )
                }
            }
        }
    }
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
