package ir.zemestoon.silentsound;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private RecyclerView soundsRecyclerView;
    private LinearLayout tabNature, tabMusic, tabNoise,tabWaves, tabStories, tabPresets;
    private LinearLayout timerButtonsLayout;
    private TextView timerDisplay;
    private Button playButton, stopButton;

    private SoundAdapter soundAdapter;
    private List<Sound> allSounds;
    private List<Sound> filteredSounds;

    private String currentTab = "nature";
    private int selectedTimer = -1;

    private AudioManager audioManager;
    private Map<String, Boolean> playingStatus; // وضعیت پخش هر صدا

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cache) {
            audioManager.clearCache();
            showToast("کش پاک شد");
            updateAllItemsAppearance();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private Map<String, Sound> soundMap = new HashMap<>(); // اضافه کردن این خط

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = AudioManager.getInstance(this);
        showDownloadStatus();
        playingStatus = new HashMap<>();

        initViews();
        setupTabs();
        setupRecyclerView();
        setupTimerButtons();
        loadSounds();
        initializeSoundMap();
    }

    private void initializeSoundMap() {
        soundMap.clear();
        for (Sound sound : allSounds) {
            soundMap.put(sound.getName(), sound);
        }
    }
    private void initViews() {
        soundsRecyclerView = findViewById(R.id.soundsRecyclerView);

        // تب‌ها
        tabNature = findViewById(R.id.tabNature);
        tabMusic = findViewById(R.id.tabMusic);
        tabNoise = findViewById(R.id.tabNoise);
        tabWaves = findViewById(R.id.tabWaves);
        tabStories = findViewById(R.id.tabStories);
        tabPresets = findViewById(R.id.tabPresets);// ... بقیه تب‌ها

        timerButtonsLayout = findViewById(R.id.timerButtonsLayout);
        timerDisplay = findViewById(R.id.timerDisplay);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);

        playButton.setOnClickListener(v -> playAllSounds());
        stopButton.setOnClickListener(v -> stopAllSounds());
    }

    private void setupTabs() {
        tabNature.setOnClickListener(v -> switchTab("nature", tabNature));
        tabMusic.setOnClickListener(v -> switchTab("music", tabMusic));
        tabNoise.setOnClickListener(v -> switchTab("noise", tabNoise));
        tabWaves.setOnClickListener(v -> switchTab("wave", tabWaves));
        tabStories.setOnClickListener(v -> switchTab("story", tabStories));
        tabPresets.setOnClickListener(v -> switchTab("preset", tabPresets));
    }

    private void switchTab(String tabName, LinearLayout selectedTab) {
        currentTab = tabName;

        // ریست کردن همه تب‌ها
        resetTabs();

        // ست کردن تب انتخاب شده
        selectedTab.setBackgroundResource(R.drawable.tab_background_selected);
        TextView textView = (TextView) selectedTab.getChildAt(1);
        textView.setTextColor(Color.parseColor("#3B82F6"));
        ImageView imageView = (ImageView) selectedTab.getChildAt(0);
        imageView.setColorFilter(Color.parseColor("#3B82F6"));

        filterSoundsByGroup(tabName);
    }

    private void resetTabs() {
        LinearLayout[] tabs = {tabNature, tabMusic, tabNoise,tabWaves, tabStories, tabPresets};
        for (LinearLayout tab : tabs) {
            if (tab != null) {
                tab.setBackgroundResource(R.drawable.tab_background);
                TextView textView = (TextView) tab.getChildAt(1);
                textView.setTextColor(Color.parseColor("#64748B"));
                ImageView imageView = (ImageView) tab.getChildAt(0);
                imageView.setColorFilter(Color.parseColor("#64748B"));
            }
        }
    }

    // آپدیت آداپتر برای پخش آهنگ هنگام کلیک
    private void setupRecyclerView() {
        filteredSounds = new ArrayList<>();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        soundsRecyclerView.setLayoutManager(layoutManager);

        soundAdapter = new SoundAdapter(filteredSounds, new SoundAdapter.OnSoundClickListener() {
            @Override
            public void onSoundClick(Sound sound) {
                toggleSoundPlayback(sound);
            }

            @Override
            public void onVolumeChanged(Sound sound, int volume) {
                sound.setVolume(volume);
                updateItemAppearance(sound);
            }

            @Override
            public void onSelectionChanged(Sound sound, boolean selected) {
                sound.setSelected(selected);
                updateItemAppearance(sound);
            }

            @Override
            public void onDownloadProgress(Sound sound, int progress) {

            }
        }, screenWidth, MainActivity.this); // ارسال this به آداپتر
        soundsRecyclerView.setAdapter(soundAdapter);
    }



    public void updateSoundDownloadProgress(final String soundName, final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // پیدا کردن صدا در لیست فیلتر شده
                int position = -1;
                for (int i = 0; i < filteredSounds.size(); i++) {
                    if (filteredSounds.get(i).getName().equals(soundName)) {
                        position = i;
                        break;
                    }
                }

                // آپدیت آیتم در آداپتر
                if (position != -1) {
                    soundAdapter.notifyItemChanged(position);

                    // لاگ برای دیباگ
                    Log.d("UI_Update", "Updating UI for: " + soundName + " at position: " + position + " progress: " + progress);
                } else {
                    Log.d("UI_Update", "Sound not found in filtered list: " + soundName);
                }
            }
        });
    }

    private Sound findSoundByName(String soundName) {
        for (Sound sound : allSounds) {
            if (sound.getName().equals(soundName)) {
                return sound;
            }
        }
        return null;
    }

    private void startDownloadWithProgress(Sound sound) {
        sound.setSelected(true);
        updateItemAppearance(sound); // آپدیت UI برای نمایش انتخاب
        audioManager.downloadSound(sound, new AudioManager.DownloadCallback() {
            @Override
            public void onDownloadProgress(String soundName, int progress) {
                updateSoundDownloadProgress(soundName, progress);
                Log.d("DownloadProgress", soundName + ": " + progress + "%");
            }

            @Override
            public void onDownloadComplete(String soundName, String localPath) {
                // آپدیت صدا و UI
                Sound currentSound = soundMap.get(soundName);
                if (currentSound != null) {
                    currentSound.setLocalPath(localPath);
                }
                updateSoundDownloadProgress(soundName, 100);
                showToast(soundName + " دانلود شد");

                // یک آپدیت نهایی بعد از 500ms برای اطمینان
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateSoundDownloadProgress(soundName, 100);
                        if (currentSound != null) {
                            playSoundAfterDownload(currentSound);
                        }
                    }
                }, 500);
            }

            @Override
            public void onDownloadError(String soundName, String error) {
                Sound currentSound = soundMap.get(soundName);
                if (currentSound != null) {
                    currentSound.setSelected(false);
                }
                updateSoundDownloadProgress(soundName, 0);
                showToast("خطا در دانلود " + soundName + ": " + error);
            }
        });
    }

    private void playSoundAfterDownload(Sound sound) {
        // مطمئن شو صدا انتخاب شده
        sound.setSelected(true);

        // پخش صدا
        audioManager.playSound(sound, sound.getVolume(), new AudioManager.PlaybackCallback() {
            @Override
            public void onPlaybackStarted(String soundName) {
                playingStatus.put(soundName, true);
                //showToast(soundName + " در حال پخش...");
                updateAllItemsAppearance();

                // لاگ برای دیباگ
                Log.d("Playback", "Playback started after download: " + soundName);
            }

            @Override
            public void onPlaybackStopped(String soundName) {
                playingStatus.put(soundName, false);
                updateAllItemsAppearance();
            }

            @Override
            public void onPlaybackError(String soundName, String error) {
                playingStatus.put(soundName, false);
                showToast("خطا در پخش " + soundName + ": " + error);
                updateAllItemsAppearance();
            }
        });

        // آپدیت UI برای نمایش وضعیت پخش
        updateAllItemsAppearance();
    }

    private void toggleSoundPlayback(Sound sound) {
        String soundName = sound.getName();

        if (sound.getAudioUrl() == null || sound.getAudioUrl().isEmpty()) {
            showToast("آهنگ " + soundName + " در دسترس نیست");
            return;
        }

        if (isSoundPlaying(soundName)) {
            // توقف پخش
            audioManager.stopSound(sound);
            playingStatus.put(soundName, false);
            //showToast(soundName + " متوقف شد");
            updateAllItemsAppearance();
        } else {
            if (!audioManager.isSoundDownloaded(sound)) {
                // فوراً UI رو آپدیت کن
                updateSoundDownloadProgress(soundName, 5); // 5% برای شروع
                // سپس دانلود رو شروع کن
                startDownloadWithProgress(sound);
            } else { // شروع پخش با نمایش progress دانلود
                audioManager.playSound(sound, sound.getVolume(), new AudioManager.PlaybackCallback() {
                    @Override
                    public void onPlaybackStarted(String soundName) {
                        playingStatus.put(soundName, true);
                        //showToast(soundName + " در حال پخش...");
                        updateAllItemsAppearance();
                    }

                    @Override
                    public void onPlaybackStopped(String soundName) {
                        playingStatus.put(soundName, false);
                        updateAllItemsAppearance();
                    }

                    @Override
                    public void onPlaybackError(String soundName, String error) {
                        playingStatus.put(soundName, false);
                        showToast("خطا در پخش " + soundName + ": " + error);
                        updateAllItemsAppearance();
                    }
                });
            }

        }
    }

    public boolean isSoundPlaying(String soundName) {
        return playingStatus.containsKey(soundName) && playingStatus.get(soundName);
    }

    private void playAllSounds() {
        for (Sound sound : filteredSounds) {
            if (sound.isSelected() && !isSoundPlaying(sound.getName())) {
                toggleSoundPlayback(sound);
            }
        }
    }

    private void stopAllSounds() {
        audioManager.stopAllSounds();
        playingStatus.clear();

        // آپدیت ظاهر همه آیتم‌ها
        for (Sound sound : filteredSounds) {
            updateItemAppearance(sound);
        }

        if (selectedTimer != -1) {
            // توقف تایمر
        }
        timerDisplay.setText("تایمر فعال: ندارد");
        selectedTimer = -1;
        updateTimerButtonsAppearance(null);

        showToast("همه صداها متوقف شدند");
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null) {
            audioManager.stopAllSounds();
        }
    }
    private void updateAllItemsAppearance() {
        if (soundAdapter != null) {
            soundAdapter.notifyDataSetChanged();
        }
    }
    private void updateItemAppearance(Sound sound) {
        // پیدا کردن موقعیت صدا در لیست فیلتر شده
        int position = -1;
        for (int i = 0; i < filteredSounds.size(); i++) {
            if (filteredSounds.get(i).getName().equals(sound.getName())) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            soundAdapter.notifyItemChanged(position);
        }
    }

    private void setupTimerButtons() {
        int[] timerMinutes = {5, 10, 20, 30, 40, 45, 50, 60, 90};

        for (int minutes : timerMinutes) {
            Button timerButton = new Button(this);
            timerButton.setText(minutes + " دقیقه");
            timerButton.setBackgroundResource(R.drawable.timer_button);
            timerButton.setTextColor(Color.WHITE);
            timerButton.setPadding(20, 10, 20, 10);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            timerButton.setLayoutParams(params);

            timerButton.setOnClickListener(v -> {
                selectedTimer = minutes;
                setTimer(minutes);
                updateTimerButtonsAppearance(timerButton);
            });

            timerButtonsLayout.addView(timerButton);
        }
    }

    private void updateTimerButtonsAppearance(Button selectedButton) {
        for (int i = 0; i < timerButtonsLayout.getChildCount(); i++) {
            Button button = (Button) timerButtonsLayout.getChildAt(i);
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.timer_button_selected);
                button.setTextColor(Color.WHITE);
            } else {
                button.setBackgroundResource(R.drawable.timer_button);
                button.setTextColor(Color.WHITE);
            }
        }
    }

    private void setTimer(int minutes) {
        timerDisplay.setText("تایمر فعال: " + minutes + " دقیقه");

        // منطق تایمر
        new CountDownTimer(minutes * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutesLeft = millisUntilFinished / (60 * 1000);
                long secondsLeft = (millisUntilFinished % (60 * 1000)) / 1000;
                timerDisplay.setText(String.format("تایمر فعال: %d:%02d", minutesLeft, secondsLeft));
            }

            public void onFinish() {
                timerDisplay.setText("تایمر فعال: ندارد");
                stopAllSounds();
                selectedTimer = -1;
                updateTimerButtonsAppearance(null);
            }
        }.start();
    }

    private void showDownloadStatus() {
        Set<String> downloadedSounds = audioManager.getDownloadedSounds();
        Log.d("DownloadStatus", "Downloaded sounds: " + downloadedSounds.size());
        for (String sound : downloadedSounds) {
            Log.d("DownloadStatus", "- " + sound);
        }
    }
    private void loadSounds() {
        allSounds = new ArrayList<>();
        String baseUrl = "https://raw.githubusercontent.com/MortezaMaghrebi/sounds/main/";
// صداهای طبیعت
// صداهای طبیعت
        // صداهای طبیعت
        allSounds.add(new Sound("nature", "پرنده", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/bird.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "پرندگان", "https://img.icons8.com/ios-filled/50/FFFFFF/hummingbird.png", baseUrl + "nature/birds.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "گربه خرخر", "https://img.icons8.com/ios-filled/50/FFFFFF/cat.png", baseUrl + "nature/cat_purring.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "جیرجیرک", "https://img.icons8.com/ios-filled/50/FFFFFF/cricket.png", baseUrl + "nature/cricket.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "چکه آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "nature/dripping.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "آتش هیزم", "https://img.icons8.com/ios-filled/50/FFFFFF/campfire.png", baseUrl + "nature/firewood.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "جنگل", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png", baseUrl + "nature/forest.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "قورباغه", "https://img.icons8.com/ios-filled/50/FFFFFF/frog.png", baseUrl + "nature/frog.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "چمنزار", "https://img.icons8.com/ios-filled/50/FFFFFF/grass.png", baseUrl + "nature/grassland.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باران شدید", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/heavy_rain.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "پرنده لون", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/loon.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "جغد", "https://img.icons8.com/ios-filled/50/FFFFFF/owl.png", baseUrl + "nature/owl.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باران", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باران روی سقف", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain_on_roof.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باران روی چادر", "https://img.icons8.com/ios-filled/50/FFFFFF/tent.png", baseUrl + "nature/rain_on_tent.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باران روی پنجره", "https://img.icons8.com/ios-filled/50/FFFFFF/window.png", baseUrl + "nature/rain_on_window.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "دریا", "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png", baseUrl + "nature/sea.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "مرغ دریایی", "https://img.icons8.com/ios-filled/50/FFFFFF/seagull.png", baseUrl + "nature/seagull.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "برف", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png", baseUrl + "nature/snow.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "رعد و برق", "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png", baseUrl + "nature/thunder.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "زیر آب", "https://img.icons8.com/ios-filled/50/FFFFFF/submarine.png", baseUrl + "nature/under_water.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "جریان آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "nature/water_flow.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "آبشار", "https://img.icons8.com/ios-filled/50/FFFFFF/waterfall.png", baseUrl + "nature/waterfall.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "نهنگ", "https://img.icons8.com/ios-filled/50/FFFFFF/whale.png", baseUrl + "nature/whale.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "باد", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png", baseUrl + "nature/wind.mp3", 50, false, false));
        allSounds.add(new Sound("nature", "گرگ", "https://img.icons8.com/ios-filled/50/FFFFFF/wolf.png", baseUrl + "nature/wolf.mp3", 50, false, false));

// موسیقی
        allSounds.add(new Sound("music", "پاییز", "https://img.icons8.com/ios-filled/50/FFFFFF/autumn.png", baseUrl + "music/brian_crain/brian_autumn.mp3", 50, false, false));
        allSounds.add(new Sound("music", "زمین", "https://img.icons8.com/ios-filled/50/FFFFFF/globe.png", baseUrl + "music/brian_crain/brian_earth.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آتش", "https://img.icons8.com/ios-filled/50/FFFFFF/campfire.png", baseUrl + "music/brian_crain/brian_fire.mp3", 50, false, false));
        allSounds.add(new Sound("music", "یخ", "https://img.icons8.com/ios-filled/50/FFFFFF/icy.png", baseUrl + "music/brian_crain/brian_ice.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باران", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "music/brian_crain/brian_rain.mp3", 50, false, false));
        allSounds.add(new Sound("music", "برف", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png", baseUrl + "music/brian_crain/brian_snow.mp3", 50, false, false));
        allSounds.add(new Sound("music", "بهار", "https://img.icons8.com/ios-filled/50/FFFFFF/spring.png", baseUrl + "music/brian_crain/brian_spring.mp3", 50, false, false));
        allSounds.add(new Sound("music", "تابستان", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png", baseUrl + "music/brian_crain/brian_summer.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "music/brian_crain/brian_water.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باد", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png", baseUrl + "music/brian_crain/brian_wind.mp3", 50, false, false));
        allSounds.add(new Sound("music", "زمستان", "https://img.icons8.com/ios-filled/50/FFFFFF/winter.png", baseUrl + "music/brian_crain/brian_winter.mp3", 50, false, false));
        allSounds.add(new Sound("music", "انتظار", "https://img.icons8.com/ios-filled/50/FFFFFF/hourglass.png", baseUrl + "music/cheshmazar/awaiting.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آزادی", "https://img.icons8.com/ios-filled/50/FFFFFF/freedom.png", baseUrl + "music/cheshmazar/freedom.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عشق پرشور", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/cheshmazar/passion_of_love.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عشق پرشور ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/cheshmazar/passion_of_love_ii.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باران عشق", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "music/cheshmazar/rain_of_love.mp3", 50, false, false));
        allSounds.add(new Sound("music", "خیزش", "https://img.icons8.com/ios-filled/50/FFFFFF/sunrise.png", baseUrl + "music/cheshmazar/rising.mp3", 50, false, false));
        allSounds.add(new Sound("music", "خواب", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/cheshmazar/sleep.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دیدار", "https://img.icons8.com/ios-filled/50/FFFFFF/handshake.png", baseUrl + "music/cheshmazar/visit.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آکوا", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "music/kitaro/kitaro_aqua.mp3", 50, false, false));
        allSounds.add(new Sound("music", "کاروانسرا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/kitaro/kitaro_caravansary.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عاشقانه", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/kitaro/kitaro_romance.mp3", 50, false, false));
        allSounds.add(new Sound("music", "افق درخشان", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png", baseUrl + "music/kitaro/kitaro_shimmering_horizon.mp3", 50, false, false));
        allSounds.add(new Sound("music", "روح دریاچه", "https://img.icons8.com/ios-filled/50/FFFFFF/lake.png", baseUrl + "music/kitaro/kitaro_spirit_of_the_west_lake.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آداجیو", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_adagio.mp3", 50, false, false));
        allSounds.add(new Sound("music", "همیشه آنجا", "https://img.icons8.com/ios-filled/50/FFFFFF/pin.png", baseUrl + "music/secret_garden/sg_always_there.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آپاسیوناتا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_apassionata.mp3", 50, false, false));
        allSounds.add(new Sound("music", "کانتولونا", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png", baseUrl + "music/secret_garden/sg_cantoluna.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شانونه", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_chanonne.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویاگیر", "https://img.icons8.com/ios-filled/50/FFFFFF/dreamcatcher.png", baseUrl + "music/secret_garden/sg_dreamcatcher.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویای تو", "https://img.icons8.com/ios-filled/50/FFFFFF/swing.png", baseUrl + "music/secret_garden/sg_dreamed_of_you.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دوئت", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_duo.mp3", 50, false, false));
        allSounds.add(new Sound("music", "یخ‌زده در زمان", "https://img.icons8.com/ios-filled/50/FFFFFF/winter.png", baseUrl + "music/secret_garden/sg_fozen_in_time.mp3", 50, false, false));
        allSounds.add(new Sound("music", "موج‌های سبز", "https://img.icons8.com/ios-filled/50/FFFFFF/deezer.png", baseUrl + "music/secret_garden/sg_greenwaves.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سرود امید", "https://img.icons8.com/ios-filled/50/FFFFFF/lullaby.png", baseUrl + "music/secret_garden/sg_hymn_to_hope.mp3", 50, false, false));
        allSounds.add(new Sound("music", "لوتوس", "https://img.icons8.com/ios-filled/50/FFFFFF/lotus.png", baseUrl + "music/secret_garden/sg_lotus.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب تاریک", "https://img.icons8.com/ios-filled/50/FFFFFF/midnight.png", baseUrl + "music/secret_garden/sg_morketid.mp3", 50, false, false));
        allSounds.add(new Sound("music", "حرکت", "https://img.icons8.com/ios-filled/50/FFFFFF/dancing.png", baseUrl + "music/secret_garden/sg_moving.mp3", 50, false, false));
        allSounds.add(new Sound("music", "قرن جدید", "https://img.icons8.com/ios-filled/50/FFFFFF/clock.png", baseUrl + "music/secret_garden/sg_new_century.mp3", 50, false, false));
        allSounds.add(new Sound("music", "نوكتورن", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_nocturn.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سادگی", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_ode_to_simplicity.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پروانه", "https://img.icons8.com/ios-filled/50/FFFFFF/butterfly.png", baseUrl + "music/secret_garden/sg_papillon.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پاساكالیا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_passacaglia.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پاستورال", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_pastorale.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب مقدس", "https://img.icons8.com/ios-filled/50/FFFFFF/owl.png", baseUrl + "music/secret_garden/sg_sacred_night.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پناهگاه", "https://img.icons8.com/ios-filled/50/FFFFFF/tent.png", baseUrl + "music/secret_garden/sg_sanctuary.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باغ مخفی", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_secret_garden.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سرناد", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_serenade.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سیگما", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_sigma.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آهنگ خواب", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/secret_garden/sg_sleepsong.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سونا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_sona.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب طوفانی", "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png", baseUrl + "music/secret_garden/sg_stormy_night.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویا", "https://img.icons8.com/ios-filled/50/FFFFFF/unicorn.png", baseUrl + "music/secret_garden/sg_the_dream.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سفر", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_voyage.mp3", 50, false, false));
        allSounds.add(new Sound("music", "بی‌خیالی", "https://img.icons8.com/ios-filled/50/FFFFFF/easy.png", baseUrl + "music/secret_garden/sg_without_care.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رقص پروانه", "https://img.icons8.com/ios-filled/50/FFFFFF/butterfly.png", baseUrl + "music/yanni/yanni_butterfly_dance.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فلیتسا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/yanni/yanni_felitsa.mp3", 50, false, false));
        allSounds.add(new Sound("music", "در آینه", "https://img.icons8.com/ios-filled/50/FFFFFF/mirror.png", baseUrl + "music/yanni/yanni_in_the_mirror.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فقط یک خاطره", "https://img.icons8.com/ios-filled/50/FFFFFF/alzheimer.png", baseUrl + "music/yanni/yanni_only_a_memory.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سوگندهای مخفی", "https://img.icons8.com/ios-filled/50/FFFFFF/promise.png", baseUrl + "music/yanni/yanni_secret_vows.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دوست قدیمی", "https://img.icons8.com/ios-filled/50/FFFFFF/children.png", baseUrl + "music/yanni/yanni_so_long_my_friend.mp3", 50, false, false));
        allSounds.add(new Sound("music", "به کسی که می‌داند", "https://img.icons8.com/ios-filled/50/FFFFFF/reading.png", baseUrl + "music/yanni/yanni_to_the_one_who_knows.mp3", 50, false, false));

// نویز
        allSounds.add(new Sound("noise", "نویز قهوه‌ای", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "noise/brown_noise.mp3", 50, false, false));
        allSounds.add(new Sound("noise", "نویز سفید", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "noise/white_noise.mp3", 50, false, false));

// امواج
        allSounds.add(new Sound("wave", "آداجیو آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Adagio_Alpha_105-115Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "آلفا سعادت", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Bliss_107-115Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "امواج آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Brain_Waves.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_107-115Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_127-135Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۳", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_97-104Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "آلفا اینرورس", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Innerverse_Reso.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "امواج شفاف", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Lucid_Waves.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "مدیتیشن آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Meditation.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "شب آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Night_106-114Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "مسیر آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/path.png", baseUrl + "wave/Alpha_Path_96-105Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "رفاه آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Prosperity_127-135Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "شانت آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Shaant_74-82Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_54.8-57.3Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_62.5-66Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۳", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_88-94Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۴", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_91-101Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "روح آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Soul_110-117Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "کره آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sphere_10Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "ترانسند آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Transcend_106-114Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "یونیورسال آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Universal_65-73Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "امواج آلفا ۸۸", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Waves_88-96Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "زون آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Zone_93-104Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس بتا", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Beta_Sinus_100-114Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "امواج بتا", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Beta_Waves_110-130Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "چرخش دلتا", "https://img.icons8.com/ios-filled/50/FFFFFF/d.png", baseUrl + "wave/Delta_Revolve_125-128Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "اکریورم", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Ecriurem_100-108Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "تعادل", "https://img.icons8.com/ios-filled/50/FFFFFF/balance.png", baseUrl + "wave/Equilibrium_96-104Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "فلو آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Flow_Alpha_203-211Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "حافظه گاما", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Memory_Training.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس گاما ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Sinus_100-140Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "سینوس گاما ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Sinus_300-350Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "امواج گاما ۸۶", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Waves_86+89Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "گاما ویلو", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Willow_29-71Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "مطالعه داخلی", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Inner_Study_110-115Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "زندگی", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Living_150-158Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "لوز آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Luz_Alpha_100-108Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "مانترا آلفا-تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Mantra_Alpha-Theta.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "فولیا تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Theta_Follia_41-45Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "رم تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Theta_Rem_60-66Hz.mp3.mp3", 50, false, false));
        allSounds.add(new Sound("wave", "راهب آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "wave/Water_Monk.mp3.mp3", 50, false, false));

        allSounds.add(new Sound("story", "داستان غواصی", "https://img.icons8.com/ios-filled/50/FFFFFF/cliff.png","", 50, false, false));
        allSounds.add(new Sound("story", "داستان قایق سواری", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png","", 50, false, false));
        allSounds.add(new Sound("story", "داستان دریاچه", "https://img.icons8.com/ios-filled/50/FFFFFF/lake.png","", 50, false, false));
        allSounds.add(new Sound("story", "داستان بزغاله", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png","", 50, false, false));
        allSounds.add(new Sound("story", "داستان کفش آهنی", "https://img.icons8.com/ios-filled/50/FFFFFF/iron.png","", 50, false, false));
        allSounds.add(new Sound("story", "داستان قطب شمال", "https://img.icons8.com/ios-filled/50/FFFFFF/north.png","", 50, false, false));
        allSounds.add(new Sound("story", "خاله سوسکه", "https://img.icons8.com/ios-filled/50/FFFFFF/spider.png","", 50, false, false));
        allSounds.add(new Sound("story", "خاله پیرزن", "https://img.icons8.com/ios-filled/50/FFFFFF/old-woman.png","", 50, false, false));
        allSounds.add(new Sound("story", "کارخانه شکلات سازی", "https://img.icons8.com/ios-filled/50/FFFFFF/factory.png","", 50, false, false));
        allSounds.add(new Sound("story", "فانوس", "https://img.icons8.com/ios-filled/50/FFFFFF/lantern.png","", 50, false, false));
        allSounds.add(new Sound("story", "کلبه آرامش", "https://img.icons8.com/ios-filled/50/FFFFFF/cabin.png","", 50, false, false));

        // ترکیب‌ها (Presets)
        allSounds.add(new Sound("preset", "ساحل و غواصی", "https://img.icons8.com/ios-filled/50/FFFFFF/beach.png","", 50, false, false));
        allSounds.add(new Sound("preset", "ساحل و قایق سواری", "https://img.icons8.com/ios-filled/50/FFFFFF/sailboat.png","", 50, false, false));
        allSounds.add(new Sound("preset", "جنگل و رودخانه", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png","", 50, false, false));
        allSounds.add(new Sound("preset", "آوای وحش", "https://img.icons8.com/ios-filled/50/FFFFFF/wolf.png","", 50, false, false));
        allSounds.add(new Sound("preset", "خاله پیرزن", "https://img.icons8.com/ios-filled/50/FFFFFF/old-woman.png","", 50, false, false));
        allSounds.add(new Sound("preset", "کفش آهنی", "https://img.icons8.com/ios-filled/50/FFFFFF/iron.png","", 50, false, false));
        allSounds.add(new Sound("preset", "باران شبانه", "https://img.icons8.com/ios-filled/50/FFFFFF/night-rain.png","", 50, false, false));
        allSounds.add(new Sound("preset", "آتش‌گاه جنگلی", "https://img.icons8.com/ios-filled/50/FFFFFF/campfire.png","", 50, false, false));

        filterSoundsByGroup("nature");
    }

    private void filterSoundsByGroup(String group) {
        filteredSounds.clear();
        for (Sound sound : allSounds) {
            if (sound.getGroup().equals(group)) {
                filteredSounds.add(sound);
            }
        }
        soundAdapter.updateList(filteredSounds);
    }




}