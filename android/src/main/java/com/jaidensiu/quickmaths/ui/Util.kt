package com.jaidensiu.quickmaths.ui

import java.util.Locale

object Util {
    /**
     * Formats a duration as `SS.cc s` under a minute and `M:SS.cc` above. Rounds to centiseconds
     * as an integer first so the carry into minutes is exact (no `60.00s` or `1:60.00`).
     */
    internal fun formatElapsedTime(elapsedTimeMs: Long): String {
        val totalCentis = (elapsedTimeMs.coerceAtLeast(minimumValue = 0L) + 5L) / 10L
        val minutes = totalCentis / 6_000L
        val seconds = totalCentis % 6_000L / 100L
        val centis = totalCentis % 100L
        return if (minutes > 0) {
            String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centis)
        } else {
            String.format(Locale.US, "%d.%02ds", seconds, centis)
        }
    }
}
