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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String DEV_NAME = "Shah Alam Laskar";
    private static final String DEV_EMAIL = "shahalamlaskar.official@gmail.com";
    private static final String DEV_GITHUB = "https://github.com/shahalamlaskarofficial";
    private static final String APP_VERSION = "4.5";
    private static final int MAX_SYNCED_DOMAINS = 175000;

    // Refined Professional Theme Keys
    private static final String THEME_OBSIDIAN = "obsidian";
    private static final String THEME_AMOLED = "amoled";
    private static final String THEME_SLATE = "slate";
    private static final String THEME_INDIGO = "indigo";
    private static final String THEME_FOREST = "forest";
    private static final String THEME_LIGHT = "light";

    private static class QueueCardRefs {
        TextView tvName;
        ProgressBar pb;
        TextView tvStatus;
        int renderedStatus;
    }

    private static class TempMailItem {
        String id;
        String subject;
        String from;
        String excerpt;
        String inlineBody;
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
                    sessionBlockedCount++;
                    pageBlockedCount++;
                    return true;
                } else if (kind == 2) {
                    trackersBlockedCount++;
                    sessionBlockedCount++;
                    pageBlockedCount++;
                    return true;
                }
            } catch (Exception ignored) {}
            return false;
        }

        @JavascriptInterface
        public void reportDomAdRemoved() {
            adsBlockedCount++;
            sessionBlockedCount++;
            pageBlockedCount++;
        }

        @JavascriptInterface
        public void openCustomizePage() {
            uiHandler.post(MainActivity.this::showCustomizeThemeSheet);
        }

        @JavascriptInterface
        public void openTempMailPage() {
            uiHandler.post(MainActivity.this::showTempMailSheet);
        }

        @JavascriptInterface
        public void openShieldPage() {
            uiHandler.post(MainActivity.this::showShieldSheet);
        }

        @JavascriptInterface
        public void openDownloadsPage() {
            uiHandler.post(MainActivity.this::showDownloadManagerSheet);
        }

        @JavascriptInterface
        public void saveBlobData(String dataUrl, String fileName, String mimeType) {
            if (dataUrl == null || dataUrl.isEmpty()) return;
            try {
                int comma = dataUrl.indexOf(',');
                String b64 = (comma != -1) ? dataUrl.substring(comma + 1) : dataUrl;
                String detectedMime = mimeType;
                if ((detectedMime == null || detectedMime.isEmpty()) && comma > 5 && dataUrl.startsWith("data:")) {
                    detectedMime = dataUrl.substring(5, comma).split(";")[0];
                }
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                DownloadService.saveBytesDirectly(MainActivity.this, bytes, fileName, detectedMime);
                updateDownloadDockBadge();
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Could not save blob file", Toast.LENGTH_SHORT).show());
            }
        }

        @JavascriptInterface
        public void reportBlobError(String err) {
            uiHandler.post(() -> Toast.makeText(MainActivity.this,
                    "Blob download failed: " + err, Toast.LENGTH_SHORT).show());
        }
    }

    private FrameLayout webContainer;
    private EditText etUrl, etFind;
    private ProgressBar progressBar;
    private Button btnShield, btnTabs, btnMenu;
    private Button btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads;
    private LinearLayout rootLayout, topBar, bottomBar, layoutFind;
    private View topDivider, bottomDivider;

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
    private boolean isNoJsBypassActive = false;
    private String currentTheme = THEME_OBSIDIAN;
    private int currentBarHeightDp = 52;
    private int textZoomPercent = 100;
    private String defaultUserAgent;

    private String searchEngineName = "Google";
    private String searchEngineUrl = "https://www.google.com/search?q=";

    private int adsBlockedCount = 0;
    private int trackersBlockedCount = 0;
    private int sessionBlockedCount = 0;
    private int pageBlockedCount = 0;
    private Set<String> allowlistDomains = new HashSet<>();

    private volatile long[] adHostHashes = new long[0];
    private volatile long[] trackerHostHashes = new long[0];
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> wallpaperPickerLauncher;

    private static final Set<String> SAFE_MAIN_DOMAINS = new HashSet<>(Arrays.asList(
        "nova.browser", "google.com", "www.google.com", "youtube.com", "m.youtube.com",
        "www.youtube.com", "open.spotify.com", "github.com", "chatgpt.com",
        "wikipedia.org", "reddit.com", "testfile.org", "fast.com",
        "adblock.turtlecute.org", "canyoublockit.com", "superadblocktest.com",
        "api.tempmail.lol", "api.guerrillamail.com", "api.mail.gw", "api.mail.tm"
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
        currentTheme = prefs.getString("nova_theme_v45", THEME_OBSIDIAN);
        allowlistDomains = new HashSet<>(prefs.getStringSet("allowlist", new HashSet<>()));

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
        topDivider = findViewById(R.id.topDivider);
        bottomDivider = findViewById(R.id.bottomDivider);
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

        applyCleanTheme();
        createNewTab("nova://home");

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUserInput(etUrl.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnShield.setOnClickListener(v -> showShieldSheet());
        btnTabs.setOnClickListener(v -> showTabSwitcherSheet());
        btnMenu.setOnClickListener(v -> showMainBottomSheet());

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
                if (url == null || url.startsWith("https://nova.browser")) loadNovaHome(wv);
                else wv.reload();
            }
        });

        btnHome.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) {
                resetTabNoJsMode(wv);
                loadNovaHome(wv);
            }
        });

        btnBookmarks.setOnClickListener(v -> showBookmarksSheet());
        btnDownloads.setOnClickListener(v -> showDownloadManagerSheet());

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
                            "Downloads remain active in the background",
                            Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true);
                } else {
                    finish();
                }
            }
        });
    }

    // =========================================================================
    // CLEAN DESIGN SYSTEM (HIGH CONTRAST, ZERO EMOJIS, NATIVE BOTTOM SHEETS)
    // =========================================================================

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private boolean isLightTheme() {
        return THEME_LIGHT.equals(currentTheme);
    }

    private String getAccentHex() {
        switch (currentTheme) {
            case THEME_AMOLED: return "#38BDF8";
            case THEME_SLATE: return "#60A5FA";
            case THEME_INDIGO: return "#818CF8";
            case THEME_FOREST: return "#34D399";
            case THEME_LIGHT: return "#2563EB";
            default: return "#38BDF8";
        }
    }

    private int colCanvas() {
        if (isLightTheme()) return Color.parseColor("#F8FAFC");
        switch (currentTheme) {
            case THEME_AMOLED: return Color.parseColor("#000000");
            case THEME_SLATE: return Color.parseColor("#0F172A");
            case THEME_INDIGO: return Color.parseColor("#0C0D1D");
            case THEME_FOREST: return Color.parseColor("#071310");
            default: return Color.parseColor("#0B0F17");
        }
    }

    private int colSurface() {
        if (isLightTheme()) return Color.parseColor("#FFFFFF");
        switch (currentTheme) {
            case THEME_AMOLED: return Color.parseColor("#0A0C10");
            case THEME_SLATE: return Color.parseColor("#162032");
            case THEME_INDIGO: return Color.parseColor("#14162B");
            case THEME_FOREST: return Color.parseColor("#0E1F1B");
            default: return Color.parseColor("#121826");
        }
    }

    private int colElevated() {
        if (isLightTheme()) return Color.parseColor("#F1F5F9");
        switch (currentTheme) {
            case THEME_AMOLED: return Color.parseColor("#151921");
            case THEME_SLATE: return Color.parseColor("#1E293B");
            case THEME_INDIGO: return Color.parseColor("#1D203D");
            case THEME_FOREST: return Color.parseColor("#163029");
            default: return Color.parseColor("#1A2234");
        }
    }

    private int colBorder() {
        return isLightTheme() ? Color.parseColor("#CBD5E1") : Color.parseColor("#243045");
    }

    private int colTextPrimary() {
        return isLightTheme() ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC");
    }

    private int colTextSecondary() {
        return isLightTheme() ? Color.parseColor("#475569") : Color.parseColor("#94A3B8");
    }

    private GradientDrawable makeRoundedBg(int fillColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) gd.setStroke(dp(strokeDp), strokeColor);
        return gd;
    }

    private GradientDrawable makeBottomSheetBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(colSurface());
        float r = dp(20);
        gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        gd.setStroke(dp(1), colBorder());
        return gd;
    }

    private Dialog createBottomSheetDialog(LinearLayout content, boolean fixedTallHeight) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackground(makeBottomSheetBg());
        sheet.setPadding(dp(16), dp(10), dp(16), dp(18));

        // Subtle drag handle
        View handle = new View(this);
        handle.setBackground(makeRoundedBg(colBorder(), Color.TRANSPARENT, 3, 0));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(dp(38), dp(4));
        hLp.gravity = Gravity.CENTER_HORIZONTAL;
        hLp.setMargins(0, 0, 0, dp(12));
        sheet.addView(handle, hLp);

        sheet.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                fixedTallHeight ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT,
                fixedTallHeight ? 1f : 0f));

        dialog.setContentView(sheet);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setGravity(Gravity.BOTTOM);
            int h = fixedTallHeight
                    ? (int) (getResources().getDisplayMetrics().heightPixels * 0.78f)
                    : ViewGroup.LayoutParams.WRAP_CONTENT;
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, h);
            win.setDimAmount(0.58f);
        }
        return dialog;
    }

    private LinearLayout makeSheetHeader(String title, String subtitle, Runnable onClose) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), 0, dp(2), dp(12));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(colTextPrimary());
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvTitle.setTextSize(16.5f);
        titles.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(subtitle);
            tvSub.setTextColor(colTextSecondary());
            tvSub.setTextSize(12f);
            tvSub.setSingleLine(true);
            tvSub.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            titles.addView(tvSub);
        }
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnX = makeSecondaryButton("Close", null);
        btnX.setOnClickListener(v -> onClose.run());
        row.addView(btnX, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        return row;
    }

    private LinearLayout makeCleanCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(makeRoundedBg(colElevated(), colBorder(), 12, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        return card;
    }

    private Button makePrimaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13f);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(Color.parseColor(isLightTheme() ? "#FFFFFF" : "#071019"));
        b.setBackground(makeRoundedBg(Color.parseColor(getAccentHex()), Color.TRANSPARENT, 10, 0));
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    private Button makeSecondaryButton(String text, String customTextHex) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12.5f);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(customTextHex != null ? Color.parseColor(customTextHex) : colTextPrimary());
        b.setBackground(makeRoundedBg(colElevated(), colBorder(), 10, 1));
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    private LinearLayout makeListRow(String title, String badgeText, String badgeColorHex, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(makeRoundedBg(Color.TRANSPARENT, Color.TRANSPARENT, 8, 0));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(colTextPrimary());
        tvTitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tvTitle.setTextSize(14f);
        row.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (badgeText != null && !badgeText.isEmpty()) {
            TextView tvBadge = new TextView(this);
            tvBadge.setText(badgeText);
            tvBadge.setTextColor(badgeColorHex != null ? Color.parseColor(badgeColorHex) : colTextSecondary());
            tvBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tvBadge.setTextSize(11.5f);
            tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
            tvBadge.setBackground(makeRoundedBg(colElevated(), colBorder(), 6, 1));
            row.addView(tvBadge);
        }

        row.setOnClickListener(v -> onClick.run());
        return row;
    }

    private View makeHairline() {
        View v = new View(this);
        v.setBackgroundColor(colBorder());
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    private TextView makeSmallLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(colTextSecondary());
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextSize(11f);
        tv.setAllCaps(true);
        tv.setLetterSpacing(0.06f);
        tv.setPadding(dp(2), dp(4), 0, dp(6));
        return tv;
    }

    private void applyCleanTheme() {
        String accent = getAccentHex();
        int barBg = isIncognito
                ? Color.parseColor(isLightTheme() ? "#EFE8F8" : "#1A1228")
                : colSurface();

        rootLayout.setBackgroundColor(colCanvas());
        topBar.setBackgroundColor(barBg);
        bottomBar.setBackgroundColor(barBg);
        layoutFind.setBackgroundColor(barBg);
        topDivider.setBackgroundColor(colBorder());
        bottomDivider.setBackgroundColor(colBorder());

        etUrl.setBackground(makeRoundedBg(colElevated(), colBorder(), 18, 1));
        etUrl.setTextColor(colTextPrimary());
        etUrl.setHintTextColor(colTextSecondary());

        etFind.setBackground(makeRoundedBg(colElevated(), colBorder(), 10, 1));
        etFind.setTextColor(colTextPrimary());
        etFind.setHintTextColor(colTextSecondary());

        btnShield.setBackground(makeRoundedBg(colElevated(), colBorder(), 10, 1));
        btnShield.setTextColor(Color.parseColor(isAdBlockEnabled ? accent : "#94A3B8"));

        btnTabs.setBackground(makeRoundedBg(Color.TRANSPARENT, colTextSecondary(), 8, 1));
        btnTabs.setTextColor(colTextPrimary());

        btnMenu.setBackground(null);
        btnMenu.setTextColor(colTextPrimary());

        int dockColor = colTextSecondary();
        for (Button dockBtn : new Button[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
            dockBtn.setBackground(null);
            dockBtn.setTextColor(dockColor);
        }
        btnHome.setTextColor(Color.parseColor(accent));
        updateDownloadDockBadge();
    }

    private void animateBarsToDp(int targetDp) {
        if (isFullScreen || currentBarHeightDp == targetDp) return;
        currentBarHeightDp = targetDp;

        final int targetTopPx = dp(targetDp);
        final int targetBotPx = targetDp == 0 ? 0 : dp(targetDp >= 50 ? 46 : 42);
        final int startTopPx = topBar.getLayoutParams().height;
        final int startBotPx = bottomBar.getLayoutParams().height;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(200);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(va -> {
            float f = (float) va.getAnimatedValue();
            int curTop = Math.round(startTopPx + (targetTopPx - startTopPx) * f);
            int curBot = Math.round(startBotPx + (targetBotPx - startBotPx) * f);

            ViewGroup.LayoutParams topLp = topBar.getLayoutParams();
            topLp.height = curTop;
            topBar.setLayoutParams(topLp);
            topBar.setAlpha(targetDp == 0 ? (1f - f) : 1f);

            ViewGroup.LayoutParams botLp = bottomBar.getLayoutParams();
            botLp.height = curBot;
            bottomBar.setLayoutParams(botLp);
            bottomBar.setAlpha(targetDp == 0 ? (1f - f) : 1f);
        });
        anim.start();
    }

    private void updateDownloadDockBadge() {
        int activeCount = DownloadService.getActiveCount();
        uiHandler.post(() -> {
            if (activeCount > 0) {
                btnDownloads.setText("↓" + activeCount);
                btnDownloads.setTextSize(13f);
                btnDownloads.setTextColor(Color.parseColor("#22C55E"));
            } else {
                btnDownloads.setText("↓");
                btnDownloads.setTextSize(17f);
                btnDownloads.setTextColor(colTextSecondary());
            }
        });
    }

    // =========================================================================
    // CLEAN MAIN MENU BOTTOM SHEET & SEPARATE SETTINGS SHEET
    // =========================================================================

    private void showMainBottomSheet() {
        WebView wv = getCurrentWebView();
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        final Dialog dialog = createBottomSheetDialog(body, false);

        // Compact 4-action horizontal strip
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qrLp.setMargins(0, 0, 0, dp(12));

        String[] qLabels = {"New Tab", "Shield", "Downloads", "Temp Mail"};
        Runnable[] qActs = {
            () -> { dialog.dismiss(); createNewTab("nova://home"); },
            () -> { dialog.dismiss(); showShieldSheet(); },
            () -> { dialog.dismiss(); showDownloadManagerSheet(); },
            () -> { dialog.dismiss(); showTempMailSheet(); }
        };
        for (int i = 0; i < qLabels.length; i++) {
            Button b = makeSecondaryButton(qLabels[i], i == 1 && isAdBlockEnabled ? getAccentHex() : null);
            b.setTextSize(12f);
            b.setPadding(dp(4), 0, dp(4), 0);
            final int idx = i;
            b.setOnClickListener(v -> qActs[idx].run());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            if (i < qLabels.length - 1) lp.setMargins(0, 0, dp(6), 0);
            quickRow.addView(b, lp);
        }
        body.addView(quickRow, qrLp);

        ScrollView sc = new ScrollView(this);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(makeRoundedBg(colElevated(), colBorder(), 12, 1));
        sc.addView(list);

        list.addView(makeListRow("Bypass Login / Paywall", isNoJsBypassActive ? "NO-JS" : null, "#F59E0B",
                () -> { dialog.dismiss(); showLoginBypassSheet(wv); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Desktop Site", isDesktopMode ? "ON" : "OFF", isDesktopMode ? getAccentHex() : null,
                () -> { dialog.dismiss(); toggleDesktopMode(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Private Browsing", isIncognito ? "ON" : "OFF", isIncognito ? "#C084FC" : null,
                () -> { dialog.dismiss(); toggleIncognitoMode(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Find in Page", null, null, () -> {
            dialog.dismiss();
            layoutFind.setVisibility(View.VISIBLE);
            etFind.requestFocus();
        }));
        list.addView(makeHairline());
        list.addView(makeListRow("Reader View", null, null,
                () -> { dialog.dismiss(); applyCleanReaderMode(wv); }));
        list.addView(makeHairline());
        list.addView(makeListRow("History", null, null,
                () -> { dialog.dismiss(); showHistorySheet(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Share / Copy Link", null, null,
                () -> { dialog.dismiss(); shareOrCopyUrl(wv); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Settings & Appearance", "More", getAccentHex(),
                () -> { dialog.dismiss(); showSettingsSheet(); }));

        body.addView(sc);
        dialog.show();
    }

    private void showSettingsSheet() {
        WebView wv = getCurrentWebView();
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        final Dialog dialog = createBottomSheetDialog(body, false);
        body.addView(makeSheetHeader("Settings", "NOVA Browser v" + APP_VERSION, dialog::dismiss));

        ScrollView sc = new ScrollView(this);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(makeRoundedBg(colElevated(), colBorder(), 12, 1));
        sc.addView(list);

        list.addView(makeListRow("Appearance & Wallpaper", currentTheme.toUpperCase(Locale.US), getAccentHex(),
                () -> { dialog.dismiss(); showCustomizeThemeSheet(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Search Engine", searchEngineName, null,
                () -> { dialog.dismiss(); showSearchEngineDialog(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Text Zoom", textZoomPercent + "%", null,
                () -> { dialog.dismiss(); showZoomDialog(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Data Saver (Block Images)", isDataSaver ? "ON" : "OFF", isDataSaver ? getAccentHex() : null, () -> {
            dialog.dismiss();
            isDataSaver = !isDataSaver;
            for (WebView tab : tabs) tab.getSettings().setBlockNetworkImage(isDataSaver);
            if (wv != null) wv.reload();
            Toast.makeText(this, "Data Saver " + (isDataSaver ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        }));
        list.addView(makeHairline());
        list.addView(makeListRow("Full-Screen Mode", isFullScreen ? "ON" : "OFF", isFullScreen ? getAccentHex() : null,
                () -> { dialog.dismiss(); toggleFullScreen(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Memory & Battery", getRamUsageMb(), null,
                () -> { dialog.dismiss(); showPerformanceSheet(); }));
        list.addView(makeHairline());
        list.addView(makeListRow("Translate Current Page", null, null, () -> {
            dialog.dismiss();
            if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(wv.getUrl()));
            }
        }));
        list.addView(makeHairline());
        list.addView(makeListRow("Clear Browsing Data", null, "#F87171",
                () -> { dialog.dismiss(); clearBrowserData(wv); }));
        list.addView(makeHairline());
        list.addView(makeListRow("About & Developer", "v" + APP_VERSION, null,
                () -> { dialog.dismiss(); showAboutSheet(); }));

        body.addView(sc);
        dialog.show();
    }

    // =========================================================================
    // HONEST, TRANSPARENT PRIVACY SHIELD SHEET
    // =========================================================================

    private void showShieldSheet() {
        WebView wv = getCurrentWebView();
        String currentUrl = (wv != null && wv.getUrl() != null) ? wv.getUrl() : "nova://home";
        String host = Uri.parse(currentUrl).getHost();
        boolean isHome = host == null || host.contains("nova.browser");
        boolean isHttps = currentUrl.startsWith("https://");
        int rulesLoaded = adHostHashes.length + trackerHostHashes.length;

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final Dialog dialog = createBottomSheetDialog(body, false);

        String sub = isHome
                ? "Local Home Page"
                : host + "  •  " + (isHttps ? "HTTPS Encrypted" : "Unencrypted HTTP");
        body.addView(makeSheetHeader("Privacy Shield", sub, dialog::dismiss));

        // 3 Verifiable Real Metrics Row
        LinearLayout metricsRow = new LinearLayout(this);
        metricsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.setMargins(0, 0, 0, dp(12));

        metricsRow.addView(makeMetricTile(String.valueOf(pageBlockedCount), "This Page", getAccentHex()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        metricsRow.addView(makeMetricTile(String.valueOf(sessionBlockedCount), "This Session", null),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        metricsRow.addView(makeMetricTile(String.valueOf(adsBlockedCount + trackersBlockedCount), "Total Blocked", null),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(metricsRow, mLp);

        // Controls List
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setBackground(makeRoundedBg(colElevated(), colBorder(), 12, 1));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, 0, 0, dp(10));

        controls.addView(makeListRow("Network Ad & Tracker Filter",
                isAdBlockEnabled ? "ACTIVE" : "PAUSED",
                isAdBlockEnabled ? "#22C55E" : "#F87171", () -> {
            isAdBlockEnabled = !isAdBlockEnabled;
            prefs.edit().putBoolean("adblock_enabled", isAdBlockEnabled).apply();
            applyCleanTheme();
            if (wv != null) wv.reload();
            dialog.dismiss();
        }));
        controls.addView(makeHairline());
        controls.addView(makeListRow("Interstitial & Pop-Up Blocker",
                isAggressiveInterstitialBlock ? "ON" : "OFF",
                isAggressiveInterstitialBlock ? getAccentHex() : null, () -> {
            isAggressiveInterstitialBlock = !isAggressiveInterstitialBlock;
            prefs.edit().putBoolean("aggressive_interstitial", isAggressiveInterstitialBlock).apply();
            if (wv != null) wv.reload();
            dialog.dismiss();
        }));
        if (!isHome) {
            boolean trusted = allowlistDomains.contains(host);
            controls.addView(makeHairline());
            controls.addView(makeListRow("Site Allowlist (" + host + ")",
                    trusted ? "TRUSTED" : "FILTERED",
                    trusted ? "#F59E0B" : null, () -> {
                if (trusted) allowlistDomains.remove(host);
                else allowlistDomains.add(host);
                prefs.edit().putStringSet("allowlist", allowlistDomains).apply();
                if (wv != null) wv.reload();
                dialog.dismiss();
            }));
        }
        body.addView(controls, cLp);

        // Transparent Filter Info Card
        LinearLayout infoCard = makeCleanCard();
        TextView tvInfo = new TextView(this);
        tvInfo.setText("Active Rules: " + String.format(Locale.US, "%,d", rulesLoaded) + " domains (FNV-1a 64-bit index)\n"
                + "Sources: StevenBlack Hosts, HaGeZi Light, AdAway, Peter Lowe\n"
                + "Third-party cookies: Disabled by default");
        tvInfo.setTextColor(colTextSecondary());
        tvInfo.setTextSize(11.8f);
        tvInfo.setLineSpacing(dp(2), 1f);
        infoCard.addView(tvInfo);
        body.addView(infoCard);

        Button btnSync = makeSecondaryButton("Refresh Filter Lists", getAccentHex());
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Updating blocklists in background...", Toast.LENGTH_SHORT).show();
            loadCachedAndSyncFilterLists(true);
            dialog.dismiss();
        });
        body.addView(btnSync, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        dialog.show();
    }

    private LinearLayout makeMetricTile(String val, String label, String valueHex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(6), dp(10), dp(6), dp(10));
        box.setBackground(makeRoundedBg(colElevated(), colBorder(), 10, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), 0, dp(3), 0);

        TextView tvV = new TextView(this);
        tvV.setText(val);
        tvV.setTextColor(valueHex != null ? Color.parseColor(valueHex) : colTextPrimary());
        tvV.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvV.setTextSize(16f);
        tvV.setGravity(Gravity.CENTER);
        box.addView(tvV);

        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextColor(colTextSecondary());
        tvL.setTextSize(11f);
        tvL.setGravity(Gravity.CENTER);
        tvL.setPadding(0, dp(2), 0, 0);
        box.addView(tvL);

        LinearLayout holder = new LinearLayout(this);
        holder.addView(box, lp);
        return holder;
    }

    // =========================================================================
    // UPGRADED 4-MODE LOGIN / SIGN-UP WALL BYPASS ENGINE
    // =========================================================================

    private void resetTabNoJsMode(WebView wv) {
        if (isNoJsBypassActive && wv != null) {
            isNoJsBypassActive = false;
            wv.getSettings().setJavaScriptEnabled(true);
        }
    }

    private void showLoginBypassSheet(WebView wv) {
        if (wv == null || wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser")) {
            Toast.makeText(this, "Open a website first to bypass a login wall", Toast.LENGTH_SHORT).show();
            return;
        }
        final String currentUrl = wv.getUrl();

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final Dialog dialog = createBottomSheetDialog(body, false);
        body.addView(makeSheetHeader("Bypass Login & Paywalls", Uri.parse(currentUrl).getHost(), dialog::dismiss));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(makeRoundedBg(colElevated(), colBorder(), 12, 1));

        list.addView(makeListRow("1. Remove Modals, Blur & Unlock Scroll", "Persistent", getAccentHex(), () -> {
            dialog.dismiss();
            injectPersistentWallBypass(wv);
            Toast.makeText(this, "Overlay & scroll-lock killer active on this page", Toast.LENGTH_SHORT).show();
        }));
        list.addView(makeHairline());

        list.addView(makeListRow("2. Reload via Google Referer (Fresh Session)", "Article Wall", null, () -> {
            dialog.dismiss();
            CookieManager.getInstance().removeSessionCookies(null);
            CookieManager.getInstance().flush();
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.google.com/");
            wv.loadUrl(currentUrl, headers);
            Toast.makeText(this, "Reloading with Google referer...", Toast.LENGTH_SHORT).show();
        }));
        list.addView(makeHairline());

        list.addView(makeListRow("3. No-JavaScript Mode (Defeats JS Walls)",
                isNoJsBypassActive ? "ON (Tap to Restore)" : "OFF",
                isNoJsBypassActive ? "#F59E0B" : null, () -> {
            dialog.dismiss();
            isNoJsBypassActive = !isNoJsBypassActive;
            wv.getSettings().setJavaScriptEnabled(!isNoJsBypassActive);
            wv.reload();
            Toast.makeText(this, isNoJsBypassActive
                    ? "JavaScript disabled for this page"
                    : "JavaScript restored", Toast.LENGTH_SHORT).show();
        }));
        list.addView(makeHairline());

        list.addView(makeListRow("4. Clean Reader View (Extract Text)", "Instant", null, () -> {
            dialog.dismiss();
            applyCleanReaderMode(wv);
        }));

        body.addView(list);
        dialog.show();
    }

    private void injectPersistentWallBypass(WebView wv) {
        String js = "javascript:(function(){"
            + "var old=document.getElementById('__nova_bypass_css');if(old)old.remove();"
            + "var st=document.createElement('style');st.id='__nova_bypass_css';"
            + "st.innerHTML='html,body{overflow:auto!important;overflow-y:auto!important;position:static!important;height:auto!important;max-height:none!important;pointer-events:auto!important;user-select:auto!important;}"
            + ".modal-backdrop,.paywall,.paywall-overlay,.login-wall,.signup-wall,.regwall,[class*=\"paywall\"],[id*=\"paywall\"],[class*=\"login-modal\"],[class*=\"signup-modal\"],[class*=\"auth-modal\"],[class*=\"gate-overlay\"]{display:none!important;visibility:hidden!important;pointer-events:none!important;}';"
            + "(document.head||document.documentElement).appendChild(st);"
            + "function sweep(){"
            + "try{"
            + "document.documentElement.style.setProperty('overflow','auto','important');"
            + "document.body.style.setProperty('overflow','auto','important');"
            + "document.body.style.setProperty('position','static','important');"
            + "document.body.classList.remove('modal-open','no-scroll','overflow-hidden','noscroll');"
            + "var vw=window.innerWidth,vh=window.innerHeight;"
            + "document.querySelectorAll('div,section,aside,dialog').forEach(function(el){"
            + "if(!el||el===document.body||el===document.documentElement)return;"
            + "var cs=window.getComputedStyle(el);"
            + "if(cs.filter&&cs.filter.indexOf('blur')!==-1){el.style.setProperty('filter','none','important');}"
            + "if(cs.webkitFilter&&cs.webkitFilter.indexOf('blur')!==-1){el.style.setProperty('-webkit-filter','none','important');}"
            + "if(cs.maxHeight&&cs.maxHeight!=='none'&&parseInt(cs.maxHeight)>150&&cs.overflow==='hidden'){el.style.setProperty('max-height','none','important');el.style.setProperty('overflow','visible','important');}"
            + "var pos=cs.position;var zi=parseInt(cs.zIndex)||0;"
            + "var idc=((el.id||'')+' '+(el.className||'')+' '+(el.getAttribute('role')||'')).toLowerCase();"
            + "var txt=(el.innerText||'').toLowerCase();"
            + "var isModal=(el.getAttribute('aria-modal')==='true'||el.getAttribute('role')==='dialog'||el.tagName==='DIALOG');"
            + "var isAuth=/log ?in|sign ?in|sign ?up|create account|continue with google|register to|subscribe to continue|member-only/.test(txt)&&txt.length<700;"
            + "var isWallName=/login|signin|signup|register|paywall|regwall|auth|backdrop|overlay|modal|gate/.test(idc);"
            + "var rect=el.getBoundingClientRect();"
            + "var coversBig=(rect.width>=vw*0.65&&rect.height>=vh*0.35);"
            + "if((pos==='fixed'||pos==='sticky'||pos==='absolute')&&(isModal||(zi>=40&&(isAuth||isWallName))||(coversBig&&zi>=10&&isAuth))){"
            + "if(el.tagName!=='HEADER'&&el.tagName!=='NAV'){el.remove();}}"
            + "});}catch(e){}}"
            + "sweep();"
            + "if(!window.__novaBypassObs){"
            + "window.__novaBypassObs=new MutationObserver(sweep);"
            + "window.__novaBypassObs.observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style']});"
            + "setInterval(sweep,600);}"
            + "})();";
        wv.evaluateJavascript(js, null);
    }

    private void applyCleanReaderMode(WebView wv) {
        if (wv == null) return;
        String bg = isLightTheme() ? "#FFFFFF" : "#0B0F17";
        String fg = isLightTheme() ? "#0F172A" : "#E2E8F0";
        String js = "javascript:(function(){"
            + "try{"
            + "var root=document.querySelector('article,main,[role=\"main\"],.post-content,.article-body')||document.body;"
            + "var title=document.title||'Reader View';"
            + "var blocks=root.querySelectorAll('h1,h2,h3,p,li,blockquote');"
            + "var out='<h1 style=\"font-size:24px;line-height:1.3;margin-bottom:18px;\">'+title+'</h1>';"
            + "var count=0;"
            + "blocks.forEach(function(b){"
            + "var t=(b.innerText||'').trim();"
            + "if(t.length>25){"
            + "var tag=b.tagName.toLowerCase();"
            + "if(tag==='h1'||tag==='h2'||tag==='h3'){out+='<h2 style=\"font-size:19px;margin:20px 0 8px;\">'+t+'</h2>';}"
            + "else{out+='<p style=\"margin:0 0 14px;\">'+t+'</p>';}count++;}"
            + "});"
            + "if(count<2){"
            + "document.body.style.maxWidth='680px';document.body.style.margin='0 auto';document.body.style.padding='20px';"
            + "document.body.style.backgroundColor='" + bg + "';document.body.style.color='" + fg + "';"
            + "}else{"
            + "document.body.innerHTML='<div style=\"max-width:680px;margin:0 auto;padding:24px 18px 60px;font-family:Georgia,serif;font-size:18px;line-height:1.72;background:" + bg + ";color:" + fg + ";\">'+out+'</div>';"
            + "document.documentElement.style.background='" + bg + "';document.body.style.background='" + bg + "';document.body.style.margin='0';document.body.style.overflow='auto';}"
            + "}catch(e){}"
            + "})();";
        wv.evaluateJavascript(js, null);
        Toast.makeText(this, "Reader View applied", Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // RELIABLE 4-ENGINE TEMP MAIL (TEMPMAIL.LOL v2 -> MAIL.TM -> MAIL.GW -> GUERRILLA)
    // =========================================================================

    private void showTempMailSheet() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final Dialog dialog = createBottomSheetDialog(body, true);

        body.addView(makeSheetHeader("Temporary Email", "Disposable inbox with automatic provider failover", dialog::dismiss));

        LinearLayout addrCard = makeCleanCard();
        TextView tvProv = new TextView(this);
        String prov = prefs.getString("tm_provider_v2", "");
        tvProv.setText(prov.isEmpty() ? "ACTIVE ADDRESS" : "ACTIVE ADDRESS  •  " + prov.toUpperCase(Locale.US));
        tvProv.setTextColor(colTextSecondary());
        tvProv.setTextSize(10.5f);
        tvProv.setLetterSpacing(0.05f);
        addrCard.addView(tvProv);

        TextView tvEmail = new TextView(this);
        String savedEmail = prefs.getString("tm_addr_v2", "");
        tvEmail.setText(savedEmail.isEmpty() ? "Generating address..." : savedEmail);
        tvEmail.setTextColor(Color.parseColor(getAccentHex()));
        tvEmail.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvEmail.setTextSize(15f);
        tvEmail.setPadding(0, dp(4), 0, dp(10));
        addrCard.addView(tvEmail);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCopy = makePrimaryButton("Copy Address");
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        cLp.setMargins(0, 0, dp(8), 0);
        btnCopy.setOnClickListener(v -> {
            String em = prefs.getString("tm_addr_v2", "");
            if (!em.isEmpty()) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("Temp Mail", em));
                    Toast.makeText(this, "Copied: " + em, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnRow.addView(btnCopy, cLp);

        Button btnNew = makeSecondaryButton("New Address", null);
        btnRow.addView(btnNew, new LinearLayout.LayoutParams(0, dp(36), 1f));
        addrCard.addView(btnRow);
        body.addView(addrCard);

        LinearLayout inboxHdr = new LinearLayout(this);
        inboxHdr.setOrientation(LinearLayout.HORIZONTAL);
        inboxHdr.setGravity(Gravity.CENTER_VERTICAL);
        inboxHdr.setPadding(dp(2), dp(2), dp(2), dp(8));

        TextView tvInboxTitle = new TextView(this);
        tvInboxTitle.setText("INBOX MESSAGES");
        tvInboxTitle.setTextColor(colTextSecondary());
        tvInboxTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvInboxTitle.setTextSize(11f);
        tvInboxTitle.setLetterSpacing(0.06f);
        inboxHdr.addView(tvInboxTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnRefresh = makeSecondaryButton("Refresh", getAccentHex());
        inboxHdr.addView(btnRefresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        body.addView(inboxHdr);

        ScrollView scInbox = new ScrollView(this);
        scInbox.setVerticalScrollBarEnabled(false);
        LinearLayout inboxContainer = new LinearLayout(this);
        inboxContainer.setOrientation(LinearLayout.VERTICAL);
        scInbox.addView(inboxContainer);
        body.addView(scInbox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Runnable refreshAction = () -> fetchTempMailInbox(tvEmail, tvProv, inboxContainer);
        btnRefresh.setOnClickListener(v -> refreshAction.run());
        btnNew.setOnClickListener(v -> createFreshTempMailAccount(tvEmail, tvProv, inboxContainer));

        if (savedEmail.isEmpty() || prefs.getString("tm_token_v2", "").isEmpty()) {
            createFreshTempMailAccount(tvEmail, tvProv, inboxContainer);
        } else {
            refreshAction.run();
        }

        dialog.show();
    }

    private String httpMailApi(String urlStr, String method, String jsonBody, String bearerToken, String acceptHeader) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", acceptHeader != null ? acceptHeader : "application/json");
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

    private void createFreshTempMailAccount(TextView tvEmail, TextView tvProv, LinearLayout inboxContainer) {
        uiHandler.post(() -> {
            tvEmail.setText("Connecting...");
            inboxContainer.removeAllViews();
        });

        new Thread(() -> {
            // Engine 1: TempMail.lol v2 (Fast, rotating domains, zero signup)
            try {
                String res = httpMailApi("https://api.tempmail.lol/v2/inbox/create", "POST", "{}", null, "application/json");
                JSONObject obj = new JSONObject(res);
                String email = obj.getString("address");
                String token = obj.getString("token");
                saveTempMailSession("tempmail.lol", email, token, tvEmail, tvProv, inboxContainer);
                return;
            } catch (Exception ignored) {}

            // Engine 2 & 3: Mail.tm & Mail.gw
            String[] bases = {"https://api.mail.tm", "https://api.mail.gw"};
            String[] names = {"mail.tm", "mail.gw"};
            for (int i = 0; i < bases.length; i++) {
                try {
                    String domStr = httpMailApi(bases[i] + "/domains", "GET", null, null, "application/json").trim();
                    JSONArray arr = domStr.startsWith("[")
                            ? new JSONArray(domStr)
                            : new JSONObject(domStr).getJSONArray("hydra:member");
                    String domain = arr.getJSONObject(0).getString("domain");

                    String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
                    Random rnd = new Random();
                    StringBuilder user = new StringBuilder("nv");
                    for (int k = 0; k < 9; k++) user.append(chars.charAt(rnd.nextInt(chars.length())));
                    String email = user + "@" + domain;
                    String pass = "Nv!" + (100000 + rnd.nextInt(899999)) + "aZ";

                    JSONObject creds = new JSONObject();
                    creds.put("address", email);
                    creds.put("password", pass);

                    httpMailApi(bases[i] + "/accounts", "POST", creds.toString(), null, "application/json");
                    String tokJson = httpMailApi(bases[i] + "/token", "POST", creds.toString(), null, "application/json");
                    String token = new JSONObject(tokJson).getString("token");

                    saveTempMailSession(names[i], email, token, tvEmail, tvProv, inboxContainer);
                    return;
                } catch (Exception ignored) {}
            }

            // Engine 4: Guerrilla Mail fallback
            try {
                String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_address&lang=en", "GET", null, null, "application/json");
                JSONObject obj = new JSONObject(res);
                String email = obj.getString("email_addr");
                String sid = obj.getString("sid_token");
                saveTempMailSession("guerrillamail", email, sid, tvEmail, tvProv, inboxContainer);
                return;
            } catch (Exception ignored) {}

            uiHandler.post(() -> {
                tvEmail.setText("Connection failed — tap New Address");
                tvProv.setText("OFFLINE OR RATE-LIMITED");
            });
        }).start();
    }

    private void saveTempMailSession(String provider, String email, String token,
                                     TextView tvEmail, TextView tvProv, LinearLayout inboxContainer) {
        prefs.edit()
            .putString("tm_provider_v2", provider)
            .putString("tm_addr_v2", email)
            .putString("tm_token_v2", token)
            .apply();
        uiHandler.post(() -> {
            tvEmail.setText(email);
            tvProv.setText("ACTIVE ADDRESS  •  " + provider.toUpperCase(Locale.US));
            fetchTempMailInbox(tvEmail, tvProv, inboxContainer);
        });
    }

    private void fetchTempMailInbox(TextView tvEmail, TextView tvProv, LinearLayout inboxContainer) {
        String provider = prefs.getString("tm_provider_v2", "tempmail.lol");
        String token = prefs.getString("tm_token_v2", "");
        if (token.isEmpty()) return;

        uiHandler.post(() -> {
            inboxContainer.removeAllViews();
            TextView tvLoad = new TextView(this);
            tvLoad.setText("Checking inbox...");
            tvLoad.setTextColor(colTextSecondary());
            tvLoad.setTextSize(13f);
            tvLoad.setPadding(dp(4), dp(12), dp(4), dp(12));
            inboxContainer.addView(tvLoad);
        });

        new Thread(() -> {
            try {
                final List<TempMailItem> items = new ArrayList<>();
                if ("tempmail.lol".equals(provider)) {
                    String res = httpMailApi("https://api.tempmail.lol/v2/inbox?token=" + Uri.encode(token), "GET", null, null, "application/json");
                    JSONObject obj = new JSONObject(res);
                    if (obj.optBoolean("expired", false)) {
                        throw new IOException("EXPIRED");
                    }
                    JSONArray arr = obj.optJSONArray("emails");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject m = arr.getJSONObject(i);
                            TempMailItem item = new TempMailItem();
                            item.id = String.valueOf(i);
                            item.subject = m.optString("subject", "(No Subject)");
                            item.from = m.optString("from", "Sender");
                            String plain = m.optString("body", "");
                            if (plain.isEmpty()) {
                                String h = m.optString("html", "");
                                plain = stripHtml(h);
                            }
                            item.inlineBody = plain;
                            item.excerpt = plain.length() > 90 ? plain.substring(0, 90).replace('\n', ' ') + "..." : plain.replace('\n', ' ');
                            items.add(item);
                        }
                    }
                } else if ("guerrillamail".equals(provider)) {
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_list&offset=0&sid_token=" + Uri.encode(token), "GET", null, null, "application/json");
                    JSONArray list = new JSONObject(res).optJSONArray("list");
                    if (list != null) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject m = list.getJSONObject(i);
                            TempMailItem item = new TempMailItem();
                            item.id = m.optString("mail_id");
                            item.subject = m.optString("mail_subject", "(No Subject)");
                            item.from = m.optString("mail_from", "Sender");
                            item.excerpt = m.optString("mail_excerpt", "");
                            items.add(item);
                        }
                    }
                } else {
                    String base = "mail.gw".equals(provider) ? "https://api.mail.gw" : "https://api.mail.tm";
                    String res = httpMailApi(base + "/messages", "GET", null, token, "application/json").trim();
                    JSONArray msgs = res.startsWith("[")
                            ? new JSONArray(res)
                            : new JSONObject(res).getJSONArray("hydra:member");
                    for (int i = 0; i < msgs.length(); i++) {
                        JSONObject m = msgs.getJSONObject(i);
                        TempMailItem item = new TempMailItem();
                        item.id = m.getString("id");
                        item.subject = m.optString("subject", "(No Subject)");
                        item.from = m.has("from") ? m.getJSONObject("from").optString("address", "Sender") : "Sender";
                        item.excerpt = m.optString("intro", "");
                        items.add(item);
                    }
                }

                uiHandler.post(() -> {
                    inboxContainer.removeAllViews();
                    if (items.isEmpty()) {
                        LinearLayout empty = makeCleanCard();
                        TextView tvE = new TextView(this);
                        tvE.setText("No messages yet. Copy your address above, use it on a signup form, and tap Refresh to view incoming emails or verification codes.");
                        tvE.setTextColor(colTextSecondary());
                        tvE.setTextSize(12.5f);
                        empty.addView(tvE);
                        inboxContainer.addView(empty);
                        return;
                    }
                    for (TempMailItem m : items) {
                        LinearLayout card = makeCleanCard();
                        TextView tvS = new TextView(this);
                        tvS.setText(m.subject);
                        tvS.setTextColor(colTextPrimary());
                        tvS.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                        tvS.setTextSize(13.5f);
                        card.addView(tvS);

                        TextView tvF = new TextView(this);
                        tvF.setText(m.from + (m.excerpt.isEmpty() ? "" : "\n" + m.excerpt));
                        tvF.setTextColor(colTextSecondary());
                        tvF.setTextSize(12f);
                        tvF.setPadding(0, dp(3), 0, 0);
                        card.addView(tvF);

                        card.setOnClickListener(v -> openTempMailMessage(provider, m, token));
                        inboxContainer.addView(card);
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    inboxContainer.removeAllViews();
                    LinearLayout errCard = makeCleanCard();
                    TextView tvErr = new TextView(this);
                    tvErr.setText("Session expired or server unavailable. Tap New Address to generate a fresh mailbox.");
                    tvErr.setTextColor(Color.parseColor("#F59E0B"));
                    tvErr.setTextSize(12.5f);
                    errCard.addView(tvErr);
                    inboxContainer.addView(errCard);
                });
            }
        }).start();
    }

    private String stripHtml(String rawHtml) {
        if (rawHtml == null || rawHtml.isEmpty()) return "";
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                : Html.fromHtml(rawHtml).toString()).trim();
    }

    private void openTempMailMessage(String provider, TempMailItem item, String token) {
        if (item.inlineBody != null) {
            showTempMailReaderDialog(item.subject, item.from, item.inlineBody);
            return;
        }
        new Thread(() -> {
            try {
                String bodyText;
                if ("guerrillamail".equals(provider)) {
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=" + Uri.encode(item.id) + "&sid_token=" + Uri.encode(token), "GET", null, null, "application/json");
                    bodyText = stripHtml(new JSONObject(res).optString("mail_body", ""));
                } else {
                    String base = "mail.gw".equals(provider) ? "https://api.mail.gw" : "https://api.mail.tm";
                    String res = httpMailApi(base + "/messages/" + item.id, "GET", null, token, "application/json");
                    JSONObject obj = new JSONObject(res);
                    bodyText = obj.optString("text", "");
                    if (bodyText.isEmpty() && obj.has("html")) {
                        bodyText = stripHtml(obj.optString("html", ""));
                    }
                }
                final String finalBody = bodyText.isEmpty() ? "(Empty message)" : bodyText.trim();
                uiHandler.post(() -> showTempMailReaderDialog(item.subject, item.from, finalBody));
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "Could not load message body", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showTempMailReaderDialog(String subject, String from, String bodyText) {
        String detectedOtp = null;
        Matcher matcher = Pattern.compile("\\b(\\d{4,8})\\b").matcher(bodyText);
        if (matcher.find()) detectedOtp = matcher.group(1);
        final String otpCode = detectedOtp;

        AlertDialog.Builder b = new AlertDialog.Builder(this)
            .setTitle(subject)
            .setMessage("From: " + from + "\n\n" + bodyText)
            .setNegativeButton("Close", null);

        if (otpCode != null) {
            b.setPositiveButton("Copy Code (" + otpCode + ")", (d, w) -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("Verification Code", otpCode));
                    Toast.makeText(this, "Code copied: " + otpCode, Toast.LENGTH_SHORT).show();
                }
            });
            b.setNeutralButton("Copy Full Text", (d, w) -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("Temp Mail", bodyText));
                    Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            b.setPositiveButton("Copy Text", (d, w) -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("Temp Mail", bodyText));
                    Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
                }
            });
        }
        b.show();
    }

    // =========================================================================
    // APPEARANCE, THEMES & WALLPAPER BOTTOM SHEET
    // =========================================================================

    private void showCustomizeThemeSheet() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final Dialog dialog = createBottomSheetDialog(body, false);
        body.addView(makeSheetHeader("Appearance", "Select a color palette or homepage wallpaper", dialog::dismiss));

        body.addView(makeSmallLabel("Color Themes"));

        String[][] themes = {
            {"Obsidian Dark", THEME_OBSIDIAN, "#121826", "#F8FAFC"},
            {"AMOLED Black", THEME_AMOLED, "#000000", "#38BDF8"},
            {"Nordic Slate", THEME_SLATE, "#1E293B", "#F8FAFC"},
            {"Deep Indigo", THEME_INDIGO, "#1D203D", "#E0E7FF"},
            {"Pine Forest", THEME_FOREST, "#122B24", "#D1FAE5"},
            {"Clean Light", THEME_LIGHT, "#F1F5F9", "#0F172A"}
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
                final String bgHex = themes[i + j][2];
                final String txtHex = themes[i + j][3];
                boolean active = currentTheme.equals(key);

                Button btn = new Button(this);
                btn.setText(active ? label + "  ✓" : label);
                btn.setAllCaps(false);
                btn.setTextSize(12.5f);
                btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                btn.setTextColor(Color.parseColor(txtHex));
                btn.setBackground(makeRoundedBg(
                        Color.parseColor(bgHex),
                        active ? Color.parseColor(getAccentHex()) : colBorder(),
                        10, active ? 2 : 1));
                btn.setMinHeight(0);
                btn.setMinimumHeight(0);
                btn.setOnClickListener(v -> {
                    currentTheme = key;
                    prefs.edit().putString("nova_theme_v45", key).apply();
                    applyCleanTheme();
                    WebView wv = getCurrentWebView();
                    if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                        loadNovaHome(wv);
                    }
                    dialog.dismiss();
                });

                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
                if (j == 0) bLp.setMargins(0, 0, dp(8), 0);
                row.addView(btn, bLp);
            }
            body.addView(row, rLp);
        }

        body.addView(makeSmallLabel("Homepage Wallpaper"));
        LinearLayout wpRow = new LinearLayout(this);
        wpRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPick = makePrimaryButton("Choose Image");
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        pLp.setMargins(0, 0, dp(8), 0);
        btnPick.setOnClickListener(v -> {
            dialog.dismiss();
            wallpaperPickerLauncher.launch("image/*");
        });
        wpRow.addView(btnPick, pLp);

        Button btnReset = makeSecondaryButton("Reset Default", "#F87171");
        btnReset.setOnClickListener(v -> {
            File wpFile = new File(getFilesDir(), "nova_home_wallpaper.b64");
            if (wpFile.exists()) wpFile.delete();
            Toast.makeText(this, "Wallpaper reset", Toast.LENGTH_SHORT).show();
            WebView wv = getCurrentWebView();
            if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                loadNovaHome(wv);
            }
            dialog.dismiss();
        });
        wpRow.addView(btnReset, new LinearLayout.LayoutParams(0, dp(38), 1f));
        body.addView(wpRow);

        dialog.show();
    }

    private void saveCustomWallpaperFromUri(Uri uri) {
        new Thread(() -> {
            Bitmap raw = null;
            Bitmap scaled = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                raw = BitmapFactory.decodeStream(in);
                if (raw == null) return;
                int maxDim = 720;
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
                    Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show();
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
    // CLEAN MINIMALIST HOMEPAGE (ZERO EMOJIS, CRISP SVG ICONS, REAL METRICS)
    // =========================================================================

    private void loadNovaHome(WebView webView) {
        int rulesCount = adHostHashes.length + trackerHostHashes.length;
        String accent = getAccentHex();
        boolean light = isLightTheme();

        String customWpB64 = readCustomWallpaperBase64();
        String bgCss;
        if (customWpB64 != null && !customWpB64.isEmpty()) {
            bgCss = "linear-gradient(rgba(11,15,23,0.62),rgba(11,15,23,0.78)),url('data:image/jpeg;base64," + customWpB64 + "') center/cover no-repeat fixed";
        } else if (THEME_AMOLED.equals(currentTheme)) {
            bgCss = "#000000";
        } else if (THEME_SLATE.equals(currentTheme)) {
            bgCss = "#0F172A";
        } else if (THEME_INDIGO.equals(currentTheme)) {
            bgCss = "#0C0D1D";
        } else if (THEME_FOREST.equals(currentTheme)) {
            bgCss = "#071310";
        } else if (light) {
            bgCss = "#F8FAFC";
        } else {
            bgCss = "#0B0F17";
        }

        boolean darkText = light && customWpB64 == null;
        String cardBg = darkText ? "#FFFFFF" : "rgba(22, 30, 46, 0.86)";
        String border = darkText ? "#E2E8F0" : "rgba(148, 163, 184, 0.18)";
        String text = darkText ? "#0F172A" : "#F8FAFC";
        String muted = darkText ? "#64748B" : "#94A3B8";

        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;}"
            + "body{margin:0;min-height:100vh;padding:34px 16px 28px;font-family:-apple-system,Roboto,sans-serif;background:" + bgCss + ";color:" + text + ";}"
            + ".wrap{max-width:460px;margin:0 auto;}"
            + ".brand{text-align:center;margin-bottom:22px;}"
            + ".wordmark{font-size:28px;font-weight:700;letter-spacing:6px;color:" + text + ";}"
            + ".sub{font-size:12px;color:" + muted + ";margin-top:6px;letter-spacing:0.3px;}"
            + ".search{display:flex;align-items:center;background:" + cardBg + ";border:1px solid " + border + ";border-radius:14px;padding:4px 6px 4px 14px;margin-bottom:20px;}"
            + ".search input{flex:1;background:transparent;border:none;color:" + text + ";font-size:14.5px;outline:none;padding:9px 4px;}"
            + ".search button{background:" + accent + ";color:" + (darkText ? "#FFFFFF" : "#071019") + ";border:none;border-radius:10px;padding:8px 16px;font-weight:600;font-size:13px;}"
            + ".section-lbl{font-size:11px;font-weight:600;letter-spacing:0.8px;color:" + muted + ";text-transform:uppercase;margin:0 0 10px 2px;}"
            + ".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:20px;}"
            + ".tile{display:flex;flex-direction:column;align-items:center;justify-content:center;padding:12px 6px;background:" + cardBg + ";border:1px solid " + border + ";border-radius:12px;text-decoration:none;color:" + text + ";font-size:11.5px;font-weight:500;}"
            + ".badge{width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;margin-bottom:7px;background:rgba(56,189,248,0.12);color:" + accent + ";}"
            + ".metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:18px;}"
            + ".mcard{background:" + cardBg + ";border:1px solid " + border + ";border-radius:12px;padding:11px 8px;text-align:center;}"
            + ".mval{font-size:15px;font-weight:700;color:" + text + ";}"
            + ".mlbl{font-size:10.5px;color:" + muted + ";margin-top:3px;}"
            + ".actions{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;}"
            + ".abtn{background:" + cardBg + ";border:1px solid " + border + ";border-radius:10px;padding:10px 6px;color:" + text + ";font-size:12px;font-weight:500;text-align:center;}"
            + "</style></head><body><div class='wrap'>"
            + "<div class='brand'><div class='wordmark'>NOVA</div><div class='sub'>Private Web Browser</div></div>"
            + "<form class='search' onsubmit=\"var q=document.getElementById('q').value.trim();if(q){window.location.href='" + searchEngineUrl + "'+encodeURIComponent(q);}return false;\">"
            + "<input id='q' type='text' placeholder='Search with " + searchEngineName + "...'/>"
            + "<button type='submit'>Go</button></form>"
            + "<div class='section-lbl'>Shortcuts</div>"
            + "<div class='grid'>"
            + "<a class='tile' href='https://www.google.com'><div class='badge'>G</div>Google</a>"
            + "<a class='tile' href='https://m.youtube.com'><div class='badge'>YT</div>YouTube</a>"
            + "<a class='tile' href='https://open.spotify.com'><div class='badge'>SP</div>Spotify</a>"
            + "<a class='tile' href='https://github.com/shahalamlaskarofficial'><div class='badge'>GH</div>GitHub</a>"
            + "<a class='tile' href='https://chatgpt.com'><div class='badge'>AI</div>ChatGPT</a>"
            + "<a class='tile' href='
