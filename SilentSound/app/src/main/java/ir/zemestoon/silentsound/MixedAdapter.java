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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;

public class MixedAdapter extends RecyclerView.Adapter<MixedAdapter.MixedViewHolder> {

    private List<Mixed> mixedList;
    private OnMixedClickListener listener;
    private int itemWidth;
    private MainActivity mainActivity;
    private AudioManager audioManager;

    public interface OnMixedClickListener {
        void onMixedClick(Mixed mixed);
        void onMixedPlayPause(Mixed mixed);

        void onMixedDetails(Mixed mixed);

        void onDownloadProgress(Mixed mixed, int progress);
    }

    public MixedAdapter(List<Mixed> mixedList, OnMixedClickListener listener, int screenWidth, MainActivity mainActivity) {
        this.mixedList = mixedList;
        this.listener = listener;
        this.itemWidth = (screenWidth - convertDpToPx(60)) / 2;
        this.mainActivity = mainActivity;
        this.audioManager = AudioManager.getInstance(mainActivity);
    }

    private boolean isMixedPlaying(String mixedId) {
        // این متد باید در MainActivity پیاده‌سازی شود
        return mainActivity != null && mainActivity.isMixedPlaying(mixedId);
    }

    private int convertDpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @NonNull
    @Override
    public MixedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mixed, parent, false);
        return new MixedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MixedViewHolder holder, int position) {
        Mixed mixed = mixedList.get(position);
        holder.bind(mixed);
    }

    @Override
    public int getItemCount() {
        return mixedList.size();
    }

    public void updateList(List<Mixed> newList) {
        mixedList = newList;
        notifyDataSetChanged();
    }

    class MixedViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private TextView durationTextView;
        private TextView soundsCountTextView;
        private TextView downloadProgressText;
        private View selectionOverlay;
        private View progressBackground;
        private ImageButton playPauseButton;

        public MixedViewHolder(@NonNull View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.width = itemWidth;
            itemView.setLayoutParams(layoutParams);

            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
            soundsCountTextView = itemView.findViewById(R.id.soundsCountTextView);
            downloadProgressText = itemView.findViewById(R.id.downloadProgressText);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            progressBackground = itemView.findViewById(R.id.progressBackground);
            playPauseButton = itemView.findViewById(R.id.playPauseButton);

            // کلیک روی کل آیتم
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMixedClick(mixedList.get(getAdapterPosition()));
                }
            });

            // کلیک روی دکمه پخش/توقف
            playPauseButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMixedPlayPause(mixedList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Mixed mixed) {
            nameTextView.setText(mixed.getName());
            durationTextView.setText(mixed.getFormattedDuration());
            soundsCountTextView.setText(mixed.getSoundCount() + " صدا");

            // بررسی وضعیت دانلود
            boolean allSoundsDownloaded = checkAllSoundsDownloaded(mixed);
            int downloadProgress = calculateDownloadProgress(mixed);

            Log.d("MixedAdapter", "Binding: " + mixed.getName() +
                    ", All Downloaded: " + allSoundsDownloaded +
                    ", Progress: " + downloadProgress);

            // مدیریت نمایش progress
            if (!allSoundsDownloaded && downloadProgress > 0) {
                progressBackground.setVisibility(View.VISIBLE);
                // تنظیم level برای ClipDrawable
                Drawable backgroundDrawable = progressBackground.getBackground();
                if (backgroundDrawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) backgroundDrawable;
                    Drawable clipDrawable = layerDrawable.getDrawable(1);
                    if (clipDrawable instanceof ClipDrawable) {
                        int level = downloadProgress * 100;
                        clipDrawable.setLevel(level);
                    }
                }

                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressText.setText(downloadProgress + "%");
                itemView.setEnabled(false);
                itemView.setAlpha(0.9f);
                playPauseButton.setEnabled(false);

            } else if (!allSoundsDownloaded && downloadProgress == 0) {
                progressBackground.setVisibility(View.VISIBLE);
                Drawable backgroundDrawable = progressBackground.getBackground();
                if (backgroundDrawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) backgroundDrawable;
                    Drawable clipDrawable = layerDrawable.getDrawable(1);
                    if (clipDrawable instanceof ClipDrawable) {
                        clipDrawable.setLevel(0);
                    }
                }

                downloadProgressText.setVisibility(View.GONE);
                itemView.setEnabled(true);
                itemView.setAlpha(1.0f);
                playPauseButton.setEnabled(true);

            } else {
                progressBackground.setVisibility(View.INVISIBLE);
                downloadProgressText.setVisibility(View.GONE);
                itemView.setEnabled(true);
                itemView.setAlpha(1.0f);
                playPauseButton.setEnabled(true);
            }

            // مدیریت ظاهر بر اساس وضعیت پخش
            if (isMixedPlaying(mixed.getId())) {
                itemView.setBackgroundResource(R.drawable.sound_item_background_playing);
                selectionOverlay.setVisibility(View.VISIBLE);
                nameTextView.setTextColor(Color.parseColor("#3B82F6"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.BOLD);
                playPauseButton.setImageResource(R.drawable.ic_pause);
            } else if (mixed.isSelected()) {
                selectionOverlay.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.sound_item_background);
                nameTextView.setTextColor(Color.parseColor("#E2E8F0"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.NORMAL);
                playPauseButton.setImageResource(R.drawable.ic_play);
            } else {
                selectionOverlay.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.sound_item_background);
                nameTextView.setTextColor(Color.parseColor("#E2E8F0"));
                nameTextView.setTypeface(nameTextView.getTypeface(), Typeface.NORMAL);
                playPauseButton.setImageResource(R.drawable.ic_play);
            }


            Glide.with(itemView.getContext())
                    .load(mixed.getIcon())
                    .placeholder(R.drawable.rounded_corners_background)
                    .error(R.drawable.rounded_corners_background)
                    .transform(new CenterCrop(), new RoundedCorners(convertDpToPx(20)))
                    .into(iconImageView);

        }

        private boolean checkAllSoundsDownloaded(Mixed mixed) {
            for (Mixed.MixedSound mixedSound : mixed.getSounds()) {
                Sound sound = findSoundByName(mixedSound.getSoundName());
                if (sound != null && !audioManager.isSoundDownloaded(sound)) {
                    return false;
                }
            }
            return true;
        }

        private int calculateDownloadProgress(Mixed mixed) {
            int totalSounds = mixed.getSounds().size();
            if (totalSounds == 0) return 100;

            int downloadedCount = 0;
            for (Mixed.MixedSound mixedSound : mixed.getSounds()) {
                Sound sound = findSoundByName(mixedSound.getSoundName());
                if (sound != null && audioManager.isSoundDownloaded(sound)) {
                    downloadedCount++;
                }
            }
            return (downloadedCount * 100) / totalSounds;
        }

        private Sound findSoundByName(String soundName) {
            // این متد باید به MainActivity دسترسی داشته باشد
            if (mainActivity != null) {
                return mainActivity.findSoundByName(soundName);
            }
            return null;
        }
    }
}