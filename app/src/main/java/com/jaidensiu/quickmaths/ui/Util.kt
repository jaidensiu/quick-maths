package com.jaidensiu.quickmaths.ui

import java.util.Locale

object Util {
    internal fun formatElapsedTime(elapsedTimeMs: Long): String {
        val minutes = elapsedTimeMs / 60_000
        val seconds = (elapsedTimeMs % 60_000) / 1000.0
        return if (minutes > 0) {
            String.format(
                locale = Locale.US,
                format = "%d:%05.2f",
                args = arrayOf<Any>(minutes, seconds),
            )
        } else {
            String.format(
                locale = Locale.US,
                format = "%.2fs",
                args = arrayOf(seconds),
            )
        }
    }
}
