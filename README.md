# kotlin-friendly-regex

`kotlin-friendly-regex` is a small Kotlin Multiplatform library for writing regular expressions with readable domain tokens instead of raw punctuation-heavy patterns.

It compiles a human-oriented pattern language into a normal Kotlin `Regex`, so you still get the standard Kotlin regex engine and APIs.

## Installation

```kotlin
implementation("one.wabbit:kotlin-friendly-regex:1.1.0")
```

## Quick Start

```kotlin
import one.wabbit.friendlyregex.FriendlyRegex

val email = FriendlyRegex.compile(
    "{start}{alpha}+@gmail.com{end}",
    FriendlyRegex.defaultConfig.copy(caseInsensitive = true),
)

check(email.matches("margo@gmail.com"))
check(!email.matches("margo@example.com"))
```

## Core Ideas

FriendlyRegex supports:
- named tokens like `{digit}`, `{alpha}`, `{email}`, `{url}`, `{delim-ws}`
- shorthand characters like `#` for digit and `%` for any character
- bracketed character classes like `{[abc]}` and `{![abc]}`
- normal regex operators like `+`, `*`, `?`, `|`, `()`
- optional space expansion, where literal spaces become `\\s+`

The result is always a standard Kotlin `Regex`.

## Examples

### Structured dates

```kotlin
val date = FriendlyRegex.compile("{digit}{4}-{digit}{2}-{digit}{2}")
check(date.matches("2026-03-13"))
```

### Delimiter-tolerant extraction

```kotlin
val account = FriendlyRegex.compile("BIN: ({digit}{4}){delim}ACCT: ({digit}{4})")
val match = account.find("BIN: 4111-ACCT: 1234") ?: error("no match")
check(match.groupValues[1] == "4111")
check(match.groupValues[2] == "1234")
```

### Custom domain packs

```kotlin
val finance = mapOf(
    "finance.ticker" to FriendlyRegex.Definition("[A-Z]{1,5}(?:\\.[A-Z]{1,2})?"),
    "finance.isin" to FriendlyRegex.Definition("[A-Z]{2}[A-Z0-9]{9}[0-9]"),
)

val cfg = FriendlyRegex.defaultConfig.extend(finance)
val ticker = FriendlyRegex.compile("Bought {finance.ticker} at {digit}+", cfg)

check(ticker.containsMatchIn("Bought BRK.B at 102"))
```

## Configuration

`FriendlyRegex.Config` lets you control:
- case-insensitive matching
- multiline mode
- dot-matches-newline mode
- whether `%` means `.`
- whether `#` means `\\d`
- whether wrangle-style classes are enabled
- whether spaces expand to `\\s+`
- whether token fragments get wrapped in non-capturing groups

## When To Use It

This library is a good fit when:
- raw regex syntax is making patterns hard to review
- you want shared token vocabularies across a codebase
- you need a few domain-specific pattern packs without building a full parser

It is not a replacement for advanced parser combinators. It is a convenience layer on top of Kotlin `Regex`.
