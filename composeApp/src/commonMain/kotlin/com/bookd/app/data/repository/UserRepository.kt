@file:OptIn(ExperimentalSettingsApi::class, ExperimentalSerializationApi::class)

package com.bookd.app.data.repository

import com.bookd.app.data.model.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private var _token: String? = null
    val token: String? get() = _token
    
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
                _token = savedToken
                _authState.value = AuthState.Authenticated(cachedUser)
                
                // 后台验证 token 并刷新用户信息
                val freshUser = validateToken(savedToken)
                if (freshUser != null) {
                    saveAuthData(savedToken, freshUser)
                    _authState.value = AuthState.Authenticated(freshUser)
                } else {
                    // Token 失效，清除缓存
                    clearAuthData()
                    _token = null
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
            val response = httpClient.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
            
            if (response.status == HttpStatusCode.OK) {
                val loginResponse: LoginResponse = response.body()
                _token = loginResponse.token
                saveAuthData(loginResponse.token, loginResponse.user)
                _authState.value = AuthState.Authenticated(loginResponse.user)
                Result.success(loginResponse.user)
            } else {
                Result.failure(Exception("用户名或密码错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 登出
     */
    suspend fun logout() {
        try {
            _token?.let { token ->
                httpClient.post("$baseUrl/api/auth/logout") {
                    header("Authorization", "Bearer $token")
                }
            }
        } catch (_: Exception) {
            // 忽略登出请求的错误
        } finally {
            _token = null
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
            val response = httpClient.post("$baseUrl/api/auth/register/guest") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, password, email))
            }
            
            if (response.status == HttpStatusCode.Created) {
                val user: User = response.body()
                Result.success(user)
            } else {
                Result.failure(Exception("注册失败"))
            }
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
            val response = httpClient.post("$baseUrl/api/auth/register/user") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, password, email, inviteToken))
            }
            
            if (response.status == HttpStatusCode.Created) {
                val user: User = response.body()
                Result.success(user)
            } else {
                Result.failure(Exception("邀请码无效或用户名已存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 刷新当前用户信息
     */
    suspend fun refreshCurrentUser(): Result<User> {
        val currentToken = _token ?: return Result.failure(Exception("未登录"))
        
        return try {
            val response = httpClient.get("$baseUrl/api/auth/me") {
                header("Authorization", "Bearer $currentToken")
            }
            
            if (response.status == HttpStatusCode.OK) {
                val user: User = response.body()
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                logout()
                Result.failure(Exception("Token 无效"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 验证 token 是否有效
     */
    private suspend fun validateToken(token: String): User? {
        return try {
            val response = httpClient.get("$baseUrl/api/auth/me") {
                header("Authorization", "Bearer $token")
            }
            
            if (response.status == HttpStatusCode.OK) {
                response.body()
            } else {
                null
            }
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
