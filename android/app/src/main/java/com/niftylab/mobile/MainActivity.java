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

    private static final String MOBILE_UI =
      "(function(){var outer=\"html,body{min-width:320px!important;background:linear-gradient(135deg,#f0eafa,#e7f0ef 30%,#f8e9e6 60%,#f7edcf 82%,#e7f0e7)!important;color:#24242a!important}body{margin:0!important;font-family:Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif!important}.terminal-layout,.terminal-main{display:block!important;width:100%!important;height:auto!important;min-height:100dvh!important;overflow:visible!important;background:transparent!important}.desktop-sidebar-slot,.mobile-sidebar-layer,.drawer-backdrop,.mobile-menu-button,.top-workspace-tabs,.historical-tag,.topbar-divider,.source-integrity{display:none!important}.theme-toggle,.theme-switch,.theme-button,.dark-mode-toggle,.dark-toggle,[data-theme-toggle],button[aria-label*=theme i],button[title*=theme i],button[aria-label*=dark i],button[title*=dark i]{display:none!important}.terminal-topbar{position:sticky!important;top:0!important;z-index:80!important;box-sizing:border-box!important;height:48px!important;min-height:48px!important;padding:5px 12px!important;display:flex!important;align-items:center!important;overflow:hidden!important;background:#faf9f9d9!important;border:0!important;backdrop-filter:blur(22px)!important}.breadcrumbs{font-size:14px!important;font-weight:850!important;color:#28272d!important}.breadcrumbs span,.breadcrumbs svg,.breadcrumbs strong{display:none!important}.breadcrumbs:before{content:'NIFTY LAB'}.api-status{width:9px!important;height:9px!important;padding:0!important;border-radius:50%!important;background:#24ad78!important;box-shadow:0 0 0 4px #24ad7820!important}.api-status span,.api-status svg{display:none!important}.content-heading-row{box-sizing:border-box!important;min-height:52px!important;padding:5px 12px!important;gap:9px!important;align-items:center!important}.current-page-icon{border-radius:15px!important;background:linear-gradient(145deg,#e7def5,#e2efeb)!important;color:#393640!important}.current-page-copy h1{font-size:16px!important;color:#29272e!important}.current-page-copy p{font-size:10px!important;color:#77737b!important}.workspace-frame-wrap{display:flex!important;height:calc(100dvh - 205px)!important;min-height:360px!important;max-height:calc(100dvh - 190px)!important;margin:0 7px!important;border:1px solid #ffffffb8!important;border-radius:25px!important;overflow:hidden!important;background:#ffffff33!important;box-shadow:0 12px 30px #51435412!important}.workspace-frame{flex:1!important;width:100%!important;height:100%!important;border:0!important;background:transparent!important}.terminal-footer{display:none!important}.terminal-footer>span:last-child{display:none!important} .standalone-bottom-nav{position:fixed!important;z-index:99999!important;left:9px!important;right:9px!important;bottom:calc(6px + env(safe-area-inset-bottom,0px))!important;display:flex!important;gap:2px!important;padding:5px 4px!important;border:1px solid #ffffffd9!important;border-radius:25px!important;background:#faf9f8e8!important;box-shadow:0 12px 30px #29232d20,inset 0 1px #fff!important;backdrop-filter:blur(24px)!important}.standalone-bottom-nav button{display:flex!important;flex:1!important;min-width:0!important;min-height:50px!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:4px!important;border:0!important;border-radius:16px!important;background:transparent!important;color:#77737b!important;font-size:8px!important;font-weight:750!important}.standalone-bottom-nav .nav-icon{width:19px!important;height:19px!important;display:grid!important;place-items:center!important}.standalone-bottom-nav .nav-icon svg{width:19px!important;height:19px!important;stroke:currentColor!important;fill:none!important;stroke-width:1.8!important;stroke-linecap:round!important;stroke-linejoin:round!important}.standalone-bottom-nav button.active{background:linear-gradient(145deg,#e8def5,#e2efeb)!important;color:#29272e!important;box-shadow:0 5px 13px #4c3e501c!important}@media(max-width:390px){.workspace-frame-wrap{height:calc(100dvh - 148px)!important;min-height:540px!important}.standalone-bottom-nav{left:7px!important;right:7px!important}.standalone-bottom-nav button{font-size:7px!important}}\",inner=\"html,body{min-width:320px!important;width:100%!important;overflow-x:hidden!important;background:linear-gradient(135deg,#f0eafa,#e7f0ef 30%,#f8e9e6 60%,#f7edcf 82%,#e7f0e7)!important;color:#24242a!important;font-family:Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif!important}body{padding:0 0 18px!important}.app-shell{display:block!important;min-height:100vh!important;width:100%!important;background:transparent!important}.app-shell>.sidebar,.app-main>.app-topbar,.app-main>header.top,#dataNotice{display:none!important}.app-main{display:block!important;width:100%!important;max-width:none!important;min-width:0!important;margin:0!important;padding:7px 8px 12px!important;background:transparent!important}.page-intro{margin:0 3px 8px!important}.page-intro h2{font-size:17px!important;font-weight:850!important;color:#28272d!important;letter-spacing:-.04em!important}.page-intro p{font-size:11px!important;color:#77737b!important}.panel{position:relative!important;padding:11px!important;margin:0 0 9px!important;border:1px solid #ffffffd9!important;border-radius:23px!important;background:linear-gradient(145deg,#ffffffe0,#faf8f7b3)!important;box-shadow:0 12px 28px #3e35400e,inset 0 1px #ffffff!important;backdrop-filter:blur(20px)!important;transition:transform .2s ease,box-shadow .2s ease!important}.panel:active,.metric:active,.level-card:active,.hlc-card:active{transform:scale(.992)!important}.panel .panel{padding:13px!important;border:1px solid #ffffffc9!important;border-radius:19px!important;background:#ffffff94!important;box-shadow:0 6px 16px #4035400a!important}.sectionhead{margin-bottom:12px!important}.sectionhead h2,h1,h2,h3,h4{color:#28272d!important;font-weight:820!important;letter-spacing:-.035em!important}.muted,.sub,.field-hint,label,.eyebrow{color:#77737b!important}.session-group{padding:14px!important;margin:11px 0!important;border:1px solid #ffffffd9!important;border-radius:20px!important;background:linear-gradient(145deg,#ffffffe0,#f6f3f9bd)!important;box-shadow:0 8px 20px #4035400b!important}.session-group-head{gap:10px!important;margin-bottom:12px!important}.session-group-head strong{font-size:14px!important;color:#28272d!important}.session-step,.info-icon{border-radius:14px!important;background:linear-gradient(145deg,#e9e0f4,#e2eeeb)!important;color:#393640!important}.info-field{padding:12px!important;border:1px solid #ffffffd9!important;border-radius:17px!important;background:#ffffffb8!important;box-shadow:0 5px 14px #40354008!important}.grid,.session-grid{gap:10px!important}.cards{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:10px!important}.metric{position:relative!important;min-width:0!important;min-height:98px!important;padding:14px!important;border:1px solid #ffffffde!important;border-radius:20px!important;background:linear-gradient(145deg,#fff,#eee8f6)!important;box-shadow:0 9px 20px #40354010!important;overflow:hidden!important;transition:transform .2s ease,box-shadow .2s ease!important}.metric:nth-child(4n+2){background:linear-gradient(145deg,#fff,#f8e9df)!important}.metric:nth-child(4n+3){background:linear-gradient(145deg,#fff,#e5f0e9)!important}.metric:nth-child(4n+4){background:linear-gradient(145deg,#fff,#f5edcf)!important}.metric small{font-size:10px!important;color:#77737b!important}.metric strong{display:block!important;margin-top:6px!important;font-size:clamp(20px,5.2vw,26px)!important;font-weight:850!important;letter-spacing:-.045em!important;color:#25242a!important;overflow-wrap:anywhere!important}.level-grid,.hlc-grid{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:10px!important}.level-card,.hlc-card{min-width:0!important;padding:14px!important;border:1px solid #ffffffdf!important;border-radius:20px!important;box-shadow:0 9px 19px #4035400e!important;transition:transform .2s ease,box-shadow .2s ease!important}.level-card.support{background:linear-gradient(145deg,#fff,#e5f0e9)!important}.level-card.resistance{background:linear-gradient(145deg,#fff,#f8e8df)!important}.level-value{font-size:clamp(21px,5.5vw,28px)!important;font-weight:900!important;color:#25242a!important;overflow-wrap:anywhere!important}.level-kind,.level-formula,.level-math{color:#77737b!important;font-size:10px!important}.hlc-card{background:linear-gradient(145deg,#fff,#eee8f6)!important}.hlc-card.put{background:linear-gradient(145deg,#fff,#f8e9df)!important}.hlc-title{font-size:12px!important;font-weight:850!important;color:#28272d!important}.ohlc-cell{padding:9px!important;border-radius:13px!important;background:#ffffffb8!important;border:1px solid #ffffffd9!important}.ohlc-cell strong{font-size:15px!important;color:#25242a!important}.bep-summary{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:9px!important}.bep-metric{padding:12px!important;border:1px solid #ffffffd9!important;border-radius:18px!important;background:linear-gradient(145deg,#fff,#eee8f6)!important;box-shadow:0 6px 16px #4035400b!important}.bep-value{font-size:20px!important;font-weight:850!important;color:#25242a!important}.calc-note,.formulanote,.session-note,.notice,.status,.source-box{border:1px solid #ffffffd9!important;border-radius:17px!important;background:#ffffffb8!important;box-shadow:0 6px 16px #40354008!important;padding:12px!important}.source-dot{background:#24ad78!important}.bep-table-wrap,.tablewrap{max-width:100%!important;overflow-x:auto!important;border:1px solid #ffffffd9!important;border-radius:18px!important;background:#ffffffc9!important;box-shadow:0 7px 18px #4035400b!important}.bep-table,table{font-size:10px!important;min-width:560px!important}.bep-table th,table th{background:#f1edf4!important;color:#57535c!important;font-weight:800!important}.bep-table td,.bep-table th,table td,table th{padding:9px 8px!important;border-color:#e9e3e9!important}.input,input,select,textarea{max-width:100%!important;min-width:0!important;min-height:48px!important;padding:11px 13px!important;border:1px solid #e6e0e8!important;border-radius:15px!important;background:#ffffffd9!important;color:#28272d!important;font-size:15px!important}.input:focus,input:focus,select:focus,textarea:focus{border-color:#c4a9a1!important;box-shadow:0 0 0 3px #d5b9aa28!important}.actions{gap:8px!important}.actions button,button{border-radius:15px!important;transition:transform .18s ease,box-shadow .18s ease,filter .18s ease!important}.actions button:not(.secondary),button:not(.secondary){background:linear-gradient(120deg,#28272d,#45434a)!important;border-color:#28272d!important;color:#fff!important;box-shadow:0 7px 16px #28272d24!important}.actions button.secondary,button.secondary{background:#ffffffc9!important;color:#403d45!important;border:1px solid #ffffffd9!important;box-shadow:0 4px 11px #40354009!important}.actions button:active,button:active{transform:scale(.97)!important}.fixed-visual-bars,.chart-depth-stage,.fixed-events-scroll{border-radius:18px!important;overflow:hidden!important;background:#ffffffa8!important;border:1px solid #ffffffd9!important;padding:12px!important}.sr-bar,.fixed-visual-track{border-radius:999px!important;overflow:hidden!important}.bep-strike-dialog,.bep-dialog-shell{border-radius:24px!important;background:#fff!important;box-shadow:0 18px 50px #28272d40!important}.bep-dialog-head,.bep-dialog-foot{padding:14px!important}.bep-strike-list{max-height:55vh!important;overflow:auto!important}@media(max-width:600px){.grid,.session-grid,.cards,.level-grid,.hlc-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important}.grid .wide{grid-column:1/-1!important}.session-actions{display:flex!important;flex-wrap:wrap!important;gap:8px!important}.session-actions button{flex:1 1 100%!important;min-height:46px!important}.panel{padding:13px!important}.metric{padding:12px!important;min-height:91px!important}}@media(max-width:370px){.grid,.session-grid,.cards,.level-grid,.hlc-grid,.bep-summary{grid-template-columns:minmax(0,1fr)!important}.panel{padding:11px!important}}@media(hover:hover){.panel:hover,.metric:hover,.level-card:hover,.hlc-card:hover{transform:translateY(-2px)!important;box-shadow:0 14px 28px #4035401a!important}}",icons=[\"<svg viewBox=\\\"0 0 24 24\\\"><rect x=\\\"3\\\" y=\\\"3\\\" width=\\\"8\\\" height=\\\"8\\\" rx=\\\"2\\\"/><rect x=\\\"13\\\" y=\\\"3\\\" width=\\\"8\\\" height=\\\"8\\\" rx=\\\"2\\\"/><rect x=\\\"3\\\" y=\\\"13\\\" width=\\\"8\\\" height=\\\"8\\\" rx=\\\"2\\\"/><rect x=\\\"13\\\" y=\\\"13\\\" width=\\\"8\\\" height=\\\"8\\\" rx=\\\"2\\\"/></svg>\",\"<svg viewBox=\\\"0 0 24 24\\\"><path d=\\\"M4 19V5M4 19h17\\\"/><path d=\\\"m7 15 4-5 3 3 5-7\\\"/></svg>\",\"<svg viewBox=\\\"0 0 24 24\\\"><rect x=\\\"3\\\" y=\\\"5\\\" width=\\\"18\\\" height=\\\"14\\\" rx=\\\"2\\\"/><path d=\\\"M3 10h18M8 5v14\\\"/></svg>\",\"<svg viewBox=\\\"0 0 24 24\\\"><rect x=\\\"4\\\" y=\\\"3\\\" width=\\\"16\\\" height=\\\"18\\\" rx=\\\"2\\\"/><path d=\\\"M8 7h8M8 11h2m4 0h2m-8 4h2m4 0h2\\\"/></svg>\",\"<svg viewBox=\\\"0 0 24 24\\\"><path d=\\\"M3 12a9 9 0 1 0 2.6-6.4L3 8\\\"/><path d=\\\"M3 3v5h5M12 7v5l3 2\\\"/></svg>\",\"<svg viewBox=\\\"0 0 24 24\\\"><circle cx=\\\"12\\\" cy=\\\"12\\\" r=\\\"9\\\"/><path d=\\\"M12 11v5m0-8h.01\\\"/></svg>\"];function style(d,id,t){if(!d||!d.head)return;var s=d.getElementById(id);if(!s){s=d.createElement('style');s.id=id;d.head.appendChild(s)}if(s.textContent!==t)s.textContent=t}function tabs(){return document.querySelectorAll('.top-workspace-tab')}function active(){return Array.prototype.findIndex.call(tabs(),function(x){return x.classList.contains('active')})}function go(i){var a=tabs();if(i<0||i>=a.length)return;a[i].click();setTimeout(sync,120);try{document.querySelector('.workspace-frame').contentWindow.scrollTo(0,0)}catch(e){}}function sync(){var n=document.getElementById('standalone-bottom-nav');if(n){var i=active();n.querySelectorAll('button').forEach(function(b,k){b.classList.toggle('active',i===k)})}}function swipe(d){if(!d||d.__niftySwipe)return;d.__niftySwipe=1;var x=0,y=0,t=0;d.addEventListener('touchstart',function(e){if(e.touches.length===1){x=e.touches[0].clientX;y=e.touches[0].clientY;t=Date.now()}},{passive:true});d.addEventListener('touchend',function(e){if(!t||!e.changedTouches.length)return;var dx=e.changedTouches[0].clientX-x,dy=e.changedTouches[0].clientY-y,dt=Date.now()-t;t=0;if(Math.abs(dx)<65||Math.abs(dx)<Math.abs(dy)*1.25||dt>750)return;var q=e.target;if(q&&q.closest&&q.closest('input,textarea,select,button,a,[contenteditable=true],.tablewrap,.bep-table-wrap'))return;var i=active();go((i<0?0:i)+(dx<0?1:-1))},{passive:true})}function attach(){try{var f=document.querySelector('.workspace-frame');if(f&&f.contentDocument){style(f.contentDocument,'nifty-reference-theme',inner);swipe(f.contentDocument)}var n=document.getElementById('standalone-bottom-nav');if(!n){n=document.createElement('nav');n.id='standalone-bottom-nav';n.className='standalone-bottom-nav';n.setAttribute('aria-label','NIFTY LAB navigation');['Session','Levels','Contracts','BEP','History','Guide'].forEach(function(label,i){var b=document.createElement('button'),ic=document.createElement('span'),tx=document.createElement('span');b.type='button';ic.className='nav-icon';ic.innerHTML=icons[i];tx.textContent=label;b.appendChild(ic);b.appendChild(tx);b.addEventListener('click',function(){go(i)});n.appendChild(b)});document.body.appendChild(n)}style(document,'nifty-reference-outer',outer);sync()}catch(e){}}if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',attach);else attach();var n=0,timer=setInterval(function(){attach();if(++n>30)clearInterval(timer)},500);var f=document.querySelector('.workspace-frame');if(f)f.addEventListener('load',attach);new MutationObserver(attach).observe(document.documentElement,{childList:true,subtree:true})})();";

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
