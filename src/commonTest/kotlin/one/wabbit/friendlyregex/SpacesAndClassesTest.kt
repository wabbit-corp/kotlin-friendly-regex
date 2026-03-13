package one.wabbit.friendlyregex

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import one.wabbit.friendlyregex.FriendlyRegex as FR

class SpacesAndClassesTest {
    @Test
    fun expand_spaces_into_whitespace_runs() {
        val r = FR.compile("A  B  C") // two space runs
        assertTrue(r.containsMatchIn("A     B\tC"))
        assertFalse(r.containsMatchIn("A_B_C"))
    }

    @Test
    fun literal_spaces_when_disabled() {
        val r = FR.compile("A  B", FR.defaultConfig.copy(expandSpaces = false))
        assertTrue(r.containsMatchIn("A  B"))
        assertFalse(r.containsMatchIn("A   B"))
    }

    @Test
    fun native_char_class_does_not_expand_spaces() {
        val r = FR.compile("[A B]{3}")
        // class includes literal space or 'A' or 'B'
        assertTrue(r.containsMatchIn("A B"))
    }

    @Test
    fun wrangle_style_class_positive() {
        val r = FR.compile("ID {[A-F0-9]}{4} ok")
        assertTrue(r.containsMatchIn("ID BEEF    ok"))
        assertFalse(r.containsMatchIn("ID beee ok"))
    }

    @Test
    fun wrangle_style_class_negated() {
        val r = FR.compile("X {![0-9]} Y")
        assertTrue(r.containsMatchIn("X _ Y"))
        assertFalse(r.containsMatchIn("X 5 Y"))
    }

    @Test
    fun wrangle_style_classes_can_be_disabled() {
        val cfg = FR.defaultConfig.copy(supportWrangleStyleClasses = false)
        assertFailsWith<IllegalArgumentException> { FR.compile("HEX: {[A-F0-9]}{2}", cfg) }
    }
}
