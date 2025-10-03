package ir.zemestoon.silentsound;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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

    private boolean isSoundPlaying(String soundName) {
        return mainActivity != null && mainActivity.isSoundPlaying(soundName);
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
        private ProgressBar downloadProgressBar;
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
            downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar);
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

            // بررسی وضعیت دانلود
            boolean isDownloaded = audioManager.isSoundDownloaded(sound);
            int downloadProgress = audioManager.getDownloadProgress(sound);

            if (downloadProgress > 0 && downloadProgress < 100) {
                // در حال دانلود
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressBar.setProgress(downloadProgress);
                downloadProgressText.setText(downloadProgress + "%");

                if (listener != null) {
                    listener.onDownloadProgress(sound, downloadProgress);
                }
            } else {
                downloadProgressBar.setVisibility(View.GONE);
                downloadProgressText.setVisibility(View.GONE);
            }

            // تغییر ظاهر بر اساس وضعیت انتخاب و پخش
            if (isSoundPlaying(sound.getName())) {
                // اگر در حال پخش است
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
                // اگر انتخاب شده اما پخش نمی‌شود
                selectionOverlay.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.sound_item_background_selected);
                nameTextView.setTextColor(Color.parseColor("#E2E8F0"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.NORMAL);
                nameTextView.setCompoundDrawables(null, null, null, null);
            } else {
                // حالت عادی
                selectionOverlay.setVisibility(View.GONE);
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