package korablique.recipecalculator.outside.partners.direct

import com.nhaarman.mockitokotlin2.*
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.partners.Partner
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class FoodstuffsCorrespondenceManagerTest {
    @Test
    fun `2 managers can communicate`() = runBlocking {
        val directMsgsManager = mock<DirectMsgsManager>()

        val manager1 = FoodstuffsCorrespondenceManager(directMsgsManager, mock(), mock())
        val foodstuff = Foodstuff.withName("carrot").withNutrition(1f, 2f, 3f, 4f)
        manager1.sendFooodstuffToPartner(foodstuff, Partner("uid", "name"))

        val encodedFoodstuff = argumentCaptor<String>()
        verify(directMsgsManager).sendDirectMSGToPartner(any(), encodedFoodstuff.capture(), any())

        val foodstuffsList2 = spy(FoodstuffsList(mock(), mock(), mock()))
        val manager2 = FoodstuffsCorrespondenceManager(directMsgsManager, foodstuffsList2, mock())

        verify(foodstuffsList2, never()).saveFoodstuff(any())
        manager2.onNewDirectMessage(encodedFoodstuff.firstValue)
        verify(foodstuffsList2).saveFoodstuff(eq(foodstuff))

        Unit
    }
}