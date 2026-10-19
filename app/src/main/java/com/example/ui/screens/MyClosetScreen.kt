package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ClothingCategory
import com.example.data.StyleType
import com.example.data.WardrobeItem
import com.example.data.WeatherType
import com.example.ui.components.GarmentThumbnail

@Composable
fun MyClosetScreen(
    isArabic: Boolean,
    allItems: List<WardrobeItem>,
    selectedCategory: ClothingCategory?,
    isBusy: Boolean,
    onSelectCategory: (ClothingCategory?) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRestyleItemWithNanoBanana: (WardrobeItem, String) -> Unit,
    onSeedSampleWardrobe: () -> Unit,
    onNavigateToAddClothes: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val filteredItems by remember(allItems, selectedCategory) {
        derivedStateOf {
            if (selectedCategory == null) {
                allItems
            } else {
                allItems.filter { ClothingCategory.fromDb(it.category) == selectedCategory }
            }
        }
    }

    var itemToRestyle by remember { mutableStateOf<WardrobeItem?>(null) }
    var restylePrompt by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("closet_category_filter_row")
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = {
                        Text(
                            if (isArabic) "الكل (${allItems.size})" else "All (${allItems.size})"
                        )
                    },
                    modifier = Modifier.testTag("category_filter_all")
                )
            }
            items(ClothingCategory.entries) { category ->
                val count = allItems.count { ClothingCategory.fromDb(it.category) == category }
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text("${category.label(isArabic)} ($count)") },
                    modifier = Modifier.testTag("category_filter_${category.dbValue.lowercase()}")
                )
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (isArabic) "لا توجد قطع في هذه الفئة" else "No items in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (allItems.isEmpty()) {
                                FilledTonalButton(
                                    onClick = onSeedSampleWardrobe,
                                    enabled = !isBusy
                                ) {
                                    Text(if (isArabic) "تحميل خزانة تجريبية" else "Load Sample Closet")
                                }
                            }
                            Button(onClick = onNavigateToAddClothes) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "إضافة قطعة" else "Add Item")
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("closet_items_grid")
            ) {
                items(
                    items = filteredItems,
                    key = { it.id.ifBlank { it.name } }
                ) { item ->
                    ClosetItemCard(
                        item = item,
                        isArabic = isArabic,
                        onDelete = { onDeleteItem(item.id) },
                        onOpenRestyle = {
                            itemToRestyle = item
                            restylePrompt = ""
                        }
                    )
                }
            }
        }
    }

    if (itemToRestyle != null) {
        val target = itemToRestyle!!
        AlertDialog(
            onDismissRequest = { itemToRestyle = null },
            title = {
                Text(
                    if (isArabic) "تعديل الصورة بالذكاء (Nano Banana 2.1)" else "Restyle Garment (Nano Banana 2.1)"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) {
                            "اكتب وصفاً لتعديل لون أو خامة '${target.name}' باستخدام نموذج gemini-nano-banana-2.1:"
                        } else {
                            "Describe how to recolor or restyle '${target.name}' using gemini-nano-banana-2.1:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = restylePrompt,
                        onValueChange = { restylePrompt = it },
                        placeholder = {
                            Text(
                                if (isArabic) "مثال: غيّر اللون إلى الكحلي مع قماش كتان" else "e.g., Change color to emerald green linen texture"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restyle_prompt_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restylePrompt.isNotBlank()) {
                            onRestyleItemWithNanoBanana(target, restylePrompt)
                            itemToRestyle = null
                        }
                    }
                ) {
                    Text(if (isArabic) "توليد وحفظ" else "Generate & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRestyle = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ClosetItemCard(
    item: WardrobeItem,
    isArabic: Boolean,
    onDelete: () -> Unit,
    onOpenRestyle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("closet_item_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            GarmentThumbnail(
                imageUrl = item.imageUrl,
                category = item.category,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = ClothingCategory.fromDb(item.category).label(isArabic),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${WeatherType.fromDb(item.weather).label(isArabic)} • ${StyleType.fromDb(item.style).label(isArabic)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenRestyle,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "تعديل ذكي" else "AI Edit")
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = if (isArabic) "حذف القطعة" else "Delete item",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
