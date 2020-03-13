@file:JvmName("VersionResolversJVM")
package org.araqnid.libraryversions

@Suppress("RedundantSuspendModifier")
internal actual suspend fun readTextFile(filename: String): String = filename.reader().readText()
