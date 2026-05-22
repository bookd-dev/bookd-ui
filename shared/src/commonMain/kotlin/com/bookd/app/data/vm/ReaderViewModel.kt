package com.bookd.app.data.vm

import androidx.compose.runtime.Immutable
import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.LocalReadingProgress
import com.bookd.app.data.model.PageAnimationType
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TocItem
import com.bookd.app.data.repository.ReaderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

// ============= State =============

/**
 * 阅读器页面状态
 */
@Immutable
data class ReaderState(
    // 书籍基本信息
    val bookId: Int = 0,
    val bookTitle: String = "",
    val bookAuthor: String? = null,
    
    // 内容
    val manifest: BookManifest? = null,
    val currentChapterIndex: Int = 0,
    val currentChapter: ChapterContent? = null,
    val isLoadingChapter: Boolean = false,
    val preloadedChapters: Map<Int, ChapterContent> = emptyMap(),
    
    // 相邻章节内容（用于翻页模式的无缝滑动）
    val adjacentChapters: Map<Int, ChapterContent> = emptyMap(),
    
    // 滑动切换章节的方向
    // 1: 向前滑动（从章节3到章节4），应跳转到新章节第一页
    // -1: 向后滑动（从章节3到章节2），应跳转到新章节最后一页
    // 0: 无方向（初始状态或目录跳转）
    val pagerSlideDirection: Int = 0,
    
    // 进度（本地）
    val localProgress: LocalReadingProgress? = null,
    val currentParagraphIndex: Int = 0,
    val scrollOffset: Int = 0,
    val currentPageIndex: Int = 0,
    val calculatedProgress: Double = 0.0,
    
    // 进度冲突
    val hasProgressConflict: Boolean = false,
    val remoteProgress: ReadingProgressResponse? = null,
    
    // 设置
    val readerSettings: ReaderSettings = ReaderSettings(),
    
    // 书签
    val bookmarks: List<BookmarkResponse> = emptyList(),
    val isAddingBookmark: Boolean = false,
    
    // 加载状态
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /**
     * 总章节数（仅包含 inToc=true 的章节，用于进度上报等语义场景）
     */
    val totalChapters: Int
        get() = manifest?.totalChapters ?: 0
    
    /**
     * 可访问章节的数量上界（基于 spine 的最大索引 + 1）。
     * spine 包含所有文档的 index（不论 inToc），因此比 totalChapters 更准确地
     * 反映了可加载章节的索引范围。所有索引边界检查应使用此属性而非 totalChapters。
     */
    val chapterCount: Int
        get() = ((manifest?.spine?.maxOrNull() ?: -1) + 1)
    
    /**
     * 当前章节标题
     */
    val currentChapterTitle: String?
        get() = currentChapter?.title ?: findTocItem(currentChapterIndex)?.title
    
    /**
     * 进度百分比（0-100）
     */
    val progressPercent: Int
        get() = (calculatedProgress * 100).toInt()
    
    /**
     * 是否有上一章
     */
    val hasPreviousChapter: Boolean
        get() = currentChapterIndex > 0
    
    /**
     * 是否有下一章
     */
    val hasNextChapter: Boolean
        get() = currentChapterIndex < chapterCount - 1
    
    /**
     * 查找目录项
     */
    private fun findTocItem(index: Int): TocItem? {
        fun findInList(items: List<TocItem>): TocItem? {
            for (item in items) {
                if (item.index == index) return item
                findInList(item.children)?.let { return it }
            }
            return null
        }
        return manifest?.toc?.let { findInList(it) }
    }
}

// ============= Effect =============

/**
 * 副作用（一次性事件）
 */
sealed interface ReaderEffect {
    /** 导航返回 */
    data object NavigateBack : ReaderEffect
    
    /** 跳转书籍详情 */
    data class NavigateToBookDetail(val bookId: Int) : ReaderEffect
    
    /** 滚动到指定位置 */
    data class ScrollToPosition(val paragraphIndex: Int, val offset: Int) : ReaderEffect
    
    /** 翻页到指定页 */
    data class ScrollToPage(val pageIndex: Int) : ReaderEffect
    
    /** 书签已添加 */
    data object BookmarkAdded : ReaderEffect
    
    /** 书签已删除 */
    data object BookmarkDeleted : ReaderEffect
    
    /** 进度已保存 */
    data object ProgressSaved : ReaderEffect
}

// ============= ViewModel =============

/**
 * 阅读器 ViewModel
 */
class ReaderViewModel(
    private val readerRepository: ReaderRepository
) : BaseViewModel() {
    
    // State
    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()
    
    // Effect
    private val _effect = MutableSharedFlow<ReaderEffect>()
    val effect: SharedFlow<ReaderEffect> = _effect.asSharedFlow()
    
    // 进度保存防抖 Job
    private var progressSaveJob: Job? = null
    
    // 自动同步 Job
    private var autoSyncJob: Job? = null
    
    // ============= 初始化 =============
    
    fun loadBook(bookId: Int, startChapterIndex: Int? = null) {
        scope.launch {
            // 清理旧书籍的状态，避免使用旧缓存
            _state.update { 
                it.copy(
                    bookId = bookId, 
                    isLoading = true, 
                    error = null,
                    currentChapter = null,
                    adjacentChapters = emptyMap(),
                    preloadedChapters = emptyMap(),
                    bookmarks = emptyList()
                ) 
            }
            
            try {
                // 1. 加载阅读器设置
                val settings = readerRepository.getReaderSettings()
                _state.update { it.copy(readerSettings = settings) }
                
                // 2. 尝试获取带进度的 manifest
                val manifestResult = readerRepository.getBookManifestWithProgress(bookId)
                val manifest = manifestResult.getOrElse {
                    readerRepository.getBookManifest(bookId).getOrThrow()
                }
                
                _state.update { 
                    it.copy(
                        manifest = manifest,
                        bookTitle = manifest.title,
                        bookAuthor = manifest.author
                    )
                }
                
                // 3. 加载本地进度
                val localProgress = readerRepository.getLocalProgress(bookId)
                
                // 4. 尝试加载远程进度
                val remoteProgressResult = readerRepository.getRemoteProgress(bookId)
                val remoteProgress = remoteProgressResult.getOrNull()
                
                // 5. 检测进度冲突
                val hasConflict = detectProgressConflict(localProgress, remoteProgress)
                
                if (hasConflict && localProgress != null && remoteProgress != null) {
                    // 有冲突，记录状态，等待 UI 处理
                    _state.update { 
                        it.copy(
                            localProgress = localProgress,
                            remoteProgress = remoteProgress,
                            hasProgressConflict = true,
                            isLoading = false
                        )
                    }
                } else {
                    // 无冲突，确定起始章节
                    val chapterIndex = startChapterIndex 
                        ?: localProgress?.chapterIndex 
                        ?: remoteProgress?.currentPage 
                        ?: 0
                    
                    _state.update { 
                        it.copy(
                            localProgress = localProgress,
                            remoteProgress = remoteProgress,
                            currentChapterIndex = chapterIndex,
                            currentParagraphIndex = localProgress?.paragraphIndex ?: 0,
                            scrollOffset = localProgress?.scrollOffset ?: 0,
                            currentPageIndex = localProgress?.pageIndex ?: 0,
                            isLoading = false
                        )
                    }
                    
                    // 加载章节内容
                    loadChapter(chapterIndex)
                    
                    // 加载书签
                    loadBookmarks(bookId)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                throw e
            }
        }
    }
    
    fun loadChapter(chapterIndex: Int) {
        scope.launch {
            _state.update { it.copy(isLoadingChapter = true) }
            
            try {
                val bookId = _state.value.bookId
                val chapterCount = _state.value.chapterCount
                
                // 确定需要加载的章节范围（当前章节 + 向前10章历史 + 向后2章预加载）
                val chaptersToLoad = buildList {
                    for (i in maxOf(0, chapterIndex - 10) until chapterIndex) add(i)
                    add(chapterIndex)
                    for (i in (chapterIndex + 1)..minOf(chapterCount - 1, chapterIndex + 2)) add(i)
                }
                
                // 加载所有需要的章节
                val loadedChapters = mutableMapOf<Int, ChapterContent>()
                for (index in chaptersToLoad) {
                    val cached = _state.value.preloadedChapters[index]
                    if (cached != null) {
                        loadedChapters[index] = cached
                    } else {
                        val chapter = readerRepository.getChapterContent(bookId, index).getOrThrow()
                        loadedChapters[index] = chapter
                    }
                }
                
                _state.update { 
                    it.copy(
                        currentChapter = loadedChapters[chapterIndex],
                        currentChapterIndex = chapterIndex,
                        adjacentChapters = loadedChapters,
                        preloadedChapters = it.preloadedChapters + loadedChapters,
                        isLoadingChapter = false
                    )
                }
                
                // 更新本地进度
                updateLocalProgress()
                
                // 预加载更远的章节
                preloadFurtherChapters(chapterIndex)
                
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingChapter = false) }
                throw e
            }
        }
    }
    
    /**
     * 预加载更远的章节（当前章节 ±2）
     */
    private fun preloadFurtherChapters(currentIndex: Int) {
        val chapterCount = _state.value.chapterCount
        val bookId = _state.value.bookId
        
        listOf(currentIndex - 2, currentIndex + 2)
            .filter { it in 0 until chapterCount }
            .filter { !_state.value.preloadedChapters.containsKey(it) }
            .forEach { index ->
                scope.launch {
                    try {
                        val chapter = readerRepository.getChapterContent(bookId, index).getOrThrow()
                        _state.update { state ->
                            state.copy(
                                preloadedChapters = state.preloadedChapters + (index to chapter)
                            )
                        }
                    } catch (_: Exception) {
                        // 预加载失败不报错
                    }
                }
            }
    }
    
    fun loadBookmarks(bookId: Int) {
        scope.launch {
            try {
                val bookmarks = readerRepository.getBookmarks(bookId).getOrElse { emptyList() }
                _state.update { it.copy(bookmarks = bookmarks) }
            } catch (_: Exception) {
                // 加载书签失败不报错
            }
        }
    }
    
    // ============= 进度管理 =============
    
    fun updateScrollPosition(chapterIndex: Int, paragraphIndex: Int, scrollOffset: Int) {
        _state.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentParagraphIndex = paragraphIndex,
                scrollOffset = scrollOffset
            )
        }
        scheduleProgressSave()
    }
    
    fun updatePagePosition(pageIndex: Int) {
        _state.update { it.copy(currentPageIndex = pageIndex) }
        scheduleProgressSave()
    }
    
    private fun scheduleProgressSave() {
        progressSaveJob?.cancel()
        progressSaveJob = scope.launch {
            delay(3000) // 3秒防抖
            updateLocalProgress()
            syncProgressIfNeeded()
        }
    }
    
    private fun updateLocalProgress() {
        val state = _state.value
        val progress = calculateProgress()
        
        _state.update { it.copy(calculatedProgress = progress) }
        
        val localProgress = LocalReadingProgress(
            bookId = state.bookId,
            chapterIndex = state.currentChapterIndex,
            paragraphIndex = state.currentParagraphIndex,
            scrollOffset = state.scrollOffset,
            pageIndex = state.currentPageIndex,
            progress = progress,
            lastReadAt = Clock.System.now().toEpochMilliseconds()
        )
        
        scope.launch {
            readerRepository.saveLocalProgress(localProgress)
        }
    }
    
    private fun calculateProgress(): Double {
        val state = _state.value
        val manifest = state.manifest ?: return 0.0
        val currentChapter = state.currentChapter ?: return 0.0
        
        val previousChaptersWords = manifest.toc
            .filter { it.index < state.currentChapterIndex }
            .sumOf { it.wordCount }
        
        val totalElements = currentChapter.elements.size
        val currentChapterProgress = if (totalElements > 0) {
            state.currentParagraphIndex.toDouble() / totalElements
        } else {
            0.0
        }
        
        val currentChapterWords = manifest.toc
            .find { it.index == state.currentChapterIndex }
            ?.wordCount ?: 0
        
        val estimatedReadWords = previousChaptersWords + (currentChapterWords * currentChapterProgress)
        
        val totalWords = manifest.toc.sumOf { it.wordCount }
        
        return if (totalWords > 0) {
            (estimatedReadWords / totalWords).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }
    
    private fun syncProgressIfNeeded() {
        val state = _state.value
        val lastProgress = state.localProgress?.progress ?: 0.0
        val currentProgress = state.calculatedProgress
        
        // 进度变化超过 5% 时同步
        if (kotlin.math.abs(currentProgress - lastProgress) >= 0.05) {
            scope.launch {
                try {
                    readerRepository.updateRemoteProgress(
                        bookId = state.bookId,
                        progress = currentProgress,
                        currentChapter = state.currentChapterIndex,
                        totalChapters = state.totalChapters,
                        chapterPageIndex = state.currentPageIndex,
                        chapterScrollPercent = if (state.readerSettings.pageMode == PageMode.SCROLL) {
                            state.currentParagraphIndex.toDouble() / (state.currentChapter?.elements?.size ?: 1)
                        } else null
                    )
                } catch (_: Exception) {
                    // 同步失败静默处理
                }
            }
        }
    }
    
    fun saveProgress() {
        progressSaveJob?.cancel()
        scope.launch {
            updateLocalProgress()
            
            val state = _state.value
            try {
                readerRepository.updateRemoteProgress(
                    bookId = state.bookId,
                    progress = state.calculatedProgress,
                    currentChapter = state.currentChapterIndex,
                    totalChapters = state.totalChapters,
                    chapterPageIndex = state.currentPageIndex,
                    chapterScrollPercent = if (state.readerSettings.pageMode == PageMode.SCROLL) {
                        state.currentParagraphIndex.toDouble() / (state.currentChapter?.elements?.size ?: 1)
                    } else null
                )
                _effect.emit(ReaderEffect.ProgressSaved)
            } catch (_: Exception) {
                // 保存失败静默处理
            }
        }
    }
    
    // ============= 进度冲突 =============
    
    private fun detectProgressConflict(
        local: LocalReadingProgress?,
        remote: ReadingProgressResponse?
    ): Boolean {
        if (local == null || remote == null) return false
        
        return local.chapterIndex != remote.currentPage && 
               local.progress > 0 && 
               remote.progress > 0
    }
    
    fun useLocalProgress() {
        val state = _state.value
        val localProgress = state.localProgress ?: return
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                currentChapterIndex = localProgress.chapterIndex,
                currentParagraphIndex = localProgress.paragraphIndex,
                scrollOffset = localProgress.scrollOffset,
                currentPageIndex = localProgress.pageIndex
            )
        }
        
        loadChapter(localProgress.chapterIndex)
        
        // 用本地进度覆盖远程
        scope.launch {
            try {
                readerRepository.updateRemoteProgress(
                    bookId = state.bookId,
                    progress = localProgress.progress,
                    currentChapter = localProgress.chapterIndex,
                    totalChapters = state.totalChapters,
                    chapterPageIndex = localProgress.pageIndex,
                    chapterScrollPercent = null
                )
            } catch (_: Exception) {
                // 静默处理
            }
        }
    }
    
    fun useRemoteProgress() {
        val state = _state.value
        val remoteProgress = state.remoteProgress ?: return
        
        val chapterIndex = remoteProgress.currentPage
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                currentChapterIndex = chapterIndex,
                currentParagraphIndex = 0,
                scrollOffset = 0,
                currentPageIndex = 0
            )
        }
        
        loadChapter(chapterIndex)
        
        // 删除本地进度
        scope.launch {
            readerRepository.deleteLocalProgress(state.bookId)
        }
    }
    
    fun dismissProgressConflict() {
        _state.update { it.copy(hasProgressConflict = false) }
    }
    
    // ============= 章节导航 =============
    
    fun nextChapter() {
        val state = _state.value
        if (state.hasNextChapter) {
            jumpToChapter(state.currentChapterIndex + 1)
        }
    }
    
    fun previousChapter() {
        val state = _state.value
        if (state.hasPreviousChapter) {
            jumpToChapter(state.currentChapterIndex - 1)
        }
    }
    
    fun jumpToChapter(index: Int) {
        val state = _state.value
        if (index < 0 || index >= state.chapterCount) return
        
        _state.update { 
            it.copy(
                currentParagraphIndex = 0,
                scrollOffset = 0,
                currentPageIndex = 0
            )
        }
        
        loadChapter(index)
        
        scope.launch {
            _effect.emit(ReaderEffect.ScrollToPosition(0, 0))
        }
    }
    
    fun jumpToBookmark(bookmark: BookmarkResponse) {
        _state.update { 
            it.copy(
                currentParagraphIndex = bookmark.paragraphIndex ?: 0
            )
        }
        
        if (bookmark.chapterIndex != _state.value.currentChapterIndex) {
            loadChapter(bookmark.chapterIndex)
        }
        
        scope.launch {
            _effect.emit(ReaderEffect.ScrollToPosition(bookmark.paragraphIndex ?: 0, 0))
        }
    }
    
    /**
     * 处理 Pager 章节切换（由 PageModeContent 触发）
     * 当用户通过滑动进入新章节时调用
     * @param direction 滑动方向：1=向前（下一章），-1=向后（上一章）
     */
    fun onPagerChapterChanged(newChapterIndex: Int, direction: Int) {
        val currentIndex = _state.value.currentChapterIndex
        if (newChapterIndex == currentIndex) return
        
        val chapterCount = _state.value.chapterCount
        if (newChapterIndex < 0 || newChapterIndex >= chapterCount) return
        
        _state.update { state ->
            state.copy(
                currentChapterIndex = newChapterIndex,
                currentChapter = state.adjacentChapters[newChapterIndex],
                currentPageIndex = 0,
                pagerSlideDirection = direction
            )
        }
        
        // 更新本地进度
        updateLocalProgress()
        
        // 异步加载新的相邻章节
        loadAdjacentChaptersForPager(newChapterIndex)
    }
    
    /**
     * 为 Pager 加载新的相邻章节
     */
    private fun loadAdjacentChaptersForPager(centerIndex: Int) {
        scope.launch {
            val bookId = _state.value.bookId
            val chapterCount = _state.value.chapterCount
            val currentAdjacent = _state.value.adjacentChapters
            
            // 向前保留10章历史 + 当前章 + 向后预加载2章
            val neededMin = maxOf(0, centerIndex - 10)
            val neededMax = minOf(chapterCount - 1, centerIndex + 2)
            val neededIndices = (neededMin..neededMax).toList()
            
            // 后端（向后方向）只增不减，避免相邻章节切换时反复增删 adjacentChapters
            // 导致 LazyColumn 布局抖动，进而触发章节切换死循环。
            // 前端（向前方向）正常裁剪，不影响历史章节访问。
            // 软上限 25 章（前10 + 当前 + 后14），防止大跳转时窗口无限增长。
            val trimMin = neededMin
            val rawMax = maxOf(neededMax, currentAdjacent.keys.maxOrNull() ?: neededMax)
            val trimMax = minOf(rawMax, trimMin + 24)
            val validRange = (trimMin..trimMax).toSet()
            
            val missingIndices = neededIndices.filter { !currentAdjacent.containsKey(it) }
            
            if (missingIndices.isEmpty()) {
                // 只有当 adjacentChapters 的键集合与 validRange 不同时才裁剪，
                // 避免产生新 Map 对象触发不必要的重组
                if (currentAdjacent.keys != validRange) {
                    _state.update { state ->
                        state.copy(
                            adjacentChapters = state.adjacentChapters.filterKeys { it in validRange }
                        )
                    }
                }
                return@launch
            }
            
            val newChapters = mutableMapOf<Int, ChapterContent>()
            for (index in missingIndices) {
                val cached = _state.value.preloadedChapters[index]
                if (cached != null) {
                    newChapters[index] = cached
                } else {
                    try {
                        val chapter = readerRepository.getChapterContent(bookId, index).getOrThrow()
                        newChapters[index] = chapter
                    } catch (_: Exception) {
                        // 加载失败静默处理
                    }
                }
            }
            
            _state.update { state ->
                state.copy(
                    adjacentChapters = (state.adjacentChapters + newChapters).filterKeys { it in validRange },
                    preloadedChapters = state.preloadedChapters + newChapters
                )
            }
            
            preloadFurtherChapters(centerIndex)
        }
    }
    
    /**
     * 处理滚动模式章节切换（由 ScrollModeContent 触发）
     * 当用户滚动时某章内容占屏超过一半，自动调用此方法更新当前章节
     */
    fun onScrollChapterChanged(newChapterIndex: Int) {
        val currentIndex = _state.value.currentChapterIndex
        if (newChapterIndex == currentIndex) return

        val chapterCount = _state.value.chapterCount
        if (newChapterIndex < 0 || newChapterIndex >= chapterCount) return

        _state.update { state ->
            state.copy(
                currentChapterIndex = newChapterIndex,
                currentChapter = state.adjacentChapters[newChapterIndex],
                currentParagraphIndex = 0,
                scrollOffset = 0
            )
        }

        // 更新本地进度
        updateLocalProgress()

        // 异步补充加载新的相邻章节
        loadAdjacentChaptersForPager(newChapterIndex)
    }

    // ============= 设置 =============
    
    fun updateSettings(settings: ReaderSettings) {
        _state.update { it.copy(readerSettings = settings) }
        syncSettings(settings)
    }
    
    fun updateFontSize(size: Int) {
        val newSettings = _state.value.readerSettings.copy(fontSize = size)
        updateSettings(newSettings)
    }
    
    fun updateLineHeight(height: Double) {
        val newSettings = _state.value.readerSettings.copy(lineHeight = height)
        updateSettings(newSettings)
    }
    
    fun updateParagraphSpacing(spacing: Int) {
        val newSettings = _state.value.readerSettings.copy(paragraphSpacing = spacing)
        updateSettings(newSettings)
    }
    
    fun updateMarginHorizontal(margin: Int) {
        val newSettings = _state.value.readerSettings.copy(marginHorizontal = margin)
        updateSettings(newSettings)
    }
    
    fun updateMarginVertical(margin: Int) {
        val newSettings = _state.value.readerSettings.copy(marginVertical = margin)
        updateSettings(newSettings)
    }
    
    fun updatePageMode(mode: PageMode) {
        val newSettings = _state.value.readerSettings.copy(pageMode = mode)
        updateSettings(newSettings)
    }
    
    fun updatePageAnimationType(type: PageAnimationType) {
        val newSettings = _state.value.readerSettings.copy(pageAnimationType = type)
        updateSettings(newSettings)
    }
    
    fun toggleFirstLineIndent() {
        val currentSettings = _state.value.readerSettings
        val newSettings = currentSettings.copy(firstLineIndent = !currentSettings.firstLineIndent)
        updateSettings(newSettings)
    }
    
    private fun syncSettings(settings: ReaderSettings) {
        scope.launch {
            try {
                readerRepository.updateReaderSettings(settings)
            } catch (_: Exception) {
                // 同步失败静默处理
            }
        }
    }
    
    // ============= 书签 =============
    
    fun addBookmark(chapterIndex: Int, paragraphIndex: Int, note: String?) {
        scope.launch {
            _state.update { it.copy(isAddingBookmark = true) }
            
            try {
                val bookmark = readerRepository.addBookmark(
                    bookId = _state.value.bookId,
                    chapterIndex = chapterIndex,
                    paragraphIndex = paragraphIndex,
                    note = note
                ).getOrThrow()
                
                _state.update { state ->
                    state.copy(
                        bookmarks = state.bookmarks + bookmark,
                        isAddingBookmark = false
                    )
                }
                
                _effect.emit(ReaderEffect.BookmarkAdded)
            } catch (e: Exception) {
                _state.update { it.copy(isAddingBookmark = false) }
                throw e
            }
        }
    }
    
    fun deleteBookmark(bookmarkId: Int) {
        scope.launch {
            try {
                readerRepository.deleteBookmark(bookmarkId).getOrThrow()
                
                _state.update { state ->
                    state.copy(
                        bookmarks = state.bookmarks.filter { it.id != bookmarkId }
                    )
                }
                
                _effect.emit(ReaderEffect.BookmarkDeleted)
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    // ============= 退出 =============
    
    fun back() {
        // 保存进度后退出
        progressSaveJob?.cancel()
        scope.launch {
            updateLocalProgress()
            
            val state = _state.value
            try {
                readerRepository.updateRemoteProgress(
                    bookId = state.bookId,
                    progress = state.calculatedProgress,
                    currentChapter = state.currentChapterIndex,
                    totalChapters = state.totalChapters,
                    chapterPageIndex = state.currentPageIndex,
                    chapterScrollPercent = null
                )
            } catch (_: Exception) {
                // 静默处理
            }
            
            _effect.emit(ReaderEffect.NavigateBack)
        }
    }
    
    fun viewBookDetail() {
        scope.launch {
            _effect.emit(ReaderEffect.NavigateToBookDetail(_state.value.bookId))
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
        autoSyncJob?.cancel()
    }
}
