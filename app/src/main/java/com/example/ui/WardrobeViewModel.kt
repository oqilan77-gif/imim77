package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AutoDetectedClothing
import com.example.data.ChatMessage
import com.example.data.ClothingCategory
import com.example.data.GeminiRepository
import com.example.data.OutfitGeneratorEngine
import com.example.data.OutfitSuggestion
import com.example.data.StyleType
import com.example.data.StylistChatMode
import com.example.data.SuggestionKind
import com.example.data.WardrobeItem
import com.example.data.WardrobeRepository
import com.example.data.WeatherType
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class WardrobeViewModel(
    private val repository: WardrobeRepository,
    private val geminiRepository: GeminiRepository,
    private val currentUserId: String
) : ViewModel() {

    // Default to Arabic RTL as confirmed in Phase 1
    private val _isArabic = MutableStateFlow(true)
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    private val _selectedWeather = MutableStateFlow(WeatherType.MILD)
    val selectedWeather: StateFlow<WeatherType> = _selectedWeather.asStateFlow()

    private val _selectedClosetCategory = MutableStateFlow<ClothingCategory?>(null)
    val selectedClosetCategory: StateFlow<ClothingCategory?> = _selectedClosetCategory.asStateFlow()

    private val _generatedOutfits = MutableStateFlow<List<OutfitSuggestion>>(emptyList())
    val generatedOutfits: StateFlow<List<OutfitSuggestion>> = _generatedOutfits.asStateFlow()

    private val _outfitTips = MutableStateFlow<Map<SuggestionKind, String>>(emptyMap())
    val outfitTips: StateFlow<Map<SuggestionKind, String>> = _outfitTips.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // AI Stylist Studio state
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                isUser = false,
                text = "مرحباً بك في استوديو المنسق الذكي! يمكنني تنسيق إطلالاتك، التخطيط لخزانة كبسولية بتفكير عميق، أو البحث عن متاجر وخياطين قريبين عبر خرائط Google.",
                modelBadge = "AI Stylist"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedChatMode = MutableStateFlow(StylistChatMode.FAST)
    val selectedChatMode: StateFlow<StylistChatMode> = _selectedChatMode.asStateFlow()

    private val _selectedImageSize = MutableStateFlow("1K")
    val selectedImageSize: StateFlow<String> = _selectedImageSize.asStateFlow()

    private val _generatedLookbookBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedLookbookBitmap: StateFlow<Bitmap?> = _generatedLookbookBitmap.asStateFlow()

    private val _videoAnalysisResult = MutableStateFlow<String?>(null)
    val videoAnalysisResult: StateFlow<String?> = _videoAnalysisResult.asStateFlow()

    private var latestItemsCache: List<WardrobeItem> = emptyList()

    val wardrobeState: StateFlow<UiState<List<WardrobeItem>>> =
        repository.observeWardrobeItems(currentUserId)
            .onEach { items ->
                latestItemsCache = items
                if (_generatedOutfits.value.isEmpty() && items.isNotEmpty()) {
                    _generatedOutfits.value = OutfitGeneratorEngine.generateOutfits(
                        allItems = items,
                        selectedWeather = _selectedWeather.value
                    )
                }
            }
            .map<List<WardrobeItem>, UiState<List<WardrobeItem>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing wardrobe items", error)
                emit(UiState.Error(error.message ?: "Failed to load wardrobe"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun toggleLanguage() {
        _isArabic.value = !_isArabic.value
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun selectWeather(weather: WeatherType) {
        _selectedWeather.value = weather
        generateOutfitsNow()
    }

    fun generateOutfitsNow() {
        _generatedOutfits.value = OutfitGeneratorEngine.generateOutfits(
            allItems = latestItemsCache,
            selectedWeather = _selectedWeather.value,
            seed = System.nanoTime()
        )
    }

    fun selectClosetCategory(category: ClothingCategory?) {
        _selectedClosetCategory.value = category
    }

    fun selectChatMode(mode: StylistChatMode) {
        _selectedChatMode.value = mode
    }

    fun selectImageSize(size: String) {
        _selectedImageSize.value = size
    }

    fun addClothingItem(
        name: String,
        category: ClothingCategory,
        weather: WeatherType,
        style: StyleType,
        imageUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            _statusBanner.value = if (_isArabic.value) "يرجى إدخال اسم القطعة" else "Please enter an item name"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val newItem = WardrobeItem(
                name = name.trim(),
                imageUrl = imageUrl.ifBlank { defaultPresetForCategory(category) },
                category = category.dbValue,
                weather = weather.dbValue,
                style = style.dbValue
            )
            val result = repository.createWardrobeItem(newItem)
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = if (_isArabic.value) "تمت إضافة القطعة إلى خزانتك بنجاح" else "Item added to your closet"
                onSuccess()
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to save item"
            }
        }
    }

    fun deleteClothingItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteWardrobeItem(itemId)
            result.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to delete item"
            }
        }
    }

    fun seedSampleWardrobe() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedStarterWardrobe(_isArabic.value)
            _isBusy.value = false
            result.onSuccess { count ->
                _statusBanner.value = if (_isArabic.value) {
                    "تمت إضافة $count قطعة نموذجية إلى خزانتك!"
                } else {
                    "Added $count sample capsule items to your closet!"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to seed wardrobe"
            }
        }
    }

    fun autoAnalyzePhoto(bitmap: Bitmap, onDetected: (AutoDetectedClothing) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.analyzeClothingPhoto(bitmap)
            _isBusy.value = false
            res.onSuccess { detected ->
                _statusBanner.value = if (_isArabic.value) {
                    "تم التعرف على القطعة بواسطة Gemini 4 Argon"
                } else {
                    "Auto-filled details using Gemini 4 Argon Vision"
                }
                onDetected(detected)
            }.onFailure { e ->
                _statusBanner.value = if (_isArabic.value) {
                    "تعذر التحليل التلقائي (تأكد من إعداد GEMINI_API_KEY في لوحة Secrets): ${e.localizedMessage}"
                } else {
                    "AI Vision error (check GEMINI_API_KEY in Secrets panel): ${e.localizedMessage}"
                }
            }
        }
    }

    fun fetchQuickTipForOutfit(suggestion: OutfitSuggestion) {
        viewModelScope.launch {
            val summary = suggestion.summaryForSpeech(_isArabic.value)
            val res = geminiRepository.generateQuickStylistTip(summary, _isArabic.value)
            res.onSuccess { tip ->
                _outfitTips.value = _outfitTips.value + (suggestion.kind to tip)
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Fast tip error"
            }
        }
    }

    fun speakText(text: String, cacheDir: File) {
        viewModelScope.launch {
            _statusBanner.value = if (_isArabic.value) "جارٍ توليد الصوت عبر gemini-3.8-flash-tts..." else "Generating speech via gemini-3.8-flash-tts..."
            val res = geminiRepository.speakTextWithGeminiTts(text, cacheDir)
            res.onSuccess {
                _statusBanner.value = if (_isArabic.value) "يتم تشغيل النص الصوتي الآن" else "Playing stylist voiceover"
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "TTS failed"
            }
        }
    }

    fun restyleWardrobeItem(
        item: WardrobeItem,
        prompt: String,
        sourceBitmap: Bitmap?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.editOrCreateGarmentImage(
                prompt = "Restyle this ${item.category} (${item.name}): $prompt. Keep clean studio background.",
                sourceBitmap = sourceBitmap
            )
            res.onSuccess { newBmp ->
                val dataUri = GeminiRepository.bitmapToDataUri(newBmp)
                repository.updateWardrobeItem(item.copy(imageUrl = dataUri))
                _isBusy.value = false
                _statusBanner.value = if (_isArabic.value) {
                    "تم تعديل صورة القطعة عبر gemini-nano-banana-2.1"
                } else {
                    "Restyled garment with gemini-nano-banana-2.1"
                }
                onSuccess()
            }.onFailure { e ->
                _isBusy.value = false
                _statusBanner.value = e.localizedMessage ?: "Image edit failed"
            }
        }
    }

    fun sendStylistMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(isUser = true, text = text.trim())
        val previousHistory = _chatMessages.value
        _chatMessages.value = previousHistory + userMsg

        val mode = _selectedChatMode.value
        val wardrobeSummary = latestItemsCache.joinToString(", ") {
            "${it.name} (${it.category}, ${it.weather}, ${it.style})"
        }.ifBlank { "Empty closet" }

        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.sendStylistChat(
                history = previousHistory,
                userMessage = text.trim(),
                wardrobeContext = wardrobeSummary,
                mode = mode,
                isArabic = _isArabic.value
            )
            _isBusy.value = false
            res.onSuccess { reply ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    isUser = false,
                    text = reply,
                    modelBadge = mode.modelId
                )
            }.onFailure { e ->
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    isUser = false,
                    text = if (_isArabic.value) {
                        "تنبيه: تأكد من إضافة GEMINI_API_KEY في لوحة Secrets. (${e.localizedMessage})"
                    } else {
                        "Note: Please ensure GEMINI_API_KEY is configured in the AI Studio Secrets panel. (${e.localizedMessage})"
                    },
                    modelBadge = mode.modelId
                )
            }
        }
    }

    fun transcribeVoiceNote(audioBytes: ByteArray, mimeType: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.transcribeAudio(audioBytes, mimeType, _isArabic.value)
            _isBusy.value = false
            res.onSuccess { text ->
                if (text.isNotBlank()) {
                    onResult(text)
                } else {
                    _statusBanner.value = if (_isArabic.value) "لم يتم التعرف على كلام واضح" else "No speech detected"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Transcription error"
            }
        }
    }

    fun generateHighResLookbook(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            val size = _selectedImageSize.value
            val res = geminiRepository.generateHighQualityLookbook(prompt, size)
            _isBusy.value = false
            res.onSuccess { bmp ->
                _generatedLookbookBitmap.value = bmp
                _statusBanner.value = if (_isArabic.value) {
                    "تم توليد الإطلالة بدقة $size عبر gemini-3-pro-image-preview"
                } else {
                    "Generated $size lookbook via gemini-3-pro-image-preview"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Lookbook generation failed"
            }
        }
    }

    fun generateNanoBananaImage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.editOrCreateGarmentImage(prompt, _generatedLookbookBitmap.value)
            _isBusy.value = false
            res.onSuccess { bmp ->
                _generatedLookbookBitmap.value = bmp
                _statusBanner.value = if (_isArabic.value) {
                    "تم تصميم/تعديل الصورة عبر gemini-nano-banana-2.1"
                } else {
                    "Created/edited image via gemini-nano-banana-2.1"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Nano Banana 2.1 error"
            }
        }
    }

    fun analyzeVideoOutfit(videoBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.analyzeOutfitVideo(videoBytes, mimeType, _isArabic.value)
            _isBusy.value = false
            res.onSuccess { critique ->
                _videoAnalysisResult.value = critique
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Video analysis error"
            }
        }
    }

    fun generateRunwayAudio(prompt: String, isFullTrack: Boolean, cacheDir: File) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = geminiRepository.generateRunwayMusic(
                prompt = prompt.ifBlank { "Minimalist luxury fashion boutique ambient electronic lounge track" },
                isFullTrack = isFullTrack,
                cacheDir = cacheDir
            )
            _isBusy.value = false
            res.onSuccess { modelUsed ->
                _statusBanner.value = if (_isArabic.value) {
                    "يتم تشغيل الموسيقى المولدة عبر $modelUsed"
                } else {
                    "Playing generated runway track ($modelUsed)"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Music generation error"
            }
        }
    }

    private fun defaultPresetForCategory(category: ClothingCategory): String = when (category) {
        ClothingCategory.TOP -> "preset:top_sage_linen"
        ClothingCategory.OUTERWEAR -> "preset:outer_wool_coat"
        ClothingCategory.BOTTOM -> "preset:bottom_chino"
        ClothingCategory.SHOES -> "preset:shoes_white_sneakers"
    }

    private companion object {
        const val TAG = "WardrobeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
