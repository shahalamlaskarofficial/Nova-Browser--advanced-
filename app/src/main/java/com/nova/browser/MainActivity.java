package com.nova.browser;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class MainActivity extends AppCompatActivity {

    private static final String DEV_NAME = "Shah Alam Laskar";
    private static final String DEV_EMAIL = "shahalamlaskar.official@gmail.com";
    private static final String DEV_GITHUB = "https://github.com/shahalamlaskarofficial";

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
        "samsungads.com", "ad.samsungadhub.com", "ads. Yap.yahoo.com", "adsystem.com"
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
        "analytics-api.samsunghealthcn.com"
    };

    private static final String[] BLOCKED_URL_KEYWORDS = {
        "/pagead/", "/ads.js", "/adframe.js", "/adserver", "/googlesyndication",
        "/doubleclick", "analytics.js", "gtag/js", "fbevents.js", "sentry-cdn",
        "bugsnag", "hotjar", "mouseflow", "luckyorange", "freshmarketer",
        "amazon-adsystem", "ads-twitter", "unityads", "adservice."
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
        syncFilterListsInBackground();

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
        btnDownloads.setOnClickListener(v -> showDownloadsManager());

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
                } else {
                    finish();
                }
            }
        });
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

    private void syncFilterListsInBackground() {
        new Thread(() -> {
            try {
                URL url = new URL("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim().toLowerCase(Locale.US);
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            blockedAdHosts.add(line);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private int classifyBlockedRequest(String host, String fullUrl) {
        if (host == null || host.isEmpty()) return 0;
        String cleanHost = host.toLowerCase(Locale.US);

        for (String allowed : allowlistDomains) {
            if (cleanHost.equals(allowed) || cleanHost.endsWith("." + allowed)) {
                return 0;
            }
        }

        String part = cleanHost;
        while (part.contains(".")) {
            if (blockedTrackerHosts.contains(part)) return 2;
            if (blockedAdHosts.contains(part)) return 1;
            int dot = part.indexOf('.');
            part = part.substring(dot + 1);
        }

        String lowerUrl = fullUrl.toLowerCase(Locale.US);
        for (String kw : BLOCKED_URL_KEYWORDS) {
            if (lowerUrl.contains(kw)) {
                return 1;
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
        String dataSavedKb = String.format(Locale.US, "%.1f KB", totalBlocked * 18.5);
        String bg = isDarkTheme ? "#0B0F19" : "#F8FAFC";
        String card = isDarkTheme ? "#1E293B" : "#E2E8F0";
        String text = isDarkTheme ? "#F8FAFC" : "#0F172A";
        String sub = isDarkTheme ? "#94A3B8" : "#475569";

        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "body{margin:0;padding:22px 16px;font-family:sans-serif;background:" + bg + ";color:" + text + ";text-align:center;}"
            + ".logo{font-size:32px;font-weight:900;letter-spacing:2px;background:linear-gradient(90deg,#38BDF8,#818CF8,#C084FC);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-top:10px;}"
            + ".tag{font-size:12px;color:" + sub + ";margin-bottom:20px;}"
            + ".search-box{display:flex;background:" + card + ";border-radius:28px;padding:6px 14px;margin:0 auto 20px;max-width:460px;border:1px solid #334155;}"
            + ".search-box input{flex:1;background:transparent;border:none;color:" + text + ";font-size:15px;outline:none;padding:8px;}"
            + ".search-box button{background:#38BDF8;color:#0F172A;border:none;border-radius:20px;padding:8px 16px;font-weight:bold;}"
            + ".stats{display:flex;justify-content:space-around;background:" + card + ";border-radius:16px;padding:14px 6px;margin:0 auto 20px;max-width:460px;}"
            + ".stat-num{font-size:18px;font-weight:bold;color:#38BDF8;}"
            + ".stat-lbl{font-size:10px;color:" + sub + ";margin-top:4px;}"
            + ".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;max-width:460px;margin:0 auto 24px;}"
            + ".tile{background:" + card + ";padding:12px 6px;border-radius:14px;text-decoration:none;color:" + text + ";font-size:11px;font-weight:600;}"
            + ".icon{font-size:20px;display:block;margin-bottom:5px;}"
            + ".dev-card{background:" + card + ";border-radius:14px;padding:12px;max-width:460px;margin:0 auto;font-size:12px;color:" + sub + ";}"
            + ".dev-card a{color:#38BDF8;text-decoration:none;font-weight:bold;}"
            + "</style></head><body>"
            + "<div class='logo'>NOVA BROWSER</div>"
            + "<div class='tag'>Shield v2.0 &bull; Battery Saver &bull; Low RAM (" + searchEngineName + ")</div>"
            + "<form class='search-box' onsubmit=\"window.location.href='" + searchEngineUrl + "'+encodeURIComponent(document.getElementById('q').value);return false;\">"
            + "<input id='q' type='text' placeholder='Search with " + searchEngineName + " or enter URL...'/>"
            + "<button type='submit'>GO</button></form>"
            + "<div class='stats'>"
            + "<div><div class='stat-num'>" + adsBlockedCount + "</div><div class='stat-lbl'>Ads Blocked</div></div>"
            + "<div><div class='stat-num'>" + trackersBlockedCount + "</div><div class='stat-lbl'>Trackers</div></div>"
            + "<div><div class='stat-num'>" + dataSavedKb + "</div><div class='stat-lbl'>Data Saved</div></div>"
            + "<div><div class='stat-num'>" + getRamUsageMb() + "</div><div class='stat-lbl'>RAM Used</div></div>"
            + "</div>"
            + "<div class='grid'>"
            + "<a class='tile' href='https://www.google.com'><span class='icon'>🔍</span>Google</a>"
            + "<a class='tile' href='https://m.youtube.com'><span class='icon'>▶️</span>YouTube</a>"
            + "<a class='tile' href='https://github.com/shahalamlaskarofficial'><span class='icon'>💻</span>GitHub</a>"
            + "<a class='tile' href='https://adblock.turtlecute.org'><span class='icon'>🛡️</span>AdTest</a>"
            + "<a class='tile' href='https://www.reddit.com'><span class='icon'>🔥</span>Reddit</a>"
            + "<a class='tile' href='https://chatgpt.com'><span class='icon'>🤖</span>ChatGPT</a>"
            + "<a class='tile' href='https://en.m.wikipedia.org'><span class='icon'>🌐</span>Wiki</a>"
            + "<a class='tile' href='https://fast.com'><span class='icon'>⚡</span>SpeedTest</a>"
            + "</div>"
            + "<div class='dev-card'>Developed by <b>" + DEV_NAME + "</b><br/>"
            + "Contact: <a href='mailto:" + DEV_EMAIL + "'>" + DEV_EMAIL + "</a></div>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://nova.browser", html, "text/html", "UTF-8", null);
        etUrl.setText("");
        etUrl.setHint("Search " + searchEngineName + " or type URL...");
    }

    private void injectNovaShieldHook(WebView view) {
        if (!isAdBlockEnabled) return;
        String jsHook = "javascript:(function(){"
            + "if(window.__novaShieldActive)return;window.__novaShieldActive=true;"
            + "const bad=['googlesyndication','doubleclick','googleadservices','adservice.google',"
            + "'google-analytics','googletagmanager','googletagservices','adsystem','ads.yahoo',"
            + "'adtech.yahoo','gemini.yahoo','udc.yahoo','udcm.yahoo','analytics.query.yahoo',"
            + "'partnerads.ysm.yahoo','log.fc.yahoo','ads-twitter','advertising.twitter',"
            + "'connect.facebook.net','pixel.facebook','an.facebook','ads.linkedin',"
            + "'ads.pinterest','log.pinterest','trk.pinterest','ads.tiktok','analytics.tiktok',"
            + "'ads-api.tiktok','ads-sg.tiktok','analytics-sg.tiktok','business-api.tiktok',"
            + "'log.byteoversea','events.reddit','d.reddit.com','alb.reddit','tr.snapchat',"
            + "'ads.snapchat','sc-analytics.appspot','hotjar','mouseflow','freshmarketer',"
            + "'luckyorange','stats.wp.com','pixel.wp.com','bugsnag','sentry.io','sentry-cdn',"
            + "'getsentry','mc.yandex','appmetrica.yandex','adfstat.yandex','metrika.yandex',"
            + "'advertising.yandex','offerwall.yandex','adfox.yandex','unityads.unity3d',"
            + "'realmemobile.com','iot-eu-logser.realme','iot-logser.realme','oppomobile.com',"
            + "'ad.xiaomi.com','mistat.xiaomi','mistat.india.xiaomi','mistat.rus.xiaomi',"
            + "'tracking.rus.miui','iadsdk.apple','api-adservices.apple','metrics.icloud',"
            + "'metrics.mzstatic','analytics-events.apple','samsungads.com','smetrics.samsung',"
            + "'nmetrics.samsung','2o7.net','samsunghealthcn.com','taboola','outbrain',"
            + "'criteo','pubmatic','rubiconproject','openx','adnxs','popads','popcash'];"
            + "function isBlocked(u){if(!u)return false;u=u.toString().toLowerCase();"
            + "for(let i=0;i<bad.length;i++){if(u.indexOf(bad[i])!==-1)return true;}return false;}"
            + "const origFetch=window.fetch;"
            + "window.fetch=function(input,init){"
            + "const u=(typeof input==='string')?input:(input&&input.url?input.url:'');"
            + "if(isBlocked(u))return Promise.reject(new TypeError('Failed to fetch'));"
            + "return origFetch.apply(this,arguments);};"
            + "const origOpen=XMLHttpRequest.prototype.open;"
            + "XMLHttpRequest.prototype.open=function(m,u){"
            + "if(isBlocked(u)){this.__blocked=true;}return origOpen.apply(this,arguments);};"
            + "const origSend=XMLHttpRequest.prototype.send;"
            + "XMLHttpRequest.prototype.send=function(){"
            + "if(this.__blocked){this.dispatchEvent(new Event('error'));return;}"
            + "return origSend.apply(this,arguments);};"
            + "var st=document.createElement('style');"
            + "st.innerHTML='.adsbygoogle,[id*=\"google_ads\"],[class*=\"ad-banner\"],[class*=\"cookie-banner\"],[id*=\"cookie-consent\"]{display:none!important;}';"
            + "if(document.head)document.head.appendChild(st);"
            + "})();";
        view.evaluateJavascript(jsHook, null);
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
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBlockNetworkImage(isDataSaver);
        settings.setTextZoom(textZoomPercent);
        settings.setCacheMode(isIncognito ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);

        if (defaultUserAgent == null) {
            defaultUserAgent = settings.getUserAgentString();
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("mailto:")) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(Intent.createChooser(emailIntent, "Contact Developer"));
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
                if (!isAdBlockEnabled) return super.shouldInterceptRequest(view, request);

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
                if (newProgress > 15 && newProgress < 90) {
                    injectNovaShieldHook(view);
                }
                if (view == getCurrentWebView()) {
                    progressBar.setProgress(newProgress);
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            String sizeMb = contentLength > 0
                    ? String.format(Locale.US, "%.2f MB", contentLength / (1024.0 * 1024.0))
                    : "Unknown size";

            new AlertDialog.Builder(this)
                .setTitle("NOVA Download Manager")
                .setMessage("File: " + fileName + "\nSize: " + sizeMb + "\nType: " + mimeType)
                .setPositiveButton("Download", (d, w) -> {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setMimeType(mimeType);
                    req.setTitle(fileName);
                    req.setDescription("Downloading via NOVA Browser");
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(req);
                        Toast.makeText(this, "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

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

        String report = "🛡️ NOVA Shield v2: " + (isAdBlockEnabled ? "ACTIVE" : "PAUSED") + "\n"
                + "📋 Filter Rules Loaded: " + rulesLoaded + " domains\n"
                + "🔒 Connection: " + (isHttps ? "Secure (HTTPS)" : "Unencrypted / Local") + "\n"
                + "🚫 Ads Blocked: " + adsBlockedCount + "\n"
                + "👁️ Trackers Blocked: " + trackersBlockedCount + "\n"
                + "📦 Total Blocked Requests: " + total + "\n"
                + "⚡ Estimated Data Saved: " + saved + "\n"
                + "🔋 Battery Saver: " + (isBatterySaver ? "ON" : "OFF");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NOVA Privacy & Shield Dashboard");
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
        String aboutMessage = "🌐 NOVA Browser — Advanced Privacy Edition\n"
                + "Version: 2.0.0 (Shield v2 + Low-RAM Engine)\n\n"
                + "👨‍💻 Developer: " + DEV_NAME + "\n"
                + "📧 Email: " + DEV_EMAIL + "\n"
                + "💻 GitHub: @shahalamlaskarofficial\n\n"
                + "Built for ultra-fast browsing, aggressive ad & tracker protection, intelligent tab suspension, and low battery consumption.";

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
            "⚡ RAM & Battery Optimizer (" + getRamUsageMb() + ")",
            "🔍 Search Engine (" + searchEngineName + ")",
            "📖 Reader Mode (Distraction-Free)",
            "🔎 Find in Page",
            "🖥️ Desktop Site (" + (isDesktopMode ? "ON" : "OFF") + ")",
            "🕵️ Private Browsing (" + (isIncognito ? "ON" : "OFF") + ")",
            "📉 Data Saver / Block Images (" + (isDataSaver ? "ON" : "OFF") + ")",
            "🌗 Theme: " + (isDarkTheme ? "Dark Mode" : "Light Mode"),
            "🔠 Font / Zoom Size (" + textZoomPercent + "%)",
            "📺 Full-Screen Mode (" + (isFullScreen ? "ON" : "OFF") + ")",
            "🌐 Translate Current Page",
            "📜 Browsing History",
            "🔗 Share, Copy Link or QR Code",
            "🧹 Clear Browsing Data & Cache",
            "👨‍💻 About & Contact Developer"
        };

        new AlertDialog.Builder(this)
            .setTitle("NOVA Browser Tools & Settings")
            .setItems(options, (d, which) -> {
                WebView wv = getCurrentWebView();
                switch (which) {
                    case 0: showPerformanceOptimizerDialog(); break;
                    case 1: showSearchEngineDialog(); break;
                    case 2: toggleReaderMode(wv); break;
                    case 3:
                        layoutFind.setVisibility(View.VISIBLE);
                        etFind.requestFocus();
                        break;
                    case 4: toggleDesktopMode(); break;
                    case 5: toggleIncognitoMode(); break;
                    case 6:
                        isDataSaver = !isDataSaver;
                        for (WebView tab : tabs) tab.getSettings().setBlockNetworkImage(isDataSaver);
                        if (wv != null) wv.reload();
                        Toast.makeText(this, "Data Saver " + (isDataSaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                        break;
                    case 7: toggleTheme(); break;
                    case 8: showZoomDialog(); break;
                    case 9: toggleFullScreen(); break;
                    case 10:
                        if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                            wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(wv.getUrl()));
                        }
                        break;
                    case 11: showHistoryDialog(); break;
                    case 12: shareOrCopyUrl(wv); break;
                    case 13: clearBrowserData(wv); break;
                    case 14: showAboutDeveloperDialog(); break;
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
            + "document.body.style.backgroundColor='#111827';"
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
            ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
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
        topBar.setBackgroundColor(Color.parseColor(isIncognito ? "#3B0764" : (isDarkTheme ? "#111827" : "#E2E8F0")));
        Toast.makeText(this, "Private Browsing " + (isIncognito ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        rootLayout.setBackgroundColor(Color.parseColor(isDarkTheme ? "#0B0F19" : "#F8FAFC"));
        topBar.setBackgroundColor(Color.parseColor(isDarkTheme ? "#111827" : "#E2E8F0"));
        bottomBar.setBackgroundColor(Color.parseColor(isDarkTheme ? "#111827" : "#E2E8F0"));
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

    private void showDownloadsManager() {
        String[] categories = {
            "📂 All Downloaded Files",
            "🤖 APK Packages (.apk)",
            "🎬 Videos (.mp4, .mkv, .webm)",
            "🖼️ Images (.jpg, .png, .webp)",
            "📄 Documents (.pdf, .docx, .txt)",
            "📦 Archives (.zip, .rar, .7z)"
        };
        String[][] exts = {
            {},
            {".apk"},
            {".mp4", ".mkv", ".webm", ".avi"},
            {".jpg", ".jpeg", ".png", ".webp", ".gif"},
            {".pdf", ".doc", ".docx", ".txt", ".xlsx"},
            {".zip", ".rar", ".7z", ".tar", ".gz"}
        };

        new AlertDialog.Builder(this)
            .setTitle("NOVA Download & File Manager")
            .setItems(categories, (d, which) -> showFilteredFiles(categories[which], exts[which]))
            .setNegativeButton("Close", null)
            .show();
    }

    private void showFilteredFiles(String title, String[] extensions) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = dir.listFiles();
        List<File> matched = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    String name = f.getName().toLowerCase(Locale.US);
                    boolean ok = extensions.length == 0;
                    for (String ext : extensions) {
                        if (name.endsWith(ext)) {
                            ok = true;
                            break;
                        }
                    }
                    if (ok) {
                        matched.add(f);
                        String size = String.format(Locale.US, "%.2f MB", f.length() / (1024.0 * 1024.0));
                        labels.add(f.getName() + " (" + size + ")");
                    }
                }
            }
        }

        if (labels.isEmpty()) {
            Toast.makeText(this, "No matching files found in Downloads", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle(title + " (" + labels.size() + ")")
            .setItems(labels.toArray(new String[0]), (d, which) -> {
                File selected = matched.get(which);
                new AlertDialog.Builder(this)
                    .setTitle(selected.getName())
                    .setMessage("Path: " + selected.getAbsolutePath())
                    .setPositiveButton("Open Downloads Folder", (d2, w2) -> {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    })
                    .setNeutralButton("Delete File", (d2, w2) -> {
                        if (selected.delete()) {
                            Toast.makeText(this, "Deleted " + selected.getName(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
            })
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
