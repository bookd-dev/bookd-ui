package com.bookd.app.basic.lifecycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlin.collections.get

abstract class BaseViewModel(
    handler: CoroutineExceptionHandler? = null
) : ViewModel() {

    constructor() : this(handler = GlobalExceptionHandler)

    /**
     * 带异常处理的协程作用域，用于处理协程中的异常
     *
     * 如果构建器中传入了异常处理器，则使用传入的异常处理器
     * 否则使用 viewModelScope
     */
    protected val scope: CoroutineScope = if (handler == null) {
        viewModelScope
    } else {
        viewModelScope.plus(handler)
    }
}