package com.bookd.app.basic.reader.extension

import com.bookd.app.basic.reader.data.RenderCommand

/**
 * 获取渲染指令的高度
 */
fun getCommandHeight(command: RenderCommand): Int {
    return when (command) {
        is RenderCommand.Text -> command.textLayout.size.height
        is RenderCommand.Image -> command.height + (command.altTextLayout?.size?.height ?: 0)
        is RenderCommand.Heading -> command.textLayout.size.height
        is RenderCommand.Quote -> command.textLayout.size.height
        is RenderCommand.Code -> command.textLayout.size.height
        is RenderCommand.ListItem -> maxOf(command.prefixLayout.size.height, command.textLayout.size.height)
        is RenderCommand.ListBlock -> command.items.sumOf { getCommandHeight(it) }
        is RenderCommand.Divider -> command.height
        is RenderCommand.Footnote -> command.markerLayout.size.height + command.contentLayouts.sumOf { it.size.height }
    }
}