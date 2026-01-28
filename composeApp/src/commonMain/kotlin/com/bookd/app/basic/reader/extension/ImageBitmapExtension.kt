package com.bookd.app.basic.reader.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.bookd.app.data.model.ContentElement

@Composable
fun List<ContentElement>.rememberImagePainterMap(): Map<String, AsyncImagePainter> {
    val painters = mutableMapOf<String, AsyncImagePainter>()

    this.forEach { element ->
        when (element) {
            is ContentElement.Image -> {
                // 关键点：使用 key 确保 Painter 与 src 绑定，不受列表顺序影响
                painters[element.src] = key(element.src) {
                    rememberAsyncImagePainter(element.src)
                }
            }

            is ContentElement.Footnote -> {
                element.footnoteImage?.let { src ->
                    painters[src] = key(src) {
                        rememberAsyncImagePainter(src)
                    }
                }
            }

            else -> { //ignore this
            }
        }
    }
    return painters
}