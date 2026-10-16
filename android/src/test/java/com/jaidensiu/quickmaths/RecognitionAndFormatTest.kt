package com.jaidensiu.quickmaths

import com.jaidensiu.quickmaths.data.asNumberOrNull
import com.jaidensiu.quickmaths.ui.Util
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecognitionAndFormatTest {
    @Test
    fun readsHandwrittenNumbers() {
        assertEquals("12", "l2".asNumberOrNull())
        assertEquals("12", "1 2".asNumberOrNull())
        assertEquals("0", "O".asNumberOrNull())
        assertEquals("15", "ls".asNumberOrNull())
        // Kept as written, so it never equals an integer answer.
        assertEquals("4.0", "4.0".asNumberOrNull())
        assertNull("abc".asNumberOrNull())
    }

    @Test
    fun formatsElapsedTime() {
        assertEquals("1.00s", Util.formatElapsedTime(elapsedTimeMs = 999))
        assertEquals("59.99s", Util.formatElapsedTime(elapsedTimeMs = 59_994))
        // Rounds before splitting, so it carries into the minute instead of showing 60.00s.
        assertEquals("1:00.00", Util.formatElapsedTime(elapsedTimeMs = 59_995))
        assertEquals("0.00s", Util.formatElapsedTime(elapsedTimeMs = -5))
    }
}
