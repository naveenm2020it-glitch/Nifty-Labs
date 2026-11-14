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
    private static final String APP_URL = "https://nifty-lab.onrender.com/workspace";
    private WebView webView;
    private ProgressBar progressBar;

    // Styling is injected only into this Android WebView. The Render website is untouched.
    private static final String MOBILE_THEME = "(function(){var css=\"html,body{width:100%!important;min-width:320px!important;min-height:100%!important;background:#f4f2fb!important;color:#28243d!important}body{margin:0!important;padding:0 0 82px!important;overflow-x:hidden!important;background:radial-gradient(ellipse at 0 0,rgba(207,194,255,.34),transparent 42%),radial-gradient(ellipse at 100% 10%,rgba(255,218,232,.3),transparent 40%),linear-gradient(145deg,#faf8ff,#f0effa 65%,#f9f0f5)!important;font-family:Inter,system-ui,-apple-system,'Segoe UI',sans-serif!important}.app-shell{display:block!important;width:100%!important;min-height:100vh!important;background:transparent!important}.app-shell>.sidebar,.sidebar,.app-main>.app-topbar,.app-main>header.top,#dataNotice{display:none!important}.app-main{display:block!important;width:100%!important;max-width:none!important;min-width:0!important;margin:0!important;padding:14px 11px 30px!important;background:transparent!important}.page-view{min-width:0!important}.page-intro{display:flex!important;margin:0 3px 13px!important}.page-intro h2{display:none!important}.page-intro p{font-size:12px!important;color:#817b96!important}.panel{padding:14px!important;margin:0 0 14px!important;border:1px solid rgba(255,255,255,.96)!important;border-radius:23px!important;background:rgba(255,255,255,.78)!important;box-shadow:0 10px 27px rgba(75,56,119,.075),inset 0 1px rgba(255,255,255,.9)!important;backdrop-filter:blur(18px)!important}.session-group{padding:13px!important;margin:11px 0!important;border:1px solid rgba(169,151,212,.24)!important;border-radius:20px!important;background:linear-gradient(145deg,rgba(255,255,255,.96),rgba(246,242,255,.9))!important;box-shadow:0 6px 17px rgba(83,61,132,.05)!important}.session-step{width:40px!important;height:40px!important;flex-basis:40px!important;border-radius:14px!important;background:linear-gradient(145deg,#eee7ff,#e3e9ff)!important;color:#8064cf!important}.session-group-head strong,h1,h2,h3,h4,label{color:#302b47!important}.session-group-head small,.muted,.sub,.field-hint{color:#817b96!important}.info-field{padding:12px!important;border:1px solid rgba(161,148,194,.23)!important;border-radius:17px!important;background:rgba(255,255,255,.8)!important;box-shadow:0 5px 14px rgba(79,61,120,.045)!important}input,select,textarea{width:100%!important;min-width:0!important;min-height:50px!important;border:1px solid rgba(158,145,194,.3)!important;border-radius:15px!important;background:linear-gradient(145deg,#fff,#f7f5fc)!important;color:#302b47!important;font-size:15px!important;box-shadow:0 4px 12px rgba(65,49,104,.04)!important}button{border-radius:15px!important;transition:transform .18s ease,box-shadow .18s ease!important}button:active{transform:scale(.97)!important}button:not(.secondary){box-shadow:0 7px 16px rgba(143,111,207,.18)!important}@media(hover:hover){button:hover{transform:translateY(-2px)!important;box-shadow:0 9px 20px rgba(126,98,185,.16)!important}}.cards,.grid{gap:10px!important}.metric,.level-card,.hlc-card,.bep-metric,.source-box,.status,.notice{border-radius:18px!important;box-shadow:0 7px 17px rgba(77,58,120,.06)!important}.tablewrap,.bep-table-wrap{max-width:100%!important;overflow-x:auto!important;border-radius:16px!important}table{min-width:560px!important}.session-actions{display:flex!important;flex-wrap:wrap!important;gap:9px!important}.session-actions button{flex:1 1 135px!important;min-height:46px!important}.nifty-apk-bottom-nav{position:fixed!important;z-index:99999!important;left:11px!important;right:11px!important;bottom:calc(9px + env(safe-area-inset-bottom,0px))!important;display:flex!important;align-items:stretch!important;justify-content:space-between!important;gap:3px!important;padding:7px 5px!important;border:1px solid rgba(255,255,255,.25)!important;border-radius:23px!important;background:rgba(48,34,74,.96)!important;box-shadow:0 12px 30px rgba(43,29,70,.25),inset 0 1px rgba(255,255,255,.12)!important;backdrop-filter:blur(22px)!important}.nifty-apk-bottom-nav button{display:flex!important;flex:1 1 0!important;min-width:0!important;min-height:49px!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:4px!important;padding:4px 2px!important;border:0!important;border-radius:15px!important;background:transparent!important;color:#e1d9f0!important;font-size:8px!important;font-weight:700!important;white-space:nowrap!important;box-shadow:none!important}.nifty-apk-bottom-nav button .nav-icon{font-size:17px!important;line-height:1!important}.nifty-apk-bottom-nav button.active{background:linear-gradient(145deg,#d4c2ff,#a98ce8)!important;color:#35244f!important;box-shadow:0 5px 13px rgba(190,163,255,.23),inset 0 1px rgba(255,255,255,.6)!important}@media(max-width:600px){.app-main{padding:11px 9px 25px!important}.panel{padding:12px!important;margin-bottom:12px!important}.session-group{padding:11px!important}.session-group-head{gap:9px!important}.session-group-head strong{font-size:13px!important}.session-group-head small{font-size:10px!important}.grid,.session-grid{grid-template-columns:minmax(0,1fr)!important}.cards{grid-template-columns:repeat(2,minmax(0,1fr))!important}.sectionhead{gap:7px!important}.sectionhead h2{font-size:15px!important}.session-actions button{flex:1 1 100%!important}}\";function apply(){try{var d=document;if(!d.head)return;var st=d.getElementById('nifty-apk-direct-theme');if(!st){st=d.createElement('style');st.id='nifty-apk-direct-theme';d.head.appendChild(st)}if(st.textContent!==css)st.textContent=css;var nav=d.getElementById('nifty-apk-bottom-nav');if(!nav){nav=d.createElement('nav');nav.id='nifty-apk-bottom-nav';nav.className='nifty-apk-bottom-nav';nav.setAttribute('aria-label','NIFTY LAB sections');var items=[['session','☷','Session'],['overview','▦','Levels'],['contracts','▤','Contracts'],['analytics','▣','BEP'],['historical-sr','◷','History'],['guide','ⓘ','Guide']];items.forEach(function(it){var b=d.createElement('button');b.type='button';b.dataset.target=it[0];b.innerHTML='<span class=\"nav-icon\">'+it[1]+'</span><span>'+it[2]+'</span>';b.addEventListener('click',function(){var original=d.querySelector('[data-page-target=\"'+it[0]+'\"]');if(original)original.click();updateActive();window.scrollTo({top:0,behavior:'smooth'})});nav.appendChild(b)});d.body.appendChild(nav)}function updateActive(){var active=d.querySelector('.page-view.active');var key=active?active.getAttribute('data-page'):'session';nav.querySelectorAll('button').forEach(function(b){b.classList.toggle('active',b.dataset.target===key)})}updateActive();var tabOrder=['session','overview','contracts','analytics','historical-sr','guide'];var touchStartX=0,touchStartY=0,touchStartAt=0;function goTab(delta){var active=d.querySelector('.page-view.active');var current=active?active.getAttribute('data-page'):'session';var pos=tabOrder.indexOf(current);if(pos<0)pos=0;var next=tabOrder[Math.max(0,Math.min(tabOrder.length-1,pos+delta))];if(next===current)return;var target=d.querySelector('[data-page-target=\"'+next+'\"]');if(target){target.click();setTimeout(updateActive,100)}}var swipeArea=d.querySelector('.app-main')||d.body;swipeArea.addEventListener('touchstart',function(e){if(!e.touches||e.touches.length!==1)return;var t=e.touches[0];touchStartX=t.clientX;touchStartY=t.clientY;touchStartAt=Date.now()},{passive:true});swipeArea.addEventListener('touchend',function(e){if(!e.changedTouches||!e.changedTouches.length||!touchStartAt)return;var t=e.changedTouches[0],dx=t.clientX-touchStartX,dy=t.clientY-touchStartY,elapsed=Date.now()-touchStartAt;touchStartAt=0;if(Math.abs(dx)<65||Math.abs(dx)<Math.abs(dy)*1.25||elapsed>700)return;var el=e.target;if(el&&el.closest&&el.closest('input,textarea,select,button,a,[contenteditable=\"true\"],.tablewrap,.bep-table-wrap'))return;goTab(dx<0?1:-1)}, {passive:true});d.querySelectorAll('[data-page-target]').forEach(function(b){if(!b.dataset.apkBound){b.dataset.apkBound='1';b.addEventListener('click',function(){setTimeout(updateActive,80)})}})}catch(e){}}if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply);else apply();var count=0;var timer=setInterval(function(){apply();if(++count>20)clearInterval(timer)},500);new MutationObserver(apply).observe(document.documentElement,{childList:true,subtree:true})})();";

    private static final String STARTUP_FIX = "(function(){try{sessionStorage.setItem('nifty-lab-intro-seen','1')}catch(e){}})();";

    private static final String OUTER_THEME = "(function(){document.documentElement.style.background='#f4f2fb';document.body.style.background='#f4f2fb';})();";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(241, 244, 252));
        getWindow().setNavigationBarColor(Color.rgb(241, 244, 252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

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
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                view.evaluateJavascript(STARTUP_FIX, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(STARTUP_FIX, null);
                view.evaluateJavascript(MOBILE_THEME, null);
                view.evaluateJavascript(OUTER_THEME, null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    String message = error.getDescription() == null ? "Unable to load NIFTY LAB" : error.getDescription().toString();
                    android.widget.Toast.makeText(MainActivity.this, "NIFTY LAB load error: " + message, android.widget.Toast.LENGTH_LONG).show();
                }
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

        // Always start a clean web session so a previously interrupted intro or stale
        // WebView state cannot trap the user on the launch screen.
        webView.loadUrl(APP_URL);
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
