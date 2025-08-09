# Voice Recorder - Starter Android Project

## What's included
- Kotlin-based Android app skeleton
- TarsosDSP dependency for audio processing (effects/pitch)
- Visualizer view, record/play/share flow
- RNNoise/WebRTC integration left as optional native step (instructions below)

## RNNoise (optional)
To include RNNoise native libs:
1. Add C sources to `app/src/main/cpp/` and provide `CMakeLists.txt`.
2. Build .so for `armeabi-v7a` and `arm64-v8a` and place in `app/src/main/jniLibs/<ABI>/`.
3. Create JNI wrappers (Kotlin `external` functions) to pass PCM buffers to RNNoise and receive denoised buffers.

## How to open
- Open Android Studio -> Open an existing project -> select the folder `voice_recorder_project`.
- Sync Gradle. Add NDK if you plan to include native RNNoise.

## Notes
- This is a starter project. Some functions (playWithPitch, playRobot) are intentionally minimal and left as exercises.
- Tell me if you want me to also include prebuilt `.so` RNNoise binaries for `armeabi-v7a` and `arm64-v8a` in the zip.


Included prebuilt placeholder RNNoise .so files for armeabi-v7a and arm64-v8a in app/src/main/jniLibs/. Replace with real binaries before release.


## Building APK via GitHub Actions
1. Push this project to a new GitHub repository.
2. Ensure the default branch is `main` (or adjust `.github/workflows/android.yml` accordingly).
3. On push, GitHub Actions will build a debug APK.
4. Download the APK from the Actions tab → your workflow run → `app-debug-apk` artifact.

## Building Locally
Run:
```
chmod +x gradlew
./gradlew assembleDebug
```
APK will be in `app/build/outputs/apk/debug/app-debug.apk`.


## RNNoise Integration
This project includes placeholder `librnnoise.so` files for `armeabi-v7a` and `arm64-v8a`.
To enable actual noise removal:
1. Replace these `.so` files in `app/src/main/jniLibs/<ABI>/` with real RNNoise binaries compiled for each ABI.
2. JNI wrapper code in `RNNoiseWrapper.kt` is ready to call these libraries.
3. Ensure `System.loadLibrary("rnnoise")` is called before use.
