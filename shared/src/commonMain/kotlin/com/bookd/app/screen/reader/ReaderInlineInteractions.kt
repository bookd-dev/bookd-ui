package com.bookd.app.screen.reader

import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookManifestDocument
import com.bookd.app.data.model.ContentElement

data class ReaderImagePreview(
    val url: String,
    val alt: String?,
)

data class ReaderImagePreviewTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

data class ReaderParagraphSelection(
    val chapterIndex: Int,
    val anchorId: String?,
    val paragraphIndex: Int,
    val scrollOffset: Int,
)

data class ReaderParagraphBookmarkRequest(
    val chapterIndex: Int,
    val anchorId: String?,
    val paragraphIndex: Int,
    val scrollOffset: Int,
    val note: String?,
)

data class ReaderInternalLinkTarget(
    val chapterIndex: Int,
    val anchorId: String?,
)

internal sealed interface ReaderExternalLinkResult {
    data object Opened : ReaderExternalLinkResult
    data class Fallback(val url: String) : ReaderExternalLinkResult
}

internal fun resolveReaderFootnote(
    elements: List<ContentElement>,
    footnoteId: String,
): ContentElement.Footnote? {
    return elements.filterIsInstance<ContentElement.Footnote>()
        .firstOrNull { it.footnoteId == footnoteId }
}

internal fun buildReaderParagraphBookmarkRequest(
    selection: ReaderParagraphSelection,
    note: String? = null,
): ReaderParagraphBookmarkRequest {
    return ReaderParagraphBookmarkRequest(
        chapterIndex = selection.chapterIndex,
        anchorId = selection.anchorId,
        paragraphIndex = selection.paragraphIndex,
        scrollOffset = selection.scrollOffset,
        note = note,
    )
}

internal fun openReaderExternalLink(
    url: String,
    openUri: (String) -> Unit,
): ReaderExternalLinkResult {
    return runCatching {
        openUri(url)
    }.fold(
        onSuccess = { ReaderExternalLinkResult.Opened },
        onFailure = { ReaderExternalLinkResult.Fallback(url) },
    )
}

internal fun isReaderExternalLink(url: String): Boolean {
    val trimmed = url.trim()
    return trimmed.startsWith("//") || readerUriScheme(trimmed) != null
}

internal fun isReaderInternalDocumentLink(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isBlank() || isReaderExternalLink(trimmed)) return false
    val path = trimmed.substringBefore('#').substringBefore('?')
    if (path.isBlank() && trimmed.contains('#')) return true
    return path.lowercase().let { lower ->
        lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
    }
}

internal fun resolveReaderInternalLink(
    url: String,
    manifest: BookManifest,
    currentChapterIndex: Int,
): ReaderInternalLinkTarget? {
    if (!isReaderInternalDocumentLink(url)) return null

    val trimmed = url.trim()
    val hrefPart = trimmed.substringBefore('#').substringBefore('?')
    val fragment = trimmed.substringAfter('#', missingDelimiterValue = "")
        .substringBefore('?')
        .takeIf { it.isNotBlank() }

    val currentDocument = manifest.documents.firstOrNull { it.index == currentChapterIndex }
    val targetDocument = if (hrefPart.isBlank()) {
        currentDocument
    } else {
        val resolvedHref = resolveReaderRelativeHref(hrefPart, currentDocument?.href)
        findReaderDocumentByHref(manifest.documents, resolvedHref)
    }
    val fallbackChapterIndex = if (targetDocument == null && hrefPart.isNotBlank()) {
        resolveReaderFallbackChapterIndex(hrefPart, manifest.spine)
    } else {
        null
    }
    val chapterIndex = targetDocument?.index ?: fallbackChapterIndex
        ?: if (hrefPart.isBlank()) currentChapterIndex else return null

    val anchorId = fragment
        ?.let { sourceId -> sanitizeReaderSourceId(sourceId) }
        ?.let { sanitized -> targetDocument?.anchorPrefix?.plus(sanitized) }

    return ReaderInternalLinkTarget(
        chapterIndex = chapterIndex,
        anchorId = anchorId,
    )
}

