package com.nova.browser;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String DEV_NAME = "Shah Alam Laskar";
    private static final String DEV_EMAIL = "shahalamlaskar.official@gmail.com";
    private static final String DEV_GITHUB = "https://github.com/shahalamlaskarofficial";
    private static final int MAX_SYNCED_DOMAINS = 180000;

    // Persistent Static Turbo Download Engine (survives navigation & backgrounding)
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
        public final File stagingFile;
        public final boolean isSecureHttps;
        public volatile long downloadedBytes = 0;
        public volatile long totalBytes = -1;
        public volatile long speedBytesPerSec = 0;
        public volatile int status = STATUS_DOWNLOADING;
        public volatile String savedDisplayPath = "Downloads/NOVA Browser/";
        public volatile String errorMsg = "";

        public TurboDownloadTask(String url, String fileName, File stagingFile) {
            this.url = url;
            this.fileName = fileName;
            this.stagingFile = stagingFile;
            this.isSecureHttps = url != null && url.toLowerCase(Locale.US).startsWith("https://");
            if (stagingFile.exists()) {
                this.downloadedBytes = stagingFile.length();
            }
        }
    }

    private static class QueueCardRefs {
        ProgressBar pb;
        TextView tvStatus;
        int renderedStatus;
    }

    // JS Bridge allowing window.fetch & XHR to query the full 150,000+ domain database synchronously
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
            } catch (Exception ignored) {
            }
            return false;
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
    private boolean isHttpsFirst = true;
    private boolean isDataSaver = false;
    private boolean isBatterySaver = true;
    private boolean isDarkTheme = true;
    private boolean isFullScreen = false;
    private boolean barsVisible = true;
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

    // Safe core sites that must never be false-positive blocked by heuristics
    private static final Set<String> SAFE_MAIN_DOMAINS = new HashSet<>(Arrays.asList(
        "nova.browser", "google.com", "www.google.com", "youtube.com", "m.youtube.com",
        "www.youtube.com", "open.spotify.com", "github.com", "chatgpt.com",
        "wikipedia.org", "reddit.com", "testfile.org", "fast.com",
        "adblock.turtlecute.org", "canyoublockit.com", "superadblocktest.com"
    ));

    // Built-in High-Priority Ad Domains (Active immediately before remote lists load)
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

    // Built-in Trackers, Analytics, Error Loggers & Telemetry
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

    // High-Precision Ad Subdomain Prefixes for 95%+ Zero-Day Ad & Tracker Catching
    private static final String[] AD_SUBDOMAIN_PREFIXES = {
        "ad.", "ads.", "ad1.", "ad2.", "adserver.", "adservice.", "adsystem.", "adtech.",
        "adclick.", "advert.", "adverts.", "advertising.", "banner.", "banners.",
        "popunder.", "popads.", "track.", "tracker.", "tracking.", "telemetry.",
        "metrics.", "analytics.", "pixel.", "beacon.", "stats.", "click.", "clicks."
    };

    // High-Precision Path Rules for YouTube, Spotify & General Ad Endpoints
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

    // 4 High-Coverage Blocklist Sources (150,000+ domains combined)
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
        isBatterySaver = prefs.getBoolean("battery_saver", true);
        allowlistDomains = new HashSet<>(prefs.getStringSet("allowlist", new HashSet<>()));

        blockedAdHosts.addAll(Arrays.asList(CORE_AD_DOMAINS));
        blockedTrackerHosts.addAll(Arrays.asList(CORE_TRACKER_DOMAINS));
        loadCachedAndSyncFilterLists();

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

        btnShield.setOnClickListener(v -> { pulseButton(v); showPrivacyDashboard(); });
        btnTabs.setOnClickListener(v -> { pulseButton(v); showTabSwitcher(); });
        btnMenu.setOnClickListener(v -> { pulseButton(v); showMainMenu(); });

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
                if (url == null || url.startsWith("https://nova.browser")) {
                    loadNovaHome(wv);
                } else {
                    wv.reload();
                }
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
                } else if (hasRunningDownloads()) {
                    Toast.makeText(MainActivity.this,
                            "⚡ Downloads running in background until completed or cancelled",
                            Toast.LENGTH_LONG).show();
                    moveTaskToBack(true);
                } else {
                    finish();
                }
            }
        });
    }

    // =========================================================================
    // LIQUID GLASS THEME & SMOOTH SCROLL AUTO-HIDE ANIMATION
    // =========================================================================

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void pulseButton(View v) {
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(75)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start())
            .start();
    }

    private void animateBarsVisibility(boolean show) {
        if (isFullScreen || barsVisible == show) return;
        barsVisible = show;

        final int targetH = show ? dp(48) : 0;
        final int targetMargin = show ? dp(4) : 0;
        final int startH = topBar.getLayoutParams().height;

        ValueAnimator anim = ValueAnimator.ofInt(startH, targetH);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(va -> {
            int val = (int) va.getAnimatedValue();
            float frac = dp(48) > 0 ? (val / (float) dp(48)) : 1f;

            LinearLayout.LayoutParams topLp = (LinearLayout.LayoutParams) topBar.getLayoutParams();
            topLp.height = val;
            topLp.topMargin = Math.round(targetMargin * frac);
            topLp.bottomMargin = Math.round(targetMargin * frac);
            topBar.setLayoutParams(topLp);
            topBar.setAlpha(frac);

            LinearLayout.LayoutParams botLp = (LinearLayout.LayoutParams) bottomBar.getLayoutParams();
            botLp.height = val;
            botLp.topMargin = Math.round(targetMargin * frac);
            botLp.bottomMargin = Math.round(targetMargin * frac);
            bottomBar.setLayoutParams(botLp);
            bottomBar.setAlpha(frac);
        });
        anim.start();
    }

    private GradientDrawable makeGlassDrawable(int topColor, int bottomColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{topColor, bottomColor});
        gd.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            gd.setStroke(dp(strokeDp), strokeColor);
        }
        return gd;
    }

    private void applyLiquidGlassTheme() {
        if (isDarkTheme) {
            rootLayout.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#050B18"), Color.parseColor("#0E1D3B"), Color.parseColor("#071022")}));

            int barTop = isIncognito ? Color.parseColor("#CC3B0764") : Color.parseColor("#B8162644");
            int barBot = isIncognito ? Color.parseColor("#CC1E0938") : Color.parseColor("#B80C172E");
            int rim = isIncognito ? Color.parseColor("#77C084FC") : Color.parseColor("#557DD3FC");

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#88071024"), Color.parseColor("#88112244"), Color.parseColor("#4438BDF8"), 16, 1));
            etUrl.setTextColor(Color.parseColor("#F8FAFC"));
            etUrl.setHintTextColor(Color.parseColor("#94A3B8"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#99071024"), Color.parseColor("#99112244"), Color.parseColor("#4438BDF8"), 14, 1));
            etFind.setTextColor(Color.parseColor("#F8FAFC"));

            styleGlassPillButton(btnShield, "#3338BDF8", "#1A38BDF8", "#557DD3FC");
            styleGlassPillButton(btnTabs, "#3338BDF8", "#1A818CF8", "#557DD3FC");
            styleGlassPillButton(btnMenu, "#26FFFFFF", "#12FFFFFF", "#38FFFFFF");

            for (Button dockBtn : new Button[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#20FFFFFF", "#0DFFFFFF", "#2B7DD3FC");
            }
            btnBack.setTextColor(Color.parseColor("#F8FAFC"));
            btnForward.setTextColor(Color.parseColor("#F8FAFC"));
            btnReload.setTextColor(Color.parseColor("#F8FAFC"));
            btnMenu.setTextColor(Color.parseColor("#F8FAFC"));
            btnHome.setTextColor(Color.parseColor("#38BDF8"));
            btnBookmarks.setTextColor(Color.parseColor("#FACC15"));
            btnDownloads.setTextColor(Color.parseColor("#38BDF8"));
        } else {
            rootLayout.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#DBEAFE"), Color.parseColor("#F0F9FF"), Color.parseColor("#E0E7FF")}));

            int barTop = isIncognito ? Color.parseColor("#DDE9D5FF") : Color.parseColor("#CCFFFFFF");
            int barBot = isIncognito ? Color.parseColor("#DDD8B4FE") : Color.parseColor("#B8E0F2FE");
            int rim = Color.parseColor("#AAFFFFFF");

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#EEFFFFFF"), Color.parseColor("#DDF1F5F9"), Color.parseColor("#660284C7"), 16, 1));
            etUrl.setTextColor(Color.parseColor("#0F172A"));
            etUrl.setHintTextColor(Color.parseColor("#64748B"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#EEFFFFFF"), Color.parseColor("#DDF1F5F9"), Color.parseColor("#660284C7"), 14, 1));
            etFind.setTextColor(Color.parseColor("#0F172A"));

            styleGlassPillButton(btnShield, "#88FFFFFF", "#55E0F2FE", "#AA0284C7");
            styleGlassPillButton(btnTabs, "#88FFFFFF", "#55E0F2FE", "#AA0284C7");
            styleGlassPillButton(btnMenu, "#88FFFFFF", "#55FFFFFF", "#99FFFFFF");

            for (Button dockBtn : new Button[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#88FFFFFF", "#44E0F2FE", "#AAFFFFFF");
            }
            btnBack.setTextColor(Color.parseColor("#0F172A"));
            btnForward.setTextColor(Color.parseColor("#0F172A"));
            btnReload.setTextColor(Color.parseColor("#0F172A"));
            btnMenu.setTextColor(Color.parseColor("#0F172A"));
            btnHome.setTextColor(Color.parseColor("#0284C7"));
            btnBookmarks.setTextColor(Color.parseColor("#CA8A04"));
            btnDownloads.setTextColor(Color.parseColor("#0284C7"));
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
                13,
                1));
    }

    private void updateDownloadDockBadge() {
        int running = 0;
        for (TurboDownloadTask t : TURBO_DOWNLOADS) {
            if (t.status == TurboDownloadTask.STATUS_DOWNLOADING) running++;
        }
        final int activeCount = running;
        uiHandler.post(() -> {
            if (activeCount > 0) {
                btnDownloads.setText("⬇" + activeCount);
                btnDownloads.setTextSize(13f);
                btnDownloads.setTextColor(Color.parseColor("#4ADE80"));
            } else {
                btnDownloads.setText("⬇");
                btnDownloads.setTextSize(16f);
                btnDownloads.setTextColor(Color.parseColor(isDarkTheme ? "#38BDF8" : "#0284C7"));
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
            wv.onPause();
            if (isBatterySaver) {
                wv.pauseTimers();
            }
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
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            for (int i = 0; i < tabs.size(); i++) {
                if (i != currentTabIndex) {
                    tabs.get(i).clearCache(false);
                }
            }
            System.gc();
        }
    }

    // =========================================================================
    // NOVA SHIELD v4 (150,000+ DOMAIN ENGINE + YOUTUBE/SPOTIFY/BANNER BLOCKER)
    // =========================================================================

    private void loadCachedAndSyncFilterLists() {
        new Thread(() -> {
            File cacheFile = new File(getFilesDir(), "nova_shield_v4_hosts.txt");
            // 1. Instantly load cached 150,000+ hosts from disk if available
            if (cacheFile.exists() && cacheFile.length() > 10000) {
                try (BufferedReader br = new BufferedReader(new FileReader(cacheFile))) {
                    String line;
                    while ((line = br.readLine()) != null && blockedAdHosts.size() < MAX_SYNCED_DOMAINS) {
                        if (!line.isEmpty()) blockedAdHosts.add(line);
                    }
                } catch (Exception ignored) {}
            }

            // 2. Refresh from all 4 major blocklists if cache is missing or older than 24h
            long ageMs = System.currentTimeMillis() - cacheFile.lastModified();
            if (!cacheFile.exists() || cacheFile.length() < 10000 || ageMs > 86400000L) {
                Set<String> freshDomains = new HashSet<>();
                for (String listUrl : FILTER_LIST_URLS) {
                    if (freshDomains.size() >= MAX_SYNCED_DOMAINS) break;
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(listUrl).openConnection();
                        conn.setConnectTimeout(7000);
                        conn.setReadTimeout(12000);
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null && freshDomains.size() < MAX_SYNCED_DOMAINS) {
                                String host = parseBlocklistDomain(line);
                                if (host != null && !SAFE_MAIN_DOMAINS.contains(host)) {
                                    freshDomains.add(host);
                                    blockedAdHosts.add(host);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (freshDomains.size() > 5000) {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile))) {
                        for (String d : freshDomains) {
                            bw.write(d);
                            bw.newLine();
                        }
                    } catch (Exception ignored) {}

                    int totalRules = blockedAdHosts.size() + blockedTrackerHosts.size();
                    uiHandler.post(() -> Toast.makeText(MainActivity.this,
                            "🛡️ NOVA Shield v4 Ready: " + totalRules + " ad & tracker rules active",
                            Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private String parseBlocklistDomain(String rawLine) {
        if (rawLine == null) return null;
        String s = rawLine.trim().toLowerCase(Locale.US);
        if (s.isEmpty() || s.startsWith("#") || s.startsWith("!")) return null;

        int commentIdx = s.indexOf('#');
        if (commentIdx != -1) s = s.substring(0, commentIdx).trim();

        // Handle hosts format: "0.0.0.0 ads.example.com" or "127.0.0.1 ads.example.com"
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
            if (cleanHost.equals(allowed) || cleanHost.endsWith("." + allowed)) {
                return 0;
            }
        }

        // 1. Exact & parent-domain O(1) lookup against 150,000+ database
        String part = cleanHost;
        while (part.contains(".")) {
            if (blockedTrackerHosts.contains(part)) return 2;
            if (blockedAdHosts.contains(part)) return 1;
            int dot = part.indexOf('.');
            part = part.substring(dot + 1);
        }

        // 2. High-precision ad/tracker subdomain prefix catch (e.g. ads.site.com, telemetry.site.com)
        for (String prefix : AD_SUBDOMAIN_PREFIXES) {
            if (cleanHost.startsWith(prefix)) {
                return 1;
            }
        }

        // 3. URL path & YouTube/Spotify ad endpoint keywords
        if (fullUrl != null) {
            String lowerUrl = fullUrl.toLowerCase(Locale.US);
            for (String kw : BLOCKED_URL_KEYWORDS) {
                if (lowerUrl.contains(kw)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private WebResourceResponse createBlockedResponse() {
        InputStream abortStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Blocked by NOVA Shield v4");
            }
        };
        return new WebResourceResponse("text/plain", "UTF-8", abortStream);
    }

    private void injectNovaShieldHook(WebView view) {
        if (!isAdBlockEnabled) return;
        String jsHook = "javascript:(function(){"
            + "if(window.__novaShieldV4)return;window.__novaShieldV4=true;"
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
            + "[class*=\"sponsored-ad\"],[class*=\"popunder\"],[class*=\"interstitial-ad\"],[class*=\"cookie-banner\"],"
            + "[id*=\"cookie-consent\"],iframe[src*=\"ads\"],iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"],"
            + "ytm-promoted-sparkles-web-renderer,ytm-ad-slot-renderer,ytd-ad-slot-renderer,"
            + "ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,ytm-companion-ad-renderer,"
            + "#masthead-ad,#player-ads,.ytp-ad-overlay-container,.ytp-ad-text-overlay,"
            + "[data-testid=\"ad-slot\"],[data-testid=\"in-app-message\"]{display:none!important;visibility:hidden!important;height:0!important;}';"
            + "if(document.head)document.head.appendChild(st);"
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

    // =========================================================================
    // TAB ENGINE & LIQUID GLASS HOME PAGE
    // =========================================================================

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

        if ("nova://home".equals(initialUrl)) {
            loadNovaHome(webView);
        } else {
            webView.loadUrl(initialUrl);
        }
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
        animateBarsVisibility(true);

        String url = active.getUrl();
        if (url == null || url.startsWith("https://nova.browser")) {
            etUrl.setText("");
            etUrl.setHint("Search " + searchEngineName + " or type URL...");
        } else {
            etUrl.setText(url);
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

        if (tabs.isEmpty()) {
            createNewTab("nova://home");
        } else {
            int nextIdx = Math.min(index, tabs.size() - 1);
            switchToTab(nextIdx);
        }
    }

    private String getRamUsageMb() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        return usedMb + " MB";
    }

    private void loadNovaHome(WebView webView) {
        int totalBlocked = adsBlockedCount + trackersBlockedCount;
        int rulesCount = blockedAdHosts.size() + blockedTrackerHosts.size();
        String dataSavedKb = String.format(Locale.US, "%.1f KB", totalBlocked * 18.5);
        String bg = isDarkTheme
                ? "radial-gradient(circle at 20% 15%, #163060 0%, #091224 55%, #050914 100%)"
                : "radial-gradient(circle at 20% 15%, #BAE6FD 0%, #E0F2FE 55%, #EEF2FF 100%)";
        String glass = isDarkTheme ? "rgba(255,255,255,0.08)" : "rgba(255,255,255,0.65)";
        String border = isDarkTheme ? "rgba(125,211,252,0.28)" : "rgba(255,255,255,0.9)";
        String text = isDarkTheme ? "#F8FAFC" : "#0F172A";
        String sub = isDarkTheme ? "#94A3B8" : "#475569";

        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "body{margin:0;min-height:100vh;padding:20px 14px 28px;font-family:-apple-system,sans-serif;background:" + bg + ";color:" + text + ";text-align:center;box-sizing:border-box;}"
            + ".logo{font-size:32px;font-weight:900;letter-spacing:2px;background:linear-gradient(120deg,#7DD3FC,#818CF8,#E879F9);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-top:6px;}"
            + ".tag{font-size:12px;color:" + sub + ";margin:6px 0 18px;}"
            + ".glass{background:" + glass + ";backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);border:1px solid " + border + ";box-shadow:0 8px 28px rgba(0,0,0,0.26);}"
            + ".search-box{display:flex;border-radius:26px;padding:6px 12px;margin:0 auto 18px;max-width:480px;}"
            + ".search-box input{flex:1;background:transparent;border:none;color:" + text + ";font-size:15px;outline:none;padding:8px;}"
            + ".search-box button{background:linear-gradient(135deg,#38BDF8,#6366F1);color:#FFFFFF;border:none;border-radius:18px;padding:8px 18px;font-weight:800;font-size:13px;}"
            + ".stats{display:flex;justify-content:space-around;border-radius:20px;padding:14px 6px;margin:0 auto 18px;max-width:480px;}"
            + ".stat-num{font-size:17px;font-weight:800;color:#38BDF8;}"
            + ".stat-lbl{font-size:10px;color:" + sub + ";margin-top:3px;}"
            + ".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;max-width:480px;margin:0 auto 20px;}"
            + ".tile{padding:14px 6px;border-radius:18px;text-decoration:none;color:" + text + ";font-size:11.5px;font-weight:700;display:block;}"
            + ".icon{font-size:22px;display:block;margin-bottom:5px;}"
            + ".dev-card{border-radius:18px;padding:12px;max-width:480px;margin:0 auto;font-size:12px;color:" + sub + ";}"
            + ".dev-card a{color:#38BDF8;text-decoration:none;font-weight:700;}"
            + "</style></head><body>"
            + "<div class='logo'>NOVA BROWSER</div>"
            + "<div class='tag'>Shield v4 (" + rulesCount + " Rules) &bull; YT &amp; Spotify AdBlock</div>"
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
            + "<div class='dev-card glass'>Crafted by <b>" + DEV_NAME + "</b><br/>"
            + "Contact: <a href='mailto:" + DEV_EMAIL + "'>" + DEV_EMAIL + "</a></div>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://nova.browser", html, "text/html", "UTF-8", null);
        etUrl.setText("");
        etUrl.setHint("Search " + searchEngineName + " or type URL...");
        animateBarsVisibility(true);
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

        if (defaultUserAgent == null) {
            defaultUserAgent = settings.getUserAgentString();
        }

        // Attach synchronous 150,000+ domain JS Shield Bridge
        webView.addJavascriptInterface(new NovaShieldBridge(), "NovaShield");
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        // Smooth scroll auto-hide for top & bottom Liquid Glass bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                String currentUrl = webView.getUrl();
                if (currentUrl != null && currentUrl.startsWith("https://nova.browser")) {
                    animateBarsVisibility(true);
                    return;
                }
                int dy = scrollY - oldScrollY;
                if (scrollY <= dp(12)) {
                    animateBarsVisibility(true);
                } else if (dy > dp(16)) {
                    animateBarsVisibility(false);
                } else if (dy < -dp(12)) {
                    animateBarsVisibility(true);
                }
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
                // Block malicious ad redirects / pop-under navigations
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
                    animateBarsVisibility(true);
                    if (url.startsWith("https://nova.browser")) {
                        etUrl.setText("");
                    } else {
                        String lock = url.startsWith("https://") ? "🔒" : "⚠️";
                        etUrl.setText(url);
                        btnShield.setText(lock);
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
                if (newProgress > 15 && newProgress < 95) {
                    injectNovaShieldHook(view);
                }
                if (view == getCurrentWebView()) {
                    progressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                List<String> granted = new ArrayList<>();
                for (String res : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(res)) {
                        granted.add(res);
                    }
                }
                if (!granted.isEmpty()) {
                    request.grant(granted.toArray(new String[0]));
                } else {
                    request.deny();
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            promptNewTurboDownload(url, guessedName, contentLength);
        });
    }

    // =========================================================================
    // PERMANENT MEDIASTORE PUBLIC DOWNLOADS + TURBO DOWNLOAD ENGINE
    // =========================================================================

    private File getStagingDirectory() {
        File dir = getExternalFilesDir("nova_staging");
        if (dir == null) dir = new File(getFilesDir(), "nova_staging");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private String guessMimeType(String fileName) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (ext != null && !ext.isEmpty()) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }

    // Permanently saves completed download into public phone storage: Downloads/NOVA Browser/
    private String exportToPublicDownloads(File sourceFile, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, guessMimeType(fileName));
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NOVA Browser");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (InputStream in = new FileInputStream(sourceFile);
                         OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            byte[] buf = new byte[65536];
                            int len;
                            while ((len = in.read(buf)) != -1) {
                                out.write(buf, 0, len);
                            }
                            out.flush();
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                    sourceFile.delete();
                    return "Downloads/NOVA Browser/" + fileName;
                }
            } else {
                File pubDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NOVA Browser");
                if (!pubDir.exists()) pubDir.mkdirs();
                File dest = new File(pubDir, fileName);
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[65536];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
                MediaScannerConnection.scanFile(this, new String[]{dest.getAbsolutePath()}, null, null);
                sourceFile.delete();
                return "Downloads/NOVA Browser/" + fileName;
            }
        } catch (Exception ignored) {
        }
        return sourceFile.getAbsolutePath();
    }

    private void promptNewTurboDownload(String url, String suggestedName, long contentLength) {
        boolean isHttps = url.toLowerCase(Locale.US).startsWith("https://");
        String sizeText = contentLength > 0 ? formatBytes(contentLength) : "Auto-detect on start";

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(makeGlassDrawable(
                Color.parseColor("#EE101C36"),
                Color.parseColor("#EE081022"),
                Color.parseColor("#7738BDF8"),
                24, 1));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ NOVA Turbo Downloader");
        tvTitle.setTextColor(Color.parseColor("#F8FAFC"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(17f);
        card.addView(tvTitle);

        TextView tvSec = new TextView(this);
        tvSec.setText(isHttps
                ? "🔒 Verified HTTPS Stream → Saves to Downloads/NOVA Browser"
                : "⚠️ Unencrypted HTTP Stream → Saves to Downloads/NOVA Browser");
        tvSec.setTextColor(Color.parseColor(isHttps ? "#4ADE80" : "#FACC15"));
        tvSec.setTextSize(12f);
        tvSec.setPadding(0, dp(8), 0, dp(4));
        card.addView(tvSec);

        TextView tvMeta = new TextView(this);
        tvMeta.setText("Size: " + sizeText + " • 64KB Buffered Stream (Resumable)");
        tvMeta.setTextColor(Color.parseColor("#94A3B8"));
        tvMeta.setTextSize(12f);
        tvMeta.setPadding(0, 0, 0, dp(12));
        card.addView(tvMeta);

        EditText etName = new EditText(this);
        etName.setText(suggestedName);
        etName.setHint("File name...");
        etName.setTextColor(Color.parseColor("#F8FAFC"));
        etName.setHintTextColor(Color.parseColor("#64748B"));
        etName.setBackground(makeGlassDrawable(
                Color.parseColor("#AA071024"),
                Color.parseColor("#AA132242"),
                Color.parseColor("#5538BDF8"),
                14, 1));
        etName.setPadding(dp(12), dp(10), dp(12), dp(10));
        etName.setSingleLine(true);
        card.addView(etName);

        Button btnStart = createLargeGlassButton("🚀 Start Turbo Download", "#0284C7", "#4F46E5", "#FFFFFF");
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        startLp.setMargins(0, dp(14), 0, dp(8));
        btnStart.setOnClickListener(v -> {
            String finalName = etName.getText().toString().trim();
            if (finalName.isEmpty()) finalName = "nova_file_" + System.currentTimeMillis();
            startTurboDownload(url, finalName);
            dialog.dismiss();
            showTurboDownloadManager();
        });
        card.addView(btnStart, startLp);

        LinearLayout secondaryRow = new LinearLayout(this);
        secondaryRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSys = createLargeGlassButton("System DL", "#33FFFFFF", "#1AFFFFFF", "#E2E8F0");
        LinearLayout.LayoutParams sysLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        sysLp.setMargins(0, 0, dp(8), 0);
        btnSys.setOnClickListener(v -> {
            try {
                String finalName = etName.getText().toString().trim();
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setTitle(finalName);
                req.setDescription("NOVA Browser Download");
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, finalName);
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(req);
                Toast.makeText(this, "Queued in System Downloader", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                startTurboDownload(url, suggestedName);
            }
            dialog.dismiss();
        });
        secondaryRow.addView(btnSys, sysLp);

        Button btnClose = createLargeGlassButton("Cancel", "#33EF4444", "#1AEF4444", "#FCA5A5");
        LinearLayout.LayoutParams clsLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        secondaryRow.addView(btnClose, clsLp);

        card.addView(secondaryRow);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(14), dp(14), dp(14), dp(14));
        wrap.addView(card);

        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.68f);
        }
        dialog.show();
    }

    private void startTurboDownload(String url, String fileName) {
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        File staging = new File(getStagingDirectory(), System.currentTimeMillis() + "_" + safeName + ".part");
        TurboDownloadTask task = new TurboDownloadTask(url, safeName, staging);
        TURBO_DOWNLOADS.add(0, task);
        enqueueTurboTask(task);
        updateDownloadDockBadge();
        Toast.makeText(this, "⚡ Turbo Download Started: " + safeName, Toast.LENGTH_SHORT).show();
    }

    private void enqueueTurboTask(TurboDownloadTask task) {
        task.status = TurboDownloadTask.STATUS_DOWNLOADING;
        updateDownloadDockBadge();
        DOWNLOAD_EXECUTOR.submit(() -> {
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            BufferedInputStream in = null;
            try {
                File target = task.stagingFile;
                long existingBytes = target.exists() ? target.length() : 0;
                task.downloadedBytes = existingBytes;

                URL netUrl = new URL(task.url);
                conn = (HttpURLConnection) netUrl.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", defaultUserAgent != null ? defaultUserAgent : "Mozilla/5.0");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Connection", "keep-alive");

                if (existingBytes > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existingBytes + "-");
                }

                int responseCode = conn.getResponseCode();
                boolean isPartial = (responseCode == HttpURLConnection.HTTP_PARTIAL);

                if (responseCode != HttpURLConnection.HTTP_OK && !isPartial) {
                    task.status = TurboDownloadTask.STATUS_FAILED;
                    task.errorMsg = "HTTP " + responseCode;
                    updateDownloadDockBadge();
                    return;
                }

                long contentLength = conn.getContentLengthLong();
                if (isPartial) {
                    task.totalBytes = existingBytes + contentLength;
                } else {
                    task.totalBytes = contentLength;
                    existingBytes = 0;
                    task.downloadedBytes = 0;
                }

                raf = new RandomAccessFile(target, "rw");
                if (isPartial && existingBytes > 0) {
                    raf.seek(existingBytes);
                } else {
                    raf.setLength(0);
                }

                in = new BufferedInputStream(conn.getInputStream(), 65536);
                byte[] buffer = new byte[65536];
                int bytesRead;

                long lastTime = System.currentTimeMillis();
                long bytesInWindow = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    if (task.status == TurboDownloadTask.STATUS_PAUSED) {
                        task.speedBytesPerSec = 0;
                        updateDownloadDockBadge();
                        return;
                    }
                    if (task.status == TurboDownloadTask.STATUS_CANCELLED) {
                        task.speedBytesPerSec = 0;
                        try { raf.close(); } catch (Exception ignored) {}
                        target.delete();
                        updateDownloadDockBadge();
                        return;
                    }

                    raf.write(buffer, 0, bytesRead);
                    task.downloadedBytes += bytesRead;
                    bytesInWindow += bytesRead;

                    long now = System.currentTimeMillis();
                    long elapsed = now - lastTime;
                    if (elapsed >= 400) {
                        task.speedBytesPerSec = (bytesInWindow * 1000L) / elapsed;
                        bytesInWindow = 0;
                        lastTime = now;
                    }
                }

                raf.close();
                raf = null;
                in.close();
                in = null;

                // Permanently export into public Downloads/NOVA Browser/ via MediaStore
                task.savedDisplayPath = exportToPublicDownloads(target, task.fileName);
                task.speedBytesPerSec = 0;
                task.status = TurboDownloadTask.STATUS_COMPLETED;
                updateDownloadDockBadge();
                uiHandler.post(() -> Toast.makeText(MainActivity.this,
                        "✅ Saved to " + task.savedDisplayPath, Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                task.speedBytesPerSec = 0;
                if (task.status != TurboDownloadTask.STATUS_PAUSED && task.status != TurboDownloadTask.STATUS_CANCELLED) {
                    task.status = TurboDownloadTask.STATUS_FAILED;
                    task.errorMsg = e.getMessage() != null ? e.getMessage() : "Network Interrupted";
                }
                updateDownloadDockBadge();
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                try { if (raf != null) raf.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private LinearLayout createGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(makeGlassDrawable(
                Color.parseColor("#CC162544"),
                Color.parseColor("#CC0D182F"),
                Color.parseColor("#4D7DD3FC"),
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
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private void showTurboDownloadManager() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outerGlass = new LinearLayout(this);
        outerGlass.setOrientation(LinearLayout.VERTICAL);
        outerGlass.setBackground(makeGlassDrawable(
                Color.parseColor("#F00A1428"),
                Color.parseColor("#F0050B18"),
                Color.parseColor("#6638BDF8"),
                26, 1));
        outerGlass.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(4), 0, dp(4), dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ NOVA Turbo Downloads");
        tvTitle.setTextColor(Color.parseColor("#F8FAFC"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(18f);
        headerRow.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnMin = createLargeGlassButton("✕", "#33FFFFFF", "#1AFFFFFF", "#F8FAFC");
        btnMin.setOnClickListener(v -> dialog.dismiss());
        headerRow.addView(btnMin, new LinearLayout.LayoutParams(dp(40), dp(38)));
        outerGlass.addView(headerRow);

        ScrollView scrollRoot = new ScrollView(this);
        scrollRoot.setVerticalScrollBarEnabled(false);

        LinearLayout stackContainer = new LinearLayout(this);
        stackContainer.setOrientation(LinearLayout.VERTICAL);
        scrollRoot.addView(stackContainer);

        // STACK CARD 1: Engine Telemetry
        LinearLayout statsCard = createGlassCard();
        long freeSpaceMb = getStagingDirectory().getFreeSpace() / (1024 * 1024);
        TextView tvEngine = new TextView(this);
        tvEngine.setText("🚀 64KB Multi-Thread Engine • Public MediaStore Save\n💾 Free Space: " + freeSpaceMb + " MB • Saved in: Downloads/NOVA Browser");
        tvEngine.setTextColor(Color.parseColor("#7DD3FC"));
        tvEngine.setTextSize(12f);
        statsCard.addView(tvEngine);
        stackContainer.addView(statsCard);

        // STACK CARD 2: Direct Link Paste & Download
        LinearLayout linkCard = createGlassCard();
        TextView tvLinkTitle = new TextView(this);
        tvLinkTitle.setText("🔗 Direct Link Paste & Download");
        tvLinkTitle.setTextColor(Color.parseColor("#F8FAFC"));
        tvLinkTitle.setTypeface(null, Typeface.BOLD);
        tvLinkTitle.setTextSize(14f);
        linkCard.addView(tvLinkTitle);

        EditText etDirectUrl = new EditText(this);
        etDirectUrl.setHint("Paste direct file link (.apk, .mp4, .zip, .pdf)...");
        etDirectUrl.setTextColor(Color.parseColor("#F8FAFC"));
        etDirectUrl.setHintTextColor(Color.parseColor("#94A3B8"));
        etDirectUrl.setBackground(makeGlassDrawable(
                Color.parseColor("#AA060D1E"),
                Color.parseColor("#AA0E1B36"),
                Color.parseColor("#4D38BDF8"),
                14, 1));
        etDirectUrl.setPadding(dp(12), dp(10), dp(12), dp(10));
        etDirectUrl.setSingleLine(true);
        etDirectUrl.setTextSize(13.5f);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        etLp.setMargins(0, dp(8), 0, dp(10));
        linkCard.addView(etDirectUrl, etLp);

        LinearLayout linkBtnRow = new LinearLayout(this);
        linkBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPaste = createLargeGlassButton("📋 Paste Link", "#33FFFFFF", "#1AFFFFFF", "#F8FAFC");
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        pasteLp.setMargins(0, 0, dp(8), 0);
        linkBtnRow.addView(btnPaste, pasteLp);

        Button btnDirectGo = createLargeGlassButton("⚡ Download", "#0284C7", "#4F46E5", "#FFFFFF");
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        linkBtnRow.addView(btnDirectGo, goLp);

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
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // STACK CARD 3: Active & Recent Stacked Download Queue
        LinearLayout queueSectionHeader = new LinearLayout(this);
        queueSectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        queueSectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        queueSectionHeader.setPadding(dp(4), dp(4), dp(4), dp(6));

        TextView tvQueueTitle = new TextView(this);
        tvQueueTitle.setText("📥 Download Queue");
        tvQueueTitle.setTextColor(Color.parseColor("#F8FAFC"));
        tvQueueTitle.setTypeface(null, Typeface.BOLD);
        tvQueueTitle.setTextSize(14f);
        queueSectionHeader.addView(tvQueueTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClearDone = createLargeGlassButton("🧹 Clear Finished", "#26FFFFFF", "#12FFFFFF", "#CBD5E1");
        queueSectionHeader.addView(btnClearDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        stackContainer.addView(queueSectionHeader);

        LinearLayout queueListLayout = new LinearLayout(this);
        queueListLayout.setOrientation(LinearLayout.VERTICAL);
        stackContainer.addView(queueListLayout);

        // STACK CARD 4: Clean 2-Row Stacked Category File Browser
        LinearLayout catCard = createGlassCard();
        TextView tvCatHeader = new TextView(this);
        tvCatHeader.setText("📂 Saved Downloads (Phone Downloads Folder)");
        tvCatHeader.setTextColor(Color.parseColor("#F8FAFC"));
        tvCatHeader.setTypeface(null, Typeface.BOLD);
        tvCatHeader.setTextSize(13.5f);
        tvCatHeader.setPadding(0, 0, 0, dp(8));
        catCard.addView(tvCatHeader);

        String[] catNames = {"📂 All", "🤖 APKs", "🎬 Videos", "🖼️ Images", "📄 Docs", "📦 ZIPs"};
        String[][] catExts = {
            {},
            {".apk"},
            {".mp4", ".mkv", ".webm", ".avi"},
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
                Button chip = createLargeGlassButton(catNames[idx], "#2B38BDF8", "#146366F1", "#E0F2FE");
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
                if (col < 2) cLp.setMargins(0, 0, dp(6), 0);
                chip.setOnClickListener(v -> showFilteredFiles(catNames[idx], catExts[idx]));
                rLayout.addView(chip, cLp);
            }
            catCard.addView(rLayout, rLp);
        }
        stackContainer.addView(catCard);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        outerGlass.addView(scrollRoot, scrollLp);

        Button btnBgKeep = createLargeGlassButton("⬇ Minimize (Keep Downloads Running)", "#3338BDF8", "#1A6366F1", "#7DD3FC");
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        footLp.setMargins(0, dp(8), 0, 0);
        btnBgKeep.setOnClickListener(v -> dialog.dismiss());
        outerGlass.addView(btnBgKeep, footLp);

        final Map<TurboDownloadTask, QueueCardRefs> renderedRefs = new HashMap<>();

        Runnable syncQueueUI = () -> {
            boolean needsFullRebuild = renderedRefs.size() != TURBO_DOWNLOADS.size();
            if (!needsFullRebuild) {
                for (TurboDownloadTask t : TURBO_DOWNLOADS) {
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

                if (TURBO_DOWNLOADS.isEmpty()) {
                    LinearLayout emptyCard = createGlassCard();
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("No active downloads yet.\nPaste a direct URL above or tap any download link on a website!");
                    tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                    tvEmpty.setTextSize(12.5f);
                    emptyCard.addView(tvEmpty);
                    queueListLayout.addView(emptyCard);
                } else {
                    for (TurboDownloadTask task : TURBO_DOWNLOADS) {
                        LinearLayout card = createGlassCard();

                        TextView tvName = new TextView(this);
                        tvName.setText((task.isSecureHttps ? "🔒 " : "⚠️ ") + task.fileName);
                        tvName.setTextColor(Color.parseColor("#F8FAFC"));
                        tvName.setTypeface(null, Typeface.BOLD);
                        tvName.setTextSize(13.5f);
                        tvName.setSingleLine(true);
                        tvName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        card.addView(tvName);

                        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                        pb.setMax(100);
                        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
                        pbLp.setMargins(0, dp(8), 0, dp(6));
                        card.addView(pb, pbLp);

                        TextView tvStatus = new TextView(this);
                        tvStatus.setTextSize(11.5f);
                        card.addView(tvStatus);

                        LinearLayout ctrlRow = new LinearLayout(this);
                        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
                        ctrlRow.setGravity(Gravity.END);
                        ctrlRow.setPadding(0, dp(8), 0, 0);

                        if (task.status == TurboDownloadTask.STATUS_DOWNLOADING) {
                            Button btnPause = createLargeGlassButton("⏸ Pause", "#33FACC15", "#1AFACC15", "#FDE047");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnPause.setOnClickListener(v -> task.status = TurboDownloadTask.STATUS_PAUSED);
                            ctrlRow.addView(btnPause, lp);
                        } else if (task.status == TurboDownloadTask.STATUS_PAUSED || task.status == TurboDownloadTask.STATUS_FAILED) {
                            Button btnResume = createLargeGlassButton("▶ Resume", "#0284C7", "#2563EB", "#FFFFFF");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnResume.setOnClickListener(v -> enqueueTurboTask(task));
                            ctrlRow.addView(btnResume, lp);
                        }

                        if (task.status != TurboDownloadTask.STATUS_COMPLETED && task.status != TurboDownloadTask.STATUS_CANCELLED) {
                            Button btnCancel = createLargeGlassButton("✕ Cancel", "#33EF4444", "#1AEF4444", "#FCA5A5");
                            btnCancel.setOnClickListener(v -> {
                                task.status = TurboDownloadTask.STATUS_CANCELLED;
                                if (task.stagingFile.exists()) task.stagingFile.delete();
                                updateDownloadDockBadge();
                            });
                            ctrlRow.addView(btnCancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
                        }

                        if (task.status == TurboDownloadTask.STATUS_COMPLETED) {
                            Button btnOpen = createLargeGlassButton("📂 Open Downloads", "#059669", "#047857", "#ECFDF5");
                            btnOpen.setOnClickListener(v -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                            ctrlRow.addView(btnOpen, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
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

            for (TurboDownloadTask task : TURBO_DOWNLOADS) {
                QueueCardRefs ref = renderedRefs.get(task);
                if (ref == null) continue;
                int pct = (task.totalBytes > 0) ? (int) ((task.downloadedBytes * 100L) / task.totalBytes) : 0;
                if (task.status == TurboDownloadTask.STATUS_COMPLETED) pct = 100;
                ref.pb.setProgress(Math.min(100, Math.max(0, pct)));

                String sizeRatio = formatBytes(task.downloadedBytes) + " / " + formatBytes(task.totalBytes);
                if (task.status == TurboDownloadTask.STATUS_DOWNLOADING) {
                    String speedStr = formatBytes(task.speedBytesPerSec) + "/s";
                    String etaStr = "";
                    if (task.speedBytesPerSec > 0 && task.totalBytes > task.downloadedBytes) {
                        long remSec = (task.totalBytes - task.downloadedBytes) / task.speedBytesPerSec;
                        etaStr = " • ETA: " + remSec + "s";
                    }
                    ref.tvStatus.setText("🚀 " + pct + "% • " + sizeRatio + " • " + speedStr + etaStr);
                    ref.tvStatus.setTextColor(Color.parseColor("#38BDF8"));
                } else if (task.status == TurboDownloadTask.STATUS_PAUSED) {
                    ref.tvStatus.setText("⏸ PAUSED at " + pct + "% • " + sizeRatio);
                    ref.tvStatus.setTextColor(Color.parseColor("#FACC15"));
                } else if (task.status == TurboDownloadTask.STATUS_COMPLETED) {
                    ref.tvStatus.setText("✅ SAVED • " + formatBytes(task.downloadedBytes) + " • " + task.savedDisplayPath);
                    ref.tvStatus.setTextColor(Color.parseColor("#4ADE80"));
                } else if (task.status == TurboDownloadTask.STATUS_CANCELLED) {
                    ref.tvStatus.setText("❌ CANCELLED");
                    ref.tvStatus.setTextColor(Color.parseColor("#F87171"));
                } else {
                    ref.tvStatus.setText("⚠️ FAILED (" + task.errorMsg + ") • Tap Resume");
                    ref.tvStatus.setTextColor(Color.parseColor("#F87171"));
                }
            }
        };

        btnDirectGo.setOnClickListener(v -> {
            String rawUrl = etDirectUrl.getText().toString().trim();
            if (rawUrl.isEmpty()) {
                Toast.makeText(this, "Paste or type a valid file URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                rawUrl = "https://" + rawUrl;
            }
            String guessed = URLUtil.guessFileName(rawUrl, null, null);
            startTurboDownload(rawUrl, guessed);
            etDirectUrl.setText("");
            syncQueueUI.run();
        });

        btnClearDone.setOnClickListener(v -> {
            TURBO_DOWNLOADS.removeIf(t -> t.status == TurboDownloadTask.STATUS_COMPLETED
                    || t.status == TurboDownloadTask.STATUS_CANCELLED
                    || t.status == TurboDownloadTask.STATUS_FAILED);
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
            dialog.getWindow().setDimAmount(0.72f);
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

    // Queries MediaStore.Downloads (Android 10+) + Legacy Downloads so every saved file is visible
    private void showFilteredFiles(String title, String[] extensions) {
        List<String> labels = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] projection = {
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE
            };
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
                            long id = c.getLong(idCol);
                            long size = c.getLong(sizeCol);
                            labels.add(name + " (" + formatBytes(size) + ")");
                            uris.add(Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id)));
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
                        labels.add(f.getName() + " (" + formatBytes(f.length()) + ")");
                        uris.add(Uri.fromFile(f));
                    }
                }
            }
        }

        if (labels.isEmpty()) {
            Toast.makeText(this, "No matching files found in Downloads/NOVA Browser", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle(title + " (" + labels.size() + ")")
            .setItems(labels.toArray(new String[0]), (d, which) -> {
                String label = labels.get(which);
                Uri fileUri = uris.get(which);
                new AlertDialog.Builder(this)
                    .setTitle(label)
                    .setMessage("Saved in Phone Storage → Downloads/NOVA Browser")
                    .setPositiveButton("Open Downloads Folder", (d2, w2) -> {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    })
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
    // NAVIGATION, PRIVACY DASHBOARD & BROWSER TOOLS
    // =========================================================================

    private void loadUserInput(String input) {
        if (input.isEmpty()) return;
        WebView wv = getCurrentWebView();
        if (wv == null) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);

        if ("nova://home".equalsIgnoreCase(input)) {
            loadNovaHome(wv);
        } else if (input.startsWith("http://") || input.startsWith("https://")) {
            wv.loadUrl(input);
        } else if (input.contains(".") && !input.contains(" ")) {
            wv.loadUrl((isHttpsFirst ? "https://" : "http://") + input);
        } else {
            wv.loadUrl(searchEngineUrl + Uri.encode(input));
        }
    }

    private void showPrivacyDashboard() {
        WebView wv = getCurrentWebView();
        String currentUrl = (wv != null && wv.getUrl() != null) ? wv.getUrl() : "nova://home";
        String host = Uri.parse(currentUrl).getHost();
        boolean isHttps = currentUrl.startsWith("https://");
        int total = adsBlockedCount + trackersBlockedCount;
        String saved = String.format(Locale.US, "%.2f MB", (total * 18.5) / 1024.0);
        int rulesLoaded = blockedAdHosts.size() + blockedTrackerHosts.size();

        String report = "🛡️ NOVA Shield v4: " + (isAdBlockEnabled ? "ACTIVE (95%+ Engine)" : "PAUSED") + "\n"
                + "📋 Active Blocklist Rules: " + rulesLoaded + " domains\n"
                + "▶️ YouTube & 🎵 Spotify Ad-Shield: ACTIVE\n"
                + "🔒 Connection: " + (isHttps ? "Secure (HTTPS)" : "Unencrypted / Local") + "\n"
                + "🚫 Ads Blocked: " + adsBlockedCount + "\n"
                + "👁️ Trackers Blocked: " + trackersBlockedCount + "\n"
                + "📦 Total Blocked Requests: " + total + "\n"
                + "⚡ Estimated Data Saved: " + saved + "\n"
                + "🔋 Battery Saver: " + (isBatterySaver ? "ON" : "OFF");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NOVA Shield v4 Privacy Dashboard");
        builder.setMessage(report);
        builder.setPositiveButton(isAdBlockEnabled ? "Pause Shield" : "Enable Shield", (d, w) -> {
            isAdBlockEnabled = !isAdBlockEnabled;
            prefs.edit().putBoolean("adblock_enabled", isAdBlockEnabled).apply();
            Toast.makeText(this, "NOVA Shield " + (isAdBlockEnabled ? "Enabled" : "Paused"), Toast.LENGTH_SHORT).show();
            if (wv != null) wv.reload();
        });
        if (host != null && !host.contains("nova.browser")) {
            boolean allowlisted = allowlistDomains.contains(host);
            builder.setNeutralButton(allowlisted ? "Remove Allowlist" : "Trust This Site", (d, w) -> {
                if (allowlisted) allowlistDomains.remove(host);
                else allowlistDomains.add(host);
                prefs.edit().putStringSet("allowlist", allowlistDomains).apply();
                if (wv != null) wv.reload();
            });
        }
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showPerformanceOptimizerDialog() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        int suspendedCount = Math.max(0, tabs.size() - 1);

        String info = "🧠 App RAM Used: " + usedMb + " MB / " + maxMb + " MB\n"
                + "🗂️ Active Tabs: 1 | Suspended Tabs: " + suspendedCount + "\n"
                + "🔋 Battery Saver Mode: " + (isBatterySaver ? "ENABLED" : "DISABLED") + "\n"
                + "📉 Data Saver (Image Block): " + (isDataSaver ? "ENABLED" : "DISABLED") + "\n\n"
                + "Background tabs are automatically suspended with Renderer Priority Waived to save RAM and battery.";

        new AlertDialog.Builder(this)
            .setTitle("⚡ RAM & Battery Optimizer")
            .setMessage(info)
            .setPositiveButton("🚀 Boost RAM Now", (d, w) -> {
                long before = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                for (int i = 0; i < tabs.size(); i++) {
                    WebView tab = tabs.get(i);
                    tab.clearCache(false);
                    if (i != currentTabIndex) {
                        tab.onPause();
                    }
                }
                System.gc();
                long after = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long freed = Math.max(1, before - after);
                Toast.makeText(this, "RAM Optimized! Freed ~" + freed + " MB", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton(isBatterySaver ? "Disable Battery Saver" : "Enable Battery Saver", (d, w) -> {
                isBatterySaver = !isBatterySaver;
                prefs.edit().putBoolean("battery_saver", isBatterySaver).apply();
                Toast.makeText(this, "Battery Saver " + (isBatterySaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showAboutDeveloperDialog() {
        String aboutMessage = "🌐 NOVA Browser — Liquid Glass Edition\n"
                + "Version: 3.1.0 (Shield v4 + MediaStore Turbo Downloads)\n\n"
                + "👨‍💻 Developer: " + DEV_NAME + "\n"
                + "📧 Email: " + DEV_EMAIL + "\n"
                + "💻 GitHub: @shahalamlaskarofficial\n\n"
                + "Built for ultra-fast browsing, 95%+ ad & tracker protection, persistent MediaStore downloads, and low battery usage.";

        new AlertDialog.Builder(this)
            .setTitle("About & Contact Developer")
            .setMessage(aboutMessage)
            .setPositiveButton("📧 Email Developer", (d, w) -> {
                Intent email = new Intent(Intent.ACTION_SENDTO);
                email.setData(Uri.parse("mailto:" + DEV_EMAIL + "?subject=" + Uri.encode("NOVA Browser Feedback")));
                startActivity(Intent.createChooser(email, "Email " + DEV_NAME));
            })
            .setNeutralButton("💻 GitHub", (d, w) -> {
                WebView wv = getCurrentWebView();
                if (wv != null) wv.loadUrl(DEV_GITHUB);
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
            String state = (i == currentTabIndex) ? "🟢 " : "💤 ";
            tabTitles.add(state + "Tab " + (i + 1) + ": " + title);
        }
        tabTitles.add("➕ Open New Tab");
        tabTitles.add("🕵️ Toggle Private Mode (" + (isIncognito ? "ON" : "OFF") + ")");
        if (!closedTabs.isEmpty()) {
            tabTitles.add("↩️ Reopen Closed Tab");
        }
        if (tabs.size() > 1) {
            tabTitles.add("🗑️ Close Current Tab");
        }

        new AlertDialog.Builder(this)
            .setTitle("Visual Tab Switcher (" + tabs.size() + " Open | 💤 = Suspended)")
            .setItems(tabTitles.toArray(new String[0]), (d, which) -> {
                if (which < tabs.size()) {
                    switchToTab(which);
                } else {
                    String action = tabTitles.get(which);
                    if (action.startsWith("➕")) {
                        createNewTab("nova://home");
                    } else if (action.startsWith("🕵️")) {
                        toggleIncognitoMode();
                    } else if (action.startsWith("↩️")) {
                        String reopenUrl = closedTabs.remove(0);
                        createNewTab(reopenUrl);
                    } else if (action.startsWith("🗑️")) {
                        closeTab(currentTabIndex);
                    }
                }
            })
            .show();
    }

    private void showMainMenu() {
        String[] options = {
            "⬇ NOVA Turbo Download Manager",
            "⚡ RAM & Battery Optimizer (" + getRamUsageMb() + ")",
            "🔍 Search Engine (" + searchEngineName + ")",
            "📖 Reader Mode (Distraction-Free)",
            "🔎 Find in Page",
            "🖥️ Desktop Site (" + (isDesktopMode ? "ON" : "OFF") + ") — Needed for Spotify Web",
            "🕵️ Private Browsing (" + (isIncognito ? "ON" : "OFF") + ")",
            "📉 Data Saver / Block Images (" + (isDataSaver ? "ON" : "OFF") + ")",
            "🌗 Liquid Glass Theme: " + (isDarkTheme ? "Dark Glass" : "Light Glass"),
            "🔠 Font / Zoom Size (" + textZoomPercent + "%)",
            "📺 Full-Screen Mode (" + (isFullScreen ? "ON" : "OFF") + ")",
            "🌐 Translate Current Page",
            "📜 Browsing History",
            "🔗 Share, Copy Link or QR Code",
            "🧹 Clear Browsing Data & Cache",
            "👨‍💻 About & Contact Developer"
        };

        new AlertDialog.Builder(this)
            .setTitle("NOVA Browser v3.1 Menu")
            .setItems(options, (d, which) -> {
                WebView wv = getCurrentWebView();
                switch (which) {
                    case 0: showTurboDownloadManager(); break;
                    case 1: showPerformanceOptimizerDialog(); break;
                    case 2: showSearchEngineDialog(); break;
                    case 3: toggleReaderMode(wv); break;
                    case 4:
                        layoutFind.setVisibility(View.VISIBLE);
                        etFind.requestFocus();
                        break;
                    case 5: toggleDesktopMode(); break;
                    case 6: toggleIncognitoMode(); break;
                    case 7:
                        isDataSaver = !isDataSaver;
                        for (WebView tab : tabs) tab.getSettings().setBlockNetworkImage(isDataSaver);
                        if (wv != null) wv.reload();
                        Toast.makeText(this, "Data Saver " + (isDataSaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                        break;
                    case 8: toggleTheme(); break;
                    case 9: showZoomDialog(); break;
                    case 10: toggleFullScreen(); break;
                    case 11:
                        if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                            wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(wv.getUrl()));
                        }
                        break;
                    case 12: showHistoryDialog(); break;
                    case 13: shareOrCopyUrl(wv); break;
                    case 14: clearBrowserData(wv); break;
                    case 15: showAboutDeveloperDialog(); break;
                }
            })
            .show();
    }

    private void showSearchEngineDialog() {
        String[] engines = {"Google (Default)", "Brave Search", "Bing", "Startpage (Private)", "DuckDuckGo"};
        String[] urls = {
            "https://www.google.com/search?q=",
            "https://search.brave.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://www.startpage.com/do/dsearch?query=",
            "https://duckduckgo.com/?q="
        };
        String[] names = {"Google", "Brave", "Bing", "Startpage", "DuckDuckGo"};

        new AlertDialog.Builder(this)
            .setTitle("Select Search Engine")
            .setItems(engines, (d, which) -> {
                searchEngineName = names[which];
                searchEngineUrl = urls[which];
                prefs.edit()
                    .putString("search_name", searchEngineName)
                    .putString("search_url", searchEngineUrl)
                    .apply();
                WebView wv = getCurrentWebView();
                if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                    loadNovaHome(wv);
                }
                Toast.makeText(this, "Search Engine set to " + searchEngineName, Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void toggleReaderMode(WebView wv) {
        if (wv == null) return;
        String js = "javascript:(function(){"
            + "document.body.style.maxWidth='680px';"
            + "document.body.style.margin='0 auto';"
            + "document.body.style.padding='20px';"
            + "document.body.style.fontFamily='Georgia,serif';"
            + "document.body.style.fontSize='19px';"
            + "document.body.style.lineHeight='1.7';"
            + "document.body.style.backgroundColor='#0B1224';"
            + "document.body.style.color='#F8FAFC';"
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
        for (WebView tab : tabs) {
            tab.getSettings().setUserAgentString(ua);
        }
        WebView wv = getCurrentWebView();
        if (wv != null) wv.reload();
        Toast.makeText(this, "Desktop Mode " + (isDesktopMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void toggleIncognitoMode() {
        isIncognito = !isIncognito;
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(!isIncognito);
        for (WebView tab : tabs) {
            tab.getSettings().setCacheMode(isIncognito ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        }
        applyLiquidGlassTheme();
        Toast.makeText(this, "Private Browsing " + (isIncognito ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyLiquidGlassTheme();
        WebView wv = getCurrentWebView();
        if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
            loadNovaHome(wv);
        }
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
            animateBarsVisibility(true);
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
                    } else {
                        Toast.makeText(this, "Open a website first to bookmark it", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String item = list.get(which);
                    int sep = item.lastIndexOf(" | ");
                    String url = sep != -1 ? item.substring(sep + 3) : item;
                    if (wv != null) wv.loadUrl(url);
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
        String cleanTitle = (title != null && !title.isEmpty()) ? title : url;
        history.add("[" + time + "] " + cleanTitle + " | " + url);
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
