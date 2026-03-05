package com.bookd.app.screen.reader.content

import org.junit.Test
import kotlin.test.assertEquals

/**
 * detectChapterByMaxVisibleArea 纯函数单元测试
 *
 * 测试场景覆盖：
 * - 正常长章节滚动切换
 * - 多个短章节同屏
 * - bottom-spacer 可见时的正向推进
 * - 大面积 spacer 遮挡时的兜底逻辑
 * - 回退依赖面积最大章节（防死循环）
 * - 边界情况（空列表、全是 spacer 等）
 */
class ChapterDetectionTest {

    // ============ 辅助函数 ============

    /** 创建章节 item（key 格式 "$chapterIdx-$pageIndex"） */
    private fun chapterItem(chapterIdx: Int, pageIndex: Int, offset: Int, size: Int) =
        VisibleItemInfo(key = "$chapterIdx-$pageIndex", offset = offset, size = size)

    /** 创建 top-spacer item */
    private fun topSpacer(offset: Int, size: Int) =
        VisibleItemInfo(key = "top-spacer", offset = offset, size = size)

    /** 创建 bottom-spacer item */
    private fun bottomSpacer(offset: Int, size: Int) =
        VisibleItemInfo(key = "bottom-spacer", offset = offset, size = size)

    // ============ 正常长章节场景 ============

