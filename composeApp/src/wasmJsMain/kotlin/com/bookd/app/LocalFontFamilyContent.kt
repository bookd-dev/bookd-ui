package com.bookd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.bookd.app.ui.theme.LocalAppFontFamily
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toUByteArray
import org.w3c.dom.Window
import org.w3c.files.Blob
import kotlin.js.Promise

@Composable
fun LocalFontFamilyContent(
    content: @Composable () -> Unit
) {
    Column {
        var fontFamily by remember { mutableStateOf<FontFamily?>(null) }
        var expanded by remember { mutableStateOf(false) }
        LocalFontDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            onValueChange = { fontFamily = it }
        ) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Outlined.FontDownload, null)
            }
        }

        fontFamily?.let {
            CompositionLocalProvider(LocalAppFontFamily provides it) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
private fun LocalFontDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onValueChange: (FontFamily) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<String?>(null) }
    val fontDataList = mutableStateListOf<FontData>()
    Box(modifier = modifier) {
        content()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            fontDataList.map { it.family }.toSet().forEach { family ->
                DropdownMenuItem(
                    text = { Text(family) },
                    onClick = {
                        coroutineScope.launch {
                            val fontFamily = fontDataList.fontFamily(family)
                            selected = family
                            onValueChange(fontFamily)
                            onDismissRequest()
                        }
                    },
                    trailingIcon = if (selected == family) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }
            LaunchedEffect(Unit) {
                fontDataList.clear()
                val fontArray = window.queryLocalFonts().await<JsArray<FontData>>()
                fontDataList.addAll(fontArray.toList())
            }
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalUnsignedTypes::class)
private suspend fun List<FontData>.fontFamily(family: String): FontFamily = coroutineScope {
    val fonts = mutableListOf<Font>()

    filter { it.family == family }.forEach {
        val styleLowercase = it.style.lowercase()
        val fontWeight = when (styleLowercase.split(' ')[0]) {
            "thin", "hairline" -> FontWeight.Thin
            "extrathin", "ultralight" -> FontWeight.ExtraLight
            "light" -> FontWeight.Light
            "medium" -> FontWeight.Medium
            "semibold", "demibold" -> FontWeight.SemiBold
            "bold" -> FontWeight.Bold
            "extrabold", "ultrabold" -> FontWeight.ExtraBold
            "black", "heavy" -> FontWeight.Black
            else -> FontWeight.Normal
        }
        val fontStyle = when {
            "italic" in styleLowercase || "oblique" in styleLowercase -> FontStyle.Italic
            else -> FontStyle.Normal
        }
        val blob = it.blob().await<Blob>()
        val arrayBuffer = blob.arrayBuffer().await<ArrayBuffer>()
        val font = Font(
            identity = it.postscriptName,
            weight = fontWeight,
            style = fontStyle,
            getData = {
                val bytes = Uint8Array(arrayBuffer)
                bytes.toUByteArray().toByteArray()
            }
        )
        fonts.add(font)
    }
    FontFamily(fonts)
}

@OptIn(ExperimentalWasmJsInterop::class)
external class FontData : JsAny {
    val postscriptName: String
    val fullName: String
    val family: String
    val style: String
    fun blob(): Promise<Blob>
}


@OptIn(ExperimentalWasmJsInterop::class)
private fun Window.queryLocalFonts(): Promise<JsArray<FontData>> {
    return queryLocalFonts(this)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun queryLocalFonts(windowArg: Window): Promise<JsArray<FontData>> = js("windowArg.queryLocalFonts()")

@OptIn(ExperimentalWasmJsInterop::class)
fun Blob.arrayBuffer(): Promise<ArrayBuffer> {
    return arrayBuffer(this)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun arrayBuffer(blobArg: Blob): Promise<ArrayBuffer> = js("blobArg.arrayBuffer()")


