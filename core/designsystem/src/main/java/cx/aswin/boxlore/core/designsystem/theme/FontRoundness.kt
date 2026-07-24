package cx.aswin.boxlore.core.designsystem.theme

import cx.aswin.boxlore.core.prefs.FontRoundnessAxis

/**
 * Google Sans Flex **ROND** axis presets for Appearance → Lettering.
 * Pref keys match [cx.aswin.boxlore.core.prefs.UserPreferencesRepository] (`crisp` / `soft` / `round`).
 */
object FontRoundness {
    const val CRISP = FontRoundnessAxis.CRISP
    const val SOFT = FontRoundnessAxis.SOFT
    const val ROUND = FontRoundnessAxis.ROUND

    const val DEFAULT_KEY = SOFT

    const val AXIS_CRISP = FontRoundnessAxis.AXIS_CRISP.toFloat()
    const val AXIS_SOFT = FontRoundnessAxis.AXIS_SOFT.toFloat()
    const val AXIS_ROUND = FontRoundnessAxis.AXIS_ROUND.toFloat()

    data class Entry(
        val key: String,
        val label: String,
        val axis: Float,
    )

    val entries =
        listOf(
            Entry(CRISP, "Crisp", AXIS_CRISP),
            Entry(SOFT, "Soft", AXIS_SOFT),
            Entry(ROUND, "Round", AXIS_ROUND),
        )

    fun sanitizeKey(key: String?): String = FontRoundnessAxis.sanitizeKey(key)

    fun axisValue(key: String?): Float = FontRoundnessAxis.axisValue(key).toFloat()
}

/**
 * Default Flex roundness (Soft / ROND 50). Prefer [FontRoundness.axisValue] when a pref is available.
 */
const val GoogleSansFlexRoundness = FontRoundness.AXIS_SOFT
