package com.bookd.app.data.vm

import androidx.compose.runtime.Immutable
import app.composeapp.generated.resources.*
import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

// ============= MVI State =============

/**
 * 登录/注册页面状态
 */
@Immutable
data class SignInState(
    // 当前模式
    val mode: SignInMode = SignInMode.LOGIN,
    
    // 注册方式 (仅在注册模式下生效)
    val registerType: RegisterType = RegisterType.GUEST,
    
    // 表单字段
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val inviteToken: String = "",
    
    // 字段验证错误 (使用 StringResource)
    val usernameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val emailError: StringResource? = null,
    val inviteTokenError: StringResource? = null,
    
    // UI 状态
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
) {
    /**
     * 表单是否有效
     */
    val isFormValid: Boolean
        get() = when (mode) {
            SignInMode.LOGIN -> {
                username.isNotBlank() && 
                password.isNotBlank() &&
                usernameError == null && 
                passwordError == null
            }
            SignInMode.REGISTER -> {
                when (registerType) {
                    RegisterType.GUEST -> {
                        username.isNotBlank() && 
                        password.isNotBlank() &&
                        usernameError == null && 
                        passwordError == null &&
                        emailError == null
                    }
                    RegisterType.INVITE -> {
                        username.isNotBlank() && 
                        password.isNotBlank() &&
                        inviteToken.isNotBlank() &&
                        usernameError == null && 
                        passwordError == null &&
                        emailError == null &&
                        inviteTokenError == null
                    }
                }
            }
        }
}

/**
 * 登录/注册模式
 */
enum class SignInMode {
    LOGIN,      // 登录
    REGISTER    // 注册
}

/**
 * SignInMode 扩展属性：关联字符串资源
 */
val SignInMode.titleRes: StringResource
    get() = when (this) {
        SignInMode.LOGIN -> Res.string.sign_in
        SignInMode.REGISTER -> Res.string.register
    }

/**
 * 注册方式
 */
enum class RegisterType {
    GUEST,      // 访客注册
    INVITE      // 邀请码注册
}

/**
 * RegisterType 扩展属性：关联字符串资源
 */
val RegisterType.labelRes: StringResource
    get() = when (this) {
        RegisterType.GUEST -> Res.string.guest_register
        RegisterType.INVITE -> Res.string.invite_register
    }

// ============= MVI Intent =============

/**
 * 用户意图 (用户操作)
 */
sealed interface SignInIntent {
    /** 切换登录/注册模式 */
    data class SwitchMode(val mode: SignInMode) : SignInIntent
    
    /** 切换注册方式 */
    data class SwitchRegisterType(val type: RegisterType) : SignInIntent
    
    /** 输入用户名 */
    data class InputUsername(val value: String) : SignInIntent
    
    /** 输入密码 */
    data class InputPassword(val value: String) : SignInIntent
    
    /** 输入邮箱 */
    data class InputEmail(val value: String) : SignInIntent
    
    /** 输入邀请码 */
    data class InputInviteToken(val value: String) : SignInIntent
    
    /** 切换密码可见性 */
    data object TogglePasswordVisibility : SignInIntent
    
    /** 提交表单 (登录或注册) */
    data object Submit : SignInIntent
    
    /** 清除错误 */
    data object ClearError : SignInIntent
}

// ============= MVI Effect =============

/**
 * 副作用 (一次性事件)
 * 
 * 仅包含导航事件，错误处理通过 error StateFlow 传递给 SnackbarHostScaffold
 */
sealed interface SignInEffect {
    /** 登录成功，导航到主页 */
    data object LoginSuccess : SignInEffect
    
    /** 注册成功，自动登录 */
    data object RegisterSuccess : SignInEffect
}

// ============= ViewModel =============

