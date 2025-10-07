package ir.zemestoon.silentsound;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SoundAdapter extends RecyclerView.Adapter<SoundAdapter.SoundViewHolder> {

    private List<Sound> soundList;
    private OnSoundClickListener listener;
    private int itemWidth;
    private MainActivity mainActivity;
    private AudioManager audioManager;

    public interface OnSoundClickListener {
        void onSoundClick(Sound sound);
        void onVolumeChanged(Sound sound, int volume);
        void onSelectionChanged(Sound sound, boolean selected);
        void onDownloadProgress(Sound sound, int progress); // اضافه شده
    }

    public SoundAdapter(List<Sound> soundList, OnSoundClickListener listener, int screenWidth, MainActivity mainActivity) {
        this.soundList = soundList;
        this.listener = listener;
        this.itemWidth = (screenWidth - convertDpToPx(60) - convertDpToPx(20)) / 3;
        this.mainActivity = mainActivity;
        this.audioManager = AudioManager.getInstance(mainActivity);
    }

    private boolean isSoundPlaying(String soundId) {
        return mainActivity != null && mainActivity.isSoundPlaying(soundId);
    }
    private int convertDpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
    @NonNull
    @Override
    public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sound, parent, false);
        return new SoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
        Sound sound = soundList.get(position);
        holder.bind(sound);
    }

    @Override
    public int getItemCount() {
        return soundList.size();
    }

    public void updateList(List<Sound> newList) {
        soundList = newList;
        notifyDataSetChanged();
    }

    class SoundViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private SeekBar volumeSeekBar;
        private View selectionOverlay;
        private View progressBackground;
        private TextView downloadProgressText;

        public SoundViewHolder(@NonNull View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.width = itemWidth;
            itemView.setLayoutParams(layoutParams);

            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            volumeSeekBar = itemView.findViewById(R.id.volumeSeekBar);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            progressBackground = itemView.findViewById(R.id.progressBackground);
            downloadProgressText = itemView.findViewById(R.id.downloadProgressText);

            // کلیک روی کل آیتم
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSoundClick(soundList.get(getAdapterPosition()));
                }
            });

            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && listener != null) {
                        Sound sound = soundList.get(getAdapterPosition());
                        sound.setVolume(progress);
                        listener.onVolumeChanged(sound, progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        public void bind(Sound sound) {
            nameTextView.setText(sound.getName());
            volumeSeekBar.setProgress(sound.getVolume());

            // بررسی وضعیت دانلود - بهبود یافته
            boolean isDownloaded = audioManager.isSoundDownloaded(sound);
            int downloadProgress = audioManager.getDownloadProgress(sound);

            Log.d("SoundAdapter", "Binding: " + sound.getName() +
                    ", Downloaded: " + isDownloaded +
                    ", Progress: " + downloadProgress);

            // مدیریت نمایش progress
            if (!isDownloaded && downloadProgress > 0) {
                progressBackground.setVisibility(View.VISIBLE);
                // تنظیم level برای ClipDrawable (0-10000)
                Drawable backgroundDrawable = progressBackground.getBackground();
                if (backgroundDrawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) backgroundDrawable;
                    Drawable clipDrawable = layerDrawable.getDrawable(1); // آیتم دوم (پیشرفت آبی)
                    if (clipDrawable instanceof ClipDrawable) {
                        // ClipDrawable از 0 (کاملاً پنهان) تا 10000 (کاملاً قابل مشاهده) کار می‌کند
                        int level = downloadProgress * 100;
                        clipDrawable.setLevel(level);
                    }
                }

                // نمایش درصد
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressText.setText(downloadProgress + "%");

                itemView.setEnabled(false);
                itemView.setAlpha(0.9f);

            } else if (!isDownloaded && downloadProgress == 0) {
                progressBackground.setVisibility(View.VISIBLE);
                // آماده برای دانلود - پنهان کردن progress
                Drawable backgroundDrawable = progressBackground.getBackground();
                if (backgroundDrawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) backgroundDrawable;
                    Drawable clipDrawable = layerDrawable.getDrawable(1);
                    if (clipDrawable instanceof ClipDrawable) {
                        clipDrawable.setLevel(0); // کاملاً پنهان
                    }
                }

                downloadProgressText.setVisibility(View.GONE);
                itemView.setEnabled(true);
                itemView.setAlpha(1.0f);

            } else {
                // دانلود کامل شده - پنهان کردن progress
                progressBackground.setVisibility(View.INVISIBLE);
                Drawable backgroundDrawable = progressBackground.getBackground();
                if (backgroundDrawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) backgroundDrawable;
                    Drawable clipDrawable = layerDrawable.getDrawable(1);
                    if (clipDrawable instanceof ClipDrawable) {
                        clipDrawable.setLevel(0); // کاملاً پنهان
                    }
                }

                downloadProgressText.setVisibility(View.GONE);
                itemView.setEnabled(true);
                itemView.setAlpha(1.0f);
            }


            // **مدیریت ظاهر بر اساس وضعیت پخش و انتخاب - اصلاح شده**
            if (isSoundPlaying(sound.getId())) {
                // اگر در حال پخش است - حاشیه آبی
                itemView.setBackgroundResource(R.drawable.sound_item_background_playing);
                selectionOverlay.setVisibility(View.VISIBLE);
                nameTextView.setTextColor(Color.parseColor("#3B82F6"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.BOLD);

                Drawable playIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_playing);
                if (playIcon != null) {
                    playIcon.setBounds(0, 0, 16, 16);
                    nameTextView.setCompoundDrawables(null, null, playIcon, null);
                    nameTextView.setCompoundDrawablePadding(8);
                }

            } else if (sound.isSelected()) {
                // اگر انتخاب شده اما پخش نمی‌شود - حاشیه آبی کمرنگ
                selectionOverlay.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.sound_item_background);
                nameTextView.setTextColor(Color.parseColor("#E2E8F0"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.NORMAL);
                nameTextView.setCompoundDrawables(null, null, null, null);

            } else {
                // حالت عادی - بدون حاشیه آبی
                selectionOverlay.setVisibility(View.GONE);
                progressBackground.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.sound_item_background);
                nameTextView.setTextColor(Color.parseColor("#E2E8F0"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.NORMAL);
                nameTextView.setCompoundDrawables(null, null, null, null);
            }

            // Load icon using Glide with error handling
            Glide.with(itemView.getContext())
                    .load(sound.getIcon())
                    .placeholder(R.drawable.music_50px)
                    .error(R.drawable.music_50px)
                    .into(iconImageView);
        }
    }
}