@file:JvmName("VersionResolversJVM")
package org.araqnid.libraryversions

import java.io.File

@Suppress("RedundantSuspendModifier")
internal actual suspend fun <T> useLinesOfTextFile(filename: String, block: (Sequence<String>) -> T): T =
        File(filename).reader().useLines(block)
