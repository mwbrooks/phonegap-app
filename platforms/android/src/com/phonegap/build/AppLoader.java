package com.phonegap.build;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.*;

import org.apache.cordova.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AppLoader extends CordovaPlugin {

    CallbackContext cb;
    String zipPath;
    String extractPath;
    String indexPath;
    
    Context context;
    
    public boolean initialize() {
        
        context = cordova.getActivity().getApplicationContext();
        
        zipPath = "/data/data/" + context.getPackageName() +
                "/app.zip";
        extractPath = "/data/data/" + context.getPackageName() +
                "/hydra_app/";
        indexPath = extractPath + "index.html";
        
        return true;
        /*
        boolean firstRun = false;
        File indexFile = new File(indexPath);
        if (!indexFile.exists()) {
            Log.d("AppLoader", "No index file, installing startup.zip");
            firstRun = true;
            try {
                InputStream in = context.getAssets().open("startup.zip");
                FileOutputStream out = new FileOutputStream(zipPath);
                copyStream(in, out);
                
                this.installApp(zipPath);
            } catch (Exception e) {
                Log.d("AppLoader", "No startup.zip in app bundle");
                e.printStackTrace();
            }
        } else {
            Log.d("AppLoader", "Found existing installed app");
        }
        return firstRun;
        */
    }

    @TargetApi(19)
	@Override
    public boolean execute(
        String action, JSONArray args, final CallbackContext callbackContext ){

        this.cb = callbackContext;
        
        this.initialize();
        
        if (action.equals("load")) {
            File f = new File(indexPath);
            if (!f.exists()) {
                PluginResult res = new PluginResult(
                    PluginResult.Status.ERROR,
                    "No hydration files found at " + indexPath
                    );
                res.setKeepCallback(false);
                cb.sendPluginResult(res);
                return true;
            }
            injectHomeScript(callbackContext);
            super.webView.loadUrl("file://" + extractPath + "index.html");
        }

        if (action.equals("fetch")) {
            String url;
            try {
                url = (String) args.get(0);
            } catch (JSONException e1) {
                PluginResult r = new PluginResult(
                        PluginResult.Status.ERROR,
                        e1.getMessage()
                        );
                r.setKeepCallback(false);
                cb.sendPluginResult(r);
                return true;
            }
            
            try {
                this.download(url, zipPath, extractPath);
            } catch (Exception e) {
                PluginResult r = new PluginResult(
                        PluginResult.Status.ERROR,
                        e.getMessage()
                        );
                r.setKeepCallback(false);
                cb.sendPluginResult(r);
                return true;
            }
        }

        PluginResult r = new PluginResult(
            PluginResult.Status.NO_RESULT
            );
        r.setKeepCallback(true);
        cb.sendPluginResult(r);
        return true;
    }


    public void download(String url, String zipPath, String extractPath)
            throws Exception {

        DefaultHttpClient http_client = new DefaultHttpClient();
        
        HttpGet get = new HttpGet(url);
        HttpResponse resp = http_client.execute(get);
        
        BufferedInputStream download = new BufferedInputStream(
                resp.getEntity().getContent()
                );
        
        File tmp = new File(zipPath);
        if (tmp.exists()) {
            tmp.delete();
        } else {
            tmp.getParentFile().mkdirs();
        }
        
        FileOutputStream file = new FileOutputStream(zipPath);

        int bytesRead = 0;
        long totalBytesRead = 0;
        long totalBytesToRead = resp.getEntity().getContentLength();
        
        if (totalBytesToRead == 0) {
            throw new Exception("... lets not divide by zero");
        }
        
        byte[] bytes = new byte[1024];
        float percentage = 0.0f;
        float nextUpdatePercent = 5.0f;
        
        while ((bytesRead = download.read(bytes)) >= 0) {
            file.write(bytes, 0, bytesRead);
            totalBytesRead += bytesRead;
            percentage = 100.0f * ((float) totalBytesRead / (float) totalBytesToRead);
            
            // only write at 5% increments
            if (percentage >= nextUpdatePercent) {
            	/*
                PluginResult r = new PluginResult(
                        PluginResult.Status.OK,
                        AppLoader.message(
                            "downloading", Float.toString(percentage)
                            )
                        );
                r.setKeepCallback(true);
                */
            	Log.d("AppLoader", Float.toString(percentage));
                nextUpdatePercent += 5.0f;
                //cb.sendPluginResult(r);
            }
            // force a write
            file.flush();
        }
        
        // just in case something is still lingering
        resp.getEntity().consumeContent();
        file.close();
        
        this.installApp(zipPath);
    }
    
    public void installApp(String path) throws Exception {
        ZipFile zip = new ZipFile(zipPath);
        Enumeration<? extends ZipEntry> entries = zip.entries(); 

        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File zipFile = new File(extractPath + entry.getName());
            if (!entry.isDirectory()) {
                zipFile.getParentFile().mkdirs();
                copyStream(
                        zip.getInputStream(entry),
                        new BufferedOutputStream(
                                new FileOutputStream(
                                    extractPath + entry.getName()
                                    )
                                )
                        );
            }
        }
        zip.close();
        
        // copy cordova.js from Hydra to downloaded app
        InputStream in = context.getAssets().open("www/cordova.js");
        FileOutputStream out = new FileOutputStream(extractPath + "cordova.js");
        copyStream(in, out);

        // copy cordova.js from Hydra to downloaded app
        in = context.getAssets().open("www/cordova.js");
        out = new FileOutputStream(extractPath + "phonegap.js");
        copyStream(in, out);
        
        PluginResult complete = new PluginResult(
                PluginResult.Status.OK,
                AppLoader.message("complete", "")
                );
        complete.setKeepCallback(false);
        cb.sendPluginResult(complete);
    }
    
    private void injectHomeScript(final CallbackContext callbackContext) {

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
	            webView.setWebViewClient(new WebViewClient(){
	    			@Override
	    	        public void onPageFinished(WebView view, String url) {
	    				// Michael Brooks' homepage.js (https://github.com/phonegap/connect-phonegap/blob/master/res/middleware/homepage.js)
	    	            String javascript="javascript: console.log('adding homepage.js'); (function(){var e={},t={touchstart:'touchstart',touchend:'touchend'};if(window.navigator.msPointerEnabled){t={touchstart:'MSPointerDown',touchend:'MSPointerUp'}}document.addEventListener(t.touchstart,function(t){var n=t.touches||[t],r;for(var i=0,s=n.length;i<s;i++){r=n[i];e[r.identifier||r.pointerId]=r}},false);document.addEventListener(t.touchend,function(t){var n=Object.keys(e).length;e={};if(n===3){t.preventDefault();window.history.back(window.history.length)}},false)})(window)";
	    	            view.loadUrl(javascript);
	    	        }
	    		});
	            //callbackContext.success();
	        }
	    });
    }
    
    private void copyStream(InputStream in, OutputStream out)
        throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }

    private static JSONObject message(String state, String status)
        throws JSONException {

        JSONObject json = new JSONObject();
        json.put("state", state);
        json.put("status", status);
        return json;
    }
}
