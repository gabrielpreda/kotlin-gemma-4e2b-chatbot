# Gemma Android Chatbot

A Kotlin-based Android chatbot app powered by the **Gemma 4:E2B** model running entirely on-device.

No backend, no API — all inference happens locally.

## Features

- Chat UI built with Jetpack Compose
- On-device inference using LiteRT-LM
- Fully offline
- Markdown rendering for responses

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/gabrielpreda/kotlin-gemma-4e2b-chatbot.git
```

Open the project in Android Studio.

### 2. Download the model

Download:

`gemma-4-E2B-it.litertlm`

### 3. Push the model to device or emulator

```bash
adb shell mkdir -p /data/local/tmp/llm/
adb push gemma-4-E2B-it.litertlm /data/local/tmp/llm/gemma-4-E2B-it.litertlm
```

### 4. Run the app
- Start an emulator or connect an Android device
- Click Run in Android Studio

## Tech Stack
- Kotlin
- Jetpack Compose
- LiteRT-LM
- Markwon

## Notes
- Uses the CPU backend for compatibility
- The model file is not included in the repository
- Emulator works for development, though performance may vary

## License

MIT