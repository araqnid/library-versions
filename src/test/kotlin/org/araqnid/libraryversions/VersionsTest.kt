package org.araqnid.libraryversions

import org.araqnid.kotlin.assertthat.assertThat
import org.araqnid.kotlin.assertthat.equalTo
import org.araqnid.kotlin.assertthat.lessThan
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
