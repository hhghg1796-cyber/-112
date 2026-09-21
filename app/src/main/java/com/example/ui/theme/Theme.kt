package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme =
  darkColorScheme(
    primary = LibraryPrimaryDark,
    onPrimary = LibraryOnPrimaryDark,
    primaryContainer = LibraryPrimaryContainerDark,
    onPrimaryContainer = LibraryOnPrimaryContainerDark,
    secondary = LibrarySecondaryDark,
    onSecondary = LibraryOnSecondaryDark,
    secondaryContainer = LibrarySecondaryContainerDark,
    onSecondaryContainer = LibraryOnSecondaryContainerDark,
    tertiary = LibraryTertiaryDark,
    onTertiary = LibraryOnTertiaryDark,
    tertiaryContainer = LibraryTertiaryContainerDark,
    onTertiaryContainer = LibraryOnTertiaryContainerDark,
    background = LibraryBackgroundDark,
    onBackground = LibraryOnBackgroundDark,
    surface = LibrarySurfaceDark,
    onSurface = LibraryOnSurfaceDark,
    surfaceVariant = LibrarySurfaceVariantDark,
    onSurfaceVariant = LibraryOnSurfaceVariantDark,
    outline = LibraryOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LibraryPrimary,
    onPrimary = LibraryOnPrimary,
    primaryContainer = LibraryPrimaryContainer,
    onPrimaryContainer = LibraryOnPrimaryContainer,
    secondary = LibrarySecondary,
    onSecondary = LibraryOnSecondary,
    secondaryContainer = LibrarySecondaryContainer,
    onSecondaryContainer = LibraryOnSecondaryContainer,
    tertiary = LibraryTertiary,
    onTertiary = LibraryOnTertiary,
    tertiaryContainer = LibraryTertiaryContainer,
    onTertiaryContainer = LibraryOnTertiaryContainer,
    background = LibraryBackground,
    onBackground = LibraryOnBackground,
    surface = LibrarySurface,
    onSurface = LibraryOnSurface,
    surfaceVariant = LibrarySurfaceVariant,
    onSurfaceVariant = LibraryOnSurfaceVariant,
    outline = LibraryOutline,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our intentional library palette instead of system tint
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

