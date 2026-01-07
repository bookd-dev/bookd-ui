package com.bookd.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Outlined.Settings: ImageVector
    get() {
        if (_Settings != null) {
            return _Settings!!
        }
        _Settings = ImageVector.Builder(
            name = "SettingsOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color(0xFF707070))) {
                moveTo(512f, 0f)
                lineToRelative(465.5f, 256f)
                verticalLineToRelative(512f)
                lineTo(512f, 1024f)
                lineTo(46.5f, 768f)
                verticalLineToRelative(-512f)
                lineTo(512f, 0f)
                close()
                moveTo(512f, 107.6f)
                lineTo(144.5f, 309.7f)
                verticalLineToRelative(404.7f)
                lineTo(512f, 916.4f)
                lineToRelative(367.5f, -202.1f)
                lineTo(879.5f, 309.7f)
                lineTo(512f, 107.6f)
                close()
                moveTo(512f, 698.2f)
                curveToRelative(-108.2f, 0f, -196f, -83.3f, -196f, -186.2f)
                reflectiveCurveTo(403.7f, 325.8f, 512f, 325.8f)
                reflectiveCurveToRelative(196f, 83.3f, 196f, 186.2f)
                reflectiveCurveToRelative(-87.7f, 186.2f, -196f, 186.2f)
                close()
                moveTo(512f, 605.1f)
                curveToRelative(35f, 0f, 67.4f, -17.7f, 84.9f, -46.5f)
                arcToRelative(89.1f, 89.1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -93.1f)
                arcTo(99f, 99f, 0f, isMoreThanHalf = false, isPositiveArc = false, 512f, 418.9f)
                curveToRelative(-54.1f, 0f, -98f, 41.7f, -98f, 93.1f)
                reflectiveCurveToRelative(43.8f, 93.1f, 98f, 93.1f)
                close()
            }
        }.build()

        return _Settings!!
    }

@Suppress("ObjectPropertyName")
private var _Settings: ImageVector? = null
