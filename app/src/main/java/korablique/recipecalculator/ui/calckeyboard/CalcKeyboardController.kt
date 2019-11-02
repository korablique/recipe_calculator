package korablique.recipecalculator.ui.calckeyboard

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.WeakHashMap

import javax.inject.Inject
import javax.inject.Singleton

import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.BaseBottomDialog
import java.lang.IllegalStateException
import androidx.constraintlayout.widget.ConstraintSet
import korablique.recipecalculator.BuildConfig
import korablique.recipecalculator.ui.KeyboardHandler

private const val CALC_KEYBOARD_PARENT_EXPECTED_TAG = "calc_keyboard_parent"

/**
 * Заменяет системную клавиатуру у переданных в него EditText'ов на клавиатуру-калькулятор.
 */
@Singleton
class CalcKeyboardController @Inject constructor() {
    private val shownKeyboards = WeakHashMap<View, CalcKeyboard>()

    /**
     * Настраивает переданный EditText для работы с клавиатурой-калькулятором (вместо системной).
     */
    fun useCalcKeyboardWith(editText: CalcEditText, parentActivity: BaseActivity) {
        useCalcKeyboardWith(editText, backPressHandlingInitializer = { initActivityBackPress(editText, parentActivity) })
    }

    /**
     * Настраивает переданный EditText для работы с клавиатурой-калькулятором (вместо системной).
     */
    fun useCalcKeyboardWith(editText: CalcEditText, parentDialog: BaseBottomDialog) {
        useCalcKeyboardWith(editText, backPressHandlingInitializer = { initDialogBackPress(editText, parentDialog) })
    }

    /**
     * @param backPressHandlingInitializer - функция, настраивающая перехват нажатий на "Назад", чтобы
     * по этим нажатиям скрывать клавиатуру-калькулятор.
     */
    private fun useCalcKeyboardWith(editText: CalcEditText, backPressHandlingInitializer: ()->Unit) {
        // Не показываем системную клавиатуру при захвате фокуса
        editText.showSoftInputOnFocus = false

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboardFor(editText)
                backPressHandlingInitializer.invoke()
            } else {
                hideKeyboardFor(editText)
            }
        }
        editText.setOnClickListener {
            // Если клавиатура ещё почему-то не показана (или уже не показана, т.е. была спрятана),
            // то покажем её.
            if (!shownKeyboards.containsKey(editText)) {
                showKeyboardFor(editText)
                backPressHandlingInitializer.invoke()
            }
        }
    }

    private fun showKeyboardFor(editText: CalcEditText) {
        var parent: ViewGroup = findCalcKeyboardParent(editText)
        val keyboard = CalcKeyboard(editText.context)
        addCalcKeyboardToParent(keyboard, parent)

        // Подключим EditText к клавиатуре-калькулятору
        keyboard.connectWith(editText)

        shownKeyboards[editText] = keyboard

        // Если по какой-то причине системная клавиатура показана, спрячем её.
        // (Такое возможно, например, если нажать tab и сместить фокус с обычного EditText'а на
        // наш - системная клавиатура никуда не денется, останется открытой)
        KeyboardHandler(editText.context as Activity).hideKeyBoardWithoutClearingFocus()
    }

    private fun findCalcKeyboardParent(editText: EditText): ViewGroup {
        // Ищем родителя EditText'а, помеченного специальным тегом - в него нужно будет вставлять
        // клавиатуру-калькулятор.
        var parent: ViewGroup? = editText.parent as ViewGroup
        while (parent != null) {
            if (parent.tag == CALC_KEYBOARD_PARENT_EXPECTED_TAG) {
                return parent
            }

            if (parent.parent is ViewGroup) {
                parent = parent.parent as ViewGroup
            } else {
                parent = null
            }
        }
        throw IllegalArgumentException("Couldn't find a parent with '$CALC_KEYBOARD_PARENT_EXPECTED_TAG' tag")
    }

    private fun addCalcKeyboardToParent(keyboard: CalcKeyboard, parent: ViewGroup) {
        // В разные ViewGroup нужно по-разному вставлять вьюшку
        // клавиатуры-калькулятора, чтобы та была снизу.

        if (parent is LinearLayout) {
            if (parent.orientation != LinearLayout.VERTICAL) {
                throw IllegalArgumentException("Calc keyboard in horizontal layout doesn't make sense")
            }
            keyboard.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            parent.addView(keyboard)
            return
        }

        if (parent is RelativeLayout) {
            val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            keyboard.layoutParams = params
            parent.addView(keyboard)
            return
        }

        if (parent is ConstraintLayout) {
            parent.addView(keyboard)
            if (keyboard.id == View.NO_ID) {
                // ConstraintLayout закрешит, если в него добавлять вьющку без ID
                keyboard.id = View.generateViewId()
            }
            val constraints = ConstraintSet()
            constraints.clone(parent)
            constraints.connect(keyboard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraints.connect(keyboard.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraints.connect(keyboard.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
            constraints.constrainWidth(keyboard.id, ConstraintLayout.LayoutParams.MATCH_PARENT)
            constraints.applyTo(parent)
            return
        }

        throw IllegalStateException("Parent type ${parent.javaClass.name} is not supported yet")
    }

    private fun hideKeyboardFor(editText: EditText) {
        val keyboard = shownKeyboards.remove(editText)
        if (keyboard != null) {
            val parent = keyboard.parent as ViewGroup
            parent.removeView(keyboard)
        }
    }

    private fun initActivityBackPress(editText: EditText, parentActivity: BaseActivity) {
        // Подписываемся на нажатия на Back у Активити, перехватываем нажатие на Back, скрываем
        // клавиатуру.
        val activityCallbacks = parentActivity.activityCallbacks
        activityCallbacks.addObserver(object : ActivityCallbacks.Observer {
            override fun onActivityBackPressed(): Boolean {
                activityCallbacks.removeObserver(this)
                return handleBackPress(editText)
            }
        })
    }

    private fun initDialogBackPress(editText: EditText, parentDialog: BaseBottomDialog) {
        // Подписываемся на нажатия на Back у диалога, перехватываем нажатие на Back, скрываем
        // клавиатуру.
        parentDialog.addOnBackPressObserver(object : BaseBottomDialog.OnBackPressObserver {
            override fun onBackPressed(): Boolean {
                parentDialog.removeOnBackPressObserver(this)
                return handleBackPress(editText)
            }
        })
    }

    private fun handleBackPress(editText: EditText): Boolean {
        val keyboard = shownKeyboards[editText]
        if (keyboard == null) {
            return false
        }
        shownKeyboards.remove(editText)
        val parent = keyboard.parent as ViewGroup
        keyboard.visibility = View.INVISIBLE
        parent.removeView(keyboard)
        return true
    }
}
