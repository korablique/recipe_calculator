package korablique.recipecalculator.ui.mainactivity.history.pages

import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.joda.time.Days
import org.joda.time.LocalDate
import java.math.BigDecimal

class HistoryPagesAdapter(
        parent: Fragment,
        private val creationTime: LocalDate)
    : FragmentStateAdapter(parent) {

    fun dateToPosition(date: LocalDate): Int {
        val creationTimePosition = Int.MAX_VALUE / 2

        val daysBetween = Days.daysBetween(creationTime, date).days
        val datePosition = creationTimePosition + daysBetween

        if (BigDecimal(creationTimePosition + daysBetween)
                != BigDecimal(creationTimePosition) + BigDecimal(daysBetween)) {
            return PagerAdapter.POSITION_NONE
        }

        return datePosition
    }

    fun positionToDate(position: Int): LocalDate {
        val creationTimePosition = Int.MAX_VALUE / 2
        val diff = position - creationTimePosition
        return creationTime.plusDays(diff)
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }

    override fun createFragment(position: Int): Fragment {
        return HistoryPageFragment(positionToDate(position))
    }
}