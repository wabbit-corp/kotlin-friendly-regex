package one.wabbit.friendlyregex

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import one.wabbit.friendlyregex.FriendlyRegex as FR

class ErrorsTest {
    @Test
    fun unknown_token() {
        val ex = assertFailsWith<IllegalArgumentException> { FR.compile("Hello {snakes}!") }
        assertTrue(ex.message!!.contains("Unknown token"))
    }

    @Test
    fun unclosed_brace() {
        assertFailsWith<IllegalArgumentException> { FR.compile("abc{digit") }
    }

    @Test
    fun dangling_backslash() {
        assertFailsWith<IllegalArgumentException> { FR.compile("abc\\") }
    }

    @Test
    fun unclosed_char_class() {
        assertFailsWith<IllegalArgumentException> { FR.compile("[A-Z") }
    }

    @Test
    fun bad_wrangle_class() {
        assertFailsWith<IllegalArgumentException> { FR.compile("{![A-Z}") }
    }
}
