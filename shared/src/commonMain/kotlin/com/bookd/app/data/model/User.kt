package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 用户角色枚举
 */
enum class UserRole(val value: String) {
    ADMIN("admin"),
    USER("user"),
    GUEST("guest");

    companion object {
        fun fromString(value: String): UserRole = entries.find { it.value == value } ?: GUEST
    }
}

/**
 * 用户信息
 */
@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String?,
    val role: String
) {
    val userRole: UserRole get() = UserRole.fromString(role)
}

/**
 * 登录请求
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 登录响应
 */
@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

/**
 * 注册请求
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val inviteToken: String? = null
)
