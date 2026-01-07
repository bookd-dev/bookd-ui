package com.bookd.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.Bookshelf: ImageVector
    get() {
        if (_Bookshelf != null) {
            return _Bookshelf!!
        }
        _Bookshelf = ImageVector.Builder(
            name = "BookshelfFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(257.3f, 96f)
                horizontalLineToRelative(-2.7f)
                curveToRelative(-28.7f, 0f, -52.2f, 0f, -71.3f, 1.5f)
                curveToRelative(-19.8f, 1.7f, -37.6f, 5.1f, -54.3f, 13.6f)
                curveToRelative(-26f, 13.3f, -47.3f, 34.6f, -60.6f, 60.6f)
                curveToRelative(-8.4f, 16.6f, -11.9f, 34.6f, -13.5f, 54.3f)
                curveToRelative(-1.6f, 19.1f, -1.6f, 42.6f, -1.6f, 71.4f)
                verticalLineToRelative(429.3f)
                curveToRelative(0f, 28.8f, 0f, 52.2f, 1.6f, 71.4f)
                curveToRelative(1.6f, 19.8f, 5.1f, 37.6f, 13.5f, 54.3f)
                curveToRelative(13.3f, 26.1f, 34.6f, 47.4f, 60.6f, 60.6f)
                curveToRelative(16.6f, 8.5f, 34.6f, 11.9f, 54.3f, 13.6f)
                curveToRelative(19.1f, 1.5f, 42.6f, 1.5f, 71.3f, 1.5f)
                horizontalLineToRelative(2.7f)
                curveToRelative(28.7f, 0f, 52.2f, 0f, 71.3f, -1.5f)
                curveToRelative(19.8f, -1.7f, 37.6f, -5.1f, 54.3f, -13.6f)
                curveToRelative(26f, -13.3f, 47.3f, -34.6f, 60.6f, -60.6f)
                curveToRelative(8.4f, -16.6f, 11.9f, -34.6f, 13.5f, -54.3f)
                curveToRelative(1.6f, -19.1f, 1.6f, -42.6f, 1.6f, -71.4f)
                lineTo(458.7f, 365.2f)
                lineToRelative(107.4f, 402.4f)
                curveToRelative(7.2f, 27.3f, 13.2f, 49.7f, 19.5f, 67.4f)
                curveToRelative(6.5f, 18.4f, 14.3f, 34.6f, 26.7f, 48.3f)
                curveToRelative(19.3f, 21.4f, 44.8f, 36.3f, 73.1f, 42.3f)
                curveToRelative(18f, 3.8f, 35.9f, 2.6f, 55f, -0.9f)
                curveToRelative(18.6f, -3.4f, 40.8f, -9.4f, 67.9f, -16.7f)
                lineToRelative(2.6f, -0.7f)
                arcToRelative(926.1f, 926.1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 67.1f, -19.6f)
                curveToRelative(18.3f, -6.6f, 34.4f, -14.4f, 48.1f, -26.9f)
                arcToRelative(137f, 137f, 0f, isMoreThanHalf = false, isPositiveArc = false, 42.1f, -73.2f)
                curveToRelative(3.8f, -18f, 2.6f, -36f, -0.9f, -55.2f)
                curveToRelative(-3.4f, -18.5f, -9.3f, -40.8f, -16.6f, -67.8f)
                lineToRelative(-108.9f, -408.2f)
                arcToRelative(934.7f, 934.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -19.5f, -67.4f)
                curveToRelative(-6.5f, -18.4f, -14.3f, -34.6f, -26.7f, -48.3f)
                arcToRelative(136.4f, 136.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -73.1f, -42.3f)
                curveToRelative(-18f, -3.8f, -35.9f, -2.6f, -55f, 0.9f)
                curveToRelative(-18.6f, 3.4f, -40.8f, 9.4f, -68f, 16.7f)
                lineToRelative(-2.6f, 0.6f)
                curveToRelative(-27.1f, 7.4f, -49.4f, 13.4f, -67.1f, 19.7f)
                curveToRelative(-18.3f, 6.6f, -34.4f, 14.5f, -48.1f, 26.9f)
                curveToRelative(-11.5f, 10.4f, -21.1f, 22.7f, -28.5f, 36.2f)
                arcToRelative(115.6f, 115.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, -9.7f, -27.6f)
                arcToRelative(138.7f, 138.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -60.6f, -60.6f)
                curveToRelative(-16.6f, -8.5f, -34.6f, -11.9f, -54.3f, -13.6f)
                arcTo(948.4f, 948.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 257.3f, 96f)
                close()
                moveTo(256f, 682.7f)
                horizontalLineToRelative(0.4f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, 85.3f)
                lineTo(256f, 768f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -85.3f)
                close()
                moveTo(394.7f, 287.8f)
                verticalLineToRelative(21.5f)
                curveToRelative(0f, 11.8f, -9.6f, 21.4f, -21.4f, 21.4f)
                lineTo(138.7f, 330.7f)
                arcToRelative(21.3f, 21.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -21.4f, -21.4f)
                verticalLineToRelative(-21.5f)
                arcToRelative(21.1f, 21.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21.2f, -21.1f)
                horizontalLineToRelative(234.9f)
                arcToRelative(21.1f, 21.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21.2f, 21.1f)
                close()
                moveTo(747.6f, 671.2f)
                lineToRelative(0.4f, -0.1f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 22.1f, 82.4f)
                lineToRelative(-0.3f, 0.1f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -22.1f, -82.4f)
                close()
                moveTo(780.5f, 274.2f)
                arcToRelative(17.9f, 17.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, -12.1f, 21.8f)
                lineTo(547.5f, 363.9f)
                arcToRelative(21.3f, 21.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -26.9f, -14.9f)
                lineToRelative(-1.2f, -4.5f)
                lineToRelative(-4.4f, -16.4f)
                arcToRelative(21.1f, 21.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14.3f, -25.5f)
                lineToRelative(218.2f, -67.1f)
                arcToRelative(20.9f, 20.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 26.4f, 14.4f)
                lineToRelative(6.6f, 24.3f)
                close()
            }
        }.build()

        return _Bookshelf!!
    }

@Suppress("ObjectPropertyName")
private var _Bookshelf: ImageVector? = null
