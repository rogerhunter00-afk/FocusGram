package uk.rogerhunter.focusgram.dm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView web;
    private TextView status;
    private ValueCallback<Uri[]> files;
    private PermissionRequest microphone;
    private String script;
    private boolean loadingError;
    private static final int PICK = 10, MIC = 11;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        WebView.setWebContentsDebuggingEnabled(false);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        // Handle status/navigation bars without covering chat controls.
        layout.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        LinearLayout toolbar = new LinearLayout(this);
        Button inbox = new Button(this); inbox.setText("Inbox");
        inbox.setOnClickListener(v -> web.loadUrl(NavigationPolicy.INBOX));
        Button refresh = new Button(this); refresh.setText("Reload");
        refresh.setOnClickListener(v -> web.reload());
        Button help = new Button(this); help.setText("Help");
        help.setOnClickListener(v -> showHelp());
        toolbar.addView(inbox); toolbar.addView(refresh); toolbar.addView(help);
        layout.addView(toolbar);
        status = new TextView(this); status.setPadding(16, 4, 16, 4);
        status.setText("Connecting to Instagram…"); layout.addView(status);
        web = new WebView(this);
        layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(true); s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSaveFormData(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        try (InputStream input = getAssets().open("dm-only.js")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            script = bytes.toString("UTF-8");
        } catch (Exception e) { status.setText("App files unavailable. Please reinstall."); return; }
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return navigate(request.getUrl().toString());
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                if (NavigationPolicy.route(url) == NavigationPolicy.Route.INBOX) {
                    view.stopLoading(); view.loadUrl(NavigationPolicy.INBOX); return;
                }
                loadingError = false;
                status.setText("Loading Instagram…"); status.setVisibility(View.VISIBLE);
            }
            @Override public void onPageCommitVisible(WebView view, String url) { inject(url); }
            @Override public void onPageFinished(WebView view, String url) {
                inject(url); CookieManager.getInstance().flush();
                if (!loadingError) status.setVisibility(View.GONE);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    loadingError = true;
                    status.setText("Couldn't connect. Check your internet and tap Reload.");
                    status.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.cancel(); loadingError = true;
                status.setText("Secure connection failed. Check your phone's date and connection.");
                status.setVisibility(View.VISIBLE);
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) {
                    loadingError = true;
                    status.setText("Instagram couldn't load this page. Tap Reload to try again.");
                    status.setVisibility(View.VISIBLE);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (!NavigationPolicy.direct(web.getUrl())) return false;
                if (files != null) files.onReceiveValue(null);
                files = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("*/*"); pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(Intent.createChooser(pick, "Choose a photo or video"), PICK); }
                catch (Exception e) { files.onReceiveValue(null); files = null; toast("No photo picker available."); }
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                boolean audio = false;
                for (String resource : request.getResources()) if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) audio = true;
                if (!audio || !NavigationPolicy.instagram(java.net.URI.create(request.getOrigin().toString()))
                    || !NavigationPolicy.direct(web.getUrl())) { request.deny(); return; }
                if (microphone != null) microphone.deny();
                microphone = request;
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) grantMic();
                else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC);
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                if (microphone == request) microphone = null;
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean gesture, android.os.Message message) {
                if (!gesture) return false;
                // Temporary window only forwards its first URL, then destroys
                // itself. It never renders an unrestricted browsing surface.
                WebView popup = new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                        String url = r.getUrl().toString();
                        if (!navigate(url)) web.loadUrl(url);
                        v.post(v::destroy); return true;
                    }
                });
                ((WebView.WebViewTransport) message.obj).setWebView(popup); message.sendToTarget(); return true;
            }
        });
        web.loadUrl(NavigationPolicy.INBOX);
        if (!getPreferences(0).getBoolean("introduced", false)) {
            showHelp(); getPreferences(0).edit().putBoolean("introduced", true).apply();
        }
    }
    private void inject(String url) {
        if (NavigationPolicy.route(url) == NavigationPolicy.Route.INTERNAL) web.evaluateJavascript(script, null);
    }
    private boolean navigate(String url) {
        switch (NavigationPolicy.route(url)) {
            case INTERNAL: return false;
            case INBOX: web.loadUrl(NavigationPolicy.INBOX); return true;
            case EXTERNAL:
                new AlertDialog.Builder(this).setTitle("Open outside FocusGram?")
                    .setMessage("This link will open in another app or your browser.")
                    .setPositiveButton("Open", (d, which) -> {
                        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                        catch (Exception e) { toast("No app can open this link."); }
                    }).setNegativeButton("Cancel", null).show(); return true;
            default: toast("This link isn't supported."); return true;
        }
    }
    private void grantMic() {
        if (microphone != null) {
            if (NavigationPolicy.direct(web.getUrl())) microphone.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            else microphone.deny();
            microphone = null;
        }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == MIC && microphone != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) grantMic();
            else { microphone.deny(); microphone = null; toast("Microphone access denied."); }
        }
    }
    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == PICK && files != null) {
            ArrayList<Uri> selected = new ArrayList<>();
            if (result == RESULT_OK && data != null && NavigationPolicy.direct(web.getUrl())) {
                if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) selected.add(data.getClipData().getItemAt(i).getUri());
                else if (data.getData() != null) selected.add(data.getData());
            }
            ArrayList<Uri> safe = new ArrayList<>();
            for (Uri uri : selected) {
                String type = getContentResolver().getType(uri);
                if ("content".equals(uri.getScheme()) && type != null && (type.startsWith("image/") || type.startsWith("video/"))) safe.add(uri);
            }
            files.onReceiveValue(safe.isEmpty() ? null : safe.toArray(new Uri[0])); files = null;
        }
    }
    @Override public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        String url = web.getUrl();
        if (NavigationPolicy.direct(url) && !Uri.parse(url).getPath().equals("/direct/inbox/")) {
            web.loadUrl(NavigationPolicy.INBOX);
        } else if (!NavigationPolicy.direct(url) && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() {
        super.onPause(); CookieManager.getInstance().flush(); if (web != null) web.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (files != null) files.onReceiveValue(null);
        if (microphone != null) microphone.deny();
        if (web != null) web.destroy(); super.onDestroy();
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("FocusGram DMs")
            .setMessage("Sign in on Instagram's own page using your Instagram username and password. Login stays on this phone; Android backup is disabled.\n\nFeed, Reels, Explore and Stories are blocked. Photos and voice messages depend on what Instagram's website offers your account. Allow microphone access only if recording a voice message.\n\nBackground message notifications and calls are NOT supported. Keep official Instagram for reliable alerts if needed.\n\nIndependent open-source app, not affiliated with Meta. Based on FocusGram by Ujwal Chapagain. AGPL-3.0 source: github.com/rogerhunter00-afk/FocusGram")
            .setPositiveButton("OK", null)
            .setNeutralButton("Source", (d, w) -> navigate("https://github.com/rogerhunter00-afk/FocusGram"))
            .setNegativeButton("Sign out", (d, w) -> new AlertDialog.Builder(this)
                .setMessage("Remove the Instagram session and website storage from this phone?")
                .setNegativeButton("Cancel", null).setPositiveButton("Sign out", (confirm, which) -> {
                    web.stopLoading(); web.loadUrl("about:blank");
                    CookieManager.getInstance().removeAllCookies(done -> {
                        CookieManager.getInstance().flush(); WebStorage.getInstance().deleteAllData();
                        web.clearCache(true); web.clearHistory(); web.loadUrl(NavigationPolicy.INBOX);
                    });
                }).show()).show();
    }
}
