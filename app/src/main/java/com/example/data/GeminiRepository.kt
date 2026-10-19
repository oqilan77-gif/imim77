package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import com.example.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String? = null
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig
)

@Serializable
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@Serializable
data class PrebuiltVoiceConfig(
    val voiceName: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class AutoDetectedClothing(
    val nameAr: String = "",
    val nameEn: String = "",
    val category: String = "Top",
    val weather: String = "All-season",
    val style: String = "Casual"
)

enum class StylistChatMode(
    val labelAr: String,
    val labelEn: String,
    val modelId: String
) {
    FAST("سريع (Flash Lite)", "Fast (Flash Lite)", "gemini-3.1-flash-lite"),
    MAPS_GROUNDED("بحث المتاجر والخرائط (Maps)", "Boutique Maps (Flash 3.8)", "gemini-3.8-flash"),
    HIGH_THINKING("تفكير عميق (Argon High)", "Deep Thinking (Argon 4)", "gemini-4-argon")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val modelBadge: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiServiceClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun parseJson(): Json = json
}

class GeminiRepository {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    fun hasValidApiKey(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    /**
     * 1. Image Understanding (`gemini-4-argon`):
     * Analyzes an uploaded clothing photo to auto-populate Name, Category, Weather, and Style.
     */
    suspend fun analyzeClothingPhoto(bitmap: Bitmap): Result<AutoDetectedClothing> =
        withContext(Dispatchers.IO) {
            try {
                val base64Jpeg = bitmapToBase64(bitmap, maxDim = 640, quality = 80)
                val schema = buildJsonObject {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("nameAr") {
                            put("type", "STRING")
                            put("description", "Arabic name of the clothing item, e.g. قميص كتان أبيض")
                        }
                        putJsonObject("nameEn") {
                            put("type", "STRING")
                            put("description", "English name of the clothing item, e.g. White Linen Shirt")
                        }
                        putJsonObject("category") {
                            put("type", "STRING")
                            putJsonArray("enum") {
                                add(kotlinx.serialization.json.JsonPrimitive("Top"))
                                add(kotlinx.serialization.json.JsonPrimitive("Outerwear"))
                                add(kotlinx.serialization.json.JsonPrimitive("Bottom"))
                                add(kotlinx.serialization.json.JsonPrimitive("Shoes"))
                            }
                        }
                        putJsonObject("weather") {
                            put("type", "STRING")
                            putJsonArray("enum") {
                                add(kotlinx.serialization.json.JsonPrimitive("Cold"))
                                add(kotlinx.serialization.json.JsonPrimitive("Mild"))
                                add(kotlinx.serialization.json.JsonPrimitive("Hot"))
                                add(kotlinx.serialization.json.JsonPrimitive("All-season"))
                            }
                        }
                        putJsonObject("style") {
                            put("type", "STRING")
                            putJsonArray("enum") {
                                add(kotlinx.serialization.json.JsonPrimitive("Casual"))
                                add(kotlinx.serialization.json.JsonPrimitive("Formal/Smart"))
                                add(kotlinx.serialization.json.JsonPrimitive("Sport"))
                            }
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("nameAr"))
                        add(kotlinx.serialization.json.JsonPrimitive("nameEn"))
                        add(kotlinx.serialization.json.JsonPrimitive("category"))
                        add(kotlinx.serialization.json.JsonPrimitive("weather"))
                        add(kotlinx.serialization.json.JsonPrimitive("style"))
                    }
                }

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = "Analyze this clothing item image for a Smart Digital Wardrobe. Return its concise Arabic name, English name, category (Top, Outerwear, Bottom, Shoes), weather suitability (Cold, Mild, Hot, All-season), and style (Casual, Formal/Smart, Sport)."),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Jpeg))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = schema
                    )
                )

                val response = GeminiServiceClient.service.generateContent(
                    model = "gemini-4-argon",
                    apiKey = apiKey,
                    request = request
                )
                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext Result.failure(IllegalStateException("Empty vision response"))
                val parsed = GeminiServiceClient.parseJson().decodeFromString<AutoDetectedClothing>(rawJson)
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 2. Low-Latency Stylist Tip (`gemini-3.1-flash-lite`):
     * Provides a quick 1-sentence stylist tip for a generated outfit card.
     */
    suspend fun generateQuickStylistTip(outfitSummary: String, isArabic: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = if (isArabic) {
                    "أنت منسق أزياء شخصي. قدم نصيحة أناقة سريعة ومختصرة في جملة واحدة لهذه الإطلالة: $outfitSummary"
                } else {
                    "You are a personal fashion stylist. Give a crisp, encouraging 1-sentence styling tip for this outfit: $outfitSummary"
                }
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiServiceClient.service.generateContent(
                    model = "gemini-3.1-flash-lite",
                    apiKey = apiKey,
                    request = request
                )
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: if (isArabic) "إطلالة متوازنة وأنيقة تناسب يومك." else "A balanced, versatile look for your day."
                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 3. Text-to-Speech (`gemini-3.8-flash-tts`):
     * Converts stylist advice or outfit descriptions into spoken audio and plays it.
     */
    suspend fun speakTextWithGeminiTts(text: String, cacheDir: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = text)))
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Kore")
                            )
                        )
                    )
                )
                val response = GeminiServiceClient.service.generateContent(
                    model = "gemini-3.8-flash-tts",
                    apiKey = apiKey,
                    request = request
                )
                val inlineAudio = response.candidates?.firstOrNull()?.content?.parts
                    ?.firstNotNullOfOrNull { it.inlineData }
                    ?: return@withContext Result.failure(IllegalStateException("No audio returned from gemini-3.8-flash-tts"))

                playInlineAudioData(inlineAudio.data, inlineAudio.mimeType, cacheDir)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 4. Create & Edit Clothing Images (`gemini-nano-banana-2.1`):
     * Edits an existing wardrobe item photo or creates a restyled garment from a text prompt.
     */
    suspend fun editOrCreateGarmentImage(
        prompt: String,
        sourceBitmap: Bitmap? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val parts = mutableListOf<Part>()
            parts.add(Part(text = prompt))
            if (sourceBitmap != null) {
                val b64 = bitmapToBase64(sourceBitmap, maxDim = 512, quality = 85)
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = b64)))
            }
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )
            val response = GeminiServiceClient.service.generateContent(
                model = "gemini-nano-banana-2.1",
                apiKey = apiKey,
                request = request
            )
            val imagePart = response.candidates?.firstOrNull()?.content?.parts
                ?.firstNotNullOfOrNull { it.inlineData }
                ?: return@withContext Result.failure(IllegalStateException("No image returned from gemini-nano-banana-2.1"))
            val bytes = Base64.decode(imagePart.data, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode generated image"))
            Result.success(bmp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 5. Generate High-Quality Lookbook Images (`gemini-3-pro-image-preview` with 1K, 2K, 4K size):
     */
    suspend fun generateHighQualityLookbook(
        prompt: String,
        imageSize: String // "1K", "2K", or "4K"
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val validSize = if (imageSize in listOf("1K", "2K", "4K")) imageSize else "1K"
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = "Editorial fashion lookbook photography, minimalist studio lighting: $prompt")))
                ),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(aspectRatio = "3:4", imageSize = validSize),
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )
            val response = GeminiServiceClient.service.generateContent(
                model = "gemini-3-pro-image-preview",
                apiKey = apiKey,
                request = request
            )
            val imagePart = response.candidates?.firstOrNull()?.content?.parts
                ?.firstNotNullOfOrNull { it.inlineData }
                ?: return@withContext Result.failure(IllegalStateException("No image returned from gemini-3-pro-image-preview"))
            val bytes = Base64.decode(imagePart.data, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode lookbook image"))
            Result.success(bmp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 6. Multi-turn Stylist Chatbot with Maps Grounding & High Thinking:
     * - `gemini-3.1-flash-lite` for fast responses
     * - `gemini-3.8-flash` (with `googleMaps` tool) for Maps-grounded boutique/tailor queries
     * - `gemini-4-argon` (with `ThinkingConfig(thinkingLevel = "HIGH")` and no maxOutputTokens) for complex capsule planning
     */
    suspend fun sendStylistChat(
        history: List<ChatMessage>,
        userMessage: String,
        wardrobeContext: String,
        mode: StylistChatMode,
        isArabic: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemText = if (isArabic) {
                "أنت منسق أزياء شخصي خبير في تطبيق 'خزانة الملابس الذكية'. ساعد المستخدم في تنسيق ملابسه، بناء خزانة كبسولية، أو العثور على متاجر وخياطين قريبين. محتويات خزانة المستخدم الحالية: $wardrobeContext. أجب باللغة العربية بأسلوب أنيق ومرتب."
            } else {
                "You are an expert personal fashion stylist inside 'Smart Digital Wardrobe'. Help the user coordinate outfits, plan capsule wardrobes, and discover local boutiques or tailors. Current user wardrobe items: $wardrobeContext. Respond clearly and stylishly."
            }

            val contents = history.takeLast(8).map { msg ->
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            } + Content(
                role = "user",
                parts = listOf(Part(text = userMessage))
            )

            val tools = if (mode == StylistChatMode.MAPS_GROUNDED) {
                listOf(
                    buildJsonObject {
                        putJsonObject("googleMaps") {}
                    }
                )
            } else {
                null
            }

            val genConfig = when (mode) {
                StylistChatMode.HIGH_THINKING -> GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
                )
                else -> GenerationConfig(temperature = 0.7f)
            }

            val request = GenerateContentRequest(
                contents = contents,
                generationConfig = genConfig,
                tools = tools,
                systemInstruction = Content(parts = listOf(Part(text = systemText)))
            )

            val response = GeminiServiceClient.service.generateContent(
                model = mode.modelId,
                apiKey = apiKey,
                request = request
            )
            val reply = response.candidates?.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?.takeIf { it.isNotBlank() }
                ?: if (isArabic) "إليك اقتراحي لتنسيق إطلالتك بأناقة." else "Here is my styling recommendation."
            Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 7. Analyze Video Content (`gemini-4-argon`):
     * Analyzes an uploaded outfit video clip for fit, drape, and styling feedback.
     */
    suspend fun analyzeOutfitVideo(
        videoBytes: ByteArray,
        mimeType: String,
        isArabic: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val b64Video = Base64.encodeToString(videoBytes, Base64.NO_WRAP)
            val prompt = if (isArabic) {
                "حلل هذا الفيديو للإطلالة والملابس. قيم تناسق الألوان، القصة، والمناسبة، وقدم ٣ نصائح لتحسين المظهر."
            } else {
                "Analyze this outfit video. Evaluate color harmony, garment fit, and occasion suitability, and provide 3 actionable styling tips."
            }
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = mimeType, data = b64Video))
                        )
                    )
                )
            )
            val response = GeminiServiceClient.service.generateContent(
                model = "gemini-4-argon",
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?: if (isArabic) "تم تحليل الفيديو بنجاح." else "Video analysis complete."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 8. Transcribe Audio (`gemini-3.5-transcribe`):
     * Transcribes user voice notes recorded via the microphone into text for the stylist.
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String,
        isArabic: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val b64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val prompt = if (isArabic) {
                "قم بتحويل هذا التسجيل الصوتي إلى نص دقيق:"
            } else {
                "Transcribe this audio recording accurately into text:"
            }
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = mimeType, data = b64Audio))
                        )
                    )
                )
            )
            val response = GeminiServiceClient.service.generateContent(
                model = "gemini-3.5-transcribe",
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: ""
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 9. Generate Runway / Boutique Ambient Music (`lyria-3-clip-preview` / `lyria-3-pro-preview`):
     */
    suspend fun generateRunwayMusic(
        prompt: String,
        isFullTrack: Boolean,
        cacheDir: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = if (isFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview"
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO")
                )
            )
            val response = GeminiServiceClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            val audioPart = response.candidates?.firstOrNull()?.content?.parts
                ?.firstNotNullOfOrNull { it.inlineData }
                ?: return@withContext Result.failure(IllegalStateException("No audio track returned from $model"))

            playInlineAudioData(audioPart.data, audioPart.mimeType, cacheDir)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun playInlineAudioData(base64Data: String, mimeType: String, cacheDir: File) {
        val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
        if (mimeType.contains("pcm", ignoreCase = true) || mimeType.contains("L16", ignoreCase = true)) {
            val sampleRate = 24000
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(audioBytes.size)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            audioTrack.write(audioBytes, 0, audioBytes.size)
            audioTrack.play()
        } else {
            val ext = if (mimeType.contains("mp3")) "mp3" else "wav"
            val tempFile = File(cacheDir, "gemini_audio_${System.currentTimeMillis()}.$ext")
            tempFile.writeBytes(audioBytes)
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
        }
    }

    companion object {
        fun bitmapToBase64(bitmap: Bitmap, maxDim: Int = 500, quality: Int = 78): String {
            val ratio = minOf(
                maxDim.toFloat() / bitmap.width.coerceAtLeast(1),
                maxDim.toFloat() / bitmap.height.coerceAtLeast(1),
                1.0f
            )
            val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (ratio < 1.0f) {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }

        fun bitmapToDataUri(bitmap: Bitmap): String {
            return "data:image/jpeg;base64,${bitmapToBase64(bitmap, maxDim = 420, quality = 72)}"
        }
    }
}
