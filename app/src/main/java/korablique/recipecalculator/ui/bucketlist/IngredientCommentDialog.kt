package korablique.recipecalculator.ui.bucketlist

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseBottomDialog

private const val INGREDIENT_COMMENT_DIALOG_TAG = "INGREDIENT_COMMENT_DIALOG_TAG"
private const val EXTRA_INITIAL_COMMENT = "EXTRA_INITIAL_COMMENT"

class IngredientCommentDialog : BaseBottomDialog() {
    private var saveButtonListener: ((String)->Unit)? = null
    private var dismissListener: (()->Unit)? = null

    override fun shouldOpenKeyboardWhenShown() = true

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val dialogLayout = LayoutInflater.from(context)
                .inflate(R.layout.bucket_list_ingredient_comment_dialog_layout, container)

        val arguments = arguments!!
        val initialComment = arguments.getString(EXTRA_INITIAL_COMMENT)
        dialogLayout.findViewById<TextView>(R.id.comment).text = initialComment
        dialogLayout.findViewById<Button>(R.id.save_button).setOnClickListener {
            saveButtonListener?.invoke(dialogLayout.findViewById<EditText>(R.id.comment).text.toString())
        }
        dialogLayout.findViewById<View>(R.id.button_close).setOnClickListener { dismiss() }

        return dialogLayout
    }

    fun setOnSaveButtonClickListener(listener: ((String)->Unit)?) {
        this.saveButtonListener = listener
    }

    fun setOnDismissListener(listener: (()->Unit)?) {
        this.dismissListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.invoke()
    }

    companion object {
        fun showDialog(fragmentManager: FragmentManager, initialComment: String): IngredientCommentDialog {
            val dialog = IngredientCommentDialog()
            val args = Bundle()
            args.putString(EXTRA_INITIAL_COMMENT, initialComment)
            dialog.arguments = args
            dialog.show(fragmentManager, INGREDIENT_COMMENT_DIALOG_TAG)
            return dialog
        }

        fun findDialog(fragmentManager: FragmentManager): IngredientCommentDialog? {
            return fragmentManager.findFragmentByTag(INGREDIENT_COMMENT_DIALOG_TAG) as IngredientCommentDialog?
        }
    }
}