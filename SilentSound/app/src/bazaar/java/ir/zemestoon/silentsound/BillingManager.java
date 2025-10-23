package ir.zemestoon.silentsound;

import android.util.Log;
import android.content.SharedPreferences;
import android.app.Activity;
import android.content.Context;

import ir.cafebazaar.poolakey.Payment;
import ir.cafebazaar.poolakey.config.PaymentConfiguration;
import ir.cafebazaar.poolakey.config.SecurityCheck;
import ir.cafebazaar.poolakey.entity.PurchaseInfo;
import ir.cafebazaar.poolakey.request.PurchaseRequest;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import java.io.UnsupportedEncodingException;

public class BillingManager {

    private static final String TAG = "BillingManager";
    private static final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";
    private static final String PREMIUM_KEY = "premium";

    private final Activity activity;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private Payment mPayment;
    private Object mConnection;
    private boolean mSetupDone = false;

    private static BillingManager instance;

    public static synchronized BillingManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new BillingManager(activity);
        }
        return instance;
    }

    private BillingManager(Activity activity) {
        this.activity = activity;
        prefs = activity.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void initializeBilling() {
        try {
            SecurityCheck securityCheck = new SecurityCheck.Enable(BuildConfig.IAB_PUBLIC_KEY);
            PaymentConfiguration config = new PaymentConfiguration(securityCheck);
            mPayment = new Payment(activity, config);
            connectToBazaar();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing Bazaar: " + e.getMessage());
        }
    }

    private void connectToBazaar() {
        try {
            mConnection = mPayment.connect(new Function1<ir.cafebazaar.poolakey.callback.ConnectionCallback, Unit>() {
                @Override
                public Unit invoke(ir.cafebazaar.poolakey.callback.ConnectionCallback callback) {

                    callback.connectionSucceed(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            Log.d(TAG, "✅ Connected to Bazaar");
                            mSetupDone = true;
                            return Unit.INSTANCE;
                        }
                    });

                    callback.connectionFailed(new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            Log.e(TAG, "❌ Bazaar connection failed: " + throwable.getMessage());
                            return Unit.INSTANCE;
                        }
                    });

                    return Unit.INSTANCE;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error connecting to Bazaar: " + e.getMessage());
        }
    }

    public void purchasePremium() {
        if (mPayment == null || !mSetupDone) {
            ToastUtils.showSafeToast(activity, "❌ سرویس پرداخت آماده نیست");
            return;
        }

        try {
            String payload = getOrCreatePayload();
            PurchaseRequest request = new PurchaseRequest(PREMIUM_KEY, payload, null);

            mPayment.purchaseProduct(((MainActivity)activity).getActivityResultRegistry(), request, 
                new Function1<ir.cafebazaar.poolakey.callback.PurchaseCallback, Unit>() {
                    @Override
                    public Unit invoke(ir.cafebazaar.poolakey.callback.PurchaseCallback callback) {

                        callback.purchaseSucceed(new Function1<PurchaseInfo, Unit>() {
                            @Override
                            public Unit invoke(PurchaseInfo purchaseInfo) {
                                Log.d(TAG, "✅ Bazaar purchase successful");
                                activatePremiumFeatures();
                                ToastUtils.showSafeToast(activity, "✅ پرداخت با موفقیت انجام شد");
                                return Unit.INSTANCE;
                            }
                        });

                        callback.purchaseCanceled(new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                Log.w(TAG, "⚠️ Purchase canceled");
                                return Unit.INSTANCE;
                            }
                        });

                        callback.purchaseFailed(new Function1<Throwable, Unit>() {
                            @Override
                            public Unit invoke(Throwable throwable) {
                                Log.e(TAG, "❌ Purchase failed: " + throwable.getMessage());
                                ToastUtils.showSafeToast(activity, "❌ پرداخت انجام نشد");
                                return Unit.INSTANCE;
                            }
                        });

                        return Unit.INSTANCE;
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during purchase: " + e.getMessage());
            ToastUtils.showSafeToast(activity, "❌ خطا در پرداخت");
        }
    }

    private void activatePremiumFeatures() {
        editor.putBoolean("premium_activated", true);
        editor.apply();
        updateAppData();
    }

    private void updateAppData() {
        try {
            editor.putInt("download_sound_counter", 2);
            editor.commit();
            // NetController.getInstance(activity).DownloadSoundList();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating app data: " + e.getMessage());
        }
    }

    private String getOrCreatePayload() {
        String payload = prefs.getString("bazaar_payload", "");
        if (payload.isEmpty()) {
            payload = java.util.UUID.randomUUID().toString();
            editor.putString("bazaar_payload", payload);
            editor.apply();
        }
        return payload;
    }

    public boolean isPremiumActivated() {
        return prefs.getBoolean("premium_activated", false);
    }

    public boolean isReady() {
        return mPayment != null && mSetupDone;
    }

    public void disconnect() {
        mPayment = null;
        mSetupDone = false;
    }

    public void onDestroy() {
        disconnect();
    }
}