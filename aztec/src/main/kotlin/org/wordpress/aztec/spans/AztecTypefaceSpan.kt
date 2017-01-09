package org.wordpress.aztec.spans

import android.text.TextUtils
import android.text.style.TypefaceSpan
import org.wordpress.aztec.model.ElementNode

class AztecTypefaceSpan : TypefaceSpan, AztecContentSpan, AztecInlineSpan {

    var tag: String
    override var attributes: String
    override var node: ElementNode? = null

    @JvmOverloads
    constructor(tag: String, family: String, attributes: String = "") : super(family) {
        this.tag = tag
        this.attributes = attributes
    }

    override fun getStartTag(): String {
        if (TextUtils.isEmpty(attributes)) {
            return tag
        }
        return tag + attributes
    }

    override fun getEndTag(): String {
        return tag
    }
}