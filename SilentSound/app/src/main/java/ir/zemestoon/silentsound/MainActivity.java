package ir.zemestoon.silentsound;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.media.midi.MidiSender;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    NetController netController;
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
        textView.setTextColor(Color.parseColor("#ffffff"));
        ImageView imageView = (ImageView) selectedTab.getChildAt(0);
        imageView.setColorFilter(Color.parseColor("#ffffff"));

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
            if(currentMusicIndex<0) currentMusicIndex+=musicPlaylist.size();
            if(currentMusicIndex>(musicPlaylist.size()-1))currentMusicIndex=musicPlaylist.size()-1;
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
        String baseSoundUrl = "https://raw.githubusercontent.com/MortezaMaghrebi/sounds/main/";
        String baseIconUrl = "https://img.icons8.com/ios-filled/50/FFFFFF/";

        netController =NetController.getInstance(MainActivity.this);
        try {
            netController.DownloadSoundList();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String soundlist_str=netController.getSoundList();
        if(soundlist_str==null)return;
        String[] soundList = soundlist_str.split("\n");
        for (String soundstr:soundList) {
            String[] items = soundstr.split(",");
            if(items.length==7)
            {
                String group = items[0];
                String name = items[1];
                String icon = items[2];
                String sound = items[3];
                String volume = items[4];
                String selected = items[5];
                String vip = items[6];
                int ivolueme =50;
                try{ivolueme=Integer.parseInt(volume);}catch (Exception e){}
                allSounds.add(new Sound(group,name,baseIconUrl+icon,baseSoundUrl+sound,ivolueme,selected.equals("1"),vip.equals("1")));
            }
        }

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
                if(mixedPlayer!=null) mixedPlayer.Dispose();
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
                for (Mixed.MixedSound mixedSound:mixed.getSounds()) {
                    mixedSound.setPlaying(false);
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

        Mixed arcticMix = new Mixed("1", "ماجرای قطب شمال",
                baseUrl + "covers/arctic_ice_landscape.jpg",
                "سفر به سرزمین یخ‌ها");
        arcticMix.addSound(new Mixed.MixedSound("باد", "wind2", 25, 0, 120, true));
        arcticMix.addSound(new Mixed.MixedSound("نهنگ", "whale", 36, 70, 110, true));
        arcticMix.addSound(new Mixed.MixedSound("پناهگاه", "sg_sanctuary", 70, 0, 900, false)); // Secret Garden
        arcticMix.addSound(new Mixed.MixedSound("نوازش فرشته ای", "dg_an_angles_caress", 80, 60, 900, false)); // Dan Gibson
        arcticMix.addSound(new Mixed.MixedSound("رهروی در سرزمین خواب", "dg_drifting_in_dreamland", 75, 60, 900, false)); // Dan Gibson
        arcticMix.addSound(new Mixed.MixedSound("آکوا", "kitaro_aqua", 70, 90, 900, false)); // Kitaro
        arcticMix.addSound(new Mixed.MixedSound("امواج شفاف", "Alpha_Lucid_Waves", 35, 100, 1800, true));
        arcticMix.addSound(new Mixed.MixedSound("داستان قطب شمال", "story_north_pole", 100, 20, 955, false));
        allMixes.add(arcticMix);

        Mixed singleTreeMix = new Mixed("2", "تک درخت",
                baseUrl + "covers/single_tree_in_bliss.jpg",
                "دویدن در چمنزار");
        singleTreeMix.addSound(new Mixed.MixedSound("داستان چمنزار", "story_grassland", 100, 10, 240, true));
        singleTreeMix.addSound(new Mixed.MixedSound("پرندگان پاییزی", "birds_autumn", 15, 0, 50, true));
        singleTreeMix.addSound(new Mixed.MixedSound("بلبل", "bulbul", 3, 0, 30, true));
        singleTreeMix.addSound(new Mixed.MixedSound("نیمه شب نیلی", "dg_midnight_blue", 100, 240, 900, false)); // Dan Gibson
        singleTreeMix.addSound(new Mixed.MixedSound("آداجیو", "sg_adagio", 35, 250, 900, false)); // Secret Garden
        singleTreeMix.addSound(new Mixed.MixedSound("آکوا", "kitaro_aqua", 30, 270, 900, false)); // Kitaro
        singleTreeMix.addSound(new Mixed.MixedSound("پاییز", "brian_autumn", 30, 290, 900, false)); // Brian Crain
        singleTreeMix.addSound(new Mixed.MixedSound("تمرکز آلفا ۱", "Alpha_Focus_107-115Hz", 15, 0, 1200, true));
        allMixes.add(singleTreeMix);

        // ترکیب‌های طبیعت
        Mixed beachMix = new Mixed("3", "ساحل آرام",
                baseUrl + "covers/beach_sea_coast_sunset.jpg",
                "ترکیبی آرامش‌بخش از صدای دریا و مرغان دریایی");
        beachMix.addSound(new Mixed.MixedSound("دریای آرام", "sea_calm", 30, 0, 180, true));
        beachMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 10, 30, 60, false));
        beachMix.addSound(new Mixed.MixedSound("باد", "wind", 10, 20, 40, true));
        beachMix.addSound(new Mixed.MixedSound("جاده ابریشم", "kitaro_silk_road", 100, 5, 1800, false)); // Secret Garden
        beachMix.addSound(new Mixed.MixedSound("کاروانسرا", "kitaro_caravansary", 100, 30, 1800, false)); // Kitaro
        beachMix.addSound(new Mixed.MixedSound("باغ مخفی", "sg_secret_garden", 100, 50, 1800, false)); // Secret Garden
        beachMix.addSound(new Mixed.MixedSound("امواج شفاف", "Alpha_Lucid_Waves", 7, 0, 180, true));
        allMixes.add(beachMix);

        Mixed darkSeaMix = new Mixed("4", "دریای تاریک و طوفانی",
                baseUrl + "covers/dark_thunder_sea.jpg",
                "ترکیبی از دریای طوفانی، رعد و برق و موسیقی کاروان");
        darkSeaMix.addSound(new Mixed.MixedSound("زیر آب", "under_water", 40, 0, 10, true));
        darkSeaMix.addSound(new Mixed.MixedSound("نهنگ", "whale", 100, 0, 30, true));
        darkSeaMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 80, 5, 10, false));
        darkSeaMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 70, 10, 30, false));
        darkSeaMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 80, 50, 70, false));
        darkSeaMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 70, 120, 140, false));
        darkSeaMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 70, 190, 240, false));
        darkSeaMix.addSound(new Mixed.MixedSound("دریای شب", "sea_night", 70, 30, 60, true));
        darkSeaMix.addSound(new Mixed.MixedSound("کاروان", "kitaro_caravan", 100, 10, 1800, false));
        darkSeaMix.addSound(new Mixed.MixedSound("شب طوفانی", "sg_stormy_night", 90, 40, 1800, false));
        darkSeaMix.addSound(new Mixed.MixedSound("امواج شفاف", "Alpha_Lucid_Waves", 25, 0, 1800, true));
       allMixes.add(darkSeaMix);

        Mixed forestMix = new Mixed("5", "جنگل بارانی",
                baseUrl + "covers/forest_trees_green_nature.jpg",
                "تجربه جنگل در یک روز بارانی با امواج و موسیقی آرامش‌بخش");
        // صداهای طبیعت
        //forestMix.addSound(new Mixed.MixedSound("پرنده لون", "loon", 10, 0, 13, true));
        forestMix.addSound(new Mixed.MixedSound("جنگل", "forest", 5, 0, 30, true));
        forestMix.addSound(new Mixed.MixedSound("باران", "rain", 15, 0, 1800, true));
        forestMix.addSound(new Mixed.MixedSound("پرنده", "bird", 10, 60, 400, false));
        forestMix.addSound(new Mixed.MixedSound("پرنده ها", "birds", 10, 40, 100, false));
        forestMix.addSound(new Mixed.MixedSound("رعد و برق", "thunder", 15, 120, 180, false));
