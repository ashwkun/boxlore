package cx.aswin.boxlore.core.designsystem.theme

import android.os.Build
import android.util.Log
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import cx.aswin.boxlore.core.designsystem.R

private const val TAG = "BoxLoreTypography"

// Google Fonts Provider for dynamic font loading
private val googleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

@OptIn(ExperimentalTextApi::class)
fun buildGoogleSansFamily(roundness: Float): FontFamily =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(
            flexFace(weight = 300, opsz = 17f, roundness = roundness, fontWeight = FontWeight.Light),
            flexFace(weight = 400, opsz = 17f, roundness = roundness, fontWeight = FontWeight.Normal),
            flexFace(weight = 500, opsz = 17f, roundness = roundness, fontWeight = FontWeight.Medium),
            flexFace(weight = 600, opsz = 17f, roundness = roundness, fontWeight = FontWeight.SemiBold),
            flexFace(weight = 700, opsz = 17f, roundness = roundness, fontWeight = FontWeight.Bold),
        )
    } else {
        FontFamily(Font(R.font.google_sans_flex_variable))
    }

@OptIn(ExperimentalTextApi::class)
fun buildSectionHeaderFontFamily(roundness: Float): FontFamily =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(
            flexFace(weight = 800, opsz = 24f, roundness = roundness, fontWeight = FontWeight.ExtraBold),
        )
    } else {
        FontFamily(Font(R.font.google_sans_flex_variable))
    }

@OptIn(ExperimentalTextApi::class)
fun buildCondensedGoogleSansFamily(roundness: Float): FontFamily =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(
            Font(
                R.font.google_sans_flex_variable,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(700),
                        FontVariation.Setting("wdth", 75f),
                        FontVariation.Setting("ROND", roundness),
                    ),
                weight = FontWeight.Bold,
            ),
        )
    } else {
        FontFamily(Font(R.font.google_sans_flex_variable))
    }

fun buildBoxLoreTypography(roundness: Float): Typography {
    val family = buildGoogleSansFamily(roundness)
    return Typography(
        displayLarge =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 57.sp,
                lineHeight = 60.sp,
                letterSpacing = (-0.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 45.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.3).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.25).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.3).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.2).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.1).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.3.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.4.sp,
            ),
    )
}

/** Soft (ROND 50) family — use [buildGoogleSansFamily] / [LocalFontRoundness] when prefs matter. */
val GoogleSansFamily: FontFamily = buildGoogleSansFamily(GoogleSansFlexRoundness)

/** Soft section-header family — prefer [rememberSectionHeaderFontFamily] in Compose. */
val SectionHeaderFontFamily: FontFamily = buildSectionHeaderFontFamily(GoogleSansFlexRoundness)

/** Soft Material 3 scale — prefer [buildBoxLoreTypography] via [BoxLoreTheme]. */
val BoxLoreTypography: Typography = buildBoxLoreTypography(GoogleSansFlexRoundness)

@Composable
fun rememberSectionHeaderFontFamily(): FontFamily {
    val roundness = LocalFontRoundness.current
    return remember(roundness) { buildSectionHeaderFontFamily(roundness) }
}

@Composable
fun rememberCondensedGoogleSansFamily(): FontFamily {
    val roundness = LocalFontRoundness.current
    return remember(roundness) { buildCondensedGoogleSansFamily(roundness) }
}

// Keep Roboto Flex for legacy/fallback or specific variable axes needs
private val robotoFlex = GoogleFont("Roboto Flex")
val RobotoFlexFamily =
    FontFamily(
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Light),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Bold),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
        Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Black),
    ).also { Log.d(TAG, "Roboto Flex loaded via Google Fonts provider") }

// Logo Font with Variable Axes - Using bundled TTF for full axis control
@OptIn(ExperimentalTextApi::class)
val LogoFontFamily =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(
            Font(
                R.font.robotoflex_variable,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(700),
                        FontVariation.width(110f),
                        FontVariation.Setting("GRAD", 50f),
                        FontVariation.Setting("opsz", 48f),
                    ),
            ),
        )
    } else {
        RobotoFlexFamily
    }

@OptIn(ExperimentalTextApi::class)
private fun flexFace(
    weight: Int,
    opsz: Float,
    roundness: Float,
    fontWeight: FontWeight,
): Font =
    Font(
        R.font.google_sans_flex_variable,
        variationSettings =
            FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.Setting("opsz", opsz),
                FontVariation.Setting("ROND", roundness),
            ),
        weight = fontWeight,
    )
