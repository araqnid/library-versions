package org.araqnid.libraryversions.assertions

fun <N : Comparable<N>> lessThan(expected: N) = object : Matcher<N> {
    override fun match(actual: N): AssertionResult {
        return if (actual < expected) AssertionResult.Match
        else AssertionResult.Mismatch("was: ${describe(actual)}")
    }

    override val description = "less than ${describe(expected)}"
}

fun <N : Comparable<N>> greaterThan(expected: N) = object : Matcher<N> {
    override fun match(actual: N): AssertionResult {
        return if (actual > expected) AssertionResult.Match
        else AssertionResult.Mismatch("was: ${describe(actual)}")
    }

    override val description = "greater than ${describe(expected)}"
}
