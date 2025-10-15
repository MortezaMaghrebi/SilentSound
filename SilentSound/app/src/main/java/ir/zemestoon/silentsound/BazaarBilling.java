package ir.zemestoon.silentsound;

import android.util.Log;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultRegistry;

import java.lang.annotation.Target;

import ir.cafebazaar.poolakey.Payment;
import ir.cafebazaar.poolakey.config.PaymentConfiguration;
import ir.cafebazaar.poolakey.config.SecurityCheck;
import ir.cafebazaar.poolakey.entity.PurchaseInfo;
import ir.cafebazaar.poolakey.request.PurchaseRequest;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class BazaarBilling {

    private static final String TAG = "BazaarBilling";
    private static final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";

    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private Payment payment;
    private Object connection; // در Kotlin نوعش Connection است

    private static BazaarBilling instance;

    final String RSA_KEY="MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCnonCeXAr6fC6Y1QDQOMo140bLrmOFLIcZp3I0M8uNc7IIfA6DecM1z4Y2OQdOYemPguqA6vyo4+3ysOhAui0C7D7y1ug35NK+G31/IlFY15RcaYhfcU8BSFIB5y5pyWGw32E0eaxYiMJBZNZPGfAFRmkkRK0B6PKapKuDZjMqP/DvJ93UZDttR4FWaXEzj7XXFiIrS3mKk+5R6RF9A+cp4nYi9HxX6e9RPXurK5MCAwEAAQ==";
    final String PREMIUM_KEY="premium";
    public static synchronized BazaarBilling getInstance(MainActivity activity) {
        if (instance == null) {
            instance = new BazaarBilling(activity);
        }
        return instance;
    }

    private BazaarBilling(MainActivity activity) {
        this.activity = activity;
        prefs = activity.getSharedPreferences(MY_PREFS_NAME, android.content.Context.MODE_PRIVATE);
        editor = prefs.edit();

    }

    // راه‌اندازی پرداخت بازار با کلید RSA
    public void initializeBazaar() {
        try {
            SecurityCheck securityCheck = new SecurityCheck.Enable(RSA_KEY);
            PaymentConfiguration config = new PaymentConfiguration(securityCheck);
            payment = new Payment(activity, config);
            connectToBazaar();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing Bazaar: " + e.getMessage());
            //ToastUtils.showSafeToast(activity,"❌ Error initializing Bazaar: " + e.getMessage());
        }
    }

    // اتصال به بازار
    private void connectToBazaar() {
        try {
            connection = payment.connect(new Function1<ir.cafebazaar.poolakey.callback.ConnectionCallback, Unit>() {
                @Override
                public Unit invoke(ir.cafebazaar.poolakey.callback.ConnectionCallback callback) {

                    // اتصال موفق
                    callback.connectionSucceed(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            Log.d(TAG, "✅ Connected to Bazaar");
                            //ToastUtils.showSafeToast(activity, "✅ Connected to Bazaar");
                            return Unit.INSTANCE;
                        }
                    });

                    // خطای اتصال
                    callback.connectionFailed(new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            Log.e(TAG, "❌ Connection failed: " + throwable.getMessage());
                            //ToastUtils.showSafeToast(activity, "❌ Connection failed: " + throwable.getMessage());
                            return Unit.INSTANCE;
                        }
                    });

                    // قطع ارتباط
                    callback.disconnected(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            Log.w(TAG, "⚠️ Disconnected from Bazaar");
                            //ToastUtils.showSafeToast(activity, "⚠️ Disconnected from Bazaar");
                            return Unit.INSTANCE;
                        }
                    });

                    return Unit.INSTANCE;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error connecting to Bazaar: " + e.getMessage());
            //ToastUtils.showSafeToast(activity, "❌ Error connecting to Bazaar: " + e.getMessage());
        }
    }

    // خرید محصول
    public void purchaseProduct(ActivityResultRegistry registry, String productId) {
        try {
            String payload=prefs.getString("bazaar_payload","");
            if(payload.length()==0) {
                payload = java.util.UUID.randomUUID().toString();
                editor.putString("bazaar_payload", payload);
                editor.apply();
            }
            PurchaseRequest request = new PurchaseRequest(productId, payload,null);

            payment.purchaseProduct(registry, request, new Function1<ir.cafebazaar.poolakey.callback.PurchaseCallback, Unit>() {
                @Override
                public Unit invoke(ir.cafebazaar.poolakey.callback.PurchaseCallback callback) {

                    callback.purchaseFlowBegan(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            //Log.d(TAG, "🛒 Purchase flow began");
                            //ToastUtils.showSafeToast(activity, "🛒 Purchase flow began");
                            return Unit.INSTANCE;
                        }
                    });

                    callback.failedToBeginFlow(new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            //Log.e(TAG, "❌ Failed to start purchase: " + throwable.getMessage());
                            ToastUtils.showSafeToast(activity, "❌ Failed to start purchase: " + throwable.getMessage());
                            return Unit.INSTANCE;
                        }
                    });

                    callback.purchaseSucceed(new Function1<PurchaseInfo, Unit>() {
                        @Override
                        public Unit invoke(PurchaseInfo purchaseInfo) {
                            //Log.d(TAG, "✅ Purchase successful: " + purchaseInfo.getProductId());
                            String token = purchaseInfo.getPurchaseToken();
                            editor.putString("last_purchase_token", token);
                            editor.putBoolean("premium_activated",true);
                            editor.apply();
                            ToastUtils.showSafeToast(activity, "✅ پرداخت با موفقیت انجام شد");
                            return Unit.INSTANCE;
                        }
                    });

                    callback.purchaseCanceled(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            //Log.w(TAG, "⚠️ Purchase canceled");
                            //ToastUtils.showSafeToast(activity, "⚠️ Purchase canceled");
                            return Unit.INSTANCE;
                        }
                    });

                    callback.purchaseFailed(new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            //Log.e(TAG, "❌ Purchase failed: " + throwable.getMessage());
                            ToastUtils.showSafeToast(activity, "❌ پرداخت انجام نشد" +"\n"+ throwable.getMessage());
                            return Unit.INSTANCE;
                        }
                    });

                    return Unit.INSTANCE;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during purchase: " + e.getMessage());
            ToastUtils.showSafeToast(activity, "❌ پرداخت با خطا مواجه شد" +"\n"+ e.getMessage());
        }
    }
    public void activePremium() {
        ActivityResultRegistry registry= activity.getActivityResultRegistry();
        purchaseProduct(registry,PREMIUM_KEY);
    }
    public boolean isPremiumActivated()
    {
        boolean premium = prefs.getBoolean("premium_activated",false);
        return premium;
    }

    // قطع ارتباط از بازار
    public void disconnect() {
        try {
            if (connection != null && payment != null) {
                Log.d(TAG, "🔌 Disconnected cleanly");
            }
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Disconnect error: " + e.getMessage());
        }
    }
}
