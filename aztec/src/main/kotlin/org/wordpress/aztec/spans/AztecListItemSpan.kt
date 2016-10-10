package org.wordpress.aztec.spans

import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle

class AztecListItemSpan : CharacterStyle, AztecSpan {

    private final val TAG = "li"

    override var attributes: String? = null

    constructor(attributes: String?) : super() {
        this.attributes = attributes
    }

    override fun getStartTag(): String {
        return TAG + attributes
    }

    override fun getEndTag(): String {
        return TAG
    }

    override fun updateDrawState(p0: TextPaint?) {
    }
}
