package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AutoDetectedClothing
import com.example.data.ClothingCategory
import com.example.data.GeminiRepository
import com.example.data.StyleType
import com.example.data.WeatherType
import com.example.ui.components.GarmentThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothesScreen(
    isArabic: Boolean,
    isBusy: Boolean,
    onAutoAnalyzeBitmap: (Bitmap, (AutoDetectedClothing) -> Unit) -> Unit,
    onSaveItem: (String, ClothingCategory, WeatherType, StyleType, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ClothingCategory.TOP) }
    var selectedWeather by remember { mutableStateOf(WeatherType.ALL_SEASON) }
    var selectedStyle by remember { mutableStateOf(StyleType.CASUAL) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUrlState by remember { mutableStateOf("preset:top_sage_linen") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var weatherExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bmp
                imageUrlState = GeminiRepository.bitmapToDataUri(bmp)
            } catch (e: Exception) {
                // Keep default preset on decode error
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isArabic) "إضافة قطعة ملابس جديدة" else "Add New Clothing Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Image Preview & Upload Controls
                GarmentThumbnail(
                    imageUrl = imageUrlState,
                    category = selectedCategory.dbValue,
                    contentDescription = if (isArabic) "معاينة الصورة" else "Clothing preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "اختيار صورة" else "Upload Photo")
                    }

                    FilledTonalButton(
                        onClick = {
                            val bmp = selectedBitmap
                            if (bmp != null) {
                                onAutoAnalyzeBitmap(bmp) { detected ->
                                    itemName = if (isArabic) {
                                        detected.nameAr.ifBlank { detected.nameEn }
                                    } else {
                                        detected.nameEn.ifBlank { detected.nameAr }
                                    }
                                    selectedCategory = ClothingCategory.fromDb(detected.category)
                                    selectedWeather = WeatherType.fromDb(detected.weather)
                                    selectedStyle = StyleType.fromDb(detected.style)
                                }
                            }
                        },
                        enabled = selectedBitmap != null && !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_autofill_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "تحليل ذكي (AI)" else "AI Auto-Fill")
                    }
                }

                // Item Name Text Field
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = {
                        Text(if (isArabic) "اسم القطعة (Item Name)" else "Item Name")
                    },
                    placeholder = {
                        Text(if (isArabic) "مثال: قميص كتان أبيض" else "e.g., White Linen Shirt")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                // Category Dropdown (Top, Outerwear, Bottom, Shoes)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label(isArabic),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (isArabic) "الفئة (Category)" else "Category")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ClothingCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label(isArabic)) },
                                onClick = {
                                    selectedCategory = category
                                    if (selectedBitmap == null) {
                                        imageUrlState = when (category) {
                                            ClothingCategory.TOP -> "preset:top_sage_linen"
                                            ClothingCategory.OUTERWEAR -> "preset:outer_wool_coat"
                                            ClothingCategory.BOTTOM -> "preset:bottom_chino"
                                            ClothingCategory.SHOES -> "preset:shoes_white_sneakers"
                                        }
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Weather Dropdown (Cold, Mild, Hot, All-season)
                ExposedDropdownMenuBox(
                    expanded = weatherExpanded,
                    onExpandedChange = { weatherExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWeather.label(isArabic),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (isArabic) "الطقس المناسب (Weather)" else "Weather")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = weatherExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("weather_item_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = weatherExpanded,
                        onDismissRequest = { weatherExpanded = false }
                    ) {
                        WeatherType.entries.forEach { weather ->
                            DropdownMenuItem(
                                text = { Text(weather.label(isArabic)) },
                                onClick = {
                                    selectedWeather = weather
                                    weatherExpanded = false
                                }
                            )
                        }
                    }
                }

                // Style Dropdown (Casual, Formal/Smart, Sport)
                ExposedDropdownMenuBox(
                    expanded = styleExpanded,
                    onExpandedChange = { styleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStyle.label(isArabic),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (isArabic) "النمط / الستايل (Style)" else "Style")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("style_item_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = styleExpanded,
                        onDismissRequest = { styleExpanded = false }
                    ) {
                        StyleType.entries.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style.label(isArabic)) },
                                onClick = {
                                    selectedStyle = style
                                    styleExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSaveItem(
                            itemName,
                            selectedCategory,
                            selectedWeather,
                            selectedStyle,
                            imageUrlState
                        )
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_clothing_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "حفظ في الخزانة" else "Save to Closet",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