class SignInViewModel(
    private val userRepository: UserRepository
) : BaseViewModel() {
    
    // State
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()
    
    // Effect (一次性事件，仅用于导航)
    private val _effect = Channel<SignInEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // Error (统一错误处理，传递给 SnackbarHostScaffold)
    private val _error = MutableStateFlow<Result<Any>?>(null)
    val error = _error.asStateFlow()
    
    /**
     * 处理用户意图
     */
    fun handleIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.SwitchMode -> switchMode(intent.mode)
            is SignInIntent.SwitchRegisterType -> switchRegisterType(intent.type)
            is SignInIntent.InputUsername -> inputUsername(intent.value)
            is SignInIntent.InputPassword -> inputPassword(intent.value)
            is SignInIntent.InputEmail -> inputEmail(intent.value)
            is SignInIntent.InputInviteToken -> inputInviteToken(intent.value)
            is SignInIntent.TogglePasswordVisibility -> togglePasswordVisibility()
            is SignInIntent.Submit -> submit()
            is SignInIntent.ClearError -> clearError()
        }
    }
    
    // ============= Intent 处理器 =============
    
    private fun switchMode(mode: SignInMode) {
        _state.update { 
            it.copy(
                mode = mode,
                // 切换模式时清除所有验证错误
                usernameError = null,
                passwordError = null,
                emailError = null,
                inviteTokenError = null
            )
        }
    }
    
    private fun switchRegisterType(type: RegisterType) {
        _state.update { 
            it.copy(
                registerType = type,
                // 切换注册方式时清除邀请码相关错误
                inviteTokenError = null
            )
        }
    }
    
    private fun inputUsername(value: String) {
        _state.update { 
            it.copy(
                username = value,
                usernameError = validateUsername(value)
            )
        }
    }
    
    private fun inputPassword(value: String) {
        _state.update { 
            it.copy(
                password = value,
                passwordError = validatePassword(value)
            )
        }
    }
    
    private fun inputEmail(value: String) {
        _state.update { 
            it.copy(
                email = value,
                emailError = validateEmail(value)
            )
        }
    }
    
    private fun inputInviteToken(value: String) {
        _state.update { 
            it.copy(
                inviteToken = value,
                inviteTokenError = validateInviteToken(value)
            )
        }
    }
    
    private fun togglePasswordVisibility() {
        _state.update { 
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }
    
    private fun submit() {
        val currentState = _state.value
        
        // 最终验证
        if (!currentState.isFormValid) {
            return
        }
        
        when (currentState.mode) {
            SignInMode.LOGIN -> performLogin()
            SignInMode.REGISTER -> performRegister()
        }
    }
    
    private fun clearError() {
        _state.update { 
            it.copy(
                usernameError = null,
                passwordError = null,
                emailError = null,
                inviteTokenError = null
            )
        }
    }
    
    // ============= 业务逻辑 =============
    
    private fun performLogin() {
        val currentState = _state.value
        
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = userRepository.login(
                username = currentState.username,
                password = currentState.password
            )
            
            _state.update { it.copy(isLoading = false) }
            
            result.fold(
                onSuccess = {
                    _effect.send(SignInEffect.LoginSuccess)
                },
                onFailure = { throwable ->
                    // 错误通过 _error 传递给 SnackbarHostScaffold 统一处理
                    _error.value = Result.failure(throwable)
                }
            )
        }
    }
    
    private fun performRegister() {
        val currentState = _state.value
        
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = when (currentState.registerType) {
                RegisterType.GUEST -> {
                    userRepository.registerGuest(
                        username = currentState.username,
                        password = currentState.password,
                        email = currentState.email.takeIf { it.isNotBlank() }
                    )
                }
                RegisterType.INVITE -> {
                    userRepository.registerWithInvite(
                        username = currentState.username,
                        password = currentState.password,
                        inviteToken = currentState.inviteToken,
                        email = currentState.email.takeIf { it.isNotBlank() }
                    )
                }
            }
            
            result.fold(
                onSuccess = {
                    // 注册成功后自动登录
                    val loginResult = userRepository.login(
                        username = currentState.username,
                        password = currentState.password
                    )
                    
                    _state.update { it.copy(isLoading = false) }
                    
                    loginResult.fold(
                        onSuccess = {
                            _effect.send(SignInEffect.RegisterSuccess)
                        },
                        onFailure = { throwable ->
                            // 注册成功但登录失败
                            _error.value = Result.failure(throwable)
                        }
                    )
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false) }
                    // 错误通过 _error 传递给 SnackbarHostScaffold 统一处理
                    _error.value = Result.failure(throwable)
                }
            )
        }
    }
    
    // ============= 验证逻辑 =============
    
    /**
     * 验证用户名
     * @return StringResource 如果验证失败，否则返回 null
     */
    private fun validateUsername(username: String): StringResource? {
        return when {
            username.isBlank() -> null  // 空值不提示 (用户还在输入)
            username.length < 3 -> Res.string.error_username_too_short
            username.length > 20 -> Res.string.error_username_too_long
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> Res.string.error_username_invalid_chars
            else -> null
        }
    }
    
    /**
     * 验证密码
     */
    private fun validatePassword(password: String): StringResource? {
        return when {
            password.isBlank() -> null
            password.length < 6 -> Res.string.error_password_too_short
            password.length > 32 -> Res.string.error_password_too_long
            else -> null
        }
    }
    
    /**
     * 验证邮箱
     */
    private fun validateEmail(email: String): StringResource? {
        if (email.isBlank()) return null  // 邮箱是可选的
        
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return if (!email.matches(emailRegex)) {
            Res.string.error_email_invalid
        } else {
            null
        }
    }
    
    /**
     * 验证邀请码
     */
    private fun validateInviteToken(token: String): StringResource? {
        return when {
            token.isBlank() -> null
            token.length < 6 -> Res.string.error_invite_code_invalid
            else -> null
        }
    }
}
