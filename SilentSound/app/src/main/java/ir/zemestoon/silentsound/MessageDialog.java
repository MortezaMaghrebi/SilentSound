// MessageDialog.java
package ir.zemestoon.silentsound;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class MessageDialog extends Dialog {
    private String htmlContent;
    private WebView webView;
    private Context context;

    public MessageDialog(@NonNull Context context, String htmlContent) {
        super(context);
        this.context = context;
        this.htmlContent = htmlContent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_message);

        // تنظیم سایز دیالوگ
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        webView = findViewById(R.id.webView);
        Button btnClose = findViewById(R.id.btnClose);

        // پیکربندی WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new CustomWebViewClient());

        // بارگذاری HTML با استایل بهتر
        String styledHtml = "<html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { margin: 20px; font-family: sans-serif; background: transparent; } " +
                "a { color: #60a5fa; text-decoration: none; } " +
                "a:active { color: #3b82f6; } " +
                ".bazaar-link { background: #ff5722; color: white; padding: 10px 15px; border-radius: 8px; display: inline-block; margin: 10px 0; }" +
                "</style>" +
                "</head><body>" + htmlContent + "</body></html>";

        webView.loadDataWithBaseURL(
                null,
                styledHtml,
                "text/html",
                "UTF-8",
                null
        );

        btnClose.setOnClickListener(v -> dismiss());

        // جلوگیری از بسته شدن با کلیک بیرون
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    // کلاس custom برای مدیریت لینک‌ها
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) return false;

            // باز کردن لینک‌های http و https در مرورگر خارجی
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }

            // باز کردن لینک کافه بازار
            if (url.startsWith("bazaar://") || url.startsWith("market://") || url.contains("cafebazaar")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    // اگر کافه بازار نصب نبود، لینک وب سایت بازار رو باز کن
                    String webUrl = "https://cafebazaar.ir";
                    if (url.contains("details?id=")) {
                        // استخراج package name از لینک
                        String packageName = extractPackageName(url);
                        if (packageName != null) {
                            webUrl = "https://cafebazaar.ir/app/" + packageName;
                        }
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                return true;
            }

            return false;
        }

        private String extractPackageName(String url) {
            try {
                if (url.contains("details?id=")) {
                    return url.substring(url.indexOf("details?id=") + 11);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}