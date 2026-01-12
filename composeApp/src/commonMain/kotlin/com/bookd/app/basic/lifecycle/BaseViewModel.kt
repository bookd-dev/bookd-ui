package com.bookd.app.basic.lifecycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel private constructor(
    private val handler: AppExceptionHandler?,
    private val shouldCatchException: Boolean,
) : ViewModel(), AppExceptionHandler.Handler {

    constructor() : this(handler = GlobalExceptionHandler, shouldCatchException = false)

    constructor(shouldCatchException: Boolean) : this(handler = GlobalExceptionHandler, shouldCatchException = shouldCatchException)

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

    init {
        if (shouldCatchException) {
            handler?.registerHandler(this)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (shouldCatchException) {
            handler?.unregisterHandler(this)
        }
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {

    }
}