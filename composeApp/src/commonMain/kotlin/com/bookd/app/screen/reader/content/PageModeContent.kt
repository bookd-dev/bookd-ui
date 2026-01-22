package com.bookd.app.screen.reader.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import kotlinx.coroutines.launch

/**
 * 翻页模式内容组件（多章节版本）
 * 
 * 支持多章节无缝滑动，将相邻章节合并到同一个 HorizontalPager 中。
 * 
 * 特性：
 * - 预加载前后章节，实现无缝跨章节滑动
 * - 使用 SubcomposeLayout 实现智能分页
 * - 三区域点击：左(上一页)、中(菜单)、右(下一页)
 * 
 * @param chapters 相邻章节内容 {章节索引 -> 章节内容}
 * @param currentChapterIndex 当前章节索引
 * @param isExplicitChapterJump 是否是主动跳转（从目录、书签等）
 * @param settings 阅读器设置
 * @param onChapterChanged 当滑动到新章节时的回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageModeContent(
    chapters: Map<Int, ChapterContent>,
    currentChapterIndex: Int,
    settings: ReaderSettings,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    onPageChanged: (pageIndex: Int) -> Unit,
    onChapterChanged: (chapterIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 如果没有章节内容，显示空状态
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
    
    // 收集所有章节中的脚注
    val allFootnotes = remember(chapters) {
        chapters.values.flatMap { chapter ->
            chapter.elements.filterIsInstance<ContentElement.Footnote>()
        }.associateBy { it.id }
    }
    
    // 合并所有章节的元素（按章节索引排序）
    val sortedChapterIndices = remember(chapters.keys) { chapters.keys.sorted() }
    
    // 用于存储分页结果
    var pages by remember(chapters.keys, currentChapterIndex) { 
        mutableStateOf<List<ReaderPage>>(emptyList()) 
    }
    
    // 当前章节在 pages 中的起始页索引
    var currentChapterStartPageIndex by remember { mutableIntStateOf(0) }
    
    SubcomposeLayout(
        modifier = modifier.fillMaxSize()
    ) { constraints ->
        val availableWidth = constraints.maxWidth - (settings.marginHorizontal * 2).dp.roundToPx()
        val availableHeight = constraints.maxHeight - (settings.marginVertical * 2).dp.roundToPx()
        val paragraphSpacingPx = settings.paragraphSpacing.dp.roundToPx()
        
        // 合并所有章节的元素并测量高度
        val allChapterElements = mutableListOf<ChapterElement>()
        val chapterTitleHeights = mutableMapOf<Int, Int>()
        
        for (chapterIndex in sortedChapterIndices) {
            val chapter = chapters[chapterIndex] ?: continue
            
            // 测量章节标题高度
            if (chapter.title != null) {
                val titlePlaceable = subcompose("title_$chapterIndex") {
                    ChapterTitleView(title = chapter.title, settings = settings)
                }.firstOrNull()?.measure(
                    Constraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                )
                chapterTitleHeights[chapterIndex] = (titlePlaceable?.height ?: 0) + paragraphSpacingPx
            }
            
            // 测量每个元素的高度
            chapter.elements.forEachIndexed { elementIndex, element ->
                val placeable = subcompose("element_${chapterIndex}_$elementIndex") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ContentElementView(
                            element = element,
                            settings = settings,
                            onImageClick = { _, _ -> },
                            onFootnoteClick = { },
                            onLinkClick = { }
                        )
                    }
                }.firstOrNull()?.measure(
                    Constraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                )
                
                allChapterElements.add(
                    ChapterElement(
                        chapterIndex = chapterIndex,
                        elementIndex = elementIndex,
                        element = element,
                        height = placeable?.height ?: 0,
                        isFirstOfChapter = elementIndex == 0,
                        chapterTitle = if (elementIndex == 0) chapter.title else null
                    )
                )
            }
        }
        
        // 分页算法：遍历所有元素，按页面高度分组
        val calculatedPages = mutableListOf<ReaderPage>()
        var currentPageElements = mutableListOf<ChapterElement>()
        var currentPageHeight = 0
        var currentPageChapterIndex = sortedChapterIndices.firstOrNull() ?: 0
        var pageIndexInCurrentChapter = 0
        var lastChapterIndex = currentPageChapterIndex
        
        // 记录每个章节的起始页索引
        val chapterStartPages = mutableMapOf<Int, Int>()
        
        for (element in allChapterElements) {
            // 检查是否进入新章节
            if (element.chapterIndex != lastChapterIndex) {
                // 保存当前页（如果有内容）
                if (currentPageElements.isNotEmpty()) {
                    val firstElement = currentPageElements.first()
                    calculatedPages.add(
                        ReaderPage(
                            chapterIndex = currentPageChapterIndex,
                            pageIndexInChapter = pageIndexInCurrentChapter,
                            elements = currentPageElements.toList(),
                            showChapterTitle = firstElement.isFirstOfChapter,
                            chapterTitle = firstElement.chapterTitle
                        )
                    )
                    currentPageElements = mutableListOf()
                    pageIndexInCurrentChapter++
                }
                
                // 重置为新章节
                currentPageChapterIndex = element.chapterIndex
                pageIndexInCurrentChapter = 0
                lastChapterIndex = element.chapterIndex
                
                // 新章节第一页需要预留标题空间
                currentPageHeight = chapterTitleHeights[element.chapterIndex] ?: 0
                
                // 记录章节起始页
                chapterStartPages[element.chapterIndex] = calculatedPages.size
            }
            
            val elementHeight = element.height + paragraphSpacingPx
            
            if (currentPageElements.isEmpty()) {
                // 记录章节起始页（如果还没记录）
                if (!chapterStartPages.containsKey(element.chapterIndex)) {
                    chapterStartPages[element.chapterIndex] = calculatedPages.size
                }
                
                // 第一个元素必须放入当前页
                currentPageElements.add(element)
                currentPageHeight += elementHeight
                currentPageChapterIndex = element.chapterIndex
            } else if (currentPageHeight + elementHeight <= availableHeight) {
                // 可以放入当前页
                currentPageElements.add(element)
                currentPageHeight += elementHeight
            } else {
                // 需要新建一页
                val firstElement = currentPageElements.first()
                calculatedPages.add(
                    ReaderPage(
                        chapterIndex = currentPageChapterIndex,
                        pageIndexInChapter = pageIndexInCurrentChapter,
                        elements = currentPageElements.toList(),
                        showChapterTitle = firstElement.isFirstOfChapter,
                        chapterTitle = firstElement.chapterTitle
                    )
                )
                
                // 检查新元素是否属于新章节
                if (element.chapterIndex != currentPageChapterIndex) {
                    currentPageChapterIndex = element.chapterIndex
                    pageIndexInCurrentChapter = 0
                    currentPageHeight = chapterTitleHeights[element.chapterIndex] ?: 0
                    chapterStartPages[element.chapterIndex] = calculatedPages.size
                } else {
                    pageIndexInCurrentChapter++
                    currentPageHeight = 0
                }
                
                currentPageElements = mutableListOf(element)
                currentPageHeight += elementHeight
            }
        }
        
        // 添加最后一页
        if (currentPageElements.isNotEmpty()) {
            val firstElement = currentPageElements.first()
            calculatedPages.add(
                ReaderPage(
                    chapterIndex = currentPageChapterIndex,
                    pageIndexInChapter = pageIndexInCurrentChapter,
                    elements = currentPageElements.toList(),
                    showChapterTitle = firstElement.isFirstOfChapter,
                    chapterTitle = firstElement.chapterTitle
                )
            )
        }
        
        // 确保至少有一页
        if (calculatedPages.isEmpty()) {
            calculatedPages.add(
                ReaderPage(
                    chapterIndex = currentChapterIndex,
                    pageIndexInChapter = 0,
                    elements = emptyList(),
                    showChapterTitle = false,
                    chapterTitle = null
                )
            )
        }
        
        pages = calculatedPages
        currentChapterStartPageIndex = chapterStartPages[currentChapterIndex] ?: 0
        
        // 渲染实际的 Pager
        val pagerPlaceable = subcompose("pager") {
            MultiChapterPagerContent(
                pages = pages,
                initialPageIndex = currentChapterStartPageIndex,
                currentChapterIndex = currentChapterIndex,
                settings = settings,
                footnotes = allFootnotes,
                onToggleMenu = onToggleMenu,
                onImageClick = onImageClick,
                onFootnoteClick = onFootnoteClick,
                onLinkClick = onLinkClick,
                onPageChanged = onPageChanged,
                onChapterChanged = onChapterChanged
            )
        }.firstOrNull()?.measure(constraints)
        
        layout(constraints.maxWidth, constraints.maxHeight) {
            pagerPlaceable?.placeRelative(0, 0)
        }
    }
}

/**
 * 章节元素（用于分页计算）
 */
