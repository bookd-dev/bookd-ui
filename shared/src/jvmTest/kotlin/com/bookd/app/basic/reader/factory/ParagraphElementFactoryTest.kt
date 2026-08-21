package com.bookd.app.basic.reader.factory

import org.junit.Test
import kotlin.test.assertEquals

class ParagraphElementFactoryTest {

    @Test
    fun `given text command y when resolve footnote inline top left then adds placeholder y`() {
        val topLeft = resolveFootnoteInlineContentTopLeft(
            commandY = 120,
            placeholderLeft = 18f,
            placeholderTop = 6f,
        )

        assertEquals(18f, topLeft.x)
        assertEquals(126f, topLeft.y)
    }
}
