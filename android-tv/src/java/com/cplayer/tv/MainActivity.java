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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainActivity extends Activity {

  private static final String REMOTE_TV_URL = "http://51.222.205.60/player/tv.html";
  private static final String CACHED_FILE_NAME = "tv_cached.html";
  private static final String FALLBACK_ASSET_URL = "file:///android_asset/index.html";

  private WebView web;
  private boolean checkStarted = false;

  @Override
  protected void onCreate(Bundle b) {
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
      @Override
      public void onReceivedSslError(WebView v, android.webkit.SslErrorHandler h, android.net.http.SslError e) {
        h.proceed();
      }
    });
    web.setWebChromeClient(new WebChromeClient());
    web.setFocusable(true);
    web.setFocusableInTouchMode(true);

    setContentView(web);
    hideBars();

    final File cachedFile = new File(getFilesDir(), CACHED_FILE_NAME);
    if (cachedFile.exists() && cachedFile.length() > 0) {
      web.loadUrl("file://" + cachedFile.getAbsolutePath());
    } else {
      web.loadUrl(FALLBACK_ASSET_URL);
    }

    web.requestFocus();

    if (!checkStarted) {
      checkStarted = true;
      checkAndUpdateTvHtml(cachedFile);
    }
  }

  private void checkAndUpdateTvHtml(final File cachedFile) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        HttpURLConnection conn = null;
        try {
          URL url = new URL(REMOTE_TV_URL);
          conn = (HttpURLConnection) url.openConnection();
          conn.setConnectTimeout(8000);
          conn.setReadTimeout(12000);
          conn.setUseCaches(false);
          conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
          conn.setRequestProperty("Pragma", "no-cache");

          if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return;
          }

          StringBuilder sb = new StringBuilder();
          InputStream in = null;
          BufferedReader br = null;
          try {
            in = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
              sb.append(line).append('\n');
            }
          } finally {
            if (br != null) {
              try { br.close(); } catch (Exception ignored) {}
            }
            if (in != null) {
              try { in.close(); } catch (Exception ignored) {}
            }
          }

          String remoteHtml = sb.toString();
          String lower = remoteHtml.toLowerCase();
          if (!lower.contains("<html") && !lower.contains("<!doctype html")) {
            return;
          }

          byte[] remoteBytes = remoteHtml.getBytes(StandardCharsets.UTF_8);
          String remoteHash = sha256(remoteBytes);
          String localHash = cachedFile.exists() ? sha256File(cachedFile) : "";

          if (!remoteHash.equals(localHash)) {
            FileOutputStream out = null;
            try {
              out = new FileOutputStream(cachedFile, false);
              out.write(remoteBytes);
              out.flush();
            } finally {
              if (out != null) {
                try { out.close(); } catch (Exception ignored) {}
              }
            }
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (web != null && !isFinishing()) {
                  web.loadUrl("file://" + cachedFile.getAbsolutePath());
                  web.requestFocus();
                }
              }
            });
          }
        } catch (Exception ignored) {
          // Sem rede ou erro na consulta: mantém a versão atual
        } finally {
          if (conn != null) {
            conn.disconnect();
          }
        }
      }
    }).start();
  }

  private String sha256(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data);
    StringBuilder hex = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  private String sha256File(File file) {
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) != -1) {
        digest.update(buf, 0, n);
      }
      byte[] hash = digest.digest();
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      return "";
    } finally {
      if (in != null) {
        try { in.close(); } catch (Exception ignored) {}
      }
    }
  }

  private void hideBars() {
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  @Override
  public void onWindowFocusChanged(boolean f) {
    super.onWindowFocusChanged(f);
    if (f) hideBars();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (e.getAction() == KeyEvent.ACTION_UP) {
        web.evaluateJavascript(
            "(window.tvBack&&window.tvBack())?'1':'0'",
            new android.webkit.ValueCallback<String>() {
              @Override
              public void onReceiveValue(String v) {
                if (!"\"1\"".equals(v)) finish();
              }
            });
      }
      return true;
    }
    return super.dispatchKeyEvent(e);
  }

  @Override
  protected void onPause() {
    super.onPause();
    web.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    web.onResume();
  }
}
