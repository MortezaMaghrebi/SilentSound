package ir.zemestoon.silentsound;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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

    private String currentTab = "طبیعت";
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
        tabNature.setOnClickListener(v -> switchTab("طبیعت", tabNature));
        tabMusic.setOnClickListener(v -> switchTab("آهنگ‌ها", tabMusic));
        tabNoise.setOnClickListener(v -> switchTab("نویزها", tabNoise));
        tabWaves.setOnClickListener(v -> switchTab("موج‌ها", tabWaves));
        tabStories.setOnClickListener(v -> switchTab("داستان‌ها", tabStories));
        tabPresets.setOnClickListener(v -> switchTab("ترکیب‌ها", tabPresets));
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
        }, screenWidth, MainActivity.this); // ارسال this به آداپتر
        soundsRecyclerView.setAdapter(soundAdapter);
    }

    private void toggleSoundPlayback(Sound sound) {
        String soundName = sound.getName();

        // اگر آدرس آهنگ خالی است، به کاربر اطلاع بده
        if (sound.getAudioUrl() == null || sound.getAudioUrl().isEmpty()) {
            showToast("آهنگ " + soundName + " در دسترس نیست");
            return;
        }

        if (isSoundPlaying(soundName)) {
            // اگر در حال پخش است، متوقفش کن
            audioManager.stopSound(soundName);
            playingStatus.put(soundName, false);
            showToast(soundName + " متوقف شد");
        } else {
            // اگر پخش نیست، پخشش کن
            audioManager.playSound(sound, sound.getVolume(), new AudioManager.PlaybackCallback() {
                @Override
                public void onPlaybackStarted(String soundName) {
                    playingStatus.put(soundName, true);
                    showToast(soundName + " در حال پخش...");
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

        updateAllItemsAppearance();
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
        allSounds.add(new Sound("طبیعت", "bird",
                "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png",
                baseUrl + "nature/bird.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "birds",
                "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png",
                baseUrl + "nature/birds.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "cat purring",
                "https://img.icons8.com/ios-filled/50/FFFFFF/cat.png",
                baseUrl + "nature/cat_purring.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "cricket",
                "https://img.icons8.com/ios-filled/50/FFFFFF/cricket.png",
                baseUrl + "nature/cricket.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "dripping",
                "https://img.icons8.com/ios-filled/50/FFFFFF/water.png",
                baseUrl + "nature/dripping.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "firewood",
                "https://img.icons8.com/ios-filled/50/FFFFFF/fire.png",
                baseUrl + "nature/firewood.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "forest",
                "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png",
                baseUrl + "nature/forest.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "frog",
                "https://img.icons8.com/ios-filled/50/FFFFFF/frog.png",
                baseUrl + "nature/frog.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "grassland",
                "https://img.icons8.com/ios-filled/50/FFFFFF/grass.png",
                baseUrl + "nature/grassland.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "heavy rain",
                "https://img.icons8.com/ios-filled/50/FFFFFF/heavy-rain.png",
                baseUrl + "nature/heavy_rain.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "loon",
                "https://img.icons8.com/ios-filled/50/FFFFFF/dove.png",
                baseUrl + "nature/loon.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "owl",
                "https://img.icons8.com/ios-filled/50/FFFFFF/owl.png",
                baseUrl + "nature/owl.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "rain",
                "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png",
                baseUrl + "nature/rain.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "rain on roof",
                "https://img.icons8.com/ios-filled/50/FFFFFF/home.png",
                baseUrl + "nature/rain_on_roof.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "rain on tent",
                "https://img.icons8.com/ios-filled/50/FFFFFF/camping-tent.png",
                baseUrl + "nature/rain_on_tent.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "rain on window",
                "https://img.icons8.com/ios-filled/50/FFFFFF/window.png",
                baseUrl + "nature/rain_on_window.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "sea",
                "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png",
                baseUrl + "nature/sea.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "seagull",
                "https://img.icons8.com/ios-filled/50/FFFFFF/seagull.png",
                baseUrl + "nature/seagull.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "snow",
                "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png",
                baseUrl + "nature/snow.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "thunder",
                "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png",
                baseUrl + "nature/thunder.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "under water",
                "https://img.icons8.com/ios-filled/50/FFFFFF/water.png",
                baseUrl + "nature/under_water.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "water flow",
                "https://img.icons8.com/ios-filled/50/FFFFFF/water.png",
                baseUrl + "nature/water_flow.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "waterfall",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waterfall.png",
                baseUrl + "nature/waterfall.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "whale",
                "https://img.icons8.com/ios-filled/50/FFFFFF/whale.png",
                baseUrl + "nature/whale.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "wind",
                "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png",
                baseUrl + "nature/wind.mp3", 50, false, false));

        allSounds.add(new Sound("طبیعت", "wolf",
                "https://img.icons8.com/ios-filled/50/FFFFFF/wolf.png",
                baseUrl + "nature/wolf.mp3", 50, false, false));


        allSounds.add(new Sound("موج‌ها", "Water\nMonk",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Water%20-%20The%20Binaural%20Monk%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Waves\n88-96Hz",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Waves%2088-96Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Willow\nGamma29",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Willow%20Gamma%2029%20-%2071%2C2Hz%20-%20Naumanni%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nTurtles",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Wunder%20-%20Alpha%20Sinus%2091Hz%20-%20101Hz%20-%20Sea%20Turtles%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Adagio\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Adagio%20Alpha%20105Hz%20-%20115Hz%20-%20Floating%20States%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nFocus",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Binaural%20Focus%20107Hz%20-%20115Hz%20-%20New%20Lab%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nFocus127",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Binaural%20Focus%20127Hz%20-%20135Hz%20-%20New%20Lab%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nInnerverse",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Binaural%20Innerverse%20Resonance%20-%20Lapalillo%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nLucid",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Binaural%20Lucid%20Waves%20-%20La%20Perezosa%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nBliss",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Bliss%20107%20Hz%20-%20115%20Hz%20-%20Brainwave%20Harmony%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nBrain",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Brain%20Waves%20-%20Brainbox%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nFocus97",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Focus%2097Hz%20-%20104Hz%20-%20Universal%20Frequency%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nMeditation",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Meditation%20-%20Brainbox%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nNight",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Night%20(106Hz%20-%20114Hz)%20-%20Binaural%20Explorer%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nPath",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Path%2096%20Hz%20-%20105%20Hz%20-%20Dreamlike%20States%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nProsperity",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Prosperity%20127%20Hz%20-%20135%20Hz%20-%20Aerial%20Lakes%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nSinus",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Sinus%2054%2C8hz%20-%2057%2C3%20hz%20-%20Mystical.mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nSoul",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Soul%20110Hz-117Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Alpha\nZone",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Alpha%20Zone%2093Hz%20-%20104Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Beta\nSinus",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Beta%20Sinus%20100%20Hz%20-%20114%20Hz%20-%20Ampinomene%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Beta\nWaves",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Beta%20Waves%20110Hz%20-%20130Hz%20-%20Calming%20Beats%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Ecriurem\n100Hz",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Ecriurem%20100-108Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Equilibrium\n96Hz",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Equilibrium%2096hz-104hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Eventi\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Eventi%20Alpha%20SInus%2088-94%20Hz%20-%20Restful%20SInus%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Flow State\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Flow%20State%20Alpha%20203.6%20Hz%20-%20211.6%20Hz%20-%20Syntropy%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Follia\nTheta",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Follia%20Theta%2041Hz%20-%2045Hz%20-%20Rerose%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Gamma\nSinus100",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Gamma%20Sinus%20100%20Hz%20-%20140%20Hz%20-%20Pola%20Ris%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Gamma\nSinus300",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Gamma%20Sinus%20300%20Hz%20-%20350%20Hz%20-%20Ampinomene%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Gamma\nWaves",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Gamma%20Waves%2089%20%2B%2086%20hz%20-%20Electronic%20Waves%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Inner\nStudy",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Inner%20Study%20110Hz%20-%20115Hz%20-%20Rerose%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Living\n150Hz",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Living%20150-158%20Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Luz\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Luz%20Alpha%20100hz-108hz%20-%20Rerose%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Mantra\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Mantra%20-%20Alpha%20Waves%20-%20Theta%20Time%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Memory\nGamma",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Memory%20Training%20Gamma%20-%20Isotopic%20Dreams%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Nation\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Nation%20-%20Alpha%20Sinus%2062%2C5%20Hz%20-%2066%20Hz%20-%20Drone%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Rem\nTheta",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Rem%20Theta%2060%20Hz%20-%2066%20Hz%20-%20Naumanni%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Revolve\nDelta",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Revolve%20Delta%20125Hz%20-%20128Hz%20-%20Solace%20Sonique%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Shaant\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Shaant%20Prayog%20Alpha%2074Hz%20-%2082Hz%20-%20Smoove%20Nappers%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Sphere\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Sphere%20of%20Alpha%2010%20Hz%20-%20The%20Inner%20Circle%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Transcend\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Transcend%20Alpha%20106%20Hz%20-%20114%20Hz%20-%20Earthbound%20(320).mp3", 50, false, false));

        allSounds.add(new Sound("موج‌ها", "Universal\nAlpha",
                "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png",
                baseUrl + "wave/Universal%20Alpha%2065%20Hz%20-%2073%20Hz%20-%20Aerial%20Lakes%20(320).mp3", 50, false, false));

        // آهنگ‌های بی‌کلام
        allSounds.add(new Sound("آهنگ‌ها", "autumn", "https://img.icons8.com/ios-filled/50/FFFFFF/autumn.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "earth", "https://img.icons8.com/ios-filled/50/FFFFFF/earth-planet.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "fire", "https://img.icons8.com/ios-filled/50/FFFFFF/fire.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "ice", "https://img.icons8.com/ios-filled/50/FFFFFF/ice.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "rain", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "snow", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "spring", "https://img.icons8.com/ios-filled/50/FFFFFF/spring.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "summer", "https://img.icons8.com/ios-filled/50/FFFFFF/summer.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "water", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "wind", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "winter", "https://img.icons8.com/ios-filled/50/FFFFFF/winter.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "an angles caress", "https://img.icons8.com/ios-filled/50/FFFFFF/feather.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "drifting in dreamland", "https://img.icons8.com/ios-filled/50/FFFFFF/cloud.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "first star in sky", "https://img.icons8.com/ios-filled/50/FFFFFF/star.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "gentle descent", "https://img.icons8.com/ios-filled/50/FFFFFF/down.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "midnight blue", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "safe and sound", "https://img.icons8.com/ios-filled/50/FFFFFF/shield.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "twilight fades", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "water flow", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "worlds away", "https://img.icons8.com/ios-filled/50/FFFFFF/planet.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "carvansaray", "https://img.icons8.com/ios-filled/50/FFFFFF/arch.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "songs from a secret garden", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png","", 50, false, false));

        // کیتارو
        allSounds.add(new Sound("آهنگ‌ها", "Heaven and Earth", "https://img.icons8.com/ios-filled/50/FFFFFF/planet.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Silk Road", "https://img.icons8.com/ios-filled/50/FFFFFF/road.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Matsuri", "https://img.icons8.com/ios-filled/50/FFFFFF/dragon.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "The Light of the Spirit", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Thinking of You", "https://img.icons8.com/ios-filled/50/FFFFFF/brain.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Kojiki", "https://img.icons8.com/ios-filled/50/FFFFFF/scroll.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Mandala", "https://img.icons8.com/ios-filled/50/FFFFFF/mandala.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "An Enchanted Evening", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png","", 50, false, false));

        // دن گیبسون
        allSounds.add(new Sound("آهنگ‌ها", "Gentle Rain", "https://img.icons8.com/ios-filled/50/FFFFFF/light-rain.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Whispers of the Forest", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Mountain Stream", "https://img.icons8.com/ios-filled/50/FFFFFF/mountain.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Ocean Dreams", "https://img.icons8.com/ios-filled/50/FFFFFF/ocean.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Summer Meadow", "https://img.icons8.com/ios-filled/50/FFFFFF/meadow.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Winter's Peace", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Desert Night", "https://img.icons8.com/ios-filled/50/FFFFFF/desert.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Spring Awakening", "https://img.icons8.com/ios-filled/50/FFFFFF/feather.png","", 50, false, false));

        // سکرت گاردن
        allSounds.add(new Sound("آهنگ‌ها", "Nocturne", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Song from a Secret Garden", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Sigma", "https://img.icons8.com/ios-filled/50/FFFFFF/infinity.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Papillon", "https://img.icons8.com/ios-filled/50/FFFFFF/butterfly.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Serenade to Spring", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Atlantia", "https://img.icons8.com/ios-filled/50/FFFFFF/atlantis.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Heartstrings", "https://img.icons8.com/ios-filled/50/FFFFFF/heart.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Escape", "https://img.icons8.com/ios-filled/50/FFFFFF/exit.png","", 50, false, false));

        // ناصر چشم‌آذر
        allSounds.add(new Sound("آهنگ‌ها", "Gole Yakh", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Baran", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Naghme-ye Shab", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Safar", "https://img.icons8.com/ios-filled/50/FFFFFF/airplane.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Deltangi", "https://img.icons8.com/ios-filled/50/FFFFFF/heart.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Peyk-e Mehr", "https://img.icons8.com/ios-filled/50/FFFFFF/dove.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Gol-e Aftabgardan", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Seda-ye Pa", "https://img.icons8.com/ios-filled/50/FFFFFF/walking.png","", 50, false, false));

        // آهنگ با خواننده
        allSounds.add(new Sound("آهنگ‌ها", "Lullaby", "https://img.icons8.com/ios-filled/50/FFFFFF/baby.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Hymn for the Weekend", "https://img.icons8.com/ios-filled/50/FFFFFF/weekend.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Weightless", "https://img.icons8.com/ios-filled/50/FFFFFF/feather.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Clair de Lune", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Starlight", "https://img.icons8.com/ios-filled/50/FFFFFF/star.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Dream a Little Dream", "https://img.icons8.com/ios-filled/50/FFFFFF/cloud.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Breathe Me", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png","", 50, false, false));
        allSounds.add(new Sound("آهنگ‌ها", "Sleep Song", "https://img.icons8.com/ios-filled/50/FFFFFF/bed.png","", 50, false, false));

        // نویزها
        allSounds.add(new Sound("نویزها", "نویز آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/audio-wave.png","", 50, false, false));
        allSounds.add(new Sound("نویزها", "نویز بتا", "https://img.icons8.com/ios-filled/50/FFFFFF/audio-wave.png","", 50, false, false));
        allSounds.add(new Sound("نویزها", "نویز گاما", "https://img.icons8.com/ios-filled/50/FFFFFF/audio-wave.png","", 50, false, false));
        allSounds.add(new Sound("نویزها", "نویز تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/audio-wave.png","", 50, false, false));
        allSounds.add(new Sound("نویزها", "نویز سفید", "https://img.icons8.com/ios-filled/50/FFFFFF/audio-wave.png","", 50, false, false));

        // داستان‌ها
        allSounds.add(new Sound("داستان‌ها", "داستان غواصی", "https://img.icons8.com/ios-filled/50/FFFFFF/diving.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "داستان قایق سواری", "https://img.icons8.com/ios-filled/50/FFFFFF/sailboat.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "داستان دریاچه", "https://img.icons8.com/ios-filled/50/FFFFFF/lake.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "داستان بزغاله", "https://img.icons8.com/ios-filled/50/FFFFFF/goat.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "داستان کفش آهنی", "https://img.icons8.com/ios-filled/50/FFFFFF/iron.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "داستان قطب شمال", "https://img.icons8.com/ios-filled/50/FFFFFF/north-pole.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "خاله سوسکه", "https://img.icons8.com/ios-filled/50/FFFFFF/spider.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "خاله پیرزن", "https://img.icons8.com/ios-filled/50/FFFFFF/old-woman.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "کارخانه شکلات سازی", "https://img.icons8.com/ios-filled/50/FFFFFF/factory.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "فانوس", "https://img.icons8.com/ios-filled/50/FFFFFF/lantern.png","", 50, false, false));
        allSounds.add(new Sound("داستان‌ها", "کلبه آرامش", "https://img.icons8.com/ios-filled/50/FFFFFF/cabin.png","", 50, false, false));

        // ترکیب‌ها (Presets)
        allSounds.add(new Sound("ترکیب‌ها", "ساحل و غواصی", "https://img.icons8.com/ios-filled/50/FFFFFF/beach.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "ساحل و قایق سواری", "https://img.icons8.com/ios-filled/50/FFFFFF/sailboat.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "جنگل و رودخانه", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "آوای وحش", "https://img.icons8.com/ios-filled/50/FFFFFF/wolf.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "خاله پیرزن", "https://img.icons8.com/ios-filled/50/FFFFFF/old-woman.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "کفش آهنی", "https://img.icons8.com/ios-filled/50/FFFFFF/iron.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "باران شبانه", "https://img.icons8.com/ios-filled/50/FFFFFF/night-rain.png","", 50, false, false));
        allSounds.add(new Sound("ترکیب‌ها", "آتش‌گاه جنگلی", "https://img.icons8.com/ios-filled/50/FFFFFF/campfire.png","", 50, false, false));

        filterSoundsByGroup("طبیعت");
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