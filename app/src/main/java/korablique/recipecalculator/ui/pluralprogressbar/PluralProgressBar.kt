package korablique.recipecalculator.ui.pluralprogressbar

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import korablique.recipecalculator.R

class PluralProgressBar : View {
    // Bounds of the view
    private val bounds = RectF()
    // All the colorful bars of this view
    private val bars: List<Bar>
    // The grey background bar
    private var backgroundBar: Bar

    // The bitmap and canvas are needed for SRC_IN drawing (see somewhere below).
    private lateinit var bitmap: Bitmap
    private lateinit var bitmapCanvas: Canvas

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val styledAttrs = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.PluralProgressBar,
                0, 0)

        val cornersRadii: Float
        try {
            cornersRadii = styledAttrs.getDimension(R.styleable.PluralProgressBar_corners_radii, 0f)
            bars = extractBars(styledAttrs, cornersRadii)
            if (!styledAttrs.hasValue(R.styleable.PluralProgressBar_background_bar_color)) {
                throw IllegalArgumentException("background_bar_color expected to be specified")
            }
            val backgroundBarColor = styledAttrs.getColor(R.styleable.PluralProgressBar_background_bar_color, 0)
            backgroundBar = Bar(backgroundBarColor, cornersRadii, ROUND_CORNERS_LEFT or ROUND_CORNERS_RIGHT)
        } finally {
            styledAttrs.recycle()
        }
    }

    private fun extractBars(styledAttrs: TypedArray, cornersRadii: Float): List<Bar> {
        val colors = extractColors(styledAttrs)

        val bars = mutableListOf<Bar>()
        for (index in 0 until colors.size) {
            // First bar will have left side round.
            // Last bar will have right side round.
            // Other bars won't have any round sides.
            val corners = when (index) {
                0 -> ROUND_CORNERS_LEFT
                colors.size - 1 -> ROUND_CORNERS_RIGHT
                else -> ROUND_CORNERS_NONE
            }

            bars.add(Bar(colors[index], cornersRadii, corners))
            // We need to change the paint mode of all (except for background) bars to SRC_IN so that
            // they will never be drawn outside of the background.
            //
            // This is needed for cases when some of the bars wouldn't be round (because they're not first or last),
            // but are drawn above of the round background (because first or last bar is <1%).
            //
            // See https://stackoverflow.com/questions/22690237/draw-intersection-of-paths-or-shapes-on-android-canvas
            bars.last().paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }

        return bars
    }

    private fun extractColors(styledAttrs: TypedArray): MutableList<Int> {
        // Convenience array - so that it would be convenient to iterate over all
        // R.styleable.PluralProgressBar_colorX in a loop (instead of dozens of if..else).
        //
        // The purpose of the array is to store all R.styleable.PluralProgressBar_colorX values.
        // styledAttrs in fact is a map, constants R.styleable.PluralProgressBar_colorX are its keys,
        // used to retrieve colors values.
        val colorsKeys =
                intArrayOf(R.styleable.PluralProgressBar_color0,
                        R.styleable.PluralProgressBar_color1,
                        R.styleable.PluralProgressBar_color2,
                        R.styleable.PluralProgressBar_color3,
                        R.styleable.PluralProgressBar_color4,
                        R.styleable.PluralProgressBar_color5,
                        R.styleable.PluralProgressBar_color6,
                        R.styleable.PluralProgressBar_color7,
                        R.styleable.PluralProgressBar_color8,
                        R.styleable.PluralProgressBar_color9)

        val colors = mutableListOf<Int>()

        // Going from 0 up to 10, looking for colors
        var index = 0
        while (index < colorsKeys.size) {
            val colorKey = colorsKeys[index]
            // If for example color3 exists, but color4 doesn't - we break
            if (!styledAttrs.hasValue(colorKey)) {
                break
            }
            colors.add(styledAttrs.getColor(colorKey, 0))
            ++index
        }

        if (colors.isEmpty()) {
            throw IllegalArgumentException("Progress bar with 0 progresses is pointless")
        }
        // Now we need to check whether the colors list in ours layout has any gaps.
        // For example, if color0, color1, color2 and color4 are specified, but color3 isn't -
        // that looks very suspicious and most likely is a mistake.
        while (index < colorsKeys.size) {
            val colorKey = colorsKeys[index]
            if (styledAttrs.hasValue(colorKey)) {
                throw IllegalArgumentException("Gaps in colors indexes are not allowed")
            }
            ++index
        }
        return colors
    }

    /**
     * @param barIndex index from 0 to 9, depending on how many colors were specified in the layout.
     * @param progress progress from 0 to 100
     * @throws IllegalArgumentException if sum of all passed progresses is bigger than 100
     */
    fun setProgress(barIndex: Int, progress: Float) {
        bars[barIndex].progress = progress

        var totalProgress = 0
        for (bar in bars) {
            totalProgress += Math.floor(bar.progress.toDouble()).toInt()
        }
        if (totalProgress > 100) {
            val barsValues = ArrayList<Float>()
            for (bar in bars) {
                barsValues.add(bar.progress)
            }
            val varsValuesStr = barsValues.joinToString()

            throw IllegalStateException("Sum of all progresses must be <=100, but is $totalProgress, progress: $varsValuesStr")
        }

        recalculateAllBounds()
    }

    fun setProgress(vararg progress: Float) {
        progress.forEachIndexed { index, value ->
            setProgress(index, value)
        }
    }

    private fun recalculateAllBounds() {
        // Background takes entire view's size
        backgroundBar.setBounds(bounds)

        // Let's move all the bars to their right places according to view's size and
        // bar's progresses.
        val viewWidth = bounds.width()
        var currentLeft = bounds.left
        for (bar in bars) {
            // Each next bar is placed to the right of the previous bar.
            val barWidth = viewWidth * bar.progress * 0.01f
            val currentRight = currentLeft + barWidth
            bar.setBounds(currentLeft, bounds.top, currentRight, bounds.bottom)
            currentLeft = currentRight
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmapCanvas = Canvas(bitmap)

        val left = 0f
        val top = 0f
        val right = width.toFloat()
        val bottom = height.toFloat()
        bounds.set(left, top, right, bottom)
        recalculateAllBounds()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundBar.onDraw(bitmapCanvas)
        for (bar in bars) {
            bar.onDraw(bitmapCanvas)
        }

        val offset = 0f
        canvas.drawBitmap(bitmap, offset, offset, backgroundBar.paint)
    }
}