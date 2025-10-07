package ir.zemestoon.silentsound;

public class Sound {
    private String group;
    private String name;
    private String icon;
    private String audioUrl; // لینک آهنگ در گیت‌هاب
    private int volume;
    private boolean selected;
    private boolean vip;
    private String localPath; // مسیر فایل دانلود شده روی دستگاه
    private int lastDownloadProgress=0;
    public Sound() {
    }

    public Sound(String group, String name, String icon, String audioUrl, int volume, boolean selected, boolean vip) {
        this.group = group;
        this.name = name;
        this.icon = icon;
        this.audioUrl = audioUrl;
        this.volume = volume;
        this.selected = selected;
        this.vip = vip;
    }

    public boolean isLoopingGroup() {
        return "nature".equals(group) || "noise".equals(group) || "wave".equals(group);
    }

    public boolean isMusicGroup() {
        return "music".equals(group);
    }

    public boolean isStoryGroup() {
        return "story".equals(group);
    }

    public boolean isPresetGroup() {
        return "preset".equals(group);
    }

    // Getter and Setter methods
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public String getId(){
        String[] parts= audioUrl.split("/");
        String soundId = parts[parts.length-1];
        //parts = soundId.split(".");
        soundId = soundId.replace(".mp3","");
        return soundId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setLastDownloadProgress(int progress){this.lastDownloadProgress=progress;}

    public int getLastDownloadProgress() {return lastDownloadProgress;}
}