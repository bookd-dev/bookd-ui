package com.bookd.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Outlined.Bookshelf: ImageVector
    get() {
        if (_Bookshelf != null) {
            return _Bookshelf!!
        }
        _Bookshelf = ImageVector.Builder(
            name = "BookshelfOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color(0xFF707070))) {
                moveTo(256f, 682.6f)
                arcTo(42.7f, 42.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 256f, 768f)
                horizontalLineToRelative(0.4f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, -85.4f)
                horizontalLineTo(256f)
                close()
                moveTo(717.4f, 723.4f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 30.1f, -52.3f)
                lineToRelative(0.4f, -0.1f)
                arcToRelative(42.7f, 42.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22.1f, 82.4f)
                lineToRelative(-0.3f, 0.1f)
                arcToRelative(42.6f, 42.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, -52.3f, -30.1f)
                close()
            }
            path(fill = SolidColor(Color(0xFF707070))) {
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
                lineToRelative(107.3f, 402.4f)
                curveToRelative(7.3f, 27.3f, 13.2f, 49.7f, 19.6f, 67.4f)
                curveToRelative(6.5f, 18.4f, 14.3f, 34.6f, 26.7f, 48.3f)
                curveToRelative(19.3f, 21.4f, 44.8f, 36.3f, 73.1f, 42.3f)
                curveToRelative(18f, 3.8f, 35.9f, 2.6f, 55f, -0.9f)
                curveToRelative(18.6f, -3.4f, 40.8f, -9.4f, 67.9f, -16.7f)
                lineToRelative(2.6f, -0.7f)
                curveToRelative(27.2f, -7.3f, 49.5f, -13.3f, 67.2f, -19.6f)
                curveToRelative(18.3f, -6.6f, 34.4f, -14.4f, 48.1f, -26.9f)
                arcToRelative(137f, 137f, 0f, isMoreThanHalf = false, isPositiveArc = false, 42.1f, -73.2f)
                curveToRelative(3.8f, -18f, 2.6f, -36f, -1f, -55.2f)
                curveToRelative(-3.3f, -18.6f, -9.3f, -40.9f, -16.6f, -68.2f)
                lineToRelative(-108.8f, -407.9f)
                arcToRelative(938.5f, 938.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, -19.5f, -67.4f)
                curveToRelative(-6.6f, -18.4f, -14.4f, -34.6f, -26.7f, -48.3f)
                arcToRelative(136.3f, 136.3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -73.1f, -42.3f)
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
                moveTo(117.3f, 725.3f)
                lineTo(117.3f, 330.7f)
                horizontalLineToRelative(277.4f)
                verticalLineToRelative(394.6f)
                curveToRelative(0f, 30.4f, 0f, 51.3f, -1.3f, 67.5f)
                curveToRelative(-1.3f, 15.8f, -3.7f, 24.3f, -6.8f, 30.5f)
                curveToRelative(-7.2f, 14.1f, -18.6f, 25.5f, -32.6f, 32.6f)
                curveToRelative(-6.1f, 3.1f, -14.7f, 5.5f, -30.5f, 6.8f)
                curveToRelative(-16.2f, 1.3f, -37.1f, 1.3f, -67.5f, 1.3f)
                curveToRelative(-30.4f, 0f, -51.3f, 0f, -67.5f, -1.3f)
                curveToRelative(-15.8f, -1.3f, -24.3f, -3.6f, -30.5f, -6.8f)
                arcToRelative(74.6f, 74.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, -32.6f, -32.6f)
                curveToRelative(-3.1f, -6.1f, -5.4f, -14.7f, -6.8f, -30.5f)
                arcToRelative(922.2f, 922.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.3f, -67.5f)
                close()
                moveTo(118.7f, 231.2f)
                curveToRelative(1.3f, -15.8f, 3.7f, -24.3f, 6.8f, -30.5f)
                curveToRelative(7.2f, -14.1f, 18.6f, -25.5f, 32.6f, -32.6f)
                curveToRelative(6.1f, -3.1f, 14.7f, -5.5f, 30.5f, -6.8f)
                arcTo(921.6f, 921.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 256f, 160f)
                curveToRelative(30.4f, 0f, 51.3f, 0f, 67.5f, 1.3f)
                curveToRelative(15.8f, 1.3f, 24.3f, 3.6f, 30.5f, 6.8f)
                curveToRelative(14.1f, 7.2f, 25.5f, 18.6f, 32.6f, 32.6f)
                curveToRelative(3.1f, 6.1f, 5.4f, 14.7f, 6.8f, 30.5f)
                arcToRelative(505.6f, 505.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.3f, 35.5f)
                lineTo(117.4f, 266.7f)
                curveToRelative(0.1f, -14.3f, 0.4f, -25.7f, 1.3f, -35.5f)
                close()
                moveTo(615f, 178.1f)
                curveToRelative(28.8f, -7.7f, 48.5f, -13f, 64.1f, -15.9f)
                curveToRelative(15.2f, -2.8f, 23.7f, -2.7f, 30.2f, -1.3f)
                curveToRelative(15f, 3.2f, 28.5f, 11f, 38.8f, 22.5f)
                curveToRelative(4.5f, 5f, 8.8f, 12.4f, 14.1f, 27f)
                curveToRelative(1.9f, 5.5f, 3.8f, 11.6f, 6f, 18.6f)
                lineTo(510f, 308.5f)
                arcToRelative(442.2f, 442.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -6.4f, -28.4f)
                curveToRelative(-2.8f, -15.3f, -2.7f, -23.9f, -1.3f, -30.5f)
                curveToRelative(3.2f, -15.1f, 11.1f, -28.8f, 22.4f, -39f)
                curveToRelative(5f, -4.5f, 12.3f, -8.8f, 26.9f, -14.1f)
                curveToRelative(14.9f, -5.4f, 34.6f, -10.7f, 63.4f, -18.4f)
                close()
                moveTo(526.3f, 370.4f)
                lineToRelative(258.6f, -79.6f)
                lineToRelative(103.7f, 388.7f)
                curveToRelative(7.7f, 28.8f, 13f, 48.6f, 15.8f, 64.4f)
                curveToRelative(2.8f, 15.3f, 2.7f, 23.9f, 1.3f, 30.5f)
                curveToRelative(-3.2f, 15.1f, -11f, 28.8f, -22.4f, 39f)
                curveToRelative(-4.9f, 4.5f, -12.3f, 8.8f, -26.9f, 14.1f)
                curveToRelative(-14.9f, 5.4f, -34.6f, 10.7f, -63.4f, 18.4f)
                curveToRelative(-28.8f, 7.7f, -48.5f, 13f, -64.1f, 15.9f)
                curveToRelative(-15.2f, 2.8f, -23.7f, 2.7f, -30.2f, 1.3f)
                arcToRelative(72.3f, 72.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -38.7f, -22.5f)
                curveToRelative(-4.5f, -5f, -8.8f, -12.4f, -14.1f, -27f)
                arcToRelative(909.6f, 909.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, -18.4f, -63.7f)
                lineTo(526.3f, 370.4f)
                close()
            }
        }.build()

        return _Bookshelf!!
    }

@Suppress("ObjectPropertyName")
private var _Bookshelf: ImageVector? = null
