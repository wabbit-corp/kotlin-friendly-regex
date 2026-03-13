# Module kotlin-friendly-regex

Human-readable regex construction for Kotlin Multiplatform.

`FriendlyRegex` turns a small token language into ordinary Kotlin `Regex` values. The goal is to keep patterns reviewable without introducing a separate runtime matcher or parser engine.

## Example

```kotlin
import one.wabbit.friendlyregex.FriendlyRegex

val cfg = FriendlyRegex.defaultConfig.copy(caseInsensitive = true)
val pattern = FriendlyRegex.compile("{start}{alpha}+@gmail.com{end}", cfg)

check(pattern.matches("margo@gmail.com"))
```

## Domain Extensions

```kotlin
val finance = mapOf(
    "finance.ticker" to FriendlyRegex.Definition("[A-Z]{1,5}(?:\\.[A-Z]{1,2})?"),
)

val cfg = FriendlyRegex.defaultConfig.extend(finance)
val ticker = FriendlyRegex.compile("Bought {finance.ticker} at {digit}+", cfg)
```

## Notable Features

- named tokens such as `{digit}`, `{alpha}`, `{email}`, and `{url}`
- configurable shorthands for `#` and `%`
- wrangle-style classes like `{[abc]}` and `{![abc]}`
- optional whitespace expansion for literal spaces
- normal Kotlin `Regex` return values, so matching and extraction stay standard

## API Notes

- Use [one.wabbit.friendlyregex.FriendlyRegex.compile] for a ready-to-use `Regex`.
- Use [one.wabbit.friendlyregex.FriendlyRegex.toRegexString] when you want the raw generated pattern for debugging or logging.
- Use [one.wabbit.friendlyregex.FriendlyRegex.Config.extend] to layer domain-specific tokens on top of the defaults.
