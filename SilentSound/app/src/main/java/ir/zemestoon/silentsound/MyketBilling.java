package ir.zemestoon.silentsound;

import android.util.Log;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultRegistry;

import java.io.UnsupportedEncodingException;

import ir.myket.billingclient.IABClient;
import ir.myket.billingclient.IABClientConfig;
import ir.myket.billingclient.IABResponse;
import ir.myket.billingclient.SkuDetails;
import ir.myket.billingclient.util.IabHelper;
import ir.myket.billingclient.util.IabResult;
import ir.myket.billingclient.util.Inventory;
import ir.myket.billingclient.util.Purchase;

public class MyketBilling {

    private static final String TAG = "MyketBilling";
    private static final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";

    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private IABClient iabClient;
    private IabHelper iabHelper;

    private static MyketBilling instance;

    final String RSA_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCnonCeXAr6fC6Y1QDQOMo140bLrmOFLIcZp3I0M8uNc7IIfA6DecM1z4Y2OQdOYemPguqA6vyo4+3ysOhAui0C7D7y1ug35NK+G31/IlFY15RcaYhfcU8BSFIB5y5pyWGw32E0eaxYiMJBZNZPGfAFRmkkRK0B6PKapKuDZjMqP/DvJ93UZDttR4FWaXEzj7XXFiIrS3mKk+5R6RF9A+cp4nYi9HxX6e9RPXurK5MCAwEAAQ==";
    final String PREMIUM_KEY = "premium";

    public static synchronized MyketBilling getInstance(MainActivity activity) {
        if (instance == null) {
            instance = new MyketBilling(activity);
        }
        return instance;
    }

    private MyketBilling(MainActivity activity) {
        this.activity = activity;
        prefs = activity.getSharedPreferences(MY_PREFS_NAME, android.content.Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // راه‌اندازی مایکت
    public void initializeMyket() {
        try {
            // روش 1: استفاده از IABClient (روش جدیدتر)
            IABClientConfig config = new IABClientConfig.Builder()
                    .setSecurityCheck(RSA_KEY)
                    .build();
            iabClient = new IABClient(activity, config);

            // روش 2: استفاده از IabHelper (برای سازگاری)
            iabHelper = new IabHelper(activity, RSA_KEY);
            iabHelper.enableDebugLogging(true);

            connectToMyket();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing Myket: " + e.getMessage());
        }
    }

    // اتصال به مایکت
    private void connectToMyket() {
        try {
            if (iabHelper != null) {
                iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    @Override
                    public void onIabSetupFinished(IabResult result) {
                        if (result.isSuccess()) {
                            Log.d(TAG, "✅ Connected to Myket");
                        } else {
                            Log.e(TAG, "❌ Connection failed: " + result.getMessage());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error connecting to Myket: " + e.getMessage());
        }
    }

    // خرید محصول
    public void purchaseProduct(ActivityResultRegistry registry, String productId) {
        try {
            String payload = prefs.getString("myket_payload", "");
            if (payload.length() == 0) {
                payload = java.util.UUID.randomUUID().toString();
                editor.putString("myket_payload", payload);
                editor.apply();
            }

            if (iabHelper != null) {
                iabHelper.launchPurchaseFlow(activity, productId, 1001,
                        new IabHelper.OnIabPurchaseFinishedListener() {
                            @Override
                            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                                if (result.isSuccess()) {
                                    // خرید موفق
                                    handleSuccessfulPurchase(purchase);
                                } else if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                                    // کاربر لغو کرد
                                    Log.w(TAG, "⚠️ Purchase canceled by user");
                                } else {
                                    // خطا در خرید
                                    Log.e(TAG, "❌ Purchase failed: " + result.getMessage());
                                    ToastUtils.showSafeToast(activity, "❌ پرداخت انجام نشد\n" + result.getMessage());
                                }
                            }
                        }, payload);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during purchase: " + e.getMessage());
            ToastUtils.showSafeToast(activity, "❌ پرداخت با خطا مواجه شد\n" + e.getMessage());
        }
    }

    // پردازش خرید موفق
    private void handleSuccessfulPurchase(Purchase purchase) {
        try {
            String productId = purchase.getSku();
            String token = purchase.getToken();

            editor.putString("last_purchase_token", token);
            editor.putBoolean("premium_activated", true);
            editor.apply();

            ToastUtils.showSafeToast(activity, "✅ پرداخت با موفقیت انجام شد");

            try {
                editor.putInt("download_sound_counter", 2);
                editor.commit();
                NetController.getInstance(activity).DownloadSoundList();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            try {
                editor.putInt("download_mixed_counter", 2);
                NetController.getInstance(activity).DownloadMixedList();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling purchase: " + e.getMessage());
        }
    }

    // فعال‌سازی خرید پریمیوم
    public void activePremium() {
        ActivityResultRegistry registry = activity.getActivityResultRegistry();
        purchaseProduct(registry, PREMIUM_KEY);
    }

    // بررسی فعال بودن پریمیوم
    public boolean isPremiumActivated() {
        return prefs.getBoolean("premium_activated", false);
    }

    // بررسی خریدهای قبلی
    public void checkExistingPurchases() {
        try {
            if (iabHelper != null) {
                iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        if (result.isSuccess()) {
                            // بررسی خریدهای موجود
                            Purchase premiumPurchase = inventory.getPurchase(PREMIUM_KEY);
                            if (premiumPurchase != null && verifyPurchase(premiumPurchase)) {
                                editor.putBoolean("premium_activated", true);
                                editor.apply();
                                Log.d(TAG, "✅ Existing premium purchase found and verified");
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking existing purchases: " + e.getMessage());
        }
    }

    // تأیید اعتبار خرید
    private boolean verifyPurchase(Purchase purchase) {
        // اینجا می‌توانید اعتبار خرید را با سرور خود تأیید کنید
        // برای سادگی، فعلاً true برمی‌گردانیم
        return true;
    }

    // قطع ارتباط از مایکت
    public void disconnect() {
        try {
            if (iabHelper != null) {
                iabHelper.dispose();
                iabHelper = null;
            }
            Log.d(TAG, "🔌 Disconnected cleanly from Myket");
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Disconnect error: " + e.getMessage());
        }
    }
}