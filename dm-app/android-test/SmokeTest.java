package uk.rogerhunter.focusgram.dm.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Synthetic fixture test: never signs in, reads messages or uses a real account. */
public class SmokeTest extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("MainActivity", 0).edit().putBoolean("introduced", true).commit();
            Intent launch = getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = startActivitySync(launch);
            Field field = activity.getClass().getDeclaredField("web"); field.setAccessible(true);
            WebView web = (WebView) field.get(activity);
            if ((getTargetContext().getApplicationInfo().flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0) throw new AssertionError("backup enabled");
            runOnMainSync(() -> {
                if (web.getSettings().getAllowFileAccess()) throw new AssertionError("file access enabled");
                if (CookieManager.getInstance().acceptThirdPartyCookies(web)) throw new AssertionError("third-party cookies enabled");
                web.stopLoading();
                web.loadDataWithBaseURL("https://www.instagram.com/direct/inbox/",
                    "<html><head></head><body><a id='home' href='/'>Home</a><a id='reels' href='/reels/'>Reels</a><a id='dm' href='/direct/t/1/'>Chat</a><input id='search'><input type='file' id='photo'><img id='image' alt='Chat photo'><audio id='audio' controls></audio><textarea id='message'></textarea></body></html>",
                    "text/html", "UTF-8", null);
            });
            String value = "";
            for (int attempt = 0; attempt < 40; attempt++) {
                value = eval(web, "Boolean(window.__focusgramDMs && document.getElementById('reels') && getComputedStyle(document.getElementById('reels')).display==='none')");
                if ("true".equals(value)) break;
                Thread.sleep(250);
            }
            if (!"true".equals(value)) throw new AssertionError("DM script did not finish hiding navigation: " + eval(web, "JSON.stringify({url:location.href,ready:document.readyState,script:window.__focusgramDMs,style:!!document.getElementById('focusgram-dm-style'),html:document.body.innerHTML})"));
            if (!"true".equals(eval(web, "['home','reels'].every(id=>getComputedStyle(document.getElementById(id)).display==='none')"))) throw new AssertionError("distractions visible");
            if (!"true".equals(eval(web, "['dm','search','photo','image','audio','message'].every(id=>getComputedStyle(document.getElementById(id)).display!=='none')"))) throw new AssertionError("chat controls hidden");
            eval(web, "history.pushState({},'', '/direct/t/123/'); location.pathname");
            if (!"\"/direct/t/123/\"".equals(eval(web, "location.pathname"))) throw new AssertionError("DM navigation broken");
            CountDownLatch backRequest = new CountDownLatch(1);
            AtomicReference<String> destination = new AtomicReference<>();
            runOnMainSync(() -> {
                // Intercept the test's final navigation to avoid using the live
                // service. Exercise the real Activity Back handler unchanged.
                web.setWebViewClient(new WebViewClient() {
                    @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                        destination.set(url); backRequest.countDown();
                    }
                    @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                        return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream("<html>Inbox fixture</html>".getBytes()));
                    }
                });
                activity.onBackPressed();
            });
            if (!backRequest.await(5, TimeUnit.SECONDS) || !"https://www.instagram.com/direct/inbox/".equals(destination.get())) throw new AssertionError("Android Back did not return to inbox: " + destination.get());
            result.putString("stream", "Passed Android WebView fixture checks: launch, script injection, distraction hiding, chat controls, DM SPA navigation, Android Back to inbox, file-access policy, third-party cookie policy, backup disabled. No real Instagram account used.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAILED: " + error.toString() + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private String eval(WebView web, String js) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> value = new AtomicReference<>();
        runOnMainSync(() -> web.evaluateJavascript(js, response -> { value.set(response); done.countDown(); }));
        if (!done.await(5, TimeUnit.SECONDS)) throw new AssertionError("JS timeout");
        return value.get();
    }
}
