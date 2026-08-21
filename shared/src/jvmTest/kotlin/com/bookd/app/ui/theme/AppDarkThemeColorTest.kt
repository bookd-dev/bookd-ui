package com.bookd.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertTrue

class AppDarkThemeColorTest {

    @Test
    fun `given dark theme content colors when compared with containers then contrast is readable`() {
        assertReadableContrast(DarkOnBackground, DarkBackground)
        assertReadableContrast(DarkOnSurface, DarkSurface)
        assertReadableContrast(DarkOnSurfaceVariant, DarkSurfaceVariant)
        assertReadableContrast(DarkOnPrimaryContainer, DarkPrimaryContainer)
        assertReadableContrast(DarkOnSecondaryContainer, DarkSecondaryContainer)
    }

    @Test
    fun `given bottom tab muted color when compared with dark background then remains visible`() {
        assertReadableContrast(DarkOutline, DarkBackground, minimum = 3.0)
    }

    private fun assertReadableContrast(
        foreground: Color,
        background: Color,
        minimum: Double = 4.5,
    ) {
        val contrast = contrastRatio(foreground, background)
        assertTrue(
            actual = contrast >= minimum,
            message = "Expected contrast >= $minimum but was $contrast",
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = first.relativeLuminance()
        val secondLuminance = second.relativeLuminance()
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.relativeLuminance(): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
