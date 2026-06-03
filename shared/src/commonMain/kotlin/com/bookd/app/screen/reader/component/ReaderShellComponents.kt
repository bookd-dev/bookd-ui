package com.bookd.app.screen.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
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
import app.composeapp.generated.resources.reader_settings_preferences
import app.composeapp.generated.resources.reader_settings_typography
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
import kotlin.math.pow
import kotlin.math.roundToInt

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
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.reader_settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            ReaderSettingsSection(title = stringResource(Res.string.reader_settings_typography)) {
                SettingSliderRow(
                    label = stringResource(Res.string.font_size),
                    value = settings.fontSize.toFloat(),
                    valueRange = 12f..32f,
                    step = 1f,
                    valueText = settings.fontSize.toString(),
                    onValueChange = { onFontSizeChange(it.toInt()) },
                )
                SettingSliderRow(
                    label = stringResource(Res.string.line_height),
                    value = settings.lineHeight.toFloat(),
                    valueRange = 1.0f..2.5f,
                    step = 0.1f,
                    valueText = "%.1f".format(settings.lineHeight),
                    onValueChange = { onLineHeightChange(it.toDouble()) },
                )
                SettingSliderRow(
                    label = stringResource(Res.string.paragraph_spacing),
                    value = settings.paragraphSpacing.toFloat(),
                    valueRange = 0f..32f,
                    step = 4f,
                    valueText = settings.paragraphSpacing.toString(),
                    onValueChange = { onParagraphSpacingChange(it.toInt()) },
                )
                SettingSliderRow(
                    label = stringResource(Res.string.margin_horizontal),
                    value = settings.marginHorizontal.toFloat(),
                    valueRange = 8f..56f,
                    step = 4f,
                    valueText = settings.marginHorizontal.toString(),
                    onValueChange = { onMarginHorizontalChange(it.toInt()) },
                )
                SettingSliderRow(
                    label = stringResource(Res.string.margin_vertical),
                    value = settings.marginVertical.toFloat(),
                    valueRange = 16f..96f,
                    step = 8f,
                    valueText = settings.marginVertical.toString(),
                    onValueChange = { onMarginVerticalChange(it.toInt()) },
                )
            }

            ReaderSettingsSection(title = stringResource(Res.string.reader_settings_preferences)) {
                Text(
                    text = stringResource(Res.string.page_mode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderPageModeChip(
                        selected = settings.pageMode == PageMode.SCROLL,
                        label = stringResource(Res.string.page_mode_scroll),
                        onClick = { onPageModeChange(PageMode.SCROLL) },
                        modifier = Modifier.weight(1f),
                    )
                    ReaderPageModeChip(
                        selected = settings.pageMode == PageMode.PAGE,
                        label = stringResource(Res.string.page_mode_page),
                        onClick = { onPageModeChange(PageMode.PAGE) },
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.first_line_indent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Switch(
                        checked = settings.firstLineIndent,
                        onCheckedChange = { onFirstLineIndentChange() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    val snappedValue = snapReaderSettingValue(value, valueRange, step)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        DiscreteSettingSlider(
            value = snappedValue,
            valueRange = valueRange,
            step = step,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DiscreteSettingSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val thumbRadius = with(density) { 9.dp.toPx() }
    val trackHeight = with(density) { 8.dp.toPx() }
    val tickRadius = with(density) { 2.dp.toPx() }
    val activeTrackColor = MaterialTheme.colorScheme.onSurface
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeTickColor = MaterialTheme.colorScheme.surface
    val inactiveTickColor = MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.onSurface
    val snappedValue = snapReaderSettingValue(value, valueRange, step)
    val tickCount = readerSettingTickCount(valueRange, step)
    var lastHapticValue by remember(valueRange, step) { mutableStateOf(snappedValue) }

    BoxWithConstraints(
        modifier = modifier.height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange, step, widthPx) {
                    fun updateFromX(x: Float) {
                        val rawValue = sliderXToReaderSettingValue(
                            x = x,
                            width = widthPx,
                            thumbRadius = thumbRadius,
                            valueRange = valueRange,
                        )
                        val snapped = snapReaderSettingValue(rawValue, valueRange, step)
                        if (snapped != lastHapticValue) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticValue = snapped
                        }
                        onValueChange(snapped)
                    }

                    detectTapGestures { offset ->
                        updateFromX(offset.x)
                    }
                }
                .pointerInput(valueRange, step, widthPx) {
                    fun updateFromX(x: Float) {
                        val rawValue = sliderXToReaderSettingValue(
                            x = x,
                            width = widthPx,
                            thumbRadius = thumbRadius,
                            valueRange = valueRange,
                        )
                        val snapped = snapReaderSettingValue(rawValue, valueRange, step)
                        if (snapped != lastHapticValue) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticValue = snapped
                        }
                        onValueChange(snapped)
                    }

                    detectDragGestures(
                        onDragStart = { offset -> updateFromX(offset.x) },
                        onDrag = { change, _ -> updateFromX(change.position.x) },
                    )
                },
        ) {
            val trackStart = thumbRadius
            val trackEnd = size.width - thumbRadius
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
            val centerY = size.height / 2f
            val activeEnd = trackStart + trackWidth * readerSettingValueFraction(snappedValue, valueRange)

            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
            )
            drawRoundRect(
                color = activeTrackColor,
                topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                size = Size((activeEnd - trackStart).coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
            )

            readerSettingInteriorTickIndices(tickCount).forEach { index ->
                val fraction = if (tickCount == 1) 0f else index.toFloat() / (tickCount - 1)
                val tickX = trackStart + trackWidth * fraction
                drawCircle(
                    color = if (tickX <= activeEnd + 0.5f) activeTickColor else inactiveTickColor,
                    radius = tickRadius,
                    center = Offset(tickX, centerY),
                )
            }

            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(activeEnd, centerY),
            )
        }
    }
}

internal fun snapReaderSettingValue(
    rawValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
): Float {
    if (step <= 0f) return rawValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val clamped = rawValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val stepIndex = ((clamped - valueRange.start) / step).roundToInt()
    val snapped = valueRange.start + stepIndex * step
    val precision = readerSettingStepPrecision(step)
    val factor = 10.0.pow(precision).toFloat()
    return ((snapped * factor).roundToInt() / factor).coerceIn(valueRange.start, valueRange.endInclusive)
}

internal fun readerSettingTickCount(
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
): Int {
    if (step <= 0f) return 2
    val distance = valueRange.endInclusive - valueRange.start
    return (distance / step).roundToInt().coerceAtLeast(1) + 1
}

internal fun readerSettingInteriorTickIndices(tickCount: Int): IntRange {
    if (tickCount <= 2) return IntRange.EMPTY
    return 1 until tickCount - 1
}

internal fun readerSettingValueFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val distance = valueRange.endInclusive - valueRange.start
    if (distance <= 0f) return 0f
    return ((value - valueRange.start) / distance).coerceIn(0f, 1f)
}

internal fun sliderXToReaderSettingValue(
    x: Float,
    width: Float,
    thumbRadius: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val trackStart = thumbRadius
    val trackEnd = width - thumbRadius
    val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
    val fraction = ((x - trackStart) / trackWidth).coerceIn(0f, 1f)
    return valueRange.start + (valueRange.endInclusive - valueRange.start) * fraction
}

private fun readerSettingStepPrecision(step: Float): Int {
    val text = step.toString().trimEnd('0')
    val decimalIndex = text.indexOf('.')
    return if (decimalIndex < 0) 0 else text.length - decimalIndex - 1
}

@Composable
private fun ReaderPageModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier,
    )
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
