@file:OptIn(ExperimentalSettingsApi::class, ExperimentalSerializationApi::class)

package com.bookd.app.data.repository

import com.bookd.app.data.api.AuthApi
import com.bookd.app.data.auth.DefaultHeaderProvider
import com.bookd.app.data.model.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
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
    private val settings: Settings,
    private val authApi: AuthApi,
    private val headerProvider: DefaultHeaderProvider,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    val token: String? get() = headerProvider.token
    
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
            val authData = loadAuthData()
            if (authData != null) {
                val (savedToken, cachedUser) = authData
                // 先用缓存的用户信息快速恢复状态
                headerProvider.updateToken(savedToken)
                _authState.value = AuthState.Authenticated(cachedUser)
                
                // 后台验证 token 并刷新用户信息
                val freshUser = validateToken()
                if (freshUser != null) {
                    saveAuthData(savedToken, freshUser)
                    _authState.value = AuthState.Authenticated(freshUser)
                } else {
                    // Token 失效，清除缓存
                    clearAuthData()
                    headerProvider.clearToken()
                    _authState.value = AuthState.NotAuthenticated
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
            val loginResponse = authApi.login(LoginRequest(username, password))
            headerProvider.updateToken(loginResponse.token)
            saveAuthData(loginResponse.token, loginResponse.user)
            _authState.value = AuthState.Authenticated(loginResponse.user)
            Result.success(loginResponse.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 登出
     */
    suspend fun logout() {
        try {
            if (headerProvider.token != null) {
                authApi.logout()
            }
        } catch (_: Exception) {
            // 忽略登出请求的错误
        } finally {
            headerProvider.clearToken()
            clearAuthData()
            _authState.value = AuthState.NotAuthenticated
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
        if (headerProvider.token == null) {
            return Result.failure(Exception("未登录"))
        }
        
        return try {
            val user = authApi.getCurrentUser()
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            logout()
            Result.failure(e)
        }
    }
    
    /**
     * 验证 token 是否有效
     */
    private suspend fun validateToken(): User? {
        return try {
            authApi.getCurrentUser()
        } catch (_: Exception) {
            null
        }
    }
    
    private fun saveAuthData(token: String, user: User) {
        settings.putString(KEY_AUTH_TOKEN, token)
        settings.encodeValue(User.serializer(), KEY_USER, user)
    }
    
    private fun loadAuthData(): Pair<String, User>? {
        val token = settings.getStringOrNull(KEY_AUTH_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        val user = settings.decodeValueOrNull(User.serializer(), KEY_USER) ?: return null
        return token to user
    }
    
    private fun clearAuthData() {
        settings.remove(KEY_AUTH_TOKEN)
        settings.remove(KEY_USER)
    }
    
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER = "cached_user"
    }
}
