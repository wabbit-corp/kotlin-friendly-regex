package one.wabbit.friendlyregex

import kotlin.test.*
import one.wabbit.friendlyregex.FriendlyRegex as FR

class OptionsTest {

    @Test
    fun case_insensitive() {
        val r = FR.compile("{start}{alpha}+@gmail.com{end}",
            FR.defaultConfig.copy(caseInsensitive = true))
        assertTrue(r.matches("FOO_BAR@gmail.com"))
    }

    @Test
    fun dot_matches_newline_affects_percent() {
        // With the default config, % maps to '.'; DOT_MATCHES_ALL makes it cross newlines
        val cfg = FR.defaultConfig.copy(dotMatchesNewline = true)
        val r = FR.compile("A%Z", cfg)
        assertTrue(r.containsMatchIn("A\nZ"))

        val cfg2 = FR.defaultConfig.copy(dotMatchesNewline = false)
        val r2 = FR.compile("A%Z", cfg2)
        assertFalse(r2.containsMatchIn("A\nZ"))
    }

    @Test
    fun multiline_anchors() {
        val text = "xx\nSTART\nEND\nyy"
        val r = FR.compile("{start}START{end}", FR.defaultConfig.copy(multiline = true))
        assertTrue(r.containsMatchIn(text))
    }
}
