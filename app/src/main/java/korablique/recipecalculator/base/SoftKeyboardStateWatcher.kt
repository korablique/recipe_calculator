package korablique.recipecalculator.base

import android.graphics.Rect
import android.view.View
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.ui.KeyboardHandler
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Следит за состоянием системной экранной клавиатуры.
 * Важно, что в Андроиде отсутствуют системные средства для получения и отслеживания состояния
 * экранной клавиатуры, поэтому _все_ значения и уведомления, полученные из этого класса,
 * далеко не стопроцентны.
 * От этого, если какая-то логика завязана на этот класс, всегда стоит иметь запасной план.
 */
@ActivityScope
class SoftKeyboardStateWatcher {
    private val context: BaseActivity
    private val mainThreadExecutor: MainThreadExecutor

    private val observers = CopyOnWriteArrayList<Observer>()
    private val keyboardHiddenCallbacks = CopyOnWriteArrayList<()->Unit>()

    var isSoftKeyboardShown: Boolean = false
        private set

    interface Observer {
        fun onKeyboardProbablyHidden() {}
        fun onKeyboardProbablyShown() {}
    }

    @Inject
    constructor(
            activity: BaseActivity,
            mainThreadExecutor: MainThreadExecutor) {
        this.context = activity
        this.mainThreadExecutor = mainThreadExecutor

        val rootLayout: View = activity.findViewById(android.R.id.content)
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(this::onGlobalLayout)

        // Делаем вид, что случился onGlobalLayout,
        // чтобы закешировать состояние показанности клавиатуры.
        onGlobalLayout()
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    /**
     * @param callback колбек, который будет вызван когда клавиатура пропадёт, либо через timeoutMillis
     * @param timeoutMillis время, через которое переданный колбек будет вызван, даже если мы не получили
     * сигнал от ОСи о закрытии клавиатуры
     */
    fun hideKeyboardAndCall(timeoutMillis: Long, callback: ()->Unit) {
        hideKeyboardAndCallImpl(timeoutMillis, callback, clearViewFocus = true)
    }

    /**
     * @see hideKeyboardAndCall
     */
    fun hideKeyboardAndCall(timeoutMillis: Long, callback: Runnable) {
        hideKeyboardAndCall(timeoutMillis, callback::run)
    }

    /**
     * То же, что #hideKeyboardAndCall, но при закрытии клавиатуры не очищается
     * фокус с сфокусированной вьюшки
     * @see hideKeyboardAndCall
     */
    fun hideKeyboardWithoutClearingFocusAndCall(timeoutMillis: Long, callback: ()->Unit) {
        hideKeyboardAndCallImpl(timeoutMillis, callback, clearViewFocus = false)
    }

    /**
     * @see hideKeyboardWithoutClearingFocusAndCall
     */
    fun hideKeyboardWithoutClearingFocusAndCall(timeoutMillis: Long, callback: Runnable) {
        hideKeyboardWithoutClearingFocusAndCall(timeoutMillis, callback::run)
    }

    private fun hideKeyboardAndCallImpl(timeoutMillis: Long, callback: ()->Unit, clearViewFocus: Boolean) {
        if (!isSoftKeyboardShown) {
            callback.invoke()
            return
        }

        // Запомним колбек
        keyboardHiddenCallbacks.add(callback)
        // Если через timeoutMillis клавиатура всё ещё не спрятана,
        // всё равно вызовем колбек
        mainThreadExecutor.executeDelayed(timeoutMillis) {
            val removed = keyboardHiddenCallbacks.remove(callback)
            if (removed) {
                // Если removed, то колбек ещё не вызван, т.к. всё ещё лежал
                // в keyboardHiddenCallbacks
                callback.invoke()
            }
        }

        if (clearViewFocus) {
            KeyboardHandler(context).hideKeyBoard()
        } else {
            KeyboardHandler(context).hideKeyBoardWithoutClearingFocus()
        }
    }

    /**
     * Весь код с вычислением высоты клавиатуры сворован из https://stackoverflow.com/a/37948358
     */
    private fun onGlobalLayout() {
        val rootLayout: View = context.findViewById(android.R.id.content)
        val window = context.window
        val resources = context.resources

        // navigation bar height
        var navigationBarHeight = 0
        var resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        // status bar height
        var statusBarHeight = 0
        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        // display window size for the app layout
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)

        // keyboard height = screen height - (user app height + status + nav)
        val keyboardHeight = rootLayout.rootView.height - (statusBarHeight + navigationBarHeight + rect.height())

        // if non-zero, then there is a soft keyboard
        val wasSoftKeyboardShown = isSoftKeyboardShown
        isSoftKeyboardShown = keyboardHeight > 0

        if (isSoftKeyboardShown != wasSoftKeyboardShown) {
            if (isSoftKeyboardShown) {
                observers.forEach { it.onKeyboardProbablyShown() }
            } else {
                observers.forEach { it.onKeyboardProbablyHidden() }
                keyboardHiddenCallbacks.forEach { it.invoke() }
                keyboardHiddenCallbacks.clear()
            }
        }
    }
}
