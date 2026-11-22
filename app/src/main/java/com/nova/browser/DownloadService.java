package com.nova.browser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadService extends Service {

    private static final String CHANNEL_ID = "nova_turbo_downloads";
    private static final int NOTIF_ID = 4041;
    private static final int STREAM_BUFFER = 131072; // 128 KB high-speed buffer
    private static final int PARALLEL_CHUNKS = 4;

    public static final List<TurboDownloadTask> TASKS = new CopyOnWriteArrayList<>();
    private static final ExecutorService TASK_POOL = Executors.newFixedThreadPool(3);
    private static final ExecutorService CHUNK_POOL = Executors.newFixedThreadPool(12);

    private NotificationManager notifManager;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean notifLoopRunning = false;

    public static class TurboDownloadTask {
        public static final int STATUS_DOWNLOADING = 0;
        public static final int STATUS_PAUSED = 1;
        public static final int STATUS_COMPLETED = 2;
        public static final int STATUS_FAILED = 3;
        public static final int STATUS_CANCELLED = 4;

        public final String id;
        public final String url;
        public final String fileName;
        public final String userAgent;
        public final File stagingFile;
        public final boolean isSecureHttps;

        public final AtomicLong downloadedBytes = new AtomicLong(0);
        public volatile long totalBytes = -1;
        public volatile long speedBytesPerSec = 0;
        public volatile int status = STATUS_DOWNLOADING;
        public volatile boolean isMultiSegment = false;
        public volatile String savedDisplayPath = "Downloads/NOVA Browser/";
        public volatile String errorMsg = "";

        // Exact chunk progress tracking for bug-free multi-segment Pause/Resume
        public long[] chunkStarts;
        public long[] chunkEnds;
        public long[] chunkWritten;

        public TurboDownloadTask(String url, String fileName, String userAgent, File stagingFile) {
            this.id = String.valueOf(System.nanoTime());
            this.url = url;
            this.fileName = fileName;
            this.userAgent = userAgent != null ? userAgent : "Mozilla/5.0";
            this.stagingFile = stagingFile;
            this.isSecureHttps = url != null && url.toLowerCase(Locale.US).startsWith("https://");
            if (stagingFile.exists()) {
                this.downloadedBytes.set(stagingFile.length());
            }
        }
    }

    public static boolean hasActiveDownloads() {
        for (TurboDownloadTask t : TASKS) {
            if (t.status == TurboDownloadTask.STATUS_DOWNLOADING) return true;
        }
        return false;
    }

    public static int getActiveCount() {
        int c = 0;
        for (TurboDownloadTask t : TASKS) {
            if (t.status == TurboDownloadTask.STATUS_DOWNLOADING) c++;
        }
        return c;
    }

    public static File getStagingDir(Context ctx) {
        File dir = ctx.getExternalFilesDir("nova_staging");
        if (dir == null) dir = new File(ctx.getFilesDir(), "nova_staging");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void startNewDownload(Context ctx, String url, String fileName, String userAgent) {
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        File staging = new File(getStagingDir(ctx), System.currentTimeMillis() + "_" + safeName + ".part");
        TurboDownloadTask task = new TurboDownloadTask(url, safeName, userAgent, staging);
        TASKS.add(0, task);
        launchServiceForTask(ctx, task.id);
    }

    public static void resumeDownload(Context ctx, TurboDownloadTask task) {
        task.status = TurboDownloadTask.STATUS_DOWNLOADING;
        task.errorMsg = "";
        launchServiceForTask(ctx, task.id);
    }

    private static void launchServiceForTask(Context ctx, String taskId) {
        Intent intent = new Intent(ctx, DownloadService.class);
        intent.putExtra("task_id", taskId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notifManager != null) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "NOVA Turbo Downloads",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps NOVA downloads running at full speed in the background");
            notifManager.createNotificationChannel(ch);
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NovaBrowser::TurboDownloadLock");
            wakeLock.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notif = buildNotification("NOVA Turbo Downloader", "Preparing high-speed download...", 0, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notif);
        }

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(3600000L); // up to 1 hour max safety cap
        }

        if (intent != null) {
            String taskId = intent.getStringExtra("task_id");
            if (taskId != null) {
                for (TurboDownloadTask t : TASKS) {
                    if (t.id.equals(taskId)) {
                        TASK_POOL.submit(() -> runTask(t));
                        break;
                    }
                }
            }
        }

        if (!notifLoopRunning) {
            notifLoopRunning = true;
            handler.post(notifUpdater);
        }
        return START_STICKY;
    }

    private final Runnable notifUpdater = new Runnable() {
        @Override
        public void run() {
            TurboDownloadTask active = null;
            int activeCount = 0;
            for (TurboDownloadTask t : TASKS) {
                if (t.status == TurboDownloadTask.STATUS_DOWNLOADING) {
                    if (active == null) active = t;
                    activeCount++;
                }
            }

            if (activeCount == 0) {
                notifLoopRunning = false;
                if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
                stopForeground(true);
                stopSelf();
                return;
            }

            int pct = active.totalBytes > 0
                    ? (int) Math.min(100, (active.downloadedBytes.get() * 100L) / active.totalBytes)
                    : 0;
            String speed = formatBytes(active.speedBytesPerSec) + "/s";
            String title = activeCount > 1
                    ? "Downloading " + activeCount + " files (" + speed + ")"
                    : active.fileName;
            String text = pct + "% • " + formatBytes(active.downloadedBytes.get())
                    + " / " + formatBytes(active.totalBytes) + " • " + speed;

            if (notifManager != null) {
                notifManager.notify(NOTIF_ID, buildNotification(title, text, pct, active.totalBytes <= 0));
            }
            handler.postDelayed(this, 800);
        }
    };

    private Notification buildNotification(String title, String content, int progress, boolean indeterminate) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, openApp, piFlags);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return b.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, indeterminate)
                .build();
    }

    private void runTask(TurboDownloadTask task) {
        task.status = TurboDownloadTask.STATUS_DOWNLOADING;
        try {
            // 1. If already initialized as multi-segment, resume directly
            if (task.isMultiSegment && task.chunkStarts != null) {
                runMultiSegment(task);
                return;
            }

            // 2. Probe server via HEAD to check file size & Accept-Ranges for 4x parallel turbo mode
            long probedLen = -1;
            boolean supportsRanges = false;
            HttpURLConnection probe = null;
            try {
                probe = (HttpURLConnection) new URL(task.url).openConnection();
                probe.setRequestMethod("HEAD");
                probe.setConnectTimeout(10000);
                probe.setReadTimeout(10000);
                probe.setInstanceFollowRedirects(true);
                probe.setRequestProperty("User-Agent", task.userAgent);
                int code = probe.getResponseCode();
                if (code >= 200 && code < 300) {
                    probedLen = probe.getContentLengthLong();
                    String ar = probe.getHeaderField("Accept-Ranges");
                    supportsRanges = ar != null && ar.toLowerCase(Locale.US).contains("bytes");
                }
            } catch (Exception ignored) {
            } finally {
                if (probe != null) probe.disconnect();
            }

            if (supportsRanges && probedLen >= 2L * 1024L * 1024L && task.downloadedBytes.get() == 0) {
                task.totalBytes = probedLen;
                task.isMultiSegment = true;
                task.chunkStarts = new long[PARALLEL_CHUNKS];
                task.chunkEnds = new long[PARALLEL_CHUNKS];
                task.chunkWritten = new long[PARALLEL_CHUNKS];

                long partSize = probedLen / PARALLEL_CHUNKS;
                for (int i = 0; i < PARALLEL_CHUNKS; i++) {
                    task.chunkStarts[i] = i * partSize;
                    task.chunkEnds[i] = (i == PARALLEL_CHUNKS - 1) ? (probedLen - 1) : ((i + 1) * partSize - 1);
                    task.chunkWritten[i] = 0;
                }
                try (RandomAccessFile initRaf = new RandomAccessFile(task.stagingFile, "rw")) {
                    initRaf.setLength(probedLen);
                }
                runMultiSegment(task);
            } else {
                runSingleStream(task);
            }
        } catch (Exception e) {
            task.speedBytesPerSec = 0;
            if (task.status != TurboDownloadTask.STATUS_PAUSED && task.status != TurboDownloadTask.STATUS_CANCELLED) {
                task.status = TurboDownloadTask.STATUS_FAILED;
                task.errorMsg = e.getMessage() != null ? e.getMessage() : "Connection error";
            }
        }
    }

    private void runMultiSegment(TurboDownloadTask task) {
        CountDownLatch latch = new CountDownLatch(PARALLEL_CHUNKS);
        AtomicBoolean anyFailed = new AtomicBoolean(false);
        AtomicLong speedWindowBytes = new AtomicLong(0);
        AtomicLong lastSpeedTime = new AtomicLong(System.currentTimeMillis());

        for (int i = 0; i < PARALLEL_CHUNKS; i++) {
            final int idx = i;
            CHUNK_POOL.submit(() -> {
                HttpURLConnection conn = null;
                RandomAccessFile raf = null;
                BufferedInputStream in = null;
                try {
                    long start = task.chunkStarts[idx] + task.chunkWritten[idx];
                    long end = task.chunkEnds[idx];
                    if (start > end) return; // Chunk already complete

                    conn = (HttpURLConnection) new URL(task.url).openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(20000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", task.userAgent);
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    conn.setRequestProperty("Connection", "keep-alive");
                    conn.setRequestProperty("Range", "bytes=" + start + "-" + end);

                    int code = conn.getResponseCode();
                    if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                        anyFailed.set(true);
                        task.errorMsg = "HTTP " + code;
                        return;
                    }

                    raf = new RandomAccessFile(task.stagingFile, "rw");
                    raf.seek(start);
                    in = new BufferedInputStream(conn.getInputStream(), STREAM_BUFFER);
                    byte[] buf = new byte[STREAM_BUFFER];
                    int read;

                    while ((read = in.read(buf)) != -1) {
                        if (task.status != TurboDownloadTask.STATUS_DOWNLOADING) return;
                        raf.write(buf, 0, read);
                        task.chunkWritten[idx] += read;
                        task.downloadedBytes.addAndGet(read);
                        long wBytes = speedWindowBytes.addAndGet(read);
                        long now = System.currentTimeMillis();
                        long prev = lastSpeedTime.get();
                        if (now - prev >= 450 && lastSpeedTime.compareAndSet(prev, now)) {
                            task.speedBytesPerSec = (wBytes * 1000L) / Math.max(1, now - prev);
                            speedWindowBytes.set(0);
                        }
                    }
                } catch (Exception e) {
                    if (task.status == TurboDownloadTask.STATUS_DOWNLOADING) {
                        anyFailed.set(true);
                        task.errorMsg = e.getMessage() != null ? e.getMessage() : "Chunk timeout";
                    }
                } finally {
                    try { if (in != null) in.close(); } catch (Exception ignored) {}
                    try { if (raf != null) raf.close(); } catch (Exception ignored) {}
                    if (conn != null) conn.disconnect();
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        task.speedBytesPerSec = 0;
        if (task.status == TurboDownloadTask.STATUS_CANCELLED) {
            task.stagingFile.delete();
            return;
        }
        if (task.status == TurboDownloadTask.STATUS_PAUSED) {
            return;
        }
        if (anyFailed.get() || task.downloadedBytes.get() < task.totalBytes) {
            task.status = TurboDownloadTask.STATUS_FAILED;
            if (task.errorMsg.isEmpty()) task.errorMsg = "Interrupted — Tap Resume";
            return;
        }

        finalizeDownload(task);
    }

    private void runSingleStream(TurboDownloadTask task) {
        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        BufferedInputStream in = null;
        try {
            File target = task.stagingFile;
            long existing = target.exists() ? target.length() : 0;
            task.downloadedBytes.set(existing);

            conn = (HttpURLConnection) new URL(task.url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", task.userAgent);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Connection", "keep-alive");
            if (existing > 0) {
                conn.setRequestProperty("Range", "bytes=" + existing + "-");
            }

            int code = conn.getResponseCode();
            boolean isPartial = (code == HttpURLConnection.HTTP_PARTIAL);
            if (code != HttpURLConnection.HTTP_OK && !isPartial) {
                task.status = TurboDownloadTask.STATUS_FAILED;
                task.errorMsg = "HTTP " + code;
                return;
            }

            long len = conn.getContentLengthLong();
            if (isPartial) {
                task.totalBytes = existing + len;
            } else {
                task.totalBytes = len;
                existing = 0;
                task.downloadedBytes.set(0);
            }

            raf = new RandomAccessFile(target, "rw");
            if (isPartial && existing > 0) raf.seek(existing);
            else raf.setLength(0);

            in = new BufferedInputStream(conn.getInputStream(), STREAM_BUFFER);
            byte[] buf = new byte[STREAM_BUFFER];
            int read;
            long lastTime = System.currentTimeMillis();
            long windowBytes = 0;

            while ((read = in.read(buf)) != -1) {
                if (task.status == TurboDownloadTask.STATUS_PAUSED) {
                    task.speedBytesPerSec = 0;
                    return;
                }
                if (task.status == TurboDownloadTask.STATUS_CANCELLED) {
                    task.speedBytesPerSec = 0;
                    try { raf.close(); } catch (Exception ignored) {}
                    target.delete();
                    return;
                }

                raf.write(buf, 0, read);
                task.downloadedBytes.addAndGet(read);
                windowBytes += read;

                long now = System.currentTimeMillis();
                long elapsed = now - lastTime;
                if (elapsed >= 450) {
                    task.speedBytesPerSec = (windowBytes * 1000L) / elapsed;
                    windowBytes = 0;
                    lastTime = now;
                }
            }

            raf.close();
            raf = null;
            in.close();
            in = null;
            finalizeDownload(task);

        } catch (Exception e) {
            task.speedBytesPerSec = 0;
            if (task.status != TurboDownloadTask.STATUS_PAUSED && task.status != TurboDownloadTask.STATUS_CANCELLED) {
                task.status = TurboDownloadTask.STATUS_FAILED;
                task.errorMsg = e.getMessage() != null ? e.getMessage() : "Network Interrupted";
            }
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (raf != null) raf.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    private void finalizeDownload(TurboDownloadTask task) {
        task.savedDisplayPath = exportToPublicDownloads(task.stagingFile, task.fileName);
        task.speedBytesPerSec = 0;
        task.status = TurboDownloadTask.STATUS_COMPLETED;
        handler.post(() -> Toast.makeText(getApplicationContext(),
                "✅ Saved: " + task.savedDisplayPath, Toast.LENGTH_LONG).show());
    }

    private String exportToPublicDownloads(File sourceFile, String fileName) {
        try {
            String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
            String mime = (ext != null && !ext.isEmpty())
                    ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US))
                    : null;
            if (mime == null) mime = "application/octet-stream";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NOVA Browser");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (InputStream in = new FileInputStream(sourceFile);
                         OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            byte[] buf = new byte[STREAM_BUFFER];
                            int len;
                            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
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
                    byte[] buf = new byte[STREAM_BUFFER];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }
                MediaScannerConnection.scanFile(this, new String[]{dest.getAbsolutePath()}, null, null);
                sourceFile.delete();
                return "Downloads/NOVA Browser/" + fileName;
            }
        } catch (Exception ignored) {}
        return sourceFile.getAbsolutePath();
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
