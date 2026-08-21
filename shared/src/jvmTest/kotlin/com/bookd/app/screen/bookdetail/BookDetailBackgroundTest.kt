package com.bookd.app.screen.bookdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookDetailBackgroundTest {

    @Test
    fun `given regular image cover when resolving detail background then returns cover path`() {
        val coverPath = "https://bookd.example/covers/real-cover.jpg"

        assertEquals(coverPath, resolveBookDetailBackgroundCoverPath(coverPath))
    }

    @Test
    fun `given generated text cover when resolving detail background then keeps original background`() {
        val coverPath = "https://bookd.example/book_images/covers/book_42_generated.png?version=2"

        assertNull(resolveBookDetailBackgroundCoverPath(coverPath))
    }

    @Test
    fun `given missing cover when resolving detail background then keeps original background`() {
        assertNull(resolveBookDetailBackgroundCoverPath(null))
        assertNull(resolveBookDetailBackgroundCoverPath("  "))
    }
}
