package ir.zemestoon.silentsound;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

public class AddsManager {
    MainActivity activity;
    final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";
    SharedPreferences.Editor editor;
    SharedPreferences prefs;

    boolean initialized=false;
    public static  AddsManager instance;
    public static synchronized AddsManager getInstance(MainActivity activity) {
        if (instance == null) {
            instance = new AddsManager(activity);
        }
        return instance;
    }
    public AddsManager(MainActivity activity) {
        this.activity = activity;
        editor = activity.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        prefs = activity.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        initializeTapsell();
    }

    String key ="rtglinlqkaanoalbdtbjrhtmthtactdsjcctboithdncqnnoeehaesmidaiendaceckndi";
    private void initializeTapsell()
    {
        TapsellPlus.initialize(activity, key,
                new TapsellPlusInitListener() {
                    @Override
                    public void onInitializeSuccess(AdNetworks adNetworks) {
                       initialized=true;
                        Log.d("onInitializeSuccess", adNetworks.name());
                    }

                    @Override
                    public void onInitializeFailed(AdNetworks adNetworks,
                                                   AdNetworkError adNetworkError) {
                        initialized=false;
                        Log.e("onInitializeFailed", "ad network: " + adNetworks.name() + ", error: " +	adNetworkError.getErrorMessage());
                    }
                });
    }

    int counter=0;
    public void RequestRewardedVideoAdd()
    {
        counter=0;
        requestAd();
    }
    private void requestAd() {
        if(!initialized) {
            ToastUtils.showSafeToast(activity,"نشد، اینترنت را وصل کنید و دوباره امتجان کنید");
            initializeTapsell();
            return;
        }
        TapsellPlus.requestRewardedVideoAd(activity ,"68ee7502a82c28634aaef56a",
                new AdRequestCallback() {
                    @Override
                    public void response(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.response(tapsellPlusAdModel);
                        String rewardedResponseId = tapsellPlusAdModel.getResponseId();
                        ShowRewardedVideoAdd(rewardedResponseId);
                    }

                    @Override
                    public void error(String message) {
                        counter++;
                        if(counter<3) requestAd();
                        else ToastUtils.showSafeToast(activity,"تبلیغ یافت نشد");
                    }

                });
    }

    private AdCompletionListener adCompletionListener;

    public interface AdCompletionListener {
        void onAdCompleted();
    }

    public void setAdCompletionListener(AdCompletionListener listener) {
        this.adCompletionListener = listener;
    }

    private void ShowRewardedVideoAdd(String rewardedResponseId) {
        TapsellPlus.showRewardedVideoAd(activity, rewardedResponseId,
                new AdShowListener() {
                    @Override
                    public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.onOpened(tapsellPlusAdModel);
                    }

                    @Override
                    public void onClosed(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.onClosed(tapsellPlusAdModel);
                    }

                    @Override
                    public void onRewarded(TapsellPlusAdModel tapsellPlusAdModel) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime currentTime = LocalDateTime.now();
                            String currentTimeString = currentTime.format(formatter);
                            editor.putString("rewardedtime", currentTimeString);
                        } else {
                            long currentTimeMillis = System.currentTimeMillis();
                            editor.putLong("rewardedtime_millis", currentTimeMillis);
                        }
                        editor.apply();

                        // اطلاع به listener پس از تماشای تبلیغ
                        if (adCompletionListener != null) {
                            adCompletionListener.onAdCompleted();
                        }

                        super.onRewarded(tapsellPlusAdModel);
                    }

                    @Override
                    public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
                        super.onError(tapsellPlusErrorModel);
                    }
                });
    }

    public boolean isPassed24HoursAfterAdd()
    {
        long diffInSeconds = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String storedTimeString = prefs.getString("rewardedtime", null);
            if (storedTimeString != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime storedTime = LocalDateTime.parse(storedTimeString, formatter);
                LocalDateTime now = LocalDateTime.now();
                diffInSeconds = java.time.Duration.between(storedTime, now).getSeconds();
            }else return true;
        } else {
            long storedTimeMillis = prefs.getLong("rewardedtime_millis", 0);
            if (storedTimeMillis != 0) {
                long now = System.currentTimeMillis();
                diffInSeconds = (now - storedTimeMillis) / 1000;
            }else return true;
        }
        return (diffInSeconds>20);//(24*60*60));
    }

}
