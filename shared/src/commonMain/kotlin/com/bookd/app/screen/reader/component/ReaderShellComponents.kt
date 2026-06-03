package com.bookd.app.screen.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.back
import app.composeapp.generated.resources.cancel
import app.composeapp.generated.resources.close
import app.composeapp.generated.resources.first_line_indent
import app.composeapp.generated.resources.font_size
import app.composeapp.generated.resources.line_height
import app.composeapp.generated.resources.load_failed
import app.composeapp.generated.resources.loading
import app.composeapp.generated.resources.margin_horizontal
import app.composeapp.generated.resources.margin_vertical
import app.composeapp.generated.resources.next_chapter
import app.composeapp.generated.resources.page_mode
import app.composeapp.generated.resources.page_mode_page
import app.composeapp.generated.resources.page_mode_scroll
import app.composeapp.generated.resources.paragraph_spacing
import app.composeapp.generated.resources.previous_chapter
import app.composeapp.generated.resources.progress_conflict_message
import app.composeapp.generated.resources.progress_conflict_title
import app.composeapp.generated.resources.reader_last_read_at
import app.composeapp.generated.resources.reader_local_progress
import app.composeapp.generated.resources.reader_remote_progress
import app.composeapp.generated.resources.reader_retry
import app.composeapp.generated.resources.reader_settings
import app.composeapp.generated.resources.reader_toc_empty
import app.composeapp.generated.resources.remote_progress_info
import app.composeapp.generated.resources.local_progress_info
import app.composeapp.generated.resources.toc
import app.composeapp.generated.resources.toc_chapter_fallback
import app.composeapp.generated.resources.toc_current_reading
import app.composeapp.generated.resources.use_local_progress
import app.composeapp.generated.resources.use_remote_progress
import app.composeapp.generated.resources.view_book_detail
import com.bookd.app.basic.extension.format
import com.bookd.app.data.model.LocalReadingProgress
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.TocItem
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReaderLoadingSurface(
    message: String = stringResource(Res.string.loading),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ReaderErrorSurface(
    message: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.load_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) {
                    Text(stringResource(Res.string.back))
                }
                TextButton(onClick = onRetry) {
                    Text(stringResource(Res.string.reader_retry))
                }
            }
        }
    }
}

@Composable
fun ReaderTopChrome(
    title: String,
    subtitle: String?,
    isLoadingChapter: Boolean,
    onBack: () -> Unit,
    onBookDetail: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = onBookDetail) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(Res.string.view_book_detail))
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.reader_settings))
                }
            }
            if (isLoadingChapter) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ReaderBottomMenu(
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onTocClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPreviousChapter, enabled = hasPreviousChapter) {
                    Text(stringResource(Res.string.previous_chapter))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onTocClick) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.toc))
                    }
                    TextButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.reader_settings))
                    }
                }
                TextButton(onClick = onNextChapter, enabled = hasNextChapter) {
                    Text(stringResource(Res.string.next_chapter))
                }
            }
        }
    }
}

@Composable
fun ReaderStatusBar(
    progressText: String,
    currentTime: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Double) -> Unit,
    onParagraphSpacingChange: (Int) -> Unit,
    onMarginHorizontalChange: (Int) -> Unit,
    onMarginVerticalChange: (Int) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onFirstLineIndentChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.reader_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            SettingSliderRow(
                label = stringResource(Res.string.font_size),
                value = settings.fontSize.toFloat(),
                valueRange = 12f..32f,
                valueText = settings.fontSize.toString(),
                onValueChange = { onFontSizeChange(it.toInt()) },
            )
            SettingSliderRow(
                label = stringResource(Res.string.line_height),
                value = settings.lineHeight.toFloat(),
                valueRange = 1.0f..2.5f,
                valueText = "%.1f".format(settings.lineHeight),
                onValueChange = { onLineHeightChange(it.toDouble()) },
            )
            SettingSliderRow(
                label = stringResource(Res.string.paragraph_spacing),
                value = settings.paragraphSpacing.toFloat(),
                valueRange = 0f..32f,
                valueText = settings.paragraphSpacing.toString(),
                onValueChange = { onParagraphSpacingChange(it.toInt()) },
            )
            SettingSliderRow(
                label = stringResource(Res.string.margin_horizontal),
                value = settings.marginHorizontal.toFloat(),
                valueRange = 8f..56f,
                valueText = settings.marginHorizontal.toString(),
                onValueChange = { onMarginHorizontalChange(it.toInt()) },
            )
            SettingSliderRow(
                label = stringResource(Res.string.margin_vertical),
                value = settings.marginVertical.toFloat(),
                valueRange = 16f..96f,
                valueText = settings.marginVertical.toString(),
                onValueChange = { onMarginVerticalChange(it.toInt()) },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = stringResource(Res.string.page_mode),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.pageMode == PageMode.SCROLL,
                    onClick = { onPageModeChange(PageMode.SCROLL) },
                    label = { Text(stringResource(Res.string.page_mode_scroll)) },
                )
                FilterChip(
                    selected = settings.pageMode == PageMode.PAGE,
                    onClick = { onPageModeChange(PageMode.PAGE) },
                    label = { Text(stringResource(Res.string.page_mode_page)) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.first_line_indent), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.firstLineIndent,
                    onCheckedChange = { onFirstLineIndentChange() },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTocSheet(
    tocItems: List<TocItem>,
    currentChapterIndex: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.toc),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
                }
            }
            if (tocItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.reader_toc_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    items(flattenToc(tocItems)) { item ->
                        val selected = item.index == currentChapterIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .padding(
                                    start = (item.level * 16 + 4).dp,
                                    end = 4.dp,
                                    top = 10.dp,
                                    bottom = 10.dp,
                                ),
                        ) {
                            Text(
                                text = item.title.ifBlank {
                                    stringResource(Res.string.toc_chapter_fallback, item.index + 1)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            if (selected) {
                                Text(
                                    text = stringResource(Res.string.toc_current_reading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReaderProgressConflictDialog(
    localProgress: LocalReadingProgress?,
    remoteProgress: ReadingProgressResponse?,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.progress_conflict_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.progress_conflict_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                localProgress?.let {
                    ProgressInfoBlock(
                        label = stringResource(Res.string.reader_local_progress),
                        progressText = stringResource(
                            Res.string.local_progress_info,
                            it.chapterIndex + 1,
                            (it.progress * 100).toInt(),
                        ),
                        lastReadAt = formatTimestamp(it.lastReadAt),
                    )
                }
                remoteProgress?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressInfoBlock(
                        label = stringResource(Res.string.reader_remote_progress),
                        progressText = stringResource(
                            Res.string.remote_progress_info,
                            it.currentPage + 1,
                            (it.progress * 100).toInt(),
                        ),
                        lastReadAt = it.lastReadAt,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onUseLocal, enabled = localProgress != null) {
                    Text(stringResource(Res.string.use_local_progress))
                }
                TextButton(onClick = onUseRemote, enabled = remoteProgress != null) {
                    Text(stringResource(Res.string.use_remote_progress))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun ProgressInfoBlock(
    label: String,
    progressText: String,
    lastReadAt: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(progressText, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(Res.string.reader_last_read_at, lastReadAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun flattenToc(items: List<TocItem>): List<TocItem> =
    items.flatMap { item -> listOf(item) + flattenToc(item.children) }

private fun formatTimestamp(timestamp: Long): String {
    val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${(local.month.ordinal + 1).toString().padStart(2, '0')}-${local.day.toString().padStart(2, '0')} " +
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
