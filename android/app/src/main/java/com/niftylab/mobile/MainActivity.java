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

    private static final String MOBILE_UI = "(function(){"
      + "var css='html,body{min-width:320px!important;background:#f4f2fb!important;color:#302b47!important}body{margin:0!important;font-family:Inter,system-ui,sans-serif!important;background:radial-gradient(ellipse at 0 0,rgba(207,194,255,.3),transparent 42%),linear-gradient(145deg,#faf8ff,#f0effa 65%,#f9f0f5)!important}.terminal-layout{display:block!important;width:100%!important;height:auto!important;min-height:100dvh!important;overflow:visible!important;background:transparent!important}.desktop-sidebar-slot,.mobile-sidebar-layer,.drawer-backdrop{display:none!important}.terminal-main{display:block!important;width:100%!important;min-width:0!important;min-height:100dvh!important;height:auto!important;overflow:visible!important}.terminal-topbar{position:sticky!important;top:0!important;z-index:80!important;display:flex!important;flex-wrap:wrap!important;min-height:54px!important;padding:7px 12px!important;gap:7px!important;background:rgba(255,255,255,.94)!important;border-bottom:1px solid #e6e0f2!important;backdrop-filter:blur(18px)!important}.topbar-left{min-width:0!important;flex:1!important}.mobile-menu-button{display:none!important}.breadcrumbs{font-size:11px!important}.historical-tag,.topbar-divider{display:none!important}.topbar-actions{gap:8px!important}.api-status span{display:none!important}.top-workspace-tabs{order:3!important;display:flex!important;flex:0 0 100%!important;width:100%!important;max-width:100%!important;overflow-x:auto!important;justify-content:flex-start!important;gap:6px!important;padding:3px 0 5px!important;scrollbar-width:none!important}.top-workspace-tabs::-webkit-scrollbar{display:none!important}.top-workspace-tab{display:flex!important;flex:0 0 auto!important;align-items:center!important;gap:6px!important;min-height:42px!important;padding:8px 12px!important;border:1px solid #e6e0f2!important;border-radius:15px!important;background:#fff!important;color:#706985!important;font-size:11px!important;white-space:nowrap!important;box-shadow:0 3px 10px rgba(80,61,126,.05)!important}.top-workspace-tab.active{background:linear-gradient(135deg,#e6dcff,#d8eaff)!important;border-color:#c9b8f3!important;color:#6347bd!important;box-shadow:0 6px 14px rgba(126,97,190,.14)!important}.content-heading-row{min-height:66px!important;padding:9px 13px!important;gap:10px!important}.current-page-icon{width:37px!important;height:37px!important;flex-basis:37px!important;border-radius:14px!important;background:linear-gradient(145deg,#eee7ff,#e3e9ff)!important;color:#8064cf!important}.current-page-copy h1{font-size:15px!important;color:#302b47!important}.current-page-copy p{font-size:10px!important;color:#817b96!important}.source-integrity{display:none!important}.workspace-frame-wrap{display:flex!important;position:relative!important;width:auto!important;height:calc(100dvh - 188px)!important;min-height:570px!important;max-height:none!important;margin:0 8px!important;border:1px solid rgba(255,255,255,.9)!important;border-radius:20px!important;overflow:hidden!important;background:#f4f2fb!important;box-shadow:0 10px 28px rgba(75,56,119,.08)!important}.workspace-frame{display:block!important;flex:1!important;width:100%!important;height:100%!important;min-height:0!important;border:0!important;background:transparent!important}.terminal-footer{display:flex!important;padding:8px 12px 88px!important;font-size:9px!important;color:#817b96!important}.terminal-footer>span:last-child{display:none!important}.standalone-bottom-nav{position:fixed!important;z-index:99999!important;left:10px!important;right:10px!important;bottom:calc(8px + env(safe-area-inset-bottom,0px))!important;display:flex!important;align-items:stretch!important;gap:3px!important;padding:7px 5px!important;border:1px solid rgba(255,255,255,.25)!important;border-radius:22px!important;background:rgba(48,34,74,.96)!important;box-shadow:0 12px 30px rgba(43,29,70,.25),inset 0 1px rgba(255,255,255,.12)!important;backdrop-filter:blur(22px)!important}.standalone-bottom-nav button{display:flex!important;flex:1 1 0!important;min-width:0!important;min-height:48px!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:4px!important;padding:4px 1px!important;border:0!important;border-radius:14px!important;background:transparent!important;color:#e1d9f0!important;font-size:8px!important;font-weight:700!important;white-space:nowrap!important;box-shadow:none!important}.standalone-bottom-nav button .nav-icon{font-size:17px!important;line-height:1!important}.standalone-bottom-nav button.active{background:linear-gradient(145deg,#d4c2ff,#a98ce8)!important;color:#35244f!important;box-shadow:0 5px 13px rgba(190,163,255,.23)!important}@media(max-width:390px){.top-workspace-tab{padding:7px 10px!important;font-size:10px!important}.content-heading-row{min-height:60px!important}.workspace-frame-wrap{height:calc(100dvh - 178px)!important}}';"
      + "function addStyle(doc,id,text){if(!doc||!doc.head)return;var s=doc.getElementById(id);if(!s){s=doc.createElement('style');s.id=id;doc.head.appendChild(s)}if(s.textContent!==text)s.textContent=text}"
      + "function selected(){return Array.prototype.findIndex.call(document.querySelectorAll('.top-workspace-tab'),function(b){return b.classList.contains('active')})}"
      + "function navTo(i){var a=document.querySelectorAll('.top-workspace-tab');if(i<0||i>=a.length)return;a[i].click();setTimeout(update,120);try{document.querySelector('.workspace-frame').contentWindow.scrollTo(0,0)}catch(e){}}"
      + "function update(){var n=document.getElementById('standalone-bottom-nav');if(!n)return;var i=selected();n.querySelectorAll('button').forEach(function(b,k){b.classList.toggle('active',i===k)})}"
      + "function addSwipe(doc){if(!doc||doc.__niftySwipeReady)return;doc.__niftySwipeReady=true;var sx=0,sy=0,st=0;doc.addEventListener('touchstart',function(e){if(e.touches&&e.touches.length===1){sx=e.touches[0].clientX;sy=e.touches[0].clientY;st=Date.now()}},{passive:true});doc.addEventListener('touchend',function(e){if(!st||!e.changedTouches||!e.changedTouches.length)return;var dx=e.changedTouches[0].clientX-sx,dy=e.changedTouches[0].clientY-sy,dt=Date.now()-st;st=0;if(Math.abs(dx)<65||Math.abs(dx)<Math.abs(dy)*1.25||dt>750)return;var t=e.target;if(t&&t.closest&&t.closest('input,textarea,select,button,a,[contenteditable=true],.tablewrap,.bep-table-wrap'))return;var i=selected();if(i<0)i=0;navTo(i+(dx<0?1:-1))},{passive:true})}"
      + "function attach(){try{var frame=document.querySelector('.workspace-frame');if(frame&&frame.contentDocument){var d=frame.contentDocument;addStyle(d,'nifty-standalone-inner','html,body{min-width:320px!important;width:100%!important;overflow-x:hidden!important;background:radial-gradient(ellipse at 0 0,rgba(207,194,255,.24),transparent 42%),linear-gradient(145deg,#faf8ff,#f0effa 65%,#f9f0f5)!important;color:#302b47!important}body{padding:0 0 15px!important}.app-shell{display:block!important;min-height:100vh!important;width:100%!important;background:transparent!important}.app-shell>.sidebar,.app-main>.app-topbar,.app-main>header.top,#dataNotice{display:none!important}.app-main{display:block!important;width:100%!important;max-width:none!important;min-width:0!important;margin:0!important;padding:12px 9px 20px!important;background:transparent!important}.page-intro{margin:0 3px 12px!important}.page-intro h2{display:none!important}.page-intro p{font-size:11px!important;color:#817b96!important}.panel{padding:12px!important;margin:0 0 12px!important;border:1px solid rgba(255,255,255,.96)!important;border-radius:21px!important;background:rgba(255,255,255,.8)!important;box-shadow:0 9px 24px rgba(75,56,119,.07)!important;backdrop-filter:blur(16px)!important}.session-group{padding:11px!important;margin:10px 0!important;border:1px solid rgba(169,151,212,.24)!important;border-radius:18px!important;background:linear-gradient(145deg,#fff,#f6f2ff)!important;box-shadow:0 6px 16px rgba(83,61,132,.05)!important}.session-step{width:38px!important;height:38px!important;flex-basis:38px!important;border-radius:13px!important;background:linear-gradient(145deg,#eee7ff,#e3e9ff)!important;color:#8064cf!important}.session-group-head strong,h1,h2,h3,h4,label{color:#302b47!important}.session-group-head small,.muted,.sub,.field-hint{color:#817b96!important}.info-field{padding:11px!important;border:1px solid rgba(161,148,194,.23)!important;border-radius:16px!important;background:rgba(255,255,255,.84)!important}.cards,.grid{gap:9px!important}.metric,.level-card,.hlc-card,.bep-metric,.source-box,.status,.notice{border-radius:17px!important;box-shadow:0 7px 16px rgba(77,58,120,.06)!important}.input, input,select,textarea{max-width:100%!important;min-width:0!important;min-height:48px!important;border:1px solid rgba(158,145,194,.3)!important;border-radius:14px!important;background:#fff!important;color:#302b47!important;font-size:15px!important}.tablewrap,.bep-table-wrap{max-width:100%!important;overflow-x:auto!important;border-radius:15px!important}table{min-width:540px!important}.session-actions{display:flex!important;flex-wrap:wrap!important;gap:8px!important}.session-actions button{flex:1 1 130px!important;min-height:44px!important}@media(max-width:600px){.grid,.session-grid{grid-template-columns:minmax(0,1fr)!important}.cards{grid-template-columns:repeat(2,minmax(0,1fr))!important}.session-actions button{flex:1 1 100%!important}}');addSwipe(d)}var n=document.getElementById('standalone-bottom-nav');if(!n){n=document.createElement('nav');n.id='standalone-bottom-nav';n.className='standalone-bottom-nav';n.setAttribute('aria-label','NIFTY LAB tabs');var items=[['☷','Session'],['▦','Levels'],['▤','Contracts'],['▣','BEP'],['◷','History'],['ⓘ','Guide']];items.forEach(function(x,i){var b=document.createElement('button');b.type='button';b.innerHTML='<span class=\"nav-icon\">'+x[0]+'</span><span>'+x[1]+'</span>';b.addEventListener('click',function(){navTo(i)});n.appendChild(b)});document.body.appendChild(n)}addStyle(document,'nifty-standalone-outer',css);update()}catch(e){}}"
      + "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',attach);else attach();var n=0;var timer=setInterval(function(){attach();if(++n>30)clearInterval(timer)},500);var frame=document.querySelector('.workspace-frame');if(frame)frame.addEventListener('load',attach);new MutationObserver(attach).observe(document.documentElement,{childList:true,subtree:true});})();";

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
        settings.setAllowFileAccess(false);
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
                view.evaluateJavascript("try{sessionStorage.setItem('nifty-lab-intro-seen','1');var x=document.querySelector('.intro-skip');if(x)x.click()}catch(e){}", null);
                view.evaluateJavascript(MOBILE_UI, null);
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
