package org.araqnid.libraryversions.assertions

fun <T> containsInOrder(vararg expected: Matcher<T>): Matcher<Collection<T>> {
    return object : Matcher<Collection<T>> {
        override fun match(actual: Collection<T>): AssertionResult {
            val expectedIter = expected.iterator()
            val actualIter = actual.iterator()
            var index = 0
            while (expectedIter.hasNext() && actualIter.hasNext()) {
                val itemMatcher = expectedIter.next()
                val actualItem = actualIter.next()
                val itemMatch = itemMatcher.match(actualItem)
                if (itemMatch is AssertionResult.Mismatch) {
                    return AssertionResult.Mismatch("item at #$index ${itemMatch.description}")
                }
                ++index
            }
            if (expectedIter.hasNext()) {
                return AssertionResult.Mismatch("did not have these items: " + expectedIter.asSequence().joinToString(", ") { describe(it) })
            }
            if (actualIter.hasNext()) {
                return AssertionResult.Mismatch("had these extra items: " + actualIter.asSequence().joinToString(", ") { describe(it) })
            }
            return AssertionResult.Match
        }

        override val description: String = "collection containing " + expected.joinToString(",") { describe(it) }
    }
}
