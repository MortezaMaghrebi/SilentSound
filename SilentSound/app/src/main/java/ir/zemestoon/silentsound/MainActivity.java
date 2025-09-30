package ir.zemestoon.silentsound;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // متغیرهای صدا
    private MediaPlayer mediaPlayer;
    private MediaPlayer backgroundMusic;
    private MediaPlayer storyAudio;

    // ویوها
    private SeekBar frequencySeekBar, volumeSeekBar;
    private TextView frequencyValue, volumeValue, timerDisplay;
    private Button playButton, stopButton;
    private GridView musicGrid, storyGrid;

    // تایمر
    private CountDownTimer autoShutdownTimer;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;

    // داده‌ها
    private ArrayList<AudioItem> musicList;
    private ArrayList<AudioItem> storyList;
    private AudioAdapter musicAdapter, storyAdapter;

    // وضعیت پخش
    private boolean isPlaying = false;
    private int currentFrequency = 200;
    private int currentVolume = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupAudioData();
        setupEventListeners();
    }

    private void initializeViews() {
        frequencySeekBar = findViewById(R.id.frequencySeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        frequencyValue = findViewById(R.id.frequencyValue);
        volumeValue = findViewById(R.id.volumeValue);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);
        musicGrid = findViewById(R.id.musicGrid);
        storyGrid = findViewById(R.id.storyGrid);
        timerDisplay = findViewById(R.id.timerDisplay);

        // تنظیم مقادیر اولیه
        frequencyValue.setText("200 Hz");
        volumeValue.setText("50%");
    }

    private void setupAudioData() {
        // لیست موسیقی‌های زمینه
        musicList = new ArrayList<>();
        musicList.add(new AudioItem("طبیعت", R.raw.nature, R.drawable.ic_nature));
        musicList.add(new AudioItem("اقیانوس", R.raw.ocean, R.drawable.ic_ocean));
        musicList.add(new AudioItem("باران", R.raw.rain, R.drawable.ic_rain));
        musicList.add(new AudioItem("جنگل", R.raw.forest, R.drawable.ic_forest));
        musicList.add(new AudioItem("باد", R.raw.wind, R.drawable.ic_wind));
        musicList.add(new AudioItem("سکوت", -1, R.drawable.ic_silence));

        // لیست داستان‌ها
        storyList = new ArrayList<>();
        storyList.add(new AudioItem("ساحل آرامش", R.raw.beach_story, R.drawable.ic_beach));
        storyList.add(new AudioItem("باغ ذهن", R.raw.garden_story, R.drawable.ic_garden));
        storyList.add(new AudioItem("کوهستان درون", R.raw.mountain_story, R.drawable.ic_mountain));
        storyList.add(new AudioItem("رودخانه زندگی", R.raw.river_story, R.drawable.ic_river));

        // تنظیم آداپتورها
        musicAdapter = new AudioAdapter(this, musicList);
        storyAdapter = new AudioAdapter(this, storyList);

        musicGrid.setAdapter(musicAdapter);
        storyGrid.setAdapter(storyAdapter);
    }

    private void setupEventListeners() {
        // کنترل فرکانس
        frequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFrequency = 80 + progress;
                frequencyValue.setText(currentFrequency + " Hz");
                if (isPlaying) {
                    updateAudioFrequency();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // کنترل حجم
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress;
                volumeValue.setText(progress + "%");
                updateVolume();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // دکمه پخش/توقف
        playButton.setOnClickListener(v -> togglePlayback());
        stopButton.setOnClickListener(v -> stopPlayback());

        // تایمرها
        setupTimerButtons();
    }

    private void setupTimerButtons() {
        int[] timerButtons = {R.id.timer5, R.id.timer10, R.id.timer15, R.id.timer30};
        int[] durations = {5, 10, 15, 30};

        for (int i = 0; i < timerButtons.length; i++) {
            int duration = durations[i];
            findViewById(timerButtons[i]).setOnClickListener(v -> setTimer(duration));
        }
    }

    private void togglePlayback() {
        if (!isPlaying) {
            startPlayback();
        } else {
            pausePlayback();
        }
    }

    private void startPlayback() {
        isPlaying = true;
        playButton.setText("مکث");

        // شروع پخش صداهای اصلی
        playMainSound();

        // شروع تایمر اگر فعال باشد
        if (timerRunning) {
            startTimer();
        }

        Toast.makeText(this, "سشن مدیتیشن شروع شد", Toast.LENGTH_SHORT).show();
    }

    private void pausePlayback() {
        isPlaying = false;
        playButton.setText("شروع");

        // توقف صداها
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        if (timerRunning) {
            pauseTimer();
        }
    }

    private void stopPlayback() {
        isPlaying = false;
        playButton.setText("شروع");

        // توقف کامل صداها
        stopAllAudio();

        // توقف تایمر
        stopTimer();

        Toast.makeText(this, "سشن مدیتیشن متوقف شد", Toast.LENGTH_SHORT).show();
    }

    private void playMainSound() {
        // در اینجا تولید صداهای سینت سایزر اضافه می‌شود
        // برای نمونه از یک فایل صوتی استفاده می‌کنیم
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.base_sound);
            mediaPlayer.setLooping(true);
        }
        mediaPlayer.start();
        updateVolume();
    }

    private void updateAudioFrequency() {
        // برای تغییر فرکانس در حالت واقعی نیاز به سینت سایزر داریم
        // اینجا فقط حجم را تنظیم می‌کنیم
        updateVolume();
    }

    private void updateVolume() {
        float volume = currentVolume / 100.0f;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume * 0.7f, volume * 0.7f);
        }
        if (storyAudio != null) {
            storyAudio.setVolume(volume * 0.8f, volume * 0.8f);
        }
    }

    private void stopAllAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (storyAudio != null) {
            storyAudio.stop();
            storyAudio.release();
            storyAudio = null;
        }
    }

    // مدیریت تایمر
    private void setTimer(int minutes) {
        timeLeftInMillis = minutes * 60 * 1000L;
        updateTimerDisplay();

        if (isPlaying) {
            startTimer();
        }

        Toast.makeText(this, "تایمر برای " + minutes + " دقیقه تنظیم شد", Toast.LENGTH_SHORT).show();
    }

    private void startTimer() {
        if (autoShutdownTimer != null) {
            autoShutdownTimer.cancel();
        }

        autoShutdownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                stopPlayback();
                timerDisplay.setText("--:--");
                Toast.makeText(MainActivity.this, "زمان سشن به پایان رسید!", Toast.LENGTH_LONG).show();
            }
        }.start();

        timerRunning = true;
    }

    private void pauseTimer() {
        if (autoShutdownTimer != null) {
            autoShutdownTimer.cancel();
        }
        timerRunning = false;
    }

    private void stopTimer() {
        if (autoShutdownTimer != null) {
            autoShutdownTimer.cancel();
        }
        timerRunning = false;
        timeLeftInMillis = 0;
        timerDisplay.setText("--:--");
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeft = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerDisplay.setText(timeLeft);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllAudio();
        stopTimer();
    }

    // کلاس برای آیتم‌های صوتی
    public class AudioItem {
        private String title;
        private int audioResource;
        private int iconResource;

        public AudioItem(String title, int audioResource, int iconResource) {
            this.title = title;
            this.audioResource = audioResource;
            this.iconResource = iconResource;
        }

        // Getter methods
        public String getTitle() { return title; }
        public int getAudioResource() { return audioResource; }
        public int getIconResource() { return iconResource; }
    }

    // آداپتور برای GridView
    public class AudioAdapter extends ArrayAdapter<AudioItem> {
        public AudioAdapter(MainActivity context, ArrayList<AudioItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            AudioItem item = getItem(position);

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.audio_item, parent, false);
            }

            CardView cardView = convertView.findViewById(R.id.audioCard);
            ImageView icon = convertView.findViewById(R.id.audioIcon);
            TextView title = convertView.findViewById(R.id.audioTitle);

            icon.setImageResource(item.getIconResource());
            title.setText(item.getTitle());

            cardView.setOnClickListener(v -> {
                playSelectedAudio(item);
            });

            return convertView;
        }
    }

    private void playSelectedAudio(AudioItem item) {
        // توقف صداهای قبلی
        if (item.getAudioResource() == -1) {
            // حالت سکوت
            if (backgroundMusic != null) {
                backgroundMusic.pause();
            }
            if (storyAudio != null) {
                storyAudio.pause();
            }
            return;
        }

        // تشخیص نوع صدا و پخش
        if (musicList.contains(item)) {
            // موسیقی زمینه
            if (backgroundMusic != null) {
                backgroundMusic.release();
            }
            backgroundMusic = MediaPlayer.create(this, item.getAudioResource());
            backgroundMusic.setLooping(true);
            backgroundMusic.start();
        } else {
            // داستان
            if (storyAudio != null) {
                storyAudio.release();
            }
            storyAudio = MediaPlayer.create(this, item.getAudioResource());
            storyAudio.start();
        }

        updateVolume();
        Toast.makeText(this, "در حال پخش: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }
}