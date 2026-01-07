package com.bookd.app.basic.lifecycle

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 全局协程异常处理器
 */
object GlobalExceptionHandler
    : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {


    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            else -> {

            }
        }
    }
}