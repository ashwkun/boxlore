package cx.aswin.boxlore.core.designsystem.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import cx.aswin.boxlore.core.designsystem.R

/** Android [Typeface] loader for Google Sans Flex with a ROND axis value. */
object GoogleSansTypefaces {
    fun create(
        context: Context,
        style: Int = Typeface.NORMAL,
        roundness: Float = GoogleSansFlexRoundness,
    ): Typeface {
        val italic = style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC
        val weight =
            when (style) {
                Typeface.BOLD, Typeface.BOLD_ITALIC -> 700
                else -> 400
            }
        return create(context, weight = weight, italic = italic, roundness = roundness)
    }

    fun create(
        context: Context,
        weight: Int,
        italic: Boolean = false,
        roundness: Float = GoogleSansFlexRoundness,
    ): Typeface {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val font =
                    Font.Builder(context.resources, R.font.google_sans_flex_variable)
                        .setFontVariationSettings("'ROND' ${roundness.toInt()}")
                        .setWeight(weight.coerceIn(1, 1000))
                        .setSlant(
                            if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT,
                        )
                        .build()
                val family = FontFamily.Builder(font).build()
                return Typeface.CustomFallbackBuilder(family).build()
            } catch (_: Exception) {
                // Fall through.
            }
        }
        val base =
            ResourcesCompat.getFont(context, R.font.google_sans_flex_variable)
                ?: return Typeface.DEFAULT
        val style =
            when {
                italic && weight >= 600 -> Typeface.BOLD_ITALIC
                italic -> Typeface.ITALIC
                weight >= 600 -> Typeface.BOLD
                else -> Typeface.NORMAL
            }
        return Typeface.create(base, style)
    }

    /** Reads lettering roundness from the theme fast-cache SharedPreferences. */
    fun cachedRoundness(context: Context): Float {
        val prefs = context.getSharedPreferences("boxlore_theme_fast_cache", Context.MODE_PRIVATE)
        return FontRoundness.axisValue(prefs.getString("font_roundness", null))
    }
}
