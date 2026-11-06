package com.nova.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private Button btnDesktop, btnIncognito;

    private boolean isDesktopMode = false;
    private boolean isIncognito = false;
    private String defaultUserAgent;
    private static final String HOME_URL = "https://duckduckgo.com";

    private static final String[] AD_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "adnxs.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.etUrl);
        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);
        Button btnGo = findViewById(R.id.btnGo);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnReload = findViewById(R.id.btnReload);
        Button btnHome = findViewById(R.id.btnHome);
        btnDesktop = findViewById(R.id.btnDesktop);
        btnIncognito = findViewById(R.id.btnIncognito);

        setupWebView();

        btnGo.setOnClickListener(v -> loadUserInput());
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUserInput();
                return true;
            }
            return false;
        });

        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnReload.setOnClickListener(v -> webView.reload());
        btnHome.setOnClickListener(v -> webView.loadUrl(HOME_URL));

        btnDesktop.setOnClickListener(v -> toggleDesktopMode());
        btnIncognito.setOnClickListener(v -> toggleIncognitoMode());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });

        webView.loadUrl(HOME_URL);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        defaultUserAgent = settings.getUserAgentString();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                etUrl.setText(url);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null) {
                    for (String adHost : AD_HOSTS) {
                        if (host.contains(adHost)) {
                            return new WebResourceResponse(
                                "text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes())
                            );
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            request.setTitle(fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Downloading " + fileName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserInput() {
        String input = etUrl.getText().toString().trim();
        if (input.isEmpty()) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);

        if (input.startsWith("http://") || input.startsWith("https://")) {
            webView.loadUrl(input);
        } else if (input.contains(".") && !input.contains(" ")) {
            webView.loadUrl("https://" + input);
        } else {
            webView.loadUrl("https://duckduckgo.com/?q=" + Uri.encode(input));
        }
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        WebSettings settings = webView.getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            btnDesktop.setTextColor(Color.parseColor("#38BDF8"));
            Toast.makeText(this, "Desktop Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            settings.setUserAgentString(defaultUserAgent);
            btnDesktop.setTextColor(Color.parseColor("#94A3B8"));
            Toast.makeText(this, "Desktop Mode OFF", Toast.LENGTH_SHORT).show();
        }
        webView.reload();
    }

    private void toggleIncognitoMode() {
        isIncognito = !isIncognito;
        CookieManager cookieManager = CookieManager.getInstance();
        WebSettings settings = webView.getSettings();
        if (isIncognito) {
            cookieManager.setAcceptCookie(false);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.clearHistory();
            webView.clearCache(true);
            btnIncognito.setTextColor(Color.parseColor("#A855F7"));
            Toast.makeText(this, "Incognito Mode ON", Toast.LENGTH_SHORT).show();
        } else {
            cookieManager.setAcceptCookie(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            btnIncognito.setTextColor(Color.parseColor("#94A3B8"));
            Toast.makeText(this, "Incognito Mode OFF", Toast.LENGTH_SHORT).show();
        }
    }
}
