package org.araqnid.libraryversions

import libxml2.xmlReadMemory

internal actual suspend fun fetchMavenVersionsFromMetadata(url: String, httpFetcher: HttpFetcher): Collection<String> {
    val content = httpFetcher.getText(url).data

    return xmlReadMemory(content, content.length, null, "utf-8", 0)!!.use { doc ->
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
}
