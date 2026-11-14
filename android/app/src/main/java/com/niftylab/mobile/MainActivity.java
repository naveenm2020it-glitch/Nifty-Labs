package com.niftylab.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://nifty-lab.onrender.com/";
    private WebView webView;
    private ProgressBar progressBar;

    // Styling is injected only into this Android WebView. The Render website is untouched.
    private static final String MOBILE_THEME = "(function(){var s=document.getElementById('nifty-apk-theme');if(!s){s=document.createElement('style');s.id='nifty-apk-theme';document.head.appendChild(s);}s.textContent=\":root{color-scheme:light!important;--app-bg:#eef0f8!important;--surface:rgba(255,255,255,.76)!important;--surface-2:rgba(246,247,252,.82)!important;--surface-3:#e9edf8!important;--app-line:rgba(139,151,184,.22)!important;--app-text:#202638!important;--app-muted:#68748c!important;--app-subtle:#8d97aa!important;--app-accent:#6475d9!important;--app-accent-strong:#5365c7!important;--app-accent-soft:#e8ebff!important;--app-shadow:0 12px 32px rgba(56,66,110,.08)!important;--app-shadow-raised:0 18px 42px rgba(56,66,110,.12)!important}\\nhtml,body{background:#eef0f8!important}\\nbody{background:radial-gradient(ellipse at 4% 2%,rgba(188,198,242,.55),transparent 38%),radial-gradient(ellipse at 96% 8%,rgba(244,205,218,.48),transparent 38%),radial-gradient(ellipse at 12% 95%,rgba(207,224,215,.55),transparent 42%),linear-gradient(145deg,#f5f6fc,#edf0f7 55%,#f7eef1)!important;color:#202638!important;background-attachment:fixed!important}\\n.sidebar{position:sticky!important;top:8px!important;z-index:100!important;display:flex!important;flex-direction:column!important;align-items:stretch!important;gap:10px!important;margin:10px 12px 16px!important;padding:12px 14px!important;width:calc(100% - 24px)!important;border:1px solid rgba(255,255,255,.82)!important;border-radius:24px!important;background:rgba(255,255,255,.62)!important;backdrop-filter:blur(26px) saturate(155%)!important;-webkit-backdrop-filter:blur(26px) saturate(155%)!important;box-shadow:0 12px 34px rgba(54,63,104,.09),inset 0 1px 0 rgba(255,255,255,.9)!important}\\n.side-brand{min-width:0!important;width:100%!important;padding:2px 2px 5px!important}\\n.sidebar>div:nth-child(2){width:100%!important;justify-content:flex-start!important}\\n.side-nav{display:flex!important;justify-content:flex-start!important;gap:8px!important;width:100%!important;overflow-x:auto!important;overflow-y:hidden!important;padding:0 0 2px!important;scrollbar-width:none!important}\\n.side-nav::-webkit-scrollbar{display:none!important}\\n.nav-item{flex:0 0 auto!important;min-height:46px!important;padding:9px 13px!important;border:1px solid rgba(142,153,188,.14)!important;border-radius:16px!important;background:rgba(255,255,255,.58)!important;color:#68748c!important;box-shadow:0 4px 12px rgba(50,60,100,.035)!important}\\n.nav-item.active{border:1px solid rgba(103,119,218,.24)!important;background:linear-gradient(120deg,rgba(232,235,255,.95),rgba(225,246,242,.92))!important;color:#5868ca!important;box-shadow:0 6px 16px rgba(89,104,190,.10),inset 0 1px 0 rgba(255,255,255,.95)!important}\\n.app-main{width:100%!important;padding:12px 14px 38px!important}\\n.panel{background:rgba(255,255,255,.66)!important;border:1px solid rgba(255,255,255,.88)!important;border-radius:23px!important;box-shadow:0 12px 34px rgba(54,63,104,.075),inset 0 1px 0 rgba(255,255,255,.85)!important;backdrop-filter:blur(18px) saturate(145%)!important;-webkit-backdrop-filter:blur(18px) saturate(145%)!important}\\n.panel .panel{background:rgba(245,247,253,.78)!important;border:1px solid rgba(143,154,190,.2)!important;border-radius:19px!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.9)!important}\\n.metric,.info-field,.notice,.status,.source-box,.calc-note{background:rgba(249,250,255,.72)!important;border-color:rgba(139,151,184,.22)!important;border-radius:18px!important;box-shadow:0 6px 18px rgba(54,63,104,.045)!important}\\ninput,select,textarea{background:rgba(255,255,255,.72)!important;border:1px solid rgba(139,151,184,.25)!important;border-radius:15px!important;color:#202638!important;min-height:48px!important}\\nbutton{border-radius:15px!important}\\nbutton:not(.secondary){background:linear-gradient(120deg,#7583df,#6c9bc5)!important;border-color:rgba(255,255,255,.65)!important;box-shadow:0 7px 18px rgba(92,111,194,.17)!important}\\nbutton.secondary{background:rgba(255,255,255,.72)!important;border-color:rgba(139,151,184,.22)!important;color:#39445b!important}\\nh1,h2,h3,h4,strong{color:#202638!important}\\nlabel,.muted,.sub,.field-hint,.page-intro p{color:#68748c!important}\\n@media(max-width:768px){.app-main{padding:10px 12px 34px!important}.sidebar{margin:8px 10px 13px!important;width:calc(100% - 20px)!important;padding:10px 12px!important}.app-topbar{min-height:auto!important;padding:17px!important;margin-bottom:16px!important}.page-heading h1{font-size:23px!important}.panel{padding:15px!important;margin-bottom:13px!important}.panel .panel{padding:13px!important;margin:12px 0!important}.grid{grid-template-columns:1fr!important;gap:12px!important}.cards{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:9px!important}.metric{padding:13px!important}.metric strong{font-size:18px!important}.actions{gap:8px!important}.actions button{flex:1 1 auto!important}}\";document.documentElement.dataset.theme='light';})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(22, 23, 29));
        getWindow().setNavigationBarColor(Color.rgb(22, 23, 29));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(241, 244, 252));
        int statusBarHeight = 0;
        int statusId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusId > 0) statusBarHeight = getResources().getDimensionPixelSize(statusId);
        root.setPadding(0, statusBarHeight, 0, 0);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(112, 103, 177)));
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        root.addView(progressBar);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(239, 235, 243));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("nifty-lab.onrender.com".equalsIgnoreCase(host)) {
                    return false;
                }
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(MOBILE_THEME, null);
                view.evaluateJavascript(OUTER_THEME, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        root.addView(webView);
        setContentView(root);

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
