package cx.aswin.boxlore.core.playback.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansFlexTypeface
import cx.aswin.boxlore.core.prefs.FontRoundnessAxis
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoCollageBadgeTypefaceTest {
    @Test
    fun badgeTypeface_readsCachedRoundnessFromThemeFastCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context
            .getSharedPreferences(FontRoundnessAxis.THEME_FAST_CACHE, Context.MODE_PRIVATE)
            .edit()
            .putString(FontRoundnessAxis.PREF_KEY, FontRoundnessAxis.ROUND)
            .apply()

        val typeface = GoogleSansFlexTypeface.createFromCachedRoundness(context, weight = 700)

        assertNotEquals(
            "Expected bundled Google Sans Flex instead of plain sans-serif fallback",
            android.graphics.Typeface.SANS_SERIF,
            typeface,
        )
    }
}
