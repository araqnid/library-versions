package org.araqnid.libraryversions

import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import org.araqnid.libraryversions.assertions.lessThan
import kotlin.test.Test

class VersionsTest {
    @Test
    fun parser_splits_versions_into_parts() {
        assertThat(
            parseVersion("1.2.3"), equalTo(
                Version(
                    "1.2.3", listOf(
                        VersionPart("", 1, ""),
                        VersionPart("", 2, ""),
                        VersionPart("", 3, "")
                    )
                )
            )
        )
    }

    @Test
    fun versions_compare_component_wise() {
        assertThat(parseVersion("1.2.3"), lessThan(parseVersion("1.2.4")))
    }
}
