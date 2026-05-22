package com.bookd.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.Settings: ImageVector
    get() {
        if (_Settings != null) {
            return _Settings!!
        }
        _Settings = ImageVector.Builder(
            name = "SettingsFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(919.3f, 225.9f)
                lineTo(552f, 10.8f)
                arcToRelative(79.9f, 79.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -80f, 0f)
                lineTo(104.7f, 225.9f)
                arcTo(81.4f, 81.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 64.5f, 296.2f)
                verticalLineToRelative(430.6f)
                arcToRelative(81.4f, 81.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 40.2f, 70.3f)
                lineToRelative(367.3f, 215.2f)
                arcToRelative(80.2f, 80.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 80f, 0f)
                lineToRelative(367.3f, -215.2f)
                arcToRelative(81.4f, 81.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 40.2f, -70.3f)
                verticalLineTo(296.2f)
                arcToRelative(81.8f, 81.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -40.2f, -70.3f)
                close()
                moveTo(512f, 678.9f)
                arcToRelative(167.4f, 167.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 165.1f, -167.6f)
                arcTo(166.2f, 166.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 512f, 678.9f)
                close()
            }
        }.build()

        return _Settings!!
    }

@Suppress("ObjectPropertyName")
private var _Settings: ImageVector? = null
