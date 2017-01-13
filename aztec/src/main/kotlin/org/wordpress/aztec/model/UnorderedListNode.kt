package org.wordpress.aztec.model

import java.util.*

class UnorderedListNode(initialStart: Int, attributes: String, parent: ElementNode?, children: ArrayList<Element> = ArrayList()) : ListNode(initialStart, "ul", attributes, parent, children) {

    init {
        children.add(0, ElementText(start, "\n"))
        children.add(ElementText(end, "\n"))
    }


}