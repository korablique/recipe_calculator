package korablique.recipecalculator.ui.bucketlist

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import korablique.recipecalculator.R
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher.OnTextChangedListener

class CommentLayoutController(private val layout: ConstraintLayout) {
    private var state = State.EMPTY_NOT_EDITABLE
    private val commentView = layout.findViewById<EditText>(R.id.comment)
    private val addCommentButton = layout.findViewById<View>(R.id.add_comment_button)
    private val commentTitle = layout.findViewById<TextView>(R.id.comment_title)
    private val commentEditsObservers = mutableListOf<CommentEditsObserver>()

    interface CommentEditsObserver {
        fun onCommentViewTextEdited(comment: String)
    }

    init {
        commentView.addTextChangedListener(
                SimpleTextWatcher(commentView, OnTextChangedListener { onCommentChanged() }))

        commentView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && state.isEmpty()) {
                updateEmptiessState(forcedEmpty = false)
            } else if (!hasFocus) {
                updateEmptiessState()
            }
        }

        addCommentButton.setOnClickListener {
            updateEmptiessState(forcedEmpty = false)
            commentView.requestFocus()
        }
    }

    fun addCommentEditsObserver(observer: CommentEditsObserver) {
        commentEditsObservers += observer
    }

    fun removeCommentEditsObserver(observer: CommentEditsObserver) {
        commentEditsObservers -= observer
    }

    private fun onCommentChanged() {
        updateEmptiessState()
        commentEditsObservers.forEach { it.onCommentViewTextEdited(commentView.text.toString().trim()) }
    }

    fun setEditable(editable: Boolean) {
        if (!editable) {
            commentView.clearFocus()
        }
        state = state.turnEditabilityInto(editable)
        updateVisualState()
    }

    fun setComment(comment: String) {
        if (commentView.text.toString() == comment) {
            return
        }
        commentView.setText(comment)
        updateEmptiessState()
    }

    private fun updateEmptiessState(forcedEmpty: Boolean? = null) {
        state = if (forcedEmpty != null) {
            state.turnEmptinessTo(forcedEmpty)
        } else {
            state.turnEmptinessTo(commentView.text.isEmpty() && !commentView.hasFocus())
        }
        updateVisualState()
    }

    private fun updateVisualState() {
        commentView.isEnabled = state.isEditable()

        when (state) {
            State.EMPTY_NOT_EDITABLE -> {
                commentView.visibility = View.GONE
                addCommentButton.visibility = View.GONE
                commentTitle.visibility = View.GONE
            }
            State.EMPTY_EDITABLE -> {
                commentView.visibility = View.GONE
                addCommentButton.visibility = View.VISIBLE
                commentTitle.visibility = View.VISIBLE
            }
            State.NOT_EMPTY_NOT_EDITABLE -> {
                commentView.visibility = View.VISIBLE
                addCommentButton.visibility = View.GONE
                commentTitle.visibility = View.VISIBLE
            }
            State.NOT_EMPTY_EDITABLE -> {
                commentView.visibility = View.VISIBLE
                addCommentButton.visibility = View.VISIBLE
                commentTitle.visibility = View.VISIBLE
            }
        }
    }
}

private enum class State {
    EMPTY_NOT_EDITABLE,
    EMPTY_EDITABLE,
    NOT_EMPTY_NOT_EDITABLE,
    NOT_EMPTY_EDITABLE
}

private fun State.isEditable(): Boolean = when(this) {
    State.EMPTY_NOT_EDITABLE -> false
    State.EMPTY_EDITABLE -> true
    State.NOT_EMPTY_NOT_EDITABLE -> false
    State.NOT_EMPTY_EDITABLE -> true
}

private fun State.isEmpty(): Boolean = when(this) {
    State.EMPTY_NOT_EDITABLE -> true
    State.EMPTY_EDITABLE -> true
    State.NOT_EMPTY_NOT_EDITABLE -> false
    State.NOT_EMPTY_EDITABLE -> false
}

private fun State.turnEditable(): State = when (this) {
    State.EMPTY_NOT_EDITABLE -> State.EMPTY_EDITABLE
    State.EMPTY_EDITABLE -> State.EMPTY_EDITABLE
    State.NOT_EMPTY_NOT_EDITABLE -> State.NOT_EMPTY_EDITABLE
    State.NOT_EMPTY_EDITABLE -> State.NOT_EMPTY_EDITABLE
}

private fun State.turnNotEditable(): State = when (this) {
    State.EMPTY_NOT_EDITABLE -> State.EMPTY_NOT_EDITABLE
    State.EMPTY_EDITABLE -> State.EMPTY_NOT_EDITABLE
    State.NOT_EMPTY_NOT_EDITABLE -> State.NOT_EMPTY_NOT_EDITABLE
    State.NOT_EMPTY_EDITABLE -> State.NOT_EMPTY_NOT_EDITABLE
}

private fun State.turnEditabilityInto(editable: Boolean): State = when (editable) {
    true -> turnEditable()
    false -> turnNotEditable()
}

private fun State.turnEmpty(): State = when (this) {
    State.EMPTY_NOT_EDITABLE -> State.EMPTY_NOT_EDITABLE
    State.EMPTY_EDITABLE -> State.EMPTY_EDITABLE
    State.NOT_EMPTY_NOT_EDITABLE -> State.EMPTY_NOT_EDITABLE
    State.NOT_EMPTY_EDITABLE -> State.EMPTY_EDITABLE
}

private fun State.turnNotEmpty(): State = when (this) {
    State.EMPTY_NOT_EDITABLE -> State.NOT_EMPTY_NOT_EDITABLE
    State.EMPTY_EDITABLE -> State.NOT_EMPTY_EDITABLE
    State.NOT_EMPTY_NOT_EDITABLE -> State.NOT_EMPTY_NOT_EDITABLE
    State.NOT_EMPTY_EDITABLE -> State.NOT_EMPTY_EDITABLE
}

private fun State.turnEmptinessTo(empty: Boolean): State = when (empty) {
    true -> turnEmpty()
    false -> turnNotEmpty()
}
