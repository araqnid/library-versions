package org.araqnid.libraryversions

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import libxml2.XML_ELEMENT_NODE
import libxml2.XML_TEXT_NODE
import libxml2.xmlCharVar
import libxml2.xmlDocGetRootElement
import libxml2.xmlDocPtr
import libxml2.xmlElementType
import libxml2.xmlFreeDoc
import libxml2.xmlNodePtr

fun CPointer<xmlCharVar>.toKString() = reinterpret<ByteVar>().toKString()

val xmlNodePtr.name: String?
    get() = pointed.name?.toKString()
val xmlNodePtr.type: xmlElementType
    get() = pointed.type

val xmlNodePtr.isElement: Boolean
    get() = type == XML_ELEMENT_NODE
val xmlNodePtr.isText: Boolean
    get() = type == XML_TEXT_NODE

fun xmlNodePtr.children() = Iterable {
    object : AbstractIterator<xmlNodePtr>() {
        private var cur: xmlNodePtr? = pointed.children

        override fun computeNext() {
            val emit = cur
            if (emit != null) {
                cur = emit.pointed.next
                setNext(emit)
            } else {
                done()
            }
        }
    }
}

fun xmlNodePtr.childrenSeq() = generateSequence(pointed.children) { node -> node.pointed.next }

inline fun xmlNodePtr.forEachChild(action: (xmlNodePtr) -> Unit) {
    var cur = pointed.children
    while (cur != null) {
        action(cur)
        cur = cur.pointed.next
    }
}

fun xmlNodePtr.firstChild(name: String): xmlNodePtr? {
    forEachChild { node ->
        if (node.name == name) return node
    }
    return null
}

fun xmlNodePtr.visit(visitor: (xmlNodePtr) -> Unit) {
    visitor(this)
    forEachChild { child ->
        child.visit(visitor)
    }
}

fun xmlNodePtr.text(): String {
    val stringBuilder = StringBuilder()
    visit { node ->
        if (node.isText) {
            node.pointed.content?.let {
                stringBuilder.append(it.toKString())
            }
        }
    }
    return stringBuilder.toString()
}

val xmlDocPtr.rootElement: xmlNodePtr
    get() = xmlDocGetRootElement(this)!!

inline fun <T> xmlDocPtr.use(action: (xmlDocPtr) -> T): T {
    return try {
        action(this)
    } finally {
        xmlFreeDoc(this)
    }
}
