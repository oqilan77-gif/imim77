package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ClothingCategory

@Composable
fun GarmentThumbnail(
    imageUrl: String,
    category: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(imageUrl) {
        if (imageUrl.startsWith("data:image")) {
            try {
                val base64Part = imageUrl.substringAfter(",")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            imageUrl.startsWith("http") || imageUrl.startsWith("content://") || imageUrl.startsWith("file://") -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                PresetGarmentIllustration(
                    presetKey = imageUrl,
                    category = ClothingCategory.fromDb(category),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PresetGarmentIllustration(
    presetKey: String,
    category: ClothingCategory,
    modifier: Modifier = Modifier
) {
    val (bgStart, bgEnd, garmentColor) = remember(presetKey, category) {
        when {
            presetKey.contains("sage") || presetKey.contains("puffer") || presetKey.contains("runners") ->
                Triple(Color(0xFFE6F0EC), Color(0xFFD2E5DC), Color(0xFF3D6B58))
            presetKey.contains("white") || presetKey.contains("oxford") ->
                Triple(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFF8FAFC))
            presetKey.contains("wool") || presetKey.contains("merino") || presetKey.contains("dress") ->
                Triple(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF334155))
            presetKey.contains("chino") || presetKey.contains("loafers") ->
                Triple(Color(0xFFF5EFE6), Color(0xFFE8DEC8), Color(0xFF785A3C))
            else -> when (category) {
                ClothingCategory.TOP -> Triple(Color(0xFFE6F0EC), Color(0xFFDCECE4), Color(0xFF3D6B58))
                ClothingCategory.OUTERWEAR -> Triple(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF1E293B))
                ClothingCategory.BOTTOM -> Triple(Color(0xFFF3ECE1), Color(0xFFE5Dec9), Color(0xFF5C4D3C))
                ClothingCategory.SHOES -> Triple(Color(0xFFEEF2F6), Color(0xFFDCE3EC), Color(0xFF475569))
            }
        }
    }

    Canvas(
        modifier = modifier.background(Brush.linearGradient(listOf(bgStart, bgEnd)))
    ) {
        val w = size.width
        val h = size.height
        val strokeColor = Color(0xFF1E293B).copy(alpha = 0.25f)

        // Subtle studio backdrop circle
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = minOf(w, h) * 0.36f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        when (category) {
            ClothingCategory.TOP -> {
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.24f)
                    lineTo(w * 0.65f, h * 0.24f)
                    lineTo(w * 0.82f, h * 0.40f)
                    lineTo(w * 0.72f, h * 0.50f)
                    lineTo(w * 0.64f, h * 0.42f)
                    lineTo(w * 0.64f, h * 0.78f)
                    lineTo(w * 0.36f, h * 0.78f)
                    lineTo(w * 0.36f, h * 0.42f)
                    lineTo(w * 0.28f, h * 0.50f)
                    lineTo(w * 0.18f, h * 0.40f)
                    close()
                }
                drawPath(path, color = garmentColor)
                drawPath(path, color = strokeColor, style = Stroke(width = 3f))
            }
            ClothingCategory.OUTERWEAR -> {
                val path = Path().apply {
                    moveTo(w * 0.34f, h * 0.20f)
                    lineTo(w * 0.66f, h * 0.20f)
                    lineTo(w * 0.84f, h * 0.42f)
                    lineTo(w * 0.74f, h * 0.68f)
                    lineTo(w * 0.66f, h * 0.68f)
                    lineTo(w * 0.66f, h * 0.82f)
                    lineTo(w * 0.34f, h * 0.82f)
                    lineTo(w * 0.34f, h * 0.68f)
                    lineTo(w * 0.26f, h * 0.68f)
                    lineTo(w * 0.16f, h * 0.42f)
                    close()
                }
                drawPath(path, color = garmentColor)
                drawLine(
                    color = strokeColor,
                    start = Offset(w * 0.5f, h * 0.24f),
                    end = Offset(w * 0.5f, h * 0.82f),
                    strokeWidth = 3f
                )
                drawPath(path, color = strokeColor, style = Stroke(width = 3f))
            }
            ClothingCategory.BOTTOM -> {
                val path = Path().apply {
                    moveTo(w * 0.34f, h * 0.20f)
                    lineTo(w * 0.66f, h * 0.20f)
                    lineTo(w * 0.70f, h * 0.80f)
                    lineTo(w * 0.54f, h * 0.80f)
                    lineTo(w * 0.50f, h * 0.44f)
                    lineTo(w * 0.46f, h * 0.80f)
                    lineTo(w * 0.30f, h * 0.80f)
                    close()
                }
                drawPath(path, color = garmentColor)
                drawPath(path, color = strokeColor, style = Stroke(width = 3f))
            }
            ClothingCategory.SHOES -> {
                drawRoundRect(
                    color = garmentColor,
                    topLeft = Offset(w * 0.22f, h * 0.44f),
                    size = Size(w * 0.56f, h * 0.22f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.20f, h * 0.62f),
                    size = Size(w * 0.60f, h * 0.08f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
    }
}

@Composable
fun EditorialWardrobeHeroBanner(
    isArabic: Boolean,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.height * 0.75f,
                    center = Offset(size.width * 0.15f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.height * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.85f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (isArabic) "منسق الأزياء الشخصي الذكي" else "PERSONAL AI STYLIST",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) "خزانة الملابس الذكية" else "Smart Digital Wardrobe",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) {
                            "لديك $itemCount قطعة مسجلة • جاهزة لتوليد ٣ إطلالات متكاملة"
                        } else {
                            "$itemCount pieces in your closet • Ready for 3 curated outfits"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = if (isArabic) "أيقونة الخزانة" else "Wardrobe icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}
