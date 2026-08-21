package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 后端统一成功响应包装
 *
 * 所有成功的 API 响应都被包装为此格式：
 * ```json
 * {
 *   "data": { ... },
 *   "timestamp": "2026-01-20T10:30:00",
 *   "path": "/api/books/1"
 * }
 * ```
 *
 * 前端使用 SuccessResponseConverterFactory 自动解包，
 * API 接口可以直接声明返回 T 类型而非 SuccessResponse<T>
 */
@Serializable
data class SuccessResponse<T>(
    val data: T,
    val timestamp: String,
    val path: String
)

/**
 * 后端统一错误响应
 *
 * HTTP 4xx/5xx 错误返回此格式：
 * ```json
 * {
 *   "code": "AUTH_001",
 *   "message": "用户名或密码错误",
 *   "details": null,
 *   "timestamp": "2026-01-20T10:30:00",
 *   "path": "/api/auth/login"
 * }
 * ```
 *
 * 前端使用 HttpCallValidator 拦截并解析，
 * 根据 code 映射到对应的异常类型
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: String? = null,
    val timestamp: String,
    val path: String
)

/**
 * 操作消息响应
 *
 * 用于无具体数据返回的成功操作（如登出、删除等）：
 * ```json
 * {
 *   "message": "已登出",
 *   "success": true
 * }
 * ```
 */
@Serializable
data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

/**
 * 健康检查响应
 */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)
