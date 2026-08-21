package com.bookd.app.screen

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppShellInsetsTest {

    @Test
    fun `given top level shell policy then outer shell owns system safe areas`() {
        assertTrue(AppShellOuterSafeAreaPolicy.includesTop)
        assertTrue(AppShellOuterSafeAreaPolicy.includesBottom)
        assertTrue(AppShellOuterSafeAreaPolicy.includesHorizontal)
    }

    @Test
    fun `given top level shell policy then nested content does not duplicate system safe areas`() {
        assertFalse(AppShellContentSafeAreaPolicy.includesTop)
        assertFalse(AppShellContentSafeAreaPolicy.includesBottom)
        assertFalse(AppShellContentSafeAreaPolicy.includesHorizontal)
    }
}
