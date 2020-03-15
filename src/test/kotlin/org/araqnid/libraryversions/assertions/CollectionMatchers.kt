package org.araqnid.libraryversions.assertions

val emptyCollection = object : Matcher<Collection<Any?>> {
    override fun match(actual: Collection<Any?>): AssertionResult {
        return if (actual.isEmpty())
            AssertionResult.Match
        else
            AssertionResult.Mismatch("had these items: ${describe(actual)}")
    }

    override val description = "is empty"
    override val negatedDescription = "is not empty"
}

fun <T> containsInOrder(vararg expected: Matcher<T>): Matcher<Collection<T>> {
    if (expected.isEmpty()) {
        return emptyCollection
    }
    return object : Matcher<Collection<T>> {
        override fun match(actual: Collection<T>): AssertionResult {
            if (actual.isEmpty())
                return AssertionResult.Mismatch("was empty")
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
                return AssertionResult.Mismatch("did not have these items: ${describe(expectedIter.asSequence().toList())}")
            }
            if (actualIter.hasNext()) {
                return AssertionResult.Mismatch("had these extra items: ${describe(actualIter.asSequence().toList())}")
            }
            return AssertionResult.Match
        }

        override val description: String = "contains ${describe(expected.toList())}"
        override val negatedDescription: String = "does not contain ${describe(expected.toList())}"
    }
}

fun <T> containsInAnyOrder(vararg expected: Matcher<T>): Matcher<Collection<T>> {
    if (expected.isEmpty()) {
        return emptyCollection
    }
    return object : Matcher<Collection<T>> {
        override fun match(actual: Collection<T>): AssertionResult {
            if (actual.isEmpty())
                return AssertionResult.Mismatch("was empty")
            val remaining = expected.toMutableList()
            val actualIter = actual.iterator()
            var counter = 0
            actual@ while (actualIter.hasNext()) {
                val actualItem = actualIter.next()
                val index = counter++

                val remainingIter = remaining.iterator()
                val mismatches = mutableListOf<Pair<Matcher<T>, AssertionResult.Mismatch>>()
                while (remainingIter.hasNext()) {
                    val matcher = remainingIter.next()
                    when (val match = matcher.match(actualItem)) {
                        AssertionResult.Match -> {
                            remainingIter.remove()
                            continue@actual
                        }
                        is AssertionResult.Mismatch -> {
                            mismatches += matcher to match
                        }
                    }
                }
                return AssertionResult.Mismatch(sequence {
                    yield("item #$index did not match:")
                    for ((matcher, mismatch) in mismatches) {
                        yield(" ${describe(matcher)}: ${mismatch.description}")
                    }
                }.joinToString("\n"))
            }
            if (remaining.isNotEmpty()) {
                return AssertionResult.Mismatch("did not have these items: ${describe(remaining.toList())}")
            }
            if (actualIter.hasNext()) {
                return AssertionResult.Mismatch("had these extra items: ${describe(actualIter.asSequence().toList())}")
            }
            return AssertionResult.Match
        }

        override val description: String = "contains ${describe(expected.toList())}"
        override val negatedDescription: String = "does not contain ${describe(expected.toList())}"
    }
}
