package com.moodlebridge.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun PathBuilder.applySvgPath(d: String) {
    var i = 0
    var cx = 0f; var cy = 0f
    var lastCmd = ' '
    fun nextNum(): Float {
        while (i < d.length && (d[i] == ' ' || d[i] == ',')) i++
        val start = i
        if (i < d.length && (d[i] == '-' || d[i] == '+')) i++
        while (i < d.length && d[i].isDigit()) i++
        if (i < d.length && d[i] == '.') { i++; while (i < d.length && d[i].isDigit()) i++ }
        return if (start == i) 0f else d.substring(start, i).toFloat()
    }
    fun hasMoreNumbers(): Boolean {
        var j = i
        while (j < d.length && (d[j] == ' ' || d[j] == ',')) j++
        return j < d.length && (d[j].isDigit() || d[j] == '-' || d[j] == '+' || d[j] == '.')
    }

    while (i < d.length) {
        while (i < d.length && (d[i] == ' ' || d[i] == ',')) i++
        if (i >= d.length) break
        val c = d[i]
        if (c.isLetter()) { i++; lastCmd = c } else { /* repeat last command */ }

        when (lastCmd) {
            'M' -> { cx = nextNum(); cy = nextNum(); moveTo(cx, cy); lastCmd = 'L' }
            'm' -> { cx += nextNum(); cy += nextNum(); moveTo(cx, cy); lastCmd = 'l' }
            'L' -> { cx = nextNum(); cy = nextNum(); lineTo(cx, cy) }
            'l' -> { cx += nextNum(); cy += nextNum(); lineTo(cx, cy) }
            'H' -> { cx = nextNum(); horizontalLineTo(cx) }
            'h' -> { cx += nextNum(); horizontalLineTo(cx) }
            'V' -> { cy = nextNum(); verticalLineTo(cy) }
            'v' -> { cy += nextNum(); verticalLineTo(cy) }
            'C' -> {
                val x1 = nextNum(); val y1 = nextNum()
                val x2 = nextNum(); val y2 = nextNum()
                cx = nextNum(); cy = nextNum()
                curveTo(x1, y1, x2, y2, cx, cy)
            }
            'c' -> {
                val x1 = cx + nextNum(); val y1 = cy + nextNum()
                val x2 = cx + nextNum(); val y2 = cy + nextNum()
                cx += nextNum(); cy += nextNum()
                curveTo(x1, y1, x2, y2, cx, cy)
            }
            'S' -> {
                val x2 = nextNum(); val y2 = nextNum()
                cx = nextNum(); cy = nextNum()
                reflectiveCurveTo(x2, y2, cx, cy)
            }
            's' -> {
                val x2 = cx + nextNum(); val y2 = cy + nextNum()
                cx += nextNum(); cy += nextNum()
                reflectiveCurveTo(x2, y2, cx, cy)
            }
            'Q' -> {
                val x1 = nextNum(); val y1 = nextNum()
                cx = nextNum(); cy = nextNum()
                quadTo(x1, y1, cx, cy)
            }
            'q' -> {
                val x1 = cx + nextNum(); val y1 = cy + nextNum()
                cx += nextNum(); cy += nextNum()
                quadTo(x1, y1, cx, cy)
            }
            'T' -> { cx = nextNum(); cy = nextNum(); reflectiveQuadTo(cx, cy) }
            't' -> { cx += nextNum(); cy += nextNum(); reflectiveQuadTo(cx, cy) }
            'A' -> {
                val rx = nextNum(); val ry = nextNum()
                val angle = nextNum()
                val largeArc = nextNum().toInt() != 0
                val sweep = nextNum().toInt() != 0
                cx = nextNum(); cy = nextNum()
                arcTo(rx, ry, angle, largeArc, sweep, cx, cy)
            }
            'a' -> {
                val rx = nextNum(); val ry = nextNum()
                val angle = nextNum()
                val largeArc = nextNum().toInt() != 0
                val sweep = nextNum().toInt() != 0
                cx += nextNum(); cy += nextNum()
                arcTo(rx, ry, angle, largeArc, sweep, cx, cy)
            }
            'Z', 'z' -> { close(); /* reset to first point of subpath is handled by Compose */ }
        }
        if (!hasMoreNumbers() && lastCmd != 'Z' && lastCmd != 'z' && lastCmd != 'H' && lastCmd != 'h' && lastCmd != 'V' && lastCmd != 'v' && lastCmd != 'M' && lastCmd != 'm') {
            // No more numbers for this command, stop repeating
        }
    }
}

