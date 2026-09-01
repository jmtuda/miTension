package com.mitension.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MiTensionColors {
    val Ink = Color(0xFF24302B)
    val Muted = Color(0xFF69736E)
    val Green = Color(0xFF276856)
    val GreenDark = Color(0xFF174A3D)
    val Mint = Color(0xFFDCEBE3)
    val Cream = Color(0xFFF7F5F0)
    val Paper = Color(0xFFFFFDFA)
    val Line = Color(0xFFD9DDD8)
    val Danger = Color(0xFF9A3F3F)
}

private val colorScheme = lightColorScheme(
    primary = MiTensionColors.Green,
    onPrimary = Color.White,
    primaryContainer = MiTensionColors.Mint,
    onPrimaryContainer = MiTensionColors.GreenDark,
    secondary = MiTensionColors.GreenDark,
    onSecondary = Color.White,
    background = MiTensionColors.Cream,
    onBackground = MiTensionColors.Ink,
    surface = MiTensionColors.Paper,
    onSurface = MiTensionColors.Ink,
    surfaceVariant = MiTensionColors.Mint,
    onSurfaceVariant = MiTensionColors.Muted,
    outline = MiTensionColors.Line,
    error = MiTensionColors.Danger,
)

private val typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        color = MiTensionColors.GreenDark,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        color = MiTensionColors.GreenDark,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = MiTensionColors.GreenDark,
    ),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

@Composable
fun MiTensionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
