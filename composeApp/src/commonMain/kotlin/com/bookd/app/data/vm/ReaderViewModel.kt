package com.bookd.app.data.vm

import androidx.compose.runtime.Immutable
import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
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

// ============= MVI State =============

/**
 * 目录排序顺序
 */
enum class TocSortOrder {
    ASC, DESC
}

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
    // Key: 章节索引, Value: 章节内容
    // 例如当前在第5章时: {4 -> Chapter4, 5 -> Chapter5, 6 -> Chapter6}
    val adjacentChapters: Map<Int, ChapterContent> = emptyMap(),
    
    // 滑动切换章节的方向（用于决定跳转到章节的第一页还是最后一页）
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
    
    // UI 状态
    val showMenu: Boolean = false,
    val showTopMenu: Boolean = false,
    val showTocSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showBookmarkMenu: Boolean = false,
    val bookmarkMenuParagraphIndex: Int? = null,
    val showProgressConflictDialog: Boolean = false,
    
    // 目录排序
    val tocSortOrder: TocSortOrder = TocSortOrder.ASC,
    
    // 图片预览
    val showImagePreview: Boolean = false,
    val previewImageUrl: String? = null,
    val previewImageAlt: String? = null,
    
    // 脚注弹窗
    val showFootnoteDialog: Boolean = false,
    val currentFootnote: ContentElement.Footnote? = null,
    
    // 加载状态
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /**
     * 总章节数
     */
    val totalChapters: Int
        get() = manifest?.totalChapters ?: 0
    
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
        get() = currentChapterIndex < totalChapters - 1
    
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
    
    /**
     * 获取排序后的目录
     */
    val sortedToc: List<TocItem>
        get() = when (tocSortOrder) {
            TocSortOrder.ASC -> manifest?.toc ?: emptyList()
            TocSortOrder.DESC -> manifest?.toc?.reversed() ?: emptyList()
        }
}

// ============= MVI Intent =============

/**
 * 用户意图（用户操作）
 */
sealed interface ReaderIntent {
    // 初始化
    data class LoadBook(val bookId: Int, val startChapterIndex: Int? = null) : ReaderIntent
    data class LoadChapter(val chapterIndex: Int) : ReaderIntent
    
    // 进度
    data class UpdateScrollPosition(val paragraphIndex: Int, val scrollOffset: Int) : ReaderIntent
    data class UpdatePagePosition(val pageIndex: Int) : ReaderIntent
    data object SaveProgress : ReaderIntent
    
    // 进度冲突
    data object UseLocalProgress : ReaderIntent
    data object UseRemoteProgress : ReaderIntent
    data object DismissProgressConflict : ReaderIntent
    
    // 菜单
    data object ToggleMenu : ReaderIntent
    data object ShowTocSheet : ReaderIntent
    data object HideTocSheet : ReaderIntent
    data object ShowSettingsSheet : ReaderIntent
    data object HideSettingsSheet : ReaderIntent
    
    // 右上角菜单
    data object ShowTopMenu : ReaderIntent
    data object HideTopMenu : ReaderIntent
    data object ViewBookDetail : ReaderIntent
    
    // 目录排序
    data object ToggleTocSortOrder : ReaderIntent
    
    // 章节导航
    data object NextChapter : ReaderIntent
    data object PreviousChapter : ReaderIntent
    data class JumpToChapter(val index: Int) : ReaderIntent
    data class JumpToBookmark(val bookmark: BookmarkResponse) : ReaderIntent
    
    // Pager 章节切换（由 PageModeContent 触发，当用户滑动到新章节时）
    // direction: 1=向前（下一章），-1=向后（上一章）
    data class OnPagerChapterChanged(val newChapterIndex: Int, val direction: Int) : ReaderIntent
    
    // 设置
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent
    data class UpdateFontSize(val size: Int) : ReaderIntent
    data class UpdateLineHeight(val height: Double) : ReaderIntent
    data class UpdateParagraphSpacing(val spacing: Int) : ReaderIntent
    data class UpdateMarginHorizontal(val margin: Int) : ReaderIntent
    data class UpdateMarginVertical(val margin: Int) : ReaderIntent
    data class UpdatePageMode(val mode: PageMode) : ReaderIntent
    data class UpdatePageAnimationType(val type: PageAnimationType) : ReaderIntent
    data object ToggleFirstLineIndent : ReaderIntent
    
    // 书签
    data class ShowBookmarkMenu(val paragraphIndex: Int) : ReaderIntent
    data object HideBookmarkMenu : ReaderIntent
    data class AddBookmark(val chapterIndex: Int, val paragraphIndex: Int, val note: String?) : ReaderIntent
    data class DeleteBookmark(val bookmarkId: Int) : ReaderIntent
    
    // 图片预览
    data class ShowImagePreview(val url: String, val alt: String?) : ReaderIntent
    data object HideImagePreview : ReaderIntent
    
    // 脚注
    data class ShowFootnote(val footnote: ContentElement.Footnote) : ReaderIntent
    data object HideFootnote : ReaderIntent
    
    // 退出
    data object Back : ReaderIntent
}

// ============= MVI Effect =============

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
    
    /**
     * 处理用户意图
     */
    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            // 初始化
            is ReaderIntent.LoadBook -> loadBook(intent.bookId, intent.startChapterIndex)
            is ReaderIntent.LoadChapter -> loadChapter(intent.chapterIndex)
            
            // 进度
            is ReaderIntent.UpdateScrollPosition -> updateScrollPosition(intent.paragraphIndex, intent.scrollOffset)
            is ReaderIntent.UpdatePagePosition -> updatePagePosition(intent.pageIndex)
            is ReaderIntent.SaveProgress -> saveProgress()
            
            // 进度冲突
            is ReaderIntent.UseLocalProgress -> useLocalProgress()
            is ReaderIntent.UseRemoteProgress -> useRemoteProgress()
            is ReaderIntent.DismissProgressConflict -> dismissProgressConflict()
            
            // 菜单
            is ReaderIntent.ToggleMenu -> toggleMenu()
            is ReaderIntent.ShowTocSheet -> showTocSheet()
            is ReaderIntent.HideTocSheet -> hideTocSheet()
            is ReaderIntent.ShowSettingsSheet -> showSettingsSheet()
            is ReaderIntent.HideSettingsSheet -> hideSettingsSheet()
            
            // 右上角菜单
            is ReaderIntent.ShowTopMenu -> showTopMenu()
            is ReaderIntent.HideTopMenu -> hideTopMenu()
            is ReaderIntent.ViewBookDetail -> viewBookDetail()
            
            // 目录排序
            is ReaderIntent.ToggleTocSortOrder -> toggleTocSortOrder()
            
            // 章节导航
            is ReaderIntent.NextChapter -> nextChapter()
            is ReaderIntent.PreviousChapter -> previousChapter()
            is ReaderIntent.JumpToChapter -> jumpToChapter(intent.index)
            is ReaderIntent.JumpToBookmark -> jumpToBookmark(intent.bookmark)
            is ReaderIntent.OnPagerChapterChanged -> onPagerChapterChanged(intent.newChapterIndex, intent.direction)
            
            // 设置
            is ReaderIntent.UpdateSettings -> updateSettings(intent.settings)
            is ReaderIntent.UpdateFontSize -> updateFontSize(intent.size)
            is ReaderIntent.UpdateLineHeight -> updateLineHeight(intent.height)
            is ReaderIntent.UpdateParagraphSpacing -> updateParagraphSpacing(intent.spacing)
            is ReaderIntent.UpdateMarginHorizontal -> updateMarginHorizontal(intent.margin)
            is ReaderIntent.UpdateMarginVertical -> updateMarginVertical(intent.margin)
            is ReaderIntent.UpdatePageMode -> updatePageMode(intent.mode)
            is ReaderIntent.UpdatePageAnimationType -> updatePageAnimationType(intent.type)
            is ReaderIntent.ToggleFirstLineIndent -> toggleFirstLineIndent()
            
            // 书签
            is ReaderIntent.ShowBookmarkMenu -> showBookmarkMenu(intent.paragraphIndex)
            is ReaderIntent.HideBookmarkMenu -> hideBookmarkMenu()
            is ReaderIntent.AddBookmark -> addBookmark(intent.chapterIndex, intent.paragraphIndex, intent.note)
            is ReaderIntent.DeleteBookmark -> deleteBookmark(intent.bookmarkId)
            
            // 图片预览
            is ReaderIntent.ShowImagePreview -> showImagePreview(intent.url, intent.alt)
            is ReaderIntent.HideImagePreview -> hideImagePreview()
            
            // 脚注
            is ReaderIntent.ShowFootnote -> showFootnote(intent.footnote)
            is ReaderIntent.HideFootnote -> hideFootnote()
            
            // 退出
            is ReaderIntent.Back -> back()
        }
    }
    
    // ============= 初始化 =============
    
    private fun loadBook(bookId: Int, startChapterIndex: Int?) {
        scope.launch {
            // 清理旧书籍的状态，避免使用旧缓存
            _state.update { 
                it.copy(
                    bookId = bookId, 
                    isLoading = true, 
                    error = null,
                    // 清理章节缓存和内容
                    currentChapter = null,
                    adjacentChapters = emptyMap(),
                    preloadedChapters = emptyMap(),
                    // 清理书签
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
                    // 如果失败，尝试获取不带进度的 manifest
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
                    // 有冲突，显示冲突对话框
                    _state.update { 
                        it.copy(
                            localProgress = localProgress,
                            remoteProgress = remoteProgress,
                            hasProgressConflict = true,
                            showProgressConflictDialog = true,
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
    
    private fun loadChapter(chapterIndex: Int) {
        scope.launch {
            _state.update { it.copy(isLoadingChapter = true) }
            
            try {
                val bookId = _state.value.bookId
                val totalChapters = _state.value.totalChapters
                
                // 确定需要加载的章节范围（当前章节 + 前后各一章）
                val chaptersToLoad = buildList {
                    if (chapterIndex > 0) add(chapterIndex - 1)
                    add(chapterIndex)
                    if (chapterIndex < totalChapters - 1) add(chapterIndex + 1)
                }
                
                // 加载所有需要的章节
                val loadedChapters = mutableMapOf<Int, ChapterContent>()
                for (index in chaptersToLoad) {
                    // 优先从预加载缓存获取
                    val cached = _state.value.preloadedChapters[index]
                    if (cached != null) {
                        loadedChapters[index] = cached
                    } else {
                        // 从服务器加载
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
                
                // 预加载更远的章节（可选，为下次切换做准备）
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
        val totalChapters = _state.value.totalChapters
        val bookId = _state.value.bookId
        
        // 预加载前后各 2 章（但排除已加载的相邻章节）
        listOf(currentIndex - 2, currentIndex + 2)
            .filter { it in 0 until totalChapters }
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
    
    private fun loadBookmarks(bookId: Int) {
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
    
    private fun updateScrollPosition(paragraphIndex: Int, scrollOffset: Int) {
        _state.update { 
            it.copy(
                currentParagraphIndex = paragraphIndex,
                scrollOffset = scrollOffset
            )
        }
        scheduleProgressSave()
    }
    
    private fun updatePagePosition(pageIndex: Int) {
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
        
        // 计算已读章节字数
        val previousChaptersWords = manifest.toc
            .filter { it.index < state.currentChapterIndex }
            .sumOf { it.wordCount }
        
        // 计算当前章节进度
        val totalElements = currentChapter.elements.size
        val currentChapterProgress = if (totalElements > 0) {
            state.currentParagraphIndex.toDouble() / totalElements
        } else {
            0.0
        }
        
        // 当前章节字数
        val currentChapterWords = manifest.toc
            .find { it.index == state.currentChapterIndex }
            ?.wordCount ?: 0
        
        // 估算已读字数
        val estimatedReadWords = previousChaptersWords + (currentChapterWords * currentChapterProgress)
        
        // 总字数
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
    
    private fun saveProgress() {
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
        
        // 章节索引不同且都有有效进度
        return local.chapterIndex != remote.currentPage && 
               local.progress > 0 && 
               remote.progress > 0
    }
    
    private fun useLocalProgress() {
        val state = _state.value
        val localProgress = state.localProgress ?: return
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                showProgressConflictDialog = false,
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
    
    private fun useRemoteProgress() {
        val state = _state.value
        val remoteProgress = state.remoteProgress ?: return
        
        val chapterIndex = remoteProgress.currentPage
        
        _state.update { 
            it.copy(
                hasProgressConflict = false,
                showProgressConflictDialog = false,
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
    
    private fun dismissProgressConflict() {
        _state.update { it.copy(showProgressConflictDialog = false) }
    }
    
    // ============= 菜单 =============
    
    private fun toggleMenu() {
        _state.update { state ->
            // 如果设置面板或目录面板显示中，则先关闭它们
            if (state.showSettingsSheet || state.showTocSheet) {
                state.copy(
                    showSettingsSheet = false,
                    showTocSheet = false
                )
            } else {
                state.copy(
                    showMenu = !state.showMenu,
                    showTopMenu = false
                )
            }
        }
    }
    
    private fun showTocSheet() {
        _state.update { 
            it.copy(
                showTocSheet = true,
                showMenu = false
            )
        }
    }
    
    private fun hideTocSheet() {
        _state.update { it.copy(showTocSheet = false) }
    }
    
    private fun showSettingsSheet() {
        _state.update { 
            it.copy(
                showSettingsSheet = true,
                showMenu = false
            )
        }
    }
    
    private fun hideSettingsSheet() {
        _state.update { it.copy(showSettingsSheet = false) }
    }
    
    private fun showTopMenu() {
        _state.update { it.copy(showTopMenu = true) }
    }
    
    private fun hideTopMenu() {
        _state.update { it.copy(showTopMenu = false) }
    }
    
    private fun viewBookDetail() {
        scope.launch {
            _state.update { it.copy(showTopMenu = false, showMenu = false) }
            _effect.emit(ReaderEffect.NavigateToBookDetail(_state.value.bookId))
        }
    }
    
    private fun toggleTocSortOrder() {
        _state.update { 
            it.copy(
                tocSortOrder = when (it.tocSortOrder) {
                    TocSortOrder.ASC -> TocSortOrder.DESC
                    TocSortOrder.DESC -> TocSortOrder.ASC
                }
            )
        }
    }
    
    // ============= 章节导航 =============
    
    private fun nextChapter() {
        val state = _state.value
        if (state.hasNextChapter) {
            jumpToChapter(state.currentChapterIndex + 1)
        }
    }
    
    private fun previousChapter() {
        val state = _state.value
        if (state.hasPreviousChapter) {
            jumpToChapter(state.currentChapterIndex - 1)
        }
    }
    
    private fun jumpToChapter(index: Int) {
        val state = _state.value
        if (index < 0 || index >= state.totalChapters) return
        
        _state.update { 
            it.copy(
                currentParagraphIndex = 0,
                scrollOffset = 0,
                currentPageIndex = 0,
                showTocSheet = false
            )
        }
        
        loadChapter(index)
        
        scope.launch {
            _effect.emit(ReaderEffect.ScrollToPosition(0, 0))
        }
    }
    
    private fun jumpToBookmark(bookmark: BookmarkResponse) {
        _state.update { 
            it.copy(
                currentParagraphIndex = bookmark.paragraphIndex ?: 0,
                showTocSheet = false
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
    private fun onPagerChapterChanged(newChapterIndex: Int, direction: Int) {
        val currentIndex = _state.value.currentChapterIndex
        if (newChapterIndex == currentIndex) return
        
        val totalChapters = _state.value.totalChapters
        if (newChapterIndex < 0 || newChapterIndex >= totalChapters) return
        
        // 更新当前章节索引和内容，同时保存滑动方向
        _state.update { state ->
            state.copy(
                currentChapterIndex = newChapterIndex,
                currentChapter = state.adjacentChapters[newChapterIndex],
                currentPageIndex = 0,  // 重置页码
                pagerSlideDirection = direction  // 保存滑动方向
            )
        }
        
        // 更新本地进度
        updateLocalProgress()
        
        // 异步加载新的相邻章节
        loadAdjacentChaptersForPager(newChapterIndex)
    }
    
    /**
     * 为 Pager 加载新的相邻章节
     * 当用户滑动到新章节后，需要加载新的前后章节
     */
    private fun loadAdjacentChaptersForPager(centerIndex: Int) {
        scope.launch {
            val bookId = _state.value.bookId
            val totalChapters = _state.value.totalChapters
            val currentAdjacent = _state.value.adjacentChapters
            
            // 确定需要加载的章节范围
            val neededIndices = buildList {
                if (centerIndex > 0) add(centerIndex - 1)
                add(centerIndex)
                if (centerIndex < totalChapters - 1) add(centerIndex + 1)
            }
            
            // 找出缺失的章节
            val missingIndices = neededIndices.filter { !currentAdjacent.containsKey(it) }
            
            if (missingIndices.isEmpty()) {
                // 所有章节已加载，只需要清理不需要的章节
                val validRange = neededIndices.toSet()
                _state.update { state ->
                    state.copy(
                        adjacentChapters = state.adjacentChapters.filterKeys { it in validRange }
                    )
                }
                return@launch
            }
            
            // 加载缺失的章节
            val newChapters = mutableMapOf<Int, ChapterContent>()
            for (index in missingIndices) {
                // 先检查预加载缓存
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
            
            // 合并并清理（只保留需要的章节）
            val validRange = neededIndices.toSet()
            _state.update { state ->
                state.copy(
                    adjacentChapters = (state.adjacentChapters + newChapters).filterKeys { it in validRange },
                    preloadedChapters = state.preloadedChapters + newChapters
                )
            }
            
            // 预加载更远的章节
            preloadFurtherChapters(centerIndex)
        }
    }
    
    // ============= 设置 =============
    
    private fun updateSettings(settings: ReaderSettings) {
        _state.update { it.copy(readerSettings = settings) }
        syncSettings(settings)
    }
    
    private fun updateFontSize(size: Int) {
        val newSettings = _state.value.readerSettings.copy(fontSize = size)
        updateSettings(newSettings)
    }
    
    private fun updateLineHeight(height: Double) {
        val newSettings = _state.value.readerSettings.copy(lineHeight = height)
        updateSettings(newSettings)
    }
    
    private fun updateParagraphSpacing(spacing: Int) {
        val newSettings = _state.value.readerSettings.copy(paragraphSpacing = spacing)
        updateSettings(newSettings)
    }
    
    private fun updateMarginHorizontal(margin: Int) {
        val newSettings = _state.value.readerSettings.copy(marginHorizontal = margin)
        updateSettings(newSettings)
    }
    
    private fun updateMarginVertical(margin: Int) {
        val newSettings = _state.value.readerSettings.copy(marginVertical = margin)
        updateSettings(newSettings)
    }
    
    private fun updatePageMode(mode: PageMode) {
        val newSettings = _state.value.readerSettings.copy(pageMode = mode)
        updateSettings(newSettings)
    }
    
    private fun updatePageAnimationType(type: PageAnimationType) {
        val newSettings = _state.value.readerSettings.copy(pageAnimationType = type)
        updateSettings(newSettings)
    }
    
    private fun toggleFirstLineIndent() {
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
    
    private fun showBookmarkMenu(paragraphIndex: Int) {
        _state.update { 
            it.copy(
                showBookmarkMenu = true,
                bookmarkMenuParagraphIndex = paragraphIndex
            )
        }
    }
    
    private fun hideBookmarkMenu() {
        _state.update { 
            it.copy(
                showBookmarkMenu = false,
                bookmarkMenuParagraphIndex = null
            )
        }
    }
    
    private fun addBookmark(chapterIndex: Int, paragraphIndex: Int, note: String?) {
        scope.launch {
            _state.update { it.copy(isAddingBookmark = true, showBookmarkMenu = false) }
            
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
    
    private fun deleteBookmark(bookmarkId: Int) {
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
    
    // ============= 图片预览 =============
    
    private fun showImagePreview(url: String, alt: String?) {
        _state.update { 
            it.copy(
                showImagePreview = true,
                previewImageUrl = url,
                previewImageAlt = alt
            )
        }
    }
    
    private fun hideImagePreview() {
        _state.update { 
            it.copy(
                showImagePreview = false,
                previewImageUrl = null,
                previewImageAlt = null
            )
        }
    }
    
    // ============= 脚注 =============
    
    private fun showFootnote(footnote: ContentElement.Footnote) {
        _state.update { 
            it.copy(
                showFootnoteDialog = true,
                currentFootnote = footnote
            )
        }
    }
    
    private fun hideFootnote() {
        _state.update { 
            it.copy(
                showFootnoteDialog = false,
                currentFootnote = null
            )
        }
    }
    
    // ============= 退出 =============
    
    private fun back() {
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
    
    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
        autoSyncJob?.cancel()
    }
}
