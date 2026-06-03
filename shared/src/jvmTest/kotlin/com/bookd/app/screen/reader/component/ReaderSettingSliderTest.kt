package com.bookd.app.screen.reader.component

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderSettingSliderTest {

    @Test
    fun `given decimal step when snap reader setting value then snaps to nearest tick`() {
        assertEquals(1.6f, snapReaderSettingValue(1.64f, 1.0f..2.5f, 0.1f))
        assertEquals(1.7f, snapReaderSettingValue(1.66f, 1.0f..2.5f, 0.1f))
    }

    @Test
    fun `given integer step when snap reader setting value then clamps to range`() {
        assertEquals(12f, snapReaderSettingValue(8f, 12f..32f, 1f))
        assertEquals(32f, snapReaderSettingValue(40f, 12f..32f, 1f))
        assertEquals(24f, snapReaderSettingValue(23.2f, 12f..32f, 4f))
    }

    @Test
    fun `given slider range and step when count ticks then includes endpoints`() {
        assertEquals(21, readerSettingTickCount(12f..32f, 1f))
        assertEquals(16, readerSettingTickCount(1.0f..2.5f, 0.1f))
        assertEquals(11, readerSettingTickCount(16f..96f, 8f))
    }

    @Test
    fun `given tick count when get interior tick indices then excludes endpoints`() {
        assertEquals(listOf(1, 2, 3), readerSettingInteriorTickIndices(5).toList())
        assertEquals(emptyList(), readerSettingInteriorTickIndices(2).toList())
    }

    @Test
    fun `given x position when map slider value then respects thumb inset`() {
        assertEquals(12f, sliderXToReaderSettingValue(0f, 220f, 10f, 12f..32f))
        assertEquals(22f, sliderXToReaderSettingValue(110f, 220f, 10f, 12f..32f))
        assertEquals(32f, sliderXToReaderSettingValue(220f, 220f, 10f, 12f..32f))
    }
}
