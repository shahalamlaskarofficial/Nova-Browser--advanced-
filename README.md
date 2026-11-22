# 🌐 NOVA Browser v4.0 — Liquid Glass Edition

**NOVA Browser** is an ultra-lightweight, privacy-first Android web browser engineered for **speed, aggressive ad blocking, persistent 4x parallel downloads, and ultra-low RAM usage (≤ 15–18 MB)**. Designed with a dynamic **Liquid Glass UI**, built-in **3-Engine Temp Mail**, and **Login Wall Bypass**, NOVA gives you a clean, distraction-free mobile web experience.

---

## ✨ Key Features

### 🎨 Dynamic Liquid Glass UI & Customization
* **Fluid Adaptive Bars:** Floating translucent top and bottom glass bars start comfortably sized (`56dp`) on the Home screen, smoothly animate down (`44dp`) when you enter a website, and glide away while scrolling for maximum screen space.
* **Liquid Glass Box Menu:** Clean, categorized frosted-glass dashboard for instant access to all tools.
* **6 Vibrant Neon-Glass Themes:** Switch anytime between **Electric Sapphire**, **AMOLED Neon (Pure Black)**, **Neon Cyberpunk**, **Solar Sunset**, **Cyber Emerald**, and **Crystal Frost**.
* **Custom Gallery Wallpapers:** Pick any image from your phone gallery to glow behind the frosted Liquid Glass Home screen.

