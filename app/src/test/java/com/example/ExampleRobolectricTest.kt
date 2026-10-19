package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClothingCategory
import com.example.data.OutfitGeneratorEngine
import com.example.data.SampleWardrobeData
import com.example.data.StyleType
import com.example.data.SuggestionKind
import com.example.data.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Wardrobe", appName)
    }

    @Test
    fun `outfit generator returns 3 distinct suggestions obeying weather and style rules`() {
        val items = SampleWardrobeData.getStarterItems(isArabic = true)

        // When Cold is selected, Outerwear is included
        val coldSuggestions = OutfitGeneratorEngine.generateOutfits(items, WeatherType.COLD, seed = 42L)
        assertEquals(3, coldSuggestions.size)

        val s1Cold = coldSuggestions[0]
        assertEquals(SuggestionKind.WEATHER_AWARE, s1Cold.kind)
        assertTrue(s1Cold.requiresOuterwear)
        assertNotNull(s1Cold.outerwear)
        assertNotNull(s1Cold.top)
        assertNotNull(s1Cold.bottom)
        assertNotNull(s1Cold.shoes)
        s1Cold.itemsList().forEach { item ->
            val w = WeatherType.fromDb(item.weather)
            assertTrue(w == WeatherType.COLD || w == WeatherType.ALL_SEASON)
        }

        // Suggestion 2: Casual or Sport regardless of weather
        val s2 = coldSuggestions[1]
        assertEquals(SuggestionKind.CASUAL_SPORT, s2.kind)
        s2.itemsList().forEach { item ->
            val style = StyleType.fromDb(item.style)
            assertTrue(style == StyleType.CASUAL || style == StyleType.SPORT)
        }

        // Suggestion 3: Formal/Smart regardless of weather
        val s3 = coldSuggestions[2]
        assertEquals(SuggestionKind.FORMAL_SMART, s3.kind)
        s3.itemsList().forEach { item ->
            val style = StyleType.fromDb(item.style)
            assertEquals(StyleType.FORMAL_SMART, style)
        }

        // When Hot is selected, Outerwear is not required
        val hotSuggestions = OutfitGeneratorEngine.generateOutfits(items, WeatherType.HOT, seed = 42L)
        assertNull(hotSuggestions[0].outerwear)
    }
}
