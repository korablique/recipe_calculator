package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout

import korablique.recipecalculator.R

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
            R.id.button_point to ".")

    // Подключение к EditText
    private var inputConnection: InputConnection? = null

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
        findViewById<Button>(R.id.button_backspace).setOnClickListener(this::onButtonClick)
    }
    
    private fun onButtonClick(v: View) {
        if (inputConnection == null) {
            // Пока нет подключения ни с каким EditText'ом
            return
        }

        if (v.id == R.id.button_backspace) {
            // Удалим символы при клике на бекспейс
            val selectedText = inputConnection!!.getSelectedText(0/*flags*/)
            if (TextUtils.isEmpty(selectedText)) {
                // Никакой текст не выделен, удалим 1 символ перед курсором и 0 после
                inputConnection!!.deleteSurroundingText(1, 0)
            } else {
                // Текст выделен - удалим его, заменив пустым
                inputConnection!!.commitText("", 1)
            }
        } else {
            val value = buttonsValues[v.id]
            inputConnection!!.commitText(value, 1)
        }
    }

    // Контроллер должен предоставить нам ссылку на подключение к EditText'у
    fun setInputConnection(ic: InputConnection) {
        this.inputConnection = ic
    }
}