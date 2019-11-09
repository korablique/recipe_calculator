package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout

import korablique.recipecalculator.R

const val INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS = 70L

/**
 * Вьюшка клавиатуры-калькулятора, код скопирован https://stackoverflow.com/a/45005691
 */
class CalcKeyboard : LinearLayout {
    // Создадим маппинг кнопок в разметке к их значениям
    private var buttonsValues = mapOf(
            R.id.button_1 to "1",
            R.id.button_2 to "2",
            R.id.button_3 to "3",
            R.id.button_4 to "4",
            R.id.button_5 to "5",
            R.id.button_6 to "6",
            R.id.button_7 to "7",
            R.id.button_8 to "8",
            R.id.button_9 to "9",
            R.id.button_0 to "0",
            R.id.button_point to ".",
            R.id.button_plus to "+",
            R.id.button_minus to "-",
            R.id.button_multiply to "×",
            R.id.button_divide to "÷")

    // Подключение к EditText
    private var inputConnection: InputConnection? = null

    private var isBackspaseBeingHeld = false

    constructor(context: Context, attrs: AttributeSet?) : this (context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Создаём иерархию с кнопочками из разметки и подключаем её к себе
        LayoutInflater.from(context).inflate(R.layout.calc_keyboard, this, true)
        
        // Слушаем клики на каждую кнопочку
        findViewById<Button>(R.id.button_1).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_2).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_3).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_4).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_5).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_6).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_7).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_8).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_9).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_0).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_point).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_plus).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_multiply).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_divide).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_minus).setOnClickListener(this::onButtonClick)

        findViewById<Button>(R.id.button_backspace).setOnClickListener(this::onButtonClick)
        findViewById<Button>(R.id.button_backspace).setOnLongClickListener(this::onBackspaceLongClick)
        findViewById<Button>(R.id.button_backspace).setOnTouchListener(this::onBackspaceKeyEvent)
    }

    /**
     * Подключаем EditText к клавиатуре.
     */
    fun connectWith(editText: CalcEditText) {
        this.inputConnection = editText.onCreateInputConnection(EditorInfo())
    }

    private fun onButtonClick(v: View) {
        val inputConnection = this.inputConnection
        if (inputConnection == null) {
            // Пока нет подключения ни с каким EditText'ом
            return
        }

        // На последних Андроидах текст при вводе постоянно начинает находится в "composing"
        // состоянии, и если закоммитить новый текст, пока предыдущий "composing", то предыдущий
        // текст сотрётся. Это приведёт к багу - новый текст будет стирать старый/часть старого.
        // Чтобы бага не было, перед коммитом нового текста остановим composing старого.
        inputConnection.finishComposingText()

        if (v.id == R.id.button_backspace) {
            performTextDeletion(inputConnection)
        } else {
            val value = buttonsValues[v.id]
            inputConnection.commitText(value, 1/*курсор вправо на 1*/)
        }
    }

    private fun performTextDeletion(inputConnection: InputConnection) {
        // Удалим символы при клике на бекспейс
        val selectedText = inputConnection.getSelectedText(0/*flags*/)
        if (TextUtils.isEmpty(selectedText)) {
            // Никакой текст не выделен, удалим 1 символ перед курсором и 0 после
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            // Текст выделен - удалим его, заменив пустым
            inputConnection.commitText("", 1/*курсор вправо на 1*/)
        }
    }

    private fun onBackspaceLongClick(v: View): Boolean {
        isBackspaseBeingHeld = true
        // Начинаем удалять символы!
        handler.postDelayed(this::onBackspaceHoldingTick, INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS)
        return true
    }

    private fun onBackspaceHoldingTick() {
        val inputConnection = this.inputConnection
        if (inputConnection == null) {
            return
        }
        // Бэкспейс держат в течение INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS, удалим символ
        performTextDeletion(inputConnection)

        // Если бекспейс ещё нажат, через несколько мс снова удалим символ!
        if (isBackspaseBeingHeld) {
            handler.postDelayed(this::onBackspaceHoldingTick, INTERVAL_BETWEEN_BACKSPACE_HOLD_DELETIONS)
        }
    }

    private fun onBackspaceKeyEvent(v: View, event: MotionEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            // С бэкспейса подняли палец (ACTION_UP) - бекспейс больше не нажат
            isBackspaseBeingHeld = false
        }
        return false
    }
}