package com.bookd.app.ui.theme

import androidx.compose.ui.graphics.Color

// 简洁中性色板

// 主色调 - 纯黑白
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

// 灰度层次 - 用于层级区分
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray600 = Color(0xFF757575)
val Gray700 = Color(0xFF616161)
val Gray800 = Color(0xFF424242)
val Gray900 = Color(0xFF212121)

// Light Theme Colors
val LightPrimary = Black
val LightOnPrimary = White
val LightPrimaryContainer = Gray100
val LightOnPrimaryContainer = Black

val LightSecondary = Gray700
val LightOnSecondary = White
val LightSecondaryContainer = Gray200
val LightOnSecondaryContainer = Gray900

val LightTertiary = Gray600
val LightOnTertiary = White
val LightTertiaryContainer = Gray100
val LightOnTertiaryContainer = Gray800

val LightBackground = White
val LightOnBackground = Black
val LightSurface = White
val LightOnSurface = Black
val LightSurfaceVariant = Gray100
val LightOnSurfaceVariant = Gray700

val LightOutline = Gray400
val LightOutlineVariant = Gray200

val LightSurfaceTint = Black
val LightInverseSurface = Gray900
val LightInverseOnSurface = White
val LightInversePrimary = White
val LightScrim = Black

// Surface Container 系列 - 用于不同层级的容器
val LightSurfaceDim = Gray200
val LightSurfaceBright = White
val LightSurfaceContainerLowest = White
val LightSurfaceContainerLow = Gray50
val LightSurfaceContainer = Gray100
val LightSurfaceContainerHigh = Gray200
val LightSurfaceContainerHighest = Gray300

val LightError = Color(0xFFB00020)
val LightOnError = White
val LightErrorContainer = Color(0xFFFCD8DF)
val LightOnErrorContainer = Color(0xFF8C0018)

// Dark Theme Colors
val DarkPrimary = Color(0xFFF4F6F8)
val DarkOnPrimary = Color(0xFF111315)
val DarkPrimaryContainer = Color(0xFF30353B)
val DarkOnPrimaryContainer = Color(0xFFF4F6F8)

val DarkSecondary = Color(0xFFE0E3E6)
val DarkOnSecondary = Color(0xFF15181B)
val DarkSecondaryContainer = Color(0xFF343A40)
val DarkOnSecondaryContainer = Color(0xFFF1F3F5)

val DarkTertiary = Color(0xFFD5DAE0)
val DarkOnTertiary = Color(0xFF15181B)
val DarkTertiaryContainer = Color(0xFF2D343A)
val DarkOnTertiaryContainer = Color(0xFFE9EDF1)

val DarkBackground = Color(0xFF111315)
val DarkOnBackground = Color(0xFFF4F6F8)
val DarkSurface = Color(0xFF111315)
val DarkOnSurface = Color(0xFFF4F6F8)
val DarkSurfaceVariant = Color(0xFF252A2E)
val DarkOnSurfaceVariant = Color(0xFFE1E5EA)

val DarkOutline = Color(0xFF89919A)
val DarkOutlineVariant = Color(0xFF555D66)

val DarkSurfaceTint = DarkPrimary
val DarkInverseSurface = Color(0xFFE7EAED)
val DarkInverseOnSurface = Color(0xFF111315)
val DarkInversePrimary = Color(0xFF111315)
val DarkScrim = Black

// Surface Container 系列 - 用于不同层级的容器
val DarkSurfaceDim = Color(0xFF0F1113)
val DarkSurfaceBright = Color(0xFF30363D)
val DarkSurfaceContainerLowest = Color(0xFF111315)
val DarkSurfaceContainerLow = Color(0xFF171A1D)
val DarkSurfaceContainer = Color(0xFF1D2125)
val DarkSurfaceContainerHigh = Color(0xFF242930)
val DarkSurfaceContainerHighest = Color(0xFF2C3238)

val DarkError = Color(0xFFCF6679)
val DarkOnError = Color(0xFF111315)
val DarkErrorContainer = Color(0xFF8C0018)
val DarkOnErrorContainer = Color(0xFFFCD8DF)

// ==================== 格式标签颜色 ====================
// 书籍格式标签使用的语义颜色

/** EPUB 格式标签颜色 - 绿色 */
val FormatEpub = Color(0xFF4CAF50)

/** PDF 格式标签颜色 - 粉红色 */
val FormatPdf = Color(0xFFE91E63)

/** TXT 格式标签颜色 - 蓝色 */
val FormatTxt = Color(0xFF2196F3)

/** MOBI 格式标签颜色 - 橙色 */
val FormatMobi = Color(0xFFFF9800)

// ==================== 状态颜色 ====================
// 用于表示操作状态的语义颜色

/** 成功状态颜色 - 绿色 */
val StatusSuccess = Color(0xFF4CAF50)

/** 失败状态颜色 - 红色 */
val StatusFailed = Color(0xFFF44336)
