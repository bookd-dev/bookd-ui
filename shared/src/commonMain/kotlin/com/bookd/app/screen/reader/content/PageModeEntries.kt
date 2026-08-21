package com.bookd.app.screen.reader.content

import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement

internal sealed interface PageModeEntry {
    val chapterIndex: Int
    val pageIndex: Int
    val key: String
}

internal data class ContentPageEntry(
    override val chapterIndex: Int,
    override val pageIndex: Int,
    val anchor: PageAnchor,
    val nextAnchor: PageAnchor?,
    val elements: List<ContentElement>
) : PageModeEntry {
    override val key: String = "page:$chapterIndex:$pageIndex"
}

internal data class LoadingPageEntry(
    override val chapterIndex: Int
) : PageModeEntry {
    override val pageIndex: Int = 0
    override val key: String = "loading:$chapterIndex"
}

internal fun buildReaderChapterElements(chapter: ChapterContent): List<ContentElement> {
    return buildList {
        chapter.title?.let { add(ContentElement.Heading(level = 1, text = it)) }
        addAll(chapter.elements)
    }
}

internal fun buildPageModeEntries(
    orderedChapterIndices: List<Int>,
    chapterElements: Map<Int, List<ContentElement>>,
    chapterAnchors: Map<Int, List<PageAnchor>>
): List<PageModeEntry> {
    return buildList {
        for (chapterIndex in orderedChapterIndices) {
            val elements = chapterElements[chapterIndex]
            val anchors = chapterAnchors[chapterIndex]
            if (elements == null || anchors == null) {
                add(LoadingPageEntry(chapterIndex))
                continue
            }

            val safeAnchors = anchors.ifEmpty { listOf(PageAnchor(0, 0, 0)) }
            safeAnchors.forEachIndexed { pageIndex, anchor ->
                add(
                    ContentPageEntry(
                        chapterIndex = chapterIndex,
                        pageIndex = pageIndex,
                        anchor = anchor,
                        nextAnchor = safeAnchors.getOrNull(pageIndex + 1),
                        elements = elements
                    )
                )
            }
        }
    }
}

internal fun findPageModeEntryIndex(
    entries: List<PageModeEntry>,
    chapterIndex: Int,
    pageIndex: Int
): Int {
    return entries.indexOfFirst { entry ->
        entry is ContentPageEntry &&
            entry.chapterIndex == chapterIndex &&
            entry.pageIndex == pageIndex
    }
}

internal fun findFirstChapterEntryIndex(
    entries: List<PageModeEntry>,
    chapterIndex: Int
): Int {
    return entries.indexOfFirst { it.chapterIndex == chapterIndex }
}

internal data class PageModeScrollTarget(
    val pagerIndex: Int,
    val chapterIndex: Int,
    val pageIndex: Int,
    val anchorId: String?,
    val paragraphIndex: Int,
)

internal data class ReaderPagePosition(
    val chapterIndex: Int,
    val pageIndex: Int,
    val anchorId: String?,
    val paragraphIndex: Int,
)

internal fun resolvePageReadingPosition(entry: ContentPageEntry): ReaderPagePosition {
    val elementIndex = if (entry.elements.isEmpty()) {
        0
    } else {
        entry.anchor.elementIndex.coerceIn(0, entry.elements.lastIndex)
    }
    return ReaderPagePosition(
        chapterIndex = entry.chapterIndex,
        pageIndex = entry.pageIndex,
        anchorId = entry.elements.getOrNull(elementIndex)?.anchorId,
        paragraphIndex = elementIndex,
    )
}

internal fun resolvePageModeScrollTarget(
    entries: List<PageModeEntry>,
    chapterElements: Map<Int, List<ContentElement>>,
    chapterAnchors: Map<Int, List<PageAnchor>>,
    request: ReaderScrollRequest,
): PageModeScrollTarget? {
    val elements = chapterElements[request.chapterIndex] ?: return null
    val anchors = chapterAnchors[request.chapterIndex] ?: return null
    val resolution = resolveReaderScrollAnchor(
        elements = elements,
        pageAnchors = anchors,
        anchorId = request.anchorId,
        fallbackIndex = request.paragraphIndex,
    )
    val exactPagerIndex = request.pageIndex?.let { pageIndex ->
        findPageModeEntryIndex(
            entries = entries,
            chapterIndex = request.chapterIndex,
            pageIndex = pageIndex,
        ).takeIf { it >= 0 }
    }
    val exactEntry = exactPagerIndex?.let { entries[it] as? ContentPageEntry }
    val exactPosition = exactEntry?.let(::resolvePageReadingPosition)
    val exactPageMatchesAnchor = when {
        exactPosition == null -> false
        request.anchorId != null -> exactPosition.anchorId == request.anchorId
        else -> exactPosition.paragraphIndex == request.paragraphIndex
    }
    if (exactPagerIndex != null && exactPosition != null && exactPageMatchesAnchor) {
        return PageModeScrollTarget(
            pagerIndex = exactPagerIndex,
            chapterIndex = exactPosition.chapterIndex,
            pageIndex = exactPosition.pageIndex,
            anchorId = exactPosition.anchorId,
            paragraphIndex = exactPosition.paragraphIndex,
        )
    }
    val pagerIndex = findPageModeEntryIndex(
        entries = entries,
        chapterIndex = request.chapterIndex,
        pageIndex = resolution.anchorIndex,
    )
    if (pagerIndex < 0) return null
    return PageModeScrollTarget(
        pagerIndex = pagerIndex,
        chapterIndex = request.chapterIndex,
        pageIndex = resolution.anchorIndex,
        anchorId = resolution.anchorId,
        paragraphIndex = resolution.targetElementIndex,
    )
}

internal fun pageModeChapterDirection(
    fromChapterIndex: Int,
    toChapterIndex: Int
): Int {
    return when {
        toChapterIndex > fromChapterIndex -> 1
        toChapterIndex < fromChapterIndex -> -1
        else -> 0
    }
}
