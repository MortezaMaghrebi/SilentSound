package ir.zemestoon.silentsound;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.logging.LogRecord;

public class MixedPlayer {
    MainActivity mainActivity;
    Mixed mixed;
    Handler handler;
    Runnable runnable;
    List<Mixed.MixedSound> sounds;
    int time=0;
    public MixedPlayer(MainActivity mainActivity,Mixed mixed) {
        this.mainActivity = mainActivity;
        this.mixed = mixed;
        this.sounds = mixed.getSounds();
    }

    public void startPlaying() {
        handler = new Handler(Looper.getMainLooper());
         time=0;
        runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(runnable,1000);
                task1s();
            }
        };
        handler.postDelayed(runnable,1000);

    }

    boolean insideTask=false;
    void task1s()
    {
        time++;
        if(insideTask) return;
        insideTask=true;
        if(!mainActivity.mixedPlayingStatus.containsKey(mixed.getId())) {
            mainActivity.mixedPlayingStatus.put(mixed.getId(), true);
            mainActivity.mixedPlaying = true;
            mainActivity.updateAllItemsAppearance();
        }
        //Task Codes Here
        for (Mixed.MixedSound sound:sounds) {
            if(time>mixed.getTotalDuration()){
                mainActivity.StopAllSoundsAndMixes();
                mainActivity.updateAllItemsAppearance();
            }else if(time>sound.getEndTime() && !sound.isPlaying())
            {
                //noting... sound finished
            }else if(time>sound.getEndTime() && sound.isPlaying())
            {
                //stop sound
                StopMixedSound(sound);
                mainActivity.updateAllItemsAppearance();
            }else if(time>sound.getStartTime()&&!sound.isPlaying())
            {
                //play sound
                PlayMixedSound(sound);
                mainActivity.updateAllItemsAppearance();
            }
        }
        //Finish Task Codes
        insideTask=false;
    }

    void PlayMixedSound(Mixed.MixedSound mixedSound) {
        String soundId = mixedSound.getSoundId();
        Sound sound = mainActivity.findSoundById(soundId);
        if (sound != null) {
            sound.setVolume(mixedSound.getVolume());
            if (!mainActivity.playingStatus.containsKey(sound.getId()) || !mainActivity.playingStatus.get(sound.getId())) {
                if (sound.isMusicGroup()) {
                    if (!mainActivity.audioManager.isSoundDownloaded(sound)) {
                        mainActivity.updateSoundDownloadProgress(sound.getId(), 5);
                        mainActivity.startDownloadWithProgress(sound);
                        sound.setSelected(true);
                    } else {
                        mainActivity.addToPlaylist(sound);
                        mainActivity.updateItemAppearance(sound);
                    }
                } else {
                    mainActivity.toggleSoundPlayback(sound);
                }
                mixedSound.setPlaying(true);
            }
        }
    }

    void StopMixedSound(Mixed.MixedSound mixedSound) {
        Sound sound = mainActivity.findSoundById(mixedSound.getSoundId());
        if (sound != null) {
            if (mainActivity.playingStatus.containsKey(sound.getId()) && mainActivity.playingStatus.get(sound.getId())) {
                if (sound.isMusicGroup()) {
                    sound.setSelected(false);
                    mainActivity.removeFromPlaylist(sound);
                    mainActivity.updateItemAppearance(sound);
                } else {
                    mainActivity.toggleSoundPlayback(sound);
                }
                mixedSound.setPlaying(false);
            }
        }
    }


    public void Dispose()
    {
        try {
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
        }
    }

}
