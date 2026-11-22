package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight, tree-shaken Material Rounded / Outlined icons defined directly as ImageVectors
 * so we do not bundle the ~15 MB material-icons-extended library into the APK.
 */
object SlateIcons {

  val ArrowUpward: ImageVector by lazy {
    vectorIcon("ArrowUpward") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(13f, 19f)
        verticalLineTo(7.83f)
        lineToRelative(4.88f, 4.88f)
        curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.42f, 0f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
        lineToRelative(-6.59f, -6.59f)
        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
        lineToRelative(-6.6f, 6.58f)
        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
        lineTo(11f, 7.83f)
        verticalLineTo(19f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
        close()
      }
    }
  }

  val KeyboardArrowLeft: ImageVector by lazy {
    vectorIcon("KeyboardArrowLeft", autoMirror = true) {
      path(fill = SolidColor(Color.Black)) {
        moveTo(14.71f, 15.88f)
        lineTo(10.83f, 12f)
        lineToRelative(3.88f, -3.88f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
        lineTo(8.71f, 11.3f)
        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
        lineToRelative(4.59f, 4.59f)
        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
        curveToRelative(0.38f, -0.39f, 0.39f, -1.03f, 0f, -1.42f)
        close()
      }
    }
  }

  val KeyboardArrowRight: ImageVector by lazy {
    vectorIcon("KeyboardArrowRight", autoMirror = true) {
      path(fill = SolidColor(Color.Black)) {
        moveTo(9.29f, 15.88f)
        lineTo(13.17f, 12f)
        lineTo(9.29f, 8.12f)
        curveToRelative(-0.39f, -0.39f, -0.39f, -1.02f, 0f, -1.41f)
        curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0f)
        lineToRelative(4.59f, 4.59f)
        curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0f, 1.41f)
        lineToRelative(-4.59f, 4.59f)
        curveToRelative(-0.39f, 0.39f, -1.02f, 0.39f, -1.41f, 0f)
        curveToRelative(-0.38f, -0.39f, -0.39f, -1.03f, 0f, -1.42f)
        close()
      }
    }
  }

  val DeleteOutline: ImageVector by lazy {
    vectorIcon("DeleteOutline") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(9f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineTo(8f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(10f)
        close()
        moveTo(9f, 9f)
        horizontalLineToRelative(6f)
        curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
        verticalLineToRelative(8f)
        curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
        horizontalLineTo(9f)
        curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
        verticalLineToRelative(-8f)
        curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
        close()
        moveTo(15.5f, 4f)
        lineToRelative(-0.71f, -0.71f)
        curveToRelative(-0.18f, -0.18f, -0.44f, -0.29f, -0.7f, -0.29f)
        horizontalLineTo(9.91f)
        curveToRelative(-0.26f, 0f, -0.52f, 0.11f, -0.7f, 0.29f)
        lineTo(8.5f, 4f)
        horizontalLineTo(6f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(12f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
        horizontalLineToRelative(-2.5f)
        close()
      }
    }
  }

  val CheckCircle: ImageVector by lazy {
    vectorIcon("CheckCircle") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f)
        close()
        moveTo(9.29f, 16.29f)
        lineTo(5.7f, 12.7f)
        curveToRelative(-0.39f, -0.39f, -0.39f, -1.02f, 0f, -1.41f)
        curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0f)
        lineTo(10f, 14.17f)
        lineToRelative(6.88f, -6.88f)
        curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0f)
        curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0f, 1.41f)
        lineToRelative(-7.59f, 7.59f)
        curveToRelative(-0.38f, 0.39f, -1.02f, 0.39f, -1.41f, 0f)
        close()
      }
    }
  }

  val RadioButtonUnchecked: ImageVector by lazy {
    vectorIcon("RadioButtonUnchecked") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
        reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
        reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
        reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
        close()
      }
    }
  }

  val StarOutline: ImageVector by lazy {
    vectorIcon("StarOutline") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19.65f, 9.04f)
        lineToRelative(-4.84f, -0.42f)
        lineToRelative(-1.89f, -4.45f)
        curveToRelative(-0.34f, -0.81f, -1.5f, -0.81f, -1.84f, 0f)
        lineTo(9.19f, 8.63f)
        lineToRelative(-4.83f, 0.41f)
        curveToRelative(-0.88f, 0.07f, -1.24f, 1.17f, -0.57f, 1.75f)
        lineToRelative(3.67f, 3.18f)
        lineToRelative(-1.1f, 4.72f)
        curveToRelative(-0.2f, 0.86f, 0.73f, 1.54f, 1.49f, 1.08f)
        lineToRelative(4.15f, -2.5f)
        lineToRelative(4.15f, 2.51f)
        curveToRelative(0.76f, 0.46f, 1.69f, -0.22f, 1.49f, -1.08f)
        lineToRelative(-1.1f, -4.73f)
        lineToRelative(3.67f, -3.18f)
        curveToRelative(0.67f, -0.58f, 0.32f, -1.68f, -0.56f, -1.75f)
        close()
        moveTo(12f, 15.4f)
        lineToRelative(-3.76f, 2.27f)
        lineToRelative(1f, -4.28f)
        lineToRelative(-3.32f, -2.88f)
        lineToRelative(4.38f, -0.38f)
        lineTo(12f, 6.1f)
        lineToRelative(1.71f, 4.04f)
        lineToRelative(4.38f, 0.38f)
        lineToRelative(-3.32f, 2.88f)
        lineToRelative(1f, 4.28f)
        lineTo(12f, 15.4f)
        close()
      }
    }
  }

  val Schedule: ImageVector by lazy {
    vectorIcon("Schedule") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(11.99f, 2f)
        curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
        reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
        reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
        reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
        close()
        moveTo(12.5f, 7f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        lineToRelative(5.25f, 3.15f)
        lineToRelative(0.75f, -1.23f)
        lineToRelative(-4.5f, -2.67f)
        close()
      }
    }
  }

  val Event: ImageVector by lazy {
    vectorIcon("Event") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 4f)
        horizontalLineToRelative(-1f)
        verticalLineTo(3f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        reflectiveCurveToRelative(-1f, 0.45f, -1f, 1f)
        verticalLineToRelative(1f)
        horizontalLineTo(8f)
        verticalLineTo(3f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        reflectiveCurveToRelative(-1f, 0.45f, -1f, 1f)
        verticalLineToRelative(1f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(3f, 20f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 19f)
        curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
        horizontalLineTo(6f)
        curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
        verticalLineTo(9f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(10f)
        close()
        moveTo(16f, 12f)
        horizontalLineToRelative(-3f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        verticalLineToRelative(3f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(3f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        verticalLineToRelative(-3f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        close()
      }
    }
  }

  val EventAvailable: ImageVector by lazy {
    vectorIcon("EventAvailable") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(16.53f, 11.06f)
        lineTo(15.47f, 10f)
        lineToRelative(-4.88f, 4.88f)
        lineToRelative(-2.12f, -2.12f)
        lineToRelative(-1.06f, 1.06f)
        lineTo(10.59f, 17f)
        lineToRelative(5.94f, -5.94f)
        close()
        moveTo(19f, 3f)
        horizontalLineToRelative(-1f)
        verticalLineTo(1f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(2f)
        horizontalLineTo(8f)
        verticalLineTo(1f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(3f, 19f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(8f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(11f)
        close()
      }
    }
  }

  val EventNote: ImageVector by lazy {
    vectorIcon("EventNote") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 3f)
        horizontalLineToRelative(-1f)
        verticalLineTo(1f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(2f)
        horizontalLineTo(8f)
        verticalLineTo(1f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(8f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(11f)
        close()
        moveTo(7f, 10f)
        horizontalLineToRelative(10f)
        verticalLineToRelative(2f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        close()
        moveTo(7f, 14f)
        horizontalLineToRelative(7f)
        verticalLineToRelative(2f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        close()
      }
    }
  }

  val CalendarMonthRounded: ImageVector by lazy {
    vectorIcon("CalendarMonthRounded") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 4f)
        horizontalLineToRelative(-1f)
        verticalLineTo(2f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(2f)
        horizontalLineTo(8f)
        verticalLineTo(2f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(3f, 20f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 20f)
        horizontalLineTo(5f)
        verticalLineTo(10f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(10f)
        close()
        moveTo(9f, 14f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(13f, 14f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(17f, 14f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(9f, 18f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(13f, 18f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(17f, 18f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
      }
    }
  }

  val CalendarMonthOutlined: ImageVector by lazy {
    vectorIcon("CalendarMonthOutlined") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 4f)
        horizontalLineToRelative(-1f)
        verticalLineTo(2f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(2f)
        horizontalLineTo(8f)
        verticalLineTo(2f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(3f, 20f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 20f)
        horizontalLineTo(5f)
        verticalLineTo(10f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(10f)
        close()
        moveTo(19f, 8f)
        horizontalLineTo(5f)
        verticalLineTo(6f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(2f)
        close()
        moveTo(9f, 14f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(13f, 14f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(17f, 14f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(9f, 18f)
        horizontalLineTo(7f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(13f, 18f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
        moveTo(17f, 18f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        close()
      }
    }
  }

  val DragIndicator: ImageVector by lazy {
    vectorIcon("DragIndicator") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(11f, 18f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
        reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
        reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
        close()
        moveTo(9f, 10f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
        moveTo(9f, 4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
        moveTo(15f, 8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        close()
        moveTo(15f, 10f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
        moveTo(15f, 16f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
        reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
        reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
        close()
      }
    }
  }

  val DriveFileMove: ImageVector by lazy {
    vectorIcon("DriveFileMove", autoMirror = true) {
      path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 6f)
        horizontalLineToRelative(-8f)
        lineToRelative(-2f, -2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(8f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(12f, 17f)
        verticalLineToRelative(-3f)
        horizontalLineTo(8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(4f)
        verticalLineTo(9f)
        lineToRelative(4f, 4f)
        lineToRelative(-4f, 4f)
        close()
      }
    }
  }

  val Layers: ImageVector by lazy {
    vectorIcon("Layers") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(11.99f, 18.54f)
        lineToRelative(-7.37f, -5.73f)
        lineTo(3f, 14.07f)
        lineToRelative(9f, 7f)
        lineToRelative(9f, -7f)
        lineToRelative(-1.63f, -1.27f)
        lineToRelative(-7.38f, 5.74f)
        close()
        moveTo(12f, 16f)
        lineToRelative(7.36f, -5.73f)
        lineTo(21f, 9f)
        lineToRelative(-9f, -7f)
        lineToRelative(-9f, 7f)
        lineToRelative(1.63f, 1.27f)
        lineTo(12f, 16f)
        close()
      }
    }
  }

  val Contrast: ImageVector by lazy {
    vectorIcon("Contrast") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 20f)
        verticalLineTo(4f)
        curveToRelative(4.41f, 0f, 8f, 3.59f, 8f, 8f)
        reflectiveCurveToRelative(-3.59f, 8f, -8f, 8f)
        close()
      }
    }
  }

  val BlurOn: ImageVector by lazy {
    vectorIcon("BlurOn") {
      path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(12f, 3f)
        curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
        reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
        reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
        reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
        close()
        moveTo(12f, 19f)
        curveToRelative(-3.86f, 0f, -7f, -3.14f, -7f, -7f)
        reflectiveCurveToRelative(3.14f, -7f, 7f, -7f)
        reflectiveCurveToRelative(7f, 3.14f, 7f, 7f)
        reflectiveCurveToRelative(-3.14f, 7f, -7f, 7f)
        close()
        moveTo(12f, 8f)
        curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
        reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
        close()
      }
    }
  }

  val Palette: ImageVector by lazy {
    vectorIcon("Palette") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.49f, 2f, 2f, 6.49f, 2f, 12f)
        reflectiveCurveToRelative(4.49f, 10f, 10f, 10f)
        curveToRelative(1.38f, 0f, 2.5f, -1.12f, 2.5f, -2.5f)
        curveToRelative(0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
        curveToRelative(-0.08f, -0.1f, -0.13f, -0.21f, -0.13f, -0.33f)
        curveToRelative(0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
        horizontalLineTo(16f)
        curveToRelative(3.31f, 0f, 6f, -2.69f, 6f, -6f)
        curveToRelative(0f, -4.96f, -4.49f, -9f, -10f, -9f)
        close()
        moveTo(5.5f, 13f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        reflectiveCurveTo(4.67f, 10f, 5.5f, 10f)
        reflectiveCurveTo(7f, 10.67f, 7f, 11.5f)
        reflectiveCurveTo(6.33f, 13f, 5.5f, 13f)
        close()
        moveTo(8.5f, 9f)
        curveTo(7.67f, 9f, 7f, 8.33f, 7f, 7.5f)
        reflectiveCurveTo(7.67f, 6f, 8.5f, 6f)
        reflectiveCurveTo(10f, 6.67f, 10f, 7.5f)
        reflectiveCurveTo(9.33f, 9f, 8.5f, 9f)
        close()
        moveTo(14.5f, 9f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        reflectiveCurveTo(13.67f, 6f, 14.5f, 6f)
        reflectiveCurveTo(16f, 6.67f, 16f, 7.5f)
        reflectiveCurveTo(15.33f, 9f, 14.5f, 9f)
        close()
        moveTo(17.5f, 13f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
        reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
        reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
        close()
      }
    }
  }

  val Wallpaper: ImageVector by lazy {
    vectorIcon("Wallpaper") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 4f)
        horizontalLineToRelative(7f)
        verticalLineTo(2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(7f)
        horizontalLineToRelative(2f)
        verticalLineTo(4f)
        close()
        moveTo(10f, 13f)
        lineToRelative(-4f, 5f)
        horizontalLineToRelative(12f)
        lineToRelative(-3f, -4f)
        lineToRelative(-2.03f, 2.71f)
        lineTo(10f, 13f)
        close()
        moveTo(17f, 8.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveTo(14f, 7.67f, 14f, 8.5f)
        reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
        reflectiveCurveTo(17f, 9.33f, 17f, 8.5f)
        close()
        moveTo(20f, 2f)
        horizontalLineToRelative(-7f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(7f)
        verticalLineToRelative(7f)
        horizontalLineToRelative(2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(20f, 20f)
        horizontalLineToRelative(-7f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(7f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineToRelative(-7f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(7f)
        close()
        moveTo(4f, 13f)
        horizontalLineTo(2f)
        verticalLineToRelative(7f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(7f)
        verticalLineToRelative(-2f)
        horizontalLineTo(4f)
        verticalLineToRelative(-7f)
        close()
      }
    }
  }

  val Edit: ImageVector by lazy {
    vectorIcon("Edit") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 17.25f)
        verticalLineTo(21f)
        horizontalLineToRelative(3.75f)
        lineTo(17.81f, 9.94f)
        lineToRelative(-3.75f, -3.75f)
        lineTo(3f, 17.25f)
        close()
        moveTo(20.71f, 7.04f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
        lineToRelative(-2.34f, -2.34f)
        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
        lineToRelative(-1.83f, 1.83f)
        lineToRelative(3.75f, 3.75f)
        lineToRelative(1.83f, -1.83f)
        close()
      }
    }
  }

  val LocationOn: ImageVector by lazy {
    vectorIcon("LocationOn") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
        curveToRelative(0f, 5.25f, 7f, 13f, 7f, 13f)
        reflectiveCurveToRelative(7f, -7.75f, 7f, -13f)
        curveToRelative(0f, -3.87f, -3.13f, -7f, -7f, -7f)
        close()
        moveTo(12f, 11.5f)
        curveToRelative(-1.38f, 0f, -2.5f, -1.12f, -2.5f, -2.5f)
        reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
        reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
        reflectiveCurveToRelative(-1.12f, 2.5f, -2.5f, 2.5f)
        close()
      }
    }
  }

  val Notes: ImageVector by lazy {
    vectorIcon("Notes", autoMirror = true) {
      path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(16f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
        close()
        moveTo(4f, 17f)
        horizontalLineToRelative(10f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
        close()
        moveTo(4f, 7f)
        horizontalLineToRelative(16f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
        close()
      }
    }
  }

  val VideoCall: ImageVector by lazy {
    vectorIcon("VideoCall") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(17f, 10.5f)
        verticalLineTo(7f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        verticalLineToRelative(10f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(12f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        verticalLineToRelative(-3.5f)
        lineToRelative(2.29f, 2.29f)
        curveToRelative(0.63f, 0.63f, 1.71f, 0.18f, 1.71f, -0.71f)
        verticalLineTo(8.91f)
        curveToRelative(0f, -0.89f, -1.08f, -1.34f, -1.71f, -0.71f)
        lineTo(17f, 10.5f)
        close()
      }
    }
  }

  val OpenInNew: ImageVector by lazy {
    vectorIcon("OpenInNew") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(7f)
        verticalLineTo(3f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineToRelative(-7f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(7f)
        close()
        moveTo(14f, 3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(3.59f)
        lineToRelative(-9.83f, 9.83f)
        lineToRelative(1.41f, 1.41f)
        lineTo(19f, 6.41f)
        verticalLineTo(10f)
        horizontalLineToRelative(2f)
        verticalLineTo(3f)
        horizontalLineToRelative(-7f)
        close()
      }
    }
  }

  val Shield: ImageVector by lazy {
    vectorIcon("Shield") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        lineTo(4f, 5f)
        verticalLineToRelative(6.09f)
        curveToRelative(0f, 5.05f, 3.41f, 9.76f, 8f, 10.91f)
        curveToRelative(4.59f, -1.15f, 8f, -5.86f, 8f, -10.91f)
        verticalLineTo(5f)
        lineToRelative(-8f, -3f)
        close()
        moveTo(10.94f, 15.54f)
        lineTo(7.4f, 12f)
        lineToRelative(1.41f, -1.41f)
        lineToRelative(2.12f, 2.12f)
        lineToRelative(4.24f, -4.24f)
        lineToRelative(1.41f, 1.41f)
        lineToRelative(-5.64f, 5.66f)
        close()
      }
    }
  }

  val Code: ImageVector by lazy {
    vectorIcon("Code") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(8.7f, 15.9f)
        lineTo(4.8f, 12f)
        lineToRelative(3.9f, -3.9f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.01f, 0f, -1.4f)
        curveToRelative(-0.39f, -0.39f, -1.01f, -0.39f, -1.4f, 0f)
        lineToRelative(-4.59f, 4.59f)
        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
        lineToRelative(4.59f, 4.6f)
        curveToRelative(0.39f, 0.39f, 1.01f, 0.39f, 1.4f, 0f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.01f, 0f, -1.4f)
        close()
        moveTo(15.3f, 15.9f)
        lineToRelative(3.9f, -3.9f)
        lineToRelative(-3.9f, -3.9f)
        curveToRelative(-0.39f, -0.39f, -0.39f, -1.01f, 0f, -1.4f)
        curveToRelative(0.39f, -0.39f, 1.01f, -0.39f, 1.4f, 0f)
        lineToRelative(4.59f, 4.59f)
        curveToRelative(0.39f, 0.39f, 0.39f, 1.02f, 0f, 1.41f)
        lineToRelative(-4.59f, 4.6f)
        curveToRelative(-0.39f, 0.39f, -1.01f, 0.39f, -1.4f, 0f)
        curveToRelative(-0.39f, -0.39f, -0.39f, -1.01f, 0f, -1.4f)
        close()
      }
    }
  }

  val Email: ImageVector by lazy {
    vectorIcon("Email") {
      path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(20f, 8f)
        lineToRelative(-8f, 5f)
        lineToRelative(-8f, -5f)
        verticalLineTo(6f)
        lineToRelative(8f, 5f)
        lineToRelative(8f, -5f)
        verticalLineToRelative(2f)
        close()
      }
    }
  }

  private inline fun vectorIcon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
  ): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
  ).apply(block).build()
}
