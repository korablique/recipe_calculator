package korablique.recipecalculator.ui.numbersediting

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import korablique.recipecalculator.R
import korablique.recipecalculator.ui.inputfilters.DecimalNumberInputFilter
import java.lang.IllegalStateException
import korablique.recipecalculator.ui.inputfilters.NumericBoundsInputFilter
import java.lang.IllegalArgumentException


// Bright red default color so that it would be easy to spot a mistake in code
@ColorRes
private const val DEFAULT_COLOR_RES = android.R.color.holo_red_dark
@ColorRes
private const val DEFAULT_COLOR_RES_FOCUSED_UNDERLINE = R.color.colorFocusedUnderline

private const val ADDITIONAL_HORIZONTAL_PADDING = 4f
private const val UNDERLINE_BOTTOM_PADDING = 8.25f
private const val UNDERLINE_HEIGHT_EMPTY_NORMAL = 0.95f
private const val UNDERLINE_HEIGHT_EMPTY_FOCUSED = 2f
private const val UNDERLINE_HEIGHT_FILLED = 3f

/**
 * Inheritor of EditText, which purpose is to turn the EditText's underline into a progress bar.
 */
class EditProgressText : EditText {
    private val filledUnderlinePaint = Paint()
    private val emptyUnderlinePaint = Paint()

    @ColorInt
    private val emptyUnderlineColorNormal: Int
    @ColorInt
    private val emptyUnderlineColorFocused: Int

    private val minValue = 0f
    private val maxValue: Float

    private val inputFiltersBase: Array<InputFilter>

    // % of progress from minValue to maxValue
    private var realProgress = 0f
    // Same as realProgress, but it's actually displayed on screen when
    // realProgress changes - the value changes are animated.
    private var displayedProgress = realProgress
    // Animates displayedProgress.
    private var progressAnimator = ValueAnimator()
    // See onTextChanged()
    private var constructed = false

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, android.R.attr.editTextStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Let's extract values from XML layout
        val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.EditProgressText,
                0, 0)
        @ColorInt
        val underlineColorFilled: Int
        val isBackgroundSpecified: Boolean
        val hasInputType: Boolean
        try {
            hasInputType = typedArray.hasValue(R.styleable.EditProgressText_android_inputType)
            isBackgroundSpecified = typedArray.hasValue(R.styleable.EditProgressText_android_background)
            emptyUnderlineColorNormal = typedArray.getColor(
                    R.styleable.EditProgressText_android_colorControlNormal,
                    getColor(DEFAULT_COLOR_RES))
            underlineColorFilled = typedArray.getColor(
                    R.styleable.EditProgressText_color_underline_filled,
                    getColor(DEFAULT_COLOR_RES))
            emptyUnderlineColorFocused = typedArray.getColor(
                    R.styleable.EditProgressText_color_underline_empty_focused,
                    getColor(DEFAULT_COLOR_RES_FOCUSED_UNDERLINE))

            if (!typedArray.hasValue(R.styleable.EditProgressText_max_value)) {
                throw IllegalStateException("EditProgressText needs specified max value to work")
            }
            maxValue = typedArray.getFloat(R.styleable.EditProgressText_max_value, -1f)
        } finally {
            typedArray.recycle()
        }

        filledUnderlinePaint.strokeWidth = dpToPixels(UNDERLINE_HEIGHT_FILLED)
        filledUnderlinePaint.color = underlineColorFilled

        if (!isBackgroundSpecified) {
            // When EditText has no specified background, Android adds a background with
            // underline to it.
            // Os if background is not specified, we set transparent color as the background
            // so that the Android's underline would be gone (and we could draw our own).
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            // Default background also adds paddings somehow. Since we removed the default
            // background, we have to add the paddings manually.
            setPadding(paddingLeft + dpToPixels(ADDITIONAL_HORIZONTAL_PADDING).toInt(),
                    paddingTop,
                    paddingRight + dpToPixels(ADDITIONAL_HORIZONTAL_PADDING).toInt(),
                    paddingBottom)
        }

        // Antialiasing gives our underline a 'smooth' look.
        // Real underline looks like it uses antialiasing, so we use it, too.
        filledUnderlinePaint.isAntiAlias = true
        emptyUnderlinePaint.isAntiAlias = true

        if (!hasInputType) {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        inputFiltersBase = arrayOf(
                NumericBoundsInputFilter.withBounds(minValue, maxValue),
                DecimalNumberInputFilter.of1DigitAfterPoint())
        filters = inputFiltersBase

        constructed = true
    }

    @ColorInt
    private fun getColor(@ColorRes color: Int): Int {
        return ContextCompat.getColor(context, color)
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (!constructed) {
            // The onTextChanged method is called from EditText's constructor, at which point
            // all of our fields are not initialized yet.
            // So we have to make an early return here if the instance is not constructed yet.
            return
        }

        // Let's calculate current progress by the new text.
        val displayedNumber = getDisplayedNumber()
        realProgress = when (displayedNumber) {
            null -> 0f
            else -> displayedNumber / maxValue
        }

        // Cancel the previous animation and start a new one.
        progressAnimator.cancel()
        progressAnimator = ValueAnimator.ofFloat(displayedProgress, realProgress)

        // Max duration is 1000 milliseconds, used duration is calculated by the
        // difference between displayedProgress and realProgress.
        val duration = 1000 * Math.abs(displayedProgress - realProgress)
        progressAnimator.duration = duration.toLong()

        // Let's start the animation!
        progressAnimator.addUpdateListener {
            displayedProgress = progressAnimator.animatedValue as Float
            invalidate()
        }
        progressAnimator.start()
    }

    fun getDisplayedNumber(): Float? {
        return text.toString().toFloatOrNull()
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (hasFocus()) {
            true -> {
                emptyUnderlinePaint.strokeWidth = dpToPixels(UNDERLINE_HEIGHT_EMPTY_FOCUSED)
                emptyUnderlinePaint.color = emptyUnderlineColorFocused
            }
            false -> {
                emptyUnderlinePaint.strokeWidth = dpToPixels(UNDERLINE_HEIGHT_EMPTY_NORMAL)
                emptyUnderlinePaint.color = emptyUnderlineColorNormal
            }
        }

        val left = paddingLeft.toFloat()
        val right = width.toFloat() - paddingRight
        // Note that we apply a 'padding' to the drawn underline - its value
        // is copied from the real EditText's underline by eye (so very approximately, but it looks
        // same as the real underline).
        val bottom = height.toFloat() - dpToPixels(UNDERLINE_BOTTOM_PADDING)
        val progressX = left + (right - left) * displayedProgress

        canvas.drawLine(left, bottom, progressX, bottom, filledUnderlinePaint)
        canvas.drawLine(progressX, bottom, right, bottom, emptyUnderlinePaint)
    }

    private fun dpToPixels(dip: Float): Float {
        return applyDimension(COMPLEX_UNIT_DIP, dip, resources.displayMetrics)
    }

    fun setIntermediateMax(intermediateMax: Float) {
        if (intermediateMax < minValue || maxValue < intermediateMax) {
            throw IllegalArgumentException("Intermediate max must be within absolute min-max bounds")
        }
        val intermediateMaxFilter = NumericBoundsInputFilter.withBounds(minValue, intermediateMax)
        filters = inputFiltersBase + intermediateMaxFilter
    }
}