object AppIcons {
    val Language: ImageVector by lazy {
        ImageVector.Builder("Language", 24.dp, 24.dp, 16f, 16f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M4,0H6V2H10V4H8.868C8.571,5.67 7.786,7.171 6.666,8.351C7.466,8.739 8.357,8.968 9.299,8.997L10.274,6H12.727L15.977,16H13.874L13.224,14H9.776L9.126,16H7.024L8.662,10.959C7.326,10.829 6.082,10.389 5,9.712C3.695,10.528 2.153,11 0.5,11H0V9H0.5C1.516,9 2.478,8.767 3.334,8.351C2.684,7.666 2.146,6.872 1.752,6H4.022C4.3,6.435 4.629,6.834 5,7.19C5.887,6.339 6.534,5.238 6.826,4H0V2H4V0ZM12.574,12L11.5,8.697L10.427,12H12.574Z")
            }
        }.build()
    }

    val Sun: ImageVector by lazy {
        ImageVector.Builder("Sun", 24.dp, 24.dp, 512f, 512f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M256,118.125c-76.156,0-137.875,61.719-137.875,137.875S179.844,393.875,256,393.875S393.875,332.156,393.875,256S332.156,118.125,256,118.125z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M235.906,0v77.297h40.156V0z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M235.906,434.703v77.297h40.156v-77.297z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M89.17,60.77l54.65,54.65l-28.4,28.4l-54.65-54.65z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M368.156,396.547l54.672,54.672l28.391-28.392l-54.656-54.656z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M0,235.906h77.281v40.156H0z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M434.688,235.922v40.156h77.312v-40.156z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M60.781,422.813l28.375,28.406l54.657-54.672l-28.375-28.391z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M451.219,89.156l-28.406-28.375l-54.657,54.657l28.406,28.406z")
            }
        }.build()
    }

    val MoonDark: ImageVector by lazy {
        ImageVector.Builder("MoonDark", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color(0xFF1C274C))) {
                applySvgPath("M11.017,2.802C6.371,3.292,2.75,7.223,2.75,12c0,5.109,4.141,9.25,9.25,9.25c4.777,0,8.708-3.621,9.198-8.267c-1.327,1.684-3.386,2.767-5.699,2.767c-4.004,0-7.25-3.246-7.25-7.25c0-2.313,1.083-4.372,2.767-5.699zM1.25,12c0-5.937,4.813-10.75,10.75-10.75c0.717,0,1.075,0.571,1.137,1.026c0.059,0.437-0.103,0.994-0.606,1.298c-1.668,1.008-2.781,2.837-2.781,4.926c0,3.176,2.575,5.75,5.75,5.75c2.088,0,3.917-1.113,4.925-2.781c0.304-0.503,0.861-0.665,1.298-0.606c0.455,0.061,1.026,0.42,1.026,1.137c0,5.937-4.813,10.75-10.75,10.75C6.063,22.75,1.25,17.937,1.25,12z")
            }
        }.build()
    }

    val MoonAmoled: ImageVector by lazy {
        ImageVector.Builder("MoonAmoled", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color(0xFF1C274C))) {
                applySvgPath("M12,22c5.523,0,10-4.477,10-10c0-0.463-0.694-0.539-0.933-0.143c-1.138,1.884-3.206,3.143-5.569,3.143c-3.59,0-6.5-2.91-6.5-6.5c0-2.362,1.259-4.429,3.143-5.569c0.396-0.239,0.472-0.933,0.009-0.933C6.477,2,2,6.477,2,12c0,5.523,4.477,10,10,10z")
            }
        }.build()
    }

    val Folder: ImageVector by lazy {
        ImageVector.Builder("Folder", 24.dp, 24.dp, 16f, 16f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M0,1h6l3,3h7v10H0V1z")
            }
        }.build()
    }

    val Qr: ImageVector by lazy {
        ImageVector.Builder("Qr", 24.dp, 24.dp, 122.88f, 122.88f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M0.18,0h44.63v44.45H0.18V0L0.18,0z M111.5,111.5h11.38v11.2H111.5V111.5L111.5,111.5z M89.63,111.48h11.38v10.67H89.63h-0.01H78.25v-21.82h11.02V89.27h11.21V67.22h11.38v10.84h10.84v11.2h-10.84v11.2h-11.21h-0.17H89.63V111.48L89.63,111.48z M55.84,89.09h11.02v-11.2H56.2v-11.2h10.66v-11.2H56.02v11.2H44.63v-11.2h11.2V22.23h11.38v33.25h11.02v11.2h10.84v-11.2h11.38v11.2H89.63v11.2H78.25v22.05H67.22v22.23H55.84V89.09L55.84,89.09z M111.31,55.48h11.38v11.2h-11.38V55.48L111.31,55.48z M22.41,55.48h11.38v11.2H22.41V55.48L22.41,55.48z M0.18,55.48h11.38v11.2H0.18V55.48L0.18,55.48z M55.84,0h11.38v11.2H55.84V0L55.84,0z M0,78.06h44.63v44.45H0V78.06L0,78.06z M10.84,88.86h22.95v22.86H10.84V88.86L10.84,88.86z M78.06,0h44.63v44.45H78.06V0L78.06,0z M88.91,10.8h22.95v22.86H88.91V10.8L88.91,10.8z M11.02,10.8h22.95v22.86H11.02V10.8L11.02,10.8z")
            }
        }.build()
    }

    val Obsidian: ImageVector by lazy {
        ImageVector.Builder("Obsidian", 24.dp, 24.dp, 512f, 512f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M424.853,91.195L266.896,0v96.85l74.083,42.756z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M245.085,96.85V0L87.147,91.195l83.864,48.411z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M160.11,158.513L76.247,110.092v291.826l83.863,48.43z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M87.147,420.805L245.085,512v-96.85l-73.938-42.775z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M266.896,415.158v96.842l157.948-91.195l-83.873-48.43z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M351.881,158.513v194.975l83.872,48.43v-291.836z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M330.07,158.503l-74.075-42.766l-74.074,42.775v194.976l74.074,42.774l74.075-42.774V158.503zM207.348,260.016V187.89l55.496-33.294L207.348,260.016z")
            }
        }.build()
    }

    val Logseq: ImageVector by lazy {
        ImageVector.Builder("Logseq", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M19.3,9.838c-2.677-1.366-5.467-1.56-8.316-0.607c-1.738,0.58-3.197,1.58-4.267,3.088c-1.031,1.452-1.45,3.071-1.184,4.837c0.268,1.781,1.164,3.228,2.505,4.4c1.406,1.111,3.686,1.822,6.538,1.88c0.41-0.053,1.157-0.103,1.883-0.255c2.004-0.418,3.754-1.325,5.08-2.915c1.621-1.942,2.108-4.148,1.272-6.562c-0.704-2.034-2.138-3.467-4.027-4.43zM7.515,6.295c0.507-2.162-0.88-4.664-2.988-5.37c-1.106-0.37-2.156-0.267-3.075,0.492c-1.442,1.147-1.758,2.097-1.781,3.179c0.009,0.135,0.016,0.285,0.029,0.435c0.01,0.102,0.021,0.205,0.042,0.305c0.351,1.703,1.262,2.98,2.9,3.636c1.912,0.766,3.808-0.244,4.273-2.227zM11.579,5.149c1.075,0.377,2.152,0.31,3.22-0.033c0.94-0.3,1.755-0.793,2.341-1.609c0.803-1.117,0.5-2.387-0.717-3.027c-0.6-0.317-1.246-0.438-1.927-0.48c-0.47,0.076-0.95,0.117-1.41,0.234c-1.068,0.27-2.002,0.781-2.653,1.7c-0.495,0.697-0.64,1.45-0.174,2.227c0.303,0.504,0.779,0.799,1.32,0.988z")
            }
        }.build()
    }
}
