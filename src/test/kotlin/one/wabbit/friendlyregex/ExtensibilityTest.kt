package one.wabbit.friendlyregex

import kotlin.test.*
import one.wabbit.friendlyregex.FriendlyRegex as FR

class ExtensibilityTest {

    @Test
    fun extend_with_finance_pack() {
        val finance = mapOf(
            "finance.ticker" to FR.Definition("[A-Z]{1,5}(?:\\.[A-Z]{1,2})?", description = "US-style ticker (BRK.B)"),
            "finance.isin"   to FR.Definition("[A-Z]{2}[A-Z0-9]{9}[0-9]", description = "ISIN shape")
        )
        val cfg = FR.defaultConfig.extend(finance)

        val tick = FR.compile("Bought {finance.ticker} at {digit}+", cfg)
        assertTrue(tick.containsMatchIn("Bought BRK.B at 102"))
        assertTrue(tick.containsMatchIn("Bought AAPL at 250"))
        assertFalse(tick.containsMatchIn("Bought 12345 at 10"))

        val isin = FR.compile("{start}{finance.isin}{end}", cfg)
        assertTrue(isin.matches("US0378331005"))        // Apple
        assertFalse(isin.matches("US037833100X"))       // bad check digit shape
    }

    @Test
    fun toggle_shorthands() {
        val cfg = FR.defaultConfig.copy(
            percentageIsAny = false, // % literal
            hashIsDigit = false      // # literal
        )
        val r = FR.compile("%{3}##", cfg)
        assertTrue(r.matches("%%%##"))
        assertFalse(r.matches("abc12"))
    }
}