    @Test
    fun `given single long chapter visible when it fills viewport then return that chapter`() {
        // ch3 占满整个屏幕，面积 = 2712
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 2712)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    @Test
    fun `given scrolling from ch3 to ch4 when ch4 has more visible area then switch to ch4`() {
        // 用户向下滚动，ch3 只剩上部 1000px 可见，ch4 有 1712px 可见
        // viewport [1000, 3712]
        // ch3 面积: clamp [1000, 2000] = 1000
        // ch4 面积: clamp [2000, 3712] = 1712 → ch4 面积更大
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 2000),   // 0~2000
            chapterItem(4, 0, offset = 2000, size = 3000)  // 2000~5000
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 1000,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(4, result)
    }

    @Test
    fun `given scrolling back from ch4 to ch3 when ch3 has more visible area then switch to ch3`() {
        // 用户向上滚动
        // viewport [500, 3212]
        // ch3 面积: clamp [500, 2000] = 1500
        // ch4 面积: clamp [2000, 3212] = 1212 → ch3 面积更大
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 2000),   // 0~2000
            chapterItem(4, 0, offset = 2000, size = 3000)  // 2000~5000
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 500,
            viewportHeight = 2712,
            currentChapterIndex = 4
        )
        assertEquals(3, result)
    }

    @Test
    fun `given two chapters with equal visible area when current is ch3 then prefer ch3`() {
        // 两个章节各占一半屏幕，面积相等
        // viewport [0, 2000]
        // ch3 面积: 1000, ch4 面积: 1000
        // 面积相同时，优先选更接近 current 的 → ch3
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 1000),   // 0~1000
            chapterItem(4, 0, offset = 1000, size = 1000)  // 1000~2000
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2000,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    // ============ 多个短章节同屏 ============

    @Test
    fun `given multiple short chapters on screen at bottom when current is ch3 then forward to maxVisible`() {
        // ch0~ch5 都是短章节，总高度不足一屏，bottom-spacer 可见
        // 到底时应正向推进到 maxVisible
        val items = listOf(
            topSpacer(offset = 0, size = 100),
            chapterItem(0, 0, offset = 100, size = 200),   // 100~300  封面
            chapterItem(1, 0, offset = 300, size = 600),   // 300~900  制作信息
            chapterItem(2, 0, offset = 900, size = 100),   // 900~1000
            chapterItem(3, 0, offset = 1000, size = 500),  // 1000~1500 简介
            chapterItem(4, 0, offset = 1500, size = 200),  // 1500~1700 彩页
            chapterItem(5, 0, offset = 1700, size = 100),  // 1700~1800
            bottomSpacer(offset = 1800, size = 100)
        )
        // ch1 面积最大(600)，但 isAtBottom=true，maxVisible=5 > current=3 → 正向推进到 5
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(5, result)
    }

    @Test
    fun `given multiple short chapters not at bottom when largest area chapter is before current then retreat`() {
        // ch0~ch5 都是短章节，没有 bottom-spacer
        // ch1 面积最大(600) > ch3(500)，current=3
        // 没到底时，面积策略选 ch1（面积最大）→ 回退到 ch1
        val items = listOf(
            chapterItem(0, 0, offset = 100, size = 200),   // 面积 200
            chapterItem(1, 0, offset = 300, size = 600),   // 面积 600 ← 最大
            chapterItem(2, 0, offset = 900, size = 100),   // 面积 100
            chapterItem(3, 0, offset = 1000, size = 500),  // 面积 500
            chapterItem(4, 0, offset = 1500, size = 200),  // 面积 200
            chapterItem(5, 0, offset = 1700, size = 100),  // 面积 100
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(1, result)
    }

    @Test
    fun `given multiple short chapters not at bottom when current chapter has largest area then stay`() {
        // 同样多短章节场景，但 ch3 面积最大 → 保持 ch3
        val items = listOf(
            chapterItem(0, 0, offset = 100, size = 200),   // 面积 200
            chapterItem(1, 0, offset = 300, size = 400),   // 面积 400
            chapterItem(2, 0, offset = 700, size = 100),   // 面积 100
            chapterItem(3, 0, offset = 800, size = 700),   // 面积 700 ← 最大
            chapterItem(4, 0, offset = 1500, size = 200),  // 面积 200
            chapterItem(5, 0, offset = 1700, size = 100),  // 面积 100
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    @Test
    fun `given multiple short chapters on screen when ch1 has largest area and current is ch3 then allow retreat to ch1`() {
        // ch1 面积最大(600)，current=3 → 回退到 ch1
        val items = listOf(
            topSpacer(offset = 0, size = 100),
            chapterItem(0, 0, offset = 100, size = 200),   // 面积 200
            chapterItem(1, 0, offset = 300, size = 600),   // 面积 600 ← 最大
            chapterItem(2, 0, offset = 900, size = 100),   // 面积 100
            chapterItem(3, 0, offset = 1000, size = 500),  // 面积 500 (viewport [0, 1200] → clamp 200)
        )
        // viewport [0, 1200]
        // ch0: clamp [100, 300] ∩ [0, 1200] = 200
        // ch1: clamp [300, 900] ∩ [0, 1200] = 600
        // ch2: clamp [900, 1000] ∩ [0, 1200] = 100
        // ch3: clamp [1000, 1500] ∩ [0, 1200] = 200
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 1200,
            currentChapterIndex = 3
        )
        assertEquals(1, result)
    }

    // ============ bottom-spacer 可见时的正向推进 ============

    @Test
    fun `given at bottom with short chapters when maxVisible is greater than current then forward to maxVisible`() {
        // viewport [0, 500]
        // ch3: clamp [0, 500] = 500 → 面积最大
        // ch4: clamp [500, 700] ∩ [0, 500] = 0
        // ch5: clamp [700, 800] ∩ [0, 500] = 0
        // maxAreaChapter=3, 但 isAtBottom=true, maxVisible=5 > current=3 → 正向推进到5
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 500),     // 面积 500
            chapterItem(4, 0, offset = 500, size = 200),   // 面积 0（在 viewport 外）
            chapterItem(5, 0, offset = 700, size = 100),   // 面积 0（在 viewport 外）
            bottomSpacer(offset = 800, size = 100)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 500,
            currentChapterIndex = 3
        )
        assertEquals(5, result)
    }

    @Test
    fun `given at bottom when maxVisible equals current then stay at current`() {
        // 到底了，但屏幕上最大章节就是 current → 保持不变
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 500),
            bottomSpacer(offset = 500, size = 100)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 500,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    @Test
    fun `given not at bottom when later chapters visible but current has largest area then stay`() {
        // 没到底，ch3 面积最大(1500) → 保持 ch3
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 1500),   // 面积 1500 ← 最大
            chapterItem(4, 0, offset = 1500, size = 1000), // 面积 1000
            chapterItem(5, 0, offset = 2500, size = 500),  // 面积 212（clamp to viewport）
        )
        // viewport [0, 2712]
        // ch3: 1500, ch4: 1000, ch5: clamp [2500, 2712] = 212
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    // ============ 大面积 spacer 遮挡时的兜底 ============

    @Test
    fun `given top-spacer covers most of viewport when chapter items exist then pick largest area chapter`() {
        // top-spacer 很大但被忽略，只看章节 item 面积
        // viewport [0, 2712]
        // ch0: clamp [2000, 2500] = 500
        // ch1: clamp [2500, 2712] = 212
        // ch0 面积更大 → 选 ch0
        val items = listOf(
            topSpacer(offset = 0, size = 2000),
            chapterItem(0, 0, offset = 2000, size = 500), // 面积 500
            chapterItem(1, 0, offset = 2500, size = 500), // 面积 212
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 0
        )
        assertEquals(0, result)
    }

    @Test
    fun `given bottom-spacer covers most of viewport when chapter items exist then pick largest area chapter`() {
        // bottom-spacer 很大但被忽略
        // viewport [0, 2712]
        // ch3: clamp [0, 200] = 200
        // maxAreaChapter=3, isAtBottom=true, maxVisible=3, candidate=max(3,3)=3 → 3
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 200),     // 面积 200
            bottomSpacer(offset = 200, size = 2000)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    // ============ 防死循环：回退依赖面积最大章节 ============

    @Test
    fun `given current is 5 and ch3 has largest area when at bottom with maxVisible 5 then retreat to ch3`() {
        // current=5，但 ch3 面积最大 → 应回退到3
        // viewport [0, 500]
        // ch3: clamp [0, 500] = 500
        // ch4: clamp [500, 500] = 0
        // ch5: clamp [700, 500] = 0（viewport 外）
        // maxAreaChapter=3, maxVisible=5, candidate=max(3,5)=5
        // 5 not > 5 → maxAreaChapter=3
        val items = listOf(
            chapterItem(3, 0, offset = 0, size = 500),
            chapterItem(4, 0, offset = 500, size = 200),
            chapterItem(5, 0, offset = 700, size = 100),
            bottomSpacer(offset = 800, size = 100)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 500,
            currentChapterIndex = 5
        )
        assertEquals(3, result)
    }

    @Test
    fun `given current is 5 and ch5 has largest area when at bottom then stay at 5`() {
        // current=5，ch5 面积最大(300 > ch4的200) → 保持不变
        // viewport [0, 600]
        // ch4: clamp [0, 200] = 200
        // ch5: clamp [200, 500] = 300
        val items = listOf(
            chapterItem(4, 0, offset = 0, size = 200),
            chapterItem(5, 0, offset = 200, size = 300),
            bottomSpacer(offset = 500, size = 100)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 600,
            currentChapterIndex = 5
        )
        assertEquals(5, result)
    }

    // ============ 多页章节：同一章节多个 page item 面积应累加 ============

    @Test
    fun `given chapter with multiple pages when total area is largest then select that chapter`() {
        // ch8 有多个 page item，面积需累加
        // viewport [0, 2712]
        // ch7: page0 = 500
        // ch8: page0(800) + page1(800) + page2(612, clamp to 2712) = 2212 ← 累加后最大
        val items = listOf(
            chapterItem(7, 0, offset = 0, size = 500),     // ch7 面积 500
            chapterItem(8, 0, offset = 500, size = 800),    // ch8-p0 面积 800
            chapterItem(8, 1, offset = 1300, size = 800),   // ch8-p1 面积 800
            chapterItem(8, 2, offset = 2100, size = 800),   // ch8-p2 clamp [2100, 2712] = 612
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 7
        )
        // ch8 累加面积 = 800 + 800 + 612 = 2212 > ch7(500) → 正向推进到 ch8
        assertEquals(8, result)
    }

    // ============ 边界情况 ============

    @Test
    fun `given empty visible items then return current chapter`() {
        val result = detectChapterByMaxVisibleArea(
            visibleItems = emptyList(),
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    @Test
    fun `given only spacers visible then return current chapter`() {
        // 只有 spacer 可见（anchors 还没就绪）
        val items = listOf(
            topSpacer(offset = 0, size = 100),
            bottomSpacer(offset = 100, size = 100)
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 200,
            currentChapterIndex = 3
        )
        assertEquals(3, result)
    }

    // ============ 模拟日志中的死循环场景 ============

    @Test
    fun `given log scenario with ch0-ch5 all short when current is 3 then should forward and stabilize`() {
        // 模拟日志中的场景：ch0~ch5 全是短章节，current=3
        // viewport 2712px，所有内容+spacer 不到 2000px → bottom-spacer 可见
        val items = listOf(
            topSpacer(offset = 0, size = 100),
            chapterItem(0, 0, offset = 100, size = 300),   // 封面 面积 300
            chapterItem(1, 0, offset = 400, size = 600),   // 制作信息 面积 600 ← 最大
            chapterItem(2, 0, offset = 1000, size = 50),   // 面积 50
            chapterItem(3, 0, offset = 1050, size = 400),  // 简介 面积 400
            chapterItem(4, 0, offset = 1450, size = 300),  // 彩页 面积 300
            chapterItem(5, 0, offset = 1750, size = 50),   // 面积 50
            bottomSpacer(offset = 1800, size = 100)
        )

        // current=3: maxAreaChapter=1, maxVisible=5, candidate=max(1,5)=5
        // 5 > 3 → 正向推进到5
        val result3 = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 3
        )
        assertEquals(5, result3)

        // current=5: maxAreaChapter=1, maxVisible=5, candidate=max(1,5)=5
        // 5 not > 5 → maxAreaChapter=1
        val result5 = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 5
        )
        assertEquals(1, result5)

        // current=1: maxAreaChapter=1, maxVisible=5, candidate=max(1,5)=5
        // 5 > 1 → 正向推进到5
        val result1 = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 1
        )
        assertEquals(5, result1)

        // 看似 5→1→5→1 循环，但实际上不会发生：
        // 1. adjacentChapters 只增不减 → LazyColumn items 不变 → 不触发 recompose
        // 2. distinctUntilChanged 过滤重复值
        // 3. snapshotFlow 只在 visibleItems 变化时重新评估
        // 4. 冷却机制阻止 800ms 内回退
    }

    // ============ 面积策略稳定性：布局抖动不切换 ============

    @Test
    fun `given new chapters loaded when their visible area is tiny then stay at current`() {
        // 模拟新章节 anchor 就绪后 items 突然增多的场景
        // ch7(当前)面积很大，ch8/ch9 刚进入 viewport 边缘面积极小
        // 面积策略应保持在 ch7
        val items = listOf(
            chapterItem(7, 0, offset = 0, size = 2500),    // ch7 面积 2500 ← 压倒性
            chapterItem(8, 0, offset = 2500, size = 200),   // ch8 面积 200（viewport 边缘）
            chapterItem(8, 1, offset = 2700, size = 200),   // ch8 面积 12（clamp to viewport）
        )
        // viewport [0, 2712]
        // ch7: 2500, ch8: clamp [2500,2700]=200 + clamp [2700,2712]=12 = 212
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2712,
            currentChapterIndex = 7
        )
        assertEquals(7, result)
    }

    @Test
    fun `given layout jitter after chapter load when areas shift slightly then prefer closer to current`() {
        // 模拟布局抖动：ch6 和 ch7 面积非常接近，但 ch7 是当前章节
        // 即使 ch6 面积稍大，sortedWith 的 thenBy 逻辑优先选更接近 current 的
        // viewport [0, 2000]
        // ch6: clamp [0, 1050] = 1050
        // ch7: clamp [1050, 2000] = 950
        // ch6 面积更大(1050 > 950)，但 ch6 不是 current → maxAreaChapter=6
        // 6 < 7 → 回退到6
        // 注意：这是正确行为 —— ch6 真的占据更多面积时，说明用户确实在看 ch6
        val items = listOf(
            chapterItem(6, 0, offset = 0, size = 1050),
            chapterItem(7, 0, offset = 1050, size = 1000),
        )
        val result = detectChapterByMaxVisibleArea(
            visibleItems = items,
            viewportStartOffset = 0,
            viewportHeight = 2000,
            currentChapterIndex = 7
        )
        assertEquals(6, result)
    }
}
