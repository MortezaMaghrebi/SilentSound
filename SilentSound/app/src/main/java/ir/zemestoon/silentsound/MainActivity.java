package ir.zemestoon.silentsound;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private RecyclerView soundsRecyclerView,mixesRecyclerView;

    private LinearLayout tabNature, tabMusic, tabNoise, tabWaves, tabStories, tabPresets;
    private LinearLayout timerButtonsLayout;
    private LinearLayout llMixesButton,llSoundsButton;
    private RelativeLayout rlMixes,rlSounds;
    TextView tvMixes,tvSounds;
    ImageView ivMixes,ivSounds;
    private TextView timerDisplay;
    private ImageButton playPauseButton, nextButton, prevButton;
    private SoundAdapter soundAdapter;
    private MixedAdapter mixedAdapter;
    private List<Sound> allSounds;
    private List<Sound> filteredSounds;

    private List<Mixed> allMixes;
    private String currentTab = "nature";
    private int selectedTimer = -1;

    public AudioManager audioManager;
    public Map<String, Boolean> playingStatus;
    public Map<String, Boolean> mixedPlayingStatus;

    // اضافه کردن لیست برای مدیریت ترتیب پخش آهنگ‌های music
    public List<Sound> musicPlaylist = new ArrayList<>();
    public int currentMusicIndex = -1;
    public Map<String, Sound> soundMap = new HashMap<>();

    // اضافه کردن flag برای جلوگیری از حلقه بی‌نهایت
    private boolean isAutoPlayingNext = false;

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
        audioManager.setMainActivityRef(this);
        showDownloadStatus();
        playingStatus = new HashMap<>();
        mixedPlayingStatus = new HashMap<>();
        initViews();
        setupTabs();
        setupRecyclerView();
        setupMixesRecyclerView();
        setupTimerButtons();
        loadSounds();
        loadMixes();
        initializeSoundMap();
    }

    private void initializeSoundMap() {
        soundMap.clear();
        for (Sound sound : allSounds) {
            soundMap.put(sound.getId(), sound);
        }
    }

    private void initViews() {
        soundsRecyclerView = findViewById(R.id.soundsRecyclerView);
        mixesRecyclerView = findViewById(R.id.mixesRecyclerView);

        // تب‌ها
        tabNature = findViewById(R.id.tabNature);
        tabMusic = findViewById(R.id.tabMusic);
        tabNoise = findViewById(R.id.tabNoise);
        tabWaves = findViewById(R.id.tabWaves);
        tabStories = findViewById(R.id.tabStories);
        tabPresets = findViewById(R.id.tabPresets);

        timerButtonsLayout = findViewById(R.id.timerButtonsLayout);
        timerDisplay = findViewById(R.id.timerDisplay);
        playPauseButton = findViewById(R.id.playPauseButton);
        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);

        playPauseButton.setOnClickListener(v -> playPauseButtonClick());
        nextButton.setOnClickListener(v -> nextButtonClick());
        prevButton.setOnClickListener(v -> prevButtonClick());

        rlMixes=findViewById(R.id.rlMixes);
        rlSounds=findViewById(R.id.rlSounds);
        llMixesButton=findViewById(R.id.llMixesButton);
        llSoundsButton=findViewById(R.id.llSoundsButton);
         tvMixes = findViewById(R.id.tvMixes);
         tvSounds = findViewById(R.id.tvSounds);
         ivMixes = findViewById(R.id.ivMixes);
         ivSounds = findViewById(R.id.ivSounds);

        rlMixes.setVisibility(View.VISIBLE);
        rlSounds.setVisibility(View.GONE);



        llMixesButton.setOnClickListener(v -> {
            if (rlMixes.getVisibility() == View.GONE) {
                Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
                Animation slideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

                rlMixes.startAnimation(slideInLeft);
                rlSounds.startAnimation(slideOutRight);

                rlMixes.setVisibility(View.VISIBLE);
                rlSounds.setVisibility(View.GONE);

                // تغییر رنگ دکمه‌ها
                tvMixes.setTextColor(getResources().getColor(R.color.active_blue));
                ivMixes.setColorFilter(getResources().getColor(R.color.active_blue));
                tvSounds.setTextColor(getResources().getColor(R.color.inactive_white));
                ivSounds.setColorFilter(getResources().getColor(R.color.inactive_white));
            }
        });

        llSoundsButton.setOnClickListener(v -> {
            if (rlSounds.getVisibility() == View.GONE) {
                Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                Animation slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

                rlSounds.startAnimation(slideInRight);
                rlMixes.startAnimation(slideOutLeft);

                rlSounds.setVisibility(View.VISIBLE);
                rlMixes.setVisibility(View.GONE);

                // تغییر رنگ دکمه‌ها
                tvSounds.setTextColor(getResources().getColor(R.color.active_blue));
                ivSounds.setColorFilter(getResources().getColor(R.color.active_blue));
                tvMixes.setTextColor(getResources().getColor(R.color.inactive_white));
                ivMixes.setColorFilter(getResources().getColor(R.color.inactive_white));
            }
        });

    }

    boolean isPlaying = false;

    private void playPauseButtonClick() {
        if (!isPlaying) {
            playAllSounds();
        } else {
            stopAllSounds();
        }

    }

    public void updatePlayPauseAppearance() {
        if (!musicPlaylist.isEmpty()) {
            Sound currentsound=getCurrentlyPlayingMusic();
            isPlaying = currentsound != null;
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            if(isPlaying)
            {
                TextView songTitle = findViewById(R.id.songTitle);
                TextView artistName = findViewById(R.id.artistName);
                ImageView songArt = findViewById(R.id.songArt);
                songTitle.setText(currentsound.getName());
                String url=currentsound.getAudioUrl();
                String[] urlparts=currentsound.getAudioUrl().toString().split("/");
                String[] artistNameParts = urlparts[urlparts.length-2].split("_");
                StringBuilder  artistname=new StringBuilder();
                for (String word:artistNameParts) {
                    if (!word.isEmpty()) {
                        artistname.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase())
                                .append(" ");
                    }
                }
                artistName.setText(artistname);
                songArt.setImageResource(R.drawable.ic_music);
            }
        }
    }


    private void nextButtonClick()
    {
        if (!musicPlaylist.isEmpty() ) {
            //currentMusicIndex = 0;
            Sound selectedMusic = musicPlaylist.get(currentMusicIndex);
            if (isSoundPlaying(selectedMusic.getId())) {
                // توقف پخش
                audioManager.stopSound(selectedMusic);
                playingStatus.put(selectedMusic.getId(), false);
            }
            playNextMusicTrack(selectedMusic);
            updateAllItemsAppearance();
        }
    }

    private void prevButtonClick()
    {
        if (!musicPlaylist.isEmpty() ) {
            //currentMusicIndex = 0;
            Sound selectedMusic = musicPlaylist.get(currentMusicIndex);
            if (isSoundPlaying(selectedMusic.getId())) {
                // توقف پخش
                audioManager.stopSound(selectedMusic);
                playingStatus.put(selectedMusic.getId(), false);
            }
            playPreviousMusicTrack(selectedMusic);
            updateAllItemsAppearance();
        }
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
        LinearLayout[] tabs = {tabNature, tabMusic, tabNoise, tabWaves, tabStories, tabPresets};
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
                if(sound.isMusicGroup())
                {
                    if(!sound.isSelected()) {
                        if (!audioManager.isSoundDownloaded(sound)) {
                            updateSoundDownloadProgress(sound.getId(), 5);
                            startDownloadWithProgress(sound);
                        } else {
                            sound.setSelected(true);
                            addToPlaylist(sound);
                            updateItemAppearance(sound);
                        }
                    }else {
                        sound.setSelected(false);
                        removeFromPlaylist(sound);
                        //updateMusicPlaylist();
                        updateItemAppearance(sound);

                    }
                    updateAllItemsAppearance();
                }else toggleSoundPlayback(sound);
            }

            @Override
            public void onVolumeChanged(Sound sound, int volume) {
                sound.setVolume(volume);
                if (isSoundPlaying(sound.getId())) {
                    audioManager.updateSoundVolume(sound, volume);
                }
                updateItemAppearance(sound);
            }

            @Override
            public void onSelectionChanged(Sound sound, boolean selected) {
                updateItemAppearance(sound);
            }

            @Override
            public void onDownloadProgress(Sound sound, int progress) {
                // مدیریت پیشرفت دانلود
            }
        }, screenWidth, MainActivity.this);
        soundsRecyclerView.setAdapter(soundAdapter);
    }

    public void updateSoundDownloadProgress(final String soundId, final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int position = -1;
                for (int i = 0; i < filteredSounds.size(); i++) {
                    if (filteredSounds.get(i).getId().equals(soundId)) {
                        position = i;
                        break;
                    }
                }

                if (position != -1) {
                    soundAdapter.notifyItemChanged(position);
                    filteredSounds.get(position).setLastDownloadProgress(progress);
                    Log.d("UI_Update", "Updating UI for: " + soundId + " at position: " + position + " progress: " + progress);
                } else {
                    Log.d("UI_Update", "Sound not found in filtered list: " + soundId);
                }
            }
        });
    }

    public boolean isMixedPlaying(String mixedId) {
        return mixedPlayingStatus.containsKey(mixedId) && mixedPlayingStatus.get(mixedId);
    }

    public Sound findSoundByName(String soundName) {
        for (Sound sound : allSounds) {
            if (sound.getName().equals(soundName)) {
                return sound;
            }
        }
        return null;
    }

    public Sound findSoundById(String soundId) {
        for (Sound sound : allSounds) {
            if (sound.getId().equals(soundId)) {
                return sound;
            }
        }
        return null;
    }

    public void startDownloadWithProgress(Sound sound) {
        sound.setSelected(true);
        updateItemAppearance(sound);
        audioManager.downloadSound(sound, new AudioManager.DownloadCallback() {
            @Override
            public void onDownloadProgress(String soundId, int progress) {
                updateSoundDownloadProgress(soundId, progress);
                Log.d("DownloadProgress", soundId + ": " + progress + "%");
            }

            @Override
            public void onDownloadComplete(String soundId, String localPath) {
                Sound currentSound = soundMap.get(soundId);
                if (currentSound != null) {
                    currentSound.setLocalPath(localPath);
                }
                updateSoundDownloadProgress(soundId, 100);
                showToast(findSoundById(soundId).getName()
                        + " دانلود شد");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateSoundDownloadProgress(soundId, 100);
                        if (currentSound != null) {
                            if(sound.isMusicGroup()) {
                                sound.setSelected(true);
                                addToPlaylist(sound);
                                updateItemAppearance(sound);
                            }else playSoundAfterDownload(currentSound);
                        }
                    }
                }, 500);
            }

            @Override
            public void onDownloadError(String soundId, String error) {
                Sound currentSound = soundMap.get(soundId);
                if (currentSound != null) {
                    currentSound.setSelected(false);
                }
                updateSoundDownloadProgress(soundId, 0);
                showToast("خطا در دانلود " + soundId + ": " + error);
            }
        });
    }

    private void playSoundAfterDownload(Sound sound) {
        sound.setSelected(true);
        audioManager.playSound(sound, sound.getVolume(), new AudioManager.PlaybackCallback() {
            @Override
            public void onPlaybackStarted(String soundId) {
                playingStatus.put(soundId, true);
                updateAllItemsAppearance();
                Log.d("Playback", "Playback started after download: " + soundId);

                // اگر آهنگ music است، ایندکس فعلی را تنظیم کن
                if (sound.isMusicGroup()) {
                    //updateMusicPlaylist();
                    currentMusicIndex = musicPlaylist.indexOf(sound);
                }
            }

            @Override
            public void onPlaybackStopped(String soundId) {
                playingStatus.put(soundId, false);
                updateAllItemsAppearance();
            }

            @Override
            public void onPlaybackError(String soundId, String error) {
                playingStatus.put(soundId, false);
                showToast("خطا در پخش " + soundId + ": " + error);
                updateAllItemsAppearance();
            }
        });
        updateAllItemsAppearance();
    }

    public void toggleSoundPlayback(Sound sound) {
        String soundId = sound.getId();

        if (sound.getAudioUrl() == null || sound.getAudioUrl().isEmpty()) {
            showToast("آهنگ " + soundId + " در دسترس نیست");
            return;
        }

        if (isSoundPlaying(soundId)) {
            // توقف پخش
            audioManager.stopSound(sound);
            playingStatus.put(soundId, false);
            sound.setSelected(false);
            updateAllItemsAppearance();
        } else {
            if (!audioManager.isSoundDownloaded(sound)) {
                updateSoundDownloadProgress(soundId, 5);
                startDownloadWithProgress(sound);
            } else {
                // برای آهنگ‌های music، اگر در حال پخش هستیم، فقط به لیست اضافه کن
                if (sound.isMusicGroup() && getCurrentlyPlayingMusic() != null && !isAutoPlayingNext) {
                    // آهنگ دیگری در حال پخش است، فقط لیست را به‌روزرسانی کن
                    //updateMusicPlaylist();
                    //showToast(sound.getName() + " به لیست پخش اضافه شد");
                } else {
                    // هیچ آهنگی در حال پخش نیست یا این پخش خودکار است، پس پخش کن
                    audioManager.playSound(sound, sound.getVolume(), new AudioManager.PlaybackCallback() {
                        @Override
                        public void onPlaybackStarted(String soundId) {
                            playingStatus.put(soundId, true);
                            sound.setSelected(true);
                            updateAllItemsAppearance();

                            // اگر آهنگ music است، ایندکس فعلی را تنظیم کن
                            if (sound.isMusicGroup()) {
                                //updateMusicPlaylist();
                                currentMusicIndex = musicPlaylist.indexOf(sound);
                            }
                        }

                        @Override
                        public void onPlaybackStopped(String soundId) {
                            playingStatus.put(soundId, false);
                            updateAllItemsAppearance();
                        }

                        @Override
                        public void onPlaybackError(String soundId, String error) {
                            playingStatus.put(soundId, false);
                            showToast("خطا در پخش " + soundId + ": " + error);
                            updateAllItemsAppearance();
                        }
                    });
                }
            }
        }
    }

    public boolean isSoundPlaying(String soundId) {
        return playingStatus.containsKey(soundId) && playingStatus.get(soundId);
    }

    // متد برای پخش آهنگ music بعدی
    public void playNextMusicTrack(Sound currentSound) {
        isAutoPlayingNext = true;

        try {
            //updateMusicPlaylist();

            if (musicPlaylist.isEmpty()) {
                currentMusicIndex = -1;
                return;
            }

            // پیدا کردن ایندکس آهنگ فعلی
            int currentIndex = musicPlaylist.indexOf(currentSound);

            if (currentIndex == -1) {
                // اگر آهنگ فعلی در لیست نیست، از اول شروع کن
                currentMusicIndex = 0;
            } else {
                // آهنگ بعدی را پیدا کن
                currentMusicIndex = (currentIndex + 1) % musicPlaylist.size();
            }

            // اگر آهنگ بعدی همان آهنگ فعلی است، متوقف کن (لیست فقط یک آهنگ دارد)
            //if (musicPlaylist.size() == 1) {
            //    audioManager.stopSound(currentSound);
            //    return;
            //}

            // آهنگ بعدی را پخش کن
            Sound nextSound = musicPlaylist.get(currentMusicIndex);
            if (!isSoundPlaying(nextSound.getId())) {
                toggleSoundPlayback(nextSound);
            }
        } finally {
            isAutoPlayingNext = false;
        }
    }

    // متد برای پخش آهنگ music بعدی
    public void playPreviousMusicTrack(Sound currentSound) {
        isAutoPlayingNext = true;

        try {

            if (musicPlaylist.isEmpty()) {
                currentMusicIndex = -1;
                return;
            }

            // پیدا کردن ایندکس آهنگ فعلی
            int currentIndex = musicPlaylist.indexOf(currentSound);

            if (currentIndex == -1) {
                // اگر آهنگ فعلی در لیست نیست، از اول شروع کن
                currentMusicIndex = 0;
            } else {
                // آهنگ بعدی را پیدا کن
                currentMusicIndex = (currentIndex - 1) % musicPlaylist.size();
            }

            // آهنگ بعدی را پخش کن
            Sound nextSound = musicPlaylist.get(currentMusicIndex);
            if (!isSoundPlaying(nextSound.getId())) {
                toggleSoundPlayback(nextSound);
            }
        } finally {
            isAutoPlayingNext = false;
        }
    }

    // متد برای به‌روزرسانی لیست پخش music
    private void updateMusicPlaylist() {
        //.clear();

        // فقط آهنگ‌های music که انتخاب شده‌اند و دانلود شده‌اند را اضافه کن
        for (Sound sound : allSounds) {
            if (sound.isMusicGroup() && sound.isSelected() && audioManager.isSoundDownloaded(sound)) {
                if(!musicPlaylist.contains(sound)) musicPlaylist.add(sound);
            }
        }

        for(int i=musicPlaylist.size()-1;i>=0;i--)
        {
            Sound sound=musicPlaylist.get(i);
            if(sound.isMusicGroup() && sound.isSelected() && audioManager.isSoundDownloaded(sound))
            {
                //noting
            }else {
                musicPlaylist.remove(i);
            }

        }

        Log.d("MusicPlaylist", "Updated playlist: " + musicPlaylist.size() + " songs");
        for (Sound sound : musicPlaylist) {
            Log.d("MusicPlaylist", "- " + sound.getName());
        }
    }

    // اضافه کردن به لیست پخش
    public void addToPlaylist(Sound sound) {
        if (!sound.isMusicGroup()) return;
        if (!audioManager.isSoundDownloaded(sound)) return;
        if (musicPlaylist.contains(sound)) return; // جلوگیری از اضافه کردن تکراری
        musicPlaylist.add(sound);

        if (musicPlaylist.size()==1 && !isSoundPlaying(sound.getId())) {
            if (musicPlaylist.isEmpty() || getCurrentlyPlayingMusic() == null) {
                toggleSoundPlayback(sound);
            }
        }

    }

    // حذف از لیست پخش
    public void removeFromPlaylist(Sound sound) {
        if (!sound.isMusicGroup()) return;
        if (isSoundPlaying(sound.getId())) {
            audioManager.stopSound(sound);
            playingStatus.put(sound.getId(), false);
            if(musicPlaylist.size()>1) playNextMusicTrack(sound);
        }
        musicPlaylist.remove(sound);

    }

    // متد برای پیدا کردن آهنگ music در حال پخش
    private Sound getCurrentlyPlayingMusic() {
        for (Sound sound : musicPlaylist) {
            if (isSoundPlaying(sound.getId())) {
                return sound;
            }
        }
        return null;
    }

    // متد برای بررسی و مدیریت زمانی که آهنگ آنسلکت می‌شود
    private void onMusicDeselected(Sound deselectedSound) {
        if (!deselectedSound.isMusicGroup()) return;

        // اگر آهنگی که آنسلکت شده در حال پخش است، آن را متوقف کن و آهنگ بعدی را پخش کن
        if (isSoundPlaying(deselectedSound.getId())) {
            audioManager.stopSound(deselectedSound);

            // لیست پخش را به‌روزرسانی کن
            //updateMusicPlaylist();

            if (!musicPlaylist.isEmpty()) {
                // آهنگ بعدی را پخش کن
                currentMusicIndex = 0;
                Sound nextSound = musicPlaylist.get(currentMusicIndex);
                toggleSoundPlayback(nextSound);
            } else {
                currentMusicIndex = -1;
            }
        } else {
            // فقط لیست پخش را به‌روزرسانی کن
            //updateMusicPlaylist();
        }
    }

    // دریافت لیست آهنگ‌های music انتخاب شده
    private List<Sound> getSelectedMusicSounds() {
        List<Sound> selectedMusic = new ArrayList<>();
        for (Sound sound : filteredSounds) {
            if (sound.isMusicGroup() && sound.isSelected() && audioManager.isSoundDownloaded(sound)) {
                selectedMusic.add(sound);
            }
        }
        return selectedMusic;
    }



    private void playAllSounds() {
        // اول همه صداهای غیر music را پخش کن
        for (Sound sound : allSounds) {
            if (sound.isSelected() && !isSoundPlaying(sound.getId()) && !sound.isMusicGroup()) {
                toggleSoundPlayback(sound);
            }
        }

        // سپس اولین آهنگ music را پخش کن (اگر هیچ آهنگی در حال پخش نیست)
        //updateMusicPlaylist();
        if (!musicPlaylist.isEmpty() && getCurrentlyPlayingMusic() == null) {
            //currentMusicIndex = 0;
            Sound selectedMusic = musicPlaylist.get(currentMusicIndex);
            if (!isSoundPlaying(selectedMusic.getId())) {
                toggleSoundPlayback(selectedMusic);
            }
        }
        updatePlayPauseAppearance();
    }

    @SuppressLint("SuspiciousIndentation")
    private void stopAllSounds() {
        audioManager.stopAllSounds();
        for (Sound sound:allSounds) {
            if(sound.isSelected())
            playingStatus.put(sound.getId(), false);
        }
        //playingStatus.clear();
        //musicPlaylist.clear();
        //currentMusicIndex = -1;
        //isAutoPlayingNext = false;

        for (Sound sound : allSounds) {
            updateItemAppearance(sound);
        }

        if (selectedTimer != -1) {

            if(countDownTimer != null){
                countDownTimer.cancel();
            }
        }
        timerDisplay.setText("تایمر فعال: ندارد");
        selectedTimer = -1;
        updateTimerButtonsAppearance(null);
        updatePlayPauseAppearance();
        //showToast("همه صداها متوقف شدند");
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

    public void updateAllItemsAppearance() {
        if (soundAdapter != null) {
            soundAdapter.notifyDataSetChanged();
        }
        if(mixedAdapter !=null)
        {
            mixedAdapter.notifyDataSetChanged();
        }
        updatePlayPauseAppearance();
    }

    public void updateItemAppearance(Sound sound) {

        int position = -1;
        for (int i = 0; i < filteredSounds.size(); i++) {
            if (filteredSounds.get(i).getId().equals(sound.getId())) {
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
            timerButton.setPadding(0, 0, 0, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
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

    CountDownTimer countDownTimer = null;

    private void setTimer(int minutes) {
        timerDisplay.setText("تایمر فعال: " + minutes + " دقیقه");
        if(countDownTimer != null){
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(minutes * 60 * 1000, 1000) {
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
        allSounds.add(new Sound("nature", "پرنده", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/bird.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "پرندگان", "https://img.icons8.com/ios-filled/50/FFFFFF/hummingbird.png", baseUrl + "nature/birds.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "گربه خرخر", "https://img.icons8.com/ios-filled/50/FFFFFF/cat.png", baseUrl + "nature/cat_purring.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جیرجیرک", "https://img.icons8.com/ios-filled/50/FFFFFF/cricket.png", baseUrl + "nature/cricket.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "چکه آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "nature/dripping.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "آتش هیزم", "https://img.icons8.com/ios-filled/50/FFFFFF/campfire.png", baseUrl + "nature/firewood.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جنگل", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png", baseUrl + "nature/forest.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "قورباغه", "https://img.icons8.com/ios-filled/50/FFFFFF/frog.png", baseUrl + "nature/frog.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "چمنزار", "https://img.icons8.com/ios-filled/50/FFFFFF/grass.png", baseUrl + "nature/grassland.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران شدید", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/heavy_rain.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "پرنده لون", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/loon.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جغد", "https://img.icons8.com/ios-filled/50/FFFFFF/owl.png", baseUrl + "nature/owl.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران روی سقف", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain_on_roof.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران روی چادر", "https://img.icons8.com/ios-filled/50/FFFFFF/tent.png", baseUrl + "nature/rain_on_tent.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران روی پنجره", "https://img.icons8.com/ios-filled/50/FFFFFF/window.png", baseUrl + "nature/rain_on_window.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "دریا", "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png", baseUrl + "nature/sea.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "مرغ دریایی", "https://img.icons8.com/ios-filled/50/FFFFFF/seagull.png", baseUrl + "nature/seagull.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "برف", "https://img.icons8.com/ios-filled/50/FFFFFF/snow.png", baseUrl + "nature/snow.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "رعد و برق", "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png", baseUrl + "nature/thunder.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "زیر آب", "https://img.icons8.com/ios-filled/50/FFFFFF/submarine.png", baseUrl + "nature/under_water.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جریان آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "nature/water_flow.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "آبشار", "https://img.icons8.com/ios-filled/50/FFFFFF/waterfall.png", baseUrl + "nature/waterfall.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "نهنگ", "https://img.icons8.com/ios-filled/50/FFFFFF/whale.png", baseUrl + "nature/whale.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باد", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png", baseUrl + "nature/wind.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "گرگ", "https://img.icons8.com/ios-filled/50/FFFFFF/wolf.png", baseUrl + "nature/wolf.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "آب روان", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_water_flow.mp3", 50, false, false));
        //new sounds
        allSounds.add(new Sound("nature", "پرنده در باران", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/bird_in_rain.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "پرندگان پاییزی", "https://img.icons8.com/ios-filled/50/FFFFFF/hummingbird.png", baseUrl + "nature/birds_autumn.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "آواز پرندگان", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/birds_singing.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "بلبل", "https://img.icons8.com/ios-filled/50/FFFFFF/bird.png", baseUrl + "nature/bulbul.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جنگل تاریک", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png", baseUrl + "nature/dark_jungle.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "جنگل شب", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png", baseUrl + "nature/jungle_night.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "طبیعت ظهر", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png", baseUrl + "nature/nature_noon.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "طاووس", "https://img.icons8.com/ios-filled/50/FFFFFF/peacock.png", baseUrl + "nature/peacock.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "قطرات باران", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain_drops.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باران روی سقف ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "nature/rain_on_roof2.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "رودخانه", "https://img.icons8.com/ios-filled/50/FFFFFF/river.png", baseUrl + "nature/river.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "دریای آرام", "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png", baseUrl + "nature/sea_calm.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "دریای شب", "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png", baseUrl + "nature/sea_night.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "امواج دریا", "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png", baseUrl + "nature/sea_waves.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "امواج آرام دریا ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png", baseUrl + "nature/sea_waves_calm2.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "امواج دریا با باد ملایم", "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png", baseUrl + "nature/sea_waves_light_wind.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "رعد و برق ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png", baseUrl + "nature/thunder2.mp3", 10, false, false));
        allSounds.add(new Sound("nature", "باد ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/wind.png", baseUrl + "nature/wind2.mp3", 10, false, false));

        // موسیقی
        //dan gibson
        allSounds.add(new Sound("music", "نوازش فرشته ای", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_an_angles_caress.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رهروی در سرزمین خواب", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_drifting_in_dreamland.mp3", 50, false, false));
        allSounds.add(new Sound("music", "نخستین ستاره افق", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_first_star_in_sky.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فرود آرام", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_gentle_descent.mp3", 50, false, false));
        allSounds.add(new Sound("music", "نیمه شب نیلی", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_midnight_blue.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آرام و آسوده", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_safe_and_sound.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سایه روشن های شفق", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_twilight_fades.mp3", 50, false, false));
        allSounds.add(new Sound("music", "جهانی دور", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/dan_gibson/dg_worlds_away.mp3", 50, false, false));
        //kitaro
        allSounds.add(new Sound("music", "کاروانسرا", "https://img.icons8.com/ios-filled/50/FFFFFF/k.png", baseUrl + "music/kitaro/kitaro_caravansary.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آکوا", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "music/kitaro/kitaro_aqua.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عاشقانه", "https://img.icons8.com/ios-filled/50/FFFFFF/k.png", baseUrl + "music/kitaro/kitaro_romance.mp3", 50, false, false));
        allSounds.add(new Sound("music", "افق درخشان", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png", baseUrl + "music/kitaro/kitaro_shimmering_horizon.mp3", 50, false, false));
        allSounds.add(new Sound("music", "روح دریاچه", "https://img.icons8.com/ios-filled/50/FFFFFF/lake.png", baseUrl + "music/kitaro/kitaro_spirit_of_the_west_lake.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فال نیک", "https://img.icons8.com/ios-filled/50/FFFFFF/lucky.png", baseUrl + "music/kitaro/kitaro_auspicious_omen.mp3", 50, false, false));
        allSounds.add(new Sound("music", "کاروان", "https://img.icons8.com/ios-filled/50/FFFFFF/caravan.png", baseUrl + "music/kitaro/kitaro_caravan.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دکتر سان و چینگ لینگ", "https://img.icons8.com/ios-filled/50/FFFFFF/doctor.png", baseUrl + "music/kitaro/kitaro_dr_sun_and_ching_ling.mp3", 50, false, false));
        allSounds.add(new Sound("music", "کوی", "https://img.icons8.com/ios-filled/50/FFFFFF/carp.png", baseUrl + "music/kitaro/kitaro_koi.mp3", 50, false, false));
        allSounds.add(new Sound("music", "زیارت", "https://img.icons8.com/ios-filled/50/FFFFFF/pilgrimage.png", baseUrl + "music/kitaro/kitaro_pilgrimage.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فواره مقدس", "https://img.icons8.com/ios-filled/50/FFFFFF/fountain.png", baseUrl + "music/kitaro/kitaro_sacred_fountain.mp3", 50, false, false));
        allSounds.add(new Sound("music", "جاده ابریشم", "https://img.icons8.com/ios-filled/50/FFFFFF/road.png", baseUrl + "music/kitaro/kitaro_silk_road.mp3", 50, false, false));
        allSounds.add(new Sound("music", "خواهران سونگ", "https://img.icons8.com/ios-filled/50/FFFFFF/sisters.png", baseUrl + "music/kitaro/kitaro_soong_sisters.mp3", 50, false, false));
        //secret garden
        allSounds.add(new Sound("music", "آداجیو", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_adagio.mp3", 50, false, false));
        allSounds.add(new Sound("music", "همیشه آنجا", "https://img.icons8.com/ios-filled/50/FFFFFF/pin.png", baseUrl + "music/secret_garden/sg_always_there.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آپاسیوناتا", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_apassionata.mp3", 50, false, false));
        allSounds.add(new Sound("music", "کانتولونا", "https://img.icons8.com/ios-filled/50/FFFFFF/moon.png", baseUrl + "music/secret_garden/sg_cantoluna.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شانونه", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_chanonne.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویاگیر", "https://img.icons8.com/ios-filled/50/FFFFFF/dreamcatcher.png", baseUrl + "music/secret_garden/sg_dreamcatcher.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویای تو", "https://img.icons8.com/ios-filled/50/FFFFFF/swing.png", baseUrl + "music/secret_garden/sg_dreamed_of_you.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دوئت", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_duo.mp3", 50, false, false));
        allSounds.add(new Sound("music", "یخ‌زده در زمان", "https://img.icons8.com/ios-filled/50/FFFFFF/winter.png", baseUrl + "music/secret_garden/sg_fozen_in_time.mp3", 50, false, false));
        allSounds.add(new Sound("music", "موج‌های سبز", "https://img.icons8.com/ios-filled/50/FFFFFF/deezer.png", baseUrl + "music/secret_garden/sg_greenwaves.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سرود امید", "https://img.icons8.com/ios-filled/50/FFFFFF/lullaby.png", baseUrl + "music/secret_garden/sg_hymn_to_hope.mp3", 50, false, false));
        allSounds.add(new Sound("music", "لوتوس", "https://img.icons8.com/ios-filled/50/FFFFFF/lotus.png", baseUrl + "music/secret_garden/sg_lotus.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب تاریک", "https://img.icons8.com/ios-filled/50/FFFFFF/midnight.png", baseUrl + "music/secret_garden/sg_morketid.mp3", 50, false, false));
        allSounds.add(new Sound("music", "حرکت", "https://img.icons8.com/ios-filled/50/FFFFFF/dancing.png", baseUrl + "music/secret_garden/sg_moving.mp3", 50, false, false));
        allSounds.add(new Sound("music", "قرن جدید", "https://img.icons8.com/ios-filled/50/FFFFFF/clock.png", baseUrl + "music/secret_garden/sg_new_century.mp3", 50, false, false));
        allSounds.add(new Sound("music", "نوكتورن", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_nocturn.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سادگی", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_ode_to_simplicity.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پروانه", "https://img.icons8.com/ios-filled/50/FFFFFF/butterfly.png", baseUrl + "music/secret_garden/sg_papillon.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پاساكالیا", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_passacaglia.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پاستورال", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_pastorale.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب مقدس", "https://img.icons8.com/ios-filled/50/FFFFFF/owl.png", baseUrl + "music/secret_garden/sg_sacred_night.mp3", 50, false, false));
        allSounds.add(new Sound("music", "پناهگاه", "https://img.icons8.com/ios-filled/50/FFFFFF/tent.png", baseUrl + "music/secret_garden/sg_sanctuary.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باغ مخفی", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_secret_garden.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سرناد", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_serenade.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سیگما", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_sigma.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آهنگ خواب", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/secret_garden/sg_sleepsong.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سونا", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/secret_garden/sg_sona.mp3", 50, false, false));
        allSounds.add(new Sound("music", "شب طوفانی", "https://img.icons8.com/ios-filled/50/FFFFFF/storm.png", baseUrl + "music/secret_garden/sg_stormy_night.mp3", 50, false, false));
        allSounds.add(new Sound("music", "رویا", "https://img.icons8.com/ios-filled/50/FFFFFF/unicorn.png", baseUrl + "music/secret_garden/sg_the_dream.mp3", 50, false, false));
        allSounds.add(new Sound("music", "بی‌خیالی", "https://img.icons8.com/ios-filled/50/FFFFFF/easy.png", baseUrl + "music/secret_garden/sg_without_care.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سفر", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/secret_garden/sg_voyage.mp3", 50, false, false));
        //brian crain
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
        //naser cheshmazar
        allSounds.add(new Sound("music", "انتظار", "https://img.icons8.com/ios-filled/50/FFFFFF/hourglass.png", baseUrl + "music/cheshmazar/awaiting.mp3", 50, false, false));
        allSounds.add(new Sound("music", "آزادی", "https://img.icons8.com/ios-filled/50/FFFFFF/freedom.png", baseUrl + "music/cheshmazar/freedom.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عشق پرشور", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/cheshmazar/passion_of_love.mp3", 50, false, false));
        allSounds.add(new Sound("music", "عشق پرشور ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/cheshmazar/passion_of_love_ii.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باران عشق", "https://img.icons8.com/ios-filled/50/FFFFFF/rain.png", baseUrl + "music/cheshmazar/rain_of_love.mp3", 50, false, false));
        allSounds.add(new Sound("music", "خیزش", "https://img.icons8.com/ios-filled/50/FFFFFF/sunrise.png", baseUrl + "music/cheshmazar/rising.mp3", 50, false, false));
        allSounds.add(new Sound("music", "خواب", "https://img.icons8.com/ios-filled/50/FFFFFF/sleep.png", baseUrl + "music/cheshmazar/sleep.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دیدار", "https://img.icons8.com/ios-filled/50/FFFFFF/handshake.png", baseUrl + "music/cheshmazar/visit.mp3", 50, false, false));
        //yanni
        allSounds.add(new Sound("music", "رقص پروانه", "https://img.icons8.com/ios-filled/50/FFFFFF/butterfly.png", baseUrl + "music/yanni/yanni_butterfly_dance.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فلیتسا", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "music/yanni/yanni_felitsa.mp3", 50, false, false));
        allSounds.add(new Sound("music", "در آینه", "https://img.icons8.com/ios-filled/50/FFFFFF/mirror.png", baseUrl + "music/yanni/yanni_in_the_mirror.mp3", 50, false, false));
        allSounds.add(new Sound("music", "فقط یک خاطره", "https://img.icons8.com/ios-filled/50/FFFFFF/alzheimer.png", baseUrl + "music/yanni/yanni_only_a_memory.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سوگندهای مخفی", "https://img.icons8.com/ios-filled/50/FFFFFF/promise.png", baseUrl + "music/yanni/yanni_secret_vows.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دوست قدیمی", "https://img.icons8.com/ios-filled/50/FFFFFF/children.png", baseUrl + "music/yanni/yanni_so_long_my_friend.mp3", 50, false, false));
        allSounds.add(new Sound("music", "به کسی که می‌داند", "https://img.icons8.com/ios-filled/50/FFFFFF/reading.png", baseUrl + "music/yanni/yanni_to_the_one_who_knows.mp3", 50, false, false));
        // موسیقی AI
        allSounds.add(new Sound("music", "نور شفابخش", "https://img.icons8.com/ios-filled/50/FFFFFF/sun.png", baseUrl + "music/ai/ai_healing_light.mp3", 50, false, false));
        allSounds.add(new Sound("music", "امواج شفابخش", "https://img.icons8.com/ios-filled/50/FFFFFF/waves.png", baseUrl + "music/ai/ai_healing_waves.mp3", 50, false, false));
        // موسیقی Chris Anes
        allSounds.add(new Sound("music", "پژواک محیطی زمین", "https://img.icons8.com/ios-filled/50/FFFFFF/globe.png", baseUrl + "music/chris_anes/chris_anes_ambient_echoes_of_the_earth.mp3", 50, false, false));
        allSounds.add(new Sound("music", "سرزمین رویاها", "https://img.icons8.com/ios-filled/50/FFFFFF/dream.png", baseUrl + "music/chris_anes/chris_anes_ambient_land_of_dreams.mp3", 50, false, false));
        // موسیقی David Tolk
        allSounds.add(new Sound("music", "خاطرات", "https://img.icons8.com/ios-filled/50/FFFFFF/memories.png", baseUrl + "music/david_tolk/david_tolk_memories.mp3", 50, false, false));
        allSounds.add(new Sound("music", "دعا", "https://img.icons8.com/ios-filled/50/FFFFFF/pray.png", baseUrl + "music/david_tolk/david_tolk_pray.mp3", 50, false, false));
        // موسیقی Dyathon
        allSounds.add(new Sound("music", "پس از نیمه شب", "https://img.icons8.com/ios-filled/50/FFFFFF/midnight.png", baseUrl + "music/dyathon/dyathon_after_midnight.mp3", 50, false, false));
        allSounds.add(new Sound("music", "همه در دریا", "https://img.icons8.com/ios-filled/50/FFFFFF/sea.png", baseUrl + "music/dyathon/dyathon_all_at_sea.mp3", 50, false, false));
        allSounds.add(new Sound("music", "باغ کلمات", "https://img.icons8.com/ios-filled/50/FFFFFF/garden.png", baseUrl + "music/dyathon/dyathon_the_garden_of_words.mp3", 50, false, false));
        // موسیقی Eamonn Watt
        allSounds.add(new Sound("music", "یک بهار سرد", "https://img.icons8.com/ios-filled/50/FFFFFF/winter.png", baseUrl + "music/eamonn_watt/eamonn_watt_one_cold_spring.mp3", 50, false, false));
        // موسیقی Paul Cardall
        allSounds.add(new Sound("music", "شبی در پاریس", "https://img.icons8.com/ios-filled/50/FFFFFF/eiffel-tower.png", baseUrl + "music/paul_cardall/paul_cardall_an_evening_in_paris.mp3", 50, false, false));
        // موسیقی Peder B. Helland
        allSounds.add(new Sound("music", "گل سرخ من", "https://img.icons8.com/ios-filled/50/FFFFFF/rose.png", baseUrl + "music/peder_b_helland/peder_b_helland_my_rose.mp3", 50, false, false));// موسیقی Other
        // موسیقی Other
        allSounds.add(new Sound("music", "عمق جنگل", "https://img.icons8.com/ios-filled/50/FFFFFF/forest.png", baseUrl + "music/other/other_deep_in_the_forest.mp3", 50, false, false));
        allSounds.add(new Sound("music", "جزر و مد متغیر", "https://img.icons8.com/ios-filled/50/FFFFFF/tides.png", baseUrl + "music/other/other_turning_tides.mp3", 50, false, false));

        // نویز
        allSounds.add(new Sound("noise", "نویز قهوه‌ای", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "noise/brown_noise.mp3", 30, false, false));
        allSounds.add(new Sound("noise", "نویز سفید", "https://img.icons8.com/ios-filled/50/FFFFFF/music.png", baseUrl + "noise/white_noise.mp3", 30, false, false));

// امواج
        allSounds.add(new Sound("wave", "آداجیو آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Adagio_Alpha_105-115Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "آلفا سعادت", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Bliss_107-115Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "امواج آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Brain_Waves.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_107-115Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_127-135Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "تمرکز آلفا ۳", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Focus_97-104Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "آلفا اینرورس", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Innerverse_Reso.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "امواج شفاف", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Lucid_Waves.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "مدیتیشن آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Meditation.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "شب آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Night_106-114Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "مسیر آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/path.png", baseUrl + "wave/Alpha_Path_96-105Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "رفاه آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Prosperity_127-135Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "شانت آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Shaant_74-82Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_54.8-57.3Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_62.5-66Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۳", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_88-94Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس آلفا ۴", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sinus_91-101Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "روح آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Soul_110-117Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "کره آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Sphere_10Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "ترانسند آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Transcend_106-114Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "یونیورسال آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Universal_65-73Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "امواج آلفا ۸۸", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Waves_88-96Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "زون آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Alpha_Zone_93-104Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس بتا", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Beta_Sinus_100-114Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "امواج بتا", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Beta_Waves_110-130Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "چرخش دلتا", "https://img.icons8.com/ios-filled/50/FFFFFF/d.png", baseUrl + "wave/Delta_Revolve_125-128Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "اکریورم", "https://img.icons8.com/ios-filled/50/FFFFFF/beta.png", baseUrl + "wave/Ecriurem_100-108Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "تعادل", "https://img.icons8.com/ios-filled/50/FFFFFF/balance.png", baseUrl + "wave/Equilibrium_96-104Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "فلو آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Flow_Alpha_203-211Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "حافظه گاما", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Memory_Training.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس گاما ۱", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Sinus_100-140Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "سینوس گاما ۲", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Sinus_300-350Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "امواج گاما ۸۶", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Waves_86+89Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "گاما ویلو", "https://img.icons8.com/ios-filled/50/FFFFFF/gamma.png", baseUrl + "wave/Gamma_Willow_29-71Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "مطالعه داخلی", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Inner_Study_110-115Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "زندگی", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Living_150-158Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "لوز آلفا", "https://img.icons8.com/ios-filled/50/FFFFFF/alpha.png", baseUrl + "wave/Luz_Alpha_100-108Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "مانترا آلفا-تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Mantra_Alpha-Theta.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "فولیا تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Theta_Follia_41-45Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "رم تتا", "https://img.icons8.com/ios-filled/50/FFFFFF/t.png", baseUrl + "wave/Theta_Rem_60-66Hz.mp3.mp3", 40, false, false));
        allSounds.add(new Sound("wave", "راهب آب", "https://img.icons8.com/ios-filled/50/FFFFFF/water.png", baseUrl + "wave/Water_Monk.mp3.mp3", 40, false, false));

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

    public void StopAllSoundsAndMixes() {
        if(mixedPlayer!=null){
            mixedPlayer.Dispose();
        }
        stopAllSounds();
        for (Sound _sound : allSounds) {
            _sound.setSelected(false);
        }
        playingStatus.clear();
        musicPlaylist.clear();
        updateAllItemsAppearance();
        mixedPlayingStatus.clear();
        mixedPlaying = false;
        updateAllItemsAppearance();
    }
    boolean mixedPlaying=false;
    MixedPlayer mixedPlayer=null;
    private void setupMixesRecyclerView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        mixesRecyclerView.setLayoutManager(layoutManager);

        mixedAdapter = new MixedAdapter(allMixes, new MixedAdapter.OnMixedClickListener() {
            @Override
            public void onMixedClick(Mixed mixed) {
                List<Mixed.MixedSound> mixedSounds= mixed.getSounds();
                stopAllSounds();
                for (Sound _sound : allSounds) {
                    _sound.setSelected(false);
                }
                playingStatus.clear();
                musicPlaylist.clear();
                updateAllItemsAppearance();
                if(mixedPlaying)
                {
                    boolean returnAfterStop=false;
                    if(isMixedPlaying(mixed.getId()))returnAfterStop=true;
                    mixedPlayingStatus.clear();
                    updateAllItemsAppearance();
                    if(returnAfterStop){
                        mixedPlaying=false;
                        return;
                    }
                }
                for (Mixed.MixedSound mixedSound:mixedSounds) {
                    for (Sound sound : allSounds) {

                        if (sound.getId().equals(mixedSound.getSoundId())) {
                            if(sound!=null) {
                                if (!playingStatus.containsKey(sound.getId()) || !playingStatus.get(sound.getId())) {

                                }else {
                                    stopAllSounds();
                                    playingStatus.clear();
                                    for (Sound _sound : allSounds) {
                                        _sound.setSelected(false);
                                    }
                                    mixedPlayingStatus.clear();
                                    mixedPlaying=false;
                                    updateAllItemsAppearance();
                                    return;
                                }
                            }
                        }
                    }

                }
                if(mixedPlayer!=null){
                    mixedPlayer.Dispose();
                }
                mixedPlayer= new MixedPlayer(MainActivity.this,mixed);
                mixedPlayer.startPlaying();
                mixedPlayingStatus.put(mixed.getId(),true);
                mixedPlaying=true;
                updateAllItemsAppearance();

            }

            @Override
            public void onMixedPlayPause(Mixed mixed) {
                //toggleMixedPlayback(mixed);
            }

            @Override
            public void onMixedDetails(Mixed mixed) {

            }

            @Override
            public void onDownloadProgress(Mixed mixed, int progress) {
                // مدیریت پیشرفت دانلود
            }
        }, screenWidth, this);

        mixesRecyclerView.setAdapter(mixedAdapter);
    }

    private void loadMixes() {
        allMixes=new ArrayList<>();
        String baseUrl = "https://raw.githubusercontent.com/MortezaMaghrebi/sounds/main/";

        // ترکیب‌های طبیعت
        Mixed beachMix = new Mixed("1", "ساحل آرام",
                baseUrl + "covers/beach_sea_coast_sunset.jpg",
                "ترکیبی آرامش‌بخش از صدای دریا و مرغان دریایی");
        beachMix.addSound(new Mixed.MixedSound("دریا", "sea", 70, 0, 1800, true));
        beachMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 40, 30, 300, false));
        beachMix.addSound(new Mixed.MixedSound("باد", "wind", 30, 0, 1800, true));
        beachMix.addSound(new Mixed.MixedSound("رهروی در سرزمین خواب", "dg_drifting_in_dreamland", 45, 0, 1800, false));
        allMixes.add(beachMix);

        Mixed forestMix = new Mixed("2", "جنگل بارانی",
                baseUrl + "covers/forest_trees_green_nature.jpg",
                "تجربه جنگل در یک روز بارانی");
        forestMix.addSound(new Mixed.MixedSound("جنگل", "forest", 60, 0, 1800, true));
        forestMix.addSound(new Mixed.MixedSound("باران", "rain", 50, 0, 1800, true));
        forestMix.addSound(new Mixed.MixedSound("پرنده", "bird", 30, 60, 400, false));
        forestMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 40, 120, 180, false));
        forestMix.addSound(new Mixed.MixedSound("بهار", "brian_spring", 50, 0, 1800, false));
        allMixes.add(forestMix);

        Mixed mountainMix = new Mixed("3", "کوهستان مه‌آلود",
                baseUrl + "covers/mountain_peak_fog.jpg",
                "صدای طبیعت بکر کوهستان");
        mountainMix.addSound(new Mixed.MixedSound("باد", "wind", 45, 0, 1800, true));
        mountainMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 35, 20, 1780, true));
        mountainMix.addSound(new Mixed.MixedSound("پرنده", "bird", 25, 90, 350, false));
        mountainMix.addSound(new Mixed.MixedSound("زمستان", "brian_winter", 50, 0, 1800, false));
        allMixes.add(mountainMix);

        Mixed lakeMix = new Mixed("4", "دریاچه آرام",
                baseUrl + "covers/lake_reflection_water.jpg",
                "انعکاس آرامش در آب‌های دریاچه");
        lakeMix.addSound(new Mixed.MixedSound("آب", "brian_water", 55, 0, 1800, false));
        lakeMix.addSound(new Mixed.MixedSound("قورباغه", "frog", 35, 45, 180, false));
        lakeMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 90, 240, true));
        lakeMix.addSound(new Mixed.MixedSound("آب روان", "dg_water_flow", 30, 0, 300 , false));
        lakeMix.addSound(new Mixed.MixedSound("رقص پروانه", "yanni_butterfly_dance", 45, 0, 1800, false));
        allMixes.add(lakeMix);

        Mixed waterfallMix = new Mixed("5", "آبشار خروشان",
                baseUrl + "covers/waterfall_river_nature.jpg",
                "انرژی بخش و نشاط آور");
        waterfallMix.addSound(new Mixed.MixedSound("آبشار", "waterfall", 65, 0, 1800, true));
        waterfallMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 45, 0, 1800, true));
        waterfallMix.addSound(new Mixed.MixedSound("پرنده", "bird", 30, 45, 200, false));
        waterfallMix.addSound(new Mixed.MixedSound("آزادی", "freedom", 50, 0, 1800, false));
        allMixes.add(waterfallMix);

        // ترکیب‌های داستانی
        Mixed divingStoryMix = new Mixed("6", "ماجرای غواصی",
                baseUrl + "covers/scuba_diving_ocean.jpg",
                "سفر به اعماق اقیانوس");
        divingStoryMix.addSound(new Mixed.MixedSound("زیر آب", "under_water", 60, 0, 1800, true));
        divingStoryMix.addSound(new Mixed.MixedSound("نهنگ", "whale", 35, 45, 120, false));
        divingStoryMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 25, 30, 150, false));
        divingStoryMix.addSound(new Mixed.MixedSound("افق درخشان", "kitaro_shimmering_horizon", 50, 0, 1800, false));
        allMixes.add(divingStoryMix);

        Mixed boatStoryMix = new Mixed("7", "قایق سواری ماجراجویانه",
                baseUrl + "covers/wooden_boat_lake.jpg",
                "ماجرای یک سفر دریایی");
        boatStoryMix.addSound(new Mixed.MixedSound("دریا", "sea", 65, 0, 1800, true));
        boatStoryMix.addSound(new Mixed.MixedSound("باد", "wind", 40, 0, 1800, true));
        boatStoryMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 30, 60, 200, false));
        boatStoryMix.addSound(new Mixed.MixedSound("سفر", "sg_voyage", 50, 0, 1800, false));
        allMixes.add(boatStoryMix);

        Mixed cabinMix = new Mixed("8", "کلبه جنگلی",
                baseUrl + "covers/log_cabin_forest.jpg",
                "پناهگاهی در دل طبیعت");
        cabinMix.addSound(new Mixed.MixedSound("آتش هیزم", "firewood", 50, 0, 1800, true));
        cabinMix.addSound(new Mixed.MixedSound("باران", "rain", 45, 0, 1800, true));
        cabinMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 60, 240, true));
        cabinMix.addSound(new Mixed.MixedSound("پناهگاه", "sg_sanctuary", 50, 0, 1800, false));
        allMixes.add(cabinMix);

        Mixed lanternMix = new Mixed("9", "فانوس جادویی",
                baseUrl + "covers/old_lantern_light.jpg",
                "ماجرای فانوس در شب تاریک");
        lanternMix.addSound(new Mixed.MixedSound("باد", "wind", 45, 0, 1800, true));
        lanternMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 35, 30, 1770, true));
        lanternMix.addSound(new Mixed.MixedSound("جغد", "owl", 30, 150, 200, false));
        lanternMix.addSound(new Mixed.MixedSound("شب تاریک", "sg_morketid", 50, 0, 1800, false));
        allMixes.add(lanternMix);

        Mixed chocolateMix = new Mixed("10", "کارخانه شکلات سازی",
                baseUrl + "covers/chocolate_factory_sweet.jpg",
                "ماجرای شیرین در کارخانه شکلات");
        chocolateMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 50, 0, 1800, true));
        chocolateMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 40, 45, 150, false));
        chocolateMix.addSound(new Mixed.MixedSound("پرنده", "bird", 30, 90, 200, false));
        chocolateMix.addSound(new Mixed.MixedSound("بی‌خیالی", "sg_without_care", 50, 0, 1800, false));
        allMixes.add(chocolateMix);

        Mixed spiderMix = new Mixed("11", "خاله سوسکه",
                baseUrl + "covers/spider_web_dew.jpg",
                "ماجرای خاله سوسکه در خانه قدیمی");
        spiderMix.addSound(new Mixed.MixedSound("باران روی پنجره", "rain_on_window", 50, 0, 1800, true));
        spiderMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 35, 60, 150, false));
        spiderMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 120, 240, true));
        spiderMix.addSound(new Mixed.MixedSound("رویا", "sg_the_dream", 50, 0, 1800, false));
        allMixes.add(spiderMix);

        Mixed arcticMix = new Mixed("12", "ماجرای قطب شمال",
                baseUrl + "covers/arctic_ice_landscape.jpg",
                "سفر به سرزمین یخ‌ها");
        arcticMix.addSound(new Mixed.MixedSound("باد", "wind", 55, 0, 1800, true));
        arcticMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 40, 45, 120, false));
        arcticMix.addSound(new Mixed.MixedSound("برف", "snow", 30, 0, 1800, true));
        arcticMix.addSound(new Mixed.MixedSound("یخ", "brian_ice", 50, 0, 1800, false));
        allMixes.add(arcticMix);

        Mixed goatMix = new Mixed("13", "بزغاله کوچولو",
                baseUrl + "covers/goat_farm_animal.jpg",
                "ماجراهای بزغاله در مزرعه");
        goatMix.addSound(new Mixed.MixedSound("چمنزار", "grassland", 50, 0, 1800, true));
        goatMix.addSound(new Mixed.MixedSound("پرنده", "bird", 35, 90, 200, false));
        goatMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 40, 150, 180, false));
        goatMix.addSound(new Mixed.MixedSound("تابستان", "brian_summer", 50, 0, 1800, false));
        allMixes.add(goatMix);

        // ترکیب‌های مدیتیشن و آرامش
        Mixed meditationMix = new Mixed("14", "مدیتیشن عمیق",
                baseUrl + "covers/yoga_meditation_peace.jpg",
                "مناسب برای تمرین مدیتیشن و یوگا");
        meditationMix.addSound(new Mixed.MixedSound("نویز سفید", "white_noise", 40, 0, 1800, true));
        meditationMix.addSound(new Mixed.MixedSound("آبشار", "waterfall", 35, 10, 1790, true));
        meditationMix.addSound(new Mixed.MixedSound("سرود امید", "sg_hymn_to_hope", 60, 300, 320, false));
        meditationMix.addSound(new Mixed.MixedSound("مدیتیشن", "brian_earth", 50, 0, 1800, false));
        allMixes.add(meditationMix);

        Mixed nightMix = new Mixed("15", "شب آرام",
                baseUrl + "covers/starry_night_sky.jpg",
                "صدای طبیعت در یک شب آرام");
        nightMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 50, 0, 1800, true));
        nightMix.addSound(new Mixed.MixedSound("جغد", "owl", 35, 45, 200, false));
        nightMix.addSound(new Mixed.MixedSound("باد", "wind", 25, 0, 1800, true));
        nightMix.addSound(new Mixed.MixedSound("نیمه شب نیلی", "dg_midnight_blue", 50, 0, 1800, false));
        allMixes.add(nightMix);

        Mixed desertMix = new Mixed("16", "بیابان ستاره‌ها",
                baseUrl + "covers/desert_sand_dunes.jpg",
                "شبی آرام در دل بیابان");
        desertMix.addSound(new Mixed.MixedSound("باد", "wind", 50, 0, 1800, true));
        desertMix.addSound(new Mixed.MixedSound("برف", "snow", 30, 90, 240, true));
        desertMix.addSound(new Mixed.MixedSound("دیدار", "visit", 50, 0, 1800, false));
        allMixes.add(desertMix);

        Mixed zenMix = new Mixed("17", "باغ ذن",
                baseUrl + "covers/zen_garden_calm.jpg",
                "آرامش در باغ ژاپنی");
        zenMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 45, 0, 1800, true));
        zenMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 35, 60, 120, false));
        zenMix.addSound(new Mixed.MixedSound("پرنده", "bird", 25, 90, 200, false));
        zenMix.addSound(new Mixed.MixedSound("لوتوس", "sg_lotus", 50, 0, 1800, false));
        allMixes.add(zenMix);

        Mixed candleMix = new Mixed("18", "نور شمع",
                baseUrl + "covers/candle_light_relax.jpg",
                "آرامش در نور شمع");
        candleMix.addSound(new Mixed.MixedSound("نویز قهوه‌ای", "brown_noise", 40, 0, 1800, true));
        candleMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 30, 45, 150, false));
        candleMix.addSound(new Mixed.MixedSound("خواب", "sleep", 50, 0, 1800, false));
        allMixes.add(candleMix);

        Mixed tropicalMix = new Mixed("19", "ساحل گرمسیری",
                baseUrl + "covers/tropical_beach_palm_trees.jpg",
                "گرمای آفتاب و نسیم دریا");
        tropicalMix.addSound(new Mixed.MixedSound("دریا", "sea", 70, 0, 1800, true));
        tropicalMix.addSound(new Mixed.MixedSound("باد", "wind", 35, 0, 1800, true));
        tropicalMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 40, 30, 300, false));
        tropicalMix.addSound(new Mixed.MixedSound("عشق پرشور", "passion_of_love", 50, 0, 1800, false));
        allMixes.add(tropicalMix);

        Mixed rainforestMix = new Mixed("20", "جنگل بارانی استوایی",
                baseUrl + "covers/rainforest_jungle_plants.jpg",
                "تنوع صوتی جنگل‌های بارانی");
        rainforestMix.addSound(new Mixed.MixedSound("جنگل", "forest", 60, 0, 1800, true));
        rainforestMix.addSound(new Mixed.MixedSound("پرنده", "bird", 35, 45, 180, false));
        rainforestMix.addSound(new Mixed.MixedSound("باران", "rain", 50, 0, 1800, true));
        rainforestMix.addSound(new Mixed.MixedSound("سونا", "sg_sona", 50, 0, 1800, false));
        allMixes.add(rainforestMix);

        mixedAdapter.updateList(allMixes);
    }

    private void filterSoundsByGroup(String group) {
        filteredSounds.clear();
        for (Sound sound : allSounds) {
            if (sound.getGroup().equals(group)) {
                filteredSounds.add(sound);
            }
        }
        soundAdapter.updateList(filteredSounds);



        // لیست پخش را به‌روزرسانی کن وقتی تب تغییر می‌کند
        if ("music".equals(group)) {
            //updateMusicPlaylist();
        }
    }
}