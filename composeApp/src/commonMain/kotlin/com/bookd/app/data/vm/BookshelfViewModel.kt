package com.bookd.app.data.vm

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.bookd.app.basic.lifecycle.BaseViewModel

class BookshelfViewModel : BaseViewModel() {
    
    // 当前选中的 page
    var currentPage by mutableIntStateOf(0)
    
    // 每个 page 独立的滚动状态
    val scrollStates: List<LazyListState> = List(30) { LazyListState() }
    
    // Tab 行折叠偏移量
    var tabRowOffset by mutableFloatStateOf(0f)
}