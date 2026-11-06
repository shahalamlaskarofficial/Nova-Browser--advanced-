# 🌐 NOVA Browser – Advanced Privacy, Turbo Download & Low-RAM Android Browser

**NOVA Browser** is a next-generation, ultra-lightweight Android web browser engineered by **Shah Alam Laskar**. It combines **NOVA Shield v2** (a dual-layer Ad & Tracker Blocker), **NOVA Turbo Download Engine** (persistent multi-threaded downloader with direct link pasting), and **Intelligent Tab Suspension** for minimal RAM and battery consumption.

---

## ⚙️ How the App Works (Under-the-Hood Architecture)

### 1. 🛡️ How NOVA Shield v2 Blocks Ads & Trackers (Dual-Layer Engine)
Unlike basic browsers that only hide banners visually, NOVA Browser intercepts unwanted traffic at two independent layers before it can load:
* **Layer 1 — Network Stream Interception (`shouldInterceptRequest`):** Every sub-resource request (scripts, images, iframes, tracking pixels) is checked against an in-memory `ConcurrentHashMap` containing core EasyList/EasyPrivacy domains, OEM telemetry servers, and 3,500+ dynamically synced ad servers. Using $O(1)$ parent-domain hash walking, matched requests are immediately aborted at the stream level by throwing an `IOException`, saving bandwidth and CPU.
* **Layer 2 — Runtime JavaScript & Fetch/XHR Firewall:** At `onPageStarted`, NOVA injects a lightweight JavaScript security hook (`injectNovaShieldHook`) that wraps `window.fetch` and `XMLHttpRequest.prototype.open/send`. Any script attempting to ping analytics or ad endpoints is rejected with a network error, while cosmetic CSS rules strip empty ad containers and cookie consent overlays.

### 2. 🚀 How the NOVA Turbo Download Manager Works
* **64 KB Buffered Direct-to-Disk Streaming:** Downloads run on a dedicated 4-thread `ExecutorService` (`DOWNLOAD_EXECUTOR`). Instead of buffering entire files in RAM, NOVA streams bytes through a `64 KB` (`65,536` bytes) `BufferedInputStream` directly into a `RandomAccessFile` on disk, achieving maximum network throughput with near-zero memory overhead.
* **Pause & Resume via HTTP `Range` Headers:** When you pause a download and tap **Resume**, NOVA inspects the existing byte length on disk and sends an `Range: bytes=<downloaded>-` HTTP header, resuming from the exact byte where it left off.
* **Direct Link Paste Downloader:** Users can copy any file URL, tap **📋 Paste Clipboard** inside the Download Manager, and start a high-speed download directly without opening a webpage.
* **Background Persistence:** The download queue is managed statically at the process level. If you press the Android **Back** button while a download is running, NOVA automatically calls `moveTaskToBack(true)` instead of terminating the activity—keeping your downloads running at full speed in the background until completed or explicitly cancelled.

### 3. ⚡ How RAM & Battery Optimization Works
* **Intelligent Tab Suspension (`RENDERER_PRIORITY_WAIVED`):** When you switch from Tab A to Tab B, NOVA calls `onPause()` on all background `WebView` instances and sets Android's `RENDERER_PRIORITY_WAIVED` policy. This freezes background JavaScript execution and allows the Android OS to reclaim background renderer memory automatically.
* **Zero Background Battery Drain:** When the user minimizes the app, `onPause()` halts active timers (`pauseTimers()`), stopping background animations and CPU wakeups.
* **Automatic Low-Memory Recovery (`onTrimMemory`):** When Android signals low system RAM, NOVA automatically purges non-essential cache from inactive tabs and triggers garbage collection (`System.gc()`).

---

## ✨ Complete Feature Highlights

* **🧭 Smart Browsing:** Multi-tab visual switcher (`🟢` Active / `💤` Suspended), Recently Closed Tabs recovery, Google/Brave/Bing/Startpage/DuckDuckGo engine switcher, Distraction-Free Reader Mode, Find-in-Page, Page Translation, and QR Code Generator.
* **⬇ Turbo Download & File Manager:** Live speed (`MB/s`), progress bar, `ETA`, Pause/Resume/Cancel controls, Direct URL Paste Downloader, HTTPS security check, and built-in category filters (`APKs`, `Videos`, `Images`, `Documents`, `Archives`).
* **🛡️ Privacy & Security:** HTTPS-First automatic upgrades, Third-Party Cookie Blocking, Private/Incognito Mode, Per-Site Allowlist, and a live Privacy & Shield Dashboard.

---

## 📦 How to Download & Install the APK
Every commit to `main` automatically builds a fresh APK using GitHub Actions:
1. Click the **Actions** tab at the top of this repository.
2. Open the latest workflow run with a green checkmark (**✓**).
3. Scroll down to **Artifacts**, download **`Nova-Browser-Debug-APK`**, extract the `.zip`, and install `app-debug.apk`.

---

## 👨‍💻 About & Contact Developer

* **Developer:** Shah Alam Laskar
* **Email:** [shahalamlaskar.official@gmail.com](mailto:shahalamlaskar.official@gmail.com)
* **GitHub:** [@shahalamlaskarofficial](https://github.com/shahalamlaskarofficial)

Have feedback, feature ideas, or bug reports? Reach out directly via email at **shahalamlaskar.official@gmail.com**!
