/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme

import androidx.compose.ui.graphics.Color

// Material Design 3 Color System - Light Theme
// Primary palette derived from seed #6750A4 (matches app logo exactly)
val PrimaryLight = Color(0xFF6750A4) // M3 baseline purple — matches logo stroke color
val OnPrimaryLight = Color(0xFFFFFFFF) // White text on primary
val PrimaryContainerLight = Color(0xFFEADDFF) // Light purple container
val OnPrimaryContainerLight = Color(0xFF21005D) // Dark text on primary container

// Secondary color palette — neutral violet
val SecondaryLight = Color(0xFF625B71) // Neutral gray-purple for balance
val OnSecondaryLight = Color(0xFFFFFFFF) // White text on secondary
val SecondaryContainerLight = Color(0xFFE8DEF8) // Light gray-purple container
val OnSecondaryContainerLight = Color(0xFF1D192B) // Dark text on secondary container

// Tertiary color palette — rosy pink (matches logo center pill gradient)
val TertiaryLight = Color(0xFF7D5260) // Rosy pink accent
val OnTertiaryLight = Color(0xFFFFFFFF) // White text on tertiary
val TertiaryContainerLight = Color(0xFFFFD8E4) // Light pink container (matches logo)
val OnTertiaryContainerLight = Color(0xFF31111D) // Dark text on tertiary container

// Error color palette
val ErrorLight = Color(0xFFB3261E) // Standard Material error red
val OnErrorLight = Color(0xFFFFFFFF) // White text on error
val ErrorContainerLight = Color(0xFFF9DEDC) // Light red container
val OnErrorContainerLight = Color(0xFF410E0B) // Dark text on error container

// Background and surface colors
val BackgroundLight = Color(0xFFFAF5FF) // Softened — subtle purple tint, less stark white
val OnBackgroundLight = Color(0xFF1C1B1F) // Dark text on background
val SurfaceLight = Color(0xFFFAF5FF) // Surface same as background
val OnSurfaceLight = Color(0xFF1C1B1F) // Dark text on surface
val SurfaceVariantLight = Color(0xFFE4DCF0) // Slightly richer purple-gray surface variant
val OnSurfaceVariantLight = Color(0xFF49454F) // Medium gray text

// Outline colors for borders and dividers
val OutlineLight = Color(0xFF79747E) // Medium gray outline
val OutlineVariantLight = Color(0xFFCAC4D0) // Light gray outline variant

// Surface containers for different elevation levels
val SurfaceContainerLowestLight = Color(0xFFF5EFFE) // Lowest elevation — faint purple tint
val SurfaceContainerLowLight = Color(0xFFF0E9F9) // Low elevation
val SurfaceContainerLight = Color(0xFFEAE3F3) // Medium elevation
val SurfaceContainerHighLight = Color(0xFFE4DDED) // High elevation
val SurfaceContainerHighestLight = Color(0xFFDED6E7) // Highest elevation

// Inverse colors for special cases
val InverseSurfaceLight = Color(0xFF313033) // Dark surface for light theme
val InverseOnSurfaceLight = Color(0xFFF4EFF4) // Light text on inverse surface
val InversePrimaryLight = Color(0xFFD0BCFF) // Light primary on dark surface

// Material Design 3 Color System - Dark Theme
// Primary palette derived from seed #6750A4 (matches app logo exactly)
val PrimaryDark = Color(0xFFD0BCFF) // Light purple for dark theme (M3 baseline)
val OnPrimaryDark = Color(0xFF381E72) // Dark text on primary
val PrimaryContainerDark = Color(0xFF4F378B) // Medium purple container
val OnPrimaryContainerDark = Color(0xFFEADDFF) // Light text on primary container

// Secondary color palette — neutral violet
val SecondaryDark = Color(0xFFCCC2DC) // Light gray-purple for balance
val OnSecondaryDark = Color(0xFF332D41) // Dark text on secondary
val SecondaryContainerDark = Color(0xFF4A4458) // Medium gray-purple container
val OnSecondaryContainerDark = Color(0xFFE8DEF8) // Light text on secondary container

// Tertiary color palette — rosy pink (matches logo center pill gradient)
val TertiaryDark = Color(0xFFEFB8C8) // Rosy pink for dark theme
val OnTertiaryDark = Color(0xFF492532) // Dark text on tertiary
val TertiaryContainerDark = Color(0xFF633B48) // Medium rose container
val OnTertiaryContainerDark = Color(0xFFFFD8E4) // Light text on tertiary container

// Error color palette
val ErrorDark = Color(0xFFF2B8B5) // Light red for dark theme
val OnErrorDark = Color(0xFF601410) // Dark text on error
val ErrorContainerDark = Color(0xFF8C1D18) // Medium red container
val OnErrorContainerDark = Color(0xFFF9DEDC) // Light text on error container

// Background and surface colors
val BackgroundDark = Color(0xFF141218) // Slightly deeper than M3 baseline for better depth
val OnBackgroundDark = Color(0xFFE6E1E5) // Light text on background
val SurfaceDark = Color(0xFF141218) // Surface same as background
val OnSurfaceDark = Color(0xFFE6E1E5) // Light text on surface
val SurfaceVariantDark = Color(0xFF49454F) // Medium gray surface variant
val OnSurfaceVariantDark = Color(0xFFCAC4D0) // Light gray text

// Outline colors for borders and dividers
val OutlineDark = Color(0xFF938F99) // Light gray outline
val OutlineVariantDark = Color(0xFF49454F) // Medium gray outline variant

// Surface containers for different elevation levels
val SurfaceContainerLowestDark = Color(0xFF0D0B10) // Lowest elevation — slightly deeper
val SurfaceContainerLowDark = Color(0xFF1A1820) // Low elevation
val SurfaceContainerDark = Color(0xFF201E25) // Medium elevation
val SurfaceContainerHighDark = Color(0xFF2A282F) // High elevation
val SurfaceContainerHighestDark = Color(0xFF35333A) // Highest elevation

// Inverse colors for special cases
val InverseSurfaceDark = Color(0xFFE6E1E5) // Light surface for dark theme
val InverseOnSurfaceDark = Color(0xFF313033) // Dark text on inverse surface
val InversePrimaryDark = Color(0xFF6750A4) // Dark primary on light surface (= logo color)

// Legacy music-specific colors (for backward compatibility)
val MusicPrimaryLight = PrimaryLight
val MusicPrimaryVariantLight = PrimaryContainerLight
val MusicSecondaryLight = TertiaryLight

val MusicPrimaryDark = PrimaryDark
val MusicPrimaryVariantDark = PrimaryContainerDark
val MusicSecondaryDark = TertiaryDark

// UI Specific Colors for player components
val PlayerButtonColor = PrimaryLight
val PlayerButtonColorDark = PrimaryDark
val PlayerProgressColor = TertiaryLight
val PlayerProgressBackgroundLight = SurfaceContainerLight
val PlayerProgressBackgroundDark = SurfaceContainerDark
val PlayerBackgroundLight = BackgroundLight
val PlayerBackgroundDark = BackgroundDark

// Status colors for notifications, errors, etc.
val SuccessLight = Color(0xFF2E7D32) // Material green
val SuccessDark = Color(0xFF66BB6A) // Light green for dark theme
val WarningLight = Color(0xFFEF6C00) // Material orange
val WarningDark = Color(0xFFFFB74D) // Light orange for dark theme

// ============================================
// Material 3 Preset Theme Seeds
// ============================================

val PresetDefaultSeed = Color(0xFF6750A4)
val PresetWarmSeed = Color(0xFFFF6B35)
val PresetCoolSeed = Color(0xFF1E88E5)
val PresetForestSeed = Color(0xFF2E7D32)
val PresetRoseSeed = Color(0xFFE91E63)
val PresetMonochromeSeed = Color(0xFF757575)
val PresetLavenderSeed = Color(0xFF7C4DFF)
val PresetOceanSeed = Color(0xFF006064)
val PresetAuroraSeed = Color(0xFF00C853)
val PresetAmberSeed = Color(0xFFFF6F00)
val PresetCrimsonSeed = Color(0xFFB71C1C)
val PresetEmeraldSeed = Color(0xFF00897B)
val PresetMintSeed = Color(0xFF0097A7)

