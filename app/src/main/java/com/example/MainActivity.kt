package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GeminiRepository
import com.example.data.WardrobeItem
import com.example.data.WardrobeRepository
import com.example.ui.AuthScreen
import com.example.ui.UiState
import com.example.ui.WardrobeViewModel
import com.example.ui.screens.AddClothesScreen
import com.example.ui.screens.AiStylistStudioScreen
import com.example.ui.screens.MyClosetScreen
import com.example.ui.screens.OutfitGeneratorScreen
import com.example.ui.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class WardrobeDestination {
    OUTFIT_GENERATOR,
    MY_CLOSET,
    ADD_CLOTHES,
    AI_STUDIO
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SmartWardrobeAppRoot()
            }
        }
    }
}

@Composable
fun SmartWardrobeAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var unauthIsArabic by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        val direction = if (unauthIsArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            AuthScreen(
                isArabic = unauthIsArabic,
                onToggleLanguage = { unauthIsArabic = !unauthIsArabic },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        AuthenticatedWardrobeShell(
            currentUserId = user.uid,
            onSignedOut = { currentUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedWardrobeShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: WardrobeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WardrobeViewModel(
                    repository = WardrobeRepository(db),
                    geminiRepository = GeminiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val wardrobeState by viewModel.wardrobeState.collectAsStateWithLifecycle()
    val selectedWeather by viewModel.selectedWeather.collectAsStateWithLifecycle()
    val generatedOutfits by viewModel.generatedOutfits.collectAsStateWithLifecycle()
    val outfitTips by viewModel.outfitTips.collectAsStateWithLifecycle()
    val selectedClosetCategory by viewModel.selectedClosetCategory.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedChatMode by viewModel.selectedChatMode.collectAsStateWithLifecycle()
    val selectedImageSize by viewModel.selectedImageSize.collectAsStateWithLifecycle()
    val generatedLookbookBitmap by viewModel.generatedLookbookBitmap.collectAsStateWithLifecycle()
    val videoAnalysisResult by viewModel.videoAnalysisResult.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(WardrobeDestination.OUTFIT_GENERATOR) }

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val wardrobeItems: List<WardrobeItem> = when (val s = wardrobeState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "خزانة الملابس الذكية" else "Smart Digital Wardrobe",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleLanguage() },
                            label = { Text(if (isArabic) "EN" else "عربي") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Toggle Language",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("top_bar_language_toggle")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = if (isArabic) "تسجيل الخروج" else "Sign Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination == WardrobeDestination.OUTFIT_GENERATOR,
                        onClick = { currentDestination = WardrobeDestination.OUTFIT_GENERATOR },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(if (isArabic) "منسق الإطلالات" else "Outfits") },
                        modifier = Modifier.testTag("nav_outfit_generator")
                    )
                    NavigationBarItem(
                        selected = currentDestination == WardrobeDestination.MY_CLOSET,
                        onClick = { currentDestination = WardrobeDestination.MY_CLOSET },
                        icon = { Icon(Icons.Default.Checkroom, contentDescription = null) },
                        label = { Text(if (isArabic) "خزانتي" else "My Closet") },
                        modifier = Modifier.testTag("nav_my_closet")
                    )
                    NavigationBarItem(
                        selected = currentDestination == WardrobeDestination.ADD_CLOTHES,
                        onClick = { currentDestination = WardrobeDestination.ADD_CLOTHES },
                        icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                        label = { Text(if (isArabic) "إضافة ملابس" else "Add Clothes") },
                        modifier = Modifier.testTag("nav_add_clothes")
                    )
                    NavigationBarItem(
                        selected = currentDestination == WardrobeDestination.AI_STUDIO,
                        onClick = { currentDestination = WardrobeDestination.AI_STUDIO },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text(if (isArabic) "استوديو الذكاء" else "AI Studio") },
                        modifier = Modifier.testTag("nav_ai_studio")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (statusBanner != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusBanner!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                when (val state = wardrobeState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    is UiState.Success -> {
                        when (currentDestination) {
                            WardrobeDestination.OUTFIT_GENERATOR -> {
                                OutfitGeneratorScreen(
                                    isArabic = isArabic,
                                    items = wardrobeItems,
                                    selectedWeather = selectedWeather,
                                    suggestions = generatedOutfits,
                                    outfitTips = outfitTips,
                                    isBusy = isBusy,
                                    onSelectWeather = { viewModel.selectWeather(it) },
                                    onGenerateOutfits = { viewModel.generateOutfitsNow() },
                                    onNavigateToAddClothes = {
                                        currentDestination = WardrobeDestination.ADD_CLOTHES
                                    },
                                    onSeedSampleWardrobe = { viewModel.seedSampleWardrobe() },
                                    onRequestFastTip = { viewModel.fetchQuickTipForOutfit(it) },
                                    onSpeakOutfit = { text, dir -> viewModel.speakText(text, dir) }
                                )
                            }
                            WardrobeDestination.MY_CLOSET -> {
                                MyClosetScreen(
                                    isArabic = isArabic,
                                    allItems = wardrobeItems,
                                    selectedCategory = selectedClosetCategory,
                                    isBusy = isBusy,
                                    onSelectCategory = { viewModel.selectClosetCategory(it) },
                                    onDeleteItem = { viewModel.deleteClothingItem(it) },
                                    onRestyleItemWithNanoBanana = { item, prompt ->
                                        viewModel.restyleWardrobeItem(item, prompt, null)
                                    },
                                    onSeedSampleWardrobe = { viewModel.seedSampleWardrobe() },
                                    onNavigateToAddClothes = {
                                        currentDestination = WardrobeDestination.ADD_CLOTHES
                                    },
                                    onNavigateBack = {
                                        currentDestination = WardrobeDestination.OUTFIT_GENERATOR
                                    }
                                )
                            }
                            WardrobeDestination.ADD_CLOTHES -> {
                                AddClothesScreen(
                                    isArabic = isArabic,
                                    isBusy = isBusy,
                                    onAutoAnalyzeBitmap = { bmp, cb ->
                                        viewModel.autoAnalyzePhoto(bmp, cb)
                                    },
                                    onSaveItem = { name, cat, weather, style, img ->
                                        viewModel.addClothingItem(name, cat, weather, style, img) {
                                            currentDestination = WardrobeDestination.MY_CLOSET
                                        }
                                    },
                                    onNavigateBack = {
                                        currentDestination = WardrobeDestination.OUTFIT_GENERATOR
                                    }
                                )
                            }
                            WardrobeDestination.AI_STUDIO -> {
                                AiStylistStudioScreen(
                                    isArabic = isArabic,
                                    isBusy = isBusy,
                                    chatMessages = chatMessages,
                                    selectedChatMode = selectedChatMode,
                                    selectedImageSize = selectedImageSize,
                                    generatedLookbookBitmap = generatedLookbookBitmap,
                                    videoAnalysisResult = videoAnalysisResult,
                                    onSelectChatMode = { viewModel.selectChatMode(it) },
                                    onSendChatMessage = { viewModel.sendStylistMessage(it) },
                                    onSpeakText = { text, dir -> viewModel.speakText(text, dir) },
                                    onTranscribeAudio = { bytes, mime, cb ->
                                        viewModel.transcribeVoiceNote(bytes, mime, cb)
                                    },
                                    onSelectImageSize = { viewModel.selectImageSize(it) },
                                    onGenerateProLookbook = { viewModel.generateHighResLookbook(it) },
                                    onGenerateNanoBanana = { viewModel.generateNanoBananaImage(it) },
                                    onSaveGeneratedLookToCloset = { name, cat, weather, style, img ->
                                        viewModel.addClothingItem(name, cat, weather, style, img)
                                    },
                                    onAnalyzeVideo = { bytes, mime ->
                                        viewModel.analyzeVideoOutfit(bytes, mime)
                                    },
                                    onGenerateRunwayMusic = { prompt, isFull, dir ->
                                        viewModel.generateRunwayAudio(prompt, isFull, dir)
                                    },
                                    onNavigateBack = {
                                        currentDestination = WardrobeDestination.OUTFIT_GENERATOR
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
