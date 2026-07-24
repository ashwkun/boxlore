package cx.aswin.boxlore.core.designsystem.theme

/**
 * Google Sans Flex **ROND** axis presets for Appearance → Lettering.
 * Pref keys match [cx.aswin.boxlore.core.prefs.UserPreferencesRepository] (`crisp` / `soft` / `round`).
 */
object FontRoundness {
    const val CRISP = "crisp"
    const val SOFT = "soft"
    const val ROUND = "round"

    const val DEFAULT_KEY = SOFT

    const val AXIS_CRISP = 0f
    const val AXIS_SOFT = 50f
    const val AXIS_ROUND = 100f

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

    fun sanitizeKey(key: String?): String {
        val normalized = key?.trim()?.lowercase()
        return when (normalized) {
            CRISP, ROUND -> normalized
            else -> DEFAULT_KEY
        }
    }

    fun axisValue(key: String?): Float =
        when (sanitizeKey(key)) {
            CRISP -> AXIS_CRISP
            ROUND -> AXIS_ROUND
            else -> AXIS_SOFT
        }
}

/**
 * Default Flex roundness (Soft / ROND 50). Prefer [FontRoundness.axisValue] when a pref is available.
 */
const val GoogleSansFlexRoundness = FontRoundness.AXIS_SOFT
