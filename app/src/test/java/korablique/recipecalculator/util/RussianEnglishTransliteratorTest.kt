package korablique.recipecalculator.util

import korablique.recipecalculator.BuildConfig
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class RussianEnglishTransliteratorTest {
    @Test
    fun transliteration() {
        val russianStr = "Съешь Же Ещё Этих Мягких Французских Булок, Да Выпей Чаю"
        val transliterated = "Sesh Zhe Eshchyo Etih Myagkih Francuzskih Bulok, Da Vypey Chayu"
        Assert.assertEquals(transliterated, transliterateRussian(russianStr))
    }

    @Test
    fun `partial transliteration`() {
        val russianStr = "Съешь Же Esho Этих Magkikh Французских Byulok, Да Выпей Chau"
        val transliterated = "Sesh Zhe Esho Etih Magkikh Francuzskih Byulok, Da Vypey Chau"
        Assert.assertEquals(transliterated, transliterateRussian(russianStr))
    }
}