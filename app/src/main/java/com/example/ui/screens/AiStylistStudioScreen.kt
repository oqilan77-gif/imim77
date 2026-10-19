package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessage
import com.example.data.ClothingCategory
import com.example.data.GeminiRepository
import com.example.data.StyleType
import com.example.data.StylistChatMode
import com.example.data.WeatherType
import java.io.File

@Composable
fun AiStylistStudioScreen(
    isArabic: Boolean,
    isBusy: Boolean,
    chatMessages: List<ChatMessage>,
    selectedChatMode: StylistChatMode,
    selectedImageSize: String,
    generatedLookbookBitmap: Bitmap?,
    videoAnalysisResult: String?,
    onSelectChatMode: (StylistChatMode) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSpeakText: (String, File) -> Unit,
    onTranscribeAudio: (ByteArray, String, (String) -> Unit) -> Unit,
    onSelectImageSize: (String) -> Unit,
    onGenerateProLookbook: (String) -> Unit,
    onGenerateNanoBanana: (String) -> Unit,
    onSaveGeneratedLookToCloset: (String, ClothingCategory, WeatherType, StyleType, String) -> Unit,
    onAnalyzeVideo: (ByteArray, String) -> Unit,
    onGenerateRunwayMusic: (String, Boolean, File) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = if (isArabic) {
        listOf("محادثة المنسق", "تصميم الإطلالات", "فيديو وموسيقى")
    } else {
        listOf("Stylist Chat", "Lookbook Gen", "Video & Music")
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    modifier = Modifier.testTag("studio_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> StylistChatSection(
                isArabic = isArabic,
                isBusy = isBusy,
                chatMessages = chatMessages,
                selectedChatMode = selectedChatMode,
                onSelectChatMode = onSelectChatMode,
                onSendChatMessage = onSendChatMessage,
                onSpeakText = onSpeakText,
                onTranscribeAudio = onTranscribeAudio
            )
            1 -> LookbookImageStudioSection(
                isArabic = isArabic,
                isBusy = isBusy,
                selectedImageSize = selectedImageSize,
                generatedLookbookBitmap = generatedLookbookBitmap,
                onSelectImageSize = onSelectImageSize,
                onGenerateProLookbook = onGenerateProLookbook,
                onGenerateNanoBanana = onGenerateNanoBanana,
                onSaveGeneratedLookToCloset = onSaveGeneratedLookToCloset
            )
            2 -> VideoAndMusicStudioSection(
                isArabic = isArabic,
                isBusy = isBusy,
                videoAnalysisResult = videoAnalysisResult,
                onAnalyzeVideo = onAnalyzeVideo,
                onGenerateRunwayMusic = onGenerateRunwayMusic
            )
        }
    }
}

