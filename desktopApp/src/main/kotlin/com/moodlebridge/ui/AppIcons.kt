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

    val Sakura: ImageVector by lazy {
        ImageVector.Builder("Sakura", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M14 12.5a2 2 0 1 1-4 0a2 2 0 0 1 4 0")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M12 18.088q-.172.28-.377.562C10.309 20.447 8.496 21.504 7 21.5l-.302-2.075l-2.078.356c-.466-1.413.029-3.514 1.342-5.312C3.935 13.766 2.442 12.397 2 11l1.86-.931l-.966-1.866c1.187-.883 3.526-.99 5.615-.299c0-2.404.827-4.534 2.03-5.404L12 4l1.46-1.5c1.204.87 2.042 3 2.03 5.404c2.09-.69 4.429-.584 5.616.299l-.965 1.866L22 11c-.442 1.396-1.935 2.765-3.962 3.47c1.313 1.797 1.808 3.898 1.342 5.311l-2.078-.356L17 21.5c-1.496.004-3.309-1.053-4.623-2.85a8 8 0 0 1-.377-.562")
            }
        }.build()
    }

    val SolarizedLight: ImageVector by lazy {
        ImageVector.Builder("SolarizedLight", 48.dp, 48.dp, 48f, 48f).apply {
            path(strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, stroke = SolidColor(Color.Black), fill = SolidColor(Color.Transparent)) {
                applySvgPath("M18.05 34.423a11.995 11.995 0 1 0-4.474-4.473")
            }
            path(strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, stroke = SolidColor(Color.Black), fill = SolidColor(Color.Transparent)) {
                applySvgPath("M24 9.482V2.5m0 43v-6.981m-7.259-27.092L13.25 5.38m21.5 37.24l-3.491-6.047M11.427 16.741L5.38 13.25m37.24 21.5l-6.047-3.491M9.482 24H2.5m43 0h-6.981m4.101-10.75l-6.047 3.491M34.75 5.38l-3.491 6.047M38.519 24H45.5m-43 0h6.982")
            }
        }.build()
    }

    val SolarizedDark: ImageVector by lazy {
        ImageVector.Builder("SolarizedDark", 512.dp, 512.dp, 512f, 512f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M256 26.37l-35.4 97.23c11.3-3 23.2-4.6 35.4-4.6s24.1 1.6 35.4 4.6zm162.5 67.22l-94 43.81c20.8 12 38.1 29.3 50.1 50.1zm-324.88 0l43.78 93.81c12.1-20.7 29.3-38 50.1-50zM247 137.3c-58.6 4.4-105.3 51.1-109.7 109.7H176v18h-38.7c4.4 58.6 51.1 105.3 109.7 109.7V336h18v38.7c58.6-4.4 105.3-51.1 109.7-109.7H336v-18h38.7c-4.4-58.6-51.1-105.3-109.7-109.7V176h-18zm54.5 25.9l15.4 9.2l-49.9 82.7l37.2 44.1l-13.8 11.6l-45.4-53.9zm-177.9 57.4L26.38 256l97.22 35.3c-3-11.2-4.6-23.1-4.6-35.3c0-12.2 1.6-24.1 4.6-35.4zm264.8.1c3 11.2 4.6 23.1 4.6 35.3c0 12.2-1.6 24.1-4.6 35.4l97.2-35.4zm-13.8 103.8c-12 20.8-29.3 38.1-50.1 50.1l94 43.9zm-237.1.2l-43.8 93.8l93.8-43.9c-20.7-12-38-29.2-50-49.9zm83.1 63.7l35.4 97.2l35.3-97.2c-11.2 3-23.1 4.6-35.3 4.6c-12.2 0-24.1-1.6-35.4-4.6z")
            }
        }.build()
    }

    val Nord: ImageVector by lazy {
        ImageVector.Builder("Nord", 48.dp, 48.dp, 48f, 48f).apply {
            path(strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, stroke = SolidColor(Color.Black), fill = SolidColor(Color.Transparent)) {
                applySvgPath("M6.59 36.89c-7.003-9.732-4.792-23.299 4.94-30.303A21.71 21.71 0 0 1 24 2.5c11.99.117 21.614 9.93 21.497 21.92a21.71 21.71 0 0 1-4.087 12.47L31.08 20l-1.86 3.17l1.88 3.23L24 14.17l-5.27 9l1.9 3.27L16.91 20L6.59 36.89Zm9.91 1.968h15")
            }
        }.build()
    }

    val Catppuccin: ImageVector by lazy {
        ImageVector.Builder("Catppuccin", 1024.dp, 1024.dp, 1024f, 1024f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M678 832H346q-47-33-89-76q-34 12-65 12q-80 0-136-56.5T0 576q0-63 37-112.5t95-69.5q1-6 1-10h758q5 35 5 128q0 45-21 94t-55 91t-69.5 75.5T678 832zM128 512q0-15 1-47q-30 17-47.5 46.5T64 576q0 53 37.5 90.5T192 704q4 0 16-2q-80-101-80-190zm448-320q0 23 9 55q39 28 51 73h-64q-2-14-5-24q-55-38-55-104q0-53 37.5-90.5T640 64q15 0 32 4q-42 11-69 45t-27 79zm-192-64q0 23 9 55q55 39 55 105q0 14-5 32h-63q4-18 4-32q0-24-9-56q-55-38-55-104q0-53 37.5-90.5T448 0q15 0 32 4q-42 11-69 45t-27 79zm384 896H256q-55 0-116-21.5t-100.5-52T0 896h1024q0 24-39.5 54.5t-100.5 52t-116 21.5z")
            }
        }.build()
    }

    val HighContrast: ImageVector by lazy {
        ImageVector.Builder("HighContrast", 32.dp, 32.dp, 32f, 32f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M15.907 7.689a1.75 1.75 0 0 1 1.63 1.09L22.886 21.9a1.75 1.75 0 1 1-3.241 1.322l-.734-1.801a.125.125 0 0 0-.116-.078h-5.613a.125.125 0 0 0-.116.079l-.704 1.782a1.75 1.75 0 0 1-3.255-1.286l5.182-13.122a1.75 1.75 0 0 1 1.618-1.107Zm1.475 9.982l-1.322-3.244a.125.125 0 0 0-.232 0l-1.281 3.245a.125.125 0 0 0 .116.17h2.603c.09 0 .15-.09.116-.171Z")
            }
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M6 1a5 5 0 0 0-5 5v20a5 5 0 0 0 5 5h20a5 5 0 0 0 5-5V6a5 5 0 0 0-5-5H6ZM3 6a3 3 0 0 1 3-3h20a3 3 0 0 1 3 3v20a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6Z")
            }
        }.build()
    }

    val Dracula: ImageVector by lazy {
        ImageVector.Builder("Dracula", 512.dp, 512.dp, 512f, 512f).apply {
            path(fill = SolidColor(Color.Black)) {
                applySvgPath("M169.57 106.12c-1.882-14.48-28.184-30.424-41.092-30.424c-2.54 0-4.56.612-5.773 1.974c-21.81 24.385 14.225 81.262 14.225 81.262s35.746-28.785 32.64-52.812zm128.832 60.524s34.315-54.186 13.544-77.36c-1.155-1.292-3.083-1.88-5.496-1.88c-12.308 0-37.352 15.183-39.142 28.98c-2.967 22.78 31.094 50.26 31.094 50.26zm-53.724-50.352c-1.79-13.798-26.845-28.98-39.14-28.98c-2.414 0-4.343.588-5.497 1.88c-20.782 23.22 13.544 77.36 13.544 77.36s34.06-27.387 31.094-50.26zM58.128 58.896a16.546 16.546 0 0 0-1.664.08c-87.75 8.937 11.373 286.056 40.55 304.484c0 0-16.984-151.636 2.795-244.236c7.238-34.107-24.006-60.328-41.683-60.328zm397.394.08a16.465 16.465 0 0 0-1.663-.08c-17.678 0-48.968 26.198-41.682 60.328c19.778 92.6 2.794 244.236 2.794 244.236c29.223-18.416 128.312-295.535 40.55-304.483zm-72.013 16.72c-12.92 0-39.258 15.945-41.094 30.424c-3.106 23.97 32.652 52.823 32.652 52.823s36.024-56.888 14.225-81.26c-1.224-1.375-3.244-1.987-5.784-1.987zm-148.3 348.98c-2.032-11.37-25.598-35.353-25.598-35.353s-20.54 23.97-20.32 33.727c0 2.147-1.155 23.092 2.03 26c2.876 2.645 12.84 4.043 22.62 4.043c9.468 0 18.75-1.305 21.268-4.042c2.31-2.47.335-22.733 0-24.373zm-60.04-24.35c-12.065-20.69-19.155-51.01-23.092-73.733c-3.094-17.966-4.19-31.174-4.19-31.174c-15.127 11.81-44.292 137.675 2.54 137.675a30.02 30.02 0 0 0 4.952-.428c4.62-.762 10.46-4.295 15.01-9.236c5.935-6.43 9.456-15.17 4.78-23.103zm188.85-104.93s-1.006 12.03-3.777 28.738c-3.81 22.965-10.98 54.74-23.485 76.204c-3.764 6.466-2.136 13.417 1.744 19.236c4.618 6.86 12.308 12.135 18.035 13.082a30.124 30.124 0 0 0 4.953.427c46.912-.046 17.677-125.91 2.563-137.734zm-41.406 127.7c.22-9.757-20.32-33.726-20.32-33.726s-23.532 23.935-25.518 35.307c-.254 1.444-2.563 21.592 0 24.386c2.564 2.794 11.812 4.04 21.268 4.04c9.78 0 19.744-1.396 22.62-4.04c4.387-4.088 1.962-25.286 1.985-26.002zm-143.726 5.496l.243.115v-.44z")
            }
        }.build()
    }
}
