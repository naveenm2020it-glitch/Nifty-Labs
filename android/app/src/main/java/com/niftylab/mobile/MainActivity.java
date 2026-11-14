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
      + "var css='html,body{min-width:320px!important;background:linear-gradient(145deg,#f7f3ff,#edf3ff 48%,#fff1e9)!important;color:#29263e!important}body{margin:0!important;font-family:Inter,-apple-system,BlinkMacSystemFont,system-ui,sans-serif!important}.terminal-layout,.terminal-main{display:block!important;width:100%!important;height:auto!important;min-height:100dvh!important;overflow:visible!important;background:transparent!important}.desktop-sidebar-slot,.mobile-sidebar-layer,.drawer-backdrop,.mobile-menu-button,.top-workspace-tabs,.historical-tag,.topbar-divider,.source-integrity{display:none!important}.terminal-topbar{position:sticky!important;top:0!important;z-index:80!important;display:flex!important;min-height:58px!important;padding:8px 16px!important;background:rgba(255,255,255,.86)!important;border:0!important;backdrop-filter:blur(24px)!important}.breadcrumbs{font-size:15px!important;font-weight:900!important;letter-spacing:.04em!important;color:#29263e!important}.breadcrumbs span,.breadcrumbs svg,.breadcrumbs strong{display:none!important}.breadcrumbs:before{content:\'NIFTY LAB\'}.api-status{width:10px!important;height:10px!important;padding:0!important;border-radius:50%!important;background:#22b77b!important;box-shadow:0 0 0 4px #22b77b20!important}.api-status span,.api-status svg{display:none!important}.content-heading-row{min-height:62px!important;padding:9px 15px!important}.current-page-icon{border-radius:14px!important;background:linear-gradient(145deg,#e9ddff,#dff0ff)!important;color:#8062d0!important}.current-page-copy h1{font-size:16px!important;font-weight:850!important;color:#29263e!important}.current-page-copy p{font-size:10px!important;color:#89839b!important}.workspace-frame-wrap{display:flex!important;height:calc(100dvh - 170px)!important;min-height:590px!important;margin:0 8px!important;border:1px solid #ffffffd9!important;border-radius:25px!important;overflow:hidden!important;background:#ffffff45!important;box-shadow:0 12px 30px #4b387714!important}.workspace-frame{flex:1!important;width:100%!important;height:100%!important;border:0!important;background:transparent!important}.terminal-footer{padding:8px 13px 92px!important;font-size:9px!important;color:#89839b!important}.terminal-footer>span:last-child{display:none!important}.standalone-bottom-nav{position:fixed!important;z-index:99999!important;left:12px!important;right:12px!important;bottom:calc(9px + env(safe-area-inset-bottom,0px))!important;display:flex!important;gap:3px!important;padding:7px 5px!important;border:1px solid #ffffffeb!important;border-radius:25px!important;background:#ffffffeb!important;box-shadow:0 12px 32px #382d591f!important;backdrop-filter:blur(26px)!important}.standalone-bottom-nav button{display:flex!important;flex:1!important;min-width:0!important;min-height:50px!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:4px!important;border:0!important;border-radius:16px!important;background:transparent!important;color:#817b96!important;font-size:8px!important;font-weight:750!important}.standalone-bottom-nav .nav-icon{width:19px!important;height:19px!important;stroke-width:1.9!important}.standalone-bottom-nav button.active{background:linear-gradient(145deg,#e8dfff,#e4f0ff)!important;color:#6751b8!important;box-shadow:0 5px 13px #755bbe24!important}@media(max-width:390px){.workspace-frame-wrap{height:calc(100dvh - 164px)!important;min-height:550px!important}.standalone-bottom-nav{left:7px!important;right:7px!important}.standalone-bottom-nav button{font-size:7px!important}}';"
      + "function addStyle(doc,id,text){if(!doc||!doc.head)return;var s=doc.getElementById(id);if(!s){s=doc.createElement('style');s.id=id;doc.head.appendChild(s)}if(s.textContent!==text)s.textContent=text}"
      + "function selected(){return Array.prototype.findIndex.call(document.querySelectorAll('.top-workspace-tab'),function(b){return b.classList.contains('active')})}"
      + "function navTo(i){var a=document.querySelectorAll('.top-workspace-tab');if(i<0||i>=a.length)return;a[i].click();setTimeout(update,120);try{document.querySelector('.workspace-frame').contentWindow.scrollTo(0,0)}catch(e){}}"
      + "function update(){var n=document.getElementById('standalone-bottom-nav');if(!n)return;var i=selected();n.querySelectorAll('button').forEach(function(b,k){b.classList.toggle('active',i===k)})}"
      + "function addSwipe(doc){if(!doc||doc.__niftySwipeReady)return;doc.__niftySwipeReady=true;var sx=0,sy=0,st=0;doc.addEventListener('touchstart',function(e){if(e.touches&&e.touches.length===1){sx=e.touches[0].clientX;sy=e.touches[0].clientY;st=Date.now()}},{passive:true});doc.addEventListener('touchend',function(e){if(!st||!e.changedTouches||!e.changedTouches.length)return;var dx=e.changedTouches[0].clientX-sx,dy=e.changedTouches[0].clientY-sy,dt=Date.now()-st;st=0;if(Math.abs(dx)<65||Math.abs(dx)<Math.abs(dy)*1.25||dt>750)return;var t=e.target;if(t&&t.closest&&t.closest('input,textarea,select,button,a,[contenteditable=true],.tablewrap,.bep-table-wrap'))return;var i=selected();if(i<0)i=0;navTo(i+(dx<0?1:-1))},{passive:true})}"
      + "function attach(){try{var frame=document.querySelector('.workspace-frame');if(frame&&frame.contentDocument){var d=frame.contentDocument;addStyle(d,'nifty-standalone-inner','html,body{min-width:320px!important;width:100%!important;overflow-x:hidden!important;background:linear-gradient(155deg,#f8f5ff,#eef4ff 48%,#fff2ec)!important;color:#29263e!important;font-family:Inter,-apple-system,BlinkMacSystemFont,system-ui,sans-serif!important}body{padding-bottom:16px!important}.app-shell{display:block!important;min-height:100vh!important;width:100%!important;background:transparent!important}.app-shell>.sidebar,.app-main>.app-topbar,.app-main>header.top,#dataNotice{display:none!important}.app-main{display:block!important;width:100%!important;max-width:none!important;min-width:0!important;margin:0!important;padding:13px 10px 25px!important;background:transparent!important}.page-intro{margin:0 4px 13px!important}.page-intro h2{font-size:17px!important;font-weight:850!important;color:#29263e!important;letter-spacing:-.04em!important}.page-intro p{font-size:11px!important;color:#89839b!important}.panel{padding:15px!important;margin:0 0 13px!important;border:1px solid #ffffffed!important;border-radius:24px!important;background:linear-gradient(145deg,#fffffff2,#faf8ffd9)!important;box-shadow:0 12px 30px #4a3a7413,inset 0 1px #ffffff!important;backdrop-filter:blur(18px)!important;transition:transform .18s,box-shadow .18s!important}.panel:active,.metric:active,.level-card:active,.hlc-card:active{transform:scale(.992)!important}.sectionhead h2,h2,h3{color:#29263e!important;font-weight:820!important;letter-spacing:-.035em!important}.muted,.sub,.field-hint,label,.eyebrow{color:#89839b!important}.session-group{padding:14px!important;margin:11px 0!important;border:1px solid #ded5ef!important;border-radius:21px!important;background:linear-gradient(145deg,#fffffff7,#f5f2ffe8)!important;box-shadow:0 8px 20px #533d840e!important}.session-group-head{gap:10px!important;margin-bottom:12px!important}.session-group-head strong{font-size:14px!important;color:#29263e!important}.session-step,.info-icon{border-radius:14px!important;background:linear-gradient(145deg,#e9e1ff,#e2efff)!important;color:#765bc9!important}.info-field{padding:12px!important;border:1px solid #ded5ef!important;border-radius:18px!important;background:#ffffffdf!important;box-shadow:0 5px 14px #4b3a7809!important}.grid,.session-grid{gap:10px!important}.cards{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:10px!important}.metric{position:relative!important;min-width:0!important;min-height:98px!important;padding:14px!important;border:1px solid #fffffffa!important;border-radius:21px!important;background:linear-gradient(145deg,#fff,#f1edff)!important;box-shadow:0 9px 21px #4b3a7813!important;overflow:hidden!important}.metric:nth-child(4n+2){background:linear-gradient(145deg,#fff,#fff0e7)!important}.metric:nth-child(4n+3){background:linear-gradient(145deg,#fff,#eaf7f1)!important}.metric:nth-child(4n+4){background:linear-gradient(145deg,#fff,#edf3ff)!important}.metric small{font-size:10px!important;color:#817b94!important}.metric strong{display:block!important;margin-top:6px!important;font-size:clamp(20px,5.2vw,26px)!important;font-weight:850!important;letter-spacing:-.045em!important;color:#29263e!important;overflow-wrap:anywhere!important}.sr-panel{border-radius:26px!important;background:linear-gradient(145deg,#fff,#f3edff 56%,#fff0e9)!important}.sr-legend{gap:7px!important}.legend-pill{border-radius:999px!important;padding:6px 10px!important;background:#ffffffcc!important;border:1px solid #eee8f5!important}.level-grid,.hlc-grid{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:10px!important}.level-card,.hlc-card{min-width:0!important;padding:14px!important;border:1px solid #fffffffa!important;border-radius:21px!important;box-shadow:0 9px 20px #4b3a7810!important;transition:transform .18s,box-shadow .18s!important}.level-card.support{background:linear-gradient(145deg,#fff,#eaf7f1)!important}.level-card.resistance{background:linear-gradient(145deg,#fff,#fff0e8)!important}.level-value{font-size:clamp(21px,5.5vw,28px)!important;font-weight:900!important;color:#29263e!important;overflow-wrap:anywhere!important}.level-kind,.level-formula,.level-math{color:#88829a!important;font-size:10px!important}.hlc-card{background:linear-gradient(145deg,#fff,#f1edff)!important}.hlc-card.put{background:linear-gradient(145deg,#fff,#fff0e8)!important}.hlc-title{font-size:12px!important;font-weight:850!important;color:#29263e!important}.ohlc-cell{padding:9px!important;border-radius:13px!important;background:#ffffffc9!important;border:1px solid #e6e0f2!important}.ohlc-cell strong{font-size:15px!important;color:#29263e!important}.bep-summary{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:9px!important}.bep-metric{padding:12px!important;border:1px solid #ffffffed!important;border-radius:18px!important;background:linear-gradient(145deg,#fff,#f3efff)!important;box-shadow:0 6px 16px #4b3a780d!important}.bep-value{font-size:20px!important;font-weight:850!important;color:#29263e!important}.calc-note,.formulanote,.session-note,.notice,.status,.source-box{border:1px solid #ded5ef!important;border-radius:18px!important;background:#ffffffd4!important;box-shadow:0 6px 16px #4b3a780a!important;padding:12px!important}.source-dot{background:#25b987!important}.bep-table-wrap,.tablewrap{max-width:100%!important;overflow-x:auto!important;border:1px solid #ded5ef!important;border-radius:18px!important;background:#ffffffed!important;box-shadow:0 7px 18px #4b3a780d!important}.bep-table,table{font-size:10px!important;min-width:560px!important}.bep-table th,table th{background:#f0edfa!important;color:#57516c!important;font-weight:800!important}.bep-table td,.bep-table th,table td,table th{padding:9px 8px!important;border-color:#ece8f4!important}.input,input,select,textarea{max-width:100%!important;min-width:0!important;min-height:48px!important;padding:11px 13px!important;border:1px solid #dedbed!important;border-radius:15px!important;background:#fffffff5!important;color:#29263e!important;font-size:15px!important}.input:focus,input:focus,select:focus,textarea:focus{border-color:#a99ae5!important;box-shadow:0 0 0 3px #8f77d522!important}.actions{gap:8px!important}.actions button,button{border-radius:15px!important;transition:transform .18s,box-shadow .18s,filter .18s!important}.actions button:not(.secondary),button:not(.secondary){background:linear-gradient(120deg,#7961d5,#a38adf)!important;border-color:#755bc740!important;color:#fff!important;box-shadow:0 7px 16px #7158be30!important}.actions button.secondary,button.secondary{background:#fffffff0!important;color:#514a68!important;border:1px solid #e2deef!important;box-shadow:0 4px 11px #4b3a780b!important}.button:active,button:active{transform:scale(.97)!important}.fixed-visual-bars,.chart-depth-stage,.fixed-events-scroll{border-radius:18px!important;overflow:hidden!important;background:#ffffffc9!important;border:1px solid #ded5ef!important;padding:12px!important}.sr-bar,.fixed-visual-track{border-radius:999px!important;overflow:hidden!important}.bep-strike-dialog,.bep-dialog-shell{border-radius:24px!important;background:#fff!important;box-shadow:0 18px 50px #281f7040!important}.bep-dialog-head,.bep-dialog-foot{padding:14px!important}.bep-strike-list{max-height:55vh!important;overflow:auto!important}@media(max-width:600px){.grid,.session-grid,.cards,.level-grid,.hlc-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important}.grid .wide{grid-column:1/-1!important}.session-actions{display:flex!important;flex-wrap:wrap!important;gap:8px!important}.session-actions button{flex:1 1 100%!important;min-height:46px!important}.panel{padding:13px!important}.metric{padding:12px!important;min-height:91px!important}}@media(max-width:370px){.grid,.session-grid,.cards,.level-grid,.hlc-grid,.bep-summary{grid-template-columns:minmax(0,1fr)!important}.panel{padding:11px!important}}@media(hover:hover){.panel:hover,.metric:hover,.level-card:hover,.hlc-card:hover{transform:translateY(-2px)!important;box-shadow:0 14px 28px #4a3a741c!important}}');addSwipe(d)}var n=document.getElementById('standalone-bottom-nav');if(!n){n=document.createElement('nav');n.id='standalone-bottom-nav';n.className='standalone-bottom-nav';n.setAttribute('aria-label','NIFTY LAB tabs');var items=[['☷','Session'],['▦','Levels'],['▤','Contracts'],['▣','BEP'],['◷','History'],['ⓘ','Guide']];items.forEach(function(x,i){var b=document.createElement('button');b.type='button';var source=document.querySelectorAll('.top-workspace-tab')[i];var svg=source&&source.querySelector('svg');if(svg){var ic=svg.cloneNode(true);ic.classList.add('nav-icon');b.appendChild(ic)}else{var ic=document.createElement('span');ic.className='nav-icon';ic.textContent=x[0];b.appendChild(ic)}var label=document.createElement('span');label.textContent=x[1];b.appendChild(label);b.addEventListener('click',function(){navTo(i)});n.appendChild(b)};document.body.appendChild(n)}addStyle(document,'nifty-standalone-outer',css);update()}catch(e){}}"
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
