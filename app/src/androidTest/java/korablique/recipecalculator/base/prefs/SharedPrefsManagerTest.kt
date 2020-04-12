package korablique.recipecalculator.base.prefs

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.util.FloatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SharedPrefsManagerTest {
    lateinit var prefsManager: SharedPrefsManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        PrefsCleaningHelper.cleanAllPrefs(context)
        prefsManager = SharedPrefsManager(context)
    }

    @Test
    fun canPutAndGetLongsList() {
        val myKey = "myKey"
        var extractedLongList = prefsManager.getLongList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(null, extractedLongList)

        val createdLongList = listOf(1L, 2L, 3L)
        prefsManager.putLongList(PrefsOwner.BUCKET_LIST, myKey, createdLongList)

        extractedLongList = prefsManager.getLongList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(3, extractedLongList!!.size)
        assertEquals(createdLongList, extractedLongList!!)
    }

    @Test
    fun canPutAndGetFloatList() {
        val myKey = "myKey"
        var extractedFloatList = prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(null, extractedFloatList)

        val createdFloatList = listOf(123.123.toFloat(), 321.321.toFloat(), 213.111.toFloat())
        prefsManager.putFloatList(PrefsOwner.BUCKET_LIST, myKey, createdFloatList, digitsAfterDot = 3)

        extractedFloatList = prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(3, extractedFloatList!!.size)
        for (index in extractedFloatList.indices) {
            assertTrue(FloatUtils.areFloatsEquals(extractedFloatList[index], createdFloatList[index]))
        }
    }

    @Test
    fun canChooseStoredFloatsPrecision() {
        val myKey = "myKey"
        val precision = 2.toShort()
        val createdFloatList = listOf(123.1234.toFloat(), 321.3210.toFloat(), 213.1111.toFloat())
        prefsManager.putFloatList(PrefsOwner.BUCKET_LIST, myKey, createdFloatList, digitsAfterDot = precision)

        val extractedFloatList = prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, myKey)
        val floatsAsStrs = extractedFloatList!!.map { it.toString() }
        assertEquals("123.12", floatsAsStrs[0])
        assertEquals("321.32", floatsAsStrs[1])
        assertEquals("213.11", floatsAsStrs[2])
    }

    @Test
    fun canPutAndGetStringList() {
        val myKey = "myKey"
        var extractedStringList = prefsManager.getStringList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(null, extractedStringList)

        val createdStringList = listOf("1", "2", "3")
        prefsManager.putStringList(PrefsOwner.BUCKET_LIST, myKey, createdStringList)

        extractedStringList = prefsManager.getStringList(PrefsOwner.BUCKET_LIST, myKey)
        assertEquals(3, extractedStringList!!.size)
        assertEquals(createdStringList, extractedStringList!!)
    }

    @Test
    fun canPutAndGetEmptyFloatList() {
        prefsManager.putFloatList(PrefsOwner.BUCKET_LIST, "mykey", emptyList(), digitsAfterDot = 3)
        assertEquals(null, prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, "mykey"))
    }

    @Test
    fun canPutAndGetEmptyLongList() {
        prefsManager.putLongList(PrefsOwner.BUCKET_LIST, "mykey", emptyList())
        assertEquals(null, prefsManager.getLongList(PrefsOwner.BUCKET_LIST, "mykey"))
    }

    @Test
    fun canPutAndGetEmptyStringList() {
        prefsManager.putStringList(PrefsOwner.BUCKET_LIST, "mykey", emptyList())
        assertEquals(null, prefsManager.getStringList(PrefsOwner.BUCKET_LIST, "mykey"))
    }
}