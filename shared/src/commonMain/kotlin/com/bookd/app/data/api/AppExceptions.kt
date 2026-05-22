package com.bookd.app.data.api

/**
 * 应用异常类型
 * UI 层根据异常类型显示对应的国际化消息
 */

/**
 * 未认证异常（未登录）
 */
class NotAuthenticatedException : Exception()

/**
 * Token 失效异常
 */
class TokenExpiredException : Exception()

/**
 * 网络请求失败异常
 * @param message 可选的错误消息（通常来自后端的国际化消息）
 * @param cause 原始异常
 */
class NetworkException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
