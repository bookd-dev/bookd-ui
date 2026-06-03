package com.bookd.app.data.model

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class ReaderAnchorModelCompatibilityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `given cached chapter content without anchorId when decode then element anchors default to null`() {
        val raw = """
            {
              "index": 1,
              "title": "旧章节",
              "elements": [
                {
                  "type": "paragraph",
                  "spans": [
                    { "text": "旧缓存段落" }
                  ]
                },
                {
                  "type": "divider"
                }
              ],
              "prevIndex": 0,
              "nextIndex": 2
            }
        """.trimIndent()

        val decoded = json.decodeFromString<ChapterContent>(raw)

        assertEquals(2, decoded.elements.size)
        assertEquals(null, decoded.elements[0].anchorId)
        assertEquals(null, decoded.elements[1].anchorId)
    }

    @Test
    fun `given legacy reading progress without anchor fields when decode then chapter falls back to current page`() {
        val raw = """
            {
              "id": 1,
              "bookId": 10,
              "progress": 0.25,
              "currentPage": 7,
              "totalPages": 30,
              "lastReadAt": "2026-06-03T00:00:00Z"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<ReadingProgressResponse>(raw)

        assertEquals(7, decoded.currentPage)
        assertEquals(7, decoded.chapterIndex)
        assertEquals(null, decoded.anchorId)
        assertEquals(null, decoded.paragraphIndex)
        assertEquals(null, decoded.scrollOffset)
    }

    @Test
    fun `given legacy bookmark response without anchor fields when decode then uses default fallback fields`() {
        val raw = """
            {
              "id": 5,
              "bookId": 10,
              "positionType": "page",
              "positionValue": "7",
              "title": "旧书签",
              "createdAt": "2026-06-03T00:00:00Z"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<BookmarkResponse>(raw)

        assertEquals(0, decoded.chapterIndex)
        assertEquals(null, decoded.anchorId)
        assertEquals(null, decoded.paragraphIndex)
        assertEquals(null, decoded.scrollOffset)
        assertEquals("page", decoded.positionType)
        assertEquals("7", decoded.positionValue)
    }
}
