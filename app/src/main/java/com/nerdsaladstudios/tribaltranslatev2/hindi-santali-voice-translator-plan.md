# Hindi ↔ Santali Offline Voice Translator — Android Build Plan

**Target:** Android app, Kotlin + Jetpack Compose, fully offline, runs on ≤2GB RAM devices.
**Domain for MVP:** Classroom / education phrases (focused vocabulary, not open-domain).
**Timeline:** 10-day sprint (1–2 weeks), scoped to a working, demo-able prototype.

This document is written to be handed to a coding agent (e.g. Claude Code) and followed step by step. Each milestone has explicit deliverables, exact dependencies, and file paths. Read the whole document before starting — later steps assume earlier ones exist.

---

## 0. Feasibility notes (read first)

Santali is an extremely low-resource language. Here's what's real vs. what needs to be engineered around:

| Component | Status | Source |
|---|---|---|
| Hindi & Santali speech-to-text (ASR) | **Real, usable.** AI4Bharat's IndicConformer covers both `hin_Deva` and `sat_Olck` (Santali, Ol Chiki script), with per-language ONNX exports (~188MB fp32, smaller when int8-quantized). | `ai4bharat/indic-conformer-600m-multilingual` (Hugging Face); community ONNX exports for Android exist, e.g. `remiai3/STT_MODELS_FOR_APP` |
| Hindi ↔ Santali text translation (NMT) | **Real, but low-resource quality.** IndicTrans2 officially supports `hin_Deva` ↔ `sat_Olck` as one of its 22 scheduled languages, including a **distilled (~200M param)** variant meant for lighter inference. Translation quality for Santali will be rougher than for high-resource pairs — treat this as a research-grade prototype, not production MT. | `AI4Bharat/IndicTrans2` (GitHub/HF) |
| Santali text-to-speech (TTS) | **Not reliably available as a lightweight on-device model.** `ai4bharat/indic-parler-tts` claims Santali support but is a heavy description-conditioned autoregressive model, unsuitable for ≤2GB RAM. No verified lightweight (VITS/Piper-style) ONNX Santali TTS exists as of this writing. | — |
| Hindi text-to-speech (TTS) | **Solved trivially.** Android ships a system `TextToSpeech` engine with Hindi voices on virtually every India-market device — offline once the voice pack is installed, zero extra RAM/APK cost. | Android SDK (`android.speech.tts.TextToSpeech`) |

**Design decision:** because a real Santali neural TTS isn't a safe bet on a 10-day timeline, the plan uses a **phrase-bank (retrieval) approach** for Santali voice output instead of trying to synthesize arbitrary Santali speech. Since you've scoped the MVP to classroom vocabulary, this is a strength, not a compromise: a curated set of ~150–300 classroom phrases, each with a real human-recorded Santali audio clip, will sound *better* than any neural TTS you could train in a week — and it's small (a few MB total). Open-vocabulary Santali speech synthesis is flagged as future work (Phase 2), not part of this MVP.

This also matches a hybrid-fallback design: exact/fuzzy phrase match first, NMT-only (text, no audio) as the fallback when nothing in the phrase bank matches closely enough. **No faked audio is ever played** — if there's no real clip for a phrase, the app shows translated text and says so, rather than pretending to speak it.

---

## 1. Architecture overview

```
┌─────────────────────────── Hindi → Santali ───────────────────────────┐
│ Mic (Hindi speech)                                                    │
│   → ASR (IndicConformer ONNX, hin_Deva)         → Hindi text          │
│   → NMT (IndicTrans2 ONNX, hin_Deva→sat_Olck)   → Santali text        │
│   → Phrase-bank fuzzy match on Santali text                           │
│       ├─ match found  → play pre-recorded Santali audio clip          │
│       └─ no match     → show Santali text only, "no audio available"  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────── Santali → Hindi ───────────────────────────┐
│ Mic (Santali speech)                                                  │
│   → ASR (IndicConformer ONNX, sat_Olck)          → Santali text       │
│   → NMT (IndicTrans2 ONNX, sat_Olck→hin_Deva)    → Hindi text         │
│   → Android system TextToSpeech (hi-IN)          → spoken Hindi       │
└─────────────────────────────────────────────────────────────────────┘
```

Both directions share the same ASR and NMT engines (loaded on demand, released after use — see §5 RAM budget).

---

## 2. Confirmed model resources

Verify each of these at build time before committing to them — model repos and exact filenames on Hugging Face change.

1. **ASR** — `ai4bharat/indic-conformer-600m-multilingual` (MIT license). Conformer-based hybrid CTC+RNNT ASR, 22 Indic languages including Santali. Export a **per-language, int8-quantized ONNX** model for `hi` and `sat` (don't ship the full 600M multilingual checkpoint — export just the two languages you need, quantized, to keep each under ~100MB). Run via **sherpa-onnx** (`k2-fsa/sherpa-onnx`, Apache-2.0), which has a prebuilt Android AAR and existing examples for Conformer CTC models — this avoids writing your own JNI/ONNX glue for ASR.
2. **NMT** — `AI4Bharat/IndicTrans2`, **distilled 200M variant**, direction pairs `hin_Deva→sat_Olck` and `sat_Olck→hin_Deva`. Export to ONNX (encoder + decoder-with-past) using Hugging Face **Optimum** (`optimum-cli export onnx`), then int8-quantize with `onnxruntime.quantization`. Run on-device via **onnxruntime-android** (official Microsoft AAR — simpler to integrate than building CTranslate2 from source with the NDK, which some reference projects do but which costs days you don't have on this timeline). Tokenization uses **SentencePiece** — bundle the IndicTrans2 SentencePiece model files and use the `sentencepiece-jni` Android bindings (or a pure-Kotlin BPE fallback if the JNI build gives you trouble).
3. **Santali phrase-bank audio** — source natural Santali speech for your classroom phrase list from **IndicVoices-R** (AI4Bharat, includes Santali, documented as open speech-text pairs) or **AI4Bharat's Rasa Santali speech dataset** (CC-BY-4.0). If neither has your exact classroom sentences, record them yourself with a Santali speaker, or compose them from shorter recorded units. Do **not** invent or auto-generate "Santali-sounding" audio — every clip must be real.
4. **Hindi TTS** — Android's built-in `TextToSpeech` with `Locale("hi", "IN")`. No download, no model to manage.

---

## 3. Building the classroom phrase bank (do this in parallel with app scaffolding)

This is the other critical path item besides the native model integration, and it's non-technical work an agent can't fully automate — plan for it explicitly.

1. Draft **150–300 short classroom phrases in Hindi** covering: greetings, instructions ("अपनी किताबें खोलो" / "Open your books"), classroom management, basic Q&A, common vocabulary words. Keep sentences short (≤10 words) — easier to match and to translate reliably.
2. Get Santali translations for each phrase. Prefer, in order: (a) an existing Santali-medium school textbook/glossary if you can find one (Jharkhand SCERT publishes Santali-medium materials — worth checking), (b) a native/fluent Santali speaker reviewing IndicTrans2 output, (c) raw IndicTrans2 output only as a last resort, clearly flagged as unverified in your data.
3. Get a real audio clip per Santali phrase (see §2.3). Store as small compressed files (Opus/OGG, ~16kHz mono) — a few hundred phrases should total only a few MB.
4. Store as a seed dataset: `assets/phrase_bank/phrase_bank.json` (schema below) + `assets/phrase_bank/audio/*.ogg`, bundled into the Room DB on first launch.

```json
[
  {
    "id": "pb_0001",
    "hindi_text": "अपनी किताबें खोलो",
    "santali_text": "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ",
    "audio_file": "audio/pb_0001.ogg",
    "domain": "classroom"
  }
]
```

---

## 4. Android project structure

```
app/
  src/main/java/com/<pkg>/
    MainActivity.kt
    di/                      # simple manual DI (no Hilt unless you already use it — keep deps minimal)
    ui/
      home/HomeScreen.kt          # main translate screen, direction toggle
      home/HomeViewModel.kt
      setup/ModelSetupScreen.kt   # first-run model download UI
      setup/ModelSetupViewModel.kt
      history/HistoryScreen.kt    # optional, Room-backed
      theme/ (Color.kt, Type.kt, Theme.kt)
    domain/
      TranslationPipeline.kt      # orchestrates ASR -> NMT -> phrase-match/TTS
      PhraseMatcher.kt            # fuzzy match: normalized edit distance or token overlap
    data/
      asr/AsrEngine.kt            # wraps sherpa-onnx session
      nmt/NmtEngine.kt            # wraps onnxruntime session + SentencePiece
      tts/HindiTtsEngine.kt       # wraps android.speech.tts.TextToSpeech
      phrasebank/PhraseBankDao.kt
      phrasebank/PhraseBankEntity.kt
      phrasebank/PhraseBankDatabase.kt
      download/ModelDownloadWorker.kt   # WorkManager, downloads ONNX models on first run
      audio/AudioRecorder.kt
      audio/AudioPlayer.kt
  src/main/assets/phrase_bank/
    phrase_bank.json
    audio/*.ogg
```

Keep it flat and explicit — no unused architecture layers (no repository interfaces with a single implementation, no unnecessary abstraction) given the timeline.

---

## 5. RAM budget (the ≤2GB constraint)

A 2GB device has maybe 300–500MB realistically available to a foreground app once the OS and other processes are accounted for. Design rules:

- **Never hold both ASR and NMT models in memory at once if avoidable** — load the ASR model, run inference, release it (`session.close()`), then load NMT, run, release. A few hundred ms of load latency per step is an acceptable trade for staying under budget on a 2GB device.
- **Quantize everything to int8.** Both IndicConformer and IndicTrans2 exports must be int8-quantized ONNX, not fp32/fp16.
- **Don't bundle models in the APK.** Ship the app small; download models via `WorkManager` on first run into `context.filesDir`, with a progress screen and a "requires Wi-Fi" default. This also keeps Play Store APK size sane.
- **Phrase-bank audio is cheap** — a few MB total, keep it in `assets/` and copy to disk or stream directly, no special handling needed.
- Profile actual peak RSS with Android Studio's Profiler on an emulator pinned to 2GB RAM (AVD Manager → edit device → RAM: 2048MB) before calling any milestone "done."

---

## 6. Gradle dependencies (explicit — do not add anything beyond this list without a reason)

`gradle/libs.versions.toml` additions:

```toml
[versions]
onnxruntimeAndroid = "1.19.2"   # verify latest stable at build time
sherpaOnnx = "1.10.30"          # verify latest stable at build time
workManager = "2.9.1"
room = "2.6.1"
media3 = "1.4.1"

[libraries]
onnxruntime-android = { group = "com.microsoft.onnxruntime", name = "onnxruntime-android", version.ref = "onnxruntimeAndroid" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
# sherpa-onnx: no standard Maven coordinate as of writing — vendor the AAR from
# https://github.com/k2-fsa/sherpa-onnx releases (Android artifacts) into app/libs/
# and declare: implementation(files("libs/sherpa-onnx.aar"))
```

`app/build.gradle.kts` — relevant bits:

```kotlin
android {
    defaultConfig {
        ndk { abiFilters += "arm64-v8a" }   // single ABI: smaller APK, arm64 covers virtually all target devices
    }
}

dependencies {
    implementation(libs.onnxruntime.android)
    implementation(files("libs/sherpa-onnx.aar"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.media3.exoplayer)
}
```

`AndroidManifest.xml` permissions:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />           <!-- model download only, not runtime translation -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 7. Day-by-day milestones

Each milestone lists what an agent should produce and how to verify it. Treat Days 1–8 as must-have for a working demo; Days 9–10 are polish and are the first to cut if you're behind.

**Day 1 — Project scaffold**
- New Android Studio project, Kotlin + Compose, min SDK 24 (covers ≤2GB devices, which are almost always Android 9–12).
- `HomeScreen.kt`: direction toggle (Hindi→Santali / Santali→Hindi), a mic button, a text area for both source and translated text, a placeholder "no audio available" state.
- Wire `RECORD_AUDIO` runtime permission request.
- Deliverable: app builds and runs, mic button toggles a recording/idle UI state (no real ASR yet).

**Day 2 — Model export (done on a dev machine, not in the Android project)**
- Export int8 ONNX for IndicConformer (`hi`, `sat`) and IndicTrans2 distilled (`hin_Deva→sat_Olck`, `sat_Olck→hin_Deva`).
- Host the resulting `.onnx`/`.onnx.data` + SentencePiece files somewhere fetchable (GitHub Releases or a Hugging Face repo you control) — this is the URL `ModelDownloadWorker` will pull from.
- In parallel: start the phrase-bank data collection from §3 (this doesn't block app work).
- Deliverable: model files hosted at a stable URL; `phrase_bank.json` at least half populated.

**Day 3 — Model download + setup flow**
- `ModelDownloadWorker` (WorkManager): downloads the model files with progress reporting.
- `ModelSetupScreen.kt`: shown on first run, shows download progress, blocks entry to `HomeScreen` until complete.
- Deliverable: fresh install downloads all models and lands on `HomeScreen`.

**Day 4 — ASR integration**
- `AsrEngine.kt`: loads the sherpa-onnx Conformer session for a given language, exposes `suspend fun transcribe(pcmAudio: ShortArray): String`.
- Debug-only screen or log output: record → transcribe → show raw Hindi text; same for Santali.
- Deliverable: speaking a Hindi sentence produces correct-ish Hindi text on screen; same for a Santali sentence.

**Day 5 — NMT integration**
- `NmtEngine.kt`: loads IndicTrans2 ONNX encoder+decoder, SentencePiece tokenizer, exposes `suspend fun translate(text: String, srcLang: String, tgtLang: String): String`.
- Test with typed text input (bypass ASR) first — isolate translation correctness from ASR noise.
- Deliverable: typed Hindi classroom sentences produce Santali (Ol Chiki) text, and vice versa.

**Day 6 — Phrase bank + audio playback**
- Room schema + DAO for `PhraseBankEntity` (from §3 JSON, seeded on first launch).
- `PhraseMatcher.kt`: normalized string match — start with token-overlap or edit-distance against `santali_text`; this is small-vocabulary so a simple approach is fine, don't overbuild an embedding index for ~300 phrases.
- `AudioPlayer.kt` (Media3 ExoPlayer): plays the matched `.ogg` clip.
- Deliverable: given Santali text, the app finds the closest phrase-bank entry (or reports no match) and plays the right clip.

**Day 7 — Hindi TTS**
- `HindiTtsEngine.kt`: wraps `TextToSpeech`, checks `hi-IN` availability, prompts the system voice-data installer if missing.
- Deliverable: given Hindi text, the app speaks it aloud.

**Day 8 — Full pipeline wiring**
- `TranslationPipeline.kt`: chains mic → ASR → NMT → (phrase-match+playback | TTS) per direction, sequenced with explicit model load/release per §5.
- Deliverable: both directions work end-to-end, mic-to-audio-out, no manual steps.

**Day 9 — UI/UX polish**
- Material 3 theming, mic waveform/level indicator, loading and error states, clear "no audio available for this phrase" messaging (never silently fail).
- RAM profiling pass on a 2GB-pinned emulator; fix any peak-usage violations from §5.

**Day 10 — Testing, demo prep**
- Test on the lowest-spec real or emulated device you have access to.
- Write a short README covering setup, known limitations (translation quality, phrase-bank coverage), and the architecture diagram from §1.
- Prepare a demo script: e.g., teacher says "अपनी किताबें खोलो" → recognized → translated → Santali audio plays; then a student responds in Santali → recognized → translated → spoken in Hindi.

---

## 8. Known limitations to state honestly in your demo / report

- Open-vocabulary Santali speech output isn't solved — only phrase-bank coverage produces audio; anything outside it shows text only.
- Hindi↔Santali NMT quality reflects a genuinely low-resource language pair; expect rough edges on anything but short, simple sentences.
- ASR accuracy for Santali will depend heavily on mic quality and speaker accent — IndicConformer's Santali training data is comparatively small next to Hindi.
- This is intentionally scoped to a classroom-phrase demo, not a general-purpose translator — say so up front rather than over-claiming.
