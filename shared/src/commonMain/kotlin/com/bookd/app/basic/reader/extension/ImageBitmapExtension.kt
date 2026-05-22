package com.bookd.app.basic.reader.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.bookd.app.data.model.ContentElement

/**
 * AsyncImagePainter 的 mutableStateOf 机制保证加载完自动重绘，不需要额外处理
 */
@Composable
fun List<ContentElement>.rememberImagePainterMap(): Map<String, AsyncImagePainter> {
    val painters = mutableMapOf<String, AsyncImagePainter>()

    this.forEach { element ->
        when (element) {
            is ContentElement.Image -> {
                painters[element.src] = rememberAsyncImagePainter(element.src)
            }

            is ContentElement.Footnote -> {
                element.footnoteImage?.let { src ->
                    painters[src] = rememberAsyncImagePainter(src)
                }
            }

            else -> { //ignore this
            }
        }
    }
    return painters
}