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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private static final String APP_VERSION = "4.6";
    private static final int MAX_SYNCED_DOMAINS = 175000;

    // Vibrant Liquid Glass Theme Keys
    private static final String THEME_SAPPHIRE = "sapphire";
    private static final String THEME_AMOLED = "amoled";
    private static final String THEME_CYBERPUNK = "cyberpunk";
    private static final String THEME_SUNSET = "sunset";
    private static final String THEME_EMERALD = "emerald";
    private static final String THEME_FROST = "frost";

    // Drawn Vector Icon Types
    private static final int IC_SHIELD = 1;
    private static final int IC_SHIELD_OFF = 2;
    private static final int IC_SHIELD_WARN = 3;
    private static final int IC_BACK = 4;
    private static final int IC_FORWARD = 5;
    private static final int IC_RELOAD = 6;
    private static final int IC_HOME = 7;
    private static final int IC_STAR = 8;
    private static final int IC_DOWNLOAD = 9;
    private static final int IC_MENU = 10;
    private static final int IC_CLOSE = 11;
    private static final int IC_MAIL = 12;
    private static final int IC_UNLOCK = 13;
    private static final int IC_PALETTE = 14;
    private static final int IC_BOLT = 15;
    private static final int IC_SEARCH = 16;
    private static final int IC_BOOK = 17;
    private static final int IC_DESKTOP = 18;
    private static final int IC_PRIVATE = 19;
    private static final int IC_SETTINGS = 20;
    private static final int IC_HISTORY = 21;
    private static final int IC_SHARE = 22;
    private static final int IC_TRASH = 23;
    private static final int IC_USER = 24;
    private static final int IC_COPY = 25;
    private static final int IC_PAUSE = 26;
    private static final int IC_PLAY = 27;
    private static final int IC_FOLDER = 28;
    private static final int IC_IMAGE = 29;
    private static final int IC_GLOBE = 30;
    private static final int IC_PLUS = 31;
    private static final int IC_CHECK = 32;
    private static final int IC_LINK = 33;

    // =========================================================================
    // CUSTOM ANTI-ALIASED 24x24 VECTOR ICON ENGINE (ZERO EMOJIS, ZERO XML ASSETS)
    // =========================================================================
    private static class VectorIconDrawable extends Drawable {
        private final int type;
        private final int color;
        private final int sizePx;
        private final int badgeCount;
        private final Paint strokePaint;
        private final Paint fillPaint;
        private final Paint badgePaint;
        private final Paint badgeTextPaint;

        VectorIconDrawable(int type, int color, int sizePx, int badgeCount) {
            this.type = type;
            this.color = color;
            this.sizePx = sizePx;
            this.badgeCount = badgeCount;

            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setColor(color);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.0f);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);

            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);

            badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setColor(Color.parseColor("#00E676"));
            badgePaint.setStyle(Paint.Style.FILL);

            badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeTextPaint.setColor(Color.parseColor("#041E12"));
            badgeTextPaint.setTextSize(8.2f);
            badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width() > 0 ? b.width() : sizePx;
            float h = b.height() > 0 ? b.height() : sizePx;
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(w / 24f, h / 24f);

            Path p = new Path();
            switch (type) {
                case IC_SHIELD:
                case IC_SHIELD_OFF:
                case IC_SHIELD_WARN:
                    p.moveTo(12f, 2.5f);
                    p.lineTo(20f, 5.8f);
                    p.lineTo(20f, 11.5f);
                    p.cubicTo(20f, 16.6f, 16.5f, 20.2f, 12f, 21.8f);
                    p.cubicTo(7.5f, 20.2f, 4f, 16.6f, 4f, 11.5f);
                    p.lineTo(4f, 5.8f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    if (type == IC_SHIELD) {
                        Path chk = new Path();
                        chk.moveTo(8.5f, 12.2f);
                        chk.lineTo(11f, 14.7f);
                        chk.lineTo(15.8f, 9.6f);
                        canvas.drawPath(chk, strokePaint);
                    } else if (type == IC_SHIELD_OFF) {
                        canvas.drawLine(7f, 7f, 17f, 17f, strokePaint);
                    } else {
                        canvas.drawLine(12f, 8.2f, 12f, 13f, strokePaint);
                        canvas.drawCircle(12f, 16.2f, 1.1f, fillPaint);
                    }
                    break;

                case IC_BACK:
                    p.moveTo(15.5f, 5.5f);
                    p.lineTo(9f, 12f);
                    p.lineTo(15.5f, 18.5f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_FORWARD:
                    p.moveTo(8.5f, 5.5f);
                    p.lineTo(15f, 12f);
                    p.lineTo(8.5f, 18.5f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_RELOAD:
                    canvas.drawArc(new RectF(4.5f, 4.5f, 19.5f, 19.5f), -45f, 295f, false, strokePaint);
                    p.moveTo(15.2f, 4.2f);
                    p.lineTo(19.5f, 4.8f);
                    p.lineTo(18.6f, 9.1f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_HOME:
                    p.moveTo(3.5f, 11f);
                    p.lineTo(12f, 3.8f);
                    p.lineTo(20.5f, 11f);
                    canvas.drawPath(p, strokePaint);
                    Path body = new Path();
                    body.moveTo(6f, 9.5f);
                    body.lineTo(6f, 19.8f);
                    body.lineTo(18f, 19.8f);
                    body.lineTo(18f, 9.5f);
                    canvas.drawPath(body, strokePaint);
                    canvas.drawLine(10.2f, 19.8f, 10.2f, 14.5f, strokePaint);
                    canvas.drawLine(10.2f, 14.5f, 13.8f, 14.5f, strokePaint);
                    canvas.drawLine(13.8f, 14.5f, 13.8f, 19.8f, strokePaint);
                    break;

                case IC_STAR:
                    p.moveTo(12f, 3.2f);
                    p.lineTo(14.7f, 8.7f);
                    p.lineTo(20.8f, 9.6f);
                    p.lineTo(16.4f, 13.8f);
                    p.lineTo(17.4f, 19.9f);
                    p.lineTo(12f, 17f);
                    p.lineTo(6.6f, 19.9f);
                    p.lineTo(7.6f, 13.8f);
                    p.lineTo(3.2f, 9.6f);
                    p.lineTo(9.3f, 8.7f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_DOWNLOAD:
                    canvas.drawLine(12f, 4f, 12f, 15.2f, strokePaint);
                    p.moveTo(7.5f, 11.2f);
                    p.lineTo(12f, 15.8f);
                    p.lineTo(16.5f, 11.2f);
                    canvas.drawPath(p, strokePaint);
                    Path tray = new Path();
                    tray.moveTo(4.5f, 16.5f);
                    tray.lineTo(4.5f, 19.8f);
                    tray.lineTo(19.5f, 19.8f);
                    tray.lineTo(19.5f, 16.5f);
                    canvas.drawPath(tray, strokePaint);
                    if (badgeCount > 0) {
                        canvas.drawCircle(18.5f, 6.0f, 5.2f, badgePaint);
                        String txt = badgeCount > 9 ? "9+" : String.valueOf(badgeCount);
                        canvas.drawText(txt, 18.5f, 8.8f, badgeTextPaint);
                    }
                    break;

                case IC_MENU:
                    canvas.drawCircle(12f, 5.5f, 1.7f, fillPaint);
                    canvas.drawCircle(12f, 12f, 1.7f, fillPaint);
                    canvas.drawCircle(12f, 18.5f, 1.7f, fillPaint);
                    break;

                case IC_CLOSE:
                    canvas.drawLine(6f, 6f, 18f, 18f, strokePaint);
                    canvas.drawLine(18f, 6f, 6f, 18f, strokePaint);
                    break;

                case IC_MAIL:
                    canvas.drawRoundRect(new RectF(3.5f, 5.5f, 20.5f, 18.5f), 2.2f, 2.2f, strokePaint);
                    p.moveTo(4.2f, 6.8f);
                    p.lineTo(12f, 12.8f);
                    p.lineTo(19.8f, 6.8f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_UNLOCK:
                    canvas.drawRoundRect(new RectF(5f, 10.5f, 19f, 20.5f), 2.2f, 2.2f, strokePaint);
                    canvas.drawArc(new RectF(8f, 3.5f, 16f, 12.5f), 180f, 145f, false, strokePaint);
                    canvas.drawLine(8f, 8f, 8f, 10.5f, strokePaint);
                    canvas.drawCircle(12f, 15.5f, 1.4f, fillPaint);
                    break;

                case IC_PALETTE:
                    canvas.drawCircle(12f, 12f, 8.5f, strokePaint);
                    canvas.drawCircle(9.2f, 9.2f, 1.4f, fillPaint);
                    canvas.drawCircle(14.8f, 9.2f, 1.4f, fillPaint);
                    canvas.drawCircle(8.5f, 14f, 1.4f, fillPaint);
                    canvas.drawCircle(15.2f, 14.5f, 1.6f, fillPaint);
                    break;

                case IC_BOLT:
                    p.moveTo(13.5f, 2.8f);
                    p.lineTo(5.5f, 13.2f);
                    p.lineTo(11.6f, 13.2f);
                    p.lineTo(10.5f, 21.2f);
                    p.lineTo(18.5f, 10.8f);
                    p.lineTo(12.4f, 10.8f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_SEARCH:
                    canvas.drawCircle(10.5f, 10.5f, 6.2f, strokePaint);
                    canvas.drawLine(15.2f, 15.2f, 20f, 20f, strokePaint);
                    break;

                case IC_BOOK:
                    p.moveTo(4f, 5.5f);
                    p.lineTo(10.5f, 5.5f);
                    p.quadTo(12f, 5.5f, 12f, 7.5f);
                    p.lineTo(12f, 19.5f);
                    p.quadTo(12f, 18f, 10f, 18f);
                    p.lineTo(4f, 18f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    Path p2 = new Path();
                    p2.moveTo(20f, 5.5f);
                    p2.lineTo(13.5f, 5.5f);
                    p2.quadTo(12f, 5.5f, 12f, 7.5f);
                    p2.lineTo(12f, 19.5f);
                    p2.quadTo(12f, 18f, 14f, 18f);
                    p2.lineTo(20f, 18f);
                    p2.close();
                    canvas.drawPath(p2, strokePaint);
                    break;

                case IC_DESKTOP:
                    canvas.drawRoundRect(new RectF(3.5f, 4.5f, 20.5f, 16f), 2f, 2f, strokePaint);
                    canvas.drawLine(12f, 16f, 12f, 19.5f, strokePaint);
                    canvas.drawLine(8f, 19.5f, 16f, 19.5f, strokePaint);
                    break;

                case IC_PRIVATE:
                    canvas.drawLine(3.5f, 11.2f, 20.5f, 11.2f, strokePaint);
                    p.moveTo(6.5f, 11.2f);
                    p.lineTo(8.5f, 5.5f);
                    p.lineTo(15.5f, 5.5f);
                    p.lineTo(17.5f, 11.2f);
                    canvas.drawPath(p, strokePaint);
                    canvas.drawCircle(8.5f, 16.2f, 2.5f, strokePaint);
                    canvas.drawCircle(15.5f, 16.2f, 2.5f, strokePaint);
                    canvas.drawLine(11f, 16.2f, 13f, 16.2f, strokePaint);
                    break;

                case IC_SETTINGS:
                    canvas.drawLine(4f, 7f, 20f, 7f, strokePaint);
                    canvas.drawLine(4f, 12f, 20f, 12f, strokePaint);
                    canvas.drawLine(4f, 17f, 20f, 17f, strokePaint);
                    canvas.drawCircle(9f, 7f, 2.1f, fillPaint);
                    canvas.drawCircle(15.5f, 12f, 2.1f, fillPaint);
                    canvas.drawCircle(10.5f, 17f, 2.1f, fillPaint);
                    break;

                case IC_HISTORY:
                    canvas.drawCircle(12f, 12f, 8.2f, strokePaint);
                    canvas.drawLine(12f, 7.5f, 12f, 12f, strokePaint);
                    canvas.drawLine(12f, 12f, 15.5f, 14f, strokePaint);
                    break;

                case IC_SHARE:
                    canvas.drawCircle(17.5f, 5.5f, 2.3f, strokePaint);
                    canvas.drawCircle(6.5f, 12f, 2.3f, strokePaint);
                    canvas.drawCircle(17.5f, 18.5f, 2.3f, strokePaint);
                    canvas.drawLine(8.6f, 10.8f, 15.4f, 6.7f, strokePaint);
                    canvas.drawLine(8.6f, 13.2f, 15.4f, 17.3f, strokePaint);
                    break;

                case IC_TRASH:
                    canvas.drawLine(4.5f, 6.5f, 19.5f, 6.5f, strokePaint);
                    canvas.drawLine(9.5f, 4f, 14.5f, 4f, strokePaint);
                    p.moveTo(6.5f, 6.5f);
                    p.lineTo(7.5f, 19.8f);
                    p.lineTo(16.5f, 19.8f);
                    p.lineTo(17.5f, 6.5f);
                    canvas.drawPath(p, strokePaint);
                    canvas.drawLine(10.2f, 10f, 10.2f, 16.5f, strokePaint);
                    canvas.drawLine(13.8f, 10f, 13.8f, 16.5f, strokePaint);
                    break;

                case IC_USER:
                    canvas.drawCircle(12f, 8f, 3.6f, strokePaint);
                    canvas.drawArc(new RectF(5f, 13.8f, 19f, 23f), 180f, 180f, false, strokePaint);
                    break;

                case IC_COPY:
                    canvas.drawRoundRect(new RectF(9f, 9f, 19.5f, 20f), 2f, 2f, strokePaint);
                    p.moveTo(15f, 9f);
                    p.lineTo(15f, 5.5f);
                    p.lineTo(5.5f, 5.5f);
                    p.lineTo(5.5f, 16f);
                    p.lineTo(9f, 16f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_PAUSE:
                    canvas.drawLine(9f, 6f, 9f, 18f, strokePaint);
                    canvas.drawLine(15f, 6f, 15f, 18f, strokePaint);
                    break;

                case IC_PLAY:
                    p.moveTo(8f, 5.5f);
                    p.lineTo(18.5f, 12f);
                    p.lineTo(8f, 18.5f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_FOLDER:
                    p.moveTo(3.5f, 6.5f);
                    p.lineTo(9.5f, 6.5f);
                    p.lineTo(11.5f, 8.8f);
                    p.lineTo(20.5f, 8.8f);
                    p.lineTo(20.5f, 18.8f);
                    p.lineTo(3.5f, 18.8f);
                    p.close();
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_IMAGE:
                    canvas.drawRoundRect(new RectF(3.8f, 4.8f, 20.2f, 19.2f), 2.2f, 2.2f, strokePaint);
                    canvas.drawCircle(9f, 9.5f, 1.5f, fillPaint);
                    p.moveTo(4.5f, 17f);
                    p.lineTo(10f, 12.2f);
                    p.lineTo(13.5f, 15.2f);
                    p.lineTo(16.2f, 12.6f);
                    p.lineTo(19.6f, 17f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_GLOBE:
                    canvas.drawCircle(12f, 12f, 8.5f, strokePaint);
                    canvas.drawOval(new RectF(8f, 3.5f, 16f, 20.5f), strokePaint);
                    canvas.drawLine(3.8f, 12f, 20.2f, 12f, strokePaint);
                    break;

                case IC_PLUS:
                    canvas.drawLine(12f, 5f, 12f, 19f, strokePaint);
                    canvas.drawLine(5f, 12f, 19f, 12f, strokePaint);
                    break;

                case IC_CHECK:
                    p.moveTo(5f, 12.5f);
                    p.lineTo(10f, 17.2f);
                    p.lineTo(19.2f, 7.2f);
                    canvas.drawPath(p, strokePaint);
                    break;

                case IC_LINK:
                    canvas.drawRoundRect(new RectF(3.5f, 8.5f, 13.5f, 15.5f), 3.5f, 3.5f, strokePaint);
                    canvas.drawRoundRect(new RectF(10.5f, 8.5f, 20.5f, 15.5f), 3.5f, 3.5f, strokePaint);
                    break;
            }
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { strokePaint.setAlpha(alpha); fillPaint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { strokePaint.setColorFilter(cf); fillPaint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return sizePx; }
        @Override public int getIntrinsicHeight() { return sizePx; }
    }

    private Drawable icon(int type, int color, int sizeDp) {
        return new VectorIconDrawable(type, color, dp(sizeDp), 0);
    }

    private Drawable iconWithBadge(int type, int color, int sizeDp, int badge) {
        return new VectorIconDrawable(type, color, dp(sizeDp), badge);
    }

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
            uiHandler.post(MainActivity.this::showCustomizeThemeDialog);
        }

        @JavascriptInterface
        public void openTempMailPage() {
            uiHandler.post(MainActivity.this::showTempMailDialog);
        }

        @JavascriptInterface
        public void openShieldPage() {
            uiHandler.post(MainActivity.this::showOrganizedShieldDashboard);
        }

        @JavascriptInterface
        public void openDownloadsPage() {
            uiHandler.post(MainActivity.this::showTurboDownloadManager);
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
    private ImageButton btnShield, btnMenu, btnFindClose;
    private ImageButton btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads;
    private Button btnTabs;
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
    private boolean isNoJsBypassActive = false;
    private String currentTheme = THEME_SAPPHIRE;
    private int currentBarHeightDp = 54;
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
        currentTheme = prefs.getString("nova_theme", THEME_SAPPHIRE);
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
        btnFindClose = findViewById(R.id.btnFindClose);

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
        btnTabs.setOnClickListener(v -> { pulseButton(v); showTabSwitcherDialog(); });
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
            if (wv != null) {
                resetTabNoJsMode(wv);
                loadNovaHome(wv);
            }
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
                            "Downloads continue running in the background",
                            Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true);
                } else {
                    finish();
                }
            }
        });
    }

    // =========================================================================
    // LIQUID GLASS v4.0 ENGINE + VIBRANT THEMES + DRAWN VECTOR BUTTON ICONS
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
        final float targetScale = targetDp >= 52 ? 1.0f : (targetDp == 0 ? 0.85f : 0.92f);

        ValueAnimator anim = ValueAnimator.ofInt(startPx, targetPx);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(va -> {
            int val = (int) va.getAnimatedValue();
            float alpha = targetDp == 0 ? (val / (float) Math.max(1, dp(46))) : 1f;

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

        for (View b : new View[]{btnShield, btnTabs, btnMenu, btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
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
        int accentCol = Color.parseColor(accent);
        int iconPrimaryCol = isLight ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC");
        int starCol = isLight ? Color.parseColor("#D97706") : Color.parseColor("#FFD000");

        if (isLight) {
            rootLayout.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#E0F2FE"), Color.parseColor("#F8FAFC"), Color.parseColor("#DBEAFE")}));

            int barTop = isIncognito ? Color.parseColor("#E6F3E8FF") : Color.parseColor("#E6FFFFFF");
            int barBot = isIncognito ? Color.parseColor("#E6E9D5FF") : Color.parseColor("#CCE0F2FE");
            int rim = Color.parseColor("#990066FF");

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#F2FFFFFF"), Color.parseColor("#E6F1F5F9"), Color.parseColor("#880066FF"), 16, 1));
            etUrl.setTextColor(Color.parseColor("#0F172A"));
            etUrl.setHintTextColor(Color.parseColor("#64748B"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#F2FFFFFF"), Color.parseColor("#E6F1F5F9"), Color.parseColor("#880066FF"), 14, 1));
            etFind.setTextColor(Color.parseColor("#0F172A"));

            styleGlassPillButton(btnShield, "#AAFFFFFF", "#66DBEAFE", "#880066FF");
            styleGlassPillButton(btnTabs, "#AAFFFFFF", "#66DBEAFE", "#880066FF");
            styleGlassPillButton(btnMenu, "#AAFFFFFF", "#66FFFFFF", "#660066FF");

            for (View dockBtn : new View[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#AAFFFFFF", "#66E0F2FE", "#660066FF");
            }
            btnTabs.setTextColor(accentCol);
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

            topBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            bottomBar.setBackground(makeGlassDrawable(barTop, barBot, rim, 20, 1));
            layoutFind.setBackground(makeGlassDrawable(barTop, barBot, rim, 18, 1));

            etUrl.setBackground(makeGlassDrawable(Color.parseColor("#99040914"), Color.parseColor("#990E1A33"), rim, 16, 1));
            etUrl.setTextColor(Color.parseColor("#FFFFFF"));
            etUrl.setHintTextColor(Color.parseColor("#CBD5E1"));

            etFind.setBackground(makeGlassDrawable(Color.parseColor("#AA040914"), Color.parseColor("#AA0E1A33"), rim, 14, 1));
            etFind.setTextColor(Color.parseColor("#FFFFFF"));

            styleGlassPillButton(btnShield, "#33FFFFFF", "#16FFFFFF", "#66FFFFFF");
            styleGlassPillButton(btnTabs, "#38FFFFFF", "#18FFFFFF", "#77FFFFFF");
            styleGlassPillButton(btnMenu, "#2EFFFFFF", "#14FFFFFF", "#55FFFFFF");

            for (View dockBtn : new View[]{btnBack, btnForward, btnReload, btnHome, btnBookmarks, btnDownloads}) {
                styleGlassPillButton(dockBtn, "#26FFFFFF", "#10FFFFFF", "#44FFFFFF");
            }
            btnTabs.setTextColor(accentCol);
        }

        // Apply crisp drawn vector icons to all top bar & bottom dock controls
        updateShieldToolbarIcon(false);
        btnMenu.setImageDrawable(icon(IC_MENU, iconPrimaryCol, 19));
        btnFindClose.setImageDrawable(icon(IC_CLOSE, Color.parseColor("#FB7185"), 17));
        btnBack.setImageDrawable(icon(IC_BACK, iconPrimaryCol, 20));
        btnForward.setImageDrawable(icon(IC_FORWARD, iconPrimaryCol, 20));
        btnReload.setImageDrawable(icon(IC_RELOAD, iconPrimaryCol, 19));
        btnHome.setImageDrawable(icon(IC_HOME, accentCol, 20));
        btnBookmarks.setImageDrawable(icon(IC_STAR, starCol, 19));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topBar.setElevation(dp(6));
            bottomBar.setElevation(dp(6));
        }
        updateDownloadDockBadge();
    }

    private void updateShieldToolbarIcon(boolean insecureHttp) {
        int col = !isAdBlockEnabled
                ? Color.parseColor("#FB7185")
                : (insecureHttp ? Color.parseColor("#FFD000") : Color.parseColor(getAccentHex()));
        int type = !isAdBlockEnabled ? IC_SHIELD_OFF : (insecureHttp ? IC_SHIELD_WARN : IC_SHIELD);
        btnShield.setImageDrawable(icon(type, col, 20));
    }

    private void styleGlassPillButton(View v, String topHex, String botHex, String strokeHex) {
        v.setBackground(makeGlassDrawable(
                Color.parseColor(topHex),
                Color.parseColor(botHex),
                Color.parseColor(strokeHex),
                13, 1));
    }

    private void updateDownloadDockBadge() {
        int activeCount = DownloadService.getActiveCount();
        uiHandler.post(() -> {
            int col = activeCount > 0 ? Color.parseColor("#00F5A0") : Color.parseColor(getAccentHex());
            btnDownloads.setImageDrawable(iconWithBadge(IC_DOWNLOAD, col, 20, activeCount));
        });
    }

    // Reusable Liquid Glass UI Builders (with drawn vector icons, zero emojis)
    private LinearLayout createGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(11), dp(13), dp(11));
        card.setBackground(makeGlassDrawable(
                Color.parseColor("#CC142442"),
                Color.parseColor("#CC0B152B"),
                Color.parseColor("#4DFFFFFF"),
                16, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(9));
        card.setLayoutParams(lp);
        return card;
    }

    private Button createGlassButton(String text, int iconType, String topHex, String botHex, String textHex) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12.5f);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        int tCol = Color.parseColor(textHex);
        b.setTextColor(tCol);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setBackground(makeGlassDrawable(
                Color.parseColor(topHex),
                Color.parseColor(botHex),
                Color.parseColor("#4DFFFFFF"),
                12, 1));
        b.setPadding(dp(12), 0, dp(10), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        if (iconType > 0) {
            b.setCompoundDrawablesWithIntrinsicBounds(icon(iconType, tCol, 16), null, null, null);
            b.setCompoundDrawablePadding(dp(8));
        } else {
            b.setGravity(Gravity.CENTER);
        }
        return b;
    }

    private LinearLayout createModalHeader(String title, int iconType, Runnable onClose) {
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setPadding(dp(2), 0, dp(2), dp(10));

        if (iconType > 0) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(icon(iconType, Color.parseColor(getAccentHex()), 20));
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(22), dp(22));
            ivLp.setMargins(0, 0, dp(9), 0);
            hdr.addView(iv, ivLp);
        }

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvTitle.setTextSize(16.5f);
        hdr.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton btnX = new ImageButton(this);
        btnX.setImageDrawable(icon(IC_CLOSE, Color.parseColor("#E2E8F0"), 16));
        btnX.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btnX.setBackground(makeGlassDrawable(
                Color.parseColor("#33FFFFFF"),
                Color.parseColor("#18FFFFFF"),
                Color.parseColor("#55FFFFFF"),
                11, 1));
        btnX.setOnClickListener(v -> onClose.run());
        hdr.addView(btnX, new LinearLayout.LayoutParams(dp(34), dp(34)));
        return hdr;
    }

    private TextView makeSectionLabel(String title, String accentHex) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.parseColor(accentHex));
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setTextSize(10.8f);
        tv.setLetterSpacing(0.06f);
        tv.setPadding(dp(2), 0, 0, dp(7));
        return tv;
    }

    private LinearLayout createModalOuterContainer() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackground(makeGlassDrawable(
                Color.parseColor("#F50A1429"),
                Color.parseColor("#F5050B18"),
                Color.parseColor("#88" + getAccentHex().substring(1)),
                24, 1));
        outer.setPadding(dp(14), dp(14), dp(14), dp(14));
        return outer;
    }

    private void showGlassModal(Dialog dialog, LinearLayout outer, boolean tall) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(12), dp(14), dp(12), dp(14));
        wrap.addView(outer);
        dialog.setContentView(wrap);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int h = tall
                    ? (int) (getResources().getDisplayMetrics().heightPixels * 0.82f)
                    : ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, h);
            dialog.getWindow().setDimAmount(0.72f);
        }
        dialog.show();
    }

    // =========================================================================
    // LIQUID GLASS MAIN MENU (ORGANIZED, DRAWN ICONS, SEPARATE SETTINGS)
    // =========================================================================

    private void showLiquidGlassMainMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WebView wv = getCurrentWebView();
        String accent = getAccentHex();

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("NOVA Browser  v" + APP_VERSION, IC_BOLT, dialog::dismiss));

        ScrollView sc = new ScrollView(this);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        sc.addView(stack);

        // Section 1: Core Hub
        LinearLayout hubCard = createGlassCard();
        hubCard.addView(makeSectionLabel("QUICK HUB", accent));
        addMenuPairRow(hubCard,
            "NOVA Shield", IC_SHIELD, () -> { dialog.dismiss(); showOrganizedShieldDashboard(); },
            "Downloads", IC_DOWNLOAD, () -> { dialog.dismiss(); showTurboDownloadManager(); });
        addMenuPairRow(hubCard,
            "Temp Mail", IC_MAIL, () -> { dialog.dismiss(); showTempMailDialog(); },
            "Themes & WP", IC_PALETTE, () -> { dialog.dismiss(); showCustomizeThemeDialog(); });
        stack.addView(hubCard);

        // Section 2: Current Page & Privacy
        LinearLayout pageCard = createGlassCard();
        pageCard.addView(makeSectionLabel("PAGE & PRIVACY", accent));
        addMenuPairRow(pageCard,
            isNoJsBypassActive ? "Bypass (No-JS)" : "Login Bypass", IC_UNLOCK, () -> { dialog.dismiss(); showLoginBypassDialog(wv); },
            "Reader View", IC_BOOK, () -> { dialog.dismiss(); applyCleanReaderMode(wv); });
        addMenuPairRow(pageCard,
            "Desktop: " + (isDesktopMode ? "ON" : "OFF"), IC_DESKTOP, () -> { dialog.dismiss(); toggleDesktopMode(); },
            "Private: " + (isIncognito ? "ON" : "OFF"), IC_PRIVATE, () -> { dialog.dismiss(); toggleIncognitoMode(); });
        addMenuPairRow(pageCard,
            "Find in Page", IC_SEARCH, () -> { dialog.dismiss(); layoutFind.setVisibility(View.VISIBLE); etFind.requestFocus(); },
            "Share / QR", IC_SHARE, () -> { dialog.dismiss(); shareOrCopyUrl(wv); });
        stack.addView(pageCard);

        // Section 3: Library & Settings
        LinearLayout moreCard = createGlassCard();
        moreCard.addView(makeSectionLabel("LIBRARY & SYSTEM", accent));
        addMenuPairRow(moreCard,
            "History", IC_HISTORY, () -> { dialog.dismiss(); showHistoryDialog(); },
            "Settings", IC_SETTINGS, () -> { dialog.dismiss(); showSettingsDialog(); });
        stack.addView(moreCard);

        Button btnDev = createGlassButton("About & Contact Developer", IC_USER, "#0072FF", "#4F46E5", "#FFFFFF");
        btnDev.setOnClickListener(v -> { dialog.dismiss(); showAboutDeveloperDialog(); });
        stack.addView(btnDev, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        outer.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        showGlassModal(dialog, outer, false);
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WebView wv = getCurrentWebView();
        String accent = getAccentHex();

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("Browser Settings", IC_SETTINGS, dialog::dismiss));

        LinearLayout card = createGlassCard();
        card.addView(makeSectionLabel("PREFERENCES & PERFORMANCE", accent));
        addMenuPairRow(card,
            "Engine: " + searchEngineName, IC_SEARCH, () -> { dialog.dismiss(); showSearchEngineDialog(); },
            "RAM (" + getRamUsageMb() + ")", IC_BOLT, () -> { dialog.dismiss(); showPerformanceOptimizerDialog(); });
        addMenuPairRow(card,
            "Zoom: " + textZoomPercent + "%", IC_BOOK, () -> { dialog.dismiss(); showZoomDialog(); },
            "Data Saver: " + (isDataSaver ? "ON" : "OFF"), IC_IMAGE, () -> {
                dialog.dismiss();
                isDataSaver = !isDataSaver;
                for (WebView tab : tabs) tab.getSettings().setBlockNetworkImage(isDataSaver);
                if (wv != null) wv.reload();
                Toast.makeText(this, "Data Saver " + (isDataSaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        addMenuPairRow(card,
            "FullScreen: " + (isFullScreen ? "ON" : "OFF"), IC_DESKTOP, () -> { dialog.dismiss(); toggleFullScreen(); },
            "Translate Page", IC_GLOBE, () -> {
                dialog.dismiss();
                if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                    wv.loadUrl("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(wv.getUrl()));
                }
            });
        outer.addView(card);

        Button btnClear = createGlassButton("Clear Cache, Cookies & History", IC_TRASH, "#33EF4444", "#1AEF4444", "#FCA5A5");
        btnClear.setOnClickListener(v -> { dialog.dismiss(); clearBrowserData(wv); });
        outer.addView(btnClear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        showGlassModal(dialog, outer, false);
    }

    private void addMenuPairRow(LinearLayout parent, String leftLabel, int leftIcon, Runnable leftAct,
                                String rightLabel, int rightIcon, Runnable rightAct) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, 0, 0, dp(7));

        Button b1 = createGlassButton(leftLabel, leftIcon, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b1.setOnClickListener(v -> leftAct.run());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(39), 1f);
        lp1.setMargins(0, 0, dp(7), 0);
        row.addView(b1, lp1);

        Button b2 = createGlassButton(rightLabel, rightIcon, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b2.setOnClickListener(v -> rightAct.run());
        row.addView(b2, new LinearLayout.LayoutParams(0, dp(39), 1f));

        parent.addView(row, rLp);
    }

    // =========================================================================
    // LIQUID GLASS NOVA SHIELD v5 DASHBOARD (VERIFIABLE ANALYTICS, ZERO EMOJIS)
    // =========================================================================

    private void showOrganizedShieldDashboard() {
        WebView wv = getCurrentWebView();
        String currentUrl = (wv != null && wv.getUrl() != null) ? wv.getUrl() : "nova://home";
        String host = Uri.parse(currentUrl).getHost();
        boolean isHome = host == null || host.contains("nova.browser");
        boolean isHttps = currentUrl.startsWith("https://");
        int rulesLoaded = adHostHashes.length + trackerHostHashes.length;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("NOVA Shield v5", IC_SHIELD, dialog::dismiss));

        // Master Status Card
        LinearLayout masterCard = createGlassCard();
        TextView tvStatus = new TextView(this);
        tvStatus.setText(isAdBlockEnabled
                ? "ACTIVE  •  " + String.format(Locale.US, "%,d", rulesLoaded) + " Filter Rules"
                : "PAUSED  •  Filtering Disabled");
        tvStatus.setTextColor(Color.parseColor(isAdBlockEnabled ? "#00F5A0" : "#FB7185"));
        tvStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvStatus.setTextSize(13.5f);
        masterCard.addView(tvStatus);

        TextView tvSite = new TextView(this);
        tvSite.setText(isHome
                ? "Local Start Page  •  3rd-Party Cookies Blocked"
                : host + "  •  " + (isHttps ? "HTTPS Encrypted" : "Unencrypted HTTP"));
        tvSite.setTextColor(Color.parseColor("#CBD5E1"));
        tvSite.setTextSize(12f);
        tvSite.setPadding(0, dp(3), 0, dp(10));
        masterCard.addView(tvSite);

        Button btnMaster = createGlassButton(
                isAdBlockEnabled ? "Pause Shield Protection" : "Activate Shield Protection",
                isAdBlockEnabled ? IC_PAUSE : IC_SHIELD,
                isAdBlockEnabled ? "#33EF4444" : "#00B09B",
                isAdBlockEnabled ? "#1AEF4444" : "#00F5A0",
                isAdBlockEnabled ? "#FCA5A5" : "#041E15");
        btnMaster.setOnClickListener(v -> {
            isAdBlockEnabled = !isAdBlockEnabled;
            prefs.edit().putBoolean("adblock_enabled", isAdBlockEnabled).apply();
            applyLiquidGlassTheme();
            if (wv != null) wv.reload();
            dialog.dismiss();
        });
        masterCard.addView(btnMaster, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(39)));
        outer.addView(masterCard);

        // 3 Verifiable Live Analytics Cards
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.setMargins(0, 0, 0, dp(10));
        statsRow.addView(makeMiniStatCard(String.valueOf(pageBlockedCount), "This Page", getAccentHex()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statsRow.addView(makeMiniStatCard(String.valueOf(sessionBlockedCount), "Session", "#FFFFFF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statsRow.addView(makeMiniStatCard(String.valueOf(adsBlockedCount + trackersBlockedCount), "Total Blocked", "#00F5A0"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        outer.addView(statsRow, srLp);

        // Protection Modules & Sources Card
        LinearLayout modCard = createGlassCard();
        modCard.addView(makeSectionLabel("ACTIVE FILTER MODULES", getAccentHex()));

        TextView tvMods = new TextView(this);
        tvMods.setText("Lists: StevenBlack, HaGeZi Light, AdAway, Peter Lowe\n"
                + "Index: 64-bit FNV-1a Hash Array (~1.4 MB Heap)\n"
                + "YouTube & Spotify Ad Filtering: Enabled\n"
                + "Interstitial & Pop-Up Blocker: " + (isAggressiveInterstitialBlock ? "Enabled" : "Disabled"));
        tvMods.setTextColor(Color.parseColor("#CBD5E1"));
        tvMods.setTextSize(11.8f);
        tvMods.setLineSpacing(dp(2), 1f);
        tvMods.setPadding(0, 0, 0, dp(9));
        modCard.addView(tvMods);

        Button btnTogglePop = createGlassButton(
                isAggressiveInterstitialBlock ? "Pop-Up & Interstitial Killer: ON" : "Pop-Up & Interstitial Killer: OFF",
                IC_BOLT, "#2B00D2FF", "#166366F1", "#E0F2FE");
        btnTogglePop.setOnClickListener(v -> {
            isAggressiveInterstitialBlock = !isAggressiveInterstitialBlock;
            prefs.edit().putBoolean("aggressive_interstitial", isAggressiveInterstitialBlock).apply();
            if (wv != null) wv.reload();
            dialog.dismiss();
        });
        modCard.addView(btnTogglePop, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        outer.addView(modCard);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSync = createGlassButton("Update Lists", IC_RELOAD, "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, dp(39), 1f);
        if (!isHome) sLp.setMargins(0, 0, dp(8), 0);
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing filter database...", Toast.LENGTH_SHORT).show();
            loadCachedAndSyncFilterLists(true);
            dialog.dismiss();
        });
        bottomRow.addView(btnSync, sLp);

        if (!isHome) {
            boolean allowlisted = allowlistDomains.contains(host);
            Button btnAllow = createGlassButton(
                    allowlisted ? "Untrust Site" : "Trust Site",
                    IC_CHECK, "#33FFD000", "#1AFFD000", "#FFE566");
            btnAllow.setOnClickListener(v -> {
                if (allowlisted) allowlistDomains.remove(host);
                else allowlistDomains.add(host);
                prefs.edit().putStringSet("allowlist", allowlistDomains).apply();
                if (wv != null) wv.reload();
                dialog.dismiss();
            });
            bottomRow.addView(btnAllow, new LinearLayout.LayoutParams(0, dp(39), 1f));
        }
        outer.addView(bottomRow);

        showGlassModal(dialog, outer, false);
    }

    private LinearLayout makeMiniStatCard(String value, String label, String colorHex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(6), dp(9), dp(6), dp(9));
        box.setBackground(makeGlassDrawable(
                Color.parseColor("#B8162544"),
                Color.parseColor("#B80B1426"),
                Color.parseColor("#44FFFFFF"),
                13, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), 0, dp(3), 0);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(Color.parseColor(colorHex));
        tvVal.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
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
    // UPGRADED 4-MODE LOGIN / SIGN-UP WALL BYPASS (LIQUID GLASS MODAL)
    // =========================================================================

    private void resetTabNoJsMode(WebView wv) {
        if (isNoJsBypassActive && wv != null) {
            isNoJsBypassActive = false;
            wv.getSettings().setJavaScriptEnabled(true);
        }
    }

    private void showLoginBypassDialog(WebView wv) {
        if (wv == null || wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser")) {
            Toast.makeText(this, "Open a website first to bypass a login wall", Toast.LENGTH_SHORT).show();
            return;
        }
        final String currentUrl = wv.getUrl();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("Login & Wall Bypass", IC_UNLOCK, dialog::dismiss));

        LinearLayout card = createGlassCard();
        card.addView(makeSectionLabel("SELECT BYPASS METHOD", getAccentHex()));

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        bLp.setMargins(0, 0, 0, dp(7));

        Button b1 = createGlassButton("1. Persistent Modal, Blur & Scroll Unlock", IC_UNLOCK, "#0072FF", "#00C6FF", "#FFFFFF");
        b1.setOnClickListener(v -> {
            dialog.dismiss();
            injectPersistentWallBypass(wv);
            Toast.makeText(this, "Persistent overlay & scroll-lock killer active", Toast.LENGTH_SHORT).show();
        });
        card.addView(b1, bLp);

        Button b2 = createGlassButton("2. Reload via Google Referer (Fresh Cookies)", IC_RELOAD, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b2.setOnClickListener(v -> {
            dialog.dismiss();
            CookieManager.getInstance().removeSessionCookies(null);
            CookieManager.getInstance().flush();
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.google.com/");
            wv.loadUrl(currentUrl, headers);
            Toast.makeText(this, "Reloading with Google referer...", Toast.LENGTH_SHORT).show();
        });
        card.addView(b2, bLp);

        Button b3 = createGlassButton(
                isNoJsBypassActive ? "3. No-JavaScript Mode: ON (Tap to Restore)" : "3. No-JavaScript Mode (Defeats JS Gates)",
                IC_BOLT,
                isNoJsBypassActive ? "#33FFD000" : "#2BFFFFFF",
                isNoJsBypassActive ? "#1AFFD000" : "#12FFFFFF",
                isNoJsBypassActive ? "#FFE566" : "#F8FAFC");
        b3.setOnClickListener(v -> {
            dialog.dismiss();
            isNoJsBypassActive = !isNoJsBypassActive;
            wv.getSettings().setJavaScriptEnabled(!isNoJsBypassActive);
            wv.reload();
            Toast.makeText(this, isNoJsBypassActive
                    ? "JavaScript disabled for this page"
                    : "JavaScript restored", Toast.LENGTH_SHORT).show();
        });
        card.addView(b3, bLp);

        Button b4 = createGlassButton("4. Clean Reader Extractor", IC_BOOK, "#2BFFFFFF", "#12FFFFFF", "#F8FAFC");
        b4.setOnClickListener(v -> {
            dialog.dismiss();
            applyCleanReaderMode(wv);
        });
        card.addView(b4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        outer.addView(card);
        showGlassModal(dialog, outer, false);
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
        String bg = THEME_FROST.equals(currentTheme) ? "#FFFFFF" : "#0B1224";
        String fg = THEME_FROST.equals(currentTheme) ? "#0F172A" : "#F8FAFC";
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

    private void showTempMailDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("NOVA Temp Mail", IC_MAIL, dialog::dismiss));

        LinearLayout addrCard = createGlassCard();
        TextView tvProv = new TextView(this);
        String prov = prefs.getString("tm_provider_v2", "");
        tvProv.setText(prov.isEmpty() ? "DISPOSABLE ADDRESS" : "DISPOSABLE ADDRESS  •  " + prov.toUpperCase(Locale.US));
        tvProv.setTextColor(Color.parseColor("#94A3B8"));
        tvProv.setTextSize(10.5f);
        tvProv.setLetterSpacing(0.05f);
        addrCard.addView(tvProv);

        TextView tvEmail = new TextView(this);
        String savedEmail = prefs.getString("tm_addr_v2", "");
        tvEmail.setText(savedEmail.isEmpty() ? "Generating unique address..." : savedEmail);
        tvEmail.setTextColor(Color.parseColor(getAccentHex()));
        tvEmail.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvEmail.setTextSize(14.5f);
        tvEmail.setPadding(0, dp(4), 0, dp(10));
        addrCard.addView(tvEmail);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCopy = createGlassButton("Copy Address", IC_COPY, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        cLp.setMargins(0, 0, dp(8), 0);
        btnCopy.setOnClickListener(v -> {
            String em = prefs.getString("tm_addr_v2", "");
            if (!em.isEmpty()) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("NOVA Temp Mail", em));
                    Toast.makeText(this, "Copied: " + em, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnRow.addView(btnCopy, cLp);

        Button btnNew = createGlassButton("New Email", IC_RELOAD, "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        btnRow.addView(btnNew, new LinearLayout.LayoutParams(0, dp(38), 1f));
        addrCard.addView(btnRow);
        outer.addView(addrCard);

        LinearLayout inboxHdr = new LinearLayout(this);
        inboxHdr.setOrientation(LinearLayout.HORIZONTAL);
        inboxHdr.setGravity(Gravity.CENTER_VERTICAL);
        inboxHdr.setPadding(dp(2), dp(2), dp(2), dp(8));

        TextView tvInboxLbl = makeSectionLabel("LIVE INBOX (OTPs & LINKS)", getAccentHex());
        inboxHdr.addView(tvInboxLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnRefresh = createGlassButton("Refresh", IC_RELOAD, "#2B00D2FF", "#166366F1", "#E0F2FE");
        inboxHdr.addView(btnRefresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        outer.addView(inboxHdr);

        ScrollView scInbox = new ScrollView(this);
        scInbox.setVerticalScrollBarEnabled(false);
        LinearLayout inboxContainer = new LinearLayout(this);
        inboxContainer.setOrientation(LinearLayout.VERTICAL);
        scInbox.addView(inboxContainer);
        outer.addView(scInbox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Runnable refreshAction = () -> fetchTempMailInbox(tvEmail, tvProv, inboxContainer);
        btnRefresh.setOnClickListener(v -> refreshAction.run());
        btnNew.setOnClickListener(v -> createFreshTempMailAccount(tvEmail, tvProv, inboxContainer));

        if (savedEmail.isEmpty() || prefs.getString("tm_token_v2", "").isEmpty()) {
            createFreshTempMailAccount(tvEmail, tvProv, inboxContainer);
        } else {
            refreshAction.run();
        }

        showGlassModal(dialog, outer, true);
    }

    private String httpMailApi(String urlStr, String method, String jsonBody, String bearerToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "application/json");
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
            tvEmail.setText("Connecting to mail server...");
            inboxContainer.removeAllViews();
        });
        new Thread(() -> {
            // Engine 1: tempmail.lol v2
            try {
                String res = httpMailApi("https://api.tempmail.lol/v2/inbox/create", "POST", "{}", null);
                JSONObject obj = new JSONObject(res);
                saveTempMailSession("tempmail.lol", obj.getString("address"), obj.getString("token"), tvEmail, tvProv, inboxContainer);
                return;
            } catch (Exception ignored) {}

            // Engine 2 & 3: mail.tm and mail.gw
            String[] bases = {"https://api.mail.tm", "https://api.mail.gw"};
            String[] names = {"mail.tm", "mail.gw"};
            for (int i = 0; i < bases.length; i++) {
                try {
                    String domStr = httpMailApi(bases[i] + "/domains", "GET", null, null).trim();
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

                    httpMailApi(bases[i] + "/accounts", "POST", creds.toString(), null);
                    String tokJson = httpMailApi(bases[i] + "/token", "POST", creds.toString(), null);
                    saveTempMailSession(names[i], email, new JSONObject(tokJson).getString("token"), tvEmail, tvProv, inboxContainer);
                    return;
                } catch (Exception ignored) {}
            }

            // Engine 4: Guerrilla Mail
            try {
                String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_address&lang=en", "GET", null, null);
                JSONObject obj = new JSONObject(res);
                saveTempMailSession("guerrillamail", obj.getString("email_addr"), obj.getString("sid_token"), tvEmail, tvProv, inboxContainer);
                return;
            } catch (Exception ignored) {}

            uiHandler.post(() -> {
                tvEmail.setText("Servers busy — tap New Email");
                tvProv.setText("CONNECTION FAILED");
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
            tvProv.setText("DISPOSABLE ADDRESS  •  " + provider.toUpperCase(Locale.US));
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
            tvLoad.setText("Refreshing inbox...");
            tvLoad.setTextColor(Color.parseColor("#94A3B8"));
            tvLoad.setTextSize(12.5f);
            tvLoad.setPadding(dp(6), dp(10), dp(6), dp(10));
            inboxContainer.addView(tvLoad);
        });

        new Thread(() -> {
            try {
                final List<TempMailItem> items = new ArrayList<>();
                if ("tempmail.lol".equals(provider)) {
                    String res = httpMailApi("https://api.tempmail.lol/v2/inbox?token=" + Uri.encode(token), "GET", null, null);
                    JSONObject obj = new JSONObject(res);
                    if (obj.optBoolean("expired", false)) throw new IOException("EXPIRED");
                    JSONArray arr = obj.optJSONArray("emails");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject m = arr.getJSONObject(i);
                            TempMailItem item = new TempMailItem();
                            item.id = String.valueOf(i);
                            item.subject = m.optString("subject", "(No Subject)");
                            item.from = m.optString("from", "Sender");
                            String plain = m.optString("body", "");
                            if (plain.isEmpty()) plain = stripHtml(m.optString("html", ""));
                            item.inlineBody = plain;
                            item.excerpt = plain.length() > 90 ? plain.substring(0, 90).replace('\n', ' ') + "..." : plain.replace('\n', ' ');
                            items.add(item);
                        }
                    }
                } else if ("guerrillamail".equals(provider)) {
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=get_email_list&offset=0&sid_token=" + Uri.encode(token), "GET", null, null);
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
                    String res = httpMailApi(base + "/messages", "GET", null, token).trim();
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
                        LinearLayout empty = createGlassCard();
                        TextView tvE = new TextView(this);
                        tvE.setText("Inbox is empty.\nCopy your address above, use it on a signup form, and tap Refresh to read OTPs or verification links.");
                        tvE.setTextColor(Color.parseColor("#CBD5E1"));
                        tvE.setTextSize(12.2f);
                        empty.addView(tvE);
                        inboxContainer.addView(empty);
                        return;
                    }
                    for (TempMailItem m : items) {
                        LinearLayout card = createGlassCard();
                        TextView tvS = new TextView(this);
                        tvS.setText(m.subject);
                        tvS.setTextColor(Color.parseColor("#FFFFFF"));
                        tvS.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                        tvS.setTextSize(13.5f);
                        card.addView(tvS);

                        TextView tvF = new TextView(this);
                        tvF.setText("From: " + m.from + (m.excerpt.isEmpty() ? "" : "\n" + m.excerpt));
                        tvF.setTextColor(Color.parseColor("#CBD5E1"));
                        tvF.setTextSize(11.5f);
                        tvF.setPadding(0, dp(3), 0, 0);
                        card.addView(tvF);

                        card.setOnClickListener(v -> openTempMailMessage(provider, m, token));
                        inboxContainer.addView(card);
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    inboxContainer.removeAllViews();
                    LinearLayout err = createGlassCard();
                    TextView tvErr = new TextView(this);
                    tvErr.setText("Session expired or server unavailable. Tap New Email to start a fresh mailbox.");
                    tvErr.setTextColor(Color.parseColor("#FFD000"));
                    tvErr.setTextSize(12.2f);
                    err.addView(tvErr);
                    inboxContainer.addView(err);
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
                    String res = httpMailApi("https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=" + Uri.encode(item.id) + "&sid_token=" + Uri.encode(token), "GET", null, null);
                    bodyText = stripHtml(new JSONObject(res).optString("mail_body", ""));
                } else {
                    String base = "mail.gw".equals(provider) ? "https://api.mail.gw" : "https://api.mail.tm";
                    String res = httpMailApi(base + "/messages/" + item.id, "GET", null, token);
                    JSONObject obj = new JSONObject(res);
                    bodyText = obj.optString("text", "");
                    if (bodyText.isEmpty() && obj.has("html")) bodyText = stripHtml(obj.optString("html", ""));
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
                    cb.setPrimaryClip(ClipData.newPlainText("OTP Code", otpCode));
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
    // VIBRANT LIQUID GLASS THEMES & WALLPAPER CUSTOMIZATION
    // =========================================================================

    private void showCustomizeThemeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("Themes & Wallpapers", IC_PALETTE, dialog::dismiss));

        ScrollView sc = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        sc.addView(body);

        LinearLayout themeCard = createGlassCard();
        themeCard.addView(makeSectionLabel("VIBRANT LIQUID GLASS PALETTES", getAccentHex()));

        String[][] themes = {
            {"Electric Sapphire", THEME_SAPPHIRE, "#0072FF", "#00C6FF", "#FFFFFF"},
            {"AMOLED Neon", THEME_AMOLED, "#0D1117", "#000000", "#00E5FF"},
            {"Neon Cyberpunk", THEME_CYBERPUNK, "#7B2FF7", "#F107A3", "#FFFFFF"},
            {"Solar Sunset", THEME_SUNSET, "#FF512F", "#F09819", "#FFFFFF"},
            {"Cyber Emerald", THEME_EMERALD, "#00B09B", "#00F5A0", "#052018"},
            {"Crystal Frost", THEME_FROST, "#E0F2FE", "#BAE6FD", "#0F172A"}
        };

        for (int i = 0; i < themes.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, 0, 0, dp(7));

            for (int j = 0; j < 2 && (i + j) < themes.length; j++) {
                final String label = themes[i + j][0];
                final String key = themes[i + j][1];
                final String c1 = themes[i + j][2];
                final String c2 = themes[i + j][3];
                final String txtCol = themes[i + j][4];
                boolean isSel = currentTheme.equals(key);

                Button btn = createGlassButton(label, isSel ? IC_CHECK : 0, c1, c2, txtCol);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
                if (j == 0) bLp.setMargins(0, 0, dp(7), 0);
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
        wpCard.addView(makeSectionLabel("CUSTOM HOMEPAGE WALLPAPER", getAccentHex()));

        TextView tvWpSub = new TextView(this);
        tvWpSub.setText("Pick any image from your phone gallery to glow behind the frosted Liquid Glass home screen.");
        tvWpSub.setTextColor(Color.parseColor("#CBD5E1"));
        tvWpSub.setTextSize(12f);
        tvWpSub.setPadding(0, 0, 0, dp(10));
        wpCard.addView(tvWpSub);

        Button btnPickWp = createGlassButton("Choose Image from Gallery", IC_IMAGE, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        pickLp.setMargins(0, 0, 0, dp(7));
        btnPickWp.setOnClickListener(v -> {
            dialog.dismiss();
            wallpaperPickerLauncher.launch("image/*");
        });
        wpCard.addView(btnPickWp, pickLp);

        Button btnClearWp = createGlassButton("Reset Wallpaper", IC_TRASH, "#33EF4444", "#1AEF4444", "#FCA5A5");
        btnClearWp.setOnClickListener(v -> {
            File wpFile = new File(getFilesDir(), "nova_home_wallpaper.b64");
            if (wpFile.exists()) wpFile.delete();
            Toast.makeText(this, "Wallpaper reset", Toast.LENGTH_SHORT).show();
            WebView wv = getCurrentWebView();
            if (wv != null && (wv.getUrl() == null || wv.getUrl().startsWith("https://nova.browser"))) {
                loadNovaHome(wv);
            }
            dialog.dismiss();
        });
        wpCard.addView(btnClearWp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        body.addView(wpCard);

        outer.addView(sc);
        showGlassModal(dialog, outer, false);
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
                    Toast.makeText(this, "Custom wallpaper applied", Toast.LENGTH_SHORT).show();
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
    // LIQUID GLASS HOMEPAGE WITH REAL DRAWN INLINE SVG BRAND LOGOS (ZERO EMOJIS)
    // =========================================================================

    private void loadNovaHome(WebView webView) {
        int rulesCount = adHostHashes.length + trackerHostHashes.length;
        String accent = getAccentHex();
        String accent2 = getSecondaryAccentHex();
        boolean isLight = THEME_FROST.equals(currentTheme);

        String customWpB64 = readCustomWallpaperBase64();
        String bgCss;
        if (customWpB64 != null && !customWpB64.isEmpty()) {
            bgCss = "linear-gradient(rgba(5,10,22,0.48),rgba(5,10,22,0.68)),url('data:image/jpeg;base64," + customWpB64 + "') center/cover no-repeat fixed";
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

        boolean darkText = isLight && customWpB64 == null;
        String glass = darkText ? "rgba(255,255,255,0.74)" : "rgba(255,255,255,0.10)";
        String border = darkText ? "rgba(0,102,255,0.26)" : "rgba(255,255,255,0.22)";
        String text = darkText ? "#0F172A" : "#FFFFFF";
        String sub = darkText ? "#475569" : "#CBD5E1";

        // Real Crisp Inline SVG Logos for Shortcuts & Quick Actions
        String svgGoogle = "<svg viewBox='0 0 24 24' width='24' height='24'><path fill='#4285F4' d='M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z'/><path fill='#34A853' d='M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z'/><path fill='#FBBC05' d='M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z'/><path fill='#EA4335' d='M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z'/></svg>";
        String svgYoutube = "<svg viewBox='0 0 24 24' width='24' height='24'><rect x='2' y='5' width='20' height='14' rx='4' fill='#FF1E1E'/><polygon points='10,8.5 16,12 10,15.5' fill='#FFFFFF'/></svg>";
        String svgSpotify = "<svg viewBox='0 0 24 24' width='24' height='24'><circle cx='12' cy='12' r='10' fill='#1DB954'/><path fill='none' stroke='#04140A' stroke-width='1.9' stroke-linecap='round' d='M7 9.6c3.5-1 7.5-.7 10.3.9M7.8 12.8c2.9-.8 6.2-.5 8.6.8M8.7 15.8c2.3-.6 4.8-.4 6.7.6'/></svg>";
        String svgShield = "<svg viewBox='0 0 24 24' width='24' height='24' fill='none' stroke='" + accent + "' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M12 3l8 3.5v5.5c0 4.8-3.4 8.4-8 9.8-4.6-1.4-8-5-8-9.8V6.5L12 3z'/><path d='M8.5 12.2l2.4 2.4 4.6-4.8'/></svg>";
        String svgGithub = "<svg viewBox='0 0 24 24' width='24' height='24' fill='" + text + "'><path d='M12 2C6.48 2 2 6.58 2 12.26c0 4.53 2.87 8.37 6.84 9.73.5.09.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.37-3.37-1.37-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.85.09-.66.35-1.12.63-1.38-2.22-.26-4.56-1.14-4.56-5.08 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.71 0 0 .84-.28 2.75 1.05A9.35 9.35 0 0 1 12 6.84c.85 0 1.71.12 2.51.35 1.91-1.33 2.75-1.05 2.75-1.05.55 1.41.2 2.45.1 2.71.64.72 1.03 1.64 1.03 2.76 0 3.95-2.34 4.82-4.57 5.07.36.32.68.94.68 1.9 0 1.37-.01 2.48-.01 2.82 0 .27.18.59.69.49A10.22 10.22 0 0 0 22 12.26C22 6.58 17.52 2 12 2z'/></svg>";
        String svgAi = "<svg viewBox='0 0 24 24' width='24' height='24' fill='none' stroke='#10B981' stroke-width='1.9' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='3.2'/><path d='M12 2.8v3.4M12 17.8v3.4M4.1 7.4l2.9 1.7M17 14.9l2.9 1.7M4.1 16.6L7 14.9M17 9.1l2.9-1.7'/></svg>";
        String svgReddit = "<svg viewBox='0 0 24 24' width='24' height='24'><circle cx='12' cy='12' r='10' fill='#FF4500'/><circle cx='9.2' cy='12.5' r='1.3' fill='#FFFFFF'/><circle cx='14.8' cy='12.5' r='1.3' fill='#FFFFFF'/><path fill='none' stroke='#FFFFFF' stroke-width='1.6' stroke-linecap='round' d='M9 15.8c1.8 1.2 4.2 1.2 6 0M12 8.5l1.2-3.2 2.6.6'/><circle cx='16.5' cy='6.1' r='1.1' fill='#FFFFFF'/></svg>";
        String svgSpeed = "<svg viewBox='0 0 24 24' width='24' height='24' fill='none' stroke='#F59E0B' stroke-width='2' stroke-linecap='round'><path d='M4.5 17.5A9 9 0 1 1 19.5 17.5'/><line x1='12' y1='13' x2='16.5' y2='8.5'/><circle cx='12' cy='13' r='1.6' fill='#F59E0B'/></svg>";
        String svgPalette = "<svg viewBox='0 0 24 24' width='16' height='16' fill='none' stroke='" + accent + "' stroke-width='2'><circle cx='12' cy='12' r='9'/><circle cx='9' cy='9.5' r='1.2' fill='" + accent + "'/><circle cx='15' cy='9.5' r='1.2' fill='" + accent + "'/><circle cx='9.5' cy='14.5' r='1.2' fill='" + accent + "'/></svg>";
        String svgMail = "<svg viewBox='0 0 24 24' width='16' height='16' fill='none' stroke='" + accent + "' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='5' width='18' height='14' rx='2'/><path d='M3.5 6.5l8.5 6.5 8.5-6.5'/></svg>";

        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;}"
            + "body{margin:0;min-height:100vh;padding:26px 14px 28px;font-family:-apple-system,Roboto,sans-serif;background:" + bgCss + ";color:" + text + ";text-align:center;}"
            + ".logo{font-size:32px;font-weight:900;letter-spacing:3px;background:linear-gradient(120deg," + accent + "," + accent2 + ");-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-top:6px;}"
            + ".tag{font-size:11.5px;color:" + sub + ";margin:5px 0 20px;letter-spacing:0.4px;}"
            + ".glass{background:" + glass + ";backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);border:1px solid " + border + ";box-shadow:0 8px 28px rgba(0,0,0,0.28);}"
            + ".search-box{display:flex;align-items:center;border-radius:24px;padding:5px 10px;margin:0 auto 18px;max-width:480px;}"
            + ".search-box input{flex:1;background:transparent;border:none;color:" + text + ";font-size:14.5px;outline:none;padding:8px 8px;}"
            + ".search-box button{background:linear-gradient(135deg," + accent + "," + accent2 + ");color:#FFFFFF;border:none;border-radius:16px;padding:8px 18px;font-weight:700;font-size:13px;}"
            + ".stats{display:flex;justify-content:space-around;border-radius:18px;padding:12px 6px;margin:0 auto 18px;max-width:480px;}"
            + ".stat-num{font-size:16px;font-weight:800;color:" + accent + ";}"
            + ".stat-lbl{font-size:10px;color:" + sub + ";margin-top:3px;}"
            + ".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;max-width:480px;margin:0 auto 20px;}"
            + ".tile{padding:13px 6px;border-radius:16px;text-decoration:none;color:" + text + ";font-size:11.5px;font-weight:600;display:flex;flex-direction:column;align-items:center;gap:7px;}"
            + ".quick-row{display:flex;gap:10px;max-width:480px;margin:0 auto;}"
            + ".qbtn{flex:1;padding:11px 10px;border-radius:16px;color:" + text + ";font-size:12.5px;font-weight:600;border:none;display:flex;align-items:center;justify-content:center;gap:7px;}"
            + "</style></head><body>"
            + "<div class='logo'>NOVA BROWSER</div>"
            + "<div class='tag'>Shield v5 &bull; " + String.format(Locale.US, "%,d", rulesCount) + " Filter Rules &bull; 4x Turbo Engine</div>"
            + "<form class='search-box glass' onsubmit=\"var q=document.getElementById('q').value.trim();if(q){window.location.href='" + searchEngineUrl + "'+encodeURIComponent(q);}return false;\">"
            + "<input id='q' type='text' placeholder='Search with " + searchEngineName + " or enter URL...'/>"
            + "<button type='submit'>GO</button></form>"
            + "<div class='stats glass' onclick='window.NovaShield.openShieldPage()'>"
            + "<div><div class='stat-num'>" + sessionBlockedCount + "</div><div class='stat-lbl'>Session Blocked</div></div>"
            + "<div><div class='stat-num'>" + (adsBlockedCount + trackersBlockedCount) + "</div><div class='stat-lbl'>Total Blocked</div></div>"
            + "<div><div class='stat-num'>" + String.format(Locale.US, "%,d", rulesCount) + "</div><div class='stat-lbl'>Active Rules</div></div>"
            + "<div><div class='stat-num'>" + getRamUsageMb() + "</div><div class='stat-lbl'>Heap Used</div></div>"
            + "</div>"
            + "<div class='grid'>"
            + "<a class='tile glass' href='https://www.google.com'>" + svgGoogle + "Google</a>"
            + "<a class='tile glass' href='https://m.youtube.com'>" + svgYoutube + "YouTube</a>"
            + "<a class='tile glass' href='https://open.spotify.com'>" + svgSpotify + "Spotify</a>"
            + "<a class='tile glass' href='https://adblock.turtlecute.org'>" + svgShield + "AdTest</a>"
            + "<a class='tile glass' href='https://github.com/shahalamlaskarofficial'>" + svgGithub + "GitHub</a>"
            + "<a class='tile glass' href='https://chatgpt.com'>" + svgAi + "ChatGPT</a>"
            + "<a class='tile glass' href='https://www.reddit.com'>" + svgReddit + "Reddit</a>"
            + "<a class='tile glass' href='https://fast.com'>" + svgSpeed + "Speed</a>"
            + "</div>"
            + "<div class='quick-row'>"
            + "<button class='qbtn glass' onclick='window.NovaShield.openCustomizePage()'>" + svgPalette + "Themes &amp; Wallpaper</button>"
            + "<button class='qbtn glass' onclick='window.NovaShield.openTempMailPage()'>" + svgMail + "Temp Mail</button>"
            + "</div>"
            + "</body></html>";

        webView.loadDataWithBaseURL("https://nova.browser", html, "text/html", "UTF-8", null);
        etUrl.setText("");
        etUrl.setHint("Search " + searchEngineName + " or enter URL...");
        pageBlockedCount = 0;
        animateBarsToDp(54);
    }

    // =========================================================================
    // BLOB, DATA & LONG-PRESS IMAGE DOWNLOADS
    // =========================================================================

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "").replace("\r", "");
    }

    private void fetchBlobFromWebView(WebView wv, String blobUrl, String suggestedName, String mimeType) {
        if (wv == null) return;
        String safeName = (suggestedName == null || suggestedName.isEmpty())
                ? "nova_image_" + System.currentTimeMillis() + ".png"
                : suggestedName;
        String safeMime = mimeType != null ? mimeType : "image/png";
        Toast.makeText(this, "Capturing image/blob file...", Toast.LENGTH_SHORT).show();

        String js = "(function(){"
            + "fetch('" + escapeJs(blobUrl) + "').then(function(r){return r.blob();}).then(function(b){"
            + "var rd=new FileReader();"
            + "rd.onloadend=function(){"
            + "if(window.NovaShield&&rd.result){"
            + "window.NovaShield.saveBlobData(rd.result,'" + escapeJs(safeName) + "',b.type||'" + escapeJs(safeMime) + "');}"
            + "};"
            + "rd.readAsDataURL(b);"
            + "}).catch(function(e){"
            + "if(window.NovaShield){window.NovaShield.reportBlobError(String(e));}"
            + "});})();";
        wv.evaluateJavascript(js, null);
    }

    private void showImageLongPressDialog(WebView wv, String imgUrl) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("Image Options", IC_IMAGE, dialog::dismiss));

        TextView tvUrl = new TextView(this);
        tvUrl.setText(imgUrl);
        tvUrl.setTextColor(Color.parseColor("#94A3B8"));
        tvUrl.setTextSize(11.5f);
        tvUrl.setSingleLine(true);
        tvUrl.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        tvUrl.setPadding(dp(2), 0, dp(2), dp(10));
        outer.addView(tvUrl);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        lp.setMargins(0, 0, 0, dp(7));

        Button btnDl = createGlassButton("Download Image", IC_DOWNLOAD, "#0072FF", "#00C6FF", "#FFFFFF");
        btnDl.setOnClickListener(v -> {
            dialog.dismiss();
            String guessed = URLUtil.guessFileName(imgUrl, null, "image/jpeg");
            if (imgUrl.startsWith("blob:")) {
                fetchBlobFromWebView(wv, imgUrl, guessed, "image/png");
            } else if (imgUrl.startsWith("data:")) {
                DownloadService.saveDataUrl(this, imgUrl, "nova_image_" + System.currentTimeMillis());
            } else {
                String cookie = CookieManager.getInstance().getCookie(imgUrl);
                DownloadService.startNewDownload(this, imgUrl, guessed, defaultUserAgent, cookie, wv.getUrl());
                updateDownloadDockBadge();
                Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show();
            }
        });
        outer.addView(btnDl, lp);

        Button btnOpen = createGlassButton("Open in New Tab", IC_PLUS, "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
        btnOpen.setOnClickListener(v -> { dialog.dismiss(); createNewTab(imgUrl); });
        outer.addView(btnOpen, lp);

        Button btnCopy = createGlassButton("Copy Image Link", IC_COPY, "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
        btnCopy.setOnClickListener(v -> {
            dialog.dismiss();
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Image URL", imgUrl));
                Toast.makeText(this, "Image link copied", Toast.LENGTH_SHORT).show();
            }
        });
        outer.addView(btnCopy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        showGlassModal(dialog, outer, false);
    }

    private boolean isStreamingWebpageUrl(String url) {
        String lower = url.toLowerCase(Locale.US);
        return lower.contains("youtube.com/watch")
                || lower.contains("youtu.be/")
                || lower.contains("youtube.com/shorts/")
                || lower.contains("open.spotify.com/");
    }

    // =========================================================================
    // LIQUID GLASS 4x TURBO DOWNLOAD MANAGER (ZERO EMOJIS, DRAWN ICONS)
    // =========================================================================

    private void promptNewTurboDownload(String url, String suggestedName, long contentLength, String cookie, String referer) {
        boolean isHttps = url.toLowerCase(Locale.US).startsWith("https://");
        String sizeText = contentLength > 0 ? DownloadService.formatBytes(contentLength) : "Auto-detect on start";

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("NOVA 4x Downloader", IC_DOWNLOAD, dialog::dismiss));

        TextView tvSec = new TextView(this);
        tvSec.setText((isHttps ? "HTTPS Encrypted" : "HTTP Stream") + "  •  " + sizeText + "\nForeground Service  •  Saves to Downloads/NOVA Browser");
        tvSec.setTextColor(Color.parseColor(isHttps ? "#00F5A0" : "#FFD000"));
        tvSec.setTextSize(11.8f);
        tvSec.setPadding(dp(2), 0, dp(2), dp(10));
        outer.addView(tvSec);

        EditText etName = new EditText(this);
        etName.setText(suggestedName);
        etName.setHint("File name...");
        etName.setTextColor(Color.parseColor("#FFFFFF"));
        etName.setHintTextColor(Color.parseColor("#64748B"));
        etName.setBackground(makeGlassDrawable(
                Color.parseColor("#AA060D1E"),
                Color.parseColor("#AA122140"),
                Color.parseColor("#6600D2FF"),
                12, 1));
        etName.setPadding(dp(12), dp(9), dp(12), dp(9));
        etName.setSingleLine(true);
        etName.setTextSize(13.5f);
        outer.addView(etName);

        Button btnStart = createGlassButton("Start 4x Turbo Download", IC_DOWNLOAD, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        startLp.setMargins(0, dp(12), 0, 0);
        btnStart.setOnClickListener(v -> {
            String finalName = etName.getText().toString().trim();
            if (finalName.isEmpty()) finalName = "nova_file_" + System.currentTimeMillis();
            DownloadService.startNewDownload(this, url, finalName, defaultUserAgent, cookie, referer);
            updateDownloadDockBadge();
            dialog.dismiss();
            showTurboDownloadManager();
        });
        outer.addView(btnStart, startLp);

        showGlassModal(dialog, outer, false);
    }

    private void showTurboDownloadManager() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outerGlass = createModalOuterContainer();
        outerGlass.addView(createModalHeader("NOVA 4x Downloads", IC_DOWNLOAD, dialog::dismiss));

        ScrollView scrollRoot = new ScrollView(this);
        scrollRoot.setVerticalScrollBarEnabled(false);
        LinearLayout stackContainer = new LinearLayout(this);
        stackContainer.setOrientation(LinearLayout.VERTICAL);
        scrollRoot.addView(stackContainer);

        LinearLayout statsCard = createGlassCard();
        long freeSpaceMb = DownloadService.getStagingDir(this).getFreeSpace() / (1024 * 1024);
        TextView tvEngine = new TextView(this);
        tvEngine.setText("4x Parallel 128 KB Foreground Engine  •  Supports Images, Blob & Direct Files\nFree Space: " + freeSpaceMb + " MB  •  Folder: Downloads/NOVA Browser");
        tvEngine.setTextColor(Color.parseColor(getAccentHex()));
        tvEngine.setTextSize(11.5f);
        statsCard.addView(tvEngine);
        stackContainer.addView(statsCard);

        LinearLayout linkCard = createGlassCard();
        linkCard.addView(makeSectionLabel("DIRECT LINK DOWNLOAD", getAccentHex()));

        EditText etDirectUrl = new EditText(this);
        etDirectUrl.setHint("Paste direct link (.jpg, .png, .mp4, .apk, .zip, .pdf)...");
        etDirectUrl.setTextColor(Color.parseColor("#FFFFFF"));
        etDirectUrl.setHintTextColor(Color.parseColor("#94A3B8"));
        etDirectUrl.setBackground(makeGlassDrawable(
                Color.parseColor("#AA060D1E"),
                Color.parseColor("#AA0E1B36"),
                Color.parseColor("#5500D2FF"),
                12, 1));
        etDirectUrl.setPadding(dp(12), dp(9), dp(12), dp(9));
        etDirectUrl.setSingleLine(true);
        etDirectUrl.setTextSize(13f);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        etLp.setMargins(0, 0, 0, dp(8));
        linkCard.addView(etDirectUrl, etLp);

        LinearLayout linkBtnRow = new LinearLayout(this);
        linkBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPaste = createGlassButton("Paste Link", IC_COPY, "#33FFFFFF", "#1AFFFFFF", "#FFFFFF");
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        pasteLp.setMargins(0, 0, dp(8), 0);
        linkBtnRow.addView(btnPaste, pasteLp);

        Button btnDirectGo = createGlassButton("Download", IC_DOWNLOAD, "#0072FF", "#00C6FF", "#FFFFFF");
        linkBtnRow.addView(btnDirectGo, new LinearLayout.LayoutParams(0, dp(38), 1f));
        linkCard.addView(linkBtnRow);
        stackContainer.addView(linkCard);

        btnPaste.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
                CharSequence clip = cb.getPrimaryClip().getItemAt(0).getText();
                if (clip != null) {
                    etDirectUrl.setText(clip.toString().trim());
                    Toast.makeText(this, "Link pasted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout queueSectionHeader = new LinearLayout(this);
        queueSectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        queueSectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        queueSectionHeader.setPadding(dp(2), dp(4), dp(2), dp(6));

        TextView tvQueueTitle = makeSectionLabel("ACTIVE & RECENT QUEUE", getAccentHex());
        queueSectionHeader.addView(tvQueueTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnClearDone = createGlassButton("Clear Done", IC_TRASH, "#26FFFFFF", "#12FFFFFF", "#CBD5E1");
        queueSectionHeader.addView(btnClearDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
        stackContainer.addView(queueSectionHeader);

        LinearLayout queueListLayout = new LinearLayout(this);
        queueListLayout.setOrientation(LinearLayout.VERTICAL);
        stackContainer.addView(queueListLayout);

        LinearLayout catCard = createGlassCard();
        catCard.addView(makeSectionLabel("SAVED FILES (DOWNLOADS/NOVA BROWSER)", getAccentHex()));

        String[] catNames = {"All", "APKs", "Videos", "Images", "Docs", "ZIPs"};
        String[][] catExts = {
            {}, {".apk"}, {".mp4", ".mkv", ".webm", ".avi"},
            {".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg"},
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
                Button chip = createGlassButton(catNames[idx], 0, "#2B00D2FF", "#166366F1", "#E0F2FE");
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(35), 1f);
                if (col < 2) cLp.setMargins(0, 0, dp(6), 0);
                chip.setOnClickListener(v -> showFilteredFiles(catNames[idx], catExts[idx]));
                rLayout.addView(chip, cLp);
            }
            catCard.addView(rLayout, rLp);
        }
        stackContainer.addView(catCard);

        outerGlass.addView(scrollRoot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button btnBgKeep = createGlassButton("Minimize (Keep Downloading in Background)", IC_CHECK, "#2B00D2FF", "#166366F1", "#7DD3FC");
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
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
                    tvEmpty.setText("No active downloads.\nLong-press any image on a webpage, or paste a direct file URL above.");
                    tvEmpty.setTextColor(Color.parseColor("#CBD5E1"));
                    tvEmpty.setTextSize(12.2f);
                    emptyCard.addView(tvEmpty);
                    queueListLayout.addView(emptyCard);
                } else {
                    for (DownloadService.TurboDownloadTask task : DownloadService.TASKS) {
                        LinearLayout card = createGlassCard();

                        TextView tvName = new TextView(this);
                        tvName.setTextColor(Color.parseColor("#FFFFFF"));
                        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                        tvName.setTextSize(13f);
                        tvName.setSingleLine(true);
                        tvName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        card.addView(tvName);

                        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                        pb.setMax(100);
                        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
                        pbLp.setMargins(0, dp(7), 0, dp(5));
                        card.addView(pb, pbLp);

                        TextView tvStatus = new TextView(this);
                        tvStatus.setTextSize(11.5f);
                        card.addView(tvStatus);

                        LinearLayout ctrlRow = new LinearLayout(this);
                        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
                        ctrlRow.setGravity(Gravity.END);
                        ctrlRow.setPadding(0, dp(7), 0, 0);

                        if (task.status == DownloadService.TurboDownloadTask.STATUS_DOWNLOADING) {
                            Button btnPause = createGlassButton("Pause", IC_PAUSE, "#33FFD000", "#1AFFD000", "#FFE566");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnPause.setOnClickListener(v -> task.status = DownloadService.TurboDownloadTask.STATUS_PAUSED);
                            ctrlRow.addView(btnPause, lp);
                        } else if (task.status == DownloadService.TurboDownloadTask.STATUS_PAUSED || task.status == DownloadService.TurboDownloadTask.STATUS_FAILED) {
                            Button btnResume = createGlassButton("Resume", IC_PLAY, "#0072FF", "#00C6FF", "#FFFFFF");
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
                            lp.setMargins(0, 0, dp(8), 0);
                            btnResume.setOnClickListener(v -> DownloadService.resumeDownload(this, task));
                            ctrlRow.addView(btnResume, lp);
                        }

                        if (task.status != DownloadService.TurboDownloadTask.STATUS_COMPLETED && task.status != DownloadService.TurboDownloadTask.STATUS_CANCELLED) {
                            Button btnCancel = createGlassButton("Cancel", IC_CLOSE, "#33EF4444", "#1AEF4444", "#FCA5A5");
                            btnCancel.setOnClickListener(v -> {
                                task.status = DownloadService.TurboDownloadTask.STATUS_CANCELLED;
                                if (task.stagingFile != null && task.stagingFile.exists()) task.stagingFile.delete();
                            });
                            ctrlRow.addView(btnCancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
                        }

                        if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) {
                            Button btnOpen = createGlassButton("Open Folder", IC_FOLDER, "#00B09B", "#00F5A0", "#042016");
                            btnOpen.setOnClickListener(v -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                            ctrlRow.addView(btnOpen, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
                        }

                        card.addView(ctrlRow);
                        queueListLayout.addView(card);

                        QueueCardRefs ref = new QueueCardRefs();
                        ref.tvName = tvName;
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
                ref.tvName.setText(task.fileName + (task.isMultiSegment ? " [4x]" : ""));

                long dl = task.downloadedBytes.get();
                int pct = (task.totalBytes > 0) ? (int) ((dl * 100L) / task.totalBytes) : 0;
                if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) pct = 100;
                ref.pb.setProgress(Math.min(100, Math.max(0, pct)));

                String sizeRatio = DownloadService.formatBytes(dl) + " / " + DownloadService.formatBytes(task.totalBytes);
                if (task.status == DownloadService.TurboDownloadTask.STATUS_DOWNLOADING) {
                    String speedStr = DownloadService.formatBytes(task.speedBytesPerSec) + "/s";
                    String etaStr = (task.speedBytesPerSec > 0 && task.totalBytes > dl)
                            ? "  •  ETA " + ((task.totalBytes - dl) / task.speedBytesPerSec) + "s" : "";
                    ref.tvStatus.setText(pct + "%  •  " + sizeRatio + "  •  " + speedStr + etaStr);
                    ref.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_PAUSED) {
                    ref.tvStatus.setText("PAUSED at " + pct + "%  •  " + sizeRatio);
                    ref.tvStatus.setTextColor(Color.parseColor("#FFD000"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED) {
                    ref.tvStatus.setText("SAVED  •  " + DownloadService.formatBytes(dl) + "  •  " + task.savedDisplayPath);
                    ref.tvStatus.setTextColor(Color.parseColor("#00F5A0"));
                } else if (task.status == DownloadService.TurboDownloadTask.STATUS_CANCELLED) {
                    ref.tvStatus.setText("CANCELLED");
                    ref.tvStatus.setTextColor(Color.parseColor("#FB7185"));
                } else {
                    ref.tvStatus.setText("FAILED: " + task.errorMsg);
                    ref.tvStatus.setTextColor(Color.parseColor("#FB7185"));
                }
            }
        };

        btnDirectGo.setOnClickListener(v -> {
            String rawUrl = etDirectUrl.getText().toString().trim();
            if (rawUrl.isEmpty()) {
                Toast.makeText(this, "Paste a valid file URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (rawUrl.startsWith("data:")) {
                DownloadService.saveDataUrl(this, rawUrl, "nova_image_" + System.currentTimeMillis());
                etDirectUrl.setText("");
                return;
            }
            if (rawUrl.startsWith("blob:")) {
                fetchBlobFromWebView(getCurrentWebView(), rawUrl, "nova_blob_" + System.currentTimeMillis() + ".png", "image/png");
                etDirectUrl.setText("");
                return;
            }
            if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) rawUrl = "https://" + rawUrl;
            final String finalUrl = rawUrl;
            if (isStreamingWebpageUrl(finalUrl)) {
                new AlertDialog.Builder(this)
                    .setTitle("Streaming Webpage Detected")
                    .setMessage("YouTube and Spotify links point to protected streaming webpages rather than direct file links. Open ad-free in a browser tab instead?")
                    .setPositiveButton("Open in Tab", (d, w) -> {
                        dialog.dismiss();
                        WebView wv = getCurrentWebView();
                        if (wv != null) wv.loadUrl(finalUrl);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return;
            }
            String guessed = URLUtil.guessFileName(finalUrl, null, null);
            String cookie = CookieManager.getInstance().getCookie(finalUrl);
            DownloadService.startNewDownload(this, finalUrl, guessed, defaultUserAgent, cookie, null);
            etDirectUrl.setText("");
            syncQueueUI.run();
        });

        btnClearDone.setOnClickListener(v -> {
            DownloadService.TASKS.removeIf(t -> t.status == DownloadService.TurboDownloadTask.STATUS_COMPLETED
                    || t.status == DownloadService.TurboDownloadTask.STATUS_CANCELLED
                    || t.status == DownloadService.TurboDownloadTask.STATUS_FAILED);
            syncQueueUI.run();
        });

        Runnable liveUpdater = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                syncQueueUI.run();
                uiHandler.postDelayed(this, 500);
            }
        };
        dialog.setOnShowListener(d -> uiHandler.post(liveUpdater));
        showGlassModal(dialog, outerGlass, true);
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
                    .setMessage("Saved in Phone Storage -> Downloads/NOVA Browser")
                    .setPositiveButton("Open Downloads Folder", (d2, w2) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)))
                    .setNeutralButton("Delete File", (d2, w2) -> {
                        try {
                            getContentResolver().delete(fileUri, null, null);
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
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
    // FILTER ENGINE (ULTRA-LOW RAM 64-BIT SORTED HASH ARRAYS)
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
            if (arr[i] != arr[unique - 1]) arr[unique++] = arr[i];
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
                            "NOVA Shield Ready: " + String.format(Locale.US, "%,d", total) + " rules",
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
                if (!line.isEmpty()) buf[count++] = fnv1a64(line);
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

    private void injectNovaShieldHook(WebView view) {
        if (!isAdBlockEnabled || isNoJsBypassActive) return;
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

    // =========================================================================
    // TAB ENGINE, WEBVIEW SETUP & LIFECYCLE
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
        btnTabs.setText(String.valueOf(tabs.size()));

        String url = active.getUrl();
        if (url == null || url.startsWith("https://nova.browser")) {
            etUrl.setText("");
            etUrl.setHint("Search " + searchEngineName + " or enter URL...");
            updateShieldToolbarIcon(false);
            animateBarsToDp(54);
        } else {
            etUrl.setText(url);
            updateShieldToolbarIcon(!url.startsWith("https://"));
            animateBarsToDp(46);
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

        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult res = webView.getHitTestResult();
            if (res != null && (res.getType() == WebView.HitTestResult.IMAGE_TYPE
                    || res.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                String imgUrl = res.getExtra();
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    showImageLongPressDialog(webView, imgUrl);
                    return true;
                }
            }
            return false;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                String currentUrl = webView.getUrl();
                if (currentUrl != null && currentUrl.startsWith("https://nova.browser")) {
                    animateBarsToDp(54);
                    return;
                }
                int dy = scrollY - oldScrollY;
                if (scrollY <= dp(10)) animateBarsToDp(46);
                else if (dy > dp(18)) animateBarsToDp(0);
                else if (dy < -dp(14)) animateBarsToDp(46);
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
                    sessionBlockedCount++;
                    pageBlockedCount++;
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
                if (view == getCurrentWebView()) {
                    pageBlockedCount = 0;
                    if (url.startsWith("https://nova.browser")) {
                        etUrl.setText("");
                        updateShieldToolbarIcon(false);
                        animateBarsToDp(54);
                    } else {
                        etUrl.setText(url);
                        updateShieldToolbarIcon(!url.startsWith("https://"));
                        animateBarsToDp(46);
                    }
                    progressBar.setVisibility(View.VISIBLE);
                }
                injectNovaShieldHook(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectNovaShieldHook(view);
                if (view == getCurrentWebView()) {
                    progressBar.setVisibility(View.GONE);
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
                    sessionBlockedCount++;
                    pageBlockedCount++;
                    return createBlockedResponse();
                } else if (kind == 2) {
                    trackersBlockedCount++;
                    sessionBlockedCount++;
                    pageBlockedCount++;
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
            if (url != null && url.startsWith("blob:")) {
                fetchBlobFromWebView(webView, url, guessedName, mimeType);
                return;
            }
            if (url != null && url.startsWith("data:")) {
                DownloadService.saveDataUrl(this, url, guessedName);
                return;
            }
            String cookie = CookieManager.getInstance().getCookie(url);
            promptNewTurboDownload(url, guessedName, contentLength, cookie, webView.getUrl());
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
    // DEVELOPER PAGE, TAB SWITCHER, BOOKMARKS, HISTORY & UTILITIES
    // =========================================================================

    private void showAboutDeveloperDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("About & Developer", IC_USER, dialog::dismiss));

        LinearLayout card = createGlassCard();
        TextView tvInfo = new TextView(this);
        tvInfo.setText("NOVA Browser v" + APP_VERSION + " — Liquid Glass Edition\n\n"
                + "Developer: " + DEV_NAME + "\n"
                + "Email: " + DEV_EMAIL + "\n"
                + "GitHub: @shahalamlaskarofficial\n\n"
                + "Regular Updates:\n"
                + "NOVA Browser is actively maintained with regular updates bringing stronger filter lists, faster download stability, UI improvements, and RAM optimization. Check Releases for seamless in-place APK updates.");
        tvInfo.setTextColor(Color.parseColor("#E2E8F0"));
        tvInfo.setTextSize(12.5f);
        tvInfo.setLineSpacing(dp(2), 1f);
        card.addView(tvInfo);
        outer.addView(card);

        Button btnEmail = createGlassButton("Email Developer", IC_MAIL, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        bLp.setMargins(0, 0, 0, dp(8));
        btnEmail.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:" + DEV_EMAIL + "?subject=" + Uri.encode("NOVA Browser v" + APP_VERSION + " Feedback")));
            startActivity(Intent.createChooser(email, "Email " + DEV_NAME));
        });
        outer.addView(btnEmail, bLp);

        Button btnGit = createGlassButton("Check GitHub Releases", IC_RELOAD, "#2B00F5A0", "#1400B09B", "#00F5A0");
        btnGit.setOnClickListener(v -> {
            WebView wv = getCurrentWebView();
            if (wv != null) wv.loadUrl(DEV_GITHUB + "/Nova-Browser--advanced-/releases");
            dialog.dismiss();
        });
        outer.addView(btnGit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(39)));

        showGlassModal(dialog, outer, false);
    }

    private void showTabSwitcherDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("Open Tabs (" + tabs.size() + ")", IC_LINK, dialog::dismiss));

        ScrollView sc = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sc.addView(list);

        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            WebView wv = tabs.get(i);
            String title = wv.getTitle();
            if (title == null || title.isEmpty() || (wv.getUrl() != null && wv.getUrl().startsWith("https://nova.browser"))) {
                title = "NOVA Home";
            }
            boolean isActive = (i == currentTabIndex);
            Button tb = createGlassButton(
                    (i + 1) + ". " + title + (isActive ? "  [Active]" : "  [Suspended]"),
                    isActive ? IC_CHECK : IC_GLOBE,
                    isActive ? "#3300D2FF" : "#22FFFFFF",
                    isActive ? "#186366F1" : "#10FFFFFF",
                    isActive ? "#FFFFFF" : "#CBD5E1");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
            lp.setMargins(0, 0, 0, dp(6));
            tb.setOnClickListener(v -> { dialog.dismiss(); switchToTab(idx); });
            list.addView(tb, lp);
        }
        outer.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.setMargins(0, dp(8), 0, 0);

        Button bNew = createGlassButton("New Tab", IC_PLUS, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, dp(38), 1f);
        l1.setMargins(0, 0, dp(6), 0);
        bNew.setOnClickListener(v -> { dialog.dismiss(); createNewTab("nova://home"); });
        actions.addView(bNew, l1);

        if (!closedTabs.isEmpty()) {
            Button bRe = createGlassButton("Reopen", IC_RELOAD, "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
            LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, dp(38), 1f);
            l2.setMargins(0, 0, dp(6), 0);
            bRe.setOnClickListener(v -> { dialog.dismiss(); createNewTab(closedTabs.remove(0)); });
            actions.addView(bRe, l2);
        }

        if (tabs.size() > 1) {
            Button bCls = createGlassButton("Close", IC_TRASH, "#33EF4444", "#1AEF4444", "#FCA5A5");
            bCls.setOnClickListener(v -> { dialog.dismiss(); closeTab(currentTabIndex); });
            actions.addView(bCls, new LinearLayout.LayoutParams(0, dp(38), 1f));
        }

        outer.addView(actions, aLp);
        showGlassModal(dialog, outer, false);
    }

    private void showPerformanceOptimizerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = createModalOuterContainer();
        outer.addView(createModalHeader("RAM & Battery Optimizer", IC_BOLT, dialog::dismiss));

        LinearLayout card = createGlassCard();
        TextView tv = new TextView(this);
        tv.setText("JVM Heap Used: " + getRamUsageMb() + "\n"
                + "Blocklist Index: ~1.4 MB (64-bit Primitive Array)\n"
                + "Active Tabs: 1  •  Suspended Tabs: " + Math.max(0, tabs.size() - 1) + "\n"
                + "Battery Saver (Pause Timers): " + (isBatterySaver ? "Enabled" : "Disabled"));
        tv.setTextColor(Color.parseColor("#CBD5E1"));
        tv.setTextSize(12.5f);
        tv.setLineSpacing(dp(3), 1f);
        card.addView(tv);
        outer.addView(card);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button btnBoost = createGlassButton("Boost RAM Now", IC_BOLT, "#0072FF", "#00C6FF", "#FFFFFF");
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, dp(39), 1f);
        l1.setMargins(0, 0, dp(8), 0);
        btnBoost.setOnClickListener(v -> {
            for (int i = 0; i < tabs.size(); i++) {
                tabs.get(i).clearCache(false);
                if (i != currentTabIndex) tabs.get(i).onPause();
            }
            System.gc();
            Toast.makeText(this, "RAM trimmed (" + getRamUsageMb() + ")", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        row.addView(btnBoost, l1);

        Button btnBat = createGlassButton(isBatterySaver ? "Battery: ON" : "Battery: OFF", IC_CHECK, "#2BFFFFFF", "#12FFFFFF", "#FFFFFF");
        btnBat.setOnClickListener(v -> {
            isBatterySaver = !isBatterySaver;
            prefs.edit().putBoolean("battery_saver", isBatterySaver).apply();
            Toast.makeText(this, "Battery Saver " + (isBatterySaver ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        row.addView(btnBat, new LinearLayout.LayoutParams(0, dp(39), 1f));
        outer.addView(row);

        showGlassModal(dialog, outer, false);
    }

    private void loadUserInput(String input) {
        if (input.isEmpty()) return;
        WebView wv = getCurrentWebView();
        if (wv == null) return;
        resetTabNoJsMode(wv);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);

        if ("nova://home".equalsIgnoreCase(input)) loadNovaHome(wv);
        else if (input.startsWith("http://") || input.startsWith("https://")) wv.loadUrl(input);
        else if (input.contains(".") && !input.contains(" ")) wv.loadUrl((isHttpsFirst ? "https://" : "http://") + input);
        else wv.loadUrl(searchEngineUrl + Uri.encode(input));
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
            animateBarsToDp(46);
        }
    }

    private void showBookmarksDialog() {
        Set<String> saved = new HashSet<>(prefs.getStringSet("bookmarks", new HashSet<>()));
        List<String> list = new ArrayList<>(saved);
        list.add(0, "+ Bookmark Current Page");

        new AlertDialog.Builder(this)
            .setTitle("NOVA Bookmarks (" + saved.size() + ")")
            .setItems(list.toArray(new String[0]), (d, which) -> {
                WebView wv = getCurrentWebView();
                if (which == 0) {
                    if (wv != null && wv.getUrl() != null && !wv.getUrl().startsWith("https://nova.browser")) {
                        String entry = (wv.getTitle() != null ? wv.getTitle() : "Site") + " | " + wv.getUrl();
                        saved.add(entry);
                        prefs.edit().putStringSet("bookmarks", saved).apply();
                        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String item = list.get(which);
                    int sep = item.lastIndexOf(" | ");
                    if (wv != null) wv.loadUrl(sep != -1 ? item.substring(sep + 3) : item);
                }
            })
            .setNeutralButton("Clear All", (d, w) -> {
                prefs.edit().remove("bookmarks").apply();
                Toast.makeText(this, "Bookmarks cleared", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void shareOrCopyUrl(WebView wv) {
        if (wv == null || wv.getUrl() == null) return;
        String url = wv.getUrl();
        String[] actions = {"Copy Page Link", "Share Website Link", "Generate QR Code for URL"};
        new AlertDialog.Builder(this)
            .setTitle("Share & Link Tools")
            .setItems(actions, (d, which) -> {
                if (which == 0) {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("NOVA URL", url));
                        Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Cache, cookies, and history cleared", Toast.LENGTH_SHORT).show();
    }
}
