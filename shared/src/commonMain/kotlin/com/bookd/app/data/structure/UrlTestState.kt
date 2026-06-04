package com.bookd.app.data.structure

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.failed
import app.composeapp.generated.resources.pending
import app.composeapp.generated.resources.success
import com.bookd.app.ui.theme.StatusFailed
import com.bookd.app.ui.theme.StatusSuccess
import org.jetbrains.compose.resources.StringResource

/**
 * 单个 URL 的连接测试状态
 */
enum class UrlTestState {
    Pending,
    Success,
    Failed
}


/**
 * UrlTestState 的文本资源
 */
val UrlTestState.text: StringResource
    get() = when (this) {
        UrlTestState.Pending -> Res.string.pending
        UrlTestState.Success -> Res.string.success
        UrlTestState.Failed -> Res.string.failed
    }

/**
 * UrlTestState 的颜色
 */
@Composable
fun UrlTestState.color(): Color = when (this) {
    UrlTestState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
    UrlTestState.Success -> StatusSuccess
    UrlTestState.Failed -> StatusFailed
}
