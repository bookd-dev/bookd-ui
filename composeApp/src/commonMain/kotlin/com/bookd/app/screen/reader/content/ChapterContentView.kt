package com.bookd.app.screen.reader.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

/**
 * 章节内容渲染组件
 * 
 * 渲染整个章节的内容列表，支持：
 * - 滚动浏览
 * - 长按段落添加书签
 * - 图片点击预览
 * - 脚注点击弹窗
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterContentView(
    chapter: ChapterContent,
    settings: ReaderSettings,
    listState: LazyListState = rememberLazyListState(),
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 收集章节中的所有脚注，用于通过 ID 查找
    val footnotes = chapter.elements.filterIsInstance<ContentElement.Footnote>()
        .associateBy { it.footnoteId }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = settings.marginHorizontal.dp,
            vertical = settings.marginVertical.dp
        )
    ) {
        itemsIndexed(
            items = chapter.elements,
            key = { index, _ -> "${chapter.index}_$index" }
        ) { index, element ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = settings.paragraphSpacing.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            // 只对段落类型支持长按
                            if (element is ContentElement.Paragraph) {
                                onParagraphLongClick(index)
                            }
                        }
                    )
            ) {
                ContentElementView(
                    element = element,
                    settings = settings,
                    onImageClick = onImageClick,
                    onFootnoteClick = { footnoteId ->
                        footnotes[footnoteId]?.let { onFootnoteClick(it) }
                    },
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}
