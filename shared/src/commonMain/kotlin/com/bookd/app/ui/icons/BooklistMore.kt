package com.bookd.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.BooklistMore: ImageVector
    get() {
        if (_BooklistMore != null) {
            return _BooklistMore!!
        }
        _BooklistMore = ImageVector.Builder(
            name = "BooklistMore",
            defaultWidth = 200.dp,
            defaultHeight = 200.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(66.5f, 211.8f)
                horizontalLineToRelative(891f)
                curveToRelative(28.2f, 0f, 51f, -22.2f, 51f, -49.6f)
                curveToRelative(0f, -27.4f, -22.8f, -49.6f, -51f, -49.6f)
                lineTo(66.5f, 112.5f)
                curveTo(38.3f, 112.5f, 15.5f, 134.7f, 15.5f, 162.1f)
                reflectiveCurveToRelative(22.8f, 49.6f, 51f, 49.6f)
                close()
                moveTo(957.5f, 460f)
                lineTo(66.5f, 460f)
                curveTo(38.3f, 460f, 15.5f, 482.3f, 15.5f, 509.7f)
                reflectiveCurveToRelative(22.8f, 49.6f, 51f, 49.6f)
                horizontalLineToRelative(891f)
                curveToRelative(28.2f, 0f, 51f, -22.2f, 51f, -49.6f)
                curveToRelative(-0f, -27.4f, -22.9f, -49.6f, -51f, -49.6f)
                close()
                moveTo(957.5f, 811.7f)
                lineTo(66.5f, 811.7f)
                curveToRelative(-28.1f, 0f, -51f, 22.2f, -51f, 49.6f)
                reflectiveCurveToRelative(22.8f, 49.6f, 51f, 49.6f)
                horizontalLineToRelative(891f)
                curveToRelative(28.2f, 0f, 51f, -22.2f, 51f, -49.6f)
                curveToRelative(-0f, -27.4f, -22.8f, -49.6f, -51f, -49.6f)
                close()
                moveTo(957.5f, 811.7f)
            }
        }.build()

        return _BooklistMore!!
    }

@Suppress("ObjectPropertyName")
private var _BooklistMore: ImageVector? = null
