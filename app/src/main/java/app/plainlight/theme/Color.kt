package app.plainlight.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #006A71  — brand: filled buttons, FAB, selected tab
//   secondary #4F6366  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #F9BE11  — accents and caution markers, used sparingly
//   neutral   #F4EFE6  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF006970),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA0EFF7),
        onPrimaryContainer = Color(0xFF002022),
        inversePrimary = Color(0xFF84D3DB),
        secondary = Color(0xFF4E6265),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD1E7EA),
        onSecondaryContainer = Color(0xFF0A1E21),
        tertiary = Color(0xFF795900),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDF97),
        onTertiaryContainer = Color(0xFF261A00),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFEF9F0),
        onBackground = Color(0xFF1D1C16),
        surface = Color(0xFFFEF9F0),
        onSurface = Color(0xFF1D1C16),
        surfaceVariant = Color(0xFFE8E2D0),
        onSurfaceVariant = Color(0xFF4A4739),
        surfaceDim = Color(0xFFDED9D1),
        surfaceBright = Color(0xFFFEF9F0),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8F3EA),
        surfaceContainer = Color(0xFFF2EDE4),
        surfaceContainerHigh = Color(0xFFECE7DE),
        surfaceContainerHighest = Color(0xFFE7E2D9),
        inverseSurface = Color(0xFF32302A),
        inverseOnSurface = Color(0xFFF5F0E7),
        outline = Color(0xFF7B7768),
        outlineVariant = Color(0xFFCBC6B5),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFF84D3DB),
        onPrimary = Color(0xFF00363B),
        primaryContainer = Color(0xFF004F55),
        onPrimaryContainer = Color(0xFFA0EFF7),
        inversePrimary = Color(0xFF006970),
        secondary = Color(0xFFB5CACD),
        onSecondary = Color(0xFF203336),
        secondaryContainer = Color(0xFF364A4D),
        onSecondaryContainer = Color(0xFFD1E7EA),
        tertiary = Color(0xFFF9BE11),
        onTertiary = Color(0xFF3F2E00),
        tertiaryContainer = Color(0xFF5C4300),
        onTertiaryContainer = Color(0xFFFFDF97),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF14130E),
        onBackground = Color(0xFFE7E2D9),
        surface = Color(0xFF14130E),
        onSurface = Color(0xFFE7E2D9),
        surfaceVariant = Color(0xFF4A4739),
        onSurfaceVariant = Color(0xFFCBC6B5),
        surfaceDim = Color(0xFF14130E),
        surfaceBright = Color(0xFF3B3933),
        surfaceContainerLowest = Color(0xFF0F0E09),
        surfaceContainerLow = Color(0xFF1D1C16),
        surfaceContainer = Color(0xFF21201A),
        surfaceContainerHigh = Color(0xFF2C2A24),
        surfaceContainerHighest = Color(0xFF36352F),
        inverseSurface = Color(0xFFE7E2D9),
        inverseOnSurface = Color(0xFF32302A),
        outline = Color(0xFF959181),
        outlineVariant = Color(0xFF4A4739),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
