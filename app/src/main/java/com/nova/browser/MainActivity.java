package com.nova.browser;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String DEV_NAME = "Shah Alam Laskar";
    private static final String DEV_EMAIL = "shahalamlaskar.official@gmail.com";
    private static final String DEV_GITHUB = "https://github.com/shahalamlaskarofficial";
    private static final long MAX_CACHE_BYTES = 15L * 1024L * 1024L; // 15 MB Strict Cap

    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final List<TurboDownloadTask> TURBO_DOWNLOADS = new CopyOnWriteArrayList<>();

    public static class TurboDownloadTask {
        public static final int STATUS_DOWNLOADING = 0;
        public static final int STATUS_PAUSED = 1;
        public static final int STATUS_COMPLETED = 2;
        public static final int STATUS_FAILED = 3;
        public static final int STATUS_CANCELLED = 4;

        public final String url;
        public final String fileName;
        public final File workingFile;
        public volatile File finalPublicFile;
        public volatile Uri publicMediaUri;
        public final boolean isSecureHttps;
        public volatile long downloadedBytes = 0;
        public volatile long totalBytes = -1;
        public volatile long speedBytesPerSec = 0;
        public volatile int status = STATUS_DOWNLOADING;
        public volatile String errorMsg = "";

        public TurboDownloadTask(String url, String fileName, File workingFile) {
            this.url = url;
            this.fileName = fileName;
            this.workingFile = workingFile;
            this.isSecureHttps = url != null && url.toLowerCase(Locale.US).startsWith("https://");
            if (workingFile.exists()) {
                this.downloadedBytes = workingFile.length();
            }
        }
    }

    private FrameLayout webContainer;
    private EditText etUrl, etFind;
    private ProgressBar progressBar;
    private Button btnShield, btnTabs, btnMenu;
    private LinearLayout rootLayout, topBar, bottomBar, layoutFind;

    private final List<WebView> tabs = new ArrayList<>();
    private final List<String> closedTabs = new ArrayList<>();
    private int currentTabIndex = 0;

    private SharedPreferences prefs;
    private boolean isDesktopMode = false;
    private boolean isIncognito = false;
    private boolean isAdBlockEnabled = true;
    private boolean isOverlayBypassEnabled = true;
    private boolean isZeroCacheMode = true;
    private boolean isBgAudioEnabled = true;
    private boolean isHttpsFirst = true;
    private boolean isDataSaver = false;
    private boolean isBatterySaver = true;
    private boolean isDarkTheme = true;
    private boolean isFullScreen = false;
    private int textZoomPercent = 100;
    private String defaultUserAgent;

    private String searchEngineName = "Google";
    private String searchEngineUrl = "https://www.google.com/search?q=";

    private int adsBlockedCount = 0;
    private int trackersBlockedCount = 0;
    private Set<String> allowlistDomains = new HashSet<>();

    private final Set<String> blockedAdHosts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> blockedTrackerHosts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private static final String[] CORE_AD_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "ads.youtube.com",
        "adnxs.com", "ads.yahoo.com", "adtech.yahooinc.com", "gemini.yahoo.com",
        "partnerads.ysm.yahoo.com", "amazon-adsystem.com", "adcolony.com",
        "media.net", "outbrain.com", "taboola.com", "popads.net", "popcash.net",
        "propellerads.com", "criteo.com", "criteo.net", "rubiconproject.com",
        "pubmatic.com", "openx.net", "smartadserver.com", "advertising.com",
        "moatads.com", "ads-twitter.com", "ads-api.twitter.com", "advertising.twitter.com",
        "an.facebook.com", "ads.linkedin.com", "ads.pinterest.com", "ads.tiktok.com",
        "ads-api.tiktok.com", "ads-sg.tiktok.com", "business-api.tiktok.com",
        "ads.snapchat.com", "unityads.unity3d.com", "applovin.com", "applvn.com",
        "ironsrc.com", "vungle.com", "chartboost.com", "inmobi.com", "startappservice.com",
        "yandexadexchange.net", "adfox.yandex.ru", "advertising.yandex.ru",
        "offerwall.yandex.net", "bdapi-ads.realmemobile.com", "bdapi-in-ads.realmemobile.com",
        "adsfs.oppomobile.com", "adx.ads.oppomobile.com", "ck.ads.oppomobile.com",
        "data.ads.oppomobile.com", "api.ad.xiaomi.com", "sdkconfig.ad.xiaomi.com",
        "sdkconfig.ad.intl.xiaomi.com", "iadsdk.apple.com", "api-adservices.apple.com",
        "samsungads.com", "ad.samsungadhub.com", "adsystem.com", "2mdn.net",
        "adform.net", "serving-sys.com", "flashtalking.com", "bidswitch.net",
        "casalemedia.com", "contextweb.com", "indexww.com", "lijit.com", "sharethrough.com",
        "spotxchange.com", "teads.tv", "triplelift.com", "yieldmo.com", "zedo.com",
        "exoclick.com", "juicyads.com", "trafficjunky.net", "revcontent.com", "mgid.com",
        "adblade.com", "adrecover.com", "adthrive.com", "mediavine.com", "ezoic.net",
        "audio-ak-spotify-com.akamaized.net", "adeventtracker.spotify.com", "ads-fa.spotify.com"
    };

    private static final String[] CORE_TRACKER_DOMAINS = {
        "google-analytics.com", "ssl.google-analytics.com", "analytics.google.com",
        "click.googleanalytics.com", "googletagmanager.com", "googletagservices.com",
        "connect.facebook.net", "pixel.facebook.com", "graph.facebook.com",
        "scorecardresearch.com", "quantserve.com", "hotjar.com", "hotjar.io",
        "mouseflow.com", "freshmarketer.com", "luckyorange.com", "luckyorange.net",
        "stats.wp.com", "pixel.wp.com", "bugsnag.com", "sentry.io", "sentry-cdn.com",
        "getsentry.com", "mixpanel.com", "segment.io", "segment.com", "newrelic.com",
        "clarity.ms", "fullstory.com", "amplitude.com", "branch.io", "appsflyer.com",
        "adjust.com", "kochava.com", "clever.com", "clevertap-prod.com",
        "udc.yahoo.com", "udcm.yahoo.com", "analytics.query.yahoo.com", "log.fc.yahoo.com",
        "trk.pinterest.com", "log.pinterest.com", "analytics.tiktok.com",
        "analytics-sg.tiktok.com", "log.byteoversea.com", "events.reddit.com",
        "events.redditmedia.com", "d.reddit.com", "alb.reddit.com", "tr.snapchat.com",
        "sc-analytics.appspot.com", "mc.yandex.ru", "appmetrica.yandex.ru",
        "adfstat.yandex.ru", "metrika.yandex.ru", "iot-eu-logser.realme.com",
        "iot-logser.realme.com", "data.mistat.xiaomi.com", "data.mistat.india.xiaomi.com",
        "data.mistat.rus.xiaomi.com", "tracking.rus.miui.com", "metrics.icloud.com",
        "metrics.mzstatic.com", "books-analytics-events.apple.com",
        "weather-analytics-events.apple.com", "notes-analytics-events.apple.com",
        "smetrics.samsung.com", "nmetrics.samsung.com", "samsung-com.112.2o7.net",
        "analytics-api.samsunghealthcn.com", "crashlytics.com", "app-measurement.com"
    };

    private static final String[] BLOCKED_URL_PATTERNS = {
        "/pagead/", "/ads.js", "/adframe.js", "/adserver", "/googlesyndication",
        "/doubleclick", "analytics.js", "gtag/js", "fbevents.js", "sentry-cdn",
        "bugsnag", "hotjar", "mouseflow", "luckyorange", "freshmarketer",
        "amazon-adsystem", "ads-twitter", "unityads", "adservice.", "/api/stats/ads",
        "/ptracking", "ad_type=", "&adurl=", "?adurl=", "/get_midroll_info"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("NovaBrowserPrefs", MODE_PRIVATE);
        adsBlockedCount = prefs.getInt("ads_blocked", 0);
        trackersBlockedCount = prefs.getInt("trackers_blocked", 0);
        searchEngineName = prefs.getString("search_name", "Google");
        searchEngineUrl = prefs.getString("search_url", "https://www.google.com/search?q=");
        isAdBlockEnabled = prefs.getBoolean("adblock_enabled", true);
        isOverlayBypassEnabled = prefs.getBoolean("overlay_bypass", true);
        isZeroCacheMode = prefs.getBoolean("zero_cache_mode", true);
        isBgAudioEnabled = prefs.getBoolean("bg_audio", true);
        isBatterySaver = prefs.getBoolean("battery_saver", true);
        allowlistDomains = new HashSet<>(prefs.getStringSet("allowlist", new HashSet<>()));

        blockedAdHosts.addAll(Arrays.asList(CORE_AD_DOMAINS));
        blockedTrackerHosts.addAll(Arrays.asList(CORE_TRACKER_DOMAINS));
        loadAndSyncFilterLists();
        trimDiskCacheIfNeeded(false);

        rootLayout = findViewById(R.id.rootLayout);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
        layoutFind = findViewById(R.id.layoutFind);
        webContainer = findViewById(R.id.webContainer);
        etUrl = findViewById(R.id.etUrl);
        etFind = findViewById(R.id.etFind);
        progressBar = findViewById(R.id.progressBar);
        btnShield = findViewById(R.id.btnShield);
        btnTabs = findViewById(R.id.btnTabs);
        btnMenu = findViewById(R.id.btnMenu);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnReload = findViewById(R.id.btnReload);
        Button btnHome = findViewById(R.id.btnHome);
        Button btnBookmarks = findViewById(R.id.btnBookmarks);
        Button btnDownloads = findViewById(R.id.btnDownloads);
        Button btnFindNext = findViewById(R.id.btnFindNext);
        Button btnFindClose = findViewById(R.id.btnFindClose);

        createNewTab("nova://home");

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUserInput(etUrl.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnShield.setOnClickListener(v -> showPrivacyDashboard());
        btnTabs.setOnClickListener(v -> showTabSwitcher());
        btnMenu.setOnClickListener(v -> showMainMenu());

        btnBack.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnReload.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) {
                String url = wv.getUrl();
                if (url == null || url.startsWith("https://nova.browser")) {
                    loadNovaHome(wv);
                } else {
                    wv.reload();
                }
            }
        });

        btnHome.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) loadNovaHome(wv);
        });

        btnBookmarks.setOnClickListener(v -> showBookmarksDialog());
        btnDownloads.setOnClickListener(v -> showTurboDownloadManager());

        etFind.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                WebView wv = getCurrentWebView();
                if (wv != null) wv.findAllAsync(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFindNext.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) wv.findNext(true);
        });

        btnFindClose.setOnClickListener(v -> {
            layoutFind.setVisibility(View.GONE);
            WebView wv = getCurrentWebView();
            if (wv != null) wv.clearMatches();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutFind.getVisibility() == View.VISIBLE) {
                    layoutFind.setVisibility(View.GONE);
                    return;
                }
                WebView wv = getCurrentWebView();
                if (wv != null && wv.canGoBack()) {
                    wv.goBack();
                } else if (tabs.size() > 1) {
                    closeTab(currentTabIndex);
                } else if (hasRunningDownloads()) {
                    Toast.makeText(MainActivity.this,
                            "⚡ Downloads running in background until completed or cancelled",
                            Toast.LENGTH_LONG).show();
                    moveTaskToBack(true);
                } else {
                    trimDiskCacheIfNeeded(true);
                    finish();
                }
            }
        });
    }

    private boolean hasRunningDownloads() {
        for (TurboDownloadTask t : TURBO_DOWNLOADS) {
            if (t.status == TurboDownloadTask.STATUS_DOWNLOADING) return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebView wv = getCurrentWebView();
        if (wv != null) {
            boolean isMediaSite = wv.getUrl() != null &&
                    (wv.getUrl().contains("youtube.com") || wv.getUrl().contains("spotify.com"));
            if (!isBgAudioEnabled || !isMediaSite) {
                wv.onPause();
                if (isBatterySaver) {
                    wv.pauseTimers();
                }
            }
        }
        trimDiskCacheIfNeeded(false);
        prefs.edit()
            .putInt("ads_blocked", adsBlockedCount)
            .putInt("trackers_blocked", trackersBlockedCount)
            .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView wv = getCurrentWebView();
        if (wv != null) {
            wv.onResume();
            wv.resumeTimers();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            for (int i = 0; i < tabs.size(); i++) {
                tabs.get(i).clearCache(isZeroCacheMode);
            }
            trimDiskCacheIfNeeded(true);
            System.gc();
        }
    }

    // =========================================================================
    // STRICT CACHE CONTROL (PREVENTS WEBVIEW STORAGE BLOAT)
    // =========================================================================

    private long getFolderSizeBytes(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                size += child.isDirectory() ? getFolderSizeBytes(child) : child.length();
            }
        }
        return size;
    }

    private void deleteFolderContents(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteFolderContents(child);
                }
                // Keep our local adblock host cache file intact
                if (!"nova_hosts_cache.txt".equals(child.getName())) {
                    child.delete();
                }
            }
        }
    }

    private void trimDiskCacheIfNeeded(boolean forceClean) {
        new Thread(() -> {
            try {
                File cacheDir = getCacheDir();
                File codeCache = getCodeCacheDir();
                File webViewCache = new File(getApplicationInfo().dataDir, "app_webview/Default/Cache");
                long totalCache = getFolderSizeBytes(cacheDir)
                        + getFolderSizeBytes(codeCache)
                        + getFolderSizeBytes(webViewCache);

                if (forceClean || isZeroCacheMode || totalCache > MAX_CACHE_BYTES) {
                    deleteFolderContents(cacheDir);
                    deleteFolderContents(codeCache);
                    deleteFolderContents(webViewCache);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // =========================================================================
    // NOVA SHIELD v3: INSTANT DISK-CACHED FILTER LISTS + YOUTUBE/WEB AD BLOCKER
    // =========================================================================

    private void loadAndSyncFilterLists() {
        new Thread(() -> {
            File cacheFile = new File(getFilesDir(), "nova_hosts_cache.txt");
            if (cacheFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(cacheFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) blockedAdHosts.add(line);
                    }
                } catch (Exception ignored) {}
            }

            String[] filterUrls = {
                "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext",
                "https://adaway.org/hosts.txt"
            };

            Set<String> newlyFetched = new HashSet<>();
            for (String src : filterUrls) {
                try {
                    URL url = new URL(src);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim().toLowerCase(Locale.US);
                            if (line.isEmpty() || line.startsWith("#")) continue;
                            if (line.startsWith("127.0.0.1") || line.startsWith("0.0.0.0")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) line = parts[1].trim();
                            }
                            if (!line.isEmpty() && !line.equals("localhost") && line.contains(".")) {
                                blockedAdHosts.add(line);
                                newlyFetched.add(line);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (!newlyFetched.isEmpty()) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile, false))) {
                    for (String h : blockedAdHosts) {
                        bw.write(h);
                        bw.newLine();
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private int classifyBlockedRequest(String host, String fullUrl) {
        if (host == null || host.isEmpty()) return 0;
        String cleanHost = host.toLowerCase(Locale.US);

        for (String allowed : allowlistDomains) {
            if (cleanHost.
