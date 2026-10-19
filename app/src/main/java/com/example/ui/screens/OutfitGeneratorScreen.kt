package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ClothingCategory
import com.example.data.OutfitSuggestion
import com.example.data.StyleType
import com.example.data.SuggestionKind
import com.example.data.WardrobeItem
import com.example.data.WeatherType
import com.example.ui.components.EditorialWardrobeHeroBanner
import com.example.ui.components.GarmentThumbnail
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitGeneratorScreen(
    isArabic: Boolean,
    items: List<WardrobeItem>,
    selectedWeather: WeatherType,
    suggestions: List<OutfitSuggestion>,
    outfitTips: Map<SuggestionKind, String>,
    isBusy: Boolean,
    onSelectWeather: (WeatherType) -> Unit,
    onGenerateOutfits: () -> Unit,
    onNavigateToAddClothes: () -> Unit,
    onSeedSampleWardrobe: () -> Unit,
    onRequestFastTip: (OutfitSuggestion) -> Unit,
    onSpeakOutfit: (String, File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var weatherExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("outfit_generator_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EditorialWardrobeHeroBanner(
                isArabic = isArabic,
                itemCount = items.size
            )
        }

        // Weather Selector & Generate Outfits Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "اختر حالة الطقس الحالية" else "Select Current Weather",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Filter Chips + Dropdown for Current Weather (Cold, Mild, Hot)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherType.generatorOptions.forEach { weatherOption ->
                            FilterChip(
                                selected = selectedWeather == weatherOption,
                                onClick = { onSelectWeather(weatherOption) },
                                label = { Text(weatherOption.label(isArabic)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weather_chip_${weatherOption.dbValue.lowercase()}")
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = weatherExpanded,
                        onExpandedChange = { weatherExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedWeather.label(isArabic),
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(if (isArabic) "الطقس الحالي (Current Weather)" else "Current Weather")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = weatherExpanded)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("weather_dropdown_field")
                        )

                        ExposedDropdownMenu(
                            expanded = weatherExpanded,
                            onDismissRequest = { weatherExpanded = false }
                        ) {
                            WeatherType.generatorOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label(isArabic)) },
                                    onClick = {
                                        onSelectWeather(option)
                                        weatherExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onGenerateOutfits,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_outfits_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تنسيق ٣ إطلالات مقترحة" else "Generate 3 Outfit Suggestions",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyWardrobePromptCard(
                    isArabic = isArabic,
                    isBusy = isBusy,
                    onSeedSampleWardrobe = onSeedSampleWardrobe,
                    onNavigateToAddClothes = onNavigateToAddClothes
                )
            }
        } else {
            items(
                items = suggestions,
                key = { it.kind.name }
            ) { suggestion ->
                OutfitSuggestionCard(
                    suggestion = suggestion,
                    isArabic = isArabic,
                    fastTip = outfitTips[suggestion.kind],
                    onRegenerate = onGenerateOutfits,
                    onRequestFastTip = { onRequestFastTip(suggestion) },
                    onSpeakOutfit = {
                        onSpeakOutfit(suggestion.summaryForSpeech(isArabic), context.cacheDir)
                    },
                    onAddMissingClothes = onNavigateToAddClothes
                )
            }
        }
    }
}

@Composable
private fun EmptyWardrobePromptCard(
    isArabic: Boolean,
    isBusy: Boolean,
    onSeedSampleWardrobe: () -> Unit,
    onNavigateToAddClothes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (isArabic) "خزانتك فارغة حالياً" else "Your Digital Closet is Empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isArabic) {
                    "أضف ملابسك الخاصة أو قم بتحميل خزانة كبسولية تجريبية (١٤ قطعة متنوعة) لتجربة مولد الإطلالات الثلاث فوراً."
                } else {
                    "Add your own clothes or load a 14-piece sample capsule wardrobe to test the 3-tier Outfit Generator right away."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onSeedSampleWardrobe,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_sample_wardrobe_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "تحميل خزانة تجريبية" else "Load Sample Closet")
                }
                Button(
                    onClick = onNavigateToAddClothes,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_add_clothes_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "إضافة ملابس" else "Add Clothes")
                }
            }
        }
    }
}

@Composable
private fun OutfitSuggestionCard(
    suggestion: OutfitSuggestion,
    isArabic: Boolean,
    fastTip: String?,
    onRegenerate: () -> Unit,
    onRequestFastTip: () -> Unit,
    onSpeakOutfit: () -> Unit,
    onAddMissingClothes: () -> Unit
) {
    val badgeColor = when (suggestion.kind) {
        SuggestionKind.WEATHER_AWARE -> MaterialTheme.colorScheme.primaryContainer
        SuggestionKind.CASUAL_SPORT -> MaterialTheme.colorScheme.secondaryContainer
        SuggestionKind.FORMAL_SMART -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onBadgeColor = when (suggestion.kind) {
        SuggestionKind.WEATHER_AWARE -> MaterialTheme.colorScheme.onPrimaryContainer
        SuggestionKind.CASUAL_SPORT -> MaterialTheme.colorScheme.onSecondaryContainer
        SuggestionKind.FORMAL_SMART -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("outfit_card_${suggestion.kind.name.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isArabic) suggestion.titleAr else suggestion.titleEn,
                    color = onBadgeColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = if (isArabic) suggestion.subtitleAr else suggestion.subtitleEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Display slots: Outerwear (if Cold), Top, Bottom, Shoes
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (suggestion.requiresOuterwear) {
                    OutfitSlotRow(
                        slotLabel = ClothingCategory.OUTERWEAR.label(isArabic),
                        item = suggestion.outerwear,
                        isArabic = isArabic
                    )
                }
                OutfitSlotRow(
                    slotLabel = ClothingCategory.TOP.label(isArabic),
                    item = suggestion.top,
                    isArabic = isArabic
                )
                OutfitSlotRow(
                    slotLabel = ClothingCategory.BOTTOM.label(isArabic),
                    item = suggestion.bottom,
                    isArabic = isArabic
                )
                OutfitSlotRow(
                    slotLabel = ClothingCategory.SHOES.label(isArabic),
                    item = suggestion.shoes,
                    isArabic = isArabic
                )
            }

            val missing = suggestion.missingCategories(isArabic)
            if (missing.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) {
                                "ينقصك لهذه الإطلالة: ${missing.joinToString("، ")}"
                            } else {
                                "Missing for this look: ${missing.joinToString(", ")}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onAddMissingClothes) {
                            Text(if (isArabic) "إضافة" else "Add")
                        }
                    }
                }
            }

            if (fastTip != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fastTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Actions Row: TTS readout, Low-latency Stylist Tip, and Re-roll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSpeakOutfit,
                    enabled = suggestion.itemsList().isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tts_outfit_${suggestion.kind.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isArabic) "استماع" else "Listen",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "استماع (TTS)" else "Listen (TTS)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onRequestFastTip,
                    enabled = suggestion.itemsList().isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fast_tip_${suggestion.kind.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "نصيحة سريعة" else "Quick Tip",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onRegenerate,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isArabic) "إعادة تنسيق" else "Shuffle"
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitSlotRow(
    slotLabel: String,
    item: WardrobeItem?,
    isArabic: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item != null) {
                GarmentThumbnail(
                    imageUrl = item.imageUrl,
                    category = item.category,
                    contentDescription = item.name,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item?.name ?: if (isArabic) "غير متوفر في الخزانة" else "Not found in closet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (item != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                if (item != null) {
                    Text(
                        text = "${WeatherType.fromDb(item.weather).label(isArabic)} • ${StyleType.fromDb(item.style).label(isArabic)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