### 🛡️ NOVA Shield v5 (Ad, Tracker, YouTube & Spotify Blocker)
* **175,000+ Domain Blocklist:** Automatically syncs and caches 4 major filter lists (*StevenBlack Hosts*, *HaGeZi Multi-Light*, *AdAway*, and *Peter Lowe's AdServers*).
* **YouTube & Spotify Ad Protection:** Strips `adPlacements`/`playerAds` from YouTube player JSON payloads, auto-skips video ads, blocks Spotify web ad/telemetry endpoints, and enables DRM protected media playback for Spotify Web Player (in Desktop Mode).
* **Interstitial & 18+ Pop-Up Killer:** Real-time DOM `MutationObserver` destroys full-screen ad overlays, pop-unders, and scroll locks automatically.
* **Organized Shield Dashboard:** Live stats for Ads Blocked, Trackers Blocked, Data Saved, per-site Trust/Allowlist, and one-tap list updates.

### 🚀 NOVA 4x Turbo Download Manager
* **4-Chunk Parallel 128 KB Streaming:** Splits supported files into 4 concurrent byte-range segments for up to 4x faster download speeds.
* **Persistent Foreground Service:** Runs inside a dedicated Android Foreground Service (`DownloadService`) with a live progress notification bar and CPU WakeLock—**downloads never stop** when you press Back or switch apps until completed or cancelled.
* **Direct Link Paste & Download:** Paste any direct file URL (`.apk`, `.mp4`, `.zip`, `.pdf`) right inside the Download Manager to start downloading immediately.
* **Pause, Resume & Public Storage Export:** Bug-free multi-segment Pause/Resume via HTTP `Range` headers, with automatic `MediaStore.Downloads` export directly into **`Downloads/NOVA Browser/`** so every file appears immediately in your phone's file manager.

### 📧 Built-In 3-Engine NOVA Temp Mail
* **Unique Disposable Email for Every User:** Generates a fresh, private email address on demand with automatic 3-server failover (**Guerrilla Mail → Mail.gw → Mail.tm**).
* **Live Inbox & OTP Reader:** Copy your address in one tap, refresh your inbox inside the app, and read verification emails or OTP codes without leaving the page.

### 🔓 Smart Browsing Tools
* **Login & Sign-Up Wall Bypass:** Strip mandatory login popups, blurred paywall overlays, and scroll locks—or reload stubborn pages using Search Crawler UA / Reader Mode.
* **Visual Multi-Tab Switcher:** Shows active (`🟢`) and suspended (`💤`) tabs, plus Recently Closed Tabs recovery.
* **Essentials Built In:** Search Engine Switcher (Google, Brave, Bing, Startpage, DuckDuckGo), Private/Incognito Mode, Desktop Site, Find-in-Page, Reader Mode, Page Translation, Font Zoom (75%–150%), QR Code Generator, Bookmarks, and History.

---

## ⚙️ How NOVA Browser Works (Under-the-Hood Architecture)

### 1. 🧠 How It Blocks 175,000+ Domains Using Only ~1.4 MB of RAM
Standard Android ad blockers store domains in `HashSet<String>` or `HashMap` objects, which easily consumes 20–30 MB of Java heap memory. NOVA Browser uses a **primitive sorted `long[]` 64-bit FNV-1a hash engine**:
* Each domain is hashed into a single 8-byte primitive `long` and sorted in memory (`175,000 × 8 bytes ≈ 1.4 MB` with zero per-object overhead).
* Every sub-resource request in `shouldInterceptRequest` walks parent domains using fast `O(log n)` binary search (`Arrays.binarySearch`). Matched ad/tracker streams are immediately aborted with an `IOException` before a single byte downloads.
* A synchronous `@JavascriptInterface` (`NovaShieldBridge`) connects `window.fetch` and `XMLHttpRequest` directly to the native hash engine so script-based probes are blocked against the full 175,000+ database.

### 2. ⚡ How the 4x Foreground Download Engine Works
* When a download starts, `DownloadService` sends an HTTP `HEAD` probe to check `Content-Length` and `Accept-Ranges: bytes`.
* For supported files ≥ 2 MB, NOVA divides the file into 4 byte ranges and downloads all 4 segments concurrently through `128 KB` `BufferedInputStream` buffers directly into a `RandomAccessFile` staging file, tracking exact per-chunk bytes written so Pause/Resume never corrupts data.
* On completion, the file is atomically streamed into Android's `MediaStore.Downloads` (`Downloads/NOVA Browser/`) and the staging file is deleted.

### 3. 🔋 How Battery & Low-RAM Optimization Works
* **Intelligent Tab Suspension:** Switching tabs immediately calls `onPause()` and sets `WebView.RENDERER_PRIORITY_WAIVED` on all background tabs so inactive pages use zero CPU and release renderer memory.
* **Zero Background Drain:** Minimizing the app pauses WebView timers (`pauseTimers()`).
* **Automatic Memory Guard:** `enforceLowRamBudget()` and `onTrimMemory()` automatically flush inactive tab caches whenever heap usage approaches 15 MB.

---

## 📦 Download, Install & Seamless Updates

Every push to `main` automatically compiles and publishes **`NOVA-Browser-v4.0.apk`** via GitHub Actions with a **persistent signing keystore** and **auto-incrementing `versionCode`**, allowing seamless in-place updates without losing your bookmarks, themes, or settings:

1. Open the **[Releases](../../releases)** section on the right side of this repository.
2. Under the latest release's **Assets**, tap **`NOVA-Browser-v4.0.apk`** to download.
3. Open the downloaded `.apk` and tap **Install / Update** *(if Play Protect shows an unrecognized developer prompt for a custom build, tap **More details → Install anyway**)*.

*(Note: If upgrading from v3.2 or earlier to v4.0, uninstall the older version once so the new persistent signing key is registered. All future updates from v4.0 onward will install directly over your existing app.)*

---

## 🗂️ Project Structure

```text
Nova-Browser--advanced-/
├── .github/workflows/
│   └── build-apk.yml                # Automated APK build, persistent keystore cache & GitHub Releases
├── app/
│   ├── build.gradle                 # Android SDK 34 config & auto-incrementing versionCode
│   └── src/main/
│       ├── AndroidManifest.xml      # Permissions & Foreground DownloadService declaration
│       ├── res/layout/
│       │   └── activity_main.xml    # Dynamic Liquid Glass top bar, find bar & bottom dock
│       └── java/com/nova/browser/
│           ├── MainActivity.java    # Core Browser, NOVA Shield v5, Temp Mail, Themes & UI
│           └── DownloadService.java # 4x Parallel Foreground Download Engine & MediaStore Export
├── build.gradle                     # Root Gradle plugin config
├── gradle.properties                # AndroidX & JVM build optimization flags
└── settings.gradle                  # Repository & module configuration
