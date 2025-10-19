package ir.zemestoon.silentsound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

    // اضافه کردن Map برای نگهداری ارتباط بین soundKey و Sound
    private Map<String, Sound> soundKeyToSoundMap = new HashMap<>();

    // اضافه کردن reference به MainActivity
    private MainActivity mainActivityRef;

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
        void onDownloadProgress(String soundId, int progress);
        void onDownloadComplete(String soundId, String localPath);
        void onDownloadError(String soundId, String error);
    }

    public interface PlaybackCallback {
        void onPlaybackStarted(String soundId);
        void onPlaybackStopped(String soundId);
        void onPlaybackError(String soundId, String error);
    }

    public interface MusicPlaybackCallback {
        void onMusicTrackCompleted(Sound completedSound);
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

    // متد برای ثبت Sound در Map
    private void registerSound(Sound sound) {
        String soundKey = getSoundKey(sound);
        soundKeyToSoundMap.put(soundKey, sound);
        Log.d(TAG, "Registered sound: " + sound.getName() + " with key: " + soundKey);
    }

    // متد برای پیدا کردن Sound بر اساس soundKey
    private Sound findSoundByKey(String soundKey) {
        return soundKeyToSoundMap.get(soundKey);
    }

    // متد برای حذف Sound از Map
    private void unregisterSound(Sound sound) {
        String soundKey = getSoundKey(sound);
        soundKeyToSoundMap.remove(soundKey);
    }

    // دانلود آهنگ
    public void downloadSound(Sound sound, DownloadCallback callback) {
        String soundKey = getSoundKey(sound);

        if (isSoundDownloaded(sound)) {
            runOnUiThread(() -> callback.onDownloadComplete(sound.getId(), getLocalPath(soundKey)));
            return;
        }

        downloadCallbacks.put(soundKey, callback);
        downloadProgressMap.put(soundKey, 0);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;

            try {
                String audioUrl = sound.getAudioUrl();
                if (audioUrl == null || audioUrl.trim().isEmpty()) {
                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadError(sound.getId(), "Audio URL is empty");
                        }
                    });
                    return;
                }

                audioUrl = audioUrl.replace(" ", "%20");
                Log.d(TAG, "Starting download: " + sound.getId() + " from: " + audioUrl);

                URL url = new URL(audioUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadError(sound.getId(), "Server returned HTTP " + responseCode);
                        }
                    });
                    return;
                }

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();

                File encryptedFile = new File(getLocalPath(soundKey) + "_enc");
                if (encryptedFile.getParentFile() != null && !encryptedFile.getParentFile().exists()) {
                    encryptedFile.getParentFile().mkdirs();
                }

                output = new FileOutputStream(encryptedFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                int lastProgress = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        final int progress = (int) (total * 100 / fileLength);
                        if (progress > lastProgress) {
                            lastProgress = progress;
                            downloadProgressMap.put(soundKey, progress);

                            runOnUiThread(() -> {
                                DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                                if (currentCallback != null) {
                                    currentCallback.onDownloadProgress(sound.getId(), progress);
                                }
                            });
                        }
                    }
                    output.write(data, 0, count);
                }
                output.flush();

                if (encryptedFile.exists() && encryptedFile.length() > 0) {
                    Log.d(TAG, "Decrypting file for: " + sound.getId());
                    File decryptedFile = decryptFile(encryptedFile, soundKey);

                    // پاک کردن فایل رمز شده برای صرفه‌جویی
                    encryptedFile.delete();

                    sound.setLocalPath(decryptedFile.getAbsolutePath());
                    downloadStatus.put(soundKey, true);
                    saveDownloadStatus(soundKey);

                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadComplete(sound.getId(), decryptedFile.getAbsolutePath());
                        }
                        downloadCallbacks.remove(soundKey);
                    });
                } else {
                    runOnUiThread(() -> {
                        DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                        if (currentCallback != null) {
                            currentCallback.onDownloadError(sound.getId(), "Downloaded file is empty or corrupted");
                        }
                        downloadCallbacks.remove(soundKey);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Download/Decrypt error for " + sound.getId() + ": " + e.getMessage(), e);
                runOnUiThread(() -> {
                    DownloadCallback currentCallback = downloadCallbacks.get(soundKey);
                    if (currentCallback != null) {
                        currentCallback.onDownloadError(sound.getId(), e.getMessage());
                    }
                    downloadCallbacks.remove(soundKey);
                });
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException ignored) {}

                downloadProgressMap.remove(soundKey);
            }
        });
    }

    private File decryptFile(File encryptedFile, String soundKey) throws Exception {
        // کلید AES باید همان باشد که در رمزگذاری استفاده کردی
        String password = "sound113355";
        File decryptedFile = FileDecryptor.decryptFile(encryptedFile, password);
        if(decryptedFile==null) decryptedFile = FileDecryptor.decryptFileAlternative(encryptedFile, password);


        return decryptedFile;
    }

    // تابع کمکی برای تبدیل hex string به byte array
    private byte[] hexStringToByteArray(String hex) {
        if (hex == null || hex.trim().isEmpty()) {
            return null;
        }

        String cleanHex = hex.trim();
        if (cleanHex.startsWith("0x") || cleanHex.startsWith("0X")) {
            cleanHex = cleanHex.substring(2);
        }

        if (cleanHex.length() % 2 != 0) {
            return null;
        }

        byte[] data = new byte[cleanHex.length() / 2];
        for (int i = 0; i < cleanHex.length(); i += 2) {
            String byteStr = cleanHex.substring(i, i + 2);
            try {
                data[i / 2] = (byte) Integer.parseInt(byteStr, 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return data;
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
                        public void onDownloadProgress(String soundId, int progress) {
                            // نمایش progress دانلود
                            Log.d(TAG, "Download progress for " + soundId + ": " + progress + "%");
                        }

                        @Override
                        public void onDownloadComplete(String soundId, String localPath) {
                            Log.d(TAG, "Download complete, now playing: " + soundId);
                            // بعد از دانلود، پخش کن
                            playDownloadedSound(sound, volume, callback);
                        }

                        @Override
                        public void onDownloadError(String soundId, String error) {
                            Log.e(TAG, "Download failed for " + soundId + ": " + error);
                            runOnUiThread(() -> callback.onPlaybackError(soundId, "Download failed: " + error));
                        }
                    });
                } else {
                    playDownloadedSound(sound, volume, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "Play sound error: " + e.getMessage(), e);
                runOnUiThread(() -> callback.onPlaybackError(sound.getId(), e.getMessage()));
            }
        });
    }

    private void playDownloadedSound(Sound sound, int volume, PlaybackCallback callback) {
        try {
            String soundKey = getSoundKey(sound);

            // ثبت Sound در Map
            registerSound(sound);

            // اگر از گروه music است و قبلاً در حال پخش است، متوقفش کن
            if (sound.isMusicGroup()) {
                stopAllMusicSounds();
            }

            // اگر قبلاً در حال پخش است، متوقفش کن
            if (mediaPlayers.containsKey(soundKey)) {
                MediaPlayer oldPlayer = mediaPlayers.get(soundKey);
                if (oldPlayer != null && oldPlayer.isPlaying()) {
                    oldPlayer.stop();
                    oldPlayer.release();
                }
            }

            String filePath = getLocalPath(soundKey);
            Log.d(TAG, "Playing sound from: " + filePath + ", Group: " + sound.getGroup() + ", Looping: " + sound.isLoopingGroup());

            // بررسی وجود فایل
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {

                downloadStatus.put(soundKey, false);
                removeFromDownloadedSounds(soundKey);

                runOnUiThread(() -> callback.onPlaybackError(sound.getId(), "File not found, please download again"));
                return;
            }

            MediaPlayer mediaPlayer = new MediaPlayer();

            // استفاده از setDataSource با FileDescriptor برای اطمینان بیشتر
            FileInputStream fileInputStream = new FileInputStream(audioFile);
            mediaPlayer.setDataSource(fileInputStream.getFD());
            fileInputStream.close();

            mediaPlayer.prepare();
            if(sound.isMusicGroup()) {
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        // محاسبه 10% آخر آهنگ
                        int duration = mp.getDuration();
                        int seekPosition = (int) (duration * 0); // 90% ابتدا رو رد کن، برو به 10% آخر

                        if (duration > 0 && seekPosition < duration) {
                            mp.seekTo(seekPosition);
                            Log.d(TAG, "Seeking to 10% end: " + seekPosition + "ms of " + duration + "ms");
                        }

                        // شروع پخش
                        mp.start();
                    }
                });
            }

            // تنظیم حجم
            float volumeLevel = volume / 100.0f;
            mediaPlayer.setVolume(volumeLevel, volumeLevel);

            // تنظیم looping بر اساس گروه
            mediaPlayer.setLooping(sound.isLoopingGroup());

            mediaPlayer.setOnCompletionListener(mp -> {
                mediaPlayers.remove(soundKey);
                unregisterSound(sound);
                runOnUiThread(() -> {
                    callback.onPlaybackStopped(sound.getId());

                    // اگر از گروه music است، آهنگ بعدی را پخش کن
                    if (sound.isMusicGroup()) {
                        playNextMusicSound(sound);
                    }
                });
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mediaPlayers.remove(soundKey);
                unregisterSound(sound);
                String errorMsg = "MediaPlayer error: " + what + ", extra: " + extra;
                Log.e(TAG, errorMsg);
                runOnUiThread(() -> callback.onPlaybackError(sound.getId(), errorMsg));
                return false;
            });

            mediaPlayer.start();
            mediaPlayers.put(soundKey, mediaPlayer);

            Log.d(TAG, "Playback started: " + sound.getId() + ", Looping: " + sound.isLoopingGroup());
            runOnUiThread(() -> callback.onPlaybackStarted(sound.getId()));

        } catch (Exception e) {
            Log.e(TAG, "Playback error for " + sound.getId() + ": " + e.getMessage(), e);
            runOnUiThread(() -> callback.onPlaybackError(sound.getId(), "Playback error: " + e.getMessage()));
        }
    }

    // متد برای توقف همه آهنگ‌های music
    private void stopAllMusicSounds() {
        Set<String> keysToRemove = new HashSet<>();

        for (Map.Entry<String, MediaPlayer> entry : mediaPlayers.entrySet()) {
            String soundKey = entry.getKey();
            MediaPlayer mediaPlayer = entry.getValue();

            // پیدا کردن sound از روی soundKey
            Sound sound = findSoundByKey(soundKey);
            if (sound != null && sound.isMusicGroup()) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    Log.d(TAG, "Stopped music sound: " + sound.getId());
                }
                keysToRemove.add(soundKey);
            }
        }

        for (String key : keysToRemove) {
            mediaPlayers.remove(key);
            Sound sound = findSoundByKey(key);
            if (sound != null) {
                unregisterSound(sound);
            }
        }
    }

    // متد برای پخش آهنگ music بعدی
    private void playNextMusicSound(Sound currentSound) {


        runOnUiThread(() -> {
            if (mainActivityRef != null) {
                mainActivityRef.playNextMusicTrack(currentSound);
            }
        });
    }








    // توقف آهنگ
    public void stopSound(Sound sound) {
        String soundKey = getSoundKey(sound);
        if (mediaPlayers.containsKey(soundKey)) {
            MediaPlayer mediaPlayer = mediaPlayers.get(soundKey);
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Log.d(TAG, "Stopped: " + sound.getId());
            }
            mediaPlayers.remove(soundKey);
        }
        unregisterSound(sound);
    }

    // توقف همه آهنگ‌ها
    public void stopAllSounds() {
        try {
            for (MediaPlayer mediaPlayer : mediaPlayers.values()) {
                if (mediaPlayer != null){
                    if(mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                }
            }
            mediaPlayers.clear();
            soundKeyToSoundMap.clear();
            Log.d(TAG, "All sounds stopped");
        }catch (Exception e){
            try {
                for (MediaPlayer mediaPlayer : mediaPlayers.values()) {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();

                    }
                }
                mediaPlayers.clear();
                soundKeyToSoundMap.clear();
            }catch (Exception e1){}
            //ToastUtils.showSafeToast(context,"Error: "+e.getMessage());
        }

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

    // به روزرسانی ولوم صدا در حال پخش
    public void updateSoundVolume(Sound sound, int volume) {
        executorService.execute(() -> {
            String soundKey = getSoundKey(sound);

            if (mediaPlayers.containsKey(soundKey)) {
                MediaPlayer mediaPlayer = mediaPlayers.get(soundKey);
                if (mediaPlayer != null) {
                    try {
                        float volumeLevel = volume / 100.0f;
                        mediaPlayer.setVolume(volumeLevel, volumeLevel);
                        Log.d(TAG, "Volume updated for " + sound.getId() + ": " + volume + "%");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating volume for " + sound.getId() + ": " + e.getMessage());
                    }
                }
            }
        });
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
        soundKeyToSoundMap.clear();

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
            Log.d(TAG, "Deleted: " + sound.getId());
        }

        return deleted;
    }

    // تنظیم reference به MainActivity
    public void setMainActivityRef(MainActivity mainActivity) {
        this.mainActivityRef = mainActivity;
    }

    // اجرا روی thread اصلی
    private void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}