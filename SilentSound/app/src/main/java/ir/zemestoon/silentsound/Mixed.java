package ir.zemestoon.silentsound;

import java.util.ArrayList;
import java.util.List;

public class Mixed {
    private String id;
    private String name;
    private String icon;
    private String description;
    private List<MixedSound> sounds;
    private boolean selected;
    private int totalDuration; // مدت زمان کل به ثانیه

    public Mixed() {
        this.sounds = new ArrayList<>();
    }

    public Mixed(String id, String name, String icon, String description) {
        this();
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
    }

    // کلاس داخلی برای مدیریت هر صدا در ترکیب
    public static class MixedSound {
        private String soundName;
        private String soundId;
        private int volume;
        private int startTime; // زمان شروع به ثانیه
        private int endTime; // زمان پایان به ثانیه
        private boolean loop;

        public MixedSound() {}

        public MixedSound(String soundName, String soundId, int volume, int startTime, int endTime, boolean loop) {
            this.soundName = soundName;
            this.soundId = soundId;
            this.volume = volume;
            this.startTime = startTime;
            this.endTime = endTime;
            this.loop = loop;
        }

        // Getter and Setter methods
        public String getSoundName() { return soundName; }
        public void setSoundName(String soundName) { this.soundName = soundName; }

        public String getSoundId() { return soundId; }
        public void setSoundId(String soundId) { this.soundId = soundId; }

        public int getVolume() { return volume; }
        public void setVolume(int volume) { this.volume = volume; }

        public int getStartTime() { return startTime; }
        public void setStartTime(int startTime) { this.startTime = startTime; }

        public int getEndTime() { return endTime; }
        public void setEndTime(int endTime) { this.endTime = endTime; }

        public boolean isLoop() { return loop; }
        public void setLoop(boolean loop) { this.loop = loop; }
    }

    // Getter and Setter methods
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<MixedSound> getSounds() { return sounds; }
    public void setSounds(List<MixedSound> sounds) { this.sounds = sounds; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public int getTotalDuration() { return totalDuration; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }

    // متدهای کمکی
    public void addSound(MixedSound sound) {
        this.sounds.add(sound);
        updateTotalDuration();
    }

    public void removeSound(MixedSound sound) {
        this.sounds.remove(sound);
        updateTotalDuration();
    }

    private void updateTotalDuration() {
        int maxEndTime = 0;
        for (MixedSound sound : sounds) {
            if (sound.getEndTime() > maxEndTime) {
                maxEndTime = sound.getEndTime();
            }
        }
        this.totalDuration = maxEndTime;
    }

    public String getFormattedDuration() {
        int minutes = totalDuration / 60;
        int seconds = totalDuration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public int getSoundCount() {
        return sounds.size();
    }
}