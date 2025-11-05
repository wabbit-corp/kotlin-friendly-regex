package one.wabbit.friendlyregex

import kotlin.test.*
import one.wabbit.friendlyregex.FriendlyRegex as FR

class BasicsTest {

    @Test
    fun anchors_and_simple_tokens() {
        val r = FR.compile("{start}{alpha}+@gmail.com{end}",
            FR.defaultConfig.copy(caseInsensitive = true))

        assertTrue(r.matches("margo@gmail.com"))
        assertFalse(r.matches("X margo_9@gmail.com Y"))
    }

    @Test
    fun shorthands_hash_and_percent() {
        val r = FR.compile("#{4} %{3}") // # -> \d, % -> .
        assertTrue(r.containsMatchIn("1234 abc"))
        assertTrue(r.containsMatchIn("5678 XYZ"))
    }

    @Test
    fun quantifier_after_token() {
        val r = FR.compile("{digit}{4}-{digit}{2}-{digit}{2}")
        assertTrue(r.matches("2025-09-24"))
        assertFalse(r.matches("25-09-24"))
    }

    @Test
    fun numeric_braced_quantifier() {
        val r = FR.compile("A({alpha}){2}Z")
        assertTrue(r.matches("AaaZ"))
        assertTrue(r.matches("A_AZ"))
        assertFalse(r.matches("AaZ"))
    }

    @Test
    fun groups_and_captures() {
        val r = FR.compile("BIN: ({digit}{4}){delim}ACCT: ({digit}{4})")
        val m = r.find("BIN: 4111-ACCT: 1234")
        assertNotNull(m)
        assertEquals("4111", m.groupValues[1])
        assertEquals("1234", m.groupValues[2])
    }
}
