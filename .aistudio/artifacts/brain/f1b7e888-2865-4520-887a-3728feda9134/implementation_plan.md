# Smart Digital Wardrobe (خزانة الملابس الذكية)

A modern, bilingual (Arabic RTL by default with instant English switching) personal wardrobe manager and AI stylist for Android that organizes clothing items in cloud storage, generates three tailored outfit combinations on demand, and provides a full multimodal AI Stylist Studio.

## User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed product and architectural decisions below before we provision Firebase and build the application.

- **Confirmed Bilingual RTL/LTR Experience**: Launches in **Arabic (Right-to-Left layout)** by default with a single-tap **AR / EN** language switcher in the top bar that dynamically flips layout direction (`LayoutDirection.Rtl` / `LayoutDirection.Ltr`) and translates all labels, categories, weather states, and stylist prompts.
- **Confirmed Visual Identity**: **Modern Minimalist** palette using Crisp Alabaster White, Slate Gray, and Sage Green semantic Material 3 tokens paired with locally bundled Arabic/Latin typography (`Cairo` and `Plus Jakarta Sans`).
- **Confirmed Screen Organization**: Four primary bottom navigation destinations (**Outfit Generator**, **My Closet**, **Add Clothes**, and **AI Stylist Studio**), keeping core wardrobe management clean while housing advanced Gemini multimodal capabilities in the Studio tab and smart auto-fill inside Add Clothes.
- **Cloud Persistence & Authentication**: Uses **Firebase Authentication (Google Sign-In via Jetpack Credential Manager)** and **Firebase Firestore** so every user's `Wardrobe` items persist securely across sessions and devices, plus a one-tap "Load Sample Wardrobe" action so new users can test the 3 outfit generator algorithms immediately.

---

## 1. Overview & Core Concept

- **What It Does**: Lets users photograph or pick clothing items from their gallery, categorize them by type, weather suitability, and style (with optional 1-tap Gemini Vision auto-tagging), browse and filter their digital closet, and generate three distinct complete outfits based on current weather conditions.
- **Target Audience / Persona**: Style-conscious Arabic and English speakers looking for an effortless daily outfit planner and intelligent personal stylist.
- **Key Value**: Eliminates morning decision fatigue by combining deterministic rule-based outfit curation (Weather-Aware, Casual/Sport, and Formal/Smart combinations) with generative AI styling, voice narration, visual lookbooks, and local boutique discovery.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Authentication & Onboarding**:
   - Users sign in via Google Sign-In (`CredentialManager`).
   - If the user's closet is empty, a welcoming empty state offers either **Add First Item** or **Load Sample Capsule Wardrobe** (pre-populating tops, outerwear, bottoms, and shoes across Cold/Mild/Hot and Casual/Formal/Sport styles in Arabic & English).
2. **Outfit Generator (Dashboard — Main Screen)**:
   - Displays a clean header with a **Current Weather** selector (`Cold / بارد`, `Mild / معتدل`, `Hot / حار`) and a prominent **Generate Outfits (تنسيق الإطلالات)** button.
   - Generates **3 distinct Outfit Cards**:
     - **Suggestion 1 — Weather-Aware (متوافق مع الطقس)**: Filters items matching the selected weather or `All-season`. Includes `Top + Bottom + Shoes`, and automatically adds `Outerwear` when `Cold` is selected.
     - **Suggestion 2 — Casual Style (إطلالة كاجوال / رياضية)**: Ignores the weather filter and combines random items where Style is `Casual` or `Sport`.
     - **Suggestion 3 — Formal / Smart Style (إطلالة رسمية / أنيقة)**: Ignores the weather filter and combines items where Style is `Formal/Smart`.
   - Each card provides a **Re-roll** action, a **Low-Latency Stylist Note** (`gemini-3.1-flash-lite`), and a **Listen (TTS)** button (`gemini-3.8-flash-tts`) that reads the outfit recommendation aloud.
