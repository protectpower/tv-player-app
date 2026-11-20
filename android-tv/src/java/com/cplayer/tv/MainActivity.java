package com.cplayer.tv;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class MainActivity extends Activity {
  private WebView web;
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(true);
    s.setAllowUniversalAccessFromFileURLs(true);
    s.setAllowFileAccessFromFileURLs(true);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    s.setLoadsImagesAutomatically(true);
    s.setBlockNetworkImage(false);
    web.setWebViewClient(new WebViewClient() {
      @Override public void onReceivedSslError(WebView v, android.webkit.SslErrorHandler h, android.net.http.SslError e) { h.proceed(); }
    });
    web.setWebChromeClient(new WebChromeClient());
    web.setFocusable(true);
    web.setFocusableInTouchMode(true);
    setContentView(web);
    hideBars();
    web.loadUrl("file:///android_asset/index.html");
    web.requestFocus();
  }
  private void hideBars() {
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }
  @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideBars(); }
  @Override public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (e.getAction() == KeyEvent.ACTION_UP) {
        web.evaluateJavascript("(window.tvBack&&window.tvBack())?'1':'0'", new android.webkit.ValueCallback<String>() { public void onReceiveValue(String v) { if (!"\"1\"".equals(v)) finish(); } });
      }
      return true;
    }
    return super.dispatchKeyEvent(e);
  }
  @Override protected void onPause() { super.onPause(); web.onPause(); }
  @Override protected void onResume() { super.onResume(); web.onResume(); }
}
