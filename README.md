# Vani Setu (वाणी सेतु)

An offline Android application for Hindi ↔ Santali voice translation and bilingual classroom worksheet generation.

[Demo](#demo) · [Documentation](hindi-santali-voice-translator-plan.md) · [Download](#setup--installation) · [Report/Paper](#architecture)

---

## Overview

**Vani Setu** is a fully offline Android application designed to bridge the communication gap between Hindi and Santali (`sat_Olck`) speakers in primary education classrooms. Santali is an underserved Indic language spoken across Jharkhand, Odisha, West Bengal, and Bihar, written in the Ol Chiki script. Due to limited connectivity in rural tribal schools, cloud-dependent speech tools fail to provide reliable real-time utility.

The application addresses this issue by embedding lightweight, int8-quantized speech-to-text (ASR) models, machine translation (NMT) pipelines, and an offline classroom phrase bank directly on Android devices. It operates completely offline without internet connectivity during runtime and is optimized to run smoothly on low-cost Android hardware with $\le$ 2 GB of RAM.

In addition to real-time voice translation, Vani Setu features an integrated **Bilingual Worksheet Generator**. Primary school educators can configure grade levels (Classes 1–5) and subjects (Hindi, Santali, Mathematics, Sanskrit) to instantly generate and preview NIPUN Bharat aligned bilingual worksheets directly rendered within the application.

Technically, Vani Setu leverages **ONNX Runtime Android** for local CTC model inference, **SentencePiece** tokenization, and **Android PdfRenderer** for crisp vector document rendering. Model memory allocation is managed through a sequential load-and-release pipeline to adhere strictly to low-memory Android hardware limits.

---

## Features

- **Hindi ↔ Santali Offline Voice Translation** — Bi-directional speech-to-text and machine translation operating completely on-device without internet access.
- **Ol Chiki Script Support** — Full native rendering and editable text input fields for Santali written in the Ol Chiki script (`ᱥᱟᱱᱛᱟᱲᱤ`).
- **Classroom Phrase Bank & Audio Retrieval** — Instant matching for curated primary education phrases with high-quality pre-recorded human Santali audio playback.
- **Bilingual Worksheet Generator Module** — Automated generation and native PDF previewing of bilingual educational worksheets for Classes 1 to 5 across Hindi, Santali, Mathematics, and Sanskrit.
- **Low-Resource Hardware Optimization** — Memory-managed lifecycle pipeline engineered specifically to operate under $\le$ 2 GB device RAM limits.
- **First-Run Model Manager** — Built-in WorkManager downloading workflow for int8 ONNX models with progress tracking and offline fallback capabilities.

---

## Demo

### Interface Overview

- **Tab 1: Voice Translation** — Interactive speech input, real-time waveform animations, editable source/translation fields, and audio output playback.
- **Tab 2: Worksheet Generator** — Grade and subject selector with instant embedded PDF worksheet rendering.

```text
┌─────────────────────────────────────────────────────────┐
│                      Vani Setu                          │
│        Offline Voice Translator • Hindi ↔ Santali       │
├────────────────────────────┬────────────────────────────┤
│   [ Voice Translation ]    │     [ Worksheets ]         │
└────────────────────────────┴────────────────────────────┘
```

---

## Architecture

Vani Setu coordinates audio processing, neural model execution, and document rendering locally on the Android device.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        Vani Setu Android Client                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
        [ Voice Translation ]               [ Worksheet Generator ]
                  │                                   │
      ┌───────────┴───────────┐                       │
      ▼                       ▼                       ▼
 Audio Capture         Phrase Bank            Class & Subject Config
(AudioRecord 16kHz)   (JSON + OGG Audio)              │
      │                       │                       ▼
      ▼                       ▼              NIPUN Curriculum Engine
Acoustic Pipeline      Fuzzy Matcher                  │
(MFCC / Energy)               │                       ▼
      │                       ├────────────────► Native PDF Renderer
      ▼                       │                 (android.graphics.pdf)
 ONNX Runtime                 │                       │
 (IndicConformer int8)        ▼                       ▼
      │               Audio Output Player      Bilingual Canvas View
      ▼               (Media3 ExoPlayer)        (2x Scaled Bitmap)
  Source Text
      │
      ▼
 ONNX NMT Model
 (IndicTrans2 200M int8)
      │
      ▼
  Santali Text (Ol Chiki)
```

### Component Breakdown

1. **ASR Layer (`com.microsoft.onnxruntime`)**: Loads int8-quantized IndicConformer CTC models (`hi_asr_int8.onnx` and `sat_asr_int8.onnx`) to convert recorded 16kHz PCM audio into text tokens.
2. **NMT Layer (`AI4Bharat/IndicTrans2`)**: Executes encoder-decoder neural machine translation between Hindi Devanagari and Santali Ol Chiki scripts using SentencePiece tokenization.
3. **Phrase Bank Engine**: Performs normalized string matching against `assets/phrase_bank/phrase_bank.json` to trigger exact human-recorded Santali `.ogg` audio clips.
4. **Worksheet Module (`android.graphics.pdf.PdfRenderer`)**: Loads and renders vector PDF worksheets directly to high-resolution Compose bitmaps without external PDF viewer dependencies.

---

## Setup & Installation

### Requirements

- Android 7.0 (API Level 24) or higher
- Android Studio Ladybug or newer
- JDK 11+
- Gradle 8.x+

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/vani-setu.git
   cd vani-setu
   ```

2. Open the project in Android Studio.

3. Sync Gradle and build the debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```

4. Install on an attached Android device or emulator:
   ```bash
   ./gradlew :app:installDebug
   ```

---

## License

This project is licensed under the Apache 2.0 License. Model weights are provided under their respective AI4Bharat / Hugging Face open licenses (MIT / CC-BY-4.0).
