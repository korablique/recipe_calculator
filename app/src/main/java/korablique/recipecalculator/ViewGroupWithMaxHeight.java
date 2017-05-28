package korablique.recipecalculator;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ViewGroupWithMaxHeight extends LinearLayout {
    private float maxWeight;

    public ViewGroupWithMaxHeight(Context context) {
        super(context);
    }

    public ViewGroupWithMaxHeight(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ViewGroupWithMaxHeight,
                0, 0);

        float maxWeight;
        try {
            maxWeight = typedArray.getFloat(R.styleable.ViewGroupWithMaxHeight_max_weight, 0.5f);
        } finally {
            typedArray.recycle();
        }
        if (maxWeight > 1 || maxWeight < 0) {
            throw new IllegalArgumentException("ViewGroup maximum weight must be from 0 to 1");
        }
        this.maxWeight = maxWeight;
    }

    public ViewGroupWithMaxHeight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * widthMeasureSpec и heightMeasureSpec - требования к размерам, наложенные родителем.
     * Родитель не в курсе, каким размером мы хотим обладать, но требует, чтобы мы были не больше его.
     * Исходя из этого считаем, что MeasureSpec.getSize(heightMeasureSpec) - высота родителя, и делаем
     * себе максимальную высоту в 2\3 от этого.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = (int) (MeasureSpec.getSize(heightMeasureSpec) * maxWeight);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
