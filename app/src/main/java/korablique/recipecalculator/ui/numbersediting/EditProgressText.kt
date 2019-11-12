package korablique.recipecalculator.ui.numbersediting

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import korablique.recipecalculator.R
import korablique.recipecalculator.ui.calckeyboard.CalcEditText


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
class EditProgressText : CalcEditText {
    private val filledUnderlinePaint = Paint()
    private val emptyUnderlinePaint = Paint()

    @ColorInt
    private val emptyUnderlineColorNormal: Int
    @ColorInt
    private val emptyUnderlineColorFocused: Int

    private val minValue = 0f
    private val maxValue: Float

    // % of progress from minValue to maxValue
    private var realProgress = 0f
    // Changes gradually from old realProgress to new realProgress value
    // (for example, instead of instant change from 0f to 0.5f,
    // it would do 0f, 0.01f, 0.02f, ..., 0.49f, 0.5f).
    // This is used for visual progress animation.
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
        try {
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

            if (!typedArray.hasValue(R.styleable.EditProgressText_progress_max_value)) {
                throw IllegalStateException("EditProgressText needs specified max value to work")
            }
            maxValue = typedArray.getFloat(R.styleable.EditProgressText_progress_max_value, -1f)
        } finally {
            typedArray.recycle()
        }

        filledUnderlinePaint.strokeWidth = dpToPixels(UNDERLINE_HEIGHT_FILLED)
        filledUnderlinePaint.color = underlineColorFilled

        if (!isBackgroundSpecified) {
            // When EditText has no specified background, Android adds a background with
            // underline to it.
            // So if background is not specified, we set transparent color as the background
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

        setBounds(minValue, maxValue)
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

        // Max duration is 1000 milliseconds, duration used for animation is calculated by the
        // difference between displayedProgress and realProgress.
        // E.g. if realProgress==1f && displayedProgress==0.5f, duration would be 500.
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
        return getCurrentCalculatedValue()
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
        // Note we use the UNDERLINE_BOTTOM_PADDING constant as bottom padding value (instead
        // of calling getBottomPadding()). The value of UNDERLINE_BOTTOM_PADDING is the closest
        // I could get to the real EditText's underline padding.
        val bottom = height.toFloat() - dpToPixels(UNDERLINE_BOTTOM_PADDING)

        // The X coord of right side of progress.
        // If View's left==10 && right=90 && displayedProgress==0.5, progressX would be 50.
        val progressX = left + (right - left) * displayedProgress

        // Draw progress line filled with color from the left side of the View to progressX.
        canvas.drawLine(left, bottom, progressX, bottom, filledUnderlinePaint)
        // Draw thin gray line from progressX to the right side of the View.
        canvas.drawLine(progressX, bottom, right, bottom, emptyUnderlinePaint)
    }

    private fun dpToPixels(dip: Float): Float {
        return applyDimension(COMPLEX_UNIT_DIP, dip, resources.displayMetrics)
    }

    /**
     * Sets an intermediate max value.
     * E.g. if minValue==0f && maxValue==100f, you may want to temporarily set 50f as a max.
     */
    fun setIntermediateMax(intermediateMax: Float) {
        if (intermediateMax < minValue || maxValue < intermediateMax) {
            throw IllegalArgumentException("Intermediate max must be within absolute min-max bounds")
        }
        setBounds(minValue, intermediateMax)
    }
}
