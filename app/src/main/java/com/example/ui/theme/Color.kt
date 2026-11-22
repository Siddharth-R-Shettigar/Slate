package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Fallback Expressive Slate & Sky Palette (used when Android Dynamic Color is unavailable on API < 31)
val SlatePrimaryLight = Color(0xFF1A56DB)
val SlateOnPrimaryLight = Color(0xFFFFFFFF)
val SlatePrimaryContainerLight = Color(0xFFD9E6FF)
val SlateOnPrimaryContainerLight = Color(0xFF001A43)

val SlateSecondaryLight = Color(0xFF475569)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFE2E8F0)
val SlateOnSecondaryContainerLight = Color(0xFF0F172A)

val SlateTertiaryLight = Color(0xFFD97706)
val SlateOnTertiaryLight = Color(0xFFFFFFFF)
val SlateTertiaryContainerLight = Color(0xFFFEF3C7)
val SlateOnTertiaryContainerLight = Color(0xFF451A03)

val SlateBackgroundLight = Color(0xFFF3F6FB)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceContainerLowLight = Color(0xFFF8FAFC)
val SlateSurfaceContainerLight = Color(0xFFEEF2F8)
val SlateSurfaceContainerHighLight = Color(0xFFE4EAF3)
val SlateOnSurfaceLight = Color(0xFF0F172A)
val SlateOnSurfaceVariantLight = Color(0xFF475569)
val SlateOutlineVariantLight = Color(0xFFCBD5E1)

val SlatePrimaryDark = Color(0xFF8EC5FF)
val SlateOnPrimaryDark = Color(0xFF002E69)
val SlatePrimaryContainerDark = Color(0xFF0E3E82)
val SlateOnPrimaryContainerDark = Color(0xFFD9E6FF)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateSecondaryContainerDark = Color(0xFF283244)
val SlateOnSecondaryContainerDark = Color(0xFFE2E8F0)

val SlateTertiaryDark = Color(0xFFFBBF24)
val SlateOnTertiaryDark = Color(0xFF451A03)
val SlateTertiaryContainerDark = Color(0xFF6B3C00)
val SlateOnTertiaryContainerDark = Color(0xFFFEF3C7)

val SlateBackgroundDark = Color(0xFF0B0F17)
val SlateSurfaceDark = Color(0xFF131926)
val SlateSurfaceContainerLowDark = Color(0xFF182030)
val SlateSurfaceContainerDark = Color(0xFF1E273A)
val SlateSurfaceContainerHighDark = Color(0xFF263148)
val SlateOnSurfaceDark = Color(0xFFF1F5F9)
val SlateOnSurfaceVariantDark = Color(0xFF94A3B8)
val SlateOutlineVariantDark = Color(0xFF334155)

// Curated List Color Tags (ARGB Long values)
object ListColorTags {
  val Sky = 0xFF3B82F6L
  val Coral = 0xFFEF4444L
  val Emerald = 0xFF10B981L
  val Amber = 0xFFF59E0BL
  val Violet = 0xFF8B5CF6L
  val Cyan = 0xFF06B6D4L

  val all = listOf(
    Sky to "Sky",
    Coral to "Coral",
    Emerald to "Emerald",
    Amber to "Amber",
    Violet to "Violet",
    Cyan to "Cyan",
  )
}
