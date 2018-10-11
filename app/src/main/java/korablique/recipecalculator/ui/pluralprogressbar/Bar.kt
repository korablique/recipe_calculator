package korablique.recipecalculator.ui.pluralprogressbar

import android.graphics.*

const val ROUND_CORNERS_NONE = 0b0000
const val ROUND_CORNERS_LEFT = 0b0001
const val ROUND_CORNERS_RIGHT = 0b0010

class Bar {
    var progress = 0f
    val cornersRadii: Float
    val rect = RectF()
    val path = Path()
    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val corners: Int

    constructor(color: Int, radii: Float, roundCorners: Int) {
        cornersRadii = radii
        paint.color = color
        corners = roundCorners
    }

    fun setBounds(bounds: RectF) {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    fun setBounds(left: Float, top: Float, right: Float, bottom: Float) {
        rect.set(left, top, right, bottom)
        path.reset()

        val radiiArray = FloatArray(8)
        if (corners and ROUND_CORNERS_LEFT != 0) {
            radiiArray[0] = cornersRadii
            radiiArray[1] = cornersRadii
            radiiArray[6] = cornersRadii
            radiiArray[7] = cornersRadii
        }
        if (corners and ROUND_CORNERS_RIGHT != 0) {
            radiiArray[2] = cornersRadii
            radiiArray[3] = cornersRadii
            radiiArray[4] = cornersRadii
            radiiArray[5] = cornersRadii
        }
        path.addRoundRect(rect, radiiArray, Path.Direction.CW)
    }

    fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}