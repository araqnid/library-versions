package org.araqnid.libraryversions.js.assertions

fun <T> producesSequence(vararg items: T): Matcher<Sequence<T>> {
    return object : Matcher<Sequence<T>> {
        override fun match(actual: Sequence<T>): AssertionResult {
            val expectedIterator = items.iterator()
            val actualIterator = actual.iterator()
            var index = 0
            while (expectedIterator.hasNext() && actualIterator.hasNext()) {
                val expectedItem = expectedIterator.next()
                val actualItem = actualIterator.next()
                if (expectedItem != actualItem) {
                    return AssertionResult.Mismatch("at #$index, produced ${describe(actualItem)} instead of ${describe(expectedItem)}")
                }
                ++index
            }
            if (expectedIterator.hasNext()) {
                return AssertionResult.Mismatch("expected more than $index items: ${describe(expectedIterator.asSequence().toList())}")
            }
            if (actualIterator.hasNext()) {
                return AssertionResult.Mismatch("produced more than $index items: ${describe(actualIterator.asSequence().toList())}")
            }
            return AssertionResult.Match
        }

        override val description: String = "sequence producing ${describe(items.toList())}"
    }
}

val producesEmptySequence = producesSequence<Any?>()
