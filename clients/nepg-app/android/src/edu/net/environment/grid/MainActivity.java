package edu.net.environment.grid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.*;
import android.net.Uri;
import android.graphics.Color;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** The APK ships its Vue interface locally; only API traffic leaves this Activity. */
public final class MainActivity extends Activity {
    private static final String ASSET_ORIGIN = "https://qingjing.local";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private WebView web;
    private volatile String server;
    private volatile String cookie = "";
    private volatile long sessionGeneration = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        server = getPreferences(MODE_PRIVATE).getString("server", "http://10.0.2.2:8080");
        getWindow().setStatusBarColor(Color.rgb(23,103,67));
        getWindow().setNavigationBarColor(Color.rgb(244,247,242));
        web = new WebView(this);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setTextZoom(100);
        web.addJavascriptInterface(new ApiBridge(), "GridNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if (!("https".equals(uri.getScheme()) && "qingjing.local".equals(uri.getHost()))) return empty();
                String path = uri.getPath();
                if (path == null || !path.startsWith("/app/") || path.contains("..") || path.contains("\\")) return empty();
                String asset = path.substring(5);
                String mime = asset.endsWith(".js") ? "application/javascript" : asset.endsWith(".css") ? "text/css" : asset.endsWith(".svg") ? "image/svg+xml" : "text/html";
                try { return new WebResourceResponse(mime, "UTF-8", getAssets().open(asset)); }
                catch (IOException e) { return empty(); }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setTitle("确认实测")
                    .setMessage(message).setPositiveButton("确认提交",(dialog,which)->result.confirm())
                    .setNegativeButton("取消",(dialog,which)->result.cancel()).setOnCancelListener(dialog->result.cancel()).show();
                return true;
            }
        });
        setContentView(web);
        web.loadUrl(ASSET_ORIGIN + "/app/index.html");
    }
    private WebResourceResponse empty() { return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0])); }
    @Override public void onBackPressed() { web.evaluateJavascript("window.appBack && window.appBack()", null); }
    @Override protected void onDestroy() { network.shutdownNow(); web.removeJavascriptInterface("GridNative"); web.destroy(); super.onDestroy(); }

    public final class ApiBridge {
        @JavascriptInterface public String getBaseUrl() { return server; }
        @JavascriptInterface public String configure(String value) {
            try {
                URI uri = new URI(value.replaceAll("/+$", ""));
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty())) return "请输入完整服务器地址，不含 /api 或其他路径";
                sessionGeneration++; cookie = ""; server = uri.toString();
                getPreferences(MODE_PRIVATE).edit().putString("server", server).apply(); return "ok";
            } catch (Exception e) { return "服务器地址格式不正确"; }
        }
        @JavascriptInterface public void closeApp() { runOnUiThread(() -> finish()); }
        @JavascriptInterface public void request(int id, String method, String path, String body) {
            final long generation = sessionGeneration;
            network.execute(() -> {
                int status = 503; String response = "{\"code\":503,\"message\":\"无法连接后端，请检查服务器地址及网络\",\"data\":null}";
                HttpURLConnection connection = null;
                try {
                    URI relative = new URI(path);
                    String route = relative.getPath();
                    boolean read = "GET".equals(method) && (route.equals("/health") || route.equals("/auth/me")
                        || route.startsWith("/dictionaries/") || route.matches("/grid/tasks(?:/[0-9]+)?"));
                    boolean write = "POST".equals(method) && (route.equals("/auth/login") || route.equals("/auth/logout")
                        || route.matches("/grid/tasks/[0-9]+/measurement"));
                    if (relative.isAbsolute() || relative.getRawAuthority() != null || !path.startsWith("/") || path.contains("..")
                        || relative.getFragment() != null || !(read || write)) throw new IllegalArgumentException("不允许的接口");
                    if (route.equals("/auth/login") && !"grid".equals(new JSONObject(body).optString("role"))) throw new IllegalArgumentException("仅允许网格员登录");
                    connection = (HttpURLConnection)new URL(server + "/api" + path).openConnection();
                    connection.setConnectTimeout(10000); connection.setReadTimeout(15000); connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod(method); connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
                    if ("POST".equals(method)) { connection.setDoOutput(true); try(OutputStream out = connection.getOutputStream()) { out.write((body.isEmpty()?"{}":body).getBytes(StandardCharsets.UTF_8)); } }
                    status = connection.getResponseCode();
                    if (generation != sessionGeneration) throw new IllegalStateException("服务地址已更改，请重新登录");
                    for (Map.Entry<String,List<String>> entry : connection.getHeaderFields().entrySet()) {
                        if ("Set-Cookie".equalsIgnoreCase(entry.getKey())) for(String value : entry.getValue()) {
                            if(value.startsWith("JSESSIONID=")) cookie = value.split(";",2)[0];
                        }
                    }
                    InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream();
                    try(InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        if (in == null) throw new IOException("服务无响应");
                        byte[] buffer = new byte[4096]; int count; while((count=in.read(buffer))!=-1) out.write(buffer,0,count);
                        response = out.toString("UTF-8");
                    }
                    if(status == 401 || (route.equals("/auth/logout") && status == 200)) cookie = "";
                } catch (IllegalArgumentException e) {
                    status = 400; response = "{\"code\":400,\"message\":\"接口或参数不正确\",\"data\":null}";
                } catch(Exception e) { status = 503; }
                finally { if(connection != null) connection.disconnect(); }
                final int resultStatus = status; final String resultBody = response;
                runOnUiThread(() -> { if(!isFinishing()) web.evaluateJavascript("window.__nativeResult(" + id + "," + resultStatus + "," + JSONObject.quote(resultBody) + ")", null); });
            });
        }
    }
}
