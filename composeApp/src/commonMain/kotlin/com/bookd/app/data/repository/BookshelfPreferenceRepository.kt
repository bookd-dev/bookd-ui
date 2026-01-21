package com.bookd.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.int
import com.russhwolf.settings.string

/**
 * 书架偏好设置仓库
 * 
 * 管理书架相关的用户偏好设置
 */
class BookshelfPreferenceRepository(settings: Settings) {
    
    /**
     * 书架显示模式
     * - "list": 列表模式
     * - "grid": 瀑布流模式
     */
    var viewMode: String by settings.string(KEY_VIEW_MODE, VIEW_MODE_LIST)
    
    /**
     * 上次选中的书架 ID
     * -1 表示未设置，会选择第一个书架
     */
    var lastSelectedBookshelfId: Int by settings.int(KEY_LAST_SELECTED_BOOKSHELF_ID, -1)
    
    /**
     * 是否为列表模式
     */
    val isListMode: Boolean
        get() = viewMode == VIEW_MODE_LIST
    
    /**
     * 是否为瀑布流模式
     */
    val isGridMode: Boolean
        get() = viewMode == VIEW_MODE_GRID
    
    /**
     * 切换显示模式
     */
    fun toggleViewMode() {
        viewMode = if (isListMode) VIEW_MODE_GRID else VIEW_MODE_LIST
    }
    
    companion object {
        const val KEY_VIEW_MODE = "bookshelf_view_mode"
        const val KEY_LAST_SELECTED_BOOKSHELF_ID = "bookshelf_last_selected_id"
        
        const val VIEW_MODE_LIST = "list"
        const val VIEW_MODE_GRID = "grid"
    }
}
