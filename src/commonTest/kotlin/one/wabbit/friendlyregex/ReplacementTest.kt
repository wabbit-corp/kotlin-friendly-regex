package one.wabbit.friendlyregex

import kotlin.test.Test
import kotlin.test.assertEquals
import one.wabbit.friendlyregex.FriendlyRegex as FR

class ReplacementTest {
    @Test
    fun mask_credit_card_last4_kept() {
        // Pattern: 4-4-4-4 digits with any separators; capture last 4
        val pat = "{digit}{4}{any}{digit}{4}{any}{digit}{4}{any}({digit}{4})"
        val r = FR.compile(pat)
        val masked = r.replace("4111-1111-1111-1234", "XXXX-XXXX-XXXX-$1")
        assertEquals("XXXX-XXXX-XXXX-1234", masked)
    }

    @Test
    fun normalize_delim_ws() {
        val r = FR.compile("{alpha}+{delim-ws}{digit}+")
        val out =
            r.replace("ABC :   42", { mr -> mr.value.replace(Regex("\\s*[:|/.,-]\\s*"), ":") })
        assertEquals("ABC:42", out)
    }
}
