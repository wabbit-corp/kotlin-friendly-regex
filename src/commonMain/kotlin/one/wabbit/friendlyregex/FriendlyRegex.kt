package one.wabbit.friendlyregex

/**
 * FriendlyRegex — a human-first pattern language that compiles to Kotlin Regex.
 *
 * Core ideas:
 * - Tokens like {digit}, {alpha}, {email}, {delim-ws}, {start}/{end}
 * - Shorthands: # -> digit, % -> any (configurable)
 * - Wrangle-style classes: {[abc]} and {![abc]} -> char classes / negated (configurable)
 * - Quantifiers after tokens: {digit}{4}, {email}+ etc.
 * - Optional "space expansion": literal spaces become \s+ (once per run)
 *
 * Nothing magical at runtime: this produces a normal Regex with chosen options.
 */
object FriendlyRegex {
    // --- Public surface --------------------------------------------------------

    data class Definition(
        val pattern: String,
        val validator: ((String) -> Boolean)? = null, // kept for future extract/validate helpers
        val description: String? = null,
    )

    data class Config(
        val definitions: Map<String, Definition> = defaultDefinitions(),
        val percentageIsAny: Boolean = true, // % -> .
        val hashIsDigit: Boolean = true, // # -> \d
        val supportWrangleStyleClasses: Boolean = true, // {[...]} and {![...]}
        val expandSpaces: Boolean = true, // runs of spaces -> \s+
        val caseInsensitive: Boolean = false,
        val dotMatchesNewline: Boolean = false,
        val multiline: Boolean = false,
        val wrapTokensInNonCapturingGroups: Boolean = true, // (?:...) around token fragments
    ) {
        /** Return a new Config with merged definitions (new ones win). */
        fun extend(newDefinitions: Map<String, Definition>): Config =
            copy(definitions = definitions + newDefinitions)
    }

    /** Default config (lazy so it doesn’t cost you until used). */
    val defaultConfig: Config by lazy { Config() }

    /** Compile a FriendlyRegex pattern into a Kotlin Regex. */
    fun compile(pattern: String, config: Config = defaultConfig): Regex {
        val rx = toRegexString(pattern, config)
        val flags =
            buildString {
                if (config.caseInsensitive) append('i')
                if (config.dotMatchesNewline) append('s')
                if (config.multiline) append('m')
            }
        return if (flags.isEmpty()) Regex(rx) else Regex("(?$flags)$rx")
    }

    // Optional but handy for debugging.
    fun toRegexString(pattern: String, config: Config = defaultConfig): String =
        Compiler.compileToRegexString(pattern, config)

    // --- Implementation --------------------------------------------------------

    private object Compiler {
        private val quantifierRe = Regex("^\\d+(?:,\\d*)?$") // {4}, {2,}, {2,5}

        fun compileToRegexString(src: String, cfg: Config): String {
            val out = StringBuilder()
            var i = 0
            val n = src.length

            fun error(msg: String): Nothing =
                throw IllegalArgumentException("$msg at index $i in pattern: $src")

            fun takeBraced(): String {
                val end = src.indexOf('}', startIndex = i + 1)
                if (end < 0) error("Unclosed '{'")
                val inside = src.substring(i + 1, end)
                i = end + 1
                return inside
            }

            fun copyCharClass() {
                // Copy a [...] class verbatim until the matching ] (handles escaping).
                out.append('[')
                i++ // consume '['
                var closed = false
                while (i < n) {
                    val c = src[i]
                    out.append(c)
                    i++
                    if (c == '\\') {
                        if (i < n) {
                            out.append(src[i])
                            i++
                        } else {
                            error("Dangling escape in character class")
                        }
                    } else if (c == ']') {
                        closed = true
                        break
                    }
                }
                if (!closed) error("Unclosed character class '['")
            }

            // Collapse runs of spaces -> \s+ (when enabled)
            fun consumeSpaceRun() {
                // We are sitting on at least one ' '
                while (i < n && src[i] == ' ') i++
                out.append("\\s+")
            }

            while (i < n) {
                when (val ch = src[i]) {
                    // --- Braced forms: {token} | {4} | {2,5} | {[...]} | {![...]} | {start}/{end}
                    '{' -> {
                        val inside = takeBraced()
                        when {
                            // Numeric quantifier in braces
                            quantifierRe.matches(inside) -> {
                                out.append('{').append(inside).append('}')
                            }

                            // Wrangle-style classes: {[...]} and {![...]}
                            cfg.supportWrangleStyleClasses &&
                                (inside.startsWith("[") || inside.startsWith("![")) -> {
                                val neg = inside.startsWith("![")
                                val body = if (neg) inside.substring(2) else inside.substring(1)
                                require(body.endsWith("]")) { "Bad class in {$inside}" }
                                val content = body.substring(0, body.length - 1)
                                out.append(if (neg) "[^" else "[").append(content).append("]")
                            }

                            // Anchors
                            inside == "start" -> out.append('^')
                            inside == "end" -> out.append('$')

                            // Token lookup
                            else -> {
                                val def =
                                    cfg.definitions[inside]
                                        ?: throw IllegalArgumentException(
                                            "Unknown token {$inside} in pattern: $src"
                                        )
                                val frag = def.pattern
                                if (cfg.wrapTokensInNonCapturingGroups && needsGrouping(frag)) {
                                    out.append("(?:").append(frag).append(")")
                                } else {
                                    out.append(frag)
                                }
                            }
                        }
                    }

                    // --- Native char class: copy verbatim so spaces inside aren't expanded
                    '[' -> copyCharClass()

                    // --- Escapes: pass through (lets you do \{ to mean a literal brace, etc.)
                    '\\' -> {
                        if (i + 1 >= n) error("Dangling backslash")
                        out.append('\\').append(src[i + 1])
                        i += 2
                    }

                    // --- Shorthands
                    '#' -> {
                        out.append(if (cfg.hashIsDigit) "\\d" else "#")
                        i++
                    }
                    '%' -> {
                        out.append(if (cfg.percentageIsAny) "." else "%")
                        i++
                    }

                    // --- Space(s)
                    ' ' -> {
                        if (cfg.expandSpaces) {
                            consumeSpaceRun()
                        } else {
                            out.append(' ')
                            i++
                        }
                    }

                    // --- Pass-through regex operators that users might want explicitly
                    '(',
                    ')',
                    '|',
                    '.',
                    '+',
                    '*',
                    '?',
                    '^',
                    '$' -> {
                        out.append(ch)
                        i++
                    }

                    // --- Default: escape to be literal
                    else -> {
                        out.append(Regex.escape(ch.toString()))
                        i++
                    }
                }
            }

            return out.toString()
        }