// موسیقی‌های آرامش‌بخش
        forestMix.addSound(new Mixed.MixedSound("بهار", "brian_spring", 100, 20, 1800, false));
        forestMix.addSound(new Mixed.MixedSound("رهروی در سرزمین خواب", "dg_drifting_in_dreamland", 100, 40, 900, false));
        forestMix.addSound(new Mixed.MixedSound("شب طوفانی", "sg_stormy_night", 60, 50, 1800, false));
        forestMix.addSound(new Mixed.MixedSound("نوازش فرشته‌ای", "dg_an_angles_caress", 70, 60, 600, false));
        // انواع امواج موجود
        forestMix.addSound(new Mixed.MixedSound("آلفا شفاف", "Alpha_Lucid_Waves", 15, 0, 1800, true));
        forestMix.addSound(new Mixed.MixedSound("آلفا مدیتیشن", "Alpha_Meditation", 15, 60, 1800, true));
        forestMix.addSound(new Mixed.MixedSound("مانترا آلفا-تتا", "Mantra_Alpha-Theta", 15, 120, 1800, true));
        allMixes.add(forestMix);

        Mixed mountainMix = new Mixed("6", "کوهستان مه‌آلود",
                baseUrl + "covers/mountain_peak_fog.jpg",
                "صدای طبیعت بکر کوهستان");
        mountainMix.addSound(new Mixed.MixedSound("باد", "wind", 45, 0, 1800, true));
        mountainMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 35, 20, 1780, true));
        mountainMix.addSound(new Mixed.MixedSound("پرنده", "bird", 25, 90, 350, false));
        mountainMix.addSound(new Mixed.MixedSound("زمستان", "brian_winter", 50, 0, 1800, false));
        allMixes.add(mountainMix);

        Mixed lakeMix = new Mixed("7", "دریاچه آرام",
                baseUrl + "covers/lake_reflection_water.jpg",
                "انعکاس آرامش در آب‌های دریاچه");
        lakeMix.addSound(new Mixed.MixedSound("آب", "brian_water", 55, 0, 1800, false));
        lakeMix.addSound(new Mixed.MixedSound("قورباغه", "frog", 35, 45, 180, false));
        lakeMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 90, 240, true));
        lakeMix.addSound(new Mixed.MixedSound("آب روان", "dg_water_flow", 30, 0, 300 , false));
        lakeMix.addSound(new Mixed.MixedSound("رقص پروانه", "yanni_butterfly_dance", 45, 0, 1800, false));
        allMixes.add(lakeMix);

        Mixed waterfallMix = new Mixed("8", "آبشار خروشان",
                baseUrl + "covers/waterfall_river_nature.jpg",
                "انرژی بخش و نشاط آور");
        waterfallMix.addSound(new Mixed.MixedSound("آبشار", "waterfall", 65, 0, 1800, true));
        waterfallMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 45, 0, 1800, true));
        waterfallMix.addSound(new Mixed.MixedSound("پرنده", "bird", 30, 45, 200, false));
        waterfallMix.addSound(new Mixed.MixedSound("آزادی", "freedom", 50, 0, 1800, false));
        allMixes.add(waterfallMix);

        // ترکیب‌های داستانی
        Mixed divingStoryMix = new Mixed("9", "ماجرای غواصی",
                baseUrl + "covers/scuba_diving_ocean.jpg",
                "سفر به اعماق اقیانوس");
        divingStoryMix.addSound(new Mixed.MixedSound("زیر آب", "under_water", 60, 0, 1800, true));
        divingStoryMix.addSound(new Mixed.MixedSound("نهنگ", "whale", 35, 45, 120, false));
        divingStoryMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 25, 30, 150, false));
        divingStoryMix.addSound(new Mixed.MixedSound("افق درخشان", "kitaro_shimmering_horizon", 50, 0, 1800, false));
        allMixes.add(divingStoryMix);

        Mixed boatStoryMix = new Mixed("10", "قایق سواری آرام",
                baseUrl + "covers/wooden_boat_lake.jpg",
                "");
        boatStoryMix.addSound(new Mixed.MixedSound("پارو زدن", "oar", 8, 60, 80, true));
        boatStoryMix.addSound(new Mixed.MixedSound("باد ملایم", "gentle_wind", 25, 0, 1800, true));
        boatStoryMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 10, 60, 200, false));
        boatStoryMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 10, 0, 15, false));
        boatStoryMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 15, 40, 55, false));
        boatStoryMix.addSound(new Mixed.MixedSound("به کسی که می داند", "yanni_to_the_one_who_knows", 90, 0, 1800, false));
        boatStoryMix.addSound(new Mixed.MixedSound("همه در دریا", "dyathon_all_at_sea", 100, 40, 1800, true));
        boatStoryMix.addSound(new Mixed.MixedSound("نوازش فرشته‌ای", "dg_an_angles_caress", 100, 60, 600, false)); // Dan Gibson
        boatStoryMix.addSound(new Mixed.MixedSound("رهروی در سرزمین خواب", "dg_drifting_in_dreamland", 100, 80, 900, false)); // Dan Gibson
        boatStoryMix.addSound(new Mixed.MixedSound("زندگی", "Living_150-158Hz", 15, 0, 180, true));
        allMixes.add(boatStoryMix);

        Mixed cabinMix = new Mixed("11", "کلبه جنگلی",
                baseUrl + "covers/log_cabin_forest.jpg",
                "پناهگاهی در دل طبیعت");
        cabinMix.addSound(new Mixed.MixedSound("آتش هیزم", "firewood", 50, 0, 1800, true));
        cabinMix.addSound(new Mixed.MixedSound("باران", "rain", 45, 0, 1800, true));
        cabinMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 60, 240, true));
        cabinMix.addSound(new Mixed.MixedSound("پناهگاه", "sg_sanctuary", 50, 0, 1800, false));
        allMixes.add(cabinMix);

        Mixed lanternMix = new Mixed("12", "فانوس جادویی",
                baseUrl + "covers/old_lantern_light.jpg",
                "ماجرای فانوس در شب تاریک");
        lanternMix.addSound(new Mixed.MixedSound("باد", "wind", 45, 0, 1800, true));
        lanternMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 35, 30, 1770, true));
        lanternMix.addSound(new Mixed.MixedSound("جغد", "owl", 30, 150, 200, false));
        lanternMix.addSound(new Mixed.MixedSound("شب تاریک", "sg_morketid", 50, 0, 1800, false));
        allMixes.add(lanternMix);

        Mixed chocolateMix = new Mixed("13", "کارخانه شکلات سازی",
                baseUrl + "covers/chocolate_factory_sweet.jpg",
                "ماجرای شیرین در کارخانه شکلات");
        chocolateMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 50, 0, 1800, true));
        chocolateMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 40, 45, 150, false));
        chocolateMix.addSound(new Mixed.MixedSound("پرنده", "bird", 30, 90, 200, false));
        chocolateMix.addSound(new Mixed.MixedSound("بی‌خیالی", "sg_without_care", 50, 0, 1800, false));
        allMixes.add(chocolateMix);

        Mixed spiderMix = new Mixed("14", "خاله سوسکه",
                baseUrl + "covers/spider_web_dew.jpg",
                "ماجرای خاله سوسکه در خانه قدیمی");
        spiderMix.addSound(new Mixed.MixedSound("باران روی پنجره", "rain_on_window", 50, 0, 1800, true));
        spiderMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 35, 60, 150, false));
        spiderMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 30, 120, 240, true));
        spiderMix.addSound(new Mixed.MixedSound("رویا", "sg_the_dream", 50, 0, 1800, false));
        allMixes.add(spiderMix);



        Mixed goatMix = new Mixed("15", "بزغاله کوچولو",
                baseUrl + "covers/goat_farm_animal.jpg",
                "ماجراهای بزغاله در مزرعه");
        goatMix.addSound(new Mixed.MixedSound("چمنزار", "grassland", 50, 0, 1800, true));
        goatMix.addSound(new Mixed.MixedSound("پرنده", "bird", 35, 90, 200, false));
        goatMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 40, 150, 180, false));
        goatMix.addSound(new Mixed.MixedSound("تابستان", "brian_summer", 50, 0, 1800, false));
        allMixes.add(goatMix);

        // ترکیب‌های مدیتیشن و آرامش
        Mixed meditationMix = new Mixed("16", "مدیتیشن عمیق",
                baseUrl + "covers/yoga_meditation_peace.jpg",
                "مناسب برای تمرین مدیتیشن و یوگا");
        meditationMix.addSound(new Mixed.MixedSound("نویز سفید", "white_noise", 40, 0, 1800, true));
        meditationMix.addSound(new Mixed.MixedSound("آبشار", "waterfall", 35, 10, 1790, true));
        meditationMix.addSound(new Mixed.MixedSound("سرود امید", "sg_hymn_to_hope", 60, 300, 320, false));
        meditationMix.addSound(new Mixed.MixedSound("مدیتیشن", "brian_earth", 50, 0, 1800, false));
        allMixes.add(meditationMix);

        Mixed nightMix = new Mixed("17", "شب آرام",
                baseUrl + "covers/starry_night_sky.jpg",
                "صدای طبیعت در یک شب آرام");
        nightMix.addSound(new Mixed.MixedSound("جیرجیرک", "cricket", 50, 0, 1800, true));
        nightMix.addSound(new Mixed.MixedSound("جغد", "owl", 35, 45, 200, false));
        nightMix.addSound(new Mixed.MixedSound("باد", "wind", 25, 0, 1800, true));
        nightMix.addSound(new Mixed.MixedSound("نیمه شب نیلی", "dg_midnight_blue", 50, 0, 1800, false));
        allMixes.add(nightMix);

        Mixed desertMix = new Mixed("18", "بیابان ستاره‌ها",
                baseUrl + "covers/desert_sand_dunes.jpg",
                "شبی آرام در دل بیابان");
        desertMix.addSound(new Mixed.MixedSound("باد", "wind", 50, 0, 1800, true));
        desertMix.addSound(new Mixed.MixedSound("برف", "snow", 30, 90, 240, true));
        desertMix.addSound(new Mixed.MixedSound("دیدار", "visit", 50, 0, 1800, false));
        allMixes.add(desertMix);

        Mixed zenMix = new Mixed("19", "باغ ذن",
                baseUrl + "covers/zen_garden_calm.jpg",
                "آرامش در باغ ژاپنی");
        zenMix.addSound(new Mixed.MixedSound("جریان آب", "water_flow", 45, 0, 1800, true));
        zenMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 35, 60, 120, false));
        zenMix.addSound(new Mixed.MixedSound("پرنده", "bird", 25, 90, 200, false));
        zenMix.addSound(new Mixed.MixedSound("لوتوس", "sg_lotus", 50, 0, 1800, false));
        allMixes.add(zenMix);

        Mixed candleMix = new Mixed("20", "نور شمع",
                baseUrl + "covers/candle_light_relax.jpg",
                "آرامش در نور شمع");
        candleMix.addSound(new Mixed.MixedSound("نویز قهوه‌ای", "brown_noise", 40, 0, 1800, true));
        candleMix.addSound(new Mixed.MixedSound("چکه آب", "dripping", 30, 45, 150, false));
        candleMix.addSound(new Mixed.MixedSound("خواب", "sleep", 50, 0, 1800, false));
        allMixes.add(candleMix);

        Mixed tropicalMix = new Mixed("21", "ساحل گرمسیری",
                baseUrl + "covers/tropical_beach_palm_trees.jpg",
                "گرمای آفتاب و نسیم دریا");
        tropicalMix.addSound(new Mixed.MixedSound("دریا", "sea", 70, 0, 1800, true));
        tropicalMix.addSound(new Mixed.MixedSound("باد", "wind", 35, 0, 1800, true));
        tropicalMix.addSound(new Mixed.MixedSound("مرغ دریایی", "seagull", 40, 30, 300, false));
        tropicalMix.addSound(new Mixed.MixedSound("عشق پرشور", "passion_of_love", 50, 0, 1800, false));
        allMixes.add(tropicalMix);

        Mixed rainforestMix = new Mixed("22", "جنگل بارانی استوایی",
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