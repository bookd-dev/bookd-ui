@file:OptIn(ExperimentalSettingsApi::class, ExperimentalSerializationApi::class)

package com.bookd.app.data.repository

import com.bookd.app.data.api.AuthApi
import com.bookd.app.data.auth.TokenProvider
import com.bookd.app.data.model.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.nullableString
import com.russhwolf.settings.serialization.nullableSerializedValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * 用户认证状态
 */
sealed class AuthState {
    /** 未初始化，正在检查登录状态 */
    data object Loading : AuthState()
    
    /** 未登录 */
    data object NotAuthenticated : AuthState()
    
    /** 已登录 */
    data class Authenticated(val user: User) : AuthState()
}

/**
 * 全局用户管理器
 * 
 * 负责管理用户的认证状态、登录、登出、token 存储等
 */
class UserRepository(
    settings: Settings,
    private val authApi: AuthApi,
) : TokenProvider {
    
    // Settings 代理，自动持久化
    override var token: String? by settings.nullableString(KEY_AUTH_TOKEN)
    private var cachedUser: User? by settings.nullableSerializedValue(User.serializer(), KEY_USER)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    val currentUser: User?
        get() = (_authState.value as? AuthState.Authenticated)?.user
    
    val isLoggedIn: Boolean
        get() = _authState.value is AuthState.Authenticated
    
    /**
     * 初始化 - 尝试恢复登录状态
     */
    suspend fun initialize() {
        _authState.value = AuthState.Loading
        
        try {
            val savedToken = token
            val savedUser = cachedUser
            
            if (savedToken != null && savedUser != null) {
                // 先用缓存的用户信息快速恢复状态
                _authState.value = AuthState.Authenticated(savedUser)
                
                // 后台验证 token 并刷新用户信息
                val freshUser = validateToken()
                if (freshUser != null) {
                    cachedUser = freshUser
                    _authState.value = AuthState.Authenticated(freshUser)
                } else {
                    // Token 失效，清除缓存
                    clearAuth()
                }
                return
            }
            _authState.value = AuthState.NotAuthenticated
        } catch (_: Exception) {
            _authState.value = AuthState.NotAuthenticated
        }
    }
    
    /**
     * 登录
     */
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            token = response.token
            cachedUser = response.user
            _authState.value = AuthState.Authenticated(response.user)
            Result.success(response.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 登出
     */
    suspend fun logout() {
        try {
            if (token != null) {
                authApi.logout()
            }
        } catch (_: Exception) {
            // 忽略登出请求的错误
        } finally {
            clearAuth()
        }
    }
    
    /**
     * 访客注册
     */
    suspend fun registerGuest(
        username: String,
        password: String,
        email: String? = null
    ): Result<User> {
        return try {
            val user = authApi.registerGuest(RegisterRequest(username, password, email))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 邀请码注册
     */
    suspend fun registerWithInvite(
        username: String,
        password: String,
        inviteToken: String,
        email: String? = null
    ): Result<User> {
        return try {
            val user = authApi.registerWithInvite(RegisterRequest(username, password, email, inviteToken))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 刷新当前用户信息
     */
    suspend fun refreshCurrentUser(): Result<User> {
        if (token == null) {
            return Result.failure(Exception("Not authenticated"))
        }
        
        return try {
            val user = authApi.getCurrentUser()
            cachedUser = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            logout()
            Result.failure(e)
        }
    }
    
    private suspend fun validateToken(): User? {
        return try {
            authApi.getCurrentUser()
        } catch (_: Exception) {
            null
        }
    }
    
    private fun clearAuth() {
        token = null
        cachedUser = null
        _authState.value = AuthState.NotAuthenticated
    }
    
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER = "cached_user"
    }
}