        // Heuristic: wrap tokens unless they already look like a single atom
        private fun needsGrouping(frag: String): Boolean {
            if (frag.isEmpty()) return false
            // Already a single char or a class or an atomic group
            val first = frag.first()
            if (frag.length == 1 && first != '\\') return false
            if (first == '[' && frag.last() == ']') return false
            if (
                frag.startsWith("(?:") ||
                    frag.startsWith("(?=") ||
                    frag.startsWith("(?<=") ||
                    frag.startsWith("(?!") ||
                    frag.startsWith("(?<!") ||
                    frag.startsWith("(?i:") ||
                    frag.startsWith("(?-i:") ||
                    frag.startsWith("(?s:") ||
                    frag.startsWith("(?-s:")
            ) {
                return false
            }
            return true
        }
    }

    // --- Defaults --------------------------------------------------------------

    private fun defaultDefinitions(): Map<String, Definition> = buildMap {
        // Primitives
        put("digit", Definition("\\d", description = "0-9"))
        put("any", Definition(".", description = "any char once"))

        put("alpha", Definition("[A-Za-z_]", description = "letters plus underscore"))
        put("upper", Definition("[A-Z_]", description = "uppercase letters plus underscore"))
        put("lower", Definition("[a-z_]", description = "lowercase letters plus underscore"))
        put("alpha-numeric", Definition("[A-Za-z0-9]"))

        // Delimiters (common punctuation or whitespace)
        put("delim", Definition("[\\s:|/.,-]"))
        put("delim-ws", Definition("\\s*[:|/.,-]\\s*"))

        // Types (conservative)
        put("email", Definition("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"))
        put(
            "url",
            Definition(
                "(?:https?://)?[A-Za-z0-9.-]+(?::\\d+)?(?:/[A-Za-z0-9._~!$&'()*+,;=:@%/-]*)?"
            ),
        )
        put(
            "phone",
            Definition("(?:\\+1[\\s.-]?)?(?:\\(\\d{3}\\)|\\d{3})[\\s.-]?\\d{3}[\\s.-]?\\d{4}"),
        )
        put(
            "ip-address",
            Definition(
                "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)"
            ),
        )
        put("bool", Definition("(?i:true|false|t|f|yes|no|1|0)"))

        // Date-ish helpers (composed with {yyyy}{delim}{MM}{delim}{dd}, etc.)
        put(
            "month",
            Definition(
                "(?i:January|February|March|April|May|June|July|August|September|October|November|December)"
            ),
        )
        put("month-abbrev", Definition("(?i:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)"))
        put(
            "dayofweek",
            Definition("(?i:Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)"),
        )
        put("dayofweek-abbrev", Definition("(?i:Sun|Mon|Tue|Wed|Thu|Fri|Sat)"))
        put("utcoffset", Definition("(?:[+-]\\d{4}|Z)"))

        // You can obviously add more as you like (uuid, hex, etc.)
        put(
            "uuid",
            Definition(
                "[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[1-5][A-Fa-f0-9]{3}-[89ABab][A-Fa-f0-9]{3}-[A-Fa-f0-9]{12}"
            ),
        )
    }
}
