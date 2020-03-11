package org.araqnid.libraryversions.js.assertions

import kotlin.test.fail

fun <T> assertThat(actual: T, criteria: Matcher<T>, message: () -> String = ::noMessage) {
    val judgement = criteria.match(actual)
    if (judgement is AssertionResult.Mismatch) {
        fail(message().let { if (it.isEmpty()) "" else "$it: " } +
                "expected: a value that ${describe(criteria)}\n" +
                "but ${describe(judgement)}")
    }
}

private fun noMessage() = ""
