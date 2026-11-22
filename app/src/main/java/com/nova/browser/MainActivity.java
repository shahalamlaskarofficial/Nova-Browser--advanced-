package com.nova.browser;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String DEV_NAME = "Shah Alam Laskar";
    private static final String DEV_EMAIL = "shahalamlaskar.official@gmail.com";
    private static final String DEV_GITHUB = "https://github.com/shahalamlaskarofficial";
    private static final int MAX_SYNCED_DOMAINS = 175000;

    // Vibrant Liquid Glass Theme Keys
    private static final String THEME_SAPPHIRE = "sapphire";
    private static final String THEME_AMOLED = "amoled";
    private static final String THEME_CYBERPUNK = "cyberpunk";
    private static final String THEME_SUNSET = "sunset";
    private static final String THEME_EMERALD = "emerald";
    private static final String THEME_FROST = "frost";

    private static class QueueCardRefs {
        ProgressBar pb;
        TextView tvStatus;
        int renderedStatus;
    }

    public class NovaShieldBridge {
        @JavascriptInterface
        public boolean isBlocked(String url) {
            if (!isAdBlockEnabled || url == null || url.isEmpty()) return false;
            try {
                Uri uri = Uri.parse(url);
                int kind = classifyBlockedRequest(uri.getHost(), url);
                if (kind == 1) {
                    adsBlockedCount++;
                    return true;
                } else if (kind == 2) {
                    trackersBlockedCount++;
                    return true;
                }
            } catch (Exception ignored) {}
            return false;
        }

        @JavascriptInterface
        public void reportDomAdRemoved() {
            adsBlockedCount++;
        }

        @JavascriptInterface
        public void openCustomizePage() {
            uiHandler.post(MainActivity.this::showCustomizeThemeDialog);
        }

        @JavascriptInterface
        public void openTempMailPage() {
            uiHandler.post(MainActivity.this::showTempMailDialog);
        }
    }

    private FrameLayout webContainer;
    private EditText etUrl, etFind;
    private ProgressBar progressBar;
    private Button btnShield, btnTabs, btnMenu;
    private Button btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads;
    private LinearLayout rootLayout, topBar, bottomBar, layoutFind;

    private final List<WebView> tabs = new ArrayList<>();
    private final List<String> closedTabs = new ArrayList<>();
    private int currentTabIndex = 0;

    private SharedPreferences prefs;
    private boolean isDesktopMode = false;
    private boolean isIncognito = false;
    private boolean isAdBlockEnabled = true;
    private boolean isAggressiveInterstitialBlock = true;
    private boolean isHttpsFirst = true;
    private boolean isDataSaver = false;
    private boolean isBatterySaver = true;
    private boolean isFullScreen = false;
    private String currentTheme = THEME_SAPPHIRE;
    private int currentBarHeightDp = 56;
    private int textZoomPercent = 100;
    private String defaultUserAgent;

    private String searchEngineName = "Google";
    private String searchEngineUrl = "https://www.google.com/search?q=";

    private int adsBlockedCount = 0;
    private int trackersBlockedCount = 0;
    private Set<String> allowlistDomains = new HashSet<>();

    // Ultra-Low-RAM Primitive Sorted 64-bit Hash Arrays (~1.4 MB for 175,000+ domains!)
    private volatile long[] adHostHashes = new long[0];
    private volatile long[] trackerHostHashes = new long[0];
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> wallpaperPickerLauncher;

    private static final Set<String> SAFE_MAIN_DOMAINS = new HashSet<>(Arrays.asList(
        "nova.browser", "google.com", "www.google.com", "youtube.com", "m.youtube.com",
        "www.youtube.com", "open.spotify.com", "github.com", "chatgpt.com",
        "wikipedia.org", "reddit.com", "testfile.org", "fast.com",
        "adblock.turtlecute.org", "canyoublockit.com", "superadblocktest.com",
        "api.guerrillamail.com", "api.mail.gw", "api.mail.tm"
    ));

    private static final String[] CORE_AD_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "ads.youtube.com", "adtago.s3.amazonaws.com",
        "advice-ads.s3.amazonaws.com", "amazon-adsystem.com", "adnxs.com",
        "ads.yahoo.com", "adtech.yahooinc.com", "adtech.yahoo.com", "gemini.yahoo.com",
        "partnerads.ysm.yahoo.com", "adcolony.com", "media.net", "outbrain.com",
        "taboola.com", "popads.net", "popcash.net", "propellerads.com",
        "criteo.com", "criteo.net", "rubiconproject.com", "pubmatic.com",
        "openx.net", "smartadserver.com", "advertising.com", "moatads.com",
        "ads-twitter.com", "ads-api.twitter.com", "advertising.twitter.com",
        "an.facebook.com", "ads.linkedin.com", "ads.pinterest.com", "ads.tiktok.com",
        "ads-api.tiktok.com", "ads-sg.tiktok.com", "business-api.tiktok.com",
        "ads.snapchat.com", "unityads.unity3d.com", "applovin.com", "applvn.com",
        "ironsrc.com", "vungle.com", "chartboost.com", "inmobi.com", "startappservice.com",
        "yandexadexchange.net", "adfox.yandex.ru", "advertising.yandex.ru",
        "offerwall.yandex.net", "bdapi-ads.realmemobile.com", "bdapi-in-ads.realmemobile.com",
        "adsfs.oppomobile.com", "adx.ads.oppomobile.com", "ck.ads.oppomobile.com",
        "data.ads.oppomobile.com", "api.ad.xiaomi.com", "sdkconfig.ad.xiaomi.com",
        "sdkconfig.ad.intl.xiaomi.com", "iadsdk.apple.com", "api-adservices.apple.com",
        "samsungads.com", "ad.samsungadhub.com", "adsystem.com",
        "adeventtracker.spotify.com", "ads-fa.spotify.com", "heads-fa.spotify.com",
        "exoclick.com", "exosrv.com", "realsrv.com", "juicyads.com", "trafficjunky.net",
        "hilltopads.net", "adsterra.com", "highcpmgate.com", "profitablegatecpm.com",
        "mgid.com", "revcontent.com", "casalemedia.com", "indexww.com", "yieldmo.com",
        "sharethrough.com", "33across.com", "triplelift.com", "teads.tv", "bidswitch.net",
        "lijit.com", "sovrn.com", "sonobi.com", "adroll.com", "carbonads.com",
        "buysellads.com", "a-ads.com", "coinzilla.com", "adblade.com", "zemanta.com",
        "plista.com", "adform.net", "serving-sys.com", "flashtalking.com", "adition.com",
        "undertone.com", "gumgum.com", "kargo.com", "yieldlab.net", "improvedigital.com",
        "contextweb.com", "pulsepoint.com", "spotxchange.com", "springserve.com"
    };

    private static final String[] CORE_TRACKER_DOMAINS = {
        "google-analytics.com", "analytics.google.com", "click.googleanalytics.com",
        "googletagmanager.com", "googletagservices.com", "analyticsengine.s3.amazonaws.com",
        "analytics.s3.amazonaws.com", "connect.facebook.net", "pixel.facebook.com",
        "graph.facebook.com", "scorecardresearch.com", "quantserve.com", "hotjar.com",
        "hotjar.io", "mouseflow.com", "freshmarketer.com", "luckyorange.com",
        "luckyorange.net", "stats.wp.com", "pixel.wp.com", "bugsnag.com", "sentry.io",
        "sentry-cdn.com", "getsentry.com", "mixpanel.com", "segment.io", "segment.com",
        "newrelic.com", "clarity.ms", "fullstory.com", "amplitude.com", "branch.io",
        "appsflyer.com", "adjust.com", "kochava.com", "clevertap-prod.com",
        "geo.yahoo.com", "udc.yahoo.com", "udcm.yahoo.com", "analytics.yahoo.com",
        "analytics.query.yahoo.com", "log.fc.yahoo.com", "analytics.pointdrive.linkedin.com",
        "px.ads.linkedin.com", "snap.licdn.com", "trk.pinterest.com", "log.pinterest.com",
        "analytics.pinterest.com", "ct.pinterest.com", "analytics.tiktok.com",
        "analytics-sg.tiktok.com", "log.byteoversea.com", "events.reddit.com",
        "events.redditmedia.com", "d.reddit.com", "alb.reddit.com", "tr.snapchat.com",
        "sc-static.net", "sc-analytics.appspot.com", "extmaps-api.yandex.net",
        "mc.yandex.ru", "appmetrica.yandex.ru", "adfstat.yandex.ru", "metrika.yandex.ru",
        "iot-eu-logser.realme.com", "iot-logser.realme.com", "data.mistat.xiaomi.com",
        "data.mistat.india.xiaomi.com", "data.mistat.rus.xiaomi.com", "tracking.rus.miui.com",
        "metrics.data.hicloud.com", "metrics2.data.hicloud.com", "grs.hicloud.com",
        "logservice.hicloud.com", "logservice1.hicloud.com", "logbak.hicloud.com",
        "click.oneplus.cn", "open.oneplus.net", "metrics.icloud.com", "metrics.mzstatic.com",
        "books-analytics-events.apple.com", "weather-analytics-events.apple.com",
        "notes-analytics-events.apple.com", "smetrics.samsung.com", "nmetrics.samsung.com",
        "samsung-com.112.2o7.net", "analytics-api.samsunghealthcn.com",
        "analytics.spotify.com", "log.spotify.com", "log2.spotify.com", "pixel.spotify.com",
        "bat.bing.com", "omtrdc.net", "demdex.net", "everesttech.net", "krxd.net",
        "bluekai.com", "rlcdn.com", "tapad.com", "crwdcntrl.net", "exelator.com",
        "eyeota.net", "mathtag.com", "rfihub.com", "adsrvr.org", "turn.com",
        "chartbeat.com", "chartbeat.net", "parsely.com", "optimizely.com", "crazyegg.com",
        "kissmetrics.com", "heap.io", "heapanalytics.com", "pendo.io", "intercom.io",
        "hubspot.com", "hs-analytics.net", "hs-scripts.com", "marketo.net", "pardot.com"
    };

    private static final String[] AD_SUBDOMAIN_PREFIXES = {
        "ad.", "ads.", "ad1.", "ad2.", "adserver.", "adservice.", "adsystem.", "adtech.",
        "adclick.", "advert.", "adverts.", "advertising.", "banner.", "banners.",
        "popunder.", "popads.", "track.", "tracker.", "tracking.", "telemetry.",
        "metrics.", "analytics.", "pixel.", "beacon.", "stats.", "click.", "clicks."
    };

    private static final String[] BLOCKED_URL_KEYWORDS = {
        "/pagead/", "/ads.js", "/adframe", "/adserver", "/adscript", "/banner_ad",
        "/popunder", "/interstitial", "/googlesyndication", "/doubleclick",
        "analytics.js", "gtag/js", "fbevents.js", "sentry-cdn", "bugsnag",
        "hotjar", "mouseflow", "luckyorange", "freshmarketer", "amazon-adsystem",
        "ads-twitter", "unityads", "adservice.", "prebid.js", "gpt.js", "pubads_impl",
        "youtube.com/api/stats/ads", "youtube.com/pagead/", "youtube.com/ptracking",
        "youtube.com/get_midroll_info", "youtube.com/api/stats/atr",
        "googlevideo.com/videoplayback?adformat=", "&ctier=l", "&oad=",
        "spotify.com/ads/", "spotify.com/ad-logic/", "spotify.com/gabo-receiver-service/",
        "spotify.com/melody/v1/ad", "adeventtracker.spotify"
    };

    private static final String[] FILTER_LIST_URLS = {
        "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext",
        "https://adaway.org/hosts.txt",
        "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/light.txt",
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
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
        isAggressiveInterstitialBlock = prefs.getBoolean("aggressive_interstitial", true);
        isBatterySaver = prefs.getBoolean("battery_saver", true);
        currentTheme = prefs.getString("nova_theme", THEME_SAPPHIRE);
        allowlistDomains = new HashSet<>(prefs.getStringSet("allowlist", new HashSet<>()));

        // Request notification permission on Android 13+ so live download bar appears cleanly
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        wallpaperPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) saveCustomWallpaperFromUri(uri);
                });

        initCoreHashes();
        loadCachedAndSyncFilterLists(false);

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

        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnReload = findViewById(R.id.btnReload);
        btnHome = findViewById(R.id.btnHome);
        btnBookmarks = findViewById(R.id.btnBookmarks);
        btnDownloads = findViewById(R.id.btnDownloads);
        Button btnFindNext = findViewById(R.id.btnFindNext);
        Button btnFindClose = findViewById(R.id.btnFindClose);

        applyLiquidGlassTheme();
        createNewTab("nova://home");

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUserInput(etUrl.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnShield.setOnClickListener(v -> { pulseButton(v); showOrganizedShieldDashboard(); });
        btnTabs.setOnClickListener(v -> { pulseButton(v); showTabSwitcher(); });
        btnMenu.setOnClickListener(v -> { pulseButton(v); showLiquidGlassMainMenu(); });

        btnBack.setOnClickListener(v -> {
            pulseButton(v);
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            pulseButton(v);
            WebView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnReload.setOnClickListener(v -> {
            pulseButton(v);
            WebView wv = getCurrentWebView();
            if (wv != null) {
                String url = wv.getUrl();
                if (url == null || url.startsWith("https://nova.browser")) loadNovaHome(wv);
                else wv.reload();
            }
        });

        btnHome.setOnClickListener(v -> {
            pulseButton(v);
            WebView wv = getCurrentWebView();
            if (wv != null) loadNovaHome(wv);
        });

        btnBookmarks.setOnClickListener(v -> { pulseButton(v); showBookmarksDialog(); });
        btnDownloads.setOnClickListener(v -> { pulseButton(v); showTurboDownloadManager(); });

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
                } else if (DownloadService.hasActiveDownloads()) {
                    Toast.makeText(MainActivity.this,
                            "⚡ Downloads continue at full speed in notification bar!",
                            Toast.LENGTH_LONG).show();
                    moveTaskToBack(true);
                } else {
                    finish();
                }
            }
        });
    }

    // =========================================================================
    // ULTRA-LOW RAM 64-BIT HASH ADBLOCK ENGINE (1.4 MB FOR 175,000+ DOMAINS!)
    // =========================================================================

    private static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= (s.charAt(i) & 0xff);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static long[] sortAndCompact(long[] arr, int count) {
        if (count <= 0) return new long[0];
        Arrays.sort(arr, 0, count);
        int unique = 1;
        for (int i = 1; i < count; i++) {
            if (arr[i] != arr[unique - 1]) {
                arr[unique++] = arr[i];
            }
        }
        return Arrays.copyOf(arr, unique);
    }

    private void initCoreHashes() {
        long[] ads = new long[CORE_AD_DOMAINS.length];
        for (int i = 0; i < CORE_AD_DOMAINS.length; i++) ads[i] = fnv1a64(CORE_AD_DOMAINS[i]);
        adHostHashes = sortAndCompact(ads, ads.length);

        long[] trk = new long[CORE_TRACKER_DOMAINS.length];
        for (int i = 0; i < CORE_TRACKER_DOMAINS.length; i++) trk[i] = fnv1a64(CORE_TRACKER_DOMAINS[i]);
        trackerHostHashes = sortAndCompact(trk, trk.length);
    }

    private void loadCachedAndSyncFilterLists(boolean forceRefresh) {
        new Thread(() -> {
            File cacheFile = new File(getFilesDir(), "nova_shield_v6_hosts.txt");
            if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 10000) {
                loadHashesFromFile(cacheFile);
            }

            long ageMs = System.currentTimeMillis() - cacheFile.lastModified();
            if (forceRefresh || !cacheFile.exists() || cacheFile.length() < 10000 || ageMs > 86400000L) {
                File tmpFile = new File(getFilesDir(), "nova_shield_v6_tmp.txt");
                int written = 0;
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile), 32768)) {
                    for (String listUrl : FILTER_LIST_URLS) {
                        if (written >= MAX_SYNCED_DOMAINS) break;
                        HttpURLConnection conn = null;
                        try {
                            conn = (HttpURLConnection) new URL(listUrl).openConnection();
                            conn.setConnectTimeout(7000);
                            conn.setReadTimeout(12000);
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 16384)) {
                                String line;
                                while ((line = reader.readLine()) != null && written < MAX_SYNCED_DOMAINS) {
                                    String host = parseBlocklistDomain(line);
                                    if (host != null && !SAFE_MAIN_DOMAINS.contains(host)) {
                                        bw.write(host);
                                        bw.newLine();
                                        written++;
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        } finally {
                            if (conn != null) conn.disconnect();
                        }
                    }
                } catch (Exception ignored) {}

                if (written > 5000) {
                    if (cacheFile.exists()) cacheFile.delete();
                    tmpFile.renameTo(cacheFile);
                    loadHashesFromFile(cacheFile);
                    int total = adHostHashes.length + trackerHostHashes.length;
                    uiHandler.post(() -> Toast.makeText(MainActivity.this,
                            "🛡️ NOVA Shield Ready: " + total + " rules (Low-RAM Mode)",
                            Toast.LENGTH_SHORT).show());
                } else {
                    tmpFile.delete();
                }
            }
            enforceLowRamBudget();
        }).start();
    }

    private void loadHashesFromFile(File file) {
        long[] buf = new long[MAX_SYNCED_DOMAINS + CORE_AD_DOMAINS.length];
        int count = 0;
        for (String core : CORE_AD_DOMAINS) buf[count++] = fnv1a64(core);

        try (BufferedReader br = new BufferedReader(new FileReader(file), 16384)) {
            String line;
            while ((line = br.readLine()) != null && count < buf.length) {
                if (!line.isEmpty()) {
                    buf[count++] = fnv1a64(line);
                }
            }
        } catch (Exception ignored) {}

        adHostHashes = sortAndCompact(buf, count);
    }

    private String parseBlocklistDomain(String rawLine) {
        if (rawLine == null) return null;
        String s = rawLine.trim().toLowerCase(Locale.US);
        if (s.isEmpty() || s.startsWith("#") || s.startsWith("!")) return null;
        int commentIdx = s.indexOf('#');
        if (commentIdx != -1) s = s.substring(0, commentIdx).trim();

        if (s.startsWith("0.0.0.0") || s.startsWith("127.0.0.1")) {
            String[] parts = s.split("\\s+");
            if (parts.length >= 2) s = parts[1];
            else return null;
        } else if (s.startsWith("||")) {
            s = s.substring(2);
            int caret = s.indexOf('^');
            if (caret != -1) s = s.substring(0, caret);
        }

        if (s.equals("localhost") || s.equals("0.0.0.0") || s.equals("127.0.0.1") || !s.contains(".") || s.contains("/")) {
            return null;
        }
        return s;
    }

    private int classifyBlockedRequest(String host, String fullUrl) {
        if (host == null || host.isEmpty()) return 0;
        String cleanHost = host.toLowerCase(Locale.US);

        if (SAFE_MAIN_DOMAINS.contains(cleanHost)) return 0;

        for (String allowed : allowlistDomains) {
            if (cleanHost.equals(allowed) || cleanHost.endsWith("." + allowed)) return 0;
        }

        long[] trkArr = trackerHostHashes;
        long[] adArr = adHostHashes;
        String part = cleanHost;
        while (part.contains(".")) {
            long h = fnv1a64(part);
            if (Arrays.binarySearch(trkArr, h) >= 0) return 2;
            if (Arrays.binarySearch(adArr, h) >= 0) return 1;
            int dot = part.indexOf('.');
            part = part.substring(dot + 1);
        }

        for (String prefix : AD_SUBDOMAIN_PREFIXES) {
            if (cleanHost.startsWith(prefix)) return 1;
        }

        if (fullUrl != null) {
            String lowerUrl = fullUrl.toLowerCase(Locale.US);
            for (String kw : BLOCKED_URL_KEYWORDS) {
                if (lowerUrl.contains(kw)) return 1;
            }
        }
        return 0;
    }

    private WebResourceResponse createBlockedResponse() {
        InputStream abortStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Blocked by NOVA Shield");
            }
        };
        return new WebResourceResponse("text/plain", "UTF-8", abortStream);
    }

    private long getUsedRamMbLong() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    private String getRamUsageMb() {
        return getUsedRamMbLong() + " MB";
    }

    private void enforceLowRamBudget() {
        if (getUsedRamMbLong() >= 15) {
            uiHandler.post(() -> {
                for (int i = 0; i < tabs.size(); i++) {
                    if (i != currentTabIndex) {
                        tabs.get(i).clearCache(false);
                        tabs.get(i).onPause();
                    }
                }
            });
            System.gc();
        }
    }

    // =========================================================================
    // VIBRANT LIQUID GLASS THEMES & SMOOTH BAR ANIMATION
    // =========================================================================

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void pulseButton(View v) {
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(105).start())
            .start();
    }

    private void animateBarsToDp(int targetDp) {
        if (isFullScreen || currentBarHeightDp == targetDp) return;
        currentBarHeightDp = targetDp;

        final int targetPx = dp(targetDp);
        final int startPx = topBar.getLayoutParams().height;
        final int targetMargin = targetDp == 0 ? 0 : (targetDp >= 52 ? dp(5) : dp(3));
        final float targetScale = targetDp >= 52 ? 1.0f : (targetDp == 0 ? 0.85f : 0.90f);

        ValueAnimator anim = ValueAnimator.ofInt(startPx, targetPx);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(va -> {
            int val = (int) va.getAnimatedValue();
            float alpha = targetDp == 0 ? (val / (float) Math.max(1, dp(44))) : 1f;

            LinearLayout.LayoutParams topLp = (LinearLayout.LayoutParams) topBar.getLayoutParams();
            topLp.height = val;
            topLp.topMargin = targetMargin;
            topLp.bottomMargin = targetMargin;
            topBar.setLayoutParams(topLp);
            topBar.setAlpha(Math.min(1f, Math.max(0f, alpha)));

            LinearLayout.LayoutParams botLp = (LinearLayout.LayoutParams) bottomBar.getLayoutParams();
            botLp.height = val;
            botLp.topMargin = targetMargin;
            botLp.bottomMargin = targetMargin;
            bottomBar.setLayoutParams(botLp);
            bottomBar.setAlpha(Math.min(1f, Math.max(0f, alpha)));
        });
        anim.start();

        for (Button b : new Button[]{btnShield, btnTabs, btnMenu, btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
            b.animate().scaleX(targetScale).scaleY(targetScale).setDuration(210).start();
        }
    }

    private GradientDrawable makeGlassDrawable(int topColor, int bottomColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{topColor, bottomColor});
        gd.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) gd.setStroke(dp(strokeDp), strokeColor);
        return gd;
    }

    private String getAccentHex() {
        switch (currentTheme) {
            case THEME_AMOLED: return "#00E5FF";
            case THEME_CYBERPUNK: return "#FF2E93";
            case THEME_SUNSET: return "#FF8A00";
            case THEME_EMERALD: return "#00F5A0";
            case THEME_FROST: return "#0066FF";
            default: return "#00D2FF";
        }
    }

    private String getSecondaryAccentHex() {
        switch (currentTheme) {
            case THEME_AMOLED: return "#2979FF";
            case THEME_CYBERPUNK: return "#B84DFF";
            case THEME_SUNSET: return "#FFD000";
            case THEME_EMERALD: return "#00D9F5";
            case THEME_FROST: return "#00B4FF";
            default: return "#3A7BD5";
        }
    }

    private void applyLiquidGlassTheme() {
        boolean isLight = THEME_FROST.equals(currentTheme);
        String accent = getAccentHex();

        if (isLight) {
            rootLayout.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#E0F2FE"), Color.parseColor("#F8FAFC"), Color.parseColor("#DBEAFE")}));

            int barTop = isIncognito ? Color.parseColor("#E6F3E8FF") : Color.parseColor("#E6FFFFFF");
            int barBot = isIncognito ? Color.parseColor("#E6E9D5FF") : Color.parseColor("#CCE0F2FE");
            int rim = Color.parseColor("#990066FF");

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 22, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 22, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#F2FFFFFF"), Color.parseColor("#E6F1F5F9"), Color.parseColor("#880066FF"), 18, 1));
            etUrl.setTextColor(Color.parseColor("#0F172A"));
            etUrl.setHintTextColor(Color.parseColor("#64748B"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#F2FFFFFF"), Color.parseColor("#E6F1F5F9"), Color.parseColor("#880066FF"), 14, 1));
            etFind.setTextColor(Color.parseColor("#0F172A"));

            styleGlassPillButton(btnShield, "#AAFFFFFF", "#66DBEAFE", "#880066FF");
            styleGlassPillButton(btnTabs, "#AAFFFFFF", "#66DBEAFE", "#880066FF");
            styleGlassPillButton(btnMenu, "#AAFFFFFF", "#66FFFFFF", "#660066FF");

            for (Button dockBtn : new Button[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#AAFFFFFF", "#66E0F2FE", "#660066FF");
            }
            btnBack.setTextColor(Color.parseColor("#0F172A"));
            btnForward.setTextColor(Color.parseColor("#0F172A"));
            btnReload.setTextColor(Color.parseColor("#0F172A"));
            btnMenu.setTextColor(Color.parseColor("#0F172A"));
            btnTabs.setTextColor(Color.parseColor(accent));
            btnHome.setTextColor(Color.parseColor(accent));
            btnBookmarks.setTextColor(Color.parseColor("#D97706"));
            btnDownloads.setTextColor(Color.parseColor(accent));
        } else {
            int bg1, bg2, bg3, barTop, barBot, rim;
            if (THEME_AMOLED.equals(currentTheme)) {
                bg1 = bg2 = bg3 = Color.parseColor("#000000");
                barTop = Color.parseColor("#F0080C12");
                barBot = Color.parseColor("#F0000000");
                rim = Color.parseColor("#8800E5FF");
            } else if (THEME_CYBERPUNK.equals(currentTheme)) {
                bg1 = Color.parseColor("#180436");
                bg2 = Color.parseColor("#350A6B");
                bg3 = Color.parseColor("#140329");
                barTop = Color.parseColor("#CC440E80");
                barBot = Color.parseColor("#CC210645");
                rim = Color.parseColor("#99FF2E93");
            } else if (THEME_SUNSET.equals(currentTheme)) {
                bg1 = Color.parseColor("#240902");
                bg2 = Color.parseColor("#471705");
                bg3 = Color.parseColor("#1A0602");
                barTop = Color.parseColor("#CC591D06");
                barBot = Color.parseColor("#CC2E0D03");
                rim = Color.parseColor("#99FF8A00");
            } else if (THEME_EMERALD.equals(currentTheme)) {
                bg1 = Color.parseColor("#021F17");
                bg2 = Color.parseColor("#054030");
                bg3 = Color.parseColor("#02140F");
                barTop = Color.parseColor("#CC074D3A");
                barBot = Color.parseColor("#CC03291F");
                rim = Color.parseColor("#9900F5A0");
            } else {
                // Electric Sapphire
                bg1 = Color.parseColor("#061438");
                bg2 = Color.parseColor("#0F2D75");
                bg3 = Color.parseColor("#050F2B");
                barTop = Color.parseColor("#CC12388A");
                barBot = Color.parseColor("#CC091D4D");
                rim = Color.parseColor("#9900D2FF");
            }

            if (isIncognito) {
                barTop = Color.parseColor("#D94A044E");
                barBot = Color.parseColor("#D92E0538");
                rim = Color.parseColor("#AAE879F9");
            }

            rootLayout.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{bg1, bg2, bg3}));

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 22, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 22, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#99040914"), Color.parseColor("#990E1A33"), rim, 18, 1));
            etUrl.setTextColor(Color.parseColor("#FFFFFF"));
            etUrl.setHintTextColor(Color.parseColor("#CBD5E1"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#AA040914"), Color.parseColor("#AA0E1A33"), rim, 14, 1));
            etFind.setTextColor(Color.parseColor("#FFFFFF"));

            styleGlassPillButton(btnShield, "#33FFFFFF", "#16FFFFFF", "#66FFFFFF");
            styleGlassPillButton(btnTabs, "#38FFFFFF", "#18FFFFFF", "#77FFFFFF");
            styleGlassPillButton(btnMenu, "#2EFFFFFF", "#14FFFFFF", "#55FFFFFF");

            for (Button dockBtn : new Button[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#28FFFFFF", "#10FFFFFF", "#44FFFFFF");
            }
            btnBack.setTextColor(Color.parseColor("#FFFFFF"));
            btnForward.setTextColor(Color.parseColor("#FFFFFF"));
            btnReload.setTextColor(Color.parseColor("#FFFFFF"));
            btnMenu.setTextColor(Color.parseColor("#FFFFFF"));
            btnTabs.setTextColor(Color.parseColor(accent));
            btnHome.setTextColor(Color.parseColor(accent));
            btnBookmarks.setTextColor(Color.parseColor("#FFD000"));
            btnDownloads.setTextColor(Color.parseColor(accent));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topBar.setElevation(dp(6));
            bottomBar.setElevation(dp(6));
        }
        updateDownloadDockBadge();
    }

    private void styleGlassPillButton(Button b, String topHex, String botHex, String strokeHex) {
        b.setBackground(makeGlassDrawable(
                Color.parseColor(topHex),
                Color.parseColor(botHex),
                Color.parseColor(strokeHex),
                14, 1));
    }

    private void saveCustomWallpaperFromUri(Uri uri) {
        new Thread(() -> {
            Bitmap raw = null;
            Bitmap scaled = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                raw = BitmapFactory.decodeStream(in);
                if (raw == null) return;
                int maxDim = 720; // Low-RAM optimized wallpaper dimension
                float scale = Math.min(1f, (float) maxDim / Math.max(raw.getWidth(), raw.getHeight()));
                scaled = Bitmap.createScaledBitmap(raw,
                        Math.max(1, Math.round(raw.getWidth() * scale)),
                        Math.max(1, Math.round(raw.getHeight() * scale)),
                        true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                File wpFile = new File(getFilesDir(), "nova_home_wallpaper.b64");
                try (FileWriter fw = new FileWriter(wpFile)) {
                    fw.write(b64);
                }
                uiHandler.post(() -> {
                    Toast.makeText(this, "🖼️ Custom Wallpaper Applied!", Toast.LENGTH_SHORT).show();
                    WebView wv = getCurrentWebView();
                    if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                        loadNovaHome(wv);
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show());
            } finally {
                if (scaled != null && scaled != raw) scaled.recycle();
                if (raw != null) raw.recycle();
                System.gc();
            }
        }).start();
    }

    private String readCustomWallpaperBase64() {
        File wpFile = new File(getFilesDir(), "nova_home_wallpaper.b64");
        if (!wpFile.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(wpFile))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // LIQUID GLASS BOX MAIN MENU (REPLACES PLAIN GREY MENU)
    // =========================================================================

    private void showLiquidGlassMainMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WebView wv = getCurrentWebView();
        String accent = getAccentHex();

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + accent.substring(1)),
                26, 1));
        outer.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Header
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setPadding(dp(4), 0, dp(4), dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✨ NOVA Browser v4.0");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(17.5f);
        hdr.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClose = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        hdr.addView(btnClose, new LinearLayout.LayoutParams(dp(38), dp(36)));
        outer.addView(hdr);

        ScrollView sc = new ScrollView(this);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        sc.addView(stack);

        // SECTION 1: Quick Hub 2x2 Neon Grid
        LinearLayout hubCard = createGlassCard();
        hubCard.addView(makeSectionLabel("⚡ QUICK HUB", accent));
        addMenuPairRow(hubCard,
            "🛡️ NOVA Shield", () -> { dialog.dismiss(); showOrganizedShieldDashboard(); },
            "⬇ Downloads", () -> { dialog.dismiss(); showTurboDownloadManager(); });
        addMenuPairRow(hubCard,
            "📧 Temp Mail", () -> { dialog.dismiss(); showTempMailDialog(); },
            "🎨 Themes & WP", () -> { dialog.dismiss(); showCustomizeThemeDialog(); });
        stack.addView(hubCard);

        // SECTION 2: Smart Browsing & Privacy Controls
        LinearLayout toolsCard = createGlassCard();
        toolsCard.addView(makeSectionLabel("🧭 BROWSING & PRIVACY", accent));
        addMenuPairRow(toolsCard,
            "🔓 Login Bypass", () -> { dialog.dismiss(); showLoginBypassDialog(wv); },
            "🔍 Engine: " + searchEngineName, () -> { dialog.dismiss(); showSearchEngineDialog(); });
        addMenuPairRow(toolsCard,
            "📖 Reader Mode", () -> { dialog.dismiss(); toggleReaderMode(wv); },
            "🔎 Find in Page", () -> { dialog.dismiss(); layoutFind.setVisibility(View.VISIBLE); etFind.requestFocus(); });
        addMenuPairRow(toolsCard,
            "🖥️ Desktop: " + (isDesktopMode ? "ON" : "OFF"), () -> { dialog.dismiss(); toggleDesktopMode(); },
            "🕵️ Private: " + (isIncognito ? "ON" : "OFF"), () -> { dialog.dismiss(); toggleIncognitoMode(); });
        stack.addView(toolsCard);

        // SECTION 3: Performance, Display & Utilities
        LinearLayout sysCard = createGlassCard();
        sysCard.addView(makeSectionLabel("⚙️ PERFORMANCE & TOOLS", accent));
        addMenuPairRow(sysCard,
            "⚡ RAM (" + getRamUsageMb() + ")", () -> { dialog.dismiss(); showPerformanceOptimizerDialog(); },
            "📉 Data Saver: " + (isDataSaver ? "ON" : "OFF"), () -> {
                dialog.dismiss();
                isDataSaver = !isDataSaver;
                for (WebView tab : tabs) tab.getSettings().setBlockNetworkImage(isDataSaver);
                if (wv != null) wv.reload();
                Toast.makeText(this, "Data Saver " + (isDataSaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        addMenuPairRow(sysCard,
            "🔠 Zoom: " + textZoomPercent + "%", () -> { dialog.dismiss(); showZoomDialog(); },
            "📺 FullScreen: " + (isFullScreen ? "ON" : "OFF"), () -> { dialog.dismiss(); toggleFullScreen(); });
        addMenuPairRow(sysCard,
            "🌐 Translate", () -> {
                dialog.dismiss();
                if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                    wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(wv.getUrl()));
                }
            },
            "📜 History", () -> { dialog.dismiss(); showHistoryDialog(); });
        addMenuPairRow(sysCard,
            "🔗 Share / QR", () -> { dialog.dismiss(); shareOrCopyUrl(wv); },
            "🧹 Clear Data", () -> { dialog.dismiss(); clearBrowserData(wv); });
        stack.addView(sysCard);

        // SECTION 4: Developer & Regular Updates Footer Button
        Button btnDev = createLargeGlassButton("👨‍💻 About & Contact Developer (Regular Updates)", "#0284C7", "#6366F1", "#FFFFFF");
        btnDev.setOnClickListener(v -> { dialog.dismiss(); showAboutDeveloperDialog(); });
        stack.addView(btnDev, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        outer.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(10), dp(14), dp(10), dp(14));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.84f);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxH);
            dialog.getWindow().setDimAmount(0.74f);
        }
        dialog.show();
    }

    private TextView makeSectionLabel(String title, String accentHex) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor(accentHex));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(11f);
        tv.setPadding(dp(2), 0, 0, dp(8));
        return tv;
    }

    private void addMenuPairRow(LinearLayout parent, String leftLabel, Runnable leftAct, String rightLabel, Runnable rightAct) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, 0, 0, dp(7));

        Button b1 = createLargeGlassButton(leftLabel, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b1.setTextSize(12.2f);
        b1.setOnClickListener(v -> leftAct.run());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp1.setMargins(0, 0, dp(7), 0);
        row.addView(b1, lp1);

        Button b2 = createLargeGlassButton(rightLabel, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b2.setTextSize(12.2f);
        b2.setOnClickListener(v -> rightAct.run());
        row.addView(b2, new LinearLayout.LayoutParams(0, dp(42), 1f));

        parent.addView(row, rLp);
    }

    // =========================================================================
    // VIBRANT THEME & WALLPAPER CUSTOMIZATION PAGE
    // =========================================================================

    private void showCustomizeThemeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                26, 1));
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎨 Vibrant Themes & Wallpapers");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(17f);
        hdr.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClose = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        hdr.addView(btnClose, new LinearLayout.LayoutParams(dp(38), dp(36)));
        outer.addView(hdr);

        ScrollView sc = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(12), 0, 0);
        sc.addView(body);

        LinearLayout themeCard = createGlassCard();
        themeCard.addView(makeSectionLabel("🌈 VIBRANT LIQUID GLASS PALETTES", getAccentHex()));

        String[][] themes = {
            {"🌊 Electric Sapphire", THEME_SAPPHIRE, "#0072FF", "#00C6FF", "#FFFFFF"},
            {"🖤 AMOLED Neon", THEME_AMOLED, "#0D1117", "#000000", "#00E5FF"},
            {"💜 Neon Cyberpunk", THEME_CYBERPUNK, "#7B2FF7", "#F107A3", "#FFFFFF"},
            {"🌅 Solar Sunset", THEME_SUNSET, "#FF512F", "#F09819", "#FFFFFF"},
            {"💚 Cyber Emerald", THEME_EMERALD, "#00B09B", "#00F5A0", "#052018"},
            {"❄️ Crystal Frost", THEME_FROST, "#E0F2FE", "#BAE6FD", "#0F172A"}
        };

        for (int i = 0; i < themes.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, 0, 0, dp(8));

            for (int j = 0; j < 2 && (i + j) < themes.length; j++) {
                final String label = themes[i + j][0];
                final String key = themes[i + j][1];
                final String c1 = themes[i + j][2];
                final String c2 = themes[i + j][3];
                final String txtCol = themes[i + j][4];
                String prefix = currentTheme.equals(key) ? "✓ " : "";

                Button btn = createLargeGlassButton(prefix + label, c1, c2, txtCol);
                btn.setTextSize(12.2f);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
                if (j == 0) bLp.setMargins(0, 0, dp(8), 0);
                btn.setOnClickListener(v -> {
                    currentTheme = key;
                    prefs.edit().putString("nova_theme", key).apply();
                    applyLiquidGlassTheme();
                    WebView wv = getCurrentWebView();
                    if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                        loadNovaHome(wv);
                    }
                    dialog.dismiss();
                    Toast.makeText(this, "Theme Applied: " + label, Toast.LENGTH_SHORT).show();
                });
                row.addView(btn, bLp);
            }
            themeCard.addView(row, rLp);
        }
        body.addView(themeCard);

        LinearLayout wpCard = createGlassCard();
        wpCard.addView(makeSectionLabel("🖼️ CUSTOM HOMEPAGE WALLPAPER", getAccentHex()));

        TextView tvWpSub = new TextView(this);
        tvWpSub.setText("Pick any photo from your gallery to glow behind the frosted Liquid Glass homepage.");
        tvWpSub.setTextColor(Color.parseColor("#CBD5E1"));
        tvWpSub.setTextSize(12f);
        tvWpSub.setPadding(0, 0, 0, dp(10));
        wpCard.addView(tvWpSub);

        Button btnPickWp = createLargeGlassButton("🖼️ Choose Image from Gallery", "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        pickLp.setMargins(0, 0, 0, dp(8));
        btnPickWp.setOnClickListener(v -> {
            dialog.dismiss();
            wallpaperPickerLauncher.launch("image/*");
        });
        wpCard.addView(btnPickWp, pickLp);

        Button btnClearWp = createLargeGlassButton("🗑️ Reset / Remove Wallpaper", "#33EF4444", "#1AEF4444", "#FCA5A5");
        btnClearWp.setOnClickListener(v -> {
            File wpFile = new File(getFilesDir(), "nova_home_wallpaper.b64");
            if (wpFile.exists()) wpFile.delete();
            Toast.makeText(this, "Wallpaper Reset", Toast.LENGTH_SHORT).show();
            WebView wv = getCurrentWebView();
            if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                loadNovaHome(wv);
            }
            dialog.dismiss();
        });
        wpCard.addView(btnClearWp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        body.addView(wpCard);

        outer.addView(sc);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(12), dp(16), dp(12), dp(16));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.74f);
        }
        dialog.show();
    }

    // =========================================================================
    // CLEANLY ARRANGED LIQUID GLASS AD BLOCKER DASHBOARD
    // =========================================================================

    private void showOrganizedShieldDashboard() {
        WebView wv = getCurrentWebView();
        String currentUrl = (wv != null && wv.getUrl() != null) ? wv.getUrl() : "nova://home";
        String host = Uri.parse(currentUrl).getHost();
        boolean isHttps = currentUrl.startsWith("https://");
        int total = adsBlockedCount + trackersBlockedCount;
        String saved = String.format(Locale.US, "%.2f MB", (total * 18.5) / 1024.0);
        int rulesLoaded = adHostHashes.length + trackerHostHashes.length;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor(isAdBlockEnabled ? "#8800E5FF" : "#88EF4444"),
                26, 1));
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Top Bar
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setPadding(dp(2), 0, dp(2), dp(12));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🛡️ NOVA Shield v5");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(18f);
        hdr.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClose = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        hdr.addView(btnClose, new LinearLayout.LayoutParams(dp(38), dp(36)));
        outer.addView(hdr);

        // Card 1: Status Banner + Master Toggle
        LinearLayout masterCard = createGlassCard();
        TextView tvStatus = new TextView(this);
        tvStatus.setText(isAdBlockEnabled ? "🟢 SHIELD ACTIVE • " + rulesLoaded + " Rules" : "🔴 SHIELD PAUSED");
        tvStatus.setTextColor(Color.parseColor(isAdBlockEnabled ? "#00F5A0" : "#FB7185"));
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextSize(14f);
        masterCard.addView(tvStatus);

        TextView tvSite = new TextView(this);
        tvSite.setText((isHttps ? "🔒 HTTPS Encrypted" : "🌐 Local / HTTP")
                + (host != null && !host.contains("nova.browser") ? " • " + host : " • NOVA Home"));
        tvSite.setTextColor(Color.parseColor("#94A3B8"));
        tvSite.setTextSize(12f);
        tvSite.setPadding(0, dp(4), 0, dp(10));
        masterCard.addView(tvSite);

        Button btnMaster = createLargeGlassButton(
                isAdBlockEnabled ? "⏸ Pause Protection" : "🛡️ Activate Protection",
                isAdBlockEnabled ? "#33EF4444" : "#00B09B",
                isAdBlockEnabled ? "#1AEF4444" : "#00F5A0",
                isAdBlockEnabled ? "#FCA5A5" : "#041E15");
        btnMaster.setOnClickListener(v -> {
            isAdBlockEnabled = !isAdBlockEnabled;
            prefs.edit().putBoolean("adblock_enabled", isAdBlockEnabled).apply();
            if (wv != null) wv.reload();
            dialog.dismiss();
        });
        masterCard.addView(btnMaster, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        outer.addView(masterCard);

        // Card 2: 3-Column Live Metrics
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.setMargins(0, 0, 0, dp(10));
        statsRow.addView(makeMiniStatCard(String.valueOf(adsBlockedCount), "Ads Blocked", "#00E5FF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statsRow.addView(makeMiniStatCard(String.valueOf(trackersBlockedCount), "Trackers", "#FF2E93"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statsRow.addView(makeMiniStatCard(saved, "Data Saved", "#00F5A0"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        outer.addView(statsRow, srLp);

        // Card 3: Organized Protection Modules
        LinearLayout modCard = createGlassCard();
        modCard.addView(makeSectionLabel("⚙️ PROTECTION MODULES", getAccentHex()));

        TextView tvMods = new TextView(this);
        tvMods.setText("• Network Filter: 175k+ Low-RAM Hash DB\n"
                + "• YouTube & Spotify Ad-Shield: ACTIVE\n"
                + "• 3rd-Party Cookies & Trackers: BLOCKED\n"
                + "• 18+ Pop-Up & Interstitial Killer: " + (isAggressiveInterstitialBlock ? "ON" : "OFF"));
        tvMods.setTextColor(Color.parseColor("#CBD5E1"));
        tvMods.setTextSize(12.2f);
        tvMods.setLineSpacing(dp(3), 1f);
        tvMods.setPadding(0, 0, 0, dp(10));
        modCard.addView(tvMods);

        Button btnTogglePop = createLargeGlassButton(
                isAggressiveInterstitialBlock ? "⚡ Pop-Up & Interstitial Killer: ON" : "⚡ Pop-Up & Interstitial Killer: OFF",
                "#2B00D2FF", "#166366F1", "#E0F2FE");
        btnTogglePop.setOnClickListener(v -> {
            isAggressiveInterstitialBlock = !isAggressiveInterstitialBlock;
            prefs.edit().putBoolean("aggressive_interstitial", isAggressiveInterstitialBlock).apply();
            if (wv != null) wv.reload();
            dialog.dismiss();
        });
        modCard.addView(btnTogglePop, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        outer.addView(modCard);

        // Bottom Controls Row
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSync = createLargeGlassButton("🔄 Update Lists", "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        sLp.setMargins(0, 0, dp(8), 0);
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing filter database...", Toast.LENGTH_SHORT).show();
            loadCachedAndSyncFilterLists(true);
            dialog.dismiss();
        });
        bottomRow.addView(btnSync, sLp);

        if (host != null && !host.contains("nova.browser")) {
            boolean allowlisted = allowlistDomains.contains(host);
            Button btnAllow = createLargeGlassButton(
                    allowlisted ? "Untrust Site" : "Trust Site",
                    "#33FFD000", "#1AFFD000", "#FFE566");
            btnAllow.setOnClickListener(v -> {
                if (allowlisted) allowlistDomains.remove(host);
                else allowlistDomains.add(host);
                prefs.edit().putStringSet("allowlist", allowlistDomains).apply();
                if (wv != null) wv.reload();
                dialog.dismiss();
            });
            bottomRow.addView(btnAllow, new LinearLayout.LayoutParams(0, dp(42), 1f));
        }
        outer.addView(bottomRow);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(12), dp(16), dp(12), dp(16));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.74f);
        }
        dialog.show();
    }

    private LinearLayout makeMiniStatCard(String value, String label, String colorHex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(10), dp(8), dp(10));
        box.setBackground(makeGlassDrawable(
                Color.parseColor("#B8162544"),
                Color.parseColor("#B80B1426"),
                Color.parseColor("#44FFFFFF"),
                14, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), 0, dp(3), 0);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(Color.parseColor(colorHex));
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setTextSize(15f);
        tvVal.setGravity(Gravity.CENTER);
        box.addView(tvVal);

        TextView tvLbl = new TextView(this);
        tvLbl.setText(label);
        tvLbl.setTextColor(Color.parseColor("#CBD5E1"));
        tvLbl.setTextSize(10.5f);
        tvLbl.setGravity(Gravity.CENTER);
        box.addView(tvLbl);

        LinearLayout holder = new LinearLayout(this);
        holder.addView(box, lp);
        return holder;
    }

    // =========================================================================
    // WORKING 3-ENGINE TEMP MAIL (GUERRILLA MAIL + MAIL.GW + MAIL.TM FAILOVER)
    // =========================================================================

    private void showTempMailDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                26, 1));
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("📧 NOVA Temp Mail");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(18f);
        hdr.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClose = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        hdr.addView(btnClose, new LinearLayout.LayoutParams(dp(38), dp(36)));
        outer.addView(hdr);

        LinearLayout addrCard = createGlassCard();
        TextView tvAddrLbl = new TextView(this);
        String prov = prefs.getString("tm_provider", "guerrilla");
        tvAddrLbl.setText("Unique Disposable Address (Auto-Failover Engine):");
        tvAddrLbl.setTextColor(Color.parseColor("#94A3B8"));
        tvAddrLbl.setTextSize(11.5f);
        addrCard.addView(tvAddrLbl);

        TextView tvEmail = new TextView(this);
        String savedEmail = prefs.getString("tm_addr", "");
        tvEmail.setText(savedEmail.isEmpty() ? "Generating unique address..." : savedEmail);
        tvEmail.setTextColor(Color.parseColor(getAccentHex()));
        tvEmail.setTypeface(null, Typeface.BOLD);
        tvEmail.setTextSize(14.5f);
        tvEmail.setPadding(0, dp(4), 0, dp(10));
        addrCard.addView(tvEmail);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCopy = createLargeGlassButton("📋 Copy Email", "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        cLp.setMargins(0, 0, dp(8), 0);
        btnCopy.setOnClickListener(v -> {
            String em = prefs.getString("tm_addr", "");
            if (!em.isEmpty()) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("NOVA Temp Mail", em));
                    Toast.makeText(this, "Copied: " + em, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnRow.addView(btnCopy, cLp);

        Button btnNew = createLargeGlassButton("🔄 New Email", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnRow.addView(btnNew, new LinearLayout.LayoutParams(0, dp(40), 1f));
        addrCard.addView(btnRow);
        outer.addView(addrCard);

        LinearLayout inboxHdr = new LinearLayout(this);
        inboxHdr.setOrientation(LinearLayout.HORIZONTAL);
        inboxHdr.setGravity(Gravity.CENTER_VERTICAL);
        inboxHdr.setPadding(dp(4), dp(4), dp(4), dp(8));

        TextView tvInboxLbl = new TextView(this);
        tvInboxLbl.setText("📥 Live Inbox (" + prov.toUpperCase(Locale.US) + ")");
        tvInboxLbl.setTextColor(Color.parseColor("#FFFFFF"));
        tvInboxLbl.setTypeface(null, Typeface.BOLD);
        tvInboxLbl.setTextSize(13.5f);
        inboxHdr.addView(tvInboxLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnRefresh = createLargeGlassButton("↻ Refresh", "#2B00D2FF", "#166366F1", "#E0F2FE");
        inboxHdr.addView(btnRefresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        outer.addView(inboxHdr);

        ScrollView scInbox = new ScrollView(this);
        LinearLayout inboxContainer = new LinearLayout(this);
        inboxContainer.setOrientation(LinearLayout.VERTICAL);
        scInbox.addView(inboxContainer);
        outer.addView(scInbox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Runnable refreshAction = () -> fetchTempMailInbox(inboxContainer);
        btnRefresh.setOnClickListener(v -> refreshAction.run());
        btnNew.setOnClickListener(v -> createFreshTempMailAccount(tvEmail, tvInboxLbl, inboxContainer));

        if (savedEmail.isEmpty() || prefs.getString("tm_token", "").isEmpty()) {
            createFreshTempMailAccount(tvEmail, tvInboxLbl, inboxContainer);
        } else {
            refreshAction.run();
        }

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(10), dp(18), dp(10), dp(18));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.80f);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxH);
            dialog.getWindow().setDimAmount(0.74f);
        }
        dialog.show();
    }

    private String httpMailApi(String urlStr, String method, String jsonBody, String bearerToken, boolean ldJson) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", ldJson ? "application/ld+json" : "application/json");
        if (bearerToken != null && !bearerToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        if (jsonBody != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        conn.disconnect();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
        return sb.toString();
    }

    private void createFreshTempMailAccount(TextView tvEmail, TextView tvInboxLbl, LinearLayout inboxContainer) {
        uiHandler.post(() -> {
            tvEmail.setText("Connecting to Temp Mail servers...");
            inboxContainer.removeAllViews();
        });
        new Thread(() -> {
            // Engine 1: Guerrilla Mail (Fast, Reliable, Zero-Signup)
            try {
                String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_address&lang=en&ip=127.0.0.1&agent=NOVA_Browser", "GET", null, null, false);
                JSONObject obj = new JSONObject(res);
                String email = obj.getString("email_addr");
                String sid = obj.getString("sid_token");
                prefs.edit()
                    .putString("tm_provider", "guerrilla")
                    .putString("tm_addr", email)
                    .putString("tm_token", sid)
                    .apply();
                uiHandler.post(() -> {
                    tvEmail.setText(email);
                    tvInboxLbl.setText("📥 Live Inbox (GUERRILLA)");
                    Toast.makeText(this, "📧 Temp Mail Ready!", Toast.LENGTH_SHORT).show();
                    fetchTempMailInbox(inboxContainer);
                });
                return;
            } catch (Exception ignored) {}

            // Engine 2 & 3: Mail.gw and Mail.tm Failover (Fixed ld+json & User-Agent)
            String[] bases = {"https://api.mail.gw", "https://api.mail.tm"};
            String[] names = {"mailgw", "mailtm"};
            for (int i = 0; i < bases.length; i++) {
                try {
                    String domStr = httpMailApi(bases[i] + "/domains", "GET", null, null, true).trim();
                    JSONArray arr = domStr.startsWith("[")
                            ? new JSONArray(domStr)
                            : new JSONObject(domStr).getJSONArray("hydra:member");
                    String domain = arr.getJSONObject(0).getString("domain");

                    String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
                    Random rnd = new Random();
                    StringBuilder user = new StringBuilder("nova");
                    for (int k = 0; k < 8; k++) user.append(chars.charAt(rnd.nextInt(chars.length())));
                    String email = user + "@" + domain;
                    String pass = "NovaPass" + (100000 + rnd.nextInt(899999));

                    JSONObject creds = new JSONObject();
                    creds.put("address", email);
                    creds.put("password", pass);

                    httpMailApi(bases[i] + "/accounts", "POST", creds.toString(), null, true);
                    String tokJson = httpMailApi(bases[i] + "/token", "POST", creds.toString(), null, true);
                    String token = new JSONObject(tokJson).getString("token");

                    final String provName = names[i];
                    prefs.edit()
                        .putString("tm_provider", provName)
                        .putString("tm_addr", email)
                        .putString("tm_token", token)
                        .apply();
                    uiHandler.post(() -> {
                        tvEmail.setText(email);
                        tvInboxLbl.setText("📥 Live Inbox (" + provName.toUpperCase(Locale.US) + ")");
                        Toast.makeText(this, "📧 Temp Mail Ready!", Toast.LENGTH_SHORT).show();
                        fetchTempMailInbox(inboxContainer);
                    });
                    return;
                } catch (Exception ignored) {}
            }

            uiHandler.post(() -> {
                tvEmail.setText("No internet / Servers unreachable");
                Toast.makeText(this, "Check internet connection and tap New Email", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void fetchTempMailInbox(LinearLayout inboxContainer) {
        String provider = prefs.getString("tm_provider", "guerrilla");
        String token = prefs.getString("tm_token", "");
        if (token.isEmpty()) return;

        uiHandler.post(() -> {
            inboxContainer.removeAllViews();
            TextView tvLoad = new TextView(this);
            tvLoad.setText("Refreshing inbox...");
            tvLoad.setTextColor(Color.parseColor("#94A3B8"));
            tvLoad.setPadding(dp(8), dp(12), dp(8), dp(12));
            inboxContainer.addView(tvLoad);
        });

        new Thread(() -> {
            try {
                final List<String[]> items = new ArrayList<>(); // [id, subject, from, excerpt]
                if ("guerrilla".equals(provider)) {
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_list&offset=0&sid_token=" + Uri.encode(token), "GET", null, null, false);
                    JSONArray list = new JSONObject(res).getJSONArray("list");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject m = list.getJSONObject(i);
                        items.add(new String[]{
                            m.optString("mail_id"),
                            m.optString("mail_subject", "(No Subject)"),
                            m.optString("mail_from", "Sender"),
                            m.optString("mail_excerpt", "")
                        });
                    }
                } else {
                    String base = "mailgw".equals(provider) ? "https://api.mail.gw" : "https://api.mail.tm";
                    String res = httpMailApi(base + "/messages", "GET", null, token, true).trim();
                    JSONArray msgs = res.startsWith("[")
                            ? new JSONArray(res)
                            : new JSONObject(res).getJSONArray("hydra:member");
                    for (int i = 0; i < msgs.length(); i++) {
                        JSONObject m = msgs.getJSONObject(i);
                        String from = m.has("from") ? m.getJSONObject("from").optString("address", "Sender") : "Sender";
                        items.add(new String[]{
                            m.getString("id"),
                            m.optString("subject", "(No Subject)"),
                            from,
                            m.optString("intro", "")
                        });
                    }
                }

                uiHandler.post(() -> {
                    inboxContainer.removeAllViews();
                    if (items.isEmpty()) {
                        LinearLayout empty = createGlassCard();
                        TextView tvE = new TextView(this);
                        tvE.setText("Inbox is empty.\nPaste your email into any signup form, then tap ↻ Refresh to view OTPs!");
                        tvE.setTextColor(Color.parseColor("#94A3B8"));
                        tvE.setTextSize(12.5f);
                        empty.addView(tvE);
                        inboxContainer.addView(empty);
                        return;
                    }
                    for (String[] m : items) {
                        LinearLayout card = createGlassCard();
                        TextView tvS = new TextView(this);
                        tvS.setText("✉️ " + m[1]);
                        tvS.setTextColor(Color.parseColor("#FFFFFF"));
                        tvS.setTypeface(null, Typeface.BOLD);
                        tvS.setTextSize(13.5f);
                        card.addView(tvS);

                        TextView tvF = new TextView(this);
                        tvF.setText("From: " + m[2] + (m[3].isEmpty() ? "" : "\n" + m[3]));
                        tvF.setTextColor(Color.parseColor("#CBD5E1"));
                        tvF.setTextSize(11.5f);
                        tvF.setPadding(0, dp(4), 0, 0);
                        card.addView(tvF);

                        card.setOnClickListener(v -> openTempMailMessage(provider, m[0], m[1], m[2], token));
                        inboxContainer.addView(card);
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "Tap 🔄 New Email to start a fresh session", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void openTempMailMessage(String provider, String msgId, String subject, String from, String token) {
        new Thread(() -> {
            try {
                String bodyText;
                if ("guerrilla".equals(provider)) {
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=" + Uri.encode(msgId) + "&sid_token=" + Uri.encode(token), "GET", null, null, false);
                    String rawHtml = new JSONObject(res).optString("mail_body", "");
                    bodyText = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            ? Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                            : Html.fromHtml(rawHtml).toString();
                } else {
                    String base = "mailgw".equals(provider) ? "https://api.mail.gw" : "https://api.mail.tm";
                    String res = httpMailApi(base + "/messages/" + msgId, "GET", null, token, true);
                    bodyText = new JSONObject(res).optString("text", "(Empty message)");
                }
                final String finalBody = bodyText.trim();
                uiHandler.post(() -> new AlertDialog.Builder(this)
                    .setTitle(subject)
                    .setMessage("From: " + from + "\n\n" + finalBody)
                    .setPositiveButton("📋 Copy Text", (d, w) -> {
                        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cb != null) {
                            cb.setPrimaryClip(ClipData.newPlainText("Temp Mail", finalBody));
                            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show());
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "Could not load message body", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // LOGIN / SIGN-UP WALL BYPASSER
    // =========================================================================

    private void showLoginBypassDialog(WebView wv) {
        if (wv == null || wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser")) {
            Toast.makeText(this, "Open a website first to bypass its login wall", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] methods = {
            "🔓 Strip Login / Sign-Up Modals & Blur (Instant)",
            "🤖 Reload as Search Crawler (Bypass Hard Login/Paywall)",
            "📖 Extract Clean Text (Reader Bypass)"
        };
        new AlertDialog.Builder(this)
            .setTitle("🔓 NOVA Login & Sign-Up Wall Bypass")
            .setItems(methods, (d, which) -> {
                if (which == 0) {
                    String jsBypass = "javascript:(function(){"
                        + "document.querySelectorAll('*').forEach(function(el){"
                        + "var st=window.getComputedStyle(el);"
                        + "var idc=((el.id||'')+' '+(el.className||'')).toLowerCase();"
                        + "var isWall=/login|signin|sign-in|signup|sign-up|register|paywall|auth-modal|gate|subscribe-modal/.test(idc);"
                        + "if((st.position==='fixed'||st.position==='sticky')&&(isWall||parseInt(st.zIndex||0)>500)){"
                        + "if(el.tagName!=='HEADER'&&el.tagName!=='NAV'){el.remove();}}"
                        + "if(st.filter&&st.filter.indexOf('blur')!==-1){el.style.filter='none';}"
                        + "});"
                        + "document.documentElement.style.setProperty('overflow','auto','important');"
                        + "document.body.style.setProperty('overflow','auto','important');"
                        + "document.body.style.setProperty('position','static','important');"
                        + "})();";
                    wv.evaluateJavascript(jsBypass, null);
                    Toast.makeText(this, "🔓 Login modals & scroll locks removed!", Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    wv.getSettings().setUserAgentString("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
                    wv.reload();
                    Toast.makeText(this, "🤖 Reloading as Crawler...", Toast.LENGTH_SHORT).show();
                } else {
                    toggleReaderMode(wv);
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    // =========================================================================
    // TAB ENGINE, SHIELD JS INJECTION & VIBRANT HOMEPAGE
    // =========================================================================

    private void updateDownloadDockBadge() {
        int activeCount = DownloadService.getActiveCount();
        uiHandler.post(() -> {
            if (activeCount > 0) {
                btnDownloads.setText("⬇" + activeCount);
                btnDownloads.setTextSize(13f);
                btnDownloads.setTextColor(Color.parseColor("#00F5A0"));
            } else {
                btnDownloads.setText("⬇");
                btnDownloads.setTextSize(17f);
                btnDownloads.setTextColor(Color.parseColor(getAccentHex()));
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebView wv = getCurrentWebView();
        if (wv != null) {
            wv.onPause();
            if (isBatterySaver) wv.pauseTimers();
        }
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
        updateDownloadDockBadge();
        enforceLowRamBudget();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            for (int i = 0; i < tabs.size(); i++) {
                if (i != currentTabIndex) tabs.get(i).clearCache(false);
            }
            System.gc();
        }
    }

    private void injectNovaShieldHook(WebView view) {
        if (!isAdBlockEnabled) return;
        String aggFlag = isAggressiveInterstitialBlock ? "true" : "false";
        String jsHook = "javascript:(function(){"
            + "if(window.__novaShieldV6)return;window.__novaShieldV6=true;"
            + "function checkBlocked(u){"
            + "try{if(!u)return false;const s=u.toString();"
            + "if(window.NovaShield&&window.NovaShield.isBlocked(s))return true;}catch(e){}return false;}"
            + "const origFetch=window.fetch;"
            + "window.fetch=function(input,init){"
            + "const u=(typeof input==='string')?input:(input&&input.url?input.url:'');"
            + "if(checkBlocked(u))return Promise.reject(new TypeError('Blocked by NOVA Shield'));"
            + "return origFetch.apply(this,arguments).then(function(resp){"
            + "try{if(u.indexOf('youtubei/v1/player')!==-1||u.indexOf('youtubei/v1/next')!==-1){"
            + "const clone=resp.clone();return clone.text().then(function(txt){"
            + "try{let j=JSON.parse(txt);delete j.adPlacements;delete j.playerAds;delete j.adSlots;delete j.adBreakHeartbeatParams;"
            + "return new Response(JSON.stringify(j),{status:resp.status,statusText:resp.statusText,headers:resp.headers});}"
            + "catch(e){return resp;}});}}catch(e){}return resp;});};"
            + "const origOpen=XMLHttpRequest.prototype.open;"
            + "XMLHttpRequest.prototype.open=function(m,u){"
            + "if(checkBlocked(u)){this.__blocked=true;}return origOpen.apply(this,arguments);};"
            + "const origSend=XMLHttpRequest.prototype.send;"
            + "XMLHttpRequest.prototype.send=function(){"
            + "if(this.__blocked){this.dispatchEvent(new Event('error'));return;}"
            + "return origSend.apply(this,arguments);};"
            + "const origWinOpen=window.open;"
            + "window.open=function(u){if(!u||checkBlocked(u))return null;return origWinOpen.apply(this,arguments);};"
            + "const origParse=JSON.parse;"
            + "JSON.parse=function(t){let r=origParse.apply(this,arguments);"
            + "if(r&&typeof r==='object'&&(r.adPlacements||r.playerAds||r.adSlots)){"
            + "delete r.adPlacements;delete r.playerAds;delete r.adSlots;}return r;};"
            + "var st=document.createElement('style');"
            + "st.innerHTML='ins.adsbygoogle,.adsbygoogle,[id*=\"google_ads\"],[id*=\"div-gpt-ad\"],[class*=\"ad-banner\"],"
            + "[class*=\"banner-ad\"],[class*=\"ad-container\"],[class*=\"ad-wrapper\"],[class*=\"ad-slot\"],"
            + "[class*=\"ad-overlay\"],[class*=\"ad_overlay\"],[class*=\"interstitial\"],[id*=\"interstitial\"],"
            + "[class*=\"popunder\"],[class*=\"sponsored-ad\"],[class*=\"cookie-banner\"],[id*=\"cookie-consent\"],"
            + "iframe[src*=\"ads\"],iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"],"
            + "ytm-promoted-sparkles-web-renderer,ytm-ad-slot-renderer,ytd-ad-slot-renderer,"
            + "ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,ytm-companion-ad-renderer,"
            + "#masthead-ad,#player-ads,.ytp-ad-overlay-container,.ytp-ad-text-overlay,"
            + "[data-testid=\"ad-slot\"],[data-testid=\"in-app-message\"]{display:none!important;visibility:hidden!important;height:0!important;}';"
            + "if(document.head)document.head.appendChild(st);"
            + "if(" + aggFlag + "){"
            + "function purgeInterstitials(){"
            + "try{var nodes=document.querySelectorAll('div,section,aside,iframe');"
            + "for(var i=0;i<nodes.length;i++){"
            + "var el=nodes[i];if(!el||el===document.body||el===document.documentElement)continue;"
            + "var cs=window.getComputedStyle(el);"
            + "var idc=((el.id||'')+' '+(el.className||'')).toLowerCase();"
            + "var txt=(el.innerText||'').trim();"
            + "var isFixed=(cs.position==='fixed'||cs.position==='absolute')&&parseInt(cs.zIndex||0)>=10;"
            + "var hasAdWord=/close ad|are you 18\\+|confirm.*18\\+|advertisement|sponsored/i.test(txt)&&txt.length<260;"
            + "var hasAdClass=/interstitial|popunder|ad-overlay|ad_overlay|ad-modal|ad-popup|vignette|banner-ad|adframe/.test(idc);"
            + "if((isFixed&&hasAdWord)||hasAdClass){"
            + "el.remove();if(window.NovaShield)window.NovaShield.reportDomAdRemoved();"
            + "document.body.style.overflow='auto';document.documentElement.style.overflow='auto';}"
            + "}}catch(e){}}"
            + "purgeInterstitials();"
            + "new MutationObserver(purgeInterstitials).observe(document.documentElement,{childList:true,subtree:true});"
            + "setInterval(purgeInterstitials,700);}"
            + "const h=location.hostname;"
            + "if(h.indexOf('youtube.com')!==-1){"
            + "setInterval(function(){"
            + "try{const p=document.querySelector('.html5-video-player');"
            + "const v=document.querySelector('video');"
            + "const skip=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-slot button');"
            + "if(skip)skip.click();"
            + "if(p&&(p.classList.contains('ad-showing')||p.classList.contains('ad-interrupting'))&&v){"
            + "v.muted=true;if(isFinite(v.duration)&&v.duration>0){v.currentTime=v.duration;}else{v.playbackRate=16;}}"
            + "}catch(e){}},300);}"
            + "if(h.indexOf('spotify.com')!==-1){"
            + "setInterval(function(){"
            + "try{const titleEl=document.querySelector('[data-testid=\"context-item-info-title\"],[data-testid=\"now-playing-widget\"]');"
            + "const isAd=titleEl&&/advertisement|spotify\\s*free/i.test(titleEl.textContent||'');"
            + "document.querySelectorAll('audio,video').forEach(function(m){"
            + "if(isAd){m.muted=true;if(isFinite(m.duration)&&m.duration>0&&m.duration<65){m.currentTime=m.duration;}}"
            + "else if(m.muted&&m.__novaMuted){m.muted=false;m.__novaMuted=false;}"
            + "if(isAd)m.__novaMuted=true;});}catch(e){}},450);}"
            + "})();";
        view.evaluateJavascript(jsHook, null);
    }

    private WebView getCurrentWebView() {
        if (tabs.isEmpty() || currentTabIndex < 0 || currentTabIndex >= tabs.size()) return null;
        return tabs.get(currentTabIndex);
    }

    private void createNewTab(String initialUrl) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setupWebView(webView);
        tabs.add(webView);
        switchToTab(tabs.size() - 1);

        if ("nova://home".equals(initialUrl)) loadNovaHome(webView);
        else webView.loadUrl(initialUrl);
        enforceLowRamBudget();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        for (int i = 0; i < tabs.size(); i++) {
            WebView tab = tabs.get(i);
            if (i == index) {
                tab.onResume();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tab.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
                }
            } else {
                tab.onPause();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tab.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, true);
                }
            }
        }
        currentTabIndex = index;
        webContainer.removeAllViews();
        WebView active = tabs.get(currentTabIndex);
        webContainer.addView(active);
        btnTabs.setText("[" + tabs.size() + "]");

        String url = active.getUrl();
        if (url == null || url.startsWith("https://nova.browser")) {
            etUrl.setText("");
            etUrl.setHint("Search " + searchEngineName + " or type URL...");
            animateBarsToDp(56);
        } else {
            etUrl.setText(url);
            animateBarsToDp(44);
        }
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        WebView removed = tabs.remove(index);
        String lastUrl = removed.getUrl();
        if (lastUrl != null && !lastUrl.startsWith("https://nova.browser")) {
            closedTabs.add(0, lastUrl);
        }
        removed.stopLoading();
        removed.clearHistory();
        removed.removeAllViews();
        removed.destroy();

        if (tabs.isEmpty()) createNewTab("nova://home");
        else switchToTab(Math.min(index, tabs.size() - 1));
        enforceLowRamBudget();
    }

    private void loadNovaHome(WebView webView) {
        int totalBlocked = adsBlockedCount + trackersBlockedCount;
        int rulesCount = adHostHashes.length + trackerHostHashes.length;
        String dataSavedKb = String.format(Locale.US, "%.1f KB", totalBlocked * 18.5);
        String accent = getAccentHex();
        String accent2 = getSecondaryAccentHex();
        boolean isLight = THEME_FROST.equals(currentTheme);

        String customWpB64 = readCustomWallpaperBase64();
        String bgCss;
        if (customWpB64 != null && !customWpB64.isEmpty()) {
            bgCss = "linear-gradient(rgba(5,10,22,0.48),rgba(5,10,22,0.65)),url('data:image/jpeg;base64," + customWpB64 + "') center/cover no-repeat fixed";
        } else if (THEME_AMOLED.equals(currentTheme)) {
            bgCss = "#000000";
        } else if (THEME_CYBERPUNK.equals(currentTheme)) {
            bgCss = "radial-gradient(circle at 18% 12%, #6D28D9 0%, #26074F 48%, #0E021F 100%)";
        } else if (THEME_SUNSET.equals(currentTheme)) {
            bgCss = "radial-gradient(circle at 18% 12%, #C2410C 0%, #431407 48%, #160502 100%)";
        } else if (THEME_EMERALD.equals(currentTheme)) {
            bgCss = "radial-gradient(circle at 18% 12%, #059669 0%, #042F25 48%, #01140F 100%)";
        } else if (isLight) {
            bgCss = "radial-gradient(circle at 20% 15%, #BAE6FD 0%, #E0F2FE 55%, #F0F9FF 100%)";
        } else {
            bgCss = "radial-gradient(circle at 18% 12%, #1D4ED8 0%, #0B1F54 48%, #050C24 100%)";
        }

        String glass = (isLight && customWpB64 == null) ? "rgba(255,255,255,0.72)" : "rgba(255,255,255,0.10)";
        String border = (isLight && customWpB64 == null) ? "rgba(0,102,255,0.25)" : "rgba(255,255,255,0.24)";
        String text = (isLight && customWpB64 == null) ? "#0F172A" : "#FFFFFF";
        String sub = (isLight && customWpB64 == null) ? "#475569" : "#CBD5E1";

        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "body{margin:0;min-height:100vh;padding:24px 14px 28px;font-family:-apple-system,sans-serif;background:" + bgCss + ";color:" + text + ";text-align:center;box-sizing:border-box;}"
            + ".logo{font-size:34px;font-weight:900;letter-spacing:2.5px;background:linear-gradient(120deg," + accent + "," + accent2 + ");-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-top:8px;}"
            + ".tag{font-size:12px;color:" + sub + ";margin:6px 0 20px;}"
            + ".glass{background:" + glass + ";backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);border:1px solid " + border + ";box-shadow:0 8px 28px rgba(0,0,0,0.30);}"
            + ".search-box{display:flex;border-radius:26px;padding:6px 12px;margin:0 auto 20px;max-width:480px;}"
            + ".search-box input{flex:1;background:transparent;border:none;color:" + text + ";font-size:15px;outline:none;padding:8px;}"
            + ".search-box button{background:linear-gradient(135deg," + accent + "," + accent2 + ");color:#FFFFFF;border:none;border-radius:18px;padding:8px 18px;font-weight:800;font-size:13px;}"
            + ".stats{display:flex;justify-content:space-around;border-radius:20px;padding:14px 6px;margin:0 auto 20px;max-width:480px;}"
            + ".stat-num{font-size:17px;font-weight:800;color:" + accent + ";}"
            + ".stat-lbl{font-size:10px;color:" + sub + ";margin-top:3px;}"
            + ".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;max-width:480px;margin:0 auto 22px;}"
            + ".tile{padding:14px 6px;border-radius:18px;text-decoration:none;color:" + text + ";font-size:11.5px;font-weight:700;display:block;}"
            + ".icon{font-size:22px;display:block;margin-bottom:5px;}"
            + ".quick-row{display:flex;gap:10px;max-width:480px;margin:0 auto;}"
            + ".qbtn{flex:1;padding:12px;border-radius:18px;color:" + text + ";font-size:12.5px;font-weight:700;border:none;}"
            + "</style></head><body>"
            + "<div class='logo'>NOVA BROWSER</div>"
            + "<div class='tag'>v4.0 Low-RAM &bull; " + rulesCount + " Shield Rules &bull; 4x Turbo DL</div>"
            + "<form class='search-box glass' onsubmit=\"window.location.href='" + searchEngineUrl + "'+encodeURIComponent(document.getElementById('q').value);return false;\">"
            + "<input id='q' type='text' placeholder='Search with " + searchEngineName + " or enter URL...'/>"
            + "<button type='submit'>GO</button></form>"
            + "<div class='stats glass'>"
            + "<div><div class='stat-num'>" + adsBlockedCount + "</div><div class='stat-lbl'>Ads Blocked</div></div>"
            + "<div><div class='stat-num'>" + trackersBlockedCount + "</div><div class='stat-lbl'>Trackers</div></div>"
            + "<div><div class='stat-num'>" + dataSavedKb + "</div><div class='stat-lbl'>Data Saved</div></div>"
            + "<div><div class='stat-num'>" + getRamUsageMb() + "</div><div class='stat-lbl'>RAM Used</div></div>"
            + "</div>"
            + "<div class='grid'>"
            + "<a class='tile glass' href='https://www.google.com'><span class='icon'>🔍</span>Google</a>"
            + "<a class='tile glass' href='https://m.youtube.com'><span class='icon'>▶️</span>YouTube</a>"
            + "<a class='tile glass' href='https://open.spotify.com'><span class='icon'>🎵</span>Spotify</a>"
            + "<a class='tile glass' href='https://adblock.turtlecute.org'><span class='icon'>🛡️</span>AdTest</a>"
            + "<a class='tile glass' href='https://github.com/shahalamlaskarofficial'><span class='icon'>💻</span>GitHub</a>"
            + "<a class='tile glass' href='https://chatgpt.com'><span class='icon'>🤖</span>ChatGPT</a>"
            + "<a class='tile glass' href='https://www.reddit.com'><span class='icon'>🔥</span>Reddit</a>"
            + "<a class='tile glass' href='https://fast.com'><span class='icon'>⚡</span>Speed</a>"
            + "</div>"
            + "<div class='quick-row'>"
            + "<button class='qbtn glass' onclick='window.NovaShield.openCustomizePage()'>🎨 Theme &amp; Wallpaper</button>"
            + "<button class='qbtn glass' onclick='window.NovaShield.openTempMailPage()'>📧 Temp Mail</button>"
            + "</div>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://nova.browser", html, "text/html", "UTF-8", null);
        etUrl.setText("");
        etUrl.setHint("Search " + searchEngineName + " or type URL...");
        animateBarsToDp(56);
    }

    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBlockNetworkImage(isDataSaver);
        settings.setTextZoom(textZoomPercent);
        settings.setCacheMode(isIncognito ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);

        if (defaultUserAgent == null) defaultUserAgent = settings.getUserAgentString();

        webView.addJavascriptInterface(new NovaShieldBridge(), "NovaShield");
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                String currentUrl = webView.getUrl();
                if (currentUrl != null && currentUrl.startsWith("https://nova.browser")) {
                    animateBarsToDp(56);
                    return;
                }
                int dy = scrollY - oldScrollY;
                if (scrollY <= dp(10)) animateBarsToDp(44);
                else if (dy > dp(18)) animateBarsToDp(0);
                else if (dy < -dp(14)) animateBarsToDp(44);
            });
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("mailto:")) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(Intent.createChooser(emailIntent, "Contact Developer"));
                    return true;
                }
                if (isAdBlockEnabled && classifyBlockedRequest(request.getUrl().getHost(), url) != 0) {
                    adsBlockedCount++;
                    return true;
                }
                if (isHttpsFirst && url.startsWith("http://") && !url.contains("localhost") && !url.contains("127.0.0.1")) {
                    view.loadUrl(url.replaceFirst("http://", "https://"));
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectNovaShieldHook(view);
                if (view == getCurrentWebView()) {
                    if (url.startsWith("https://nova.browser")) {
                        etUrl.setText("");
                        animateBarsToDp(56);
                    } else {
                        String lock = url.startsWith("https://") ? "🔒" : "⚠️";
                        etUrl.setText(url);
                        btnShield.setText(lock);
                        animateBarsToDp(44);
                    }
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectNovaShieldHook(view);
                if (view == getCurrentWebView()) {
                    progressBar.setVisibility(View.GONE);
                    btnShield.setText("🛡️");
                }
                if (!isIncognito && url != null && !url.startsWith("https://nova.browser")) {
                    saveToHistory(view.getTitle(), url);
                }
                enforceLowRamBudget();
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!isAdBlockEnabled || request.isForMainFrame()) {
                    return super.shouldInterceptRequest(view, request);
                }
                Uri uri = request.getUrl();
                int kind = classifyBlockedRequest(uri.getHost(), uri.toString());
                if (kind == 1) {
                    adsBlockedCount++;
                    return createBlockedResponse();
                } else if (kind == 2) {
                    trackersBlockedCount++;
                    return createBlockedResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress > 15 && newProgress < 95) injectNovaShieldHook(view);
                if (view == getCurrentWebView()) progressBar.setProgress(newProgress);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                List<String> granted = new ArrayList<>();
                for (String res : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(res)) granted.add(res);
                }
                if (!granted.isEmpty()) request.grant(granted.toArray(new String[0]));
                else request.deny();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            promptNewTurboDownload(url, guessedName, contentLength);
        });
    }

    // =========================================================================
    // FOREGROUND TURBO DOWNLOAD MANAGER UI (4x PARALLEL + NEVER STOPS ON BACK)
    // =========================================================================

    private LinearLayout createGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(makeGlassDrawable(
                Color.parseColor("#CC142442"),
                Color.parseColor("#CC0B152B"),
                Color.parseColor("#4DFFFFFF"),
                18, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        return card;
    }

    private Button createLargeGlassButton(String text, String topHex, String botHex, String textHex) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13f);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.parseColor(textHex));
        b.setBackground(makeGlassDrawable(
                Color.parseColor(topHex),
                Color.parseColor(botHex),
                Color.parseColor("#55FFFFFF"),
                14, 1));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        return b;
    }

    private void promptNewTurboDownload(String url, String suggestedName, long contentLength) {
        boolean isHttps = url.toLowerCase(Locale.US).startsWith("https://");
        String sizeText = contentLength > 0 ? DownloadService.formatBytes(contentLength) : "Auto-detect on start";

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(makeGlassDrawable(
                Color.parseColor("#F50C1730"),
                Color.parseColor("#F5060D1C"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                24, 1));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ NOVA 4x Turbo Downloader");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(17f);
        card.addView(tvTitle);

        TextView tvSec = new TextView(this);
        tvSec.setText(isHttps
                ? "🔒 HTTPS Stream • Foreground Service (Never Stops)"
                : "⚠️ HTTP Stream • Foreground Service (Never Stops)");
        tvSec.setTextColor(Color.parseColor(isHttps ? "#00F5A0" : "#FFD000"));
        tvSec.setTextSize(12f);
        tvSec.setPadding(0, dp(8), 0, dp(4));
        card.addView(tvSec);

        TextView tvMeta = new TextView(this);
        tvMeta.setText("Size: " + sizeText + " • 4x Parallel 128KB Engine → Downloads/NOVA Browser");
        tvMeta.setTextColor(Color.parseColor("#94A3B8"));
        tvMeta.setTextSize(11.5f);
        tvMeta.setPadding(0, 0, 0, dp(12));
        card.addView(tvMeta);

        EditText etName = new EditText(this);
        etName.setText(suggestedName);
        etName.setHint("File name...");
        etName.setTextColor(Color.parseColor("#FFFFFF"));
        etName.setHintTextColor(Color.parseColor("#64748B"));
        etName.setBackground(makeGlassDrawable(
                Color.parseColor("#AA060D1E"),
                Color.parseColor("#AA122140"),
                Color.parseColor("#6600D2FF"),
                14, 1));
        etName.setPadding(dp(12), dp(10), dp(12), dp(10));
        etName.setSingleLine(true);
        card.addView(etName);

        Button btnStart = createLargeGlassButton("🚀 Start 4x Turbo Download", "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        startLp.setMargins(0, dp(14), 0, dp(8));
        btnStart.setOnClickListener(v -> {
            String finalName = etName.getText().toString().trim();
            if (finalName.isEmpty()) finalName = "nova_file_" + System.currentTimeMillis();
            DownloadService.startNewDownload(this, url, finalName, defaultUserAgent);
            updateDownloadDockBadge();
            dialog.dismiss();
            showTurboDownloadManager();
        });
        card.addView(btnStart, startLp);

        Button btnClose = createLargeGlassButton("Cancel", "#33EF4444", "#1AEF4444", "#FCA5A5");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(14), dp(14), dp(14), dp(14));
        wrap.addView(card);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.72f);
        }
        dialog.show();
    }

    private void showTurboDownloadManager() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outerGlass = new LinearLayout(this);
        outerGlass.setOrientation(LinearLayout.VERTICAL);
        outerGlass.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                26, 1));
        outerGlass.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(4), 0, dp(4), dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ NOVA 4x Turbo Downloads");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(18f);
        headerRow.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnMin = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnMin.setOnClickListener(v -> dialog.dismiss());
        headerRow.addView(btnMin, new LinearLayout.LayoutParams(dp(38), dp(36)));
        outerGlass.addView(headerRow);

        ScrollView scrollRoot = new ScrollView(this);
        scrollRoot.setVerticalScrollBarEnabled(false);
        LinearLayout stackContainer = new LinearLayout(this);
        stackContainer.setOrientation(LinearLayout.VERTICAL);
        scrollRoot.addView(stackContainer);

        LinearLayout statsCard = createGlassCard();
        long freeSpaceMb = DownloadService.getStagingDir(this).getFreeSpace() / (1024 * 1024);
        TextView tvEngine = new TextView(this);
        tvEngine.setText("🚀 4x Parallel 128KB Foreground Service • Never Stops on Exit\n💾 Free Space: " + freeSpaceMb + " MB • Saved in: Downloads/NOVA Browser");
        tvEngine.setTextColor(Color.parseColor(getAccentHex()));
        tvEngine.setTextSize(11.8f);
        statsCard.addView(tvEngine);
        stackContainer.addView(statsCard);

        LinearLayout linkCard = createGlassCard();
        linkCard.addView(makeSectionLabel("🔗 DIRECT LINK PASTE & DOWNLOAD", getAccentHex()));

        EditText etDirectUrl = new EditText(this);
        etDirectUrl.setHint("Paste direct file link (.apk, .mp4, .zip, .pdf)...");
        etDirectUrl.setTextColor(Color.parseColor("#FFFFFF"));
        etDirectUrl.setHintTextColor(Color.parseColor("#94A3B8"));
        etDirectUrl.setBackground(makeGlassDrawable(
                Color.parseColor("#AA060D1E"),
                Color.parseColor("#AA0E1B36"),
                Color.parseColor("#5500D2FF"),
                14, 1));
        etDirectUrl.setPadding(dp(12), dp(10), dp(12), dp(10));
        etDirectUrl.setSingleLine(true);
        etDirectUrl.setTextSize(13.5f);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        etLp.setMargins(0, 0, 0, dp(10));
        linkCard.addView(etDirectUrl, etLp);

        LinearLayout linkBtnRow = new LinearLayout(this);
        linkBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPaste = createLargeGlassButton("📋 Paste Link", "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        pasteLp.setMargins(0, 0, dp(8), 0);
        linkBtnRow.addView(btnPaste, pasteLp);

        Button btnDirectGo = createLargeGlassButton("⚡ Download", "#0072FF", "#00C6FF", "#FFFFFF");
        linkBtnRow.addView(btnDirectGo, new LinearLayout.LayoutParams(0, dp(42), 1f));
        linkCard.addView(linkBtnRow);
        stackContainer.addView(linkCard);

        btnPaste.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
                CharSequence clip = cb.getPrimaryClip().getItemAt(0).getText();
                if (clip != null) {
                    etDirectUrl.setText(clip.toString().trim());
                    Toast.makeText(this, "Link Pasted!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout queueSectionHeader = new LinearLayout(this);
        queueSectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        queueSectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        queueSectionHeader.setPadding(dp(4), dp(4), dp(4), dp(6));

        TextView tvQueueTitle = new TextView(this);
        tvQueueTitle.setText("📥 Active & Recent Queue");
        tvQueueTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvQueueTitle.setTypeface(null, Typeface.BOLD);
        tvQueueTitle.setTextSize(14f);
        queueSectionHeader.addView(tvQueueTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClearDone = createLargeGlassButton("🧹 Clear Finished", "#26FFFFFF", "#12FFFFFF", "#CBD5E1");
        queueSectionHeader.addView(btnClearDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        stackContainer.addView(queueSectionHeader);

        LinearLayout queueListLayout = new LinearLayout(this);
        queueListLayout.setOrientation(LinearLayout.VERTICAL);
        stackContainer.addView(queueListLayout);

        LinearLayout catCard = createGlassCard();
        catCard.addView(makeSectionLabel("📂 SAVED FILES (DOWNLOADS/NOVA BROWSER)", getAccentHex()));

        String[] catNames = {"📂 All", "🤖 APKs", "🎬 Videos", "🖼️ Images", "📄 Docs", "📦 ZIPs"};
        String[][] catExts = {
            {}, {".apk"}, {".mp4", ".mkv", ".webm", ".avi"},
            {".jpg", ".jpeg", ".png", ".webp", ".gif"},
            {".pdf", ".doc", ".docx", ".txt", ".xlsx"},
            {".zip", ".rar", ".7z", ".tar", ".gz"}
        };

        for (int row = 0; row < 2; row++) {
            LinearLayout rLayout = new LinearLayout(this);
            rLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (row == 0) rLp.setMargins(0, 0, 0, dp(6));

            for (int col = 0; col < 3; col++) {
                final int idx = row * 3 + col;
                Button chip = createLargeGlassButton(catNames[idx], "#2B00D2FF", "#166366F1", "#E0F2FE");
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
                if (col < 2) cLp.setMargins(0, 0, dp(6), 0);
                chip.setOnClickListener(v -> showFilteredFiles(catNames[idx], catExts[idx]));
                rLayout.addView(chip, cLp);
            }
            catCard.addView(rLayout, rLp);
        }
        stackContainer.addView(catCard);

        outerGlass.addView(scrollRoot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button btnBgKeep = createLargeGlassButton("⬇ Minimize (Foreground Service Keeps Downloading)", "#2B00D2FF", "#166366F1", "#7DD3FC");
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        footLp.setMargins(0, dp(8), 0, 0);
        btnBgKeep.setOnClickListener(v -> dialog.dismiss());
        outerGlass.addView(btnBgKeep, footLp);

        final Map<DownloadService.TurboDownloadTask, QueueCardRefs> renderedRefs = new HashMap<>();

        Runnable syncQueueUI = () -> {
            updateDownloadDockBadge();
            boolean needsFullRebuild = renderedRefs.size() != DownloadService.TASKS.size();
            if (!needsFullRebuild) {
                for (DownloadService.TurboDownloadTask t : DownloadService.TASKS) {
                    QueueCardRefs ref = renderedRefs.get(t);
                    if (ref == null || ref.renderedStatus != t.status) {
                        needsFullRebuild = true;
                        break;
                    }
                }
            }

            if (needsFullRebuild) {
                queueListLayout.removeAllViews();
                renderedRefs.clear();

                if (DownloadService.TASKS.isEmpty()) {
                    LinearLayout emptyCard = createGlassCard();
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("No active downloads.\nPaste a URL above or tap any file download link on a website!");
                    tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                    tvEmpty.setTextSize(12.5f);
                    emptyCard.addView(tvEmpty);
                    queueListLayout.addView(emptyCard);
                } else {
                    for (DownloadService.TurboDownloadTask task : DownloadService.TASKS) {
                        LinearLayout card = createGlassCard();

                        TextView tvName = new TextView(this);
                        String modeTag = task.isMultiSegment ? " [4x]" : "";
                        tvName.setText((task.isSecureHttps ? "🔒 " : "⚠️ ") + task.fileName + modeTag);
                        tvName.setTextColor(Color.parseColor("#FFFFFF"));
                        tvName.setTypeface(null, Typeface.BOLD);
                        tvName.setTextSize(13.5f);
                        tvName.setSingleLine(true);
                        tvName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        card.addView(tvName);

                        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                        pb.setMax(100);
                        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
                        pbLp.setMargins(0, dp(8), 0, dp(6));
                        card.addView(pb, pbLp);

                        TextView tvStatus = new TextView(this);
                        tvStatus.setTextSize(11.5f);
                        card.addView(tvStatus);

                        LinearLayout ctrlRow = new LinearLayout(this);
                        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
                        ctrlRow.setGravity(Gravity.END);
                        ctrlRow.setPadding(0, dp(8), 0, 0);

                        if (task.status == DownloadService.TurboDownloadTask.STATUS_DOWNLOADING) {
                            Button btnPause = createLargeGlassButton("⏸ Pause", "#33FFD000", "#1AFFD000", "#FFE566");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnPause.setOnClickListener(v -> task.status = DownloadService.TurboDownloadTask.STATUS_PAUSED);
                            ctrlRow.addView(btnPause, lp);
                        } else if (task.status == DownloadService.TurboDownloadTask.STATUS_PAUSED || task.status == DownloadService.TurboDownloadTask.STATUS_FAILED) {
                            Button btnResume = createLargeGlassButton("▶ Resume", "#0072FF", "#00C6FF", "#FFFFFF");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnResume.setOnClickListener(v -> DownloadService.resumeDownload(this, task));
                            ctrlRow.addView(btnResume, lp);
                        }

                        if (task.status != DownloadService.TurboDownloadTask.STATUS_COMPLETED && task.status != DownloadService.TurboDownloadTask.STATUS_CANCELLED) {
                            Button btnCancel = createLargeGlassButton("✕ Cancel", "#33EF4444", "#1AEF4444", "#FCA5A5");
                            btnCancel.setOnClickListener(v -> {
                                task.status = DownloadService.TurboDownloadTask.STATUS_CANCELLED;
                                if (task.stagingFile.exists()) task.stagingFile.delete();
                            });
                            ctrlRow.addView(btnCancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
                        }

                        if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) {
                            Button btnOpen = createLargeGlassButton("📂 Open Downloads", "#00B09B", "#00F5A0", "#042016");
                            btnOpen.setOnClickListener(v -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                            ctrlRow.addView(btnOpen, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
                        }

                        card.addView(ctrlRow);
                        queueListLayout.addView(card);

                        QueueCardRefs ref = new QueueCardRefs();
                        ref.pb = pb;
                        ref.tvStatus = tvStatus;
                        ref.renderedStatus = task.status;
                        renderedRefs.put(task, ref);
                    }
                }
            }

            for (DownloadService.TurboDownloadTask task : DownloadService.TASKS) {
                QueueCardRefs ref = renderedRefs.get(task);
                if (ref == null) continue;
                long dl = task.downloadedBytes.get();
                int pct = (task.totalBytes > 0) ? (int) ((dl * 100L) / task.totalBytes) : 0;
                if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) pct = 100;
                ref.pb.setProgress(Math.min(100, Math.max(0, pct)));

                String sizeRatio = DownloadService.formatBytes(dl) + " / " + DownloadService.formatBytes(task.totalBytes);
                if (task.status == DownloadService.TurboDownloadTask.STATUS_DOWNLOADING) {
                    String speedStr = DownloadService.formatBytes(task.speedBytesPerSec) + "/s";
                    String etaStr = "";
                    if (task.speedBytesPerSec > 0 && task.totalBytes > dl) {
                        etaStr = " • ETA: " + ((task.totalBytes - dl) / task.speedBytesPerSec) + "s";
                    }
                    ref.tvStatus.setText("🚀 " + pct + "% • " + sizeRatio + " • " + speedStr + etaStr);
                    ref.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_PAUSED) {
                    ref.tvStatus.setText("⏸ PAUSED at " + pct + "% • " + sizeRatio);
                    ref.tvStatus.setTextColor(Color.parseColor("#FFD000"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) {
                    ref.tvStatus.setText("✅ SAVED • " + DownloadService.formatBytes(dl) + " • " + task.savedDisplayPath);
                    ref.tvStatus.setTextColor(Color.parseColor("#00F5A0"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_CANCELLED) {
                    ref.tvStatus.setText("❌ CANCELLED");
                    ref.tvStatus.setTextColor(Color.parseColor("#FB7185"));
                } else {
                    ref.tvStatus.setText("⚠️ FAILED (" + task.errorMsg + ") • Tap Resume");
                    ref.tvStatus.setTextColor(Color.parseColor("#FB7185"));
                }
            }
        };

        btnDirectGo.setOnClickListener(v -> {
            String rawUrl = etDirectUrl.getText().toString().trim();
            if (rawUrl.isEmpty()) {
                Toast.makeText(this, "Paste or type a valid file URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) rawUrl = "https://" + rawUrl;
            String guessed = URLUtil.guessFileName(rawUrl, null, null);
            DownloadService.startNewDownload(this, rawUrl, guessed, defaultUserAgent);
            etDirectUrl.setText("");
            syncQueueUI.run();
        });

        btnClearDone.setOnClickListener(v -> {
            DownloadService.TASKS.removeIf(t -> t.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED
                    || t.status == DownloadService.TurboDownloadTask.STATUS_CANCELLED
                    || t.status == DownloadService.TurboDownloadTask.STATUS_FAILED);
            syncQueueUI.run();
        });

        FrameLayout modalMargin = new FrameLayout(this);
        modalMargin.setPadding(dp(10), dp(14), dp(10), dp(14));
        modalMargin.addView(outerGlass);
        dialog.setContentView(modalMargin);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.86f);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxH);
            dialog.getWindow().setDimAmount(0.74f);
        }

        Runnable liveUpdater = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                syncQueueUI.run();
                uiHandler.postDelayed(this, 500);
            }
        };
        dialog.setOnShowListener(d -> uiHandler.post(liveUpdater));
        dialog.show();
    }

    private void showFilteredFiles(String title, String[] extensions) {
        List<String> labels = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] projection = {MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE};
            try (Cursor c = getContentResolver().query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection, null, null,
                    MediaStore.Downloads.DATE_ADDED + " DESC")) {
                if (c != null) {
                    int idCol = c.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                    int nameCol = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME);
                    int sizeCol = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE);
                    while (c.moveToNext()) {
                        String name = c.getString(nameCol);
                        if (name == null) continue;
                        String lower = name.toLowerCase(Locale.US);
                        boolean ok = extensions.length == 0;
                        for (String ext : extensions) {
                            if (lower.endsWith(ext)) { ok = true; break; }
                        }
                        if (ok) {
                            labels.add(name + " (" + DownloadService.formatBytes(c.getLong(sizeCol)) + ")");
                            uris.add(Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(c.getLong(idCol))));
                        }
                    }
                }
            } catch (Exception ignored) {}
        } else {
            File pubDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NOVA Browser");
            File[] files = pubDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile()) continue;
                    String lower = f.getName().toLowerCase(Locale.US);
                    boolean ok = extensions.length == 0;
                    for (String ext : extensions) {
                        if (lower.endsWith(ext)) { ok = true; break; }
                    }
                    if (ok) {
                        labels.add(f.getName() + " (" + DownloadService.formatBytes(f.length()) + ")");
                        uris.add(Uri.fromFile(f));
                    }
                }
            }
        }

        if (labels.isEmpty()) {
            Toast.makeText(this, "No matching files in Downloads/NOVA Browser", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle(title + " (" + labels.size() + ")")
            .setItems(labels.toArray(new String[0]), (d, which) -> {
                Uri fileUri = uris.get(which);
                new AlertDialog.Builder(this)
                    .setTitle(labels.get(which))
                    .setMessage("Saved in Phone Storage → Downloads/NOVA Browser")
                    .setPositiveButton("Open Downloads Folder", (d2, w2) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)))
                    .setNeutralButton("Delete File", (d2, w2) -> {
                        try {
                            getContentResolver().delete(fileUri, null, null);
                            Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Delete from your Files app", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
            })
            .show();
    }

    // =========================================================================
    // DEVELOPER PAGE (WITH REGULAR UPDATES NOTICE) & BROWSER UTILITIES
    // =========================================================================

    private void showAboutDeveloperDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                26, 1));
        outer.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("👨‍💻 About & Contact Developer");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(18f);
        outer.addView(tvTitle);

        LinearLayout card = createGlassCard();
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, dp(12), 0, dp(12));

        TextView tvInfo = new TextView(this);
        tvInfo.setText("🌐 NOVA Browser v4.0 — Liquid Glass Edition\n\n"
                + "Developer: " + DEV_NAME + "\n"
                + "Email: " + DEV_EMAIL + "\n"
                + "GitHub: @shahalamlaskarofficial\n\n"
                + "🔄 Regular & Frequent Updates:\n"
                + "NOVA Browser is actively maintained with regular updates bringing stronger ad-blocking rules, faster download performance, fresh UI upgrades, and tighter RAM/battery optimization. Check GitHub Releases regularly for the latest seamless in-place update!");
        tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
        tvInfo.setTextSize(12.8f);
        tvInfo.setLineSpacing(dp(2), 1f);
        card.addView(tvInfo);
        outer.addView(card, cLp);

        Button btnEmail = createLargeGlassButton("📧 Email Developer", "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        bLp.setMargins(0, 0, 0, dp(8));
        btnEmail.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:" + DEV_EMAIL + "?subject=" + Uri.encode("NOVA Browser v4.0 Feedback")));
            startActivity(Intent.createChooser(email, "Email " + DEV_NAME));
        });
        outer.addView(btnEmail, bLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button btnGit = createLargeGlassButton("🚀 Check Updates", "#2B00F5A0", "#1400B09B", "#00F5A0");
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        gLp.setMargins(0, 0, dp(8), 0);
        btnGit.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) wv.loadUrl(DEV_GITHUB + "/Nova-Browser--advanced-/releases");
            dialog.dismiss();
        });
        row.addView(btnGit, gLp);

        Button btnClose = createLargeGlassButton("Close", "#33EF4444", "#1AEF4444", "#FCA5A5");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        row.addView(btnClose, new LinearLayout.LayoutParams(0, dp(42), 1f));
        outer.addView(row);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(14), dp(16), dp(14), dp(16));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.74f);
        }
        dialog.show();
    }

    private void loadUserInput(String input) {
        if (input.isEmpty()) return;
        WebView wv = getCurrentWebView();
        if (wv == null) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);

        if ("nova://home".equalsIgnoreCase(input)) loadNovaHome(wv);
        else if (input.startsWith("http://") || input.startsWith("https://")) wv.loadUrl(input);
        else if (input.contains(".") && !input.contains(" ")) wv.loadUrl((isHttpsFirst ? "https://" : "http://") + input);
        else wv.loadUrl(searchEngineUrl + Uri.encode(input));
    }

    private void showPerformanceOptimizerDialog() {
        long usedMb = getUsedRamMbLong();
        int suspendedCount = Math.max(0, tabs.size() - 1);

        String info = "🧠 Current App RAM: " + usedMb + " MB (Target Cap: ≤ 15–18 MB)\n"
                + "🛡️ Blocklist Footprint: ~1.4 MB (Primitive Hash Engine)\n"
                + "🗂️ Active Tabs: 1 | Suspended Tabs: " + suspendedCount + "\n"
                + "🔋 Battery Saver Mode: " + (isBatterySaver ? "ENABLED" : "DISABLED") + "\n"
                + "📉 Data Saver: " + (isDataSaver ? "ENABLED" : "DISABLED");

        new AlertDialog.Builder(this)
            .setTitle("⚡ RAM & Battery Optimizer")
            .setMessage(info)
            .setPositiveButton("🚀 Boost RAM Now", (d, w) -> {
                for (int i = 0; i < tabs.size(); i++) {
                    WebView tab = tabs.get(i);
                    tab.clearCache(false);
                    if (i != currentTabIndex) tab.onPause();
                }
                System.gc();
                Toast.makeText(this, "RAM Boosted! Now using " + getRamUsageMb(), Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton(isBatterySaver ? "Disable Battery Saver" : "Enable Battery Saver", (d, w) -> {
                isBatterySaver = !isBatterySaver;
                prefs.edit().putBoolean("battery_saver", isBatterySaver).apply();
                Toast.makeText(this, "Battery Saver " + (isBatterySaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showTabSwitcher() {
        List<String> tabTitles = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            WebView wv = tabs.get(i);
            String title = wv.getTitle();
            if (title == null || title.isEmpty() || (wv.getUrl() != null && wv.getUrl().startsWith("https://nova.browser"))) {
                title = "NOVA Home";
            }
            tabTitles.add(((i == currentTabIndex) ? "🟢 " : "💤 ") + "Tab " + (i + 1) + ": " + title);
        }
        tabTitles.add("➕ Open New Tab");
        tabTitles.add("🕵️ Toggle Private Mode (" + (isIncognito ? "ON" : "OFF") + ")");
        if (!closedTabs.isEmpty()) tabTitles.add("↩️ Reopen Closed Tab");
        if (tabs.size() > 1) tabTitles.add("🗑️ Close Current Tab");

        new AlertDialog.Builder(this)
            .setTitle("Visual Tab Switcher (" + tabs.size() + " Open | 💤 = Suspended)")
            .setItems(tabTitles.toArray(new String[0]), (d, which) -> {
                if (which < tabs.size()) switchToTab(which);
                else {
                    String action = tabTitles.get(which);
                    if (action.startsWith("➕")) createNewTab("nova://home");
                    else if (action.startsWith("🕵️")) toggleIncognitoMode();
                    else if (action.startsWith("↩️")) createNewTab(closedTabs.remove(0));
                    else if (action.startsWith("🗑️")) closeTab(currentTabIndex);
                }
            })
            .show();
    }

    private void showSearchEngineDialog() {
        String[] engines = {"Google (Default)", "Brave Search", "Bing", "Startpage (Private)", "DuckDuckGo"};
        String[] urls = {
            "https://www.google.com/search?q=", "https://search.brave.com/search?q=",
            "https://www.bing.com/search?q=", "https://www.startpage.com/do/dsearch?query=",
            "https://duckduckgo.com/?q="
        };
        String[] names = {"Google", "Brave", "Bing", "Startpage", "DuckDuckGo"};

        new AlertDialog.Builder(this)
            .setTitle("Select Search Engine")
            .setItems(engines, (d, which) -> {
                searchEngineName = names[which];
                searchEngineUrl = urls[which];
                prefs.edit().putString("search_name", searchEngineName).putString("search_url", searchEngineUrl).apply();
                WebView wv = getCurrentWebView();
                if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) loadNovaHome(wv);
                Toast.makeText(this, "Search Engine: " + searchEngineName, Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void toggleReaderMode(WebView wv) {
        if (wv == null) return;
        String js = "javascript:(function(){"
            + "document.body.style.maxWidth='680px';document.body.style.margin='0 auto';"
            + "document.body.style.padding='20px';document.body.style.fontFamily='Georgia,serif';"
            + "document.body.style.fontSize='19px';document.body.style.lineHeight='1.7';"
            + "document.body.style.backgroundColor='#0B1224';document.body.style.color='#F8FAFC';"
            + "var els=document.querySelectorAll('nav,header,footer,aside,iframe,.ads,.sidebar');"
            + "for(var i=0;i<els.length;i++){els[i].style.display='none';}"
            + "})()";
        wv.evaluateJavascript(js, null);
        Toast.makeText(this, "Reader Mode Applied", Toast.LENGTH_SHORT).show();
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        String ua = isDesktopMode
            ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            : defaultUserAgent;
        for (WebView tab : tabs) tab.getSettings().setUserAgentString(ua);
        WebView wv = getCurrentWebView();
        if (wv != null) wv.reload();
        Toast.makeText(this, "Desktop Mode " + (isDesktopMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void toggleIncognitoMode() {
        isIncognito = !isIncognito;
        CookieManager.getInstance().setAcceptCookie(!isIncognito);
        for (WebView tab : tabs) {
            tab.getSettings().setCacheMode(isIncognito ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        }
        applyLiquidGlassTheme();
        Toast.makeText(this, "Private Browsing " + (isIncognito ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void showZoomDialog() {
        String[] sizes = {"75% (Small)", "100% (Default)", "125% (Large)", "150% (Extra Large)"};
        int[] vals = {75, 100, 125, 150};
        new AlertDialog.Builder(this)
            .setTitle("Website Font & Zoom Size")
            .setItems(sizes, (d, which) -> {
                textZoomPercent = vals[which];
                for (WebView tab : tabs) tab.getSettings().setTextZoom(textZoomPercent);
            })
            .show();
    }

    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            bottomBar.setVisibility(View.GONE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            bottomBar.setVisibility(View.VISIBLE);
            animateBarsToDp(44);
        }
    }

    private void showBookmarksDialog() {
        Set<String> saved = new HashSet<>(prefs.getStringSet("bookmarks", new HashSet<>()));
        List<String> list = new ArrayList<>(saved);
        list.add(0, "➕ Bookmark Current Page");

        new AlertDialog.Builder(this)
            .setTitle("NOVA Bookmarks (" + saved.size() + ")")
            .setItems(list.toArray(new String[0]), (d, which) -> {
                WebView wv = getCurrentWebView();
                if (which == 0) {
                    if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                        String entry = (wv.getTitle() != null ? wv.getTitle() : "Site") + " | " + wv.getUrl();
                        saved.add(entry);
                        prefs.edit().putStringSet("bookmarks", saved).apply();
                        Toast.makeText(this, "Bookmark Saved!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String item = list.get(which);
                    int sep = item.lastIndexOf(" | ");
                    if (wv != null) wv.loadUrl(sep != -1 ? item.substring(sep + 3) : item);
                }
            })
            .setNeutralButton("Clear All", (d, w) -> {
                prefs.edit().remove("bookmarks").apply();
                Toast.makeText(this, "Bookmarks Cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void saveToHistory(String title, String url) {
        Set<String> history = new HashSet<>(prefs.getStringSet("history", new HashSet<>()));
        String time = new SimpleDateFormat("MM/dd HH:mm", Locale.US).format(new Date());
        history.add("[" + time + "] " + ((title != null && !title.isEmpty()) ? title : url) + " | " + url);
        prefs.edit().putStringSet("history", history).apply();
    }

    private void showHistoryDialog() {
        Set<String> history = new HashSet<>(prefs.getStringSet("history", new HashSet<>()));
        List<String> list = new ArrayList<>(history);
        if (list.isEmpty()) {
            Toast.makeText(this, "Browsing history is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Browsing History")
            .setItems(list.toArray(new String[0]), (d, which) -> {
                String item = list.get(which);
                int sep = item.lastIndexOf(" | ");
                if (sep != -1) {
                    WebView wv = getCurrentWebView();
                    if (wv != null) wv.loadUrl(item.substring(sep + 3));
                }
            })
            .setNeutralButton("Clear History", (d, w) -> {
                prefs.edit().remove("history").apply();
                Toast.makeText(this, "History Cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void shareOrCopyUrl(WebView wv) {
        if (wv == null || wv.getUrl() == null) return;
        String url = wv.getUrl();
        String[] actions = {"📋 Copy Page Link", "📤 Share Website Link", "📱 Generate QR Code for URL"};
        new AlertDialog.Builder(this)
            .setTitle("Share & Link Tools")
            .setItems(actions, (d, which) -> {
                if (which == 0) {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("NOVA URL", url));
                        Toast.makeText(this, "Link Copied!", Toast.LENGTH_SHORT).show();
                    }
                } else if (which == 1) {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, url);
                    startActivity(Intent.createChooser(send, "Share via NOVA Browser"));
                } else {
                    wv.loadUrl("https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=" + Uri.encode(url));
                }
            })
            .show();
    }

    private void clearBrowserData(WebView wv) {
        if (wv != null) {
            wv.clearCache(true);
            wv.clearHistory();
        }
        CookieManager.getInstance().removeAllCookies(null);
        prefs.edit().remove("history").apply();
        System.gc();
        Toast.makeText(this, "Cache, Cookies, History & RAM Cleared!", Toast.LENGTH_SHORT).show();
    }
}
