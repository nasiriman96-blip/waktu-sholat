package id.waktusholat.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingPrayer;
    private static final int REQ_NOTIF = 12, REQ_LOC = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                deliverPrayer();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });

        pendingPrayer = getIntent().getStringExtra("prayer");
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingPrayer = intent.getStringExtra("prayer");
        deliverPrayer();
    }

    private void deliverPrayer() {
        if (pendingPrayer == null || web == null) return;
        final String p = pendingPrayer.replaceAll("[^a-z]", "");
        pendingPrayer = null;
        web.evaluateJavascript("window.__pick&&window.__pick('" + p + "')", null);
    }

    private void askNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    private void sendLoc(final Double lat, final Double lon) {
        final String js = (lat == null) ? "window.onLoc&&window.onLoc(null,null)" : "window.onLoc&&window.onLoc(" + lat + "," + lon + ")";
        runOnUiThread(new Runnable() { @Override public void run() { web.evaluateJavascript(js, null); } });
    }

    private void fetchLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location best = null;
            for (String prov : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER}) {
                try {
                    Location l = lm.getLastKnownLocation(prov);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (Exception ignored) { }
            }
            if (best != null) { sendLoc(best.getLatitude(), best.getLongitude()); return; }
            String prov = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                    : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
            if (prov == null) { sendLoc(null, null); return; }
            lm.requestSingleUpdate(prov, new LocationListener() {
                @Override public void onLocationChanged(Location l) { sendLoc(l.getLatitude(), l.getLongitude()); }
                @Override public void onStatusChanged(String p, int st, Bundle e) { }
                @Override public void onProviderEnabled(String p) { }
                @Override public void onProviderDisabled(String p) { }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            sendLoc(null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
        if (code == REQ_LOC) { if (ok) fetchLocation(); else sendLoc(null, null); }
        if (code == REQ_NOTIF) web.evaluateJavascript("window.onNotifPerm&&window.onNotifPerm(" + ok + ")", null);
    }

    private class Bridge {
        @JavascriptInterface
        public String getNotif() {
            try {
                SharedPreferences p = NotifScheduler.prefs(MainActivity.this);
                JSONObject o = new JSONObject();
                o.put("on", p.getBoolean("on", false));
                o.put("delay", p.getInt("delay", 15));
                o.put("mask", p.getInt("mask", 31));
                o.put("city", p.getString("city", ""));
                if (NotifScheduler.hasLocation(p)) {
                    o.put("lat", Double.longBitsToDouble(p.getLong("lat", 0)));
                    o.put("lon", Double.longBitsToDouble(p.getLong("lon", 0)));
                }
                o.put("perm", Build.VERSION.SDK_INT < 33
                        || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
                String[] t = NotifScheduler.todayTimes(MainActivity.this);
                if (t != null) { JSONArray a = new JSONArray(); for (String x : t) a.put(x); o.put("times", a); }
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void setNotif(final String json) {
            try {
                JSONObject o = new JSONObject(json);
                SharedPreferences.Editor e = NotifScheduler.prefs(MainActivity.this).edit();
                e.putBoolean("on", o.optBoolean("on", false));
                e.putInt("delay", o.optInt("delay", 15));
                e.putInt("mask", o.optInt("mask", 31));
                e.putString("city", o.optString("city", ""));
                if (o.has("lat") && o.has("lon")) {
                    e.putLong("lat", Double.doubleToLongBits(o.getDouble("lat")));
                    e.putLong("lon", Double.doubleToLongBits(o.getDouble("lon")));
                }
                e.commit();
                final boolean on = o.optBoolean("on", false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (on) askNotifPermission();
                        NotifScheduler.schedule(MainActivity.this);
                    }
                });
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void useLocation() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
                    } else {
                        fetchLocation();
                    }
                }
            });
        }

        @JavascriptInterface
        public void setTheme(final boolean dark) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int c = dark ? 0xFF050A08 : 0xFFEDF5F0;
                    getWindow().setStatusBarColor(c);
                    getWindow().setNavigationBarColor(c);
                    int flags = dark ? 0 : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                    web.setBackgroundColor(c);
                }
            });
        }

        @JavascriptInterface
        public void vibrate(String json) {
            try {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v == null || !v.hasVibrator()) return;
                JSONArray a = new JSONArray(json);
                long[] p = new long[a.length() + 1];
                p[0] = 0;
                for (int i = 0; i < a.length(); i++) p[i + 1] = a.getLong(i);
                if (p.length == 2) {
                    v.vibrate(VibrationEffect.createOneShot(p[1], VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(VibrationEffect.createWaveform(p, -1));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__back&&window.__back())===true", value -> {
            if (!"true".equals(value)) finish();
        });
    }
}