internal fun resolveReaderRelativeHref(href: String, currentHref: String?): String {
    val rawHref = href.trim()
    val isRootRelative = rawHref.startsWith("/")
    val cleanHref = rawHref.trimStart('/')
    if (cleanHref.isBlank()) return cleanHref
    if (cleanHref.startsWith("http://") || cleanHref.startsWith("https://")) return cleanHref
    val currentDir = currentHref
        ?.substringBeforeLast('/', missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }
    val rawParts = buildList {
        if (currentDir != null && !isRootRelative) {
            addAll(currentDir.split('/'))
        }
        addAll(cleanHref.split('/'))
    }
    val normalized = mutableListOf<String>()
    rawParts.forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> if (normalized.isNotEmpty()) normalized.removeAt(normalized.lastIndex)
            else -> normalized.add(part)
        }
    }
    return normalized.joinToString("/")
}

internal fun findReaderDocumentByHref(
    documents: List<BookManifestDocument>,
    href: String,
): BookManifestDocument? {
    val normalizedHref = resolveReaderRelativeHref(href, currentHref = null)
    return documents.firstOrNull { document ->
        document.href?.let { resolveReaderRelativeHref(it, currentHref = null) } == normalizedHref
    } ?: documents.firstOrNull { document ->
        document.href?.let { resolveReaderRelativeHref(it, currentHref = null) }
            ?.equals(normalizedHref, ignoreCase = true) == true
    } ?: documents.firstOrNull { document ->
        document.href?.substringAfterLast('/') == normalizedHref.substringAfterLast('/')
    } ?: documents.firstOrNull { document ->
        document.href?.substringAfterLast('/')
            ?.equals(normalizedHref.substringAfterLast('/'), ignoreCase = true) == true
    }
}

internal fun sanitizeReaderSourceId(value: String): String? {
    return value
        .trim()
        .trimStart('#')
        .lowercase()
        .replace(Regex("[^a-z0-9._:-]+"), "-")
        .trim('-')
        .takeIf { it.isNotBlank() }
}

private fun resolveReaderFallbackChapterIndex(href: String, spine: List<Int>): Int? {
    val ordinal = parseReaderChapterOrdinal(href) ?: return null
    val zeroBased = ordinal
        .takeIf { it in spine.indices }
        ?.let { spine[it] }
    if (ordinal == 0) return zeroBased
    val oneBased = (ordinal - 1)
        .takeIf { it in spine.indices }
        ?.let { spine[it] }
    return oneBased ?: zeroBased
}

private fun parseReaderChapterOrdinal(href: String): Int? {
    val fileName = href
        .substringBefore('#')
        .substringBefore('?')
        .substringAfterLast('/')
        .substringBeforeLast('.', missingDelimiterValue = "")
        .trim()
    if (fileName.isBlank()) return null

    val namedOrdinal = Regex(
        pattern = "(?i)(?:chapter|chap|ch|section|sec|part|page|body|text|file|item)[_\\-. ]*0*(\\d+)"
    ).find(fileName)
    val ordinalText = namedOrdinal?.groupValues?.getOrNull(1)
        ?: Regex("0*(\\d+)").matchEntire(fileName)?.groupValues?.getOrNull(1)
    return ordinalText?.toIntOrNull()
}

private fun readerUriScheme(url: String): String? {
    val trimmed = url.trim()
    val separator = trimmed.indexOf(':')
    if (separator <= 0) return null
    val scheme = trimmed.substring(0, separator)
    if (!scheme.matches(Regex("[A-Za-z][A-Za-z0-9+.-]*"))) return null
    return scheme.lowercase()
}

internal fun updateReaderImagePreviewTransform(
    current: ReaderImagePreviewTransform,
    zoomChange: Float,
    panX: Float,
    panY: Float,
    minScale: Float = 1f,
    maxScale: Float = 5f,
): ReaderImagePreviewTransform {
    val targetScale = (current.scale * zoomChange).coerceIn(minScale, maxScale)
    return if (targetScale <= minScale) {
        ReaderImagePreviewTransform(scale = minScale)
    } else {
        current.copy(
            scale = targetScale,
            offsetX = current.offsetX + panX,
            offsetY = current.offsetY + panY,
        )
    }
}

internal fun toggleReaderImagePreviewZoom(
    current: ReaderImagePreviewTransform,
    zoomedScale: Float = 2.5f,
): ReaderImagePreviewTransform {
    return if (current.scale > 1f) {
        ReaderImagePreviewTransform()
    } else {
        ReaderImagePreviewTransform(scale = zoomedScale)
    }
}

internal fun ContentElement.Footnote.readerDisplayText(): String {
    return contentSpans.joinToString(separator = "") { it.text }.trim()
}
