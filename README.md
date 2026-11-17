# Vision 20-20-20 👁️

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple.svg)](gradle/libs.versions.toml)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](app/build.gradle.kts)

An open-source, offline, privacy-first native Android eye wellness companion implementing the **20-20-20 Rule**:  
*Every **20 minutes** of screen time, gaze at an object **20 feet** away for at least **20 seconds**.*

---

## Why Vision 20-20-20?

Extended screen time on phones and laptops drastically decreases natural blinking rates from ~18 blinks/min down to ~5 blinks/min, inducing tear film dry spots and chronic ciliary muscle accommodation spasms (**Computer Vision Syndrome / Digital Asthenopia**).

### Key Features
- **Zero Screen Clutter During Work:** While your 20-minute timer counts down, **nothing floats over your apps**. No badges, no floating bubbles, no reading distractions.
- **20-Second Guided Relaxation:** When the work timer completes, a soothing 20-second breathing overlay prompts you to look 20 feet away, blink gently, and relax your ciliary muscles before automatically dismissing itself.
- **Dual Session Profiles:**
  - **📱 Mobile Reading (Screen-Aware):** Ticks *only* while your screen is on. Pauses immediately when screen turns off. Auto-resets if your phone remains locked for ≥ 5 minutes (Natural Break).
  - **💻 Laptop / Desk Companion:** Continuous wall-clock timer that sits on your desk, counting intervals independently of phone screen state.
- **Lock-Screen Alert Delivery:** Wakes up your sleeping display with guided relaxation over keyguard Google Clock-alarm style (`setShowWhenLocked`).
- **Quick Settings Tile:** One-tap toggle directly in your notification shade.
- **Home Screen Widget:** Interactive Jetpack Glance widget showing live remaining time and play/pause controls.
- **Notification-Only Mode:** Optional sound/vibration reminder for users who prefer zero visual blockers.
- **100% Offline & Private:** Zero network permissions (`android.permission.INTERNET` is not included in the manifest). No tracking, no ads.

---

## Downloads & Installation

- **F-Droid:** Available on F-Droid via `com.vision202020.app`.
- **GitHub Releases:** Download pre-built release APKs from [Releases](https://github.com/xRahul/vision202020/releases).

---

## Building from Source

### Prerequisites
- JDK 21+
- Android SDK (API 34+)

```bash
# Clone the repository
git clone git@github.com:xRahul/vision202020.git
cd vision202020

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APKs will be located in:
`app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.

---

## License

Vision 20-20-20 is free and open-source software licensed under the **GNU General Public License v3.0** (`GPL-3.0-or-later`). See [LICENSE](LICENSE) for details.
