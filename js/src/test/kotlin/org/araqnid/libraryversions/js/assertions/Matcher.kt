package org.araqnid.libraryversions.js.assertions

import kotlin.reflect.KProperty1

interface Matcher<in T> : SelfDescribing {
    fun match(actual: T): AssertionResult

    val negatedDescription
        get() = "not $description"

    operator fun not(): Matcher<T> = Negation(this)

    fun asPredicate(): (T) -> Boolean = { this.match(it) == AssertionResult.Match }

    companion object {
        operator fun <T> invoke(property: KProperty1<T, Boolean>): Matcher<T> = PredicateMatcher(property.name, property)
        operator fun <T> invoke(name: String, feature: (T) -> Boolean): Matcher<T> = PredicateMatcher(name, feature)
    }
}

fun <T> Matcher<T>.describedBy(fn: () -> String): Matcher<T> {
    return object : Matcher<T> by this {
        override val description: String
            get() = fn()
    }
}

sealed class AssertionResult {
    object Match : AssertionResult() {
        override fun toString() = "Match"
    }

    class Mismatch(override val description: String) : AssertionResult(), SelfDescribing {
        override fun toString() = "Mismatch[${describe(description)}]"
    }
}

val anything = object : Matcher<Any?> {
    override fun match(actual: Any?): AssertionResult = AssertionResult.Match
    override val description = "anything"
}

val isAbsent = object : Matcher<Any?> {
    override fun match(actual: Any?): AssertionResult {
        if (actual == null) return AssertionResult.Match
        return AssertionResult.Mismatch("was: ${describe(actual)}")
    }

    override val description = "null"
}

fun <T : Any> present(valueMatcher: Matcher<T>) = object : Matcher<T?> {
    override fun match(actual: T?): AssertionResult {
        if (actual == null) return AssertionResult.Mismatch("was null")
        return valueMatcher.match(actual)
    }

    override val description: String
        get() = valueMatcher.description
}

fun <T> equalTo(value: T) = object : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        if (actual == value) return AssertionResult.Match
        return AssertionResult.Mismatch("was ${describe(actual)}")
    }

    override val description: String
        get() = "is equal to ${describe(value)}"

    override val negatedDescription: String
        get() = "is not equal to ${describe(value)}"
}

fun <T> isEqualTo(value: T) = object : Matcher<Any?> {
    override fun match(actual: Any?): AssertionResult {
        if (actual == value) return AssertionResult.Match
        return AssertionResult.Mismatch("was ${describe(actual)}")
    }

    override val description: String
        get() = "is equal to ${describe(value)}"

    override val negatedDescription: String
        get() = "is not equal to ${describe(value)}"
}

fun <T> sameInstance(value: T) = object : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        if (actual === value) return AssertionResult.Match
        return AssertionResult.Mismatch("was ${describe(actual)}")
    }

    override val description: String
        get() = "is same instance as ${describe(value)}"

    override val negatedDescription: String
        get() = "is not same instance as ${describe(value)}"
}

infix fun <T> Matcher<T>.or(that: Matcher<T>): Matcher<T> = Disjunction(this, that)
infix fun <T> Matcher<T>.and(that: Matcher<T>): Matcher<T> = Conjunction(this, that)

fun <T> allOf(matchers: List<Matcher<T>>): Matcher<T> = if (matchers.isEmpty()) anything else matchers.reduce { l, r -> l and r }
fun <T> anyOf(matchers: List<Matcher<T>>): Matcher<T> = if (matchers.isEmpty()) anything else matchers.reduce { l, r -> l or r }
fun <T> allOf(vararg matchers: Matcher<T>): Matcher<T> = allOf(matchers.toList())
fun <T> anyOf(vararg matchers: Matcher<T>): Matcher<T> = anyOf(matchers.toList())

private class Negation<in T>(private val negated: Matcher<T>) : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        return when (negated.match(actual)) {
            AssertionResult.Match -> AssertionResult.Mismatch(negatedDescription)
            is AssertionResult.Mismatch -> AssertionResult.Match
        }
    }

    override val description: String
        get() = negated.negatedDescription
    override val negatedDescription: String
        get() = negated.description

    override fun not() = negated
}

private class Disjunction<in T>(private val left: Matcher<T>, private val right: Matcher<T>) : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        val leftResult = left.match(actual)
        if (leftResult == AssertionResult.Match)
            return AssertionResult.Match
        return right.match(actual)
    }

    override val description: String
        get() = "${left.description} or ${right.description}"
}

private class Conjunction<in T>(private val left: Matcher<T>, private val right: Matcher<T>) : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        val leftResult = left.match(actual)
        if (leftResult is AssertionResult.Mismatch)
            return leftResult
        return right.match(actual)
    }

    override val description: String
        get() = "${left.description} and ${right.description}"
}

private class PredicateMatcher<in T>(name: String, private val predicate: (T) -> Boolean) : Matcher<T> {
    override fun match(actual: T): AssertionResult {
        if (predicate(actual)) return AssertionResult.Match
        return AssertionResult.Mismatch("was: ${describe(actual)}")
    }

    override val description = identifierToDescription(name)
    override val negatedDescription = identifierToNegatedDescription(name)
    override fun asPredicate() = predicate

    companion object {
        fun identifierToDescription(id: String): String {
            return identifierToWords(id).joinToString(" ")
        }

        fun identifierToNegatedDescription(id: String): String {
            val words = identifierToWords(id).iterator()
            val first = words.next()
            val rest = words.asSequence().joinToString(" ")

            return when (first) {
                "is" -> "is not $rest"
                "has" -> "does not have $rest"
                else -> "not $first $rest"
            }
        }

        fun identifierToWords(s: String) = sequence {
            val buf = StringBuilder()

            for ((prev, c) in (s[0] + s).zip(s)) {
                if (isWordStart(prev, c)) {
                    if (buf.isNotEmpty()) {
                        yield(buf.toString())
                        buf.clear()
                    }
                }

                if (isWordPart(c)) {
                    buf.append(c.toLowerCase())
                }
            }

            yield(buf.toString())
        }

        fun isWordPart(c: Char): Boolean = c.isLetterOrDigit()

        fun isWordStart(prev: Char, c: Char): Boolean = when {
            c.isLetter() != prev.isLetter() -> true
            prev.isLowerCase() && c.isUpperCase() -> true
            else -> false
        }

        fun Char.isLowerCase() = this in "abcdefghijklmnopqrstuvwxyz"
        fun Char.isUpperCase() = this in "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        fun Char.isLetter() = this.isLowerCase() || this.isUpperCase()
        fun Char.isLetterOrDigit() = this.isLetter() || this in "0123456789"
    }
}

private class FeatureMatcher<T, R>(private val name: String, private val feature: (T) -> R, private val featureMatcher: Matcher<R>) :
        Matcher<T> {
    override fun match(actual: T): AssertionResult {
        val result = featureMatcher.match(feature(actual))
        if (result is AssertionResult.Mismatch) {
            return AssertionResult.Mismatch("had $name that ${result.description}")
        }
        return AssertionResult.Match
    }

    override val description = "has $name that ${featureMatcher.description}"
    override val negatedDescription = "does not have $name that ${featureMatcher.description}"
}

fun <T, R> has(name: String, feature: (T) -> R, featureMatcher: Matcher<R>): Matcher<T> = FeatureMatcher(name, feature, featureMatcher)
fun <T, R> has(property: KProperty1<T, R>, propertyMatcher: Matcher<R>): Matcher<T> =
        has(PredicateMatcher.identifierToDescription(property.name), property, propertyMatcher)
