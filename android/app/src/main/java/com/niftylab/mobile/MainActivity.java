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
    private static final String MOBILE_THEME = "(function(){var parentCss=atob('aHRtbCxib2R5LCNyb290e2JhY2tncm91bmQ6I2Y1ZjNmYiFpbXBvcnRhbnQ7Y29sb3I6IzI1MjQzYSFpbXBvcnRhbnR9LnRlcm1pbmFsLWxheW91dCwudGVybWluYWwtbWFpbntiYWNrZ3JvdW5kOnJhZGlhbC1ncmFkaWVudChlbGxpcHNlIGF0IDEwJSAwJSxyZ2JhKDIxNywxOTcsMjU1LC4zNSksdHJhbnNwYXJlbnQgMzglKSxyYWRpYWwtZ3JhZGllbnQoZWxsaXBzZSBhdCA5NSUgMjAlLHJnYmEoMjU1LDIxOCwyMjksLjM0KSx0cmFuc3BhcmVudCA0MCUpLGxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2ZhZjhmZiwjZjBlZmY5IDY1JSwjZjlmMGY0KSFpbXBvcnRhbnR9LnRlcm1pbmFsLXRvcGJhcnttaW4taGVpZ2h0OjYycHghaW1wb3J0YW50O3BhZGRpbmc6OHB4IDE0cHghaW1wb3J0YW50O2JhY2tncm91bmQ6cmdiYSgyNTUsMjU1LDI1NSwuNzIpIWltcG9ydGFudDtib3JkZXI6MCFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDZweCAyNHB4IHJnYmEoNjIsNDIsOTYsLjA3KSFpbXBvcnRhbnQ7YmFja2Ryb3AtZmlsdGVyOmJsdXIoMjJweCkhaW1wb3J0YW50fS5tb2JpbGUtbWVudS1idXR0b24sLnRoZW1lLWJ1dHRvbntib3JkZXI6MXB4IHNvbGlkIHJnYmEoMjU1LDI1NSwyNTUsLjkpIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE1cHghaW1wb3J0YW50O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmLCNlZWU5ZmYpIWltcG9ydGFudDtjb2xvcjojNzY2MmM4IWltcG9ydGFudDtib3gtc2hhZG93OjAgNXB4IDE1cHggcmdiYSg4NSw2NSwxNDAsLjEyKSFpbXBvcnRhbnR9LmJyZWFkY3J1bWJzLC5icmVhZGNydW1icyBzdHJvbmd7Y29sb3I6IzM5MzM0ZiFpbXBvcnRhbnR9LnRvcGJhci1hY3Rpb25ze2dhcDo4cHghaW1wb3J0YW50fS5hcGktc3RhdHVzLC50b3BiYXItYWN0aW9uLWRpdmlkZXJ7ZGlzcGxheTpub25lIWltcG9ydGFudH0uY29udGVudC1oZWFkaW5nLXJvd3ttaW4taGVpZ2h0OjY1cHghaW1wb3J0YW50O3BhZGRpbmc6OHB4IDE1cHghaW1wb3J0YW50fS5jdXJyZW50LXBhZ2UtaWNvbnt3aWR0aDo0M3B4IWltcG9ydGFudDtoZWlnaHQ6NDNweCFpbXBvcnRhbnQ7ZmxleC1iYXNpczo0M3B4IWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE2cHghaW1wb3J0YW50O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmLCNlOGUwZmYpIWltcG9ydGFudDtjb2xvcjojODY2YmQ3IWltcG9ydGFudDtib3gtc2hhZG93OjAgNnB4IDE2cHggcmdiYSgxMDMsNzksMTY5LC4xMikhaW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgcmdiYSgyNTUsMjU1LDI1NSwuOTUpIWltcG9ydGFudH0uY3VycmVudC1wYWdlLWNvcHkgaDF7Zm9udC1zaXplOjE3cHghaW1wb3J0YW50O2NvbG9yOiMyODI0M2MhaW1wb3J0YW50fS5jdXJyZW50LXBhZ2UtY29weSBwe2ZvbnQtc2l6ZToxMHB4IWltcG9ydGFudDtjb2xvcjojODk4MzllIWltcG9ydGFudH0uc291cmNlLWludGVncml0eSwudGVybWluYWwtZm9vdGVye2Rpc3BsYXk6bm9uZSFpbXBvcnRhbnR9LndvcmtzcGFjZS1mcmFtZS13cmFwe21hcmdpbjowIDhweCA4OHB4IWltcG9ydGFudDtib3JkZXI6MXB4IHNvbGlkIHJnYmEoMjU1LDI1NSwyNTUsLjkpIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjIzcHghaW1wb3J0YW50O2JhY2tncm91bmQ6cmdiYSgyNTUsMjU1LDI1NSwuMzgpIWltcG9ydGFudDtib3gtc2hhZG93OjAgMTJweCAzMnB4IHJnYmEoNjksNTEsMTEwLC4wOCkhaW1wb3J0YW50O292ZXJmbG93OmhpZGRlbiFpbXBvcnRhbnR9LndvcmtzcGFjZS1mcmFtZXtiYWNrZ3JvdW5kOnRyYW5zcGFyZW50IWltcG9ydGFudH0udG9wLXdvcmtzcGFjZS10YWJze3Bvc2l0aW9uOmZpeGVkIWltcG9ydGFudDtsZWZ0OjUwJSFpbXBvcnRhbnQ7Ym90dG9tOjE3cHghaW1wb3J0YW50O3RyYW5zZm9ybTp0cmFuc2xhdGVYKC01MCUpIWltcG9ydGFudDt6LWluZGV4OjUwMCFpbXBvcnRhbnQ7d2lkdGg6Y2FsYygxMDAlIC0gMjhweCkhaW1wb3J0YW50O21heC13aWR0aDo1NjBweCFpbXBvcnRhbnQ7ZGlzcGxheTpmbGV4IWltcG9ydGFudDtnYXA6NHB4IWltcG9ydGFudDtvdmVyZmxvdy14OmF1dG8haW1wb3J0YW50O3BhZGRpbmc6OHB4IWltcG9ydGFudDtib3JkZXI6MXB4IHNvbGlkIHJnYmEoMjU1LDI1NSwyNTUsLjIyKSFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoyNHB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOnJnYmEoNDcsMzEsNzUsLjk0KSFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDEycHggMzJweCByZ2JhKDQyLDI3LDY4LC4yNiksaW5zZXQgMCAxcHggcmdiYSgyNTUsMjU1LDI1NSwuMTIpIWltcG9ydGFudDtiYWNrZHJvcC1maWx0ZXI6Ymx1cigyNHB4KSFpbXBvcnRhbnQ7c2Nyb2xsYmFyLXdpZHRoOm5vbmUhaW1wb3J0YW50fS50b3Atd29ya3NwYWNlLXRhYnM6Oi13ZWJraXQtc2Nyb2xsYmFye2Rpc3BsYXk6bm9uZSFpbXBvcnRhbnR9LnRvcC13b3Jrc3BhY2UtdGFie2ZsZXg6MSAwIGF1dG8haW1wb3J0YW50O21pbi13aWR0aDo2NHB4IWltcG9ydGFudDtkaXNwbGF5OmZsZXghaW1wb3J0YW50O2ZsZXgtZGlyZWN0aW9uOmNvbHVtbiFpbXBvcnRhbnQ7YWxpZ24taXRlbXM6Y2VudGVyIWltcG9ydGFudDtqdXN0aWZ5LWNvbnRlbnQ6Y2VudGVyIWltcG9ydGFudDtnYXA6NHB4IWltcG9ydGFudDttaW4taGVpZ2h0OjQ5cHghaW1wb3J0YW50O3BhZGRpbmc6NXB4IDhweCFpbXBvcnRhbnQ7Ym9yZGVyOjAhaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTZweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDp0cmFuc3BhcmVudCFpbXBvcnRhbnQ7Y29sb3I6I2Q3Y2RlYyFpbXBvcnRhbnQ7Zm9udC1zaXplOjlweCFpbXBvcnRhbnQ7Zm9udC13ZWlnaHQ6NjUwIWltcG9ydGFudDtib3gtc2hhZG93Om5vbmUhaW1wb3J0YW50O3RyYW5zaXRpb246YWxsIC4ycyBlYXNlIWltcG9ydGFudH0udG9wLXdvcmtzcGFjZS10YWIgc3Zne3dpZHRoOjE3cHghaW1wb3J0YW50O2hlaWdodDoxN3B4IWltcG9ydGFudDtzdHJva2Utd2lkdGg6MS44IWltcG9ydGFudH0udG9wLXdvcmtzcGFjZS10YWIuYWN0aXZle2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjY2JiNmZmLCNhOThiZTkpIWltcG9ydGFudDtjb2xvcjojMzQyMzRmIWltcG9ydGFudDtib3gtc2hhZG93OjAgNXB4IDE0cHggcmdiYSgxOTAsMTYzLDI1NSwuMjYpLGluc2V0IDAgMXB4IHJnYmEoMjU1LDI1NSwyNTUsLjY1KSFpbXBvcnRhbnR9LnRvcC13b3Jrc3BhY2UtdGFiOmFjdGl2ZXt0cmFuc2Zvcm06c2NhbGUoLjk0KSFpbXBvcnRhbnR9QG1lZGlhKGhvdmVyOmhvdmVyKXsudG9wLXdvcmtzcGFjZS10YWI6aG92ZXJ7YmFja2dyb3VuZDpyZ2JhKDI1NSwyNTUsMjU1LC4xNCkhaW1wb3J0YW50O3RyYW5zZm9ybTp0cmFuc2xhdGVZKC0ycHgpIWltcG9ydGFudH0udG9wLXdvcmtzcGFjZS10YWIuYWN0aXZlOmhvdmVye2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZDhjOGZmLCNiNzliZWEpIWltcG9ydGFudH19QG1lZGlhKG1heC13aWR0aDo0ODBweCl7LnRvcC13b3Jrc3BhY2UtdGFic3tib3R0b206MTNweCFpbXBvcnRhbnQ7d2lkdGg6Y2FsYygxMDAlIC0gMjJweCkhaW1wb3J0YW50O3BhZGRpbmc6N3B4IWltcG9ydGFudH0udG9wLXdvcmtzcGFjZS10YWJ7bWluLXdpZHRoOjU3cHghaW1wb3J0YW50O2ZvbnQtc2l6ZTo4cHghaW1wb3J0YW50fS50ZXJtaW5hbC10b3BiYXJ7bWluLWhlaWdodDo1N3B4IWltcG9ydGFudH0ud29ya3NwYWNlLWZyYW1lLXdyYXB7bWFyZ2luLWlubGluZTo1cHghaW1wb3J0YW50fX0='),frameCss=atob('OnJvb3R7Y29sb3Itc2NoZW1lOmxpZ2h0IWltcG9ydGFudDstLWFwcC1iZzojZjVmM2ZiIWltcG9ydGFudDstLXN1cmZhY2U6cmdiYSgyNTUsMjU1LDI1NSwuNzgpIWltcG9ydGFudDstLXN1cmZhY2UtMjojZjhmNWZmIWltcG9ydGFudDstLXN1cmZhY2UtMzojZWVlOWZhIWltcG9ydGFudDstLWFwcC1saW5lOnJnYmEoMTYwLDE0NSwxOTUsLjIpIWltcG9ydGFudDstLWFwcC10ZXh0OiMyODI0M2MhaW1wb3J0YW50Oy0tYXBwLW11dGVkOiM4MTdiOTYhaW1wb3J0YW50Oy0tYXBwLWFjY2VudDojYTg4YWU4IWltcG9ydGFudDstLWFwcC1hY2NlbnQtc3Ryb25nOiM4MDY0Y2YhaW1wb3J0YW50Oy0tYXBwLWFjY2VudC1zb2Z0OiNlZWU3ZmYhaW1wb3J0YW50Oy0tYXBwLXNoYWRvdzowIDEwcHggMjZweCByZ2JhKDgxLDYxLDEzMCwuMDkpIWltcG9ydGFudH1odG1sLGJvZHl7d2lkdGg6MTAwJSFpbXBvcnRhbnQ7bWluLXdpZHRoOjAhaW1wb3J0YW50O2JhY2tncm91bmQ6I2Y1ZjNmYiFpbXBvcnRhbnQ7Y29sb3I6IzI4MjQzYyFpbXBvcnRhbnR9Ym9keXtiYWNrZ3JvdW5kOnJhZGlhbC1ncmFkaWVudChlbGxpcHNlIGF0IDAlIDAlLHJnYmEoMjE2LDE5OCwyNTUsLjQyKSx0cmFuc3BhcmVudCA0MCUpLHJhZGlhbC1ncmFkaWVudChlbGxpcHNlIGF0IDEwMCUgMTAlLHJnYmEoMjU1LDIyMCwyMzIsLjM2KSx0cmFuc3BhcmVudCAzOCUpLGxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2ZhZjhmZiwjZjFlZmY5IDYwJSwjZjlmMGY0KSFpbXBvcnRhbnQ7b3ZlcmZsb3cteDpoaWRkZW4haW1wb3J0YW50fWJvZHk+LmFwcC1zaGVsbD4uc2lkZWJhciwuYXBwLW1haW4+LmFwcC10b3BiYXIsLmFwcC1tYWluPi50b3AsI2RhdGFOb3RpY2V7ZGlzcGxheTpub25lIWltcG9ydGFudH0uYXBwLXNoZWxse3dpZHRoOjEwMCUhaW1wb3J0YW50O21pbi1oZWlnaHQ6MTAwdmghaW1wb3J0YW50O2JhY2tncm91bmQ6dHJhbnNwYXJlbnQhaW1wb3J0YW50fS5hcHAtbWFpbnt3aWR0aDoxMDAlIWltcG9ydGFudDttYXgtd2lkdGg6bm9uZSFpbXBvcnRhbnQ7bWluLXdpZHRoOjAhaW1wb3J0YW50O21hcmdpbjowIWltcG9ydGFudDtwYWRkaW5nOjEycHggMTJweCAzMHB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOnRyYW5zcGFyZW50IWltcG9ydGFudH0ucGFnZS1pbnRyb3ttYXJnaW46MCAycHggMTNweCFpbXBvcnRhbnQ7Y29sb3I6IzgxN2I5NiFpbXBvcnRhbnR9LnBhZ2UtaW50cm8gaDEsLnBhZ2UtaW50cm8gaDIsLnBhZ2UtaW50cm8gc3Ryb25ne2NvbG9yOiMyODI0M2MhaW1wb3J0YW50fS5wYW5lbHtwYWRkaW5nOjE2cHghaW1wb3J0YW50O21hcmdpbjowIDAgMTRweCFpbXBvcnRhbnQ7Ym9yZGVyOjFweCBzb2xpZCByZ2JhKDI1NSwyNTUsMjU1LC45MikhaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MjRweCFpbXBvcnRhbnQ7YmFja2dyb3VuZDpyZ2JhKDI1NSwyNTUsMjU1LC43NikhaW1wb3J0YW50O2JveC1zaGFkb3c6MCAxMHB4IDI4cHggcmdiYSg3Niw1NywxMjEsLjA3NSksaW5zZXQgMCAxcHggcmdiYSgyNTUsMjU1LDI1NSwuOTUpIWltcG9ydGFudDtiYWNrZHJvcC1maWx0ZXI6Ymx1cigxOHB4KSFpbXBvcnRhbnR9LnNlc3Npb24tZ3JvdXB7cGFkZGluZzoxNHB4IWltcG9ydGFudDttYXJnaW46MTJweCAwIWltcG9ydGFudDtib3JkZXI6MXB4IHNvbGlkIHJnYmEoMTcwLDE1MywyMTEsLjIyKSFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoyMXB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxNDVkZWcscmdiYSgyNTUsMjU1LDI1NSwuOTQpLHJnYmEoMjQ2LDI0MiwyNTUsLjg4KSkhaW1wb3J0YW50O2JveC1zaGFkb3c6MCA2cHggMThweCByZ2JhKDgzLDYxLDEzMiwuMDQ1KSFpbXBvcnRhbnR9LnNlc3Npb24tZ3JvdXAtaGVhZHtnYXA6MTFweCFpbXBvcnRhbnR9LnNlc3Npb24tc3RlcCwuc3RlcC1jaGlwe3dpZHRoOjQzcHghaW1wb3J0YW50O2hlaWdodDo0M3B4IWltcG9ydGFudDtkaXNwbGF5OmdyaWQhaW1wb3J0YW50O3BsYWNlLWl0ZW1zOmNlbnRlciFpbXBvcnRhbnQ7Ym9yZGVyLXJhZGl1czoxNXB4IWltcG9ydGFudDtiYWNrZ3JvdW5kOmxpbmVhci1ncmFkaWVudCgxNDVkZWcsI2VlZTdmZiwjZTNlOGZmKSFpbXBvcnRhbnQ7Y29sb3I6IzgwNjRjZiFpbXBvcnRhbnQ7Zm9udC13ZWlnaHQ6ODAwIWltcG9ydGFudH0uaW5mby1maWVsZHtwYWRkaW5nOjEycHghaW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgcmdiYSgxNjEsMTQ4LDE5NCwuMjIpIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE4cHghaW1wb3J0YW50O2JhY2tncm91bmQ6cmdiYSgyNTUsMjU1LDI1NSwuNzgpIWltcG9ydGFudDtib3gtc2hhZG93OjAgNXB4IDE0cHggcmdiYSg3OSw2MSwxMjAsLjA0NSkhaW1wb3J0YW50fWlucHV0LHNlbGVjdCx0ZXh0YXJlYXt3aWR0aDoxMDAlIWltcG9ydGFudDttaW4taGVpZ2h0OjUxcHghaW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgcmdiYSgxNTgsMTQ1LDE5NCwuMjgpIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE2cHghaW1wb3J0YW50O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZywjZmZmLCNmN2Y1ZmMpIWltcG9ydGFudDtjb2xvcjojMzAyYjQ3IWltcG9ydGFudDtib3gtc2hhZG93Omluc2V0IDAgMXB4IDJweCByZ2JhKDY1LDQ5LDEwNCwuMDI1KSwwIDRweCAxMnB4IHJnYmEoNjUsNDksMTA0LC4wMzUpIWltcG9ydGFudH1pbnB1dDpmb2N1cyxzZWxlY3Q6Zm9jdXMsdGV4dGFyZWE6Zm9jdXN7b3V0bGluZToycHggc29saWQgcmdiYSgxNjksMTM5LDIzMiwuMzUpIWltcG9ydGFudDtib3JkZXItY29sb3I6I2I1OWJlOSFpbXBvcnRhbnR9YnV0dG9uLC5hY3Rpb25zIGJ1dHRvbiwuc2Vjb25kYXJ5e2JvcmRlci1yYWRpdXM6MTZweCFpbXBvcnRhbnQ7dHJhbnNpdGlvbjp0cmFuc2Zvcm0gLjE4cyBlYXNlLGJveC1zaGFkb3cgLjE4cyBlYXNlLGJhY2tncm91bmQgLjE4cyBlYXNlIWltcG9ydGFudH1idXR0b246bm90KC5zZWNvbmRhcnkpe2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDEzNWRlZywjYjk5Y2YwLCM5YzgzZGYpIWltcG9ydGFudDtib3JkZXI6MXB4IHNvbGlkIHJnYmEoMjU1LDI1NSwyNTUsLjgpIWltcG9ydGFudDtjb2xvcjojZmZmIWltcG9ydGFudDtib3gtc2hhZG93OjAgN3B4IDE3cHggcmdiYSgxNDMsMTExLDIwNywuMikhaW1wb3J0YW50fWJ1dHRvbi5zZWNvbmRhcnl7YmFja2dyb3VuZDpsaW5lYXItZ3JhZGllbnQoMTQ1ZGVnLCNmZmYsI2YwZWJmYikhaW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgcmdiYSgxNjUsMTUwLDE5OCwuMikhaW1wb3J0YW50O2NvbG9yOiM2NzU3OGYhaW1wb3J0YW50O2JveC1zaGFkb3c6MCA1cHggMTNweCByZ2JhKDc3LDU4LDEyMCwuMDcpIWltcG9ydGFudH1AbWVkaWEoaG92ZXI6aG92ZXIpe2J1dHRvbjpob3Zlcnt0cmFuc2Zvcm06dHJhbnNsYXRlWSgtMnB4KSFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDEwcHggMjJweCByZ2JhKDEyNiw5OCwxODUsLjE4KSFpbXBvcnRhbnR9fWJ1dHRvbjphY3RpdmV7dHJhbnNmb3JtOnNjYWxlKC45NykhaW1wb3J0YW50fS5jYXJkcywuZ3JpZHtnYXA6MTBweCFpbXBvcnRhbnR9Lm1ldHJpYywuc291cmNlLWJveCwuc3RhdHVzLC5ub3RpY2UsLmNhbGMtbm90ZXtib3JkZXI6MXB4IHNvbGlkIHJnYmEoMjU1LDI1NSwyNTUsLjkpIWltcG9ydGFudDtib3JkZXItcmFkaXVzOjE5cHghaW1wb3J0YW50O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE0NWRlZyxyZ2JhKDI1NSwyNTUsMjU1LC45MikscmdiYSgyNDQsMjQwLDI1MywuOSkpIWltcG9ydGFudDtib3gtc2hhZG93OjAgN3B4IDE4cHggcmdiYSg3Nyw1OCwxMjAsLjA2KSFpbXBvcnRhbnR9LmxldmVsLWNhcmQsLmhsYy1jYXJkLC5iZXAtbWV0cmlje2JvcmRlci1yYWRpdXM6MTlweCFpbXBvcnRhbnQ7Ym94LXNoYWRvdzowIDdweCAxOHB4IHJnYmEoNzcsNTgsMTIwLC4wNykhaW1wb3J0YW50fS50YWJsZXdyYXAsLmJlcC10YWJsZS13cmFwe21heC13aWR0aDoxMDAlIWltcG9ydGFudDtvdmVyZmxvdy14OmF1dG8haW1wb3J0YW50O2JvcmRlcjoxcHggc29saWQgcmdiYSgxNjUsMTUwLDE5OCwuMikhaW1wb3J0YW50O2JvcmRlci1yYWRpdXM6MTdweCFpbXBvcnRhbnR9dGFibGV7bWluLXdpZHRoOjU2MHB4IWltcG9ydGFudH1sYWJlbCwubXV0ZWQsLnN1YiwuZmllbGQtaGludCwucGFnZS1pbnRybyBwe2NvbG9yOiM4MTdiOTYhaW1wb3J0YW50fWgxLGgyLGgzLGg0LHN0cm9uZ3tjb2xvcjojMzAyYjQ3IWltcG9ydGFudH0uYWN0aW9uc3tkaXNwbGF5OmZsZXghaW1wb3J0YW50O2ZsZXgtd3JhcDp3cmFwIWltcG9ydGFudDtnYXA6OXB4IWltcG9ydGFudH0uYWN0aW9ucyBidXR0b257bWluLWhlaWdodDo0NnB4IWltcG9ydGFudH1AbWVkaWEobWF4LXdpZHRoOjYwMHB4KXsuYXBwLW1haW57cGFkZGluZzoxMHB4IDEwcHggMzVweCFpbXBvcnRhbnR9LnBhZ2UtaW50cm97Zm9udC1zaXplOjEycHghaW1wb3J0YW50fS5wYW5lbHtwYWRkaW5nOjEzcHghaW1wb3J0YW50O21hcmdpbi1ib3R0b206MTJweCFpbXBvcnRhbnR9LnNlc3Npb24tZ3JvdXB7cGFkZGluZzoxMnB4IWltcG9ydGFudDttYXJnaW46MTBweCAwIWltcG9ydGFudH0uc2Vzc2lvbi1ncm91cC1oZWFkIGgyLC5zZXNzaW9uLWdyb3VwLWhlYWQgaDN7Zm9udC1zaXplOjE1cHghaW1wb3J0YW50fS5ncmlkLC5ncmlkLnNlc3Npb24tZ3JpZHtncmlkLXRlbXBsYXRlLWNvbHVtbnM6bWlubWF4KDAsMWZyKSFpbXBvcnRhbnR9LmNhcmRze2dyaWQtdGVtcGxhdGUtY29sdW1uczpyZXBlYXQoMixtaW5tYXgoMCwxZnIpKSFpbXBvcnRhbnR9LmluZm8tZmllbGR7cGFkZGluZzoxMHB4IWltcG9ydGFudH0uaW5mby1maWVsZCBsYWJlbHtmb250LXNpemU6MTFweCFpbXBvcnRhbnR9aW5wdXQsc2VsZWN0LHRleHRhcmVhe2ZvbnQtc2l6ZToxNXB4IWltcG9ydGFudH0uYWN0aW9ucyBidXR0b257ZmxleDoxIDEgMTAwJSFpbXBvcnRhbnR9LnRhYmxld3JhcCwuYmVwLXRhYmxlLXdyYXB7bWFyZ2luLWlubGluZTotMnB4IWltcG9ydGFudH19');function put(d,id,c){if(!d||!d.head)return;var n=d.getElementById(id);if(!n){n=d.createElement('style');n.id=id;d.head.appendChild(n)}n.textContent=c}function scan(){put(document,'nifty-apk-parent-v2',parentCss);var f=document.querySelector('iframe.workspace-frame');if(f){try{put(f.contentDocument,'nifty-apk-frame-v2',frameCss)}catch(e){}}}scan();new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});document.querySelectorAll('iframe.workspace-frame').forEach(function(f){f.addEventListener('load',scan)});setInterval(scan,1200)})();";

    private static final String STARTUP_FIX = "(function(){try{sessionStorage.setItem('nifty-lab-intro-seen','1')}catch(e){}function dismiss(){try{var b=document.getElementById('skipIntro');if(b)b.click();var intro=document.getElementById('brandIntro');if(intro){intro.classList.add('is-leaving');intro.style.pointerEvents='none';setTimeout(function(){if(intro.parentNode)intro.remove()},800)}}catch(e){}}if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',dismiss,{once:true});else dismiss();var n=0;var t=setInterval(function(){dismiss();if(++n>15)clearInterval(t)},350);})();";

    private static final String OUTER_THEME = "(function(){var css=':root{color-scheme:light!important}html,body,#root,.terminal-layout,.terminal-main{background:linear-gradient(145deg,#f5f7ff 0%,#edf2fa 52%,#f8eff3 100%)!important;color:#202638!important}.terminal-topbar{position:sticky!important;top:0!important;z-index:120!important;display:flex!important;flex-wrap:wrap!important;gap:9px!important;padding:10px 12px!important;background:rgba(249,251,255,.9)!important;border-bottom:1px solid rgba(135,151,190,.2)!important;box-shadow:0 8px 24px rgba(62,77,126,.07)!important;backdrop-filter:blur(22px)!important}.topbar-left{min-width:0!important;flex:1 1 100%!important}.breadcrumbs{color:#65718a!important}.breadcrumbs strong{color:#202638!important}.historical-tag,.topbar-divider{display:none!important}.top-workspace-tabs{display:flex!important;order:3!important;flex:1 1 100%!important;gap:7px!important;overflow-x:auto!important;padding:2px 0 5px!important;scrollbar-width:none!important}.top-workspace-tabs::-webkit-scrollbar{display:none!important}.top-workspace-tab{flex:0 0 auto!important;min-height:42px!important;padding:8px 12px!important;border:1px solid rgba(137,153,190,.2)!important;border-radius:15px!important;background:rgba(255,255,255,.75)!important;color:#65718a!important;box-shadow:0 4px 12px rgba(62,77,126,.05)!important}.top-workspace-tab.active{background:linear-gradient(120deg,#e8edff,#e2f3f0)!important;border-color:rgba(112,132,213,.35)!important;color:#5367bf!important;box-shadow:0 6px 16px rgba(83,103,191,.11)!important}.topbar-actions{position:absolute!important;right:12px!important;top:9px!important}.api-status{display:none!important}.theme-button,.mobile-menu-button{background:rgba(255,255,255,.82)!important;border:1px solid rgba(137,153,190,.2)!important;border-radius:13px!important;color:#5968b7!important;box-shadow:0 4px 12px rgba(62,77,126,.07)!important}.content-heading-row{padding:12px 14px!important}.current-page-icon{background:linear-gradient(145deg,#fff,#e9edff)!important;border:1px solid rgba(137,153,190,.2)!important;border-radius:17px!important;box-shadow:0 7px 18px rgba(70,85,140,.09)!important;color:#6374d4!important}.current-page-copy h1{color:#202638!important}.current-page-copy p{color:#69758d!important}.workspace-frame-wrap{background:rgba(255,255,255,.58)!important;border:1px solid rgba(255,255,255,.8)!important;border-radius:22px!important;box-shadow:0 12px 32px rgba(62,77,126,.07)!important;overflow:hidden!important}.terminal-footer{background:rgba(247,249,255,.85)!important;color:#68748c!important;border-top:1px solid rgba(137,153,190,.17)!important}@media(max-width:768px){.terminal-topbar{padding:8px 10px 7px!important}.topbar-left{padding-right:76px!important}.top-workspace-tabs{margin:0!important}.top-workspace-tab{font-size:11px!important}.content-heading-row{margin:0!important}.terminal-footer{font-size:10px!important}}';function put(d,id,c){if(!d||!d.head)return;var s=d.getElementById(id);if(!s){s=d.createElement('style');s.id=id;d.head.appendChild(s)}s.textContent=c}put(document,'nifty-apk-outer-theme',css);function child(){var f=document.querySelector('iframe.workspace-frame');if(!f)return;try{var p=document.getElementById('nifty-apk-theme');put(f.contentDocument,'nifty-apk-inner-theme',p?p.textContent:'')}catch(e){}}child();document.querySelectorAll('iframe.workspace-frame').forEach(function(f){f.addEventListener('load',child)});new MutationObserver(child).observe(document.documentElement,{childList:true,subtree:true});setInterval(child,1000)})();";
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
