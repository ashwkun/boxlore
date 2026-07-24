package cx.aswin.boxlore.core.prefs

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.core.content.res.ResourcesCompat

/** Shared Android [Typeface] loader for bundled Google Sans Flex (ROND axis). */
object GoogleSansFlexTypeface {
    fun create(
        context: Context,
        weight: Int,
        roundness: Int,
        italic: Boolean = false,
    ): Typeface {
        val fontResId = resolveFontResId(context)
        if (fontResId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createWithVariation(context, fontResId, weight, roundness, italic)?.let { return it }
        }
        if (fontResId != 0) {
            createFromResource(context, fontResId, weight, italic)?.let { return it }
        }
        return Typeface.create(Typeface.SANS_SERIF, styleForWeight(weight, italic))
    }

    fun createFromCachedRoundness(
        context: Context,
        weight: Int,
        italic: Boolean = false,
    ): Typeface = create(context, weight, FontRoundnessAxis.cachedAxisValue(context), italic)

    private fun createWithVariation(
        context: Context,
        fontResId: Int,
        weight: Int,
        roundness: Int,
        italic: Boolean,
    ): Typeface? =
        try {
            val font =
                Font.Builder(context.resources, fontResId)
                    .setFontVariationSettings("'ROND' $roundness")
                    .setWeight(weight.coerceIn(1, 1000))
                    .setSlant(
                        if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT,
                    )
                    .build()
            val family = FontFamily.Builder(font).build()
            Typeface.CustomFallbackBuilder(family).build()
        } catch (_: Exception) {
            null
        }

    private fun createFromResource(
        context: Context,
        fontResId: Int,
        weight: Int,
        italic: Boolean,
    ): Typeface? =
        ResourcesCompat.getFont(context, fontResId)?.let { base ->
            Typeface.create(base, styleForWeight(weight, italic))
        }

    private fun styleForWeight(
        weight: Int,
        italic: Boolean,
    ): Int =
        when {
            italic && weight >= 600 -> Typeface.BOLD_ITALIC
            italic -> Typeface.ITALIC
            weight >= 600 -> Typeface.BOLD
            else -> Typeface.NORMAL
        }

    private fun resolveFontResId(context: Context): Int =
        context.resources.getIdentifier(
            "google_sans_flex_variable",
            "font",
            context.packageName,
        )
}
