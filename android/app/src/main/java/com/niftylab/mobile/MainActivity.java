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

    private static final String MOBILE_UI = new String(android.util.Base64.decode("KGZ1bmN0aW9uKCl7dmFyIGE9Imh0bWwsYm9keXttaW4td2lkdGg6MzIwcHghaW1wb3J0YW50O21hcmdpbjowIWltcG9ydGFudDtiYWNrZ3JvdW5kOiNmMmVmZjghaW1wb3J0YW50O2NvbG9yOiMyODI3MmUhaW1wb3J0YW50O2ZvbnQtZmFtaWx5OkludGVyLC1hcHBsZS1zeXN0ZW0sQmxpbmtNYWNTeXN0ZW1Gb250LCdTZWdvZSBVSScsc2Fucy1zZXJpZiFpbXBvcnRhbnR9LnRlcm1pbmFsLXRvcGJhcntib3gtc2l6aW5nOmJvcmRlci1ib3ghaW1wb3J0YW50O2hlaWdodDo0OHB4IWltcG9ydGFudDttaW4taGVpZ2h0OjQ4cHghaW1wb3J0YW50O3BhZGRpbmc6NHB4IDEycHghaW1wb3J0YW50O2Rpc3BsYXk6ZmxleCFpbXBvcnRhbnQ7YWxpZ24taXRlbXM6Y2VudGVyIWltcG9ydGFudDtnYXA6OHB4IWltcG9ydGFudDtvdmVyZmxvdzpoaWRkZW4haW1wb3J0YW50O3Bvc2l0aW9uOnN0aWNreSFpbXBvcnRhbnQ7dG9wOjAhaW1wb3J0YW50O3otaW5kZXg6NTAhaW1wb3J0YW50O2JhY2tncm91bmQ6I2ZhZjlmOWVkIWltcG9ydGFudDtiYWNrZHJvcC1maWx0ZXI6Ymx1cigxOHB4KSFpbXBvcnRhbnR9LmJyZWFkY3J1bWJze2ZvbnQtc2l6ZToxNHB4IWltcG9ydGFudDtmb250LXdlaWdodDo4MDAhaW1wb3J0YW50O3doaXRlLXNwYWNlOm5vd3JhcCFpbXBvcnRhbnR9LmNvbnRlbnQtaGVhZGluZy1yb3d7Ym94LXNpemluZzpib3JkZXItYm94IWltcG9ydGFudDttaW4taGVpZ2h0OjUwcHghaW1wb3J0YW50O3BhZGRpbmc6NHB4IDEycHghaW1wb3J0YW50O2Rpc3BsYXk6ZmxleCFpbXBvcnRhbnQ7YWxpZ24taXRlbXM6Y2VudGVyIWltcG9ydGFudDtnYXA6OXB4IWltcG9ydGFudH0uY3VycmVudC1wYWdlLWljb257d2lkdGg6MzhweCFpbXBvcnRhbnQ7aGVpZ2h0OjM4cHghaW1wb3J0YW50O21pbi13aWR0aDozOHB4IWltcG9ydGFudH0uY3VycmVudC1wYWdlLWNvcHkgaDF7bWFyZ2luOjAhaW1wb3J0YW50O2xpbmUtaGVpZ2h0OjEuMTUhaW1wb3J0YW50fS5jdXJyZW50LXBhZ2UtY29weSBwe21hcmdpbjoycHggMCAwIWltcG9ydGFudDtsaW5lLWhlaWdodDoxLjIhaW1wb3J0YW50fS50aGVtZS10b2dnbGUsLnRoZW1lLXN3aXRjaCwudGhlbWUtYnV0dG9uLC5kYXJrLW1vZGUtdG9nZ2xlLC5kYXJrLXRvZ2dsZSxbZGF0YS10aGVtZS10b2dnbGVdLGJ1dHRvblthcmlhLWxhYmVsKj10aGVtZSBpXSxidXR0b25bdGl0bGUqPXRoZW1lIGldLGJ1dHRvblthcmlhLWxhYmVsKj1kYXJrIGldLGJ1dHRvblt0aXRsZSo9ZGFyayBpXXtkaXNwbGF5Om5vbmUhaW1wb3J0YW50fS53b3Jrc3BhY2UtZnJhbWUtd3JhcHtoZWlnaHQ6Y2FsYygxMDBkdmggLSAxNTZweCkhaW1wb3J0YW50O21pbi1oZWlnaHQ6MzkwcHghaW1wb3J0YW50O21hcmdpbjowIDdweCFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoyMHB4IWltcG9ydGFudDtvdmVyZmxvdzpoaWRkZW4haW1wb3J0YW50fS50ZXJtaW5hbC1mb290ZXJ7ZGlzcGxheTpub25lIWltcG9ydGFudH0uc3RhbmRhbG9uZS1ib3R0b20tbmF2e2xlZnQ6OHB4IWltcG9ydGFudDtyaWdodDo4cHghaW1wb3J0YW50O2JvdHRvbTpjYWxjKDVweCArIGVudihzYWZlLWFyZWEtaW5zZXQtYm90dG9tLDBweCkpIWltcG9ydGFudDtwYWRkaW5nOjRweCFpbXBvcnRhbnQ7Z2FwOjJweCFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoyMHB4IWltcG9ydGFudH0uc3RhbmRhbG9uZS1ib3R0b20tbmF2IGJ1dHRvbnttaW4taGVpZ2h0OjQzcHghaW1wb3J0YW50O2ZvbnQtc2l6ZTo4cHghaW1wb3J0YW50O2dhcDoycHghaW1wb3J0YW50fS5hcHAtbWFpbntwYWRkaW5nOjdweCA4cHggMTBweCFpbXBvcnRhbnR9LnBhZ2UtaW50cm97bWFyZ2luOjAgM3B4IDhweCFpbXBvcnRhbnR9LnBhZ2UtaW50cm8gaDJ7Zm9udC1zaXplOjE2cHghaW1wb3J0YW50fS5wYWdlLWludHJvIHB7Zm9udC1zaXplOjEwcHghaW1wb3J0YW50fS5wYW5lbHtwYWRkaW5nOjExcHghaW1wb3J0YW50O21hcmdpbi1ib3R0b206OXB4IWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE4cHghaW1wb3J0YW50fS5zZXNzaW9uLWdyb3Vwe3BhZGRpbmc6MTFweCFpbXBvcnRhbnQ7bWFyZ2luOjhweCAwIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE2cHghaW1wb3J0YW50fS5tZXRyaWMsLmxldmVsLWNhcmQsLmhsYy1jYXJkLC5iZXAtbWV0cmlje3BhZGRpbmc6MTBweCFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoxNnB4IWltcG9ydGFudDttaW4td2lkdGg6MCFpbXBvcnRhbnR9Lm1ldHJpY3ttaW4taGVpZ2h0Ojc2cHghaW1wb3J0YW50fS5tZXRyaWMgc3Ryb25nLC5sZXZlbC12YWx1ZXtmb250LXNpemU6Y2xhbXAoMThweCw0Ljh2dywyM3B4KSFpbXBvcnRhbnQ7b3ZlcmZsb3ctd3JhcDphbnl3aGVyZSFpbXBvcnRhbnR9LmlucHV0LGlucHV0LHNlbGVjdCx0ZXh0YXJlYXttaW4taGVpZ2h0OjQzcHghaW1wb3J0YW50O3BhZGRpbmc6OXB4IDExcHghaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTNweCFpbXBvcnRhbnR9LmJlcC10YWJsZS13cmFwLC50YWJsZXdyYXB7bWF4LXdpZHRoOjEwMCUhaW1wb3J0YW50O292ZXJmbG93LXg6YXV0byFpbXBvcnRhbnR9LnRlcm1pbmFsLWxheW91dCwudGVybWluYWwtbWFpbnttaW4taGVpZ2h0OjAhaW1wb3J0YW50O2hlaWdodDphdXRvIWltcG9ydGFudH0uZGVza3RvcC1zaWRlYmFyLXNsb3QsLm1vYmlsZS1zaWRlYmFyLWxheWVyLC5kcmF3ZXItYmFja2Ryb3AsLm1vYmlsZS1tZW51LWJ1dHRvbiwudG9wLXdvcmtzcGFjZS10YWJze2Rpc3BsYXk6bm9uZSFpbXBvcnRhbnR9QG1lZGlhKG1heC13aWR0aDozOTBweCl7LndvcmtzcGFjZS1mcmFtZS13cmFwe2hlaWdodDpjYWxjKDEwMGR2aCAtIDE0OHB4KSFpbXBvcnRhbnQ7bWluLWhlaWdodDozNTBweCFpbXBvcnRhbnR9LnN0YW5kYWxvbmUtYm90dG9tLW5hdiBidXR0b257Zm9udC1zaXplOjdweCFpbXBvcnRhbnR9fSI7dmFyIGI9Imh0bWwsYm9keXttaW4td2lkdGg6MzIwcHghaW1wb3J0YW50O3dpZHRoOjEwMCUhaW1wb3J0YW50O292ZXJmbG93LXg6aGlkZGVuIWltcG9ydGFudDtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxMzVkZWcsI2YwZWFmYSwjZTdmMGVmIDM1JSwjZjhlOWU2IDcwJSwjZjdlZGNmKSFpbXBvcnRhbnQ7Y29sb3I6IzI4MjcyZSFpbXBvcnRhbnQ7Zm9udC1mYW1pbHk6SW50ZXIsLWFwcGxlLXN5c3RlbSxCbGlua01hY1N5c3RlbUZvbnQsJ1NlZ29lIFVJJyxzYW5zLXNlcmlmIWltcG9ydGFudH1ib2R5e3BhZGRpbmctYm90dG9tOjE2cHghaW1wb3J0YW50fS5hcHAtc2hlbGx7ZGlzcGxheTpibG9jayFpbXBvcnRhbnQ7d2lkdGg6MTAwJSFpbXBvcnRhbnQ7bWluLWhlaWdodDoxMDB2aCFpbXBvcnRhbnQ7YmFja2dyb3VuZDp0cmFuc3BhcmVudCFpbXBvcnRhbnR9LmFwcC1zaGVsbD4uc2lkZWJhciwuYXBwLW1haW4+LmFwcC10b3BiYXIsLmFwcC1tYWluPmhlYWRlci50b3AsI2RhdGFOb3RpY2V7ZGlzcGxheTpub25lIWltcG9ydGFudH0uYXBwLW1haW57ZGlzcGxheTpibG9jayFpbXBvcnRhbnQ7d2lkdGg6MTAwJSFpbXBvcnRhbnQ7bWF4LXdpZHRoOm5vbmUhaW1wb3J0YW50O21pbi13aWR0aDowIWltcG9ydGFudDttYXJnaW46MCFpbXBvcnRhbnQ7cGFkZGluZzo3cHggOHB4IDEycHghaW1wb3J0YW50O2JhY2tncm91bmQ6dHJhbnNwYXJlbnQhaW1wb3J0YW50fS5wYWdlLWludHJve21hcmdpbjowIDNweCA4cHghaW1wb3J0YW50fS5wYWdlLWludHJvIGgye2ZvbnQtc2l6ZToxNnB4IWltcG9ydGFudDtmb250LXdlaWdodDo4NTAhaW1wb3J0YW50fS5wYWdlLWludHJvIHB7Zm9udC1zaXplOjEwcHghaW1wb3J0YW50O2NvbG9yOiM3NzczN2IhaW1wb3J0YW50fS5wYW5lbHtwYWRkaW5nOjExcHghaW1wb3J0YW50O21hcmdpbjowIDAgOXB4IWltcG9ydGFudDtib3JkZXI6MXB4IHNvbGlkICNmZmZmZmZkOSFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoyMHB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2ZmZmZmZmU4LCNmOGY1ZjljOSkhaW1wb3J0YW50O2JveC1zaGFkb3c6MCA5cHggMjJweCAjNDAzNTQwMTIhaW1wb3J0YW50O2JhY2tkcm9wLWZpbHRlcjpibHVyKDE4cHgpIWltcG9ydGFudH0ucGFuZWwgLnBhbmVse3BhZGRpbmc6MTBweCFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoxNnB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOiNmZmZmZmZhOCFpbXBvcnRhbnR9LnNlc3Npb24tZ3JvdXB7cGFkZGluZzoxMXB4IWltcG9ydGFudDttYXJnaW46OHB4IDAhaW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgI2ZmZmZmZmQ5IWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE3cHghaW1wb3J0YW50O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmZmZmZTgsI2Y2ZjNmOWQ5KSFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDZweCAxNnB4ICM0MDM1NDAwYyFpbXBvcnRhbnR9LnNlc3Npb24tZ3JvdXAtaGVhZHtnYXA6OXB4IWltcG9ydGFudDttYXJnaW4tYm90dG9tOjlweCFpbXBvcnRhbnR9LnNlc3Npb24tc3RlcCwuaW5mby1pY29ue2JvcmRlci1yYWRpdXM6MTJweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDpsaW5lYXItZ3JhZGllbnQoMTQ1ZGVnLCNlOWUwZjQsI2UyZWVlYikhaW1wb3J0YW50fS5pbmZvLWZpZWxke3BhZGRpbmc6MTBweCFpbXBvcnRhbnQ7Ym9yZGVyOjFweCBzb2xpZCAjZmZmZmZmZDkhaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTVweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDojZmZmZmZmYzQhaW1wb3J0YW50fS5ncmlkLC5zZXNzaW9uLWdyaWQsLmNhcmRzLC5sZXZlbC1ncmlkLC5obGMtZ3JpZHtkaXNwbGF5OmdyaWQhaW1wb3J0YW50O2dyaWQtdGVtcGxhdGUtY29sdW1uczpyZXBlYXQoMixtaW5tYXgoMCwxZnIpKSFpbXBvcnRhbnQ7Z2FwOjlweCFpbXBvcnRhbnR9Lm1ldHJpYywubGV2ZWwtY2FyZCwuaGxjLWNhcmQsLmJlcC1tZXRyaWN7bWluLXdpZHRoOjAhaW1wb3J0YW50O3BhZGRpbmc6MTFweCFpbXBvcnRhbnQ7Ym9yZGVyOjFweCBzb2xpZCAjZmZmZmZmZGYhaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTdweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDpsaW5lYXItZ3JhZGllbnQoMTQ1ZGVnLCNmZmYsI2VlZThmNikhaW1wb3J0YW50O2JveC1zaGFkb3c6MCA3cHggMTZweCAjNDAzNTQwMGQhaW1wb3J0YW50O3RyYW5zaXRpb246dHJhbnNmb3JtIC4xOHMgZWFzZSxib3gtc2hhZG93IC4xOHMgZWFzZSFpbXBvcnRhbnR9Lm1ldHJpYzpudGgtY2hpbGQoNG4rMil7YmFja2dyb3VuZDpsaW5lYXItZ3JhZGllbnQoMTQ1ZGVnLCNmZmYsI2Y4ZTlkZikhaW1wb3J0YW50fS5tZXRyaWM6bnRoLWNoaWxkKDRuKzMpe2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmLCNlNWYwZTkpIWltcG9ydGFudH0ubWV0cmljOm50aC1jaGlsZCg0bis0KXtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2ZmZiwjZjVlZGNmKSFpbXBvcnRhbnR9Lm1ldHJpYyBzbWFsbCwubGV2ZWwta2luZCwubGV2ZWwtZm9ybXVsYSwubGV2ZWwtbWF0aHtmb250LXNpemU6MTBweCFpbXBvcnRhbnQ7Y29sb3I6Izc3NzM3YiFpbXBvcnRhbnR9Lm1ldHJpYyBzdHJvbmcsLmxldmVsLXZhbHVle2Rpc3BsYXk6YmxvY2shaW1wb3J0YW50O21hcmdpbi10b3A6NXB4IWltcG9ydGFudDtmb250LXNpemU6Y2xhbXAoMThweCw1dncsMjRweCkhaW1wb3J0YW50O2ZvbnQtd2VpZ2h0Ojg1MCFpbXBvcnRhbnQ7b3ZlcmZsb3ctd3JhcDphbnl3aGVyZSFpbXBvcnRhbnQ7Y29sb3I6IzI1MjQyYSFpbXBvcnRhbnR9LmxldmVsLWNhcmQuc3VwcG9ydHtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2ZmZiwjZTVmMGU5KSFpbXBvcnRhbnR9LmxldmVsLWNhcmQucmVzaXN0YW5jZSwuaGxjLWNhcmQucHV0e2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmLCNmOGU4ZGYpIWltcG9ydGFudH0uaGxjLXRpdGxle2ZvbnQtc2l6ZToxMnB4IWltcG9ydGFudDtmb250LXdlaWdodDo4NTAhaW1wb3J0YW50fS5vaGxjLWNlbGx7cGFkZGluZzo4cHghaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTJweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDojZmZmZmZmYzQhaW1wb3J0YW50fS5iZXAtc3VtbWFyeXtkaXNwbGF5OmdyaWQhaW1wb3J0YW50O2dyaWQtdGVtcGxhdGUtY29sdW1uczpyZXBlYXQoMixtaW5tYXgoMCwxZnIpKSFpbXBvcnRhbnQ7Z2FwOjhweCFpbXBvcnRhbnR9LmJlcC12YWx1ZXtmb250LXNpemU6MThweCFpbXBvcnRhbnQ7Zm9udC13ZWlnaHQ6ODUwIWltcG9ydGFudH0uaW5wdXQsaW5wdXQsc2VsZWN0LHRleHRhcmVhe21heC13aWR0aDoxMDAlIWltcG9ydGFudDttaW4td2lkdGg6MCFpbXBvcnRhbnQ7bWluLWhlaWdodDo0NHB4IWltcG9ydGFudDtwYWRkaW5nOjEwcHggMTJweCFpbXBvcnRhbnQ7Ym9yZGVyOjFweCBzb2xpZCAjZTZlMGU4IWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE0cHghaW1wb3J0YW50O2JhY2tncm91bmQ6I2ZmZmZmZmRmIWltcG9ydGFudDtjb2xvcjojMjgyNzJlIWltcG9ydGFudH0uYmVwLXRhYmxlLXdyYXAsLnRhYmxld3JhcHttYXgtd2lkdGg6MTAwJSFpbXBvcnRhbnQ7b3ZlcmZsb3cteDphdXRvIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE2cHghaW1wb3J0YW50O2JhY2tncm91bmQ6I2ZmZmZmZmM5IWltcG9ydGFudH0uYmVwLXRhYmxlLHRhYmxle21pbi13aWR0aDo1NTBweCFpbXBvcnRhbnQ7Zm9udC1zaXplOjEwcHghaW1wb3J0YW50fS5iZXAtdGFibGUgdGQsLmJlcC10YWJsZSB0aCx0YWJsZSB0ZCx0YWJsZSB0aHtwYWRkaW5nOjhweCFpbXBvcnRhbnR9LmFjdGlvbnMgYnV0dG9uLGJ1dHRvbntib3JkZXItcmFkaXVzOjE0cHghaW1wb3J0YW50O3RyYW5zaXRpb246dHJhbnNmb3JtIC4xOHMgZWFzZSxib3gtc2hhZG93IC4xOHMgZWFzZSFpbXBvcnRhbnR9LmFjdGlvbnMgYnV0dG9uOmFjdGl2ZSxidXR0b246YWN0aXZlLC5tZXRyaWM6YWN0aXZlLC5sZXZlbC1jYXJkOmFjdGl2ZSwuaGxjLWNhcmQ6YWN0aXZle3RyYW5zZm9ybTpzY2FsZSguOTgpIWltcG9ydGFudH1AbWVkaWEoaG92ZXI6aG92ZXIpey5wYW5lbDpob3ZlciwubWV0cmljOmhvdmVyLC5sZXZlbC1jYXJkOmhvdmVyLC5obGMtY2FyZDpob3Zlcnt0cmFuc2Zvcm06dHJhbnNsYXRlWSgtMnB4KSFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDEycHggMjNweCAjNDAzNTQwMWEhaW1wb3J0YW50fX1AbWVkaWEobWF4LXdpZHRoOjM3MHB4KXsuZ3JpZCwuc2Vzc2lvbi1ncmlkLC5jYXJkcywubGV2ZWwtZ3JpZCwuaGxjLWdyaWQsLmJlcC1zdW1tYXJ5e2dyaWQtdGVtcGxhdGUtY29sdW1uczptaW5tYXgoMCwxZnIpIWltcG9ydGFudH19IjtmdW5jdGlvbiBjc3MoZCxpZCx0KXtpZighZHx8IWQuaGVhZClyZXR1cm47dmFyIHM9ZC5nZXRFbGVtZW50QnlJZChpZCk7aWYoIXMpe3M9ZC5jcmVhdGVFbGVtZW50KCdzdHlsZScpO3MuaWQ9aWQ7ZC5oZWFkLmFwcGVuZENoaWxkKHMpfXMudGV4dENvbnRlbnQ9dH1mdW5jdGlvbiBoaWRlKGQpe2lmKCFkKXJldHVybjtkLnF1ZXJ5U2VsZWN0b3JBbGwoJ2J1dHRvbixbcm9sZT1idXR0b25dLGEnKS5mb3JFYWNoKGZ1bmN0aW9uKHgpe3ZhciB6PSgoeC5nZXRBdHRyaWJ1dGUoJ2FyaWEtbGFiZWwnKXx8JycpKycgJysoeC5nZXRBdHRyaWJ1dGUoJ3RpdGxlJyl8fCcnKSsnICcrKHguaW5uZXJUZXh0fHwnJykpLnRvTG93ZXJDYXNlKCk7aWYoL2RhcmsgbW9kZXxsaWdodCBtb2RlfG5pZ2h0IG1vZGV8dG9nZ2xlIHRoZW1lfGNvbG9yIHRoZW1lLy50ZXN0KHopKXguc3R5bGUuc2V0UHJvcGVydHkoJ2Rpc3BsYXknLCdub25lJywnaW1wb3J0YW50Jyl9KX1mdW5jdGlvbiBydW4oKXt0cnl7Y3NzKGRvY3VtZW50LCduaWZ0eS1jb21wYWN0LW91dGVyJyxhKTtoaWRlKGRvY3VtZW50KTt2YXIgZj1kb2N1bWVudC5xdWVyeVNlbGVjdG9yKCcud29ya3NwYWNlLWZyYW1lJyk7aWYoZiYmZi5jb250ZW50RG9jdW1lbnQpe2NzcyhmLmNvbnRlbnREb2N1bWVudCwnbmlmdHktY29tcGFjdC1pbm5lcicsYik7aGlkZShmLmNvbnRlbnREb2N1bWVudCl9fWNhdGNoKGUpe319aWYoZG9jdW1lbnQucmVhZHlTdGF0ZT09PSdsb2FkaW5nJylkb2N1bWVudC5hZGRFdmVudExpc3RlbmVyKCdET01Db250ZW50TG9hZGVkJyxydW4pO2Vsc2UgcnVuKCk7dmFyIG49MCx0PXNldEludGVydmFsKGZ1bmN0aW9uKCl7cnVuKCk7aWYoKytuPjQwKWNsZWFySW50ZXJ2YWwodCl9LDUwMCk7bmV3IE11dGF0aW9uT2JzZXJ2ZXIocnVuKS5vYnNlcnZlKGRvY3VtZW50LmRvY3VtZW50RWxlbWVudCx7Y2hpbGRMaXN0OnRydWUsc3VidHJlZTp0cnVlfSk7dmFyIGY9ZG9jdW1lbnQucXVlcnlTZWxlY3RvcignLndvcmtzcGFjZS1mcmFtZScpO2lmKGYpZi5hZGRFdmVudExpc3RlbmVyKCdsb2FkJyxydW4pfSkoKTs=", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8);

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