3. **My Closet (خزانتي)**:
   - Displays a responsive 2-column (or 3-column on tablets) gallery grid of all wardrobe items.
   - Includes horizontal **Category Filter Chips** (`All`, `Top`, `Outerwear`, `Bottom`, `Shoes`), item detail inspection, item deletion, and a **Magic Edit / Restyle** sheet (`gemini-nano-banana-2.1`) to visualize color or fabric alterations on an existing item.
4. **Add Clothes (إضافة ملابس)**:
   - Photo picker (`PickVisualMedia`) with instant preview.
   - **AI Auto-Fill Button** (`gemini-4-argon`): Analyzes the uploaded clothing photo and automatically populates the Item Name, Category, Weather suitability, and Style dropdowns.
   - Form controls for **Item Name**, **Category** (`Top`, `Outerwear`, `Bottom`, `Shoes`), **Weather** (`Cold`, `Mild`, `Hot`, `All-season`), and **Style** (`Casual`, `Formal/Smart`, `Sport`), saving directly to Firestore.
5. **AI Stylist Studio (استوديو المنسق الذكي)**:
   - **Stylist Chat & Voice**: Multi-turn chat interface with model mode selector (**Fast** via `gemini-3.1-flash-lite`, **Balanced + Maps Grounding** via `gemini-3.8-flash` with `googleMaps` tool for nearby tailors/boutiques, and **Deep Thinking** via `gemini-4-argon` with `ThinkingLevel.HIGH` for capsule wardrobe planning), plus microphone audio recording & transcription (`gemini-3.5-transcribe`) and TTS playback (`gemini-3.8-flash-tts`).
   - **Lookbook Image Generator**: Generates high-resolution fashion inspiration concepts (`gemini-3-pro-image-preview`) with an explicit **1K / 2K / 4K** image size selector.
   - **Video Outfit Check & Runway Audio**: Upload a short outfit video for fit/style critique (`gemini-4-argon`) and generate custom 30-second or full-track boutique/runway background music (`lyria-3-clip-preview` / `lyria-3-pro-preview`).

### Visual Identity & Theme
- **Aesthetic Direction**: Modern Minimalist Editorial — airy whitespace, crisp borders, subtle elevation, and tactile pill selectors.
- **Color Palette**:
  - Background / Surface: Alabaster White (`#F8FAF9`) & Pure White Cards (`#FFFFFF`)
  - Primary Accent: Sage Green (`#3D6B58`) with Soft Sage containers (`#DCECE4`)
  - Secondary / Neutral: Deep Slate (`#1E293B`) and Muted Slate (`#64748B`)
  - Dark Mode Support: Deep Pine Obsidian (`#0F1715`) with Luminous Sage (`#86EFAC`)
- **Typography**: Locally bundled `Cairo` font family for seamless Arabic & Latin display/body legibility, paired with `Plus Jakarta Sans`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic 3-Card Outfit Algorithm + Optional AI Enhancement**
  - *Chosen Approach*: The three outfit suggestions strictly follow the exact filtering rules specified in your prompt (Suggestion 1 filters by Weather + All-season and includes Outerwear if Cold; Suggestion 2 filters by Casual/Sport ignoring weather; Suggestion 3 filters by Formal/Smart ignoring weather), while Gemini provides instant styling commentary and TTS voiceover on top.
  - *Why*: Guarantees instantaneous, deterministic, zero-latency outfit combinations that strictly honor your business logic even offline, while enriching the experience with AI insights.
- **Decision 2: Compact Thumbnail Encoding for Firestore Persistence**
  - *Chosen Approach*: Uploaded photos from the Android Photo Picker are downscaled and compressed into lightweight Base64 JPEG thumbnails stored directly within the user's Firestore `Wardrobe` document.
  - *Why*: Allows clothing photos to sync reliably across sessions within Firestore Enterprise without requiring manual Firebase Cloud Storage bucket configuration.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram
```
┌────────────────────────────────────────────────────────────────────────────┐
│                   MainActivity (RTL/LTR CompositionLocal)                  │
│             AuthGate: Google Sign-In via Jetpack CredentialManager         │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Authenticated UID
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                    WardrobeViewModel & StylistViewModel                    │
│        StateFlow<UiState<List<WardrobeItem>>> + Bilingual State            │
├──────────────────┬──────────────────┬──────────────────┬───────────────────┤
│ Outfit Generator │    My Closet     │   Add Clothes    │ AI Stylist Studio │
│  - Weather Pick  │  - Category Chip │  - Photo Picker  │  - Multi-turn Chat│
│  - 3 Outfit Cards│  - Grid Gallery  │  - AI Auto-Tag   │  - 1K/2K/4K Gen   │
│  - TTS Voiceover │  - Nano Banana   │  - Enum Dropdowns│  - Video & Maps   │
│  - Fast AI Note  │    Image Restyle │  - Form Save     │  - Lyria Music    │
└────────┬─────────┴────────┬─────────┴────────┬─────────┴─────────┬─────────┘
         │                  │                  │                   │
         ▼                  ▼                  ▼                   ▼
┌─────────────────────────────────────┐ ┌────────────────────────────────────┐
│         WardrobeRepository          │ │          GeminiRepository          │
│  FirebaseFirestore (Named DB ID)    │ │  Retrofit + OkHttp (60s timeout)   │
│  /users/{uid}/wardrobe/{itemId}     │ │  - gemini-4-argon (Vision/Video/   │
│  Real-time snapshots() Flow         │ │    ThinkingLevel.HIGH)             │
│  Structured handleFirestoreError    │ │  - gemini-3.8-flash (+ googleMaps) │
│                                     │ │  - gemini-3.1-flash-lite           │
│                                     │ │  - gemini-3-pro-image-preview      │
│                                     │ │  - gemini-nano-banana-2.1          │
│                                     │ │  - gemini-3.8-flash-tts            │
│                                     │ │  - gemini-3.5-transcribe           │
│                                     │ │  - lyria-3-clip/pro-preview        │
└─────────────────────────────────────┘ └────────────────────────────────────┘
```

### Data Model & State (`Wardrobe` Collection)
Stored at `/users/{userId}/wardrobe/{itemId}` with zero-trust Firestore security rules enforcing user ownership and schema validation:
- `id`: `String` (`@DocumentId` unique identifier)
- `userId`: `String` (Owner UID matching `request.auth.uid`)
- `imageUrl`: `String` (Compressed Base64 JPEG data URI or preset vector/drawable identifier)
- `name`: `String` (Item Name, 1–100 chars)
- `category`: `String` Enum (`Top`, `Outerwear`, `Bottom`, `Shoes`)
- `weather`: `String` Enum (`Cold`, `Mild`, `Hot`, `All-season`)
- `style`: `String` Enum (`Casual`, `Formal/Smart`, `Sport`)
- `createdAt` / `updatedAt`: `Timestamp?` (`@ServerTimestamp`)

### Interactive Component & State Mapping
- **Weather Selection & Outfit Generation**: Selecting `Cold`, `Mild`, or `Hot` and tapping **Generate Outfits** computes 3 `OutfitSuggestion` objects from the active `wardrobeItems` state. If a category is missing for a specific slot, the card shows which category needs to be added and offers a 1-tap shortcut to `Add Clothes` or `Load Sample Wardrobe`.
- **Category Filtering**: Selecting a category chip in `My Closet` updates `selectedCategoryFilter` and reactively filters the displayed grid via `derivedStateOf`.
- **AI Auto-Fill & Multimodal Actions**: Uploading an image in `Add Clothes` enables the **Auto-Fill with AI** button, which requests structured JSON output (`name`, `category`, `weather`, `style`) from `gemini-4-argon` and updates the form state fields directly.
