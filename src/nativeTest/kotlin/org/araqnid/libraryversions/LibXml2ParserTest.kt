package org.araqnid.libraryversions

import libxml2.xmlReadMemory
import org.araqnid.libraryversions.assertions.assertThat
import org.araqnid.libraryversions.assertions.equalTo
import kotlin.test.Test

class LibXml2ParserTest {
    @Test
    fun `walk through elements in tree and gets Maven artifact versions`() {
        val versions = xmlReadMemory(exampleMetadataXml, exampleMetadataXml.length, null, "utf-8", 0)!!.use { doc ->
            doc.rootElement
                    .firstChild("versioning")
                    ?.firstChild("versions")
                    ?.children()
                    ?.mapNotNull { node ->
                        if (node.isElement && node.name == "version") {
                            node.text()
                        }
                        else {
                            null
                        }
                    } ?: emptyList()
        }
        assertThat(versions, equalTo(listOf("3.11.3", "3.11.4")))
    }

    companion object {
        val exampleMetadataXml =
                """<metadata>
                  | <groupId>org.example.app</groupId>
                  | <artifactId>example-app-module</artifactId>
                  | <versioning>
                  |  <latest>3.11.4</latest>
                  |  <release>3.11.4</release>
                  |  <versions>
                  |   <version>3.11.3</version>
                  |   <version>3.11.4</version>
                  |  </versions>
                  | </versioning>
                  |</metadata>""".trimMargin()
    }
}
