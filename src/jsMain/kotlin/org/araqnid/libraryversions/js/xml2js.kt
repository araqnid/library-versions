@file:JsModule("xml2js")

package org.araqnid.libraryversions.js

import kotlin.js.Promise

external interface XmlOptions {
    var trim: Boolean
    var explicitRoot: Boolean
    var explicitArray: Boolean
}

external fun parseStringPromise(data: String, options: XmlOptions = definedExternally): Promise<dynamic>

external class Parser(options: XmlOptions = definedExternally) {
    fun parseStringPromise(data: String): Promise<dynamic>
}