@Composable
private fun StylistChatSection(
    isArabic: Boolean,
    isBusy: Boolean,
    chatMessages: List<ChatMessage>,
    selectedChatMode: StylistChatMode,
    onSelectChatMode: (StylistChatMode) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSpeakText: (String, File) -> Unit,
    onTranscribeAudio: (ByteArray, String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    val startRecordingAction = {
        try {
            val outFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            audioFile = outFile
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecordingAction()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Model Mode Selector Chips (Fast Flash Lite, Maps Grounding Flash 3.8, High Thinking Argon 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StylistChatMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedChatMode == mode,
                    onClick = { onSelectChatMode(mode) },
                    label = {
                        Text(
                            text = if (isArabic) mode.labelAr else mode.labelEn,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("stylist_chat_thread"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val align = if (msg.isUser) Alignment.End else Alignment.Start
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = align
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (msg.isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth(0.86f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (msg.modelBadge != null && !msg.isUser) {
                                Text(
                                    text = msg.modelBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (msg.isUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (!msg.isUser) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { onSpeakText(msg.text, context.cacheDir) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = if (isArabic) "استماع للرد" else "Speak reply",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Bar with Microphone Transcription (gemini-3.5-transcribe) + Send
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (isRecording) {
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                        } catch (_: Exception) {}
                        mediaRecorder = null
                        isRecording = false
                        val recorded = audioFile
                        if (recorded != null && recorded.exists()) {
                            onTranscribeAudio(recorded.readBytes(), "audio/mp4") { transcribed ->
                                inputText = transcribed
                            }
                        }
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            startRecordingAction()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.testTag("voice_transcribe_button")
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isArabic) "تسجيل صوتي" else "Voice input",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isArabic) "اسأل المنسق أو ابحث عن متجر قريب..." else "Ask stylist or find nearby tailors..."
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendChatMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isBusy && inputText.isNotBlank(),
                modifier = Modifier.testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isArabic) "إرسال" else "Send",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LookbookImageStudioSection(
    isArabic: Boolean,
    isBusy: Boolean,
    selectedImageSize: String,
    generatedLookbookBitmap: Bitmap?,
    onSelectImageSize: (String) -> Unit,
    onGenerateProLookbook: (String) -> Unit,
    onGenerateNanoBanana: (String) -> Unit,
    onSaveGeneratedLookToCloset: (String, ClothingCategory, WeatherType, StyleType, String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val sizes = listOf("1K", "2K", "4K")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "توليد وتصميم صور الأزياء بالذكاء الاصطناعي" else "AI Fashion Lookbook & Garment Generator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isArabic) "اختر دقة الصورة (لـ gemini-3-pro-image-preview):" else "Select Image Resolution (gemini-3-pro-image-preview):",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { size ->
                        FilterChip(
                            selected = selectedImageSize == size,
                            onClick = { onSelectImageSize(size) },
                            label = { Text(size) },
                            modifier = Modifier.testTag("size_selector_${size.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = {
                        Text(if (isArabic) "وصف الإطلالة أو القطعة" else "Lookbook or Garment Prompt")
                    },
                    placeholder = {
                        Text(
                            if (isArabic) {
                                "مثال: معطف كشمير أخضر مريمي مع بنطال أبيض وحذاء جلدي"
                            } else {
                                "e.g., Sage green cashmere coat with white trousers and loafers"
                            }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lookbook_prompt_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onGenerateProLookbook(prompt) },
                        enabled = !isBusy && prompt.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_pro_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "توليد Pro ($selectedImageSize)" else "Pro Image ($selectedImageSize)")
                    }

                    FilledTonalButton(
                        onClick = { onGenerateNanoBanana(prompt) },
                        enabled = !isBusy && prompt.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_nano_banana_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nano Banana 2.1")
                    }
                }

                if (generatedLookbookBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Image(
                            bitmap = generatedLookbookBitmap.asImageBitmap(),
                            contentDescription = "Generated Lookbook",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val dataUri = GeminiRepository.bitmapToDataUri(generatedLookbookBitmap)
                            onSaveGeneratedLookToCloset(
                                prompt.take(50).ifBlank { "AI Custom Garment" },
                                ClothingCategory.TOP,
                                WeatherType.ALL_SEASON,
                                StyleType.CASUAL,
                                dataUri
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "حفظ التصميم في خزانتي" else "Save Generated Piece to My Closet")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoAndMusicStudioSection(
    isArabic: Boolean,
    isBusy: Boolean,
    videoAnalysisResult: String?,
    onAnalyzeVideo: (ByteArray, String) -> Unit,
    onGenerateRunwayMusic: (String, Boolean, File) -> Unit
) {
    val context = LocalContext.current
    var musicPrompt by remember {
        mutableStateOf(
            if (isArabic) "موسيقى هادئة لعرض أزياء عصري" else "Minimalist luxury fashion boutique ambient track"
        )
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                    onAnalyzeVideo(bytes, mime)
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video Analysis Card (gemini-4-argon)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تحليل فيديو الإطلالة (Gemini 4 Argon)" else "Outfit Video Check (Gemini 4 Argon)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isArabic) {
                        "ارفع مقطع فيديو قصير لإطلالتك ليقوم Gemini 4 Argon بتقييم تناسق الألوان والقصة."
                    } else {
                        "Upload a short outfit clip for Gemini 4 Argon to analyze color harmony, drape, and occasion fit."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_video_button")
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "اختيار فيديو للتحليل" else "Select Outfit Video")
                }

                if (videoAnalysisResult != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = videoAnalysisResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Music Generation Card (lyria-3-clip-preview & lyria-3-pro-preview)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "موسيقى عرض الأزياء (Lyria 3)" else "Runway & Boutique Audio (Lyria 3)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = musicPrompt,
                    onValueChange = { musicPrompt = it },
                    label = { Text(if (isArabic) "وصف الموسيقى" else "Music Style Prompt") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_prompt_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onGenerateRunwayMusic(musicPrompt, false, context.cacheDir) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_music_clip_button")
                    ) {
                        Text(if (isArabic) "مقطع ٣٠ ثانية (Clip)" else "30s Clip (Lyria)")
                    }

                    FilledTonalButton(
                        onClick = { onGenerateRunwayMusic(musicPrompt, true, context.cacheDir) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_music_pro_button")
                    ) {
                        Text(if (isArabic) "مقطع كامل (Pro)" else "Full Track (Pro)")
                    }
                }
            }
        }
    }
}
