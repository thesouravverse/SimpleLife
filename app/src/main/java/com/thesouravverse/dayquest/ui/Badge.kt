package com.thesouravverse.dayquest.ui

/**
 * Badge tiers, lowest to highest. The current badge is the highest tier
 * whose [threshold] is <= the user's total XP. Negative XP also maps to
 * the first tier (Sprout) so the UI never breaks.
 */
enum class Badge(
    val threshold: Int,
    val title: String,
    val emoji: String
) {
    SPROUT(0,            "Sprout",          "\uD83C\uDF31"),   // 🌱
    ELITE(100,           "Elite",           "\u26A1"),         // ⚡
    MASTER(1_000,        "Master",          "\uD83D\uDD25"),   // 🔥
    EPIC(10_000,         "Epic",            "\uD83D\uDC8E"),   // 💎
    LEGEND(100_000,      "Legend",          "\uD83D\uDC51"),   // 👑
    MYTHIC(1_000_000,    "Mythic",          "\uD83C\uDF1F"),   // 🌟
    MYTHICAL_GLORY(10_000_000, "Mythical Glory", "\uD83C\uDFC6"); // 🏆

    companion object {
        fun forXp(xp: Int): Badge =
            values().asList().lastOrNull { xp >= it.threshold } ?: SPROUT

        /** Returns the next badge, or null if user is at the top tier. */
        fun nextFor(xp: Int): Badge? {
            val cur = forXp(xp)
            val idx = values().indexOf(cur)
            return values().getOrNull(idx + 1)
        }
    }
}
