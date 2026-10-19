package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.random.Random

enum class ClothingCategory(
    val dbValue: String,
    val labelAr: String,
    val labelEn: String
) {
    TOP("Top", "قطعة علوية (Top)", "Top"),
    OUTERWEAR("Outerwear", "معطف / جاكيت (Outerwear)", "Outerwear"),
    BOTTOM("Bottom", "بنطال / قطعة سفلية (Bottom)", "Bottom"),
    SHOES("Shoes", "حذاء (Shoes)", "Shoes");

    fun label(isArabic: Boolean): String = if (isArabic) labelAr else labelEn

    companion object {
        fun fromDb(value: String): ClothingCategory =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: TOP
    }
}

enum class WeatherType(
    val dbValue: String,
    val labelAr: String,
    val labelEn: String
) {
    COLD("Cold", "بارد (Cold)", "Cold"),
    MILD("Mild", "معتدل (Mild)", "Mild"),
    HOT("Hot", "حار (Hot)", "Hot"),
    ALL_SEASON("All-season", "جميع الفصول (All-season)", "All-season");

    fun label(isArabic: Boolean): String = if (isArabic) labelAr else labelEn

    companion object {
        val generatorOptions = listOf(COLD, MILD, HOT)

        fun fromDb(value: String): WeatherType =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: ALL_SEASON
    }
}

enum class StyleType(
    val dbValue: String,
    val labelAr: String,
    val labelEn: String
) {
    CASUAL("Casual", "كاجوال (Casual)", "Casual"),
    FORMAL_SMART("Formal/Smart", "رسمي / أنيق (Formal/Smart)", "Formal/Smart"),
    SPORT("Sport", "رياضي (Sport)", "Sport");

    fun label(isArabic: Boolean): String = if (isArabic) labelAr else labelEn

    companion object {
        fun fromDb(value: String): StyleType =
            entries.firstOrNull {
                it.dbValue.equals(value, ignoreCase = true) ||
                    (it == FORMAL_SMART && value.contains("Formal", ignoreCase = true))
            } ?: CASUAL
    }
}

@IgnoreExtraProperties
data class WardrobeItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val category: String = ClothingCategory.TOP.dbValue,
    val weather: String = WeatherType.ALL_SEASON.dbValue,
    val style: String = StyleType.CASUAL.dbValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreatePayload(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "name" to name.trim().take(100),
            "imageUrl" to imageUrl.ifBlank { "preset:top_linen_shirt" },
            "category" to ClothingCategory.fromDb(category).dbValue,
            "weather" to WeatherType.fromDb(weather).dbValue,
            "style" to StyleType.fromDb(style).dbValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

enum class SuggestionKind {
    WEATHER_AWARE,
    CASUAL_SPORT,
    FORMAL_SMART
}

data class OutfitSuggestion(
    val kind: SuggestionKind,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val top: WardrobeItem?,
    val bottom: WardrobeItem?,
    val shoes: WardrobeItem?,
    val outerwear: WardrobeItem? = null,
    val requiresOuterwear: Boolean = false
) {
    fun itemsList(): List<WardrobeItem> = listOfNotNull(outerwear, top, bottom, shoes)

    fun isComplete(): Boolean {
        val baseComplete = top != null && bottom != null && shoes != null
        return if (requiresOuterwear) baseComplete && outerwear != null else baseComplete
    }

    fun missingCategories(isArabic: Boolean): List<String> {
        val missing = mutableListOf<String>()
        if (requiresOuterwear && outerwear == null) {
            missing.add(ClothingCategory.OUTERWEAR.label(isArabic))
        }
        if (top == null) missing.add(ClothingCategory.TOP.label(isArabic))
        if (bottom == null) missing.add(ClothingCategory.BOTTOM.label(isArabic))
        if (shoes == null) missing.add(ClothingCategory.SHOES.label(isArabic))
        return missing
    }

    fun summaryForSpeech(isArabic: Boolean): String {
        val parts = itemsList().joinToString(if (isArabic) "، و" else ", ") { it.name }
        return if (isArabic) {
            "${titleAr}: نوصي بارتداء $parts."
        } else {
            "${titleEn}: We recommend wearing $parts."
        }
    }
}

object OutfitGeneratorEngine {
    /**
     * Generates the 3 distinct outfit suggestions required by the specification:
     * 1. Suggestion 1 (Weather-Aware): Matches selected weather or "All-season". Includes Outerwear if Cold.
     * 2. Suggestion 2 (Casual Style): Ignores weather filter. Random combination where Style is "Casual" or "Sport".
     * 3. Suggestion 3 (Formal/Smart Style): Ignores weather filter. Combination where Style is "Formal/Smart".
     */
    fun generateOutfits(
        allItems: List<WardrobeItem>,
        selectedWeather: WeatherType,
        seed: Long = System.currentTimeMillis()
    ): List<OutfitSuggestion> {
        val random = Random(seed)
        val isCold = selectedWeather == WeatherType.COLD

        // 1. Weather-Aware Pool
        val weatherPool = allItems.filter { item ->
            val w = WeatherType.fromDb(item.weather)
            w == selectedWeather || w == WeatherType.ALL_SEASON
        }
        val weatherSuggestion = OutfitSuggestion(
            kind = SuggestionKind.WEATHER_AWARE,
            titleAr = "الاقتراح ١: متوافق مع الطقس (${selectedWeather.labelAr})",
            titleEn = "Suggestion 1: Weather-Aware (${selectedWeather.labelEn})",
            subtitleAr = if (isCold) {
                "قطع مناسبة للطقس البارد أو لجميع الفصول شاملة المعطف"
            } else {
                "قطع متوافقة مع الطقس المحدد أو مناسبة لجميع الفصول"
            },
            subtitleEn = if (isCold) {
                "Matches ${selectedWeather.labelEn} or All-season including Outerwear"
            } else {
                "Filtered for ${selectedWeather.labelEn} or All-season comfort"
            },
            top = pickRandomByCategory(weatherPool, ClothingCategory.TOP, random),
            bottom = pickRandomByCategory(weatherPool, ClothingCategory.BOTTOM, random),
            shoes = pickRandomByCategory(weatherPool, ClothingCategory.SHOES, random),
            outerwear = if (isCold) pickRandomByCategory(weatherPool, ClothingCategory.OUTERWEAR, random) else null,
            requiresOuterwear = isCold
        )

        // 2. Casual / Sport Style Pool (Ignores weather filter)
        val casualPool = allItems.filter { item ->
            val s = StyleType.fromDb(item.style)
            s == StyleType.CASUAL || s == StyleType.SPORT
        }
        val casualSuggestion = OutfitSuggestion(
            kind = SuggestionKind.CASUAL_SPORT,
            titleAr = "الاقتراح ٢: إطلالة كاجوال ورياضية",
            titleEn = "Suggestion 2: Casual & Sport Style",
            subtitleAr = "تنسيق يومي مريح (كاجوال أو رياضي) بغض النظر عن الطقس",
            subtitleEn = "Relaxed combination of Casual or Sport items (weather-independent)",
            top = pickRandomByCategory(casualPool, ClothingCategory.TOP, random),
            bottom = pickRandomByCategory(casualPool, ClothingCategory.BOTTOM, random),
            shoes = pickRandomByCategory(casualPool, ClothingCategory.SHOES, random),
            outerwear = if (isCold) pickRandomByCategory(casualPool, ClothingCategory.OUTERWEAR, random) else null,
            requiresOuterwear = isCold
        )

        // 3. Formal / Smart Style Pool (Ignores weather filter)
        val formalPool = allItems.filter { item ->
            StyleType.fromDb(item.style) == StyleType.FORMAL_SMART
        }
        val formalSuggestion = OutfitSuggestion(
            kind = SuggestionKind.FORMAL_SMART,
            titleAr = "الاقتراح ٣: إطلالة رسمية وأنيقة",
            titleEn = "Suggestion 3: Formal / Smart Style",
            subtitleAr = "تنسيق كلاسيكي أنيق للمناسبات والعمل بغض النظر عن الطقس",
            subtitleEn = "Refined Formal/Smart pieces curated independent of weather",
            top = pickRandomByCategory(formalPool, ClothingCategory.TOP, random),
            bottom = pickRandomByCategory(formalPool, ClothingCategory.BOTTOM, random),
            shoes = pickRandomByCategory(formalPool, ClothingCategory.SHOES, random),
            outerwear = if (isCold) pickRandomByCategory(formalPool, ClothingCategory.OUTERWEAR, random) else null,
            requiresOuterwear = isCold
        )

        return listOf(weatherSuggestion, casualSuggestion, formalSuggestion)
    }

    private fun pickRandomByCategory(
        pool: List<WardrobeItem>,
        category: ClothingCategory,
        random: Random
    ): WardrobeItem? {
        val candidates = pool.filter { ClothingCategory.fromDb(it.category) == category }
        if (candidates.isEmpty()) return null
        return candidates[random.nextInt(candidates.size)]
    }
}

object SampleWardrobeData {
    fun getStarterItems(isArabic: Boolean): List<WardrobeItem> = listOf(
        // Tops
        WardrobeItem(
            name = if (isArabic) "قميص كتان أخضر مريمي" else "Sage Linen Overshirt",
            imageUrl = "preset:top_sage_linen",
            category = ClothingCategory.TOP.dbValue,
            weather = WeatherType.MILD.dbValue,
            style = StyleType.CASUAL.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "تيشيرت قطني أبيض ناصع" else "Crisp Alabaster Cotton Tee",
            imageUrl = "preset:top_white_tee",
            category = ClothingCategory.TOP.dbValue,
            weather = WeatherType.HOT.dbValue,
            style = StyleType.CASUAL.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "قميص أكسفورد رسمي أبيض" else "Tailored White Oxford Shirt",
            imageUrl = "preset:top_oxford_shirt",
            category = ClothingCategory.TOP.dbValue,
            weather = WeatherType.ALL_SEASON.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "كنزة صوف ميرينو رمادية" else "Slate Merino Wool Knit",
            imageUrl = "preset:top_merino_knit",
            category = ClothingCategory.TOP.dbValue,
            weather = WeatherType.COLD.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "تيشيرت رياضي تقني" else "Performance Dry-Fit Top",
            imageUrl = "preset:top_sport_tee",
            category = ClothingCategory.TOP.dbValue,
            weather = WeatherType.HOT.dbValue,
            style = StyleType.SPORT.dbValue
        ),
        // Outerwear
        WardrobeItem(
            name = if (isArabic) "معطف صوف طويل رمادي داكن" else "Charcoal Tailored Wool Overcoat",
            imageUrl = "preset:outer_wool_coat",
            category = ClothingCategory.OUTERWEAR.dbValue,
            weather = WeatherType.COLD.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "جاكيت مبطن مقاوم للرياح" else "Sage Quilted Puffer Jacket",
            imageUrl = "preset:outer_puffer",
            category = ClothingCategory.OUTERWEAR.dbValue,
            weather = WeatherType.COLD.dbValue,
            style = StyleType.CASUAL.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "بليزر رسمي كحلي" else "Structured Slate Blazer",
            imageUrl = "preset:outer_blazer",
            category = ClothingCategory.OUTERWEAR.dbValue,
            weather = WeatherType.MILD.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        // Bottoms
        WardrobeItem(
            name = if (isArabic) "بنطال تشينو بيج كلاسيكي" else "Sand Tailored Chino Trousers",
            imageUrl = "preset:bottom_chino",
            category = ClothingCategory.BOTTOM.dbValue,
            weather = WeatherType.ALL_SEASON.dbValue,
            style = StyleType.CASUAL.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "بنطال قماش رسمي رمادي" else "Pleated Slate Dress Trousers",
            imageUrl = "preset:bottom_dress_pants",
            category = ClothingCategory.BOTTOM.dbValue,
            weather = WeatherType.ALL_SEASON.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "بنطال رياضي مريح" else "Technical Tapered Track Pants",
            imageUrl = "preset:bottom_joggers",
            category = ClothingCategory.BOTTOM.dbValue,
            weather = WeatherType.MILD.dbValue,
            style = StyleType.SPORT.dbValue
        ),
        // Shoes
        WardrobeItem(
            name = if (isArabic) "حذاء سنيكرز جلد أبيض" else "Minimalist White Leather Sneakers",
            imageUrl = "preset:shoes_white_sneakers",
            category = ClothingCategory.SHOES.dbValue,
            weather = WeatherType.ALL_SEASON.dbValue,
            style = StyleType.CASUAL.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "حذاء لوفر جلد كلاسيكي" else "Espresso Penny Loafers",
            imageUrl = "preset:shoes_loafers",
            category = ClothingCategory.SHOES.dbValue,
            weather = WeatherType.ALL_SEASON.dbValue,
            style = StyleType.FORMAL_SMART.dbValue
        ),
        WardrobeItem(
            name = if (isArabic) "حذاء جري خفيف" else "Sage Knit Running Trainers",
            imageUrl = "preset:shoes_runners",
            category = ClothingCategory.SHOES.dbValue,
            weather = WeatherType.HOT.dbValue,
            style = StyleType.SPORT.dbValue
        )
    )
}
