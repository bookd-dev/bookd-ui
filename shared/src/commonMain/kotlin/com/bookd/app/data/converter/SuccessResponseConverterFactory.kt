package com.bookd.app.data.converter

import com.bookd.app.data.api.NetworkException
import com.bookd.app.data.api.NotAuthenticatedException
import com.bookd.app.data.api.TokenExpiredException
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 * Ktorfit 响应转换器工厂
 *
 * 自动将后端的 SuccessResponse<T> 格式解包为 T
 *
 * ## 工作原理
 *
 * 后端返回:
 * ```json
 * {
 *   "data": { "token": "xxx", "user": {...} },
 *   "timestamp": "2026-01-20T10:30:00",
 *   "path": "/api/auth/login"
 * }
 * ```
 *
 * API 接口声明:
 * ```kotlin
 * suspend fun login(@Body request: LoginRequest): LoginResponse
 * ```
 *
 * 转换器自动提取 `data` 字段，返回 `LoginResponse` 对象
 *
 * ## 错误处理
 *
 * 当 HTTP 状态码为 4xx/5xx 时，后端返回 ErrorResponse 格式：
 * ```json
 * {
 *   "code": "AUTH_001",
 *   "message": "用户名或密码错误",
 *   "timestamp": "...",
 *   "path": "..."
 * }
 * ```
 *
 * 转换器会解析错误码并抛出对应异常：
 * - AUTH_002 -> NotAuthenticatedException
 * - AUTH_003 -> TokenExpiredException
 * - 其他 -> NetworkException（携带后端的国际化消息）
 *
 * ## 特殊情况
 *
 * - **Unit 类型**: 跳过转换（如 logout()）
 */
class SuccessResponseConverterFactory : Converter.Factory {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {

        // 跳过 Unit 类型（如 logout() 无返回值的操作）
        // Ktor 默认处理器会忽略响应体
        if (typeData.typeInfo.type == Unit::class) {
            return null
        }

        // 对所有其他类型，解包 SuccessResponse
        return SuccessResponseConverter(typeData, json)
    }

    /**
     * 响应转换器实现
     *
     * 1. 检查 HTTP 状态码，如果是错误则解析 ErrorResponse 并抛出异常
     * 2. 如果是成功响应，解析 SuccessResponse 并提取 data 字段
     */
    private class SuccessResponseConverter(
        private val typeData: TypeData,
        private val json: Json
    ) : Converter.SuspendResponseConverter<HttpResponse, Any> {

        override suspend fun convert(result: KtorfitResult): Any {
            return when (result) {
                is KtorfitResult.Failure -> {
                    throw result.throwable
                }

                is KtorfitResult.Success -> {
                    val response = result.response
                    val responseText = response.bodyAsText()
                    val jsonObject = json.parseToJsonElement(responseText).jsonObject
                    
                    // 检查是否为错误响应（有 code 字段但没有 data 字段）
                    val codeElement = jsonObject["code"]
                    val dataElement = jsonObject["data"]
                    
                    if (codeElement != null && dataElement == null) {
                        // 这是 ErrorResponse，解析并抛出对应异常
                        val errorCode = codeElement.jsonPrimitive.content
                        val errorMessage = jsonObject["message"]?.jsonPrimitive?.content ?: "Unknown error"
                        
                        throw when (errorCode) {
                            "AUTH_002" -> NotAuthenticatedException()
                            "AUTH_003" -> TokenExpiredException()
                            else -> NetworkException(message = errorMessage)
                        }
                    }
                    
                    // 这是 SuccessResponse，提取 data 字段
                    if (dataElement == null) {
                        throw IllegalStateException("Response missing 'data' field: $responseText")
                    }
                    
                    // 将 data 字段序列化回 JSON 字符串
                    val dataJson = dataElement.toString()
                    
                    // 使用目标类型的 KType 获取序列化器并反序列化
                    val kType = typeData.typeInfo.kotlinType
                        ?: throw IllegalStateException("Cannot get KType for ${typeData.typeInfo.type}")
                    
                    json.decodeFromString(serializer(kType), dataJson)
                        ?: throw IllegalStateException("Failed to deserialize data: $dataJson")
                }
            }
        }
    }
}
