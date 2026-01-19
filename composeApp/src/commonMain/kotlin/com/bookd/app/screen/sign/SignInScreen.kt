package com.bookd.app.screen.sign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.*
import com.bookd.app.data.vm.*
import com.bookd.app.screen.LocalSnackbarHostState
import com.bookd.app.screen.RouteMain.Companion.RouteBookshelf
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignInScreen() {
    val screenContext = rememberScreenContext<SignInViewModel>()
    val state by screenContext.viewModel.state.collectAsState()
    
    // 提前转换字符串资源用于 LaunchedEffect
    val loginSuccessMessage = stringResource(Res.string.login_success)
    val registerSuccessMessage = stringResource(Res.string.register_success)
    val snackbarHostState = LocalSnackbarHostState.current
    
    // 处理副作用 (Effect) - 仅处理导航
    // 错误由 GlobalExceptionHandler -> AppViewModel -> SnackbarHostScaffold 统一处理
    LaunchedEffect(Unit) {
        screenContext.viewModel.effect.collect { effect ->
            when (effect) {
                is SignInEffect.LoginSuccess -> {
                    snackbarHostState.showSnackbar(loginSuccessMessage)
                    screenContext.navigator.navigateTo(RouteBookshelf)
                }
                is SignInEffect.RegisterSuccess -> {
                    snackbarHostState.showSnackbar(registerSuccessMessage)
                    screenContext.navigator.navigateTo(RouteBookshelf)
                }
            }
        }
    }
    
    SignInContent(
        state = state,
        onIntent = screenContext.viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInContent(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // 应用标题
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Tab 切换 (登录/注册)
        PrimaryTabRow(
            selectedTabIndex = if (state.mode == SignInMode.LOGIN) 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = state.mode == SignInMode.LOGIN,
                onClick = { onIntent(SignInIntent.SwitchMode(SignInMode.LOGIN)) },
                text = { Text(stringResource(SignInMode.LOGIN.titleRes)) }
            )
            Tab(
                selected = state.mode == SignInMode.REGISTER,
                onClick = { onIntent(SignInIntent.SwitchMode(SignInMode.REGISTER)) },
                text = { Text(stringResource(SignInMode.REGISTER.titleRes)) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 注册方式选择 (仅在注册模式下显示)
        if (state.mode == SignInMode.REGISTER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 访客注册
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = state.registerType == RegisterType.GUEST,
                        onClick = { onIntent(SignInIntent.SwitchRegisterType(RegisterType.GUEST)) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(RegisterType.GUEST.labelRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 邀请码注册
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = state.registerType == RegisterType.INVITE,
                        onClick = { onIntent(SignInIntent.SwitchRegisterType(RegisterType.INVITE)) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(RegisterType.INVITE.labelRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 用户名输入框
        OutlinedTextField(
            value = state.username,
            onValueChange = { onIntent(SignInIntent.InputUsername(it)) },
            label = { Text(stringResource(Res.string.username)) },
            singleLine = true,
            isError = state.usernameError != null,
            supportingText = state.usernameError?.let { error ->
                { Text(stringResource(error)) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 密码输入框
        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(SignInIntent.InputPassword(it)) },
            label = { Text(stringResource(Res.string.password)) },
            singleLine = true,
            visualTransformation = if (state.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { onIntent(SignInIntent.TogglePasswordVisibility) }) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (state.isPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        }
                    )
                }
            },
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { error ->
                { Text(stringResource(error)) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (state.mode == SignInMode.LOGIN) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                }
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    focusManager.clearFocus()
                    if (state.isFormValid && !state.isLoading) {
                        onIntent(SignInIntent.Submit)
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 邮箱输入框 (仅在注册模式下显示)
        if (state.mode == SignInMode.REGISTER) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = state.email,
                onValueChange = { onIntent(SignInIntent.InputEmail(it)) },
                label = { Text(stringResource(Res.string.email)) },
                singleLine = true,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { error ->
                    { Text(stringResource(error)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (state.registerType == RegisterType.INVITE) {
                        ImeAction.Next
                    } else {
                        ImeAction.Done
                    }
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        if (state.isFormValid && !state.isLoading) {
                            onIntent(SignInIntent.Submit)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 邀请码输入框 (仅在邀请码注册模式下显示)
        if (state.mode == SignInMode.REGISTER && state.registerType == RegisterType.INVITE) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = state.inviteToken,
                onValueChange = { onIntent(SignInIntent.InputInviteToken(it)) },
                label = { Text(stringResource(Res.string.invite_code)) },
                singleLine = true,
                isError = state.inviteTokenError != null,
                supportingText = state.inviteTokenError?.let { error ->
                    { Text(stringResource(error)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (state.isFormValid && !state.isLoading) {
                            onIntent(SignInIntent.Submit)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 提交按钮
        Button(
            onClick = { onIntent(SignInIntent.Submit) },
            enabled = state.isFormValid && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (state.mode == SignInMode.LOGIN) {
                            Res.string.signing_in
                        } else {
                            Res.string.registering
                        }
                    )
                )
            } else {
                Text(
                    text = stringResource(
                        if (state.mode == SignInMode.LOGIN) {
                            Res.string.sign_in
                        } else {
                            Res.string.register
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ============= Preview =============

@AppVerticalZHPreview
@Composable
private fun SignInScreenPreview() {
    AppPreviewContent {
        SignInContent(
            state = SignInState(),
            onIntent = {}
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun SignInScreenRegisterPreview() {
    AppPreviewContent {
        SignInContent(
            state = SignInState(
                mode = SignInMode.REGISTER,
                registerType = RegisterType.INVITE,
                username = "testuser",
                password = "123456",
                email = "test@example.com"
            ),
            onIntent = {}
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun SignInScreenWithErrorsPreview() {
    AppPreviewContent {
        SignInContent(
            state = SignInState(
                username = "ab",
                password = "123",
                usernameError = Res.string.error_username_too_short,
                passwordError = Res.string.error_password_too_short
            ),
            onIntent = {}
        )
    }
}
