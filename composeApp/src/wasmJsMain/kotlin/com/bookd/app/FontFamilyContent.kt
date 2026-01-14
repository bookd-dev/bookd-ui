package com.bookd.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.bookd.app.screen.App
import com.bookd.app.ui.theme.LocalAppFontFamily
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator


@Composable
fun FontFamilyContent(
    content: @Composable () -> Unit
) {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    var fontFamily by remember { mutableStateOf<FontFamily?>(null) }

    LaunchedEffect(Unit) {
        //加载字体
        val notoEmoji = loadFontBytes("./fonts/NotoColorEmoji.ttf")
        val notoSans = loadFontBytes("./fonts/NotoSans.ttf")
        val notoSansJP = loadFontBytes("./fonts/NotoSansJP.ttf")
        val notoSansKR = loadFontBytes("./fonts/NotoSansKR.ttf")
        val notoSansSC = loadFontBytes("./fonts/NotoSansSC.ttf")
        val notoSansSymbols = loadFontBytes("./fonts/NotoSansSymbols.ttf")
        val notoSansTC = loadFontBytes("./fonts/NotoSansTC.ttf")

        val notoSansFamily = FontFamily(
            Font("NotoColorEmoji", getData = { notoEmoji }),
            Font("NotoSansSC", getData = { notoSansSC }),
            Font("NotoSansTC", getData = { notoSansTC }),
            Font("NotoSansJP", getData = { notoSansJP }),
            Font("NotoSansKR", getData = { notoSansKR }),
            Font("NotoSansSymbols", getData = { notoSansSymbols }),
            Font("NotoSans", getData = { notoSans }) // Latin 放最后
        )
        fontFamilyResolver.preload(notoSansFamily)
        fontFamily = notoSansFamily
    }

    fontFamily?.let {
        CompositionLocalProvider(LocalAppFontFamily provides it) {
            content()
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun loadFontBytes(url: String): ByteArray {
    val buffer = window.fetch(url).await<Response>().arrayBuffer().await<ArrayBuffer>()
    val source = Int8Array(buffer, 0, buffer.byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

@OptIn(UnsafeWasmMemoryApi::class)
private fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}