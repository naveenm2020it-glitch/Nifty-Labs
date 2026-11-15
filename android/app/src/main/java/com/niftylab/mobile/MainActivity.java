package com.niftylab.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends Activity {
    private static final String LOCAL_URL = "http://127.0.0.1:8765/";
    private static final int PORT = 8765;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView startupMessage;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(244, 242, 251));
        getWindow().setNavigationBarColor(Color.rgb(244, 242, 251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 242, 251));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(126, 97, 190)));
        root.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        startupMessage = new TextView(this);
        startupMessage.setText("Preparing NIFTY LAB local engine…");
        startupMessage.setTextColor(Color.rgb(63, 52, 91));
        startupMessage.setTextSize(15);
        startupMessage.setGravity(android.view.Gravity.CENTER);
        startupMessage.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.addView(startupMessage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(244, 242, 251));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(true); // Required only to load the bundled engine-only HTML from android_asset.
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost())) return false;
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                }
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) showStartupError("Local NIFTY LAB page failed: " + error.getDescription());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progressBar.setProgress(value);
                progressBar.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        root.addView(webView);
        setContentView(root);
        startBundledEngine();
    }

    private void startBundledEngine() {
        new Thread(() -> {
            try {
                File root = new File(getFilesDir(), "niftylab_root");
                copyAssetTree("niftylab_root", root);
                File db = new File(getFilesDir(), "niftylab_cache.sqlite3");
                new Thread(() -> {
                    try {
                        if (!Python.isStarted()) Python.start(new AndroidPlatform(getApplicationContext()));
                        Python.getInstance().getModule("standalone_server").callAttr("start", root.getAbsolutePath(), db.getAbsolutePath(), "127.0.0.1", PORT);
                    } catch (Throwable error) {
                        showStartupError("Embedded calculation engine failed: " + error.getMessage());
                    }
                }, "niftylab-python-server").start();

                boolean ready = false;
                for (int i = 0; i < 100; i++) {
                    if (isLocalServerReady()) { ready = true; break; }
                    Thread.sleep(350);
                }
                if (!ready) throw new IllegalStateException("Local engine did not become ready. Check app storage and bundled Python libraries.");
                handler.post(() -> {
                    startupMessage.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.loadUrl(LOCAL_URL);
                });
            } catch (Throwable error) {
                showStartupError("NIFTY LAB setup failed: " + error.getMessage());
            }
        }, "niftylab-bootstrap").start();
    }

    private boolean isLocalServerReady() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 400);
            return true;
        } catch (Exception ignored) { return false; }
    }

    private void copyAssetTree(String assetPath, File destination) throws Exception {
        AssetManager assets = getAssets();
        String[] children = assets.list(assetPath);
        if (children == null || children.length == 0) {
            destination.getParentFile().mkdirs();
            try (InputStream in = assets.open(assetPath); FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            return;
        }
        if (!destination.exists() && !destination.mkdirs()) throw new IllegalStateException("Cannot create app folder: " + destination);
        for (String child : children) copyAssetTree(assetPath + "/" + child, new File(destination, child));
    }

    private void showStartupError(String message) {
        handler.post(() -> {
            if (isFinishing()) return;
            startupMessage.setVisibility(View.VISIBLE);
            startupMessage.setText(message + "\n\nUse Retry after checking available storage and internet access.");
            progressBar.setVisibility(View.GONE);
            new AlertDialog.Builder(this).setTitle("NIFTY LAB startup error").setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> { startupMessage.setText("Restarting local NIFTY LAB engine…"); progressBar.setVisibility(View.VISIBLE); startBundledEngine(); })
                .setNegativeButton("Close", (dialog, which) -> finish()).show();
        });
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
