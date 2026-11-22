package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppColorTheme

val SlateShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SlateTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  oledDarkMode: Boolean = false,
  appColorTheme: AppColorTheme = AppColorTheme.WALLPAPER,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current

  val colorScheme = androidx.compose.runtime.remember(context, darkTheme, oledDarkMode, appColorTheme) {
    val baseScheme = when {
      appColorTheme == AppColorTheme.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      appColorTheme == AppColorTheme.WALLPAPER -> {
        buildTonalColorScheme(
          darkTheme = darkTheme,
          primarySeed = Color(0xFF2563EB),
          secondarySeed = Color(0xFF64748B),
          tertiarySeed = Color(0xFFF59E0B),
        )
      }
      appColorTheme == AppColorTheme.BLUE -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFF2563EB),
        secondarySeed = Color(0xFF3B82F6),
        tertiarySeed = Color(0xFF06B6D4),
      )
      appColorTheme == AppColorTheme.CYAN -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFF0891B2),
        secondarySeed = Color(0xFF06B6D4),
        tertiarySeed = Color(0xFF3B82F6),
      )
      appColorTheme == AppColorTheme.EMERALD -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFF059669),
        secondarySeed = Color(0xFF10B981),
        tertiarySeed = Color(0xFF0D9488),
      )
      appColorTheme == AppColorTheme.SAGE -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFF4D7C0F),
        secondarySeed = Color(0xFF65A30D),
        tertiarySeed = Color(0xFFCA8A04),
      )
      appColorTheme == AppColorTheme.PURPLE -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFF7C3AED),
        secondarySeed = Color(0xFF8B5CF6),
        tertiarySeed = Color(0xFFEC4899),
      )
      appColorTheme == AppColorTheme.ORANGE -> buildTonalColorScheme(
        darkTheme = darkTheme,
        primarySeed = Color(0xFFEA580C),
        secondarySeed = Color(0xFFF97316),
        tertiarySeed = Color(0xFFEAB308),
      )
      else -> buildTonalColorScheme( // ROSE
        darkTheme = darkTheme,
        primarySeed = Color(0xFFE11D48),
        secondarySeed = Color(0xFFF43F5E),
        tertiarySeed = Color(0xFFF97316),
      )
    }

    if (darkTheme) {
      if (oledDarkMode) {
        baseScheme.copy(
          background = Color.Black,
          surface = Color.Black,
          surfaceContainerLowest = Color.Black,
          surfaceContainerLow = Color(0xFF0A0A0D),
          surfaceContainer = Color(0xFF121216),
          surfaceContainerHigh = Color(0xFF1A1A20),
          surfaceContainerHighest = Color(0xFF23232B),
        )
      } else {
        val tintBase = baseScheme.primary
        val subtleTintBg = lerp(Color(0xFF111318), tintBase, 0.065f)
        val subtleTintSurface = lerp(Color(0xFF161920), tintBase, 0.06f)
        val subtleContainerLow = lerp(Color(0xFF1B1F28), tintBase, 0.07f)
        val subtleContainer = lerp(Color(0xFF212632), tintBase, 0.08f)
        val subtleContainerHigh = lerp(Color(0xFF29303F), tintBase, 0.09f)
        val subtleContainerHighest = lerp(Color(0xFF323B4D), tintBase, 0.10f)
        baseScheme.copy(
          background = subtleTintBg,
          surface = subtleTintSurface,
          surfaceContainerLowest = subtleTintBg,
          surfaceContainerLow = subtleContainerLow,
          surfaceContainer = subtleContainer,
          surfaceContainerHigh = subtleContainerHigh,
          surfaceContainerHighest = subtleContainerHighest,
        )
      }
    } else {
      val tintBase = baseScheme.primary
      baseScheme.copy(
        background = lerp(Color(0xFFF7F8FB), tintBase, 0.03f),
        surface = lerp(Color(0xFFFFFFFF), tintBase, 0.015f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFF3F5F9), tintBase, 0.035f),
        surfaceContainer = lerp(Color(0xFFECEFF5), tintBase, 0.05f),
        surfaceContainerHigh = lerp(Color(0xFFE3E8F1), tintBase, 0.065f),
        surfaceContainerHighest = lerp(Color(0xFFD9E0EC), tintBase, 0.08f),
      )
    }
  }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    shapes = SlateShapes,
    typography = SlateTypography,
    content = content,
  )
}

private fun buildTonalColorScheme(
  darkTheme: Boolean,
  primarySeed: Color,
  secondarySeed: Color,
  tertiarySeed: Color,
): ColorScheme {
  return if (darkTheme) {
    val primary = lerp(primarySeed, Color.White, 0.42f)
    val onPrimary = lerp(primarySeed, Color.Black, 0.72f)
    val primaryContainer = lerp(primarySeed, Color.Black, 0.45f)
    val onPrimaryContainer = lerp(primarySeed, Color.White, 0.85f)

    val secondary = lerp(secondarySeed, Color.White, 0.48f)
    val secondaryContainer = lerp(secondarySeed, Color.Black, 0.52f)
    val onSecondaryContainer = lerp(secondarySeed, Color.White, 0.84f)

    val tertiary = lerp(tertiarySeed, Color.White, 0.45f)
    val tertiaryContainer = lerp(tertiarySeed, Color.Black, 0.48f)
    val onTertiaryContainer = lerp(tertiarySeed, Color.White, 0.85f)

    darkColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      primaryContainer = primaryContainer,
      onPrimaryContainer = onPrimaryContainer,
      secondary = secondary,
      onSecondary = Color(0xFF0F172A),
      secondaryContainer = secondaryContainer,
      onSecondaryContainer = onSecondaryContainer,
      tertiary = tertiary,
      onTertiary = Color(0xFF1F1300),
      tertiaryContainer = tertiaryContainer,
      onTertiaryContainer = onTertiaryContainer,
      onSurface = Color(0xFFF1F5F9),
      onSurfaceVariant = lerp(Color(0xFF94A3B8), primarySeed, 0.12f),
      outlineVariant = lerp(Color(0xFF334155), primarySeed, 0.16f),
    )
  } else {
    val primary = lerp(primarySeed, Color.Black, 0.12f)
    val primaryContainer = lerp(primarySeed, Color.White, 0.80f)
    val onPrimaryContainer = lerp(primarySeed, Color.Black, 0.65f)

    val secondary = lerp(secondarySeed, Color.Black, 0.25f)
    val secondaryContainer = lerp(secondarySeed, Color.White, 0.84f)
    val onSecondaryContainer = lerp(secondarySeed, Color.Black, 0.68f)

    val tertiary = lerp(tertiarySeed, Color.Black, 0.12f)
    val tertiaryContainer = lerp(tertiarySeed, Color.White, 0.80f)
    val onTertiaryContainer = lerp(tertiarySeed, Color.Black, 0.68f)

    lightColorScheme(
      primary = primary,
      onPrimary = Color.White,
      primaryContainer = primaryContainer,
      onPrimaryContainer = onPrimaryContainer,
      secondary = secondary,
      onSecondary = Color.White,
      secondaryContainer = secondaryContainer,
      onSecondaryContainer = onSecondaryContainer,
      tertiary = tertiary,
      onTertiary = Color.White,
      tertiaryContainer = tertiaryContainer,
      onTertiaryContainer = onTertiaryContainer,
      onSurface = Color(0xFF0F172A),
      onSurfaceVariant = lerp(Color(0xFF475569), primarySeed, 0.12f),
      outlineVariant = lerp(Color(0xFFCBD5E1), primarySeed, 0.14f),
    )
  }
}
