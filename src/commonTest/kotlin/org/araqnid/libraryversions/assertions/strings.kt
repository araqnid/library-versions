package org.araqnid.libraryversions.assertions

fun contains(pattern: Regex) = object : Matcher<CharSequence> {
    override fun match(actual: CharSequence): AssertionResult {
        return if (actual.contains(pattern)) AssertionResult.Match
        else AssertionResult.Mismatch("was ${describe(actual)}")
    }

    override val description: String = "matches ${pattern.pattern}"
    override val negatedDescription: String = "does not match ${pattern.pattern}"
}
