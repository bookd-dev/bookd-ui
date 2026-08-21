package com.bookd.app.screen.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val generatedTextCoverFileName = Regex(
    pattern = "^book_\\d+_generated\\.[^./]+$",
    option = RegexOption.IGNORE_CASE
)

/**
 * 文字生成封面沿用原有纯色页面背景，真实图片封面才参与详情页背景绘制。
 */
internal fun resolveBookDetailBackgroundCoverPath(coverPath: String?): String? {
    val normalizedPath = coverPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val fileName = normalizedPath
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('/')

    return normalizedPath.takeUnless { generatedTextCoverFileName.matches(fileName) }
}

@Composable
internal fun BookDetailBackground(
    coverPath: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = coverPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.42f
                    scaleX = 1.12f
                    scaleY = 1.12f
                }
                .blur(36.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.48f))
        )
    }
}
