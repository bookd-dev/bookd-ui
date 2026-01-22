package com.bookd.app.screen.reader.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.screen.reader.content.element.CodeView
import com.bookd.app.screen.reader.content.element.DividerView
import com.bookd.app.screen.reader.content.element.FootnoteView
import com.bookd.app.screen.reader.content.element.HeadingView
import com.bookd.app.screen.reader.content.element.ImageView
import com.bookd.app.screen.reader.content.element.ListBlockView
import com.bookd.app.screen.reader.content.element.ParagraphView
import com.bookd.app.screen.reader.content.element.QuoteView

/**
 * 内容元素统一渲染入口
 * 
 * 根据 ContentElement 的具体类型分发到对应的渲染组件
 */
@Composable
fun ContentElementView(
    element: ContentElement,
    settings: ReaderSettings,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (String) -> Unit,
    onLinkClick: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (element) {
        is ContentElement.Paragraph -> ParagraphView(
            paragraph = element,
            settings = settings,
            onFootnoteClick = onFootnoteClick,
            onLinkClick = onLinkClick,
            modifier = modifier
        )
        
        is ContentElement.Heading -> HeadingView(
            heading = element,
            settings = settings,
            modifier = modifier
        )
        
        is ContentElement.Image -> ImageView(
            image = element,
            settings = settings,
            onImageClick = onImageClick,
            modifier = modifier
        )
        
        is ContentElement.Quote -> QuoteView(
            quote = element,
            settings = settings,
            onFootnoteClick = onFootnoteClick,
            onLinkClick = onLinkClick,
            modifier = modifier
        )
        
        is ContentElement.Code -> CodeView(
            code = element,
            settings = settings,
            modifier = modifier
        )
        
        is ContentElement.ListBlock -> ListBlockView(
            list = element,
            settings = settings,
            onFootnoteClick = onFootnoteClick,
            onLinkClick = onLinkClick,
            modifier = modifier
        )
        
        is ContentElement.Divider -> DividerView(
            modifier = modifier
        )
        
        is ContentElement.Footnote -> FootnoteView(
            footnote = element,
            settings = settings,
            modifier = modifier
        )
    }
}
