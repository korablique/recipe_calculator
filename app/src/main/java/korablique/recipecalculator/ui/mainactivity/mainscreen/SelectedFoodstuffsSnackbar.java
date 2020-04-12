package korablique.recipecalculator.ui.mainactivity.mainscreen;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.behavior.SwipeDismissBehavior;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;

public class SelectedFoodstuffsSnackbar {
    private static final long DURATION = 250L;
    private boolean isShown;
    private final ViewGroup snackbarLayout;
    private final TextView selectedFoodstuffsCounter;
    private List<Ingredient> selectedIngredients = new ArrayList<>();

    @Nullable
    private Runnable onDismissListener;

    public SelectedFoodstuffsSnackbar(View fragmentView,
                                      FragmentCallbacks fragmentCallbacks,
                                      BucketList bucketList) {
        snackbarLayout = fragmentView.findViewById(R.id.snackbar);
        selectedFoodstuffsCounter = fragmentView.findViewById(R.id.selected_foodstuffs_counter);

        // Настраиваем "высвайпываемость" снекбара
        SwipeDismissBehavior<View> swipeDismissBehavior = new SwipeDismissBehavior<>();
        swipeDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);

        // Задаём behaviour в часть снекбара, которая позволяет себя "высвайпывать" (swipeable_snackbar_part)
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                snackbarLayout.findViewById(R.id.swipeable_snackbar_part).getLayoutParams();
        params.setBehavior(swipeDismissBehavior);

        // Задаём слушателя "высвайпывания"
        swipeDismissBehavior.setListener(new SwipeDismissBehaviourListener() {
            @Override
            public void onDismiss(View view) {
                // Сбрасываем состояние "высвайпности" (https://stackoverflow.com/a/40193547)
                CoordinatorLayout.LayoutParams swipedParams =
                        (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                swipedParams.setMargins(0, 0, 0, 0);
                view.requestLayout();
                view.setAlpha(1.0f);
                // Прячем основной леяут снекбара так же, как это делается в методе hide
                // (чтобы снекбар потом нормально показался через вызов метода show)
                snackbarLayout.setVisibility(View.INVISIBLE);
                snackbarLayout.setTranslationY(getParentHeight());
                isShown = false;

                // Уведомляем о "высвайпывании"
                if (onDismissListener != null) {
                    onDismissListener.run();
                }
            }
        });

        BucketList.Observer bucketListObserver = new BucketList.Observer() {
            @Override
            public void onIngredientAdded(Ingredient ingredient) {
                update(bucketList.getList());
            }
            @Override
            public void onIngredientRemoved(Ingredient ingredient) {
                update(bucketList.getList());
            }
        };
        bucketList.addObserver(bucketListObserver);
        fragmentCallbacks.addObserver(new FragmentCallbacks.Observer() {
            @Override
            public void onFragmentDestroy() {
                bucketList.removeObserver(bucketListObserver);
            }
        });
        update(bucketList.getList());
    }

    public void show() {
        snackbarLayout.setVisibility(View.VISIBLE);
        float startValue = snackbarLayout.getHeight();
        float endValue = 0;
        if (!isShown) {
            animateSnackbar(startValue, endValue);
        }
        isShown = true;
    }

    private void hide() {
        float startValue = snackbarLayout.getTranslationY();
        float endValue = getParentHeight();
        animateSnackbar(startValue, endValue, () -> snackbarLayout.setVisibility(View.INVISIBLE));
        isShown = false;
    }

    private void animateSnackbar(float startValue, float endValue) {
        animateSnackbar(startValue, endValue, () -> {});
    }

    private void animateSnackbar(float startValue, float endValue, Runnable finishCallback) {
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(DURATION);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            snackbarLayout.setTranslationY(animatedValue);
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishCallback.run();
            }
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    private void update(List<Ingredient> newSelectedIngredients) {
        selectedIngredients.clear();
        if (newSelectedIngredients.isEmpty()) {
            hide();
        } else {
            selectedIngredients.addAll(newSelectedIngredients);
            updateSelectedFoodstuffsCounter();
            show();
        }
    }

    private void updateSelectedFoodstuffsCounter() {
        selectedFoodstuffsCounter.setText(String.valueOf(selectedIngredients.size()));
    }

    private int getParentHeight() {
        View snackbarParent = (View) snackbarLayout.getParent();
        return snackbarParent.getHeight();
    }

    public void setOnBasketClickRunnable(Runnable runnable) {
        // "Высвайпываемая" часть снекбара уже ждёт жеста свайпа, поэтому вешаем на неё ещё
        // и слушателя кликов - если 2 разные вьюшки в иерархии будут ждать 2 разных жестов,
        // они будут конфликтовать за них.
        snackbarLayout.findViewById(R.id.swipeable_snackbar_part)
                .setOnClickListener(v -> runnable.run());
    }

    public void setOnDismissListener(Runnable dismissListener) {
        this.onDismissListener = dismissListener;
    }
}
