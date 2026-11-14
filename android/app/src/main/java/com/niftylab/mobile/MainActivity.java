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
    private static final String MOBILE_THEME = "(function(){"
        + "if(document.getElementById('nifty-apk-theme'))return;"
        + "var s=document.createElement('style');s.id='nifty-apk-theme';"
        + "s.textContent="
        + "'html{background:#eeeaf4!important;}'"
        + "+'body{background:radial-gradient(ellipse at 8% 4%,rgba(197,184,232,.72),transparent 42%),radial-gradient(ellipse at 94% 16%,rgba(242,194,210,.62),transparent 40%),radial-gradient(ellipse at 15% 92%,rgba(213,224,204,.58),transparent 42%),linear-gradient(145deg,#f5f1f8,#edf0ed 58%,#f8e9e8)!important;color:#17191d!important;background-attachment:fixed!important;}'"
        + "+'body,button,input,select,textarea{font-family:Inter,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif!important;}'"
        + "+'header,nav,[role=tablist]{background:rgba(255,255,255,.58)!important;border-color:rgba(255,255,255,.72)!important;backdrop-filter:blur(22px) saturate(145%)!important;-webkit-backdrop-filter:blur(22px) saturate(145%)!important;box-shadow:0 8px 28px rgba(45,42,60,.07)!important;}'"
        + "+'header{border-radius:0 0 22px 22px!important;}'"
        + "+'nav,[role=tablist]{border-radius:18px!important;}'"
        + "+'nav{max-width:100vw!important;overflow-x:auto!important;scrollbar-width:none!important;}'"
        + "+'nav::-webkit-scrollbar,[role=tablist]::-webkit-scrollbar{display:none!important;}'"
        + "+'[role=tab],nav a,nav button{border-radius:14px!important;}'"
        + "+'main, [role=main]{background:transparent!important;}'"
        + "+'[class*=card],[class*=Card],[class*=panel],[class*=Panel],article,section{border-color:rgba(255,255,255,.78)!important;}'"
        + "+'[class*=card],[class*=Card],[class*=panel],[class*=Panel]{background:rgba(255,255,255,.48)!important;backdrop-filter:blur(18px) saturate(135%)!important;-webkit-backdrop-filter:blur(18px) saturate(135%)!important;border-radius:22px!important;box-shadow:0 10px 30px rgba(50,45,65,.075),inset 0 1px 0 rgba(255,255,255,.72)!important;}'"
        + "+'input,select,textarea{background:rgba(255,255,255,.68)!important;border:1px solid rgba(128,135,160,.2)!important;border-radius:15px!important;color:#20232a!important;box-shadow:inset 0 1px 2px rgba(35,38,50,.025)!important;}'"
        + "+'button{border-radius:14px!important;}'"
        + "+'button:not([class*=ghost]):not([class*=outline]),[role=button]{box-shadow:0 5px 14px rgba(40,42,60,.07)!important;}'"
        + "+'h1,h2,h3,h4,strong{letter-spacing:-.025em!important;color:#17191f!important;}'"
        + "+'p,small,label{color:inherit;}'"
        + "+'table{border-collapse:separate!important;border-spacing:0!important;overflow:hidden!important;border-radius:16px!important;}'"
        + "+'::-webkit-scrollbar{width:4px;height:4px;}::-webkit-scrollbar-thumb{background:rgba(80,83,100,.22);border-radius:8px;}'"
        + "+'@media(max-width:768px){body{font-size:14px!important;}header{position:sticky!important;top:0!important;z-index:50!important;}main,[role=main]{padding-left:12px!important;padding-right:12px!important;}[class*=card],[class*=Card],[class*=panel],[class*=Panel]{margin-bottom:12px!important;}nav,[role=tablist]{gap:6px!important;padding:7px!important;}input,select,textarea{min-height:46px!important;}}';"
        + "document.head.appendChild(s);document.documentElement.classList.add('nifty-apk-mobile');"
        + "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(22, 23, 29));
        getWindow().setNavigationBarColor(Color.rgb(22, 23, 29));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(239, 235, 243));

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
