package org.araqnid.libraryversions

private val versionPartPattern = Regex("""([^0-9]*)([0-9]+)([^0-9]?.*)""")

fun parseVersion(input: String): Version {
    val versionParts = input.split('.').mapNotNull { partText ->
        versionPartPattern.matchEntire(partText)?.let { matchResult ->
            val (prefix, numberString, suffix) = matchResult.destructured
            VersionPart(prefix, numberString.toInt(), suffix)
        }
    }
    return Version(input, versionParts)
}

data class VersionPart(val prefix: String, val number: Int, val suffix: String) : Comparable<VersionPart> {
    companion object {
        val comparator: Comparator<VersionPart> = Comparator.comparing(
                        VersionPart::number)
                .thenComparing(VersionPart::suffix)
                .thenComparing(VersionPart::prefix)
    }

    override fun compareTo(other: VersionPart): Int = comparator.compare(this, other)
}

data class Version(val string: String, val parts: List<VersionPart>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        val ourParts = parts
        val theirParts = other.parts
        val compareLength = ourParts.size.coerceAtMost(theirParts.size)
        for (i in 0 until compareLength) {
            val n = ourParts[i].compareTo(theirParts[i])
            if (n != 0) return n
        }
        return ourParts.size - theirParts.size
    }
}
