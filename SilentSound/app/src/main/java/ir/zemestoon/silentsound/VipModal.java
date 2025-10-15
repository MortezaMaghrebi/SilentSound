package ir.zemestoon.silentsound;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class VipModal {
    private Dialog dialog;
    private Context context;
    private VipModalListener listener;
    private Mixed mixed;

    public interface VipModalListener {
        void onPurchaseClicked(Mixed mixed);
        void onWatchAdClicked(Mixed mixed);
        void onCancelClicked();
    }

    public VipModal(Context context, Mixed mixed, VipModalListener listener) {
        this.context = context;
        this.mixed = mixed;
        this.listener = listener;
        createDialog();
    }

    private void createDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.vip_modal);
        dialog.setCancelable(true);

        // تنظیم سایز دیالوگ
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        setupViews();
    }

    private void setupViews() {
        Button btnPurchase = dialog.findViewById(R.id.btnPurchase);
        Button btnWatchAd = dialog.findViewById(R.id.btnWatchAd);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btnPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPurchaseClicked(mixed);
                }
                dismiss();
            }
        });

        btnWatchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onWatchAdClicked(mixed);
                }
                dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onCancelClicked();
                }
                dismiss();
            }
        });
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}