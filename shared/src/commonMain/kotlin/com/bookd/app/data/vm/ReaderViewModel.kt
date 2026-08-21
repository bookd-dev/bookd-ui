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
import com.bookd.app.data.model.ReadingProgressSnapshot
import com.bookd.app.data.model.ReadingPosition
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TocItem
import com.bookd.app.data.model.toReadingPosition
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

@Immutable
data class ReaderPositionRequest(
    val sequence: Long,
    val chapterIndex: Int,
    val pageIndex: Int?,
    val anchorId: String?,
    val paragraphIndex: Int,
    val offset: Int,
)

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
    val currentAnchorId: String? = null,
    val currentParagraphIndex: Int = 0,
    val scrollOffset: Int = 0,
    val currentPageIndex: Int = 0,
    val calculatedProgress: Double = 0.0,
    val pendingPositionRequest: ReaderPositionRequest? = null,
    
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

    val isProgrammaticJumpPending: Boolean
        get() = pendingPositionRequest != null
    
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
    
    // 本地进度与云同步都使用单飞最新值队列，避免较旧异步请求最后落盘。
    private var localProgressSaveJob: Job? = null
    private var pendingLocalSnapshot: ReadingProgressSnapshot? = null
    private var progressSyncDebounceJob: Job? = null
    private var progressSyncJob: Job? = null
    private var pendingRemoteSnapshot: ReadingProgressSnapshot? = null
    private var lastSyncedPosition: ReadingPosition? = null
    private var scrollRequestSequence: Long = 0L

    // 阅读设置保存防抖 Job
    private var settingsDebounceJob: Job? = null
    private var settingsSyncJob: Job? = null
    private var pendingReaderSettings: ReaderSettings? = null
    
    // ============= 初始化 =============
    
    fun loadBook(bookId: Int, startChapterIndex: Int? = null) {
        scope.launch {
            resetProgressPipelines()
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
                lastSyncedPosition = remoteProgress?.toReadingPosition()
                
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
                        ?: remoteProgress?.chapterIndex
                        ?: 0
                    val initialAnchorId = if (startChapterIndex == null) {
                        localProgress?.anchorId ?: remoteProgress?.anchorId
                    } else {
                        null
                    }
                    val initialParagraphIndex = if (startChapterIndex == null) {
                        localProgress?.paragraphIndex ?: remoteProgress?.paragraphIndex ?: 0
                    } else {
                        0
                    }
                    val initialScrollOffset = if (startChapterIndex == null) {
                        localProgress?.scrollOffset ?: remoteProgress?.scrollOffset ?: 0
                    } else {
                        0
                    }
                    val initialPageIndex = if (startChapterIndex == null) {
                        localProgress?.pageIndex ?: remoteProgress?.chapterPageIndex ?: 0
                    } else {
                        0
                    }
                    
                    _state.update { 
                        it.copy(
                            localProgress = localProgress,
                            remoteProgress = remoteProgress,
                            currentChapterIndex = chapterIndex,
                            currentAnchorId = initialAnchorId,
                            currentParagraphIndex = initialParagraphIndex,
                            scrollOffset = initialScrollOffset,
                            currentPageIndex = initialPageIndex,
                            isLoading = false
                        )
                    }
                    
                    // 加载章节内容
                    loadChapter(chapterIndex)

                    if (
                        initialAnchorId != null ||
                        initialParagraphIndex > 0 ||
                        initialScrollOffset > 0 ||
                        initialPageIndex > 0
                    ) {
                        requestScrollToPosition(
                            chapterIndex = chapterIndex,
                            pageIndex = initialPageIndex,
                            anchorId = initialAnchorId,
                            paragraphIndex = initialParagraphIndex,
                            offset = initialScrollOffset
                        )
                    }
                    
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
                
                // 章节内容就绪后刷新本地快照；初始加载不主动制造一次云端写入。
                recordCurrentProgress(scheduleCloudSync = false)
                
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
    
    fun updateScrollPosition(chapterIndex: Int, anchorId: String?, paragraphIndex: Int, scrollOffset: Int) {
        if (_state.value.isProgrammaticJumpPending) return
        val chapterChanged = chapterIndex != _state.value.currentChapterIndex
        _state.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentChapter = it.adjacentChapters[chapterIndex] ?: it.currentChapter,
                currentAnchorId = anchorId,
                currentParagraphIndex = paragraphIndex,
                scrollOffset = scrollOffset
            )
        }
        recordCurrentProgress(immediateCloudSync = chapterChanged)
    }
    
    fun updatePagePosition(
        chapterIndex: Int,
        pageIndex: Int,
        anchorId: String?,
        paragraphIndex: Int,
    ) {
        if (_state.value.isProgrammaticJumpPending) return
        val chapterChanged = chapterIndex != _state.value.currentChapterIndex
        _state.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentChapter = it.adjacentChapters[chapterIndex] ?: it.currentChapter,
                currentPageIndex = pageIndex,
                currentAnchorId = anchorId,
                currentParagraphIndex = paragraphIndex,
                scrollOffset = 0,
            )
        }
        recordCurrentProgress(immediateCloudSync = chapterChanged)
    }

    private fun recordCurrentProgress(
        immediateCloudSync: Boolean = false,
        scheduleCloudSync: Boolean = true,
    ): ReadingProgressSnapshot {
        val snapshot = captureCurrentProgressSnapshot()
        _state.update {
            it.copy(
                calculatedProgress = snapshot.progress,
                localProgress = snapshot.toLocalProgress(),
            )
        }
        enqueueLocalProgress(snapshot)
        if (scheduleCloudSync) enqueueRemoteProgress(snapshot, immediateCloudSync)
        return snapshot
    }

    private fun captureCurrentProgressSnapshot(): ReadingProgressSnapshot {
        val state = _state.value
        val progress = calculateProgress(state)
        val elementCount = state.currentChapter?.elements?.size ?: 0
        return ReadingProgressSnapshot(
            bookId = state.bookId,
            position = ReadingPosition(
                chapterIndex = state.currentChapterIndex,
                anchorId = state.currentAnchorId,
                paragraphIndex = state.currentParagraphIndex,
                scrollOffset = state.scrollOffset,
                pageIndex = state.currentPageIndex,
            ),
            progress = progress,
            totalChapters = state.totalChapters,
            chapterScrollPercent = if (
                state.readerSettings.pageMode == PageMode.SCROLL && elementCount > 0
            ) {
                state.currentParagraphIndex.toDouble() / elementCount
            } else {
                null
            },
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private fun enqueueLocalProgress(snapshot: ReadingProgressSnapshot) {
        pendingLocalSnapshot = snapshot
        startLocalProgressSaveIfNeeded()
    }

    private fun startLocalProgressSaveIfNeeded() {
        if (localProgressSaveJob?.isActive == true) return
        localProgressSaveJob = scope.launch {
            try {
                while (true) {
                    val snapshot = pendingLocalSnapshot ?: break
                    pendingLocalSnapshot = null
                    readerRepository.saveLocalProgress(snapshot)
                }
            } finally {
                localProgressSaveJob = null
                if (pendingLocalSnapshot != null) startLocalProgressSaveIfNeeded()
            }
        }
    }

    private fun enqueueRemoteProgress(snapshot: ReadingProgressSnapshot, immediate: Boolean) {
        if (
            snapshot.position == lastSyncedPosition &&
            progressSyncJob?.isActive != true &&
            pendingRemoteSnapshot == null
        ) return
        pendingRemoteSnapshot = snapshot
        progressSyncDebounceJob?.cancel()
        if (immediate) {
            startProgressSyncIfNeeded()
        } else {
            progressSyncDebounceJob = scope.launch {
                delay(PROGRESS_SYNC_DEBOUNCE_MS)
                startProgressSyncIfNeeded()
            }
        }
    }

    private fun startProgressSyncIfNeeded() {
        if (progressSyncJob?.isActive == true) return
        progressSyncJob = scope.launch {
            var failed = false
            try {
                while (true) {
                    val snapshot = pendingRemoteSnapshot ?: break
                    pendingRemoteSnapshot = null
                    val result = readerRepository.syncProgressSnapshot(snapshot)
                    result.onSuccess { response ->
                        lastSyncedPosition = response.toReadingPosition()
                        _state.update { it.copy(remoteProgress = response) }
                    }.onFailure {
                        pendingRemoteSnapshot = pendingRemoteSnapshot ?: snapshot
                        failed = true
                    }
                    if (failed) break
                }
            } finally {
                progressSyncJob = null
                if (!failed && pendingRemoteSnapshot != null) startProgressSyncIfNeeded()
            }
        }
    }

    private suspend fun flushProgress(snapshot: ReadingProgressSnapshot): Boolean {
        pendingLocalSnapshot = snapshot
        startLocalProgressSaveIfNeeded()
        localProgressSaveJob?.join()

        progressSyncDebounceJob?.cancel()
        if (
            snapshot.position != lastSyncedPosition ||
            progressSyncJob?.isActive == true ||
            pendingRemoteSnapshot != null
        ) {
            pendingRemoteSnapshot = snapshot
            startProgressSyncIfNeeded()
            progressSyncJob?.join()
        }
        return snapshot.position == lastSyncedPosition
    }

    private fun calculateProgress(state: ReaderState = _state.value): Double {
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
    
    fun saveProgress() {
        scope.launch {
            val snapshot = recordCurrentProgress(scheduleCloudSync = false)
            if (flushProgress(snapshot)) {
                _effect.emit(ReaderEffect.ProgressSaved)
            }
        }
    }
    
    // ============= 进度冲突 =============
    
    private fun detectProgressConflict(
        local: LocalReadingProgress?,
        remote: ReadingProgressResponse?
    ): Boolean {
        if (local == null || remote == null) return false
        
        return local.progress > 0 &&
            remote.progress > 0 &&
            local.toReadingPosition() != remote.toReadingPosition()
    }
    
    fun useLocalProgress() {
        val state = _state.value
        val localProgress = state.localProgress ?: return
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                currentChapterIndex = localProgress.chapterIndex,
                currentAnchorId = localProgress.anchorId,
                currentParagraphIndex = localProgress.paragraphIndex,
                scrollOffset = localProgress.scrollOffset,
                currentPageIndex = localProgress.pageIndex
            )
        }
        
        loadChapter(localProgress.chapterIndex)
        requestScrollToPosition(
            chapterIndex = localProgress.chapterIndex,
            pageIndex = localProgress.pageIndex,
            anchorId = localProgress.anchorId,
            paragraphIndex = localProgress.paragraphIndex,
            offset = localProgress.scrollOffset
        )
        
        val snapshot = ReadingProgressSnapshot(
            bookId = state.bookId,
            position = localProgress.toReadingPosition(),
            progress = localProgress.progress,
            totalChapters = state.totalChapters,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        enqueueLocalProgress(snapshot)
        enqueueRemoteProgress(snapshot, immediate = true)
    }
    
    fun useRemoteProgress() {
        val state = _state.value
        val remoteProgress = state.remoteProgress ?: return
        
        val chapterIndex = remoteProgress.chapterIndex
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                currentChapterIndex = chapterIndex,
                currentAnchorId = remoteProgress.anchorId,
                currentParagraphIndex = remoteProgress.paragraphIndex ?: 0,
                scrollOffset = remoteProgress.scrollOffset ?: 0,
                currentPageIndex = remoteProgress.chapterPageIndex ?: 0
            )
        }
        
        loadChapter(chapterIndex)
        requestScrollToPosition(
            chapterIndex = chapterIndex,
            pageIndex = remoteProgress.chapterPageIndex,
            anchorId = remoteProgress.anchorId,
            paragraphIndex = remoteProgress.paragraphIndex ?: 0,
            offset = remoteProgress.scrollOffset ?: 0
        )
        
        val localProgress = LocalReadingProgress(
            bookId = state.bookId,
            chapterIndex = chapterIndex,
            anchorId = remoteProgress.anchorId,
            paragraphIndex = remoteProgress.paragraphIndex ?: 0,
            scrollOffset = remoteProgress.scrollOffset ?: 0,
            pageIndex = remoteProgress.chapterPageIndex ?: 0,
            progress = remoteProgress.progress,
            lastReadAt = Clock.System.now().toEpochMilliseconds(),
        )
        _state.update { it.copy(localProgress = localProgress) }
        enqueueLocalProgress(
            ReadingProgressSnapshot(
                bookId = state.bookId,
                position = localProgress.toReadingPosition(),
                progress = remoteProgress.progress,
                totalChapters = state.totalChapters,
                chapterTotalPages = remoteProgress.chapterTotalPages,
                chapterScrollPercent = remoteProgress.chapterScrollPercent,
                updatedAt = localProgress.lastReadAt,
            )
        )
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
                currentAnchorId = null,
                scrollOffset = 0,
                currentPageIndex = 0
            )
        }
        
        loadChapter(index)
        requestScrollToPosition(index, anchorId = null, paragraphIndex = 0, offset = 0)
    }
    
    fun jumpToBookmark(bookmark: BookmarkResponse) {
        _state.update { 
            it.copy(
                currentAnchorId = bookmark.anchorId,
                currentParagraphIndex = bookmark.paragraphIndex ?: 0
            )
        }
        
        if (bookmark.chapterIndex != _state.value.currentChapterIndex) {
            loadChapter(bookmark.chapterIndex)
        }
        
        requestScrollToPosition(
            chapterIndex = bookmark.chapterIndex,
            anchorId = bookmark.anchorId,
            paragraphIndex = bookmark.paragraphIndex ?: 0,
            offset = bookmark.scrollOffset ?: 0
        )
    }

    fun jumpToInternalLink(chapterIndex: Int, anchorId: String?) {
        val state = _state.value
        if (chapterIndex < 0 || chapterIndex >= state.chapterCount) return

        _state.update {
            it.copy(
                currentAnchorId = anchorId,
                currentParagraphIndex = 0,
                scrollOffset = 0,
                currentPageIndex = 0
            )
        }

        if (chapterIndex != state.currentChapterIndex) {
            loadChapter(chapterIndex)
        }

        requestScrollToPosition(
            chapterIndex = chapterIndex,
            anchorId = anchorId,
            paragraphIndex = 0,
            offset = 0
        )
    }

    fun onProgrammaticScrollCompleted(
        chapterIndex: Int,
        anchorId: String?,
        paragraphIndex: Int,
        scrollOffset: Int,
        pageIndex: Int? = null,
        sequence: Long? = null,
    ) {
        if (
            sequence != null &&
            _state.value.pendingPositionRequest?.sequence != sequence
        ) return
        _state.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentPageIndex = pageIndex ?: it.currentPageIndex,
                currentAnchorId = anchorId,
                currentParagraphIndex = paragraphIndex,
                scrollOffset = scrollOffset,
                pendingPositionRequest = null,
            )
        }
        recordCurrentProgress()
    }

    private fun requestScrollToPosition(
        chapterIndex: Int,
        pageIndex: Int? = null,
        anchorId: String?,
        paragraphIndex: Int,
        offset: Int
    ) {
        scrollRequestSequence += 1
        val sequence = scrollRequestSequence
        _state.update {
            it.copy(
                pendingPositionRequest = ReaderPositionRequest(
                    sequence = sequence,
                    chapterIndex = chapterIndex,
                    pageIndex = pageIndex,
                    anchorId = anchorId,
                    paragraphIndex = paragraphIndex,
                    offset = offset,
                )
            )
        }
    }
    
    /**
     * 处理 Pager 章节切换（由 PageModeContent 触发）
     * 当用户通过滑动进入新章节时调用
     * @param direction 滑动方向：1=向前（下一章），-1=向后（上一章）
     */
    fun onPagerChapterChanged(newChapterIndex: Int, direction: Int) {
        if (_state.value.isProgrammaticJumpPending) return

        val currentIndex = _state.value.currentChapterIndex
        if (newChapterIndex == currentIndex) return
        
        val chapterCount = _state.value.chapterCount
        if (newChapterIndex < 0 || newChapterIndex >= chapterCount) return
        
        _state.update { state ->
            state.copy(
                currentChapterIndex = newChapterIndex,
                currentChapter = state.adjacentChapters[newChapterIndex],
                currentPageIndex = 0,
                currentAnchorId = null,
                currentParagraphIndex = 0,
                scrollOffset = 0,
                pagerSlideDirection = direction
            )
        }
        
        // 章节边界立即同步；随后 Pager 的精确页位置会再合并为最新快照。
        recordCurrentProgress(immediateCloudSync = true)
        
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
        if (_state.value.isProgrammaticJumpPending) return

        val currentIndex = _state.value.currentChapterIndex
        if (newChapterIndex == currentIndex) return

        val chapterCount = _state.value.chapterCount
        if (newChapterIndex < 0 || newChapterIndex >= chapterCount) return

        _state.update { state ->
            state.copy(
                currentChapterIndex = newChapterIndex,
                currentChapter = state.adjacentChapters[newChapterIndex],
                currentAnchorId = null,
                currentParagraphIndex = 0,
                scrollOffset = 0
            )
        }

        // 章节边界立即同步；后续视口位置事件会补齐精确段落。
        recordCurrentProgress(immediateCloudSync = true)

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
        pendingReaderSettings = settings
        settingsDebounceJob?.cancel()
        settingsDebounceJob = scope.launch {
            delay(500)
            startSettingsSyncIfNeeded()
        }
    }

    private fun startSettingsSyncIfNeeded() {
        if (settingsSyncJob?.isActive == true) return

        settingsSyncJob = scope.launch {
            while (true) {
                val settings = pendingReaderSettings ?: break
                pendingReaderSettings = null
                readerRepository.updateReaderSettings(settings)

                if (pendingReaderSettings == null) break
                delay(500)
            }
        }
    }
    
    // ============= 书签 =============
    
    fun addBookmark(chapterIndex: Int, anchorId: String?, paragraphIndex: Int, scrollOffset: Int, note: String?) {
        scope.launch {
            _state.update { it.copy(isAddingBookmark = true) }
            
            try {
                val bookmark = readerRepository.addBookmark(
                    bookId = _state.value.bookId,
                    chapterIndex = chapterIndex,
                    anchorId = anchorId,
                    paragraphIndex = paragraphIndex,
                    scrollOffset = scrollOffset,
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

    fun addBookmarkAtCurrentPosition(note: String? = null) {
        val state = _state.value
        addBookmark(
            chapterIndex = state.currentChapterIndex,
            anchorId = state.currentAnchorId,
            paragraphIndex = state.currentParagraphIndex,
            scrollOffset = state.scrollOffset,
            note = note
        )
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
        scope.launch {
            val snapshot = recordCurrentProgress(scheduleCloudSync = false)
            flushProgress(snapshot)
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
        localProgressSaveJob?.cancel()
        progressSyncDebounceJob?.cancel()
        progressSyncJob?.cancel()
        settingsDebounceJob?.cancel()
        settingsSyncJob?.cancel()
    }

    private fun resetProgressPipelines() {
        localProgressSaveJob?.cancel()
        progressSyncDebounceJob?.cancel()
        progressSyncJob?.cancel()
        localProgressSaveJob = null
        progressSyncDebounceJob = null
        progressSyncJob = null
        pendingLocalSnapshot = null
        pendingRemoteSnapshot = null
        lastSyncedPosition = null
    }

    private companion object {
        const val PROGRESS_SYNC_DEBOUNCE_MS = 600L
    }
}
