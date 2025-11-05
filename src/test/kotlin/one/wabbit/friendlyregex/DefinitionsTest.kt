package one.wabbit.friendlyregex

import kotlin.test.*
import one.wabbit.friendlyregex.FriendlyRegex as FR

class DefinitionsTest {

    @Test
    fun email_token_is_conservative() {
        val r = FR.compile("{email}")
        assertTrue(r.matches("first.last+tag@sub.domain.io"))
        assertFalse(r.matches("not-an-email@"))
    }

    @Test
    fun url_token_basic() {
        val r = FR.compile("{url}")
        assertTrue(r.matches("https://example.com/path/to/thing"))
        assertTrue(r.matches("example.com"))
        assertFalse(r.matches(":// broken"))
    }

    @Test
    fun ip_address_v4() {
        val r = FR.compile("{ip-address}")
        assertTrue(r.matches("192.168.0.1"))
        assertFalse(r.matches("999.10.10.10"))
    }

    @Test
    fun bool_token() {
        assertTrue(FR.compile("{bool}").matches("TRUE"))
        assertTrue(FR.compile("{bool}").matches("0"))
        assertFalse(FR.compile("{bool}").matches("maybe"))
    }
}
