package org.wordpress.aztec.spans

import android.content.Context
import android.graphics.drawable.Drawable
import org.wordpress.aztec.AztecAttributes
import org.wordpress.aztec.AztecText

class AztecImageSpan(context: Context, imageProvider: IImageProvider, attributes: AztecAttributes = AztecAttributes(),
                     var onImageTappedListener: AztecText.OnImageTappedListener? = null,
                     editor: AztecText? = null) :
        AztecMediaSpan(context, imageProvider, attributes, editor) {

    override val TAG: String = "img"

    override fun onClick() {
        onImageTappedListener?.onImageTapped(attributes, getWidth(getDrawable()), getHeight(getDrawable()))
    }
}