package com.bookd.app.data.vm

import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.TocItem
import org.junit.Test
import kotlin.test.assertEquals

/**
 * ReaderState 计算属性单元测试
 *
 * 重点验证 chapterCount 和 totalChapters 在各种 spine/toc 配置下的行为差异，
 * 确保索引边界检查使用 chapterCount（基于 spine）而非 totalChapters（仅 inToc 章节数）。
 */
class ReaderStateTest {

    // ============ 辅助函数 ============

    /** 创建一个最小的 BookManifest，只需指定关键字段 */
    private fun manifest(
        totalChapters: Int,
        spine: List<Int>,
        toc: List<TocItem> = emptyList()
    ) = BookManifest(
        id = 1,
        title = "Test Book",
        author = null,
        format = "epub",
        totalChapters = totalChapters,
        toc = toc,
        spine = spine,
        metadata = null
    )

    /** 创建简单的 TocItem */
    private fun tocItem(index: Int, title: String = "Chapter $index") =
        TocItem(index = index, title = title)

    // ============ chapterCount 测试 ============

    @Test
    fun `given manifest is null when get chapterCount then returns 0`() {
        val state = ReaderState(manifest = null)
        assertEquals(0, state.chapterCount)
    }

    @Test
    fun `given empty spine when get chapterCount then returns 0`() {
        val state = ReaderState(manifest = manifest(totalChapters = 0, spine = emptyList()))
        assertEquals(0, state.chapterCount)
    }

    @Test
    fun `given contiguous spine 0-4 when get chapterCount then returns 5`() {
        val state = ReaderState(manifest = manifest(
            totalChapters = 5,
            spine = listOf(0, 1, 2, 3, 4)
        ))
        assertEquals(5, state.chapterCount)
    }

    @Test
    fun `given sparse spine with gaps when get chapterCount then returns max index plus 1`() {
        // 模拟 bookId=257 的情况：toc 有 12 个条目，但 spine 最大 index 是 14
        val state = ReaderState(manifest = manifest(
            totalChapters = 12,
            spine = listOf(0, 1, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14)
        ))
        assertEquals(15, state.chapterCount)  // maxIndex=14, 所以 chapterCount=15
        assertEquals(12, state.totalChapters) // totalChapters 仍然是 12（inToc 数量）
    }

    @Test
    fun `given single chapter spine when get chapterCount then returns 1`() {
        val state = ReaderState(manifest = manifest(
            totalChapters = 1,
            spine = listOf(0)
        ))
        assertEquals(1, state.chapterCount)
    }

    @Test
    fun `given spine starting from high index when get chapterCount then returns max plus 1`() {
        // 极端情况：spine 不从 0 开始
        val state = ReaderState(manifest = manifest(
            totalChapters = 3,
            spine = listOf(5, 8, 10)
        ))
        assertEquals(11, state.chapterCount)  // maxIndex=10 → 11
    }

    // ============ totalChapters 与 chapterCount 对比测试 ============

    @Test
    fun `given normal book when totalChapters and chapterCount may differ`() {
        // 连续 spine 且所有章节都 inToc：两者相同
        val state1 = ReaderState(manifest = manifest(
            totalChapters = 10,
            spine = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        ))
        assertEquals(state1.totalChapters, state1.chapterCount)

        // 有 gap 的 spine：chapterCount > totalChapters
        val state2 = ReaderState(manifest = manifest(
            totalChapters = 5,
            spine = listOf(0, 2, 5, 8, 10)
        ))
        assertEquals(5, state2.totalChapters)
        assertEquals(11, state2.chapterCount) // maxIndex=10 → 11
    }

    // ============ hasNextChapter 测试 ============

    @Test
    fun `given sparse spine at last toc chapter when hasNextChapter then returns true`() {
        // 核心 bug 场景：用户在 index 11（totalChapters-1），
        // 如果用 totalChapters 判断则 hasNextChapter=false
        // 但实际 spine 到 14，应该还有下一章
        val state = ReaderState(
            currentChapterIndex = 11,
            manifest = manifest(
                totalChapters = 12,
                spine = listOf(0, 1, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14)
            )
        )
        assertEquals(true, state.hasNextChapter)  // chapterCount=15, 11 < 14 → true
    }

    @Test
    fun `given at actual last chapter when hasNextChapter then returns false`() {
        val state = ReaderState(
            currentChapterIndex = 14,
            manifest = manifest(
                totalChapters = 12,
                spine = listOf(0, 1, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14)
            )
        )
        assertEquals(false, state.hasNextChapter) // chapterCount=15, 14 < 14 → false
    }

    @Test
    fun `given contiguous spine at last chapter when hasNextChapter then returns false`() {
        val state = ReaderState(
            currentChapterIndex = 9,
            manifest = manifest(
                totalChapters = 10,
                spine = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            )
        )
        assertEquals(false, state.hasNextChapter)
    }

    @Test
    fun `given at first chapter when hasPreviousChapter then returns false`() {
        val state = ReaderState(
            currentChapterIndex = 0,
            manifest = manifest(totalChapters = 10, spine = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        )
        assertEquals(false, state.hasPreviousChapter)
    }

    @Test
    fun `given at second chapter when hasPreviousChapter then returns true`() {
        val state = ReaderState(
            currentChapterIndex = 1,
            manifest = manifest(totalChapters = 10, spine = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        )
        assertEquals(true, state.hasPreviousChapter)
    }
}