private data class ChapterElement(
    val chapterIndex: Int,
    val elementIndex: Int,
    val element: ContentElement,
    val height: Int,
    val isFirstOfChapter: Boolean,
    val chapterTitle: String?
)

/**
 * 分页后的页面
 */
private data class ReaderPage(
    val chapterIndex: Int,
    val pageIndexInChapter: Int,
    val elements: List<ChapterElement>,
    val showChapterTitle: Boolean,
    val chapterTitle: String?
)

/**
 * 章节标题视图
 */
@Composable
private fun ChapterTitleView(
    title: String,
    settings: ReaderSettings
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
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

/**
 * 多章节 Pager 内容
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiChapterPagerContent(
    pages: List<ReaderPage>,
    initialPageIndex: Int,
    currentChapterIndex: Int,
    settings: ReaderSettings,
    footnotes: Map<String, ContentElement.Footnote>,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onPageChanged: (pageIndex: Int) -> Unit,
    onChapterChanged: (chapterIndex: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 创建 PagerState
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    ) { pages.size }
    
    // 记录上一次报告的章节索引，避免重复触发
    var lastReportedChapterIndex by remember { mutableIntStateOf(currentChapterIndex) }
    
    // 监听页面变化，检测章节切换
    LaunchedEffect(pagerState.currentPage, pages) {
        val currentPage = pages.getOrNull(pagerState.currentPage)
        if (currentPage != null) {
            // 报告页面变化（章节内页码）
            onPageChanged(currentPage.pageIndexInChapter)
            
            // 检测章节切换（仅当用户滑动导致的切换）
            if (currentPage.chapterIndex != lastReportedChapterIndex) {
                lastReportedChapterIndex = currentPage.chapterIndex
                onChapterChanged(currentPage.chapterIndex)
            }
        }
    }
    
    // 当 currentChapterIndex 变化时（如从目录跳转），跳转到对应章节的第一页
    LaunchedEffect(currentChapterIndex) {
        val targetPageIndex = pages.indexOfFirst { it.chapterIndex == currentChapterIndex }
        if (targetPageIndex >= 0 && targetPageIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetPageIndex)
        }
        lastReportedChapterIndex = currentChapterIndex
    }
    
    // 记录容器尺寸用于计算点击区域
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(pages.size) {
                // 使用 awaitEachGesture 检测点击
                // PointerEventPass.Final 确保事件先被内层处理
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Final)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    
                    // 只有当 up 事件存在且未被消费时，才处理点击
                    if (up != null && !up.isConsumed) {
                        val tapPosition = up.position
                        val width = containerSize.width.toFloat()
                        val leftBoundary = width / 3f
                        val rightBoundary = width * 2f / 3f
                        
                        when {
                            tapPosition.x < leftBoundary -> {
                                // 左侧 1/3：上一页
                                coroutineScope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                    // 如果已经是第一页，不做任何事（没有更早的章节了）
                                }
                            }
                            tapPosition.x > rightBoundary -> {
                                // 右侧 1/3：下一页
                                coroutineScope.launch {
                                    if (pagerState.currentPage < pages.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                    // 如果已经是最后一页，不做任何事（没有更多章节了）
                                }
                            }
                            else -> {
                                // 中间 1/3：切换菜单
                                onToggleMenu()
                            }
                        }
                    }
                }
            }
    ) {
        // HorizontalPager 支持滑动翻页
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex)
            
            if (page != null) {
                PageContentView(
                    page = page,
                    settings = settings,
                    footnotes = footnotes,
                    onImageClick = onImageClick,
                    onFootnoteClick = onFootnoteClick,
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}

/**
 * 单页内容视图
 */
@Composable
private fun PageContentView(
    page: ReaderPage,
    settings: ReaderSettings,
    footnotes: Map<String, ContentElement.Footnote>,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = settings.marginHorizontal.dp,
                vertical = settings.marginVertical.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 章节标题（仅章节第一页显示）
            if (page.showChapterTitle && page.chapterTitle != null) {
                ChapterTitleView(title = page.chapterTitle, settings = settings)
            }
            
            // 内容元素
            page.elements.forEachIndexed { idx, chapterElement ->
                ContentElementView(
                    element = chapterElement.element,
                    settings = settings,
                    onImageClick = onImageClick,
                    onFootnoteClick = { footnoteId ->
                        footnotes[footnoteId]?.let { onFootnoteClick(it) }
                    },
                    onLinkClick = onLinkClick
                )
                
                if (idx < page.elements.size - 1) {
                    Spacer(modifier = Modifier.height(settings.paragraphSpacing.dp))
                }
            }
        }
    }
}
