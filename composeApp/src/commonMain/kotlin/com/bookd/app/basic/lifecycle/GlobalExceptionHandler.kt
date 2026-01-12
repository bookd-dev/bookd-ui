package com.bookd.app.basic.lifecycle

import com.bookd.app.basic.lifecycle.GlobalExceptionHandler.listeners
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


abstract class AppExceptionHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {

    abstract fun registerHandler(handler: Handler)

    abstract fun unregisterHandler(handler: Handler)

    fun interface Handler {
        fun handleException(context: CoroutineContext, exception: Throwable)
    }
}

/**
 * 全局协程异常处理器
 */
object GlobalExceptionHandler : AppExceptionHandler() {

    private val listeners: MutableList<Handler> = mutableListOf()

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        listeners.forEach { it.handleException(context, exception) }
    }

    override fun registerHandler(handler: Handler) {
        if (!listeners.contains(handler)) {
            listeners.add(handler)
        }
    }

    override fun unregisterHandler(handler: Handler) {
        listeners.remove(handler)
    }
}