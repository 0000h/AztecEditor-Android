package org.wordpress.aztec.source

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import org.wordpress.aztec.History
import org.wordpress.aztec.R
import org.wordpress.aztec.spans.AztecCursorSpan
import org.wordpress.aztec.util.TypefaceCache

class SourceViewEditText : EditText, TextWatcher {

    @ColorInt var tagColor = ContextCompat.getColor(context, R.color.tag_color)
        internal set
    @ColorInt var attributeColor = ContextCompat.getColor(context, R.color.attribute_color)
        internal set

    private var styleTextWatcher: HtmlStyleTextWatcher? = null

    lateinit var history: History

    private var consumeEditEvent: Boolean = true

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        TypefaceCache.setCustomTypeface(context, this, TypefaceCache.TYPEFACE_DEJAVU_SANS_MONO)

        val values = context.obtainStyledAttributes(attrs, R.styleable.SourceViewEditText)
        setBackgroundColor(values.getColor(R.styleable.SourceViewEditText_codeBackgroundColor, ContextCompat.getColor(context, R.color.background)))
        setTextColor(values.getColor(R.styleable.SourceViewEditText_codeTextColor, ContextCompat.getColor(context, R.color.text)))

        tagColor = values.getColor(R.styleable.SourceViewEditText_tagColor, tagColor)
        attributeColor = values.getColor(R.styleable.SourceViewEditText_attributeColor, attributeColor)

        styleTextWatcher = HtmlStyleTextWatcher(tagColor, attributeColor)

        values.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addTextChangedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeTextChangedListener(this)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        val customState = savedState.state
        visibility = customState.getInt("visibility")
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        val bundle = Bundle()
        bundle.putInt("visibility", visibility)
        savedState.state = bundle
        return savedState
    }

    internal class SavedState : BaseSavedState {
        var state: Bundle = Bundle()

        constructor(superState: Parcelable) : super(superState) {
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeBundle(state)
        }
    }

    override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        history.beforeTextChanged(text.toString())
        styleTextWatcher?.beforeTextChanged(text, start, count, after)
    }

    override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
        styleTextWatcher?.onTextChanged(text, start, before, count)
    }

    override fun afterTextChanged(text: Editable?) {
        if (isTextChangedListenerDisabled()) {
            enableTextChangedListener()
            return
        }

        history.handleHistory(this)
        styleTextWatcher?.afterTextChanged(text)
    }

    fun redo() {
        history.redo(this)
    }

    fun undo() {
        history.undo(this)
    }

    override fun setVisibility(visibility: Int) {
        val selectionBefore = selectionStart
        super.setVisibility(visibility)

        //There are some cases when changing visibility affects cursor position in EditText, so we making sure it's in
        //a correct place
        if (visibility == View.VISIBLE) {
            requestFocus()
            if (selectionBefore != selectionStart) {
                setSelection(0)
            }
        }
    }

    fun displayStyledAndFormattedHtml(source: String) {
        val styledHtml = styleHtml(Format.addFormatting(source))

        disableTextChangedListener()
        val cursorPosition = consumeCursorTag(styledHtml)
        text = styledHtml
        enableTextChangedListener()

        if (cursorPosition > 0)
            setSelection(cursorPosition)
    }

    fun consumeCursorTag(styledHtml: SpannableStringBuilder): Int {
        val cursorTagIndex = styledHtml.indexOf(AztecCursorSpan.AZTEC_CURSOR_TAG)
        if (cursorTagIndex < 0) return 0
        styledHtml.delete(cursorTagIndex, cursorTagIndex + AztecCursorSpan.AZTEC_CURSOR_TAG.length)

        //if something went wrong make sure to remove cursor tag
        styledHtml.replace(AztecCursorSpan.AZTEC_CURSOR_TAG.toRegex(), "")

        return cursorTagIndex
    }

    fun displayStyledHtml(source: String) {
        val styledHtml = styleHtml(source)
        disableTextChangedListener()
        setTextKeepState(styledHtml)
        enableTextChangedListener()
    }

    private fun styleHtml(source: String): SpannableStringBuilder {
        val styledHtml = SpannableStringBuilder(source)
        HtmlStyleUtils.styleHtmlForDisplayWithColors(styledHtml, tagColor, attributeColor)
        return styledHtml
    }

    fun isCursorInsideTag(): Boolean {
        val indexOfFirstClosingBracketOnTheRight = text.indexOf(">", selectionEnd)
        val indexOfFirstOpeningBracketOnTheRight = text.indexOf("<", selectionEnd)

        val isThereClosingBracketBeforeOpeningBracket = indexOfFirstClosingBracketOnTheRight != -1 &&
                ((indexOfFirstClosingBracketOnTheRight < indexOfFirstOpeningBracketOnTheRight)
                        || indexOfFirstOpeningBracketOnTheRight == -1)

        val indexOfFirstClosingBracketOnTheLeft = text.lastIndexOf(">", selectionEnd-1)
        val indexOfFirstOpeningBracketOnTheLeft = text.lastIndexOf("<", selectionEnd-1)

        val isThereOpeningBracketBeforeClosingBracket = indexOfFirstOpeningBracketOnTheLeft != -1 &&
                ((indexOfFirstOpeningBracketOnTheLeft > indexOfFirstClosingBracketOnTheLeft) || indexOfFirstClosingBracketOnTheLeft == -1 )

        return isThereClosingBracketBeforeOpeningBracket && isThereOpeningBracketBeforeClosingBracket
    }


    fun getPureHtml(withCursorTag: Boolean = false): String {
        if (withCursorTag) {
            disableTextChangedListener()
            if (!isCursorInsideTag()) {
                text.insert(selectionEnd, "<aztec_cursor></aztec_cursor>")
            } else {
                text.insert(text.lastIndexOf("<", selectionEnd), "<aztec_cursor></aztec_cursor>")
            }
            enableTextChangedListener()
        }


        return Format.clearFormatting(text.toString())
    }

    fun disableTextChangedListener() {
        consumeEditEvent = true
    }


    fun enableTextChangedListener() {
        consumeEditEvent = false
    }

    fun isTextChangedListenerDisabled(): Boolean {
        return consumeEditEvent
    }
}