package com.bookd.app.data.vm

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.bookd.app.basic.lifecycle.BaseViewModel

class BookSourceViewModel : BaseViewModel() {

    // 当前选中的 page
    var currentPage by mutableIntStateOf(0)

    // 每个 page 独立的滚动状态（懒加载）
    private val _scrollStates = mutableStateMapOf<Int, LazyListState>()

    /**
     * 获取或创建指定 page 的滚动状态
     * 按需创建，避免提前初始化所有页面的状态
     */
    fun getScrollState(page: Int): LazyListState {
        return _scrollStates.getOrPut(page) { LazyListState() }
    }

    // Tab 行折叠偏移量
    var tabRowOffset by mutableFloatStateOf(0f)
}
