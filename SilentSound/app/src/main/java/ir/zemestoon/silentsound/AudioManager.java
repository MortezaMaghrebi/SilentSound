package ir.zemestoon.silentsound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AudioManager {
    private static final String TAG = "AudioManager";
    private static final String PREF_NAME = "audio_downloads";
    private static final String KEY_DOWNLOADED_SOUNDS = "downloaded_sounds";

    private static AudioManager instance;
    private Context context;
    private ExecutorService executorService;
    private Map<String, MediaPlayer> mediaPlayers;
    private Map<String, Boolean> downloadStatus;
    private Handler mainHandler;
    private SharedPreferences sharedPreferences;

    // اضافه کردن Map برای نگهداری progressهای دانلود
    private Map<String, Integer> downloadProgressMap;
    private Map<String, DownloadCallback> downloadCallbacks;

    private AudioManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(3);
        this.mediaPlayers = new HashMap<>();
        this.downloadStatus = new HashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.downloadProgressMap = new HashMap<>();
        this.downloadCallbacks = new HashMap<>();

        // بارگذاری وضعیت دانلود‌های قبلی
        loadDownloadStatus();
    }

    public static synchronized AudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioManager(context);
        }
        return instance;
    }

    public interface DownloadCallback {
        void onDownloadProgress(String soundName, int progress);
        void onDownloadComplete(String soundName, String localPath);
        void onDownloadError(String soundName, String error);
    }

    public interface PlaybackCallback {
        void onPlaybackStarted(String soundName);
        void onPlaybackStopped(String soundName);
        void onPlaybackError(String soundName, String error);
    }

    // تولید نام یکتا برای فایل بر اساس URL
    private String generateUniqueFileName(String soundName, String audioUrl) {
        try {
            // استفاده از hash برای ایجاد نام یکتا
            String uniqueString = soundName + "_" + audioUrl;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(uniqueString.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString() + ".mp3";
        } catch (NoSuchAlgorithmException e) {
            // اگر خطا رخ داد، از نام ساده استفاده کن
            return soundName.replace(" ", "_").replace("/", "_") + "_" +
                    Math.abs(audioUrl.hashCode()) + ".mp3";
        }
    }

    // بارگذاری وضعیت دانلود‌ها از SharedPreferences
    private void loadDownloadStatus() {
        Set<String> downloadedSounds = sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>());

        for (String soundKey : downloadedSounds) {
            String filePath = getLocalPath(soundKey);
            File file = new File(filePath);
            if (file.exists()) {
                downloadStatus.put(soundKey, true);
                Log.d(TAG, "Loaded downloaded sound: " + soundKey);
            } else {
                // اگر فایل وجود ندارد، از لیست حذف کن
                removeFromDownloadedSounds(soundKey);
            }
        }
    }

    // ذخیره وضعیت دانلود در SharedPreferences
    private void saveDownloadStatus(String soundKey) {
        Set<String> downloadedSounds = new HashSet<>(
                sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>())
        );
        downloadedSounds.add(soundKey);

        sharedPreferences.edit()
                .putStringSet(KEY_DOWNLOADED_SOUNDS, downloadedSounds)
                .apply();

        Log.d(TAG, "Saved download status for: " + soundKey);
    }

    // حذف از لیست دانلود‌ها
    private void removeFromDownloadedSounds(String soundKey) {
        Set<String> downloadedSounds = new HashSet<>(
                sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>())
        );
        downloadedSounds.remove(soundKey);

        sharedPreferences.edit()
                .putStringSet(KEY_DOWNLOADED_SOUNDS, downloadedSounds)
                .apply();

        Log.d(TAG, "Removed from downloaded sounds: " + soundKey);
    }

    // ایجاد کلید یکتا برای صدا
    private String getSoundKey(Sound sound) {
        return generateUniqueFileName(sound.getName(), sound.getAudioUrl());
    }

    // دانلود آهنگ
    public void downloadSound(Sound sound, DownloadCallback callback) {
        String soundKey = getSoundKey(sound);

        if (isSoundDownloaded(sound)) {
            runOnUiThread(() -> callback.onDownloadComplete(sound.getName(), getLocalPath(soundKey)));
            return;
        }

        // ذخیره callback برای آپدیت progress
        downloadCallbacks.put(soundKey, callback);
        downloadProgressMap.put(soundKey, 0);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;

            try {
                String audioUrl = sound.getAudioUrl();
                if (audioUrl == null || audioUrl.trim().isEmpty()) {
                    runOnUiThread(() -> callback.onDownloadError(sound.getName(), "Audio URL is empty"));
                    downloadCallbacks.remove(soundKey);
                    downloadProgressMap.remove(soundKey);
                    return;
                }

                // Encode برای URL هایی که فاصله یا کاراکتر خاص دارند
                audioUrl = audioUrl.replace(" ", "%20");

                Log.d(TAG, "Starting download: " + sound.getName() + " from: " + audioUrl);

                URL url = new URL(audioUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000); // تایم اوت بیشتر
                connection.setInstanceFollowRedirects(true); // دنبال کردن ریدایرکت‌ها
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() ->
                            callback.onDownloadError(sound.getName(),
                                    "Server returned HTTP " + responseCode));
                    downloadCallbacks.remove(soundKey);
                    downloadProgressMap.remove(soundKey);
                    return;
                }

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();

                File outputFile = new File(getLocalPath(soundKey));
                // مطمئن شو فولدر ساخته بشه
                if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }

                output = new FileOutputStream(outputFile);

                byte[] data = new byte[8192]; // بافر بزرگ‌تر
                long total = 0;
                int count;
                int lastProgress = 0;

                while ((count = input.read(data)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        input.close();
                        output.close();
                        outputFile.delete();
                        downloadCallbacks.remove(soundKey);
                        downloadProgressMap.remove(soundKey);
                        return;
                    }

                    total += count;
                    if (fileLength > 0) {
                        final int progress = (int) (total * 100 / fileLength);
                        if (progress > lastProgress) { // فقط اگر progress تغییر کرد آپدیت کن
                            lastProgress = progress;
                            downloadProgressMap.put(soundKey, progress);
                            runOnUiThread(() -> {
                                DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                                if (currentCallback != null) {
                                    currentCallback.onDownloadProgress(sound.getName(), progress);
                                }
                            });
                        }
                    }
                    output.write(data, 0, count);
                }

                output.flush();

                // بررسی صحت فایل دانلود شده
                if (outputFile.exists()
                        && outputFile.length() > 0
                        && (fileLength <= 0 || outputFile.length() == fileLength)) {

                    sound.setLocalPath(outputFile.getAbsolutePath());
                    downloadStatus.put(soundKey, true);

                    // ذخیره وضعیت دانلود
                    saveDownloadStatus(soundKey);

                    Log.d(TAG, "Download completed successfully: " + sound.getName() +
                            ", size: " + outputFile.length() + " bytes");

                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadComplete(sound.getName(), outputFile.getAbsolutePath());
                        }
                    });
                } else {
                    outputFile.delete();
                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadError(sound.getName(),
                                    "Downloaded file is incomplete or corrupted");
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Download error for " + sound.getName() + ": " + e.getMessage(), e);
                runOnUiThread(() -> {
                    DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                    if (currentCallback != null) {
                        currentCallback.onDownloadError(sound.getName(), e.getMessage());
                    }
                });
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException ignored) {}

                // پاک کردن callback و progress بعد از اتمام
                downloadCallbacks.remove(soundKey);
                downloadProgressMap.remove(soundKey);
            }
        });
    }

    // دریافت progress فعلی دانلود
    public int getDownloadProgress(Sound sound) {
        String soundKey = getSoundKey(sound);
        return downloadProgressMap.getOrDefault(soundKey, 0);
    }

    // پخش آهنگ
    public void playSound(Sound sound, int volume, PlaybackCallback callback) {
        executorService.execute(() -> {
            try {
                // اگر آهنگ دانلود نشده، اول دانلود کن
                if (!isSoundDownloaded(sound)) {
                    downloadSound(sound, new DownloadCallback() {
                        @Override
                        public void onDownloadProgress(String soundName, int progress) {
                            // نمایش progress دانلود
                            Log.d(TAG, "Download progress for " + soundName + ": " + progress + "%");
                        }

                        @Override
                        public void onDownloadComplete(String soundName, String localPath) {
                            Log.d(TAG, "Download complete, now playing: " + soundName);
                            // بعد از دانلود، پخش کن
                            playDownloadedSound(sound, volume, callback);
                        }

                        @Override
                        public void onDownloadError(String soundName, String error) {
                            Log.e(TAG, "Download failed for " + soundName + ": " + error);
                            runOnUiThread(() -> callback.onPlaybackError(soundName, "Download failed: " + error));
                        }
                    });
                } else {
                    playDownloadedSound(sound, volume, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Play sound error: " + e.getMessage(), e);
                runOnUiThread(() -> callback.onPlaybackError(sound.getName(), e.getMessage()));
            }
        });
    }

    private void playDownloadedSound(Sound sound, int volume, PlaybackCallback callback) {
        try {
            String soundKey = getSoundKey(sound);

            // اگر قبلاً در حال پخش است، متوقفش کن
            if (mediaPlayers.containsKey(soundKey)) {
                MediaPlayer oldPlayer = mediaPlayers.get(soundKey);
                if (oldPlayer != null && oldPlayer.isPlaying()) {
                    oldPlayer.stop();
                    oldPlayer.release();
                }
            }

            String filePath = getLocalPath(soundKey);
            Log.d(TAG, "Playing sound from: " + filePath);

            // بررسی وجود فایل
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                // اگر فایل وجود ندارد، وضعیت دانلود را ریست کن
                downloadStatus.put(soundKey, false);
                removeFromDownloadedSounds(soundKey);

                runOnUiThread(() -> callback.onPlaybackError(sound.getName(), "File not found, please download again"));
                return;
            }

            MediaPlayer mediaPlayer = new MediaPlayer();

            // استفاده از setDataSource با FileDescriptor برای اطمینان بیشتر
            FileInputStream fileInputStream = new FileInputStream(audioFile);
            mediaPlayer.setDataSource(fileInputStream.getFD());
            fileInputStream.close();

            mediaPlayer.prepare();

            // تنظیم حجم
            float volumeLevel = volume / 100.0f;
            mediaPlayer.setVolume(volumeLevel, volumeLevel);

            mediaPlayer.setOnCompletionListener(mp -> {
                mediaPlayers.remove(soundKey);
                runOnUiThread(() -> callback.onPlaybackStopped(sound.getName()));
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mediaPlayers.remove(soundKey);
                String errorMsg = "MediaPlayer error: " + what + ", extra: " + extra;
                Log.e(TAG, errorMsg);
                runOnUiThread(() -> callback.onPlaybackError(sound.getName(), errorMsg));
                return false;
            });

            mediaPlayer.start();
            mediaPlayers.put(soundKey, mediaPlayer);

            Log.d(TAG, "Playback started: " + sound.getName());
            runOnUiThread(() -> callback.onPlaybackStarted(sound.getName()));

        } catch (Exception e) {
            Log.e(TAG, "Playback error for " + sound.getName() + ": " + e.getMessage(), e);
            runOnUiThread(() -> callback.onPlaybackError(sound.getName(), "Playback error: " + e.getMessage()));
        }
    }

    // توقف آهنگ
    public void stopSound(Sound sound) {
        String soundKey = getSoundKey(sound);
        if (mediaPlayers.containsKey(soundKey)) {
            MediaPlayer mediaPlayer = mediaPlayers.get(soundKey);
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Log.d(TAG, "Stopped: " + sound.getName());
            }
            mediaPlayers.remove(soundKey);
        }
    }

    // توقف همه آهنگ‌ها
    public void stopAllSounds() {
        for (MediaPlayer mediaPlayer : mediaPlayers.values()) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
        mediaPlayers.clear();
        Log.d(TAG, "All sounds stopped");
    }

    // بررسی آیا آهنگ دانلود شده است
    public boolean isSoundDownloaded(Sound sound) {
        String soundKey = getSoundKey(sound);

        if (downloadStatus.containsKey(soundKey)) {
            return downloadStatus.get(soundKey);
        }

        // بررسی از SharedPreferences و وجود فایل
        Set<String> downloadedSounds = sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>());
        if (downloadedSounds.contains(soundKey)) {
            File file = new File(getLocalPath(soundKey));
            boolean exists = file.exists() && file.length() > 0;
            downloadStatus.put(soundKey, exists);

            // اگر فایل وجود ندارد، از لیست حذف کن
            if (!exists) {
                removeFromDownloadedSounds(soundKey);
            }

            return exists;
        }

        return false;
    }

    // دریافت لیست صداهای دانلود شده
    public Set<String> getDownloadedSounds() {
        return sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>());
    }

    // مسیر local برای ذخیره آهنگ
    private String getLocalPath(String soundKey) {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "sleep_sounds");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return new File(directory, soundKey).getAbsolutePath();
    }

    // حذف فایل‌های دانلود شده
    public void clearCache() {
        stopAllSounds();
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "sleep_sounds");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        downloadStatus.clear();
        downloadProgressMap.clear();
        downloadCallbacks.clear();

        // پاک کردن SharedPreferences
        sharedPreferences.edit().clear().apply();

        Log.d(TAG, "Cache cleared");
    }

    // حذف یک فایل خاص
    public boolean deleteSound(Sound sound) {
        String soundKey = getSoundKey(sound);
        stopSound(sound);
        File file = new File(getLocalPath(soundKey));
        boolean deleted = file.delete();

        if (deleted) {
            downloadStatus.put(soundKey, false);
            removeFromDownloadedSounds(soundKey);
            Log.d(TAG, "Deleted: " + sound.getName());
        }

        return deleted;
    }

    // اجرا روی thread اصلی
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}