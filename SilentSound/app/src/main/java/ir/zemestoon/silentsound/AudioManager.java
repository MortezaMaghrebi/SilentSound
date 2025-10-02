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

    private AudioManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(3);
        this.mediaPlayers = new HashMap<>();
        this.downloadStatus = new HashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

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

    // بارگذاری وضعیت دانلود‌ها از SharedPreferences
    private void loadDownloadStatus() {
        Set<String> downloadedSounds = sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>());

        for (String soundName : downloadedSounds) {
            String filePath = getLocalPath(soundName);
            File file = new File(filePath);
            if (file.exists()) {
                downloadStatus.put(soundName, true);
                Log.d(TAG, "Loaded downloaded sound: " + soundName);
            } else {
                // اگر فایل وجود ندارد، از لیست حذف کن
                removeFromDownloadedSounds(soundName);
            }
        }
    }

    // ذخیره وضعیت دانلود در SharedPreferences
    private void saveDownloadStatus(String soundName) {
        Set<String> downloadedSounds = new HashSet<>(
                sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>())
        );
        downloadedSounds.add(soundName);

        sharedPreferences.edit()
                .putStringSet(KEY_DOWNLOADED_SOUNDS, downloadedSounds)
                .apply();

        Log.d(TAG, "Saved download status for: " + soundName);
    }

    // حذف از لیست دانلود‌ها
    private void removeFromDownloadedSounds(String soundName) {
        Set<String> downloadedSounds = new HashSet<>(
                sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>())
        );
        downloadedSounds.remove(soundName);

        sharedPreferences.edit()
                .putStringSet(KEY_DOWNLOADED_SOUNDS, downloadedSounds)
                .apply();

        Log.d(TAG, "Removed from downloaded sounds: " + soundName);
    }

    // دانلود آهنگ
    public void downloadSound(Sound sound, DownloadCallback callback) {
        if (isSoundDownloaded(sound.getName())) {
            runOnUiThread(() -> callback.onDownloadComplete(sound.getName(), getLocalPath(sound.getName())));
            return;
        }

        executorService.execute(() -> {
            try {
                String audioUrl = sound.getAudioUrl();
                if (audioUrl == null || audioUrl.isEmpty()) {
                    runOnUiThread(() -> callback.onDownloadError(sound.getName(), "Audio URL is empty"));
                    return;
                }

                Log.d(TAG, "Starting download: " + sound.getName() + " from: " + audioUrl);

                URL url = new URL(audioUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        try {
                            callback.onDownloadError(sound.getName(), "Server returned HTTP " + connection.getResponseCode());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return;
                }

                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();

                File outputFile = new File(getLocalPath(sound.getName()));
                FileOutputStream output = new FileOutputStream(outputFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        input.close();
                        output.close();
                        outputFile.delete();
                        return;
                    }

                    total += count;
                    if (fileLength > 0) {
                        final int progress = (int) (total * 100 / fileLength);
                        runOnUiThread(() -> callback.onDownloadProgress(sound.getName(), progress));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                // بررسی صحت فایل دانلود شده
                if (outputFile.exists() && outputFile.length() > 0) {
                    sound.setLocalPath(outputFile.getAbsolutePath());
                    downloadStatus.put(sound.getName(), true);

                    // ذخیره وضعیت دانلود
                    saveDownloadStatus(sound.getName());

                    Log.d(TAG, "Download completed successfully: " + sound.getName() +
                            ", size: " + outputFile.length() + " bytes");

                    runOnUiThread(() -> callback.onDownloadComplete(sound.getName(), outputFile.getAbsolutePath()));
                } else {
                    runOnUiThread(() -> callback.onDownloadError(sound.getName(), "Downloaded file is empty or corrupted"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Download error for " + sound.getName() + ": " + e.getMessage(), e);
                runOnUiThread(() -> callback.onDownloadError(sound.getName(), e.getMessage()));
            }
        });
    }

    // پخش آهنگ
    public void playSound(Sound sound, int volume, PlaybackCallback callback) {
        executorService.execute(() -> {
            try {
                // اگر آهنگ دانلود نشده، اول دانلود کن
                if (!isSoundDownloaded(sound.getName())) {
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
            // اگر قبلاً در حال پخش است، متوقفش کن
            if (mediaPlayers.containsKey(sound.getName())) {
                MediaPlayer oldPlayer = mediaPlayers.get(sound.getName());
                if (oldPlayer != null && oldPlayer.isPlaying()) {
                    oldPlayer.stop();
                    oldPlayer.release();
                }
            }

            String filePath = sound.getLocalPath();
            Log.d(TAG, "Playing sound from: " + filePath);

            // بررسی وجود فایل
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                // اگر فایل وجود ندارد، وضعیت دانلود را ریست کن
                downloadStatus.put(sound.getName(), false);
                removeFromDownloadedSounds(sound.getName());

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
                mediaPlayers.remove(sound.getName());
                runOnUiThread(() -> callback.onPlaybackStopped(sound.getName()));
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mediaPlayers.remove(sound.getName());
                String errorMsg = "MediaPlayer error: " + what + ", extra: " + extra;
                Log.e(TAG, errorMsg);
                runOnUiThread(() -> callback.onPlaybackError(sound.getName(), errorMsg));
                return false;
            });

            mediaPlayer.start();
            mediaPlayers.put(sound.getName(), mediaPlayer);

            Log.d(TAG, "Playback started: " + sound.getName());
            runOnUiThread(() -> callback.onPlaybackStarted(sound.getName()));

        } catch (Exception e) {
            Log.e(TAG, "Playback error for " + sound.getName() + ": " + e.getMessage(), e);
            runOnUiThread(() -> callback.onPlaybackError(sound.getName(), "Playback error: " + e.getMessage()));
        }
    }

    // توقف آهنگ
    public void stopSound(String soundName) {
        if (mediaPlayers.containsKey(soundName)) {
            MediaPlayer mediaPlayer = mediaPlayers.get(soundName);
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Log.d(TAG, "Stopped: " + soundName);
            }
            mediaPlayers.remove(soundName);
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
    public boolean isSoundDownloaded(String soundName) {
        if (downloadStatus.containsKey(soundName)) {
            return downloadStatus.get(soundName);
        }

        // بررسی از SharedPreferences و وجود فایل
        Set<String> downloadedSounds = sharedPreferences.getStringSet(KEY_DOWNLOADED_SOUNDS, new HashSet<>());
        if (downloadedSounds.contains(soundName)) {
            File file = new File(getLocalPath(soundName));
            boolean exists = file.exists() && file.length() > 0;
            downloadStatus.put(soundName, exists);

            // اگر فایل وجود ندارد، از لیست حذف کن
            if (!exists) {
                removeFromDownloadedSounds(soundName);
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
    private String getLocalPath(String soundName) {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "sleep_sounds");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = soundName.replace(" ", "_") + ".mp3";
        return new File(directory, fileName).getAbsolutePath();
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

        // پاک کردن SharedPreferences
        sharedPreferences.edit().clear().apply();

        Log.d(TAG, "Cache cleared");
    }

    // حذف یک فایل خاص
    public boolean deleteSound(String soundName) {
        stopSound(soundName);
        File file = new File(getLocalPath(soundName));
        boolean deleted = file.delete();

        if (deleted) {
            downloadStatus.put(soundName, false);
            removeFromDownloadedSounds(soundName);
            Log.d(TAG, "Deleted: " + soundName);
        }

        return deleted;
    }

    // اجرا روی thread اصلی
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}