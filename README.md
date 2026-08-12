# MYRAA — Android source

This is a starter Android project for MYRAA: a voice-first AI companion screen with mic
listening state, an error card ("CORE ERROR PROTOCOL"), and a screen-share (MediaProjection)
flow — matching the layout and copy from your app's existing screenshot.

## What's included
- `MainActivity.kt` — mic permission handling, Android `SpeechRecognizer` wiring, listening/idle
  state text, and the screen-capture request flow.
- `ScreenCaptureService.kt` — foreground service that holds the `MediaProjection` session
  (required by Android 10+ or capture is killed immediately). Frame-grabbing (VirtualDisplay +
  ImageReader) is stubbed with a TODO — plug in your existing capture/encode pipeline there.
- `activity_main.xml` — the character stage, listening prompt, mic button, close button, and the
  error card overlay, styled to match your screenshot's dark blue / cyan palette.
- Full color/theme/string resources.

## What's NOT included (needs your input)
- **Character art**: `imgCharacter` is an empty `ImageView`. Drop your character asset into
  `res/drawable/` and set `android:src`.
- **AI/voice backend**: `onResults()` in `MainActivity.kt` has a `TODO` where recognized speech
  should be sent to your LLM/response pipeline — that's specific to how MYRAA's brain works on
  desktop, which I don't have visibility into.
- **Actual frame capture**: `ScreenCaptureService.onProjectionReady()` is stubbed. This is where
  you'd build a `VirtualDisplay` off the `MediaProjection` and start pulling frames.

## Building the APK
I can't compile this into a binary from this environment (no Android SDK / no network access
here). To get an actual `.apk` on your phone:

1. Install **Android Studio** (free, from developer.android.com).
2. `File → Open` and select this `MYRAA` folder.
3. Let Gradle sync (it'll download the SDK/build tools automatically).
4. **For quick testing**: `Build → Build Bundle(s) / APK(s) → Build APK(s)`. This produces a
   debug APK you can install directly by dragging it onto your phone or via
   `adb install app/debug/app-debug.apk`.
5. **For a real release build**: `Build → Generate Signed Bundle / APK`, create a keystore (first
   time only), and build a signed release APK.

Total time from opening the project to an installable APK is usually 5–10 minutes on a normal
laptop with a decent internet connection.

## Why the original "Could not capture screen: Not supported" error happens
On Android, this maps to `MediaProjectionManager` being unavailable or the user/OS declining the
capture intent — most commonly on emulators without proper GPU passthrough, very old Android
versions (below API 21), or restricted work-profile devices. `MainActivity.requestScreenCapture()`
now surfaces that failure through the same error card UI instead of crashing silently.
