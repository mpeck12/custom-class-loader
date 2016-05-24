/*
 * Copyright 2011 Google Inc.
 * Copyright 2015 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.dex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.cert.X509Certificate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.app.ActionBar;
import android.widget.EditText;



import com.example.dex.R;

import dalvik.system.DexClassLoader;


public class MainActivity extends Activity {

   // from http://blog.denevell.org/android-trust-all-ssl-certificates.html
   TrustManager[] trustAllCerts = new TrustManager[] {
       new X509TrustManager() {
           @Override
           public X509Certificate[] getAcceptedIssuers() {
               X509Certificate[] myTrustAnchors = new X509Certificate[0];
               return myTrustAnchors;
           }

           @Override
           public void checkClientTrusted(X509Certificate[] certs, String authType) { }

           @Override
           public void checkServerTrusted(X509Certificate[] certs, String authType) { }
       }
    };

    public final static String EXTRA_MESSAGE = "com.example.dex.MESSAGE";
    private static final String SECONDARY_DEX_NAME = "secondary_dex.jar";
    private static final String SO_NAME = "libhello-jni.so";

    private static String dex_url;
    private static String nativeLib_url;

    private SharedPreferences sharedPref;
    // Buffer size for file copying.  While 8kb is used in this sample, you
    // may want to tweak it based on actual size of the secondary dex file involved.
    private static final int BUF_SIZE = 8 * 1024;

    private Button mToastButtonDEX = null;
    private Button mToastButtonLoadDex = null;
    private Button mToastButtonSO = null;
    private Button mToastButtonLoadSo = null;
    private Button mToastButtonLoadSoLocal = null;
    private Switch mInsecureSwitch = null;
    private boolean insecureFilePermissions = false;
    private boolean insecureTLS = false;
    private ProgressDialog mProgressDialog = null;
    private Display display;
    private SharedPreferences prefs;

    private native String stringFromJni();
    private native String stringFromLocalJni();
    private ActionBar actionbar;

    private void Toastit(String txt, Context ctx) {

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }

        return true;
    }

    static public void loadNative(Context ctx, File nativepath) {
        if (!nativepath.exists()) {
            Log.e("MainActivity","Native load error: "+nativepath+" does not exist");
            return;
        }
        try {
            System.load(nativepath.toString());
        } catch(Exception e) {
            Log.e("MainActivity", "System.load() Error: " + e.getMessage());
        }
        Log.e("MainActivity", "System.load() success! : " + nativepath.toString());
    }






    public void loadDex(Activity ctx, File nativepath) {

        File optimizedDexOutputPath;
        if (insecureFilePermissions)
          optimizedDexOutputPath = getDir("outdex", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE);
        else
          optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);

        // Initialize the class loader with the secondary dex file.
        DexClassLoader cl = new DexClassLoader(nativepath.getAbsolutePath(),
                optimizedDexOutputPath.getAbsolutePath(),
                null,
                getClassLoader());
        Class libProviderClazz = null;

        if (!optimizedDexOutputPath.exists()){
            Log.e("MainActivity", "error optimizedDexOutput path doesnot exist!");
        }


        try {
            // Load the library class from the class loader.
            libProviderClazz = cl.loadClass("com.example.dex.lib.LibraryProvider");


            // Cast the return object to the library interface so that the
            // caller can directly invoke methods in the interface.
            // Alternatively, the caller can invoke methods through reflection,
            // which is more verbose and slow.
            LibraryInterface lib = (LibraryInterface) libProviderClazz.newInstance();

            // Display the toast!
            lib.showAwesomeToast(ctx, "Supporting Code (.DEX FORMAT) Executed!");
        } catch (Exception exception) {
            // Handle exception gracefully here.
            exception.printStackTrace();
        }
    }


    /* from Helloworld-jni NDK samples */
    public void PrintFromNative(){

        final Context ctx = getApplicationContext();
        final CharSequence text = stringFromJni();
        final int duration = Toast.LENGTH_LONG;
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ctx, text, duration).show();
            }
        });

        return;

    }

    static public void loadNativeLocal(Context ctx){
        try {
            System.loadLibrary("app");
        } catch(Exception e) {
            Log.e("MainActivity", "System.load() local Error: " + e.getMessage());
        }
    }

    /* from Helloworld-jni NDK samples
     * Load  */
    public void PrintFromNativeLocal() {
        Context ctx = getApplicationContext();
        //loadNativeLocal(ctx);
        CharSequence text = stringFromLocalJni();
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(ctx, text, duration);
        toast.show();
        try {
            // Provide proof that the app mapped memory at a fixed address and as both writable
            // and executable
            InputStream in = new FileInputStream("/proc/" + android.os.Process.myPid() + "/maps");
            File outFile = new File(getExternalFilesDir(null), "maps");
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {} // Fail silently if any errors occur writing out memory mappings
        return;
    }

    private void refreshButtons() {
        if(insecureFilePermissions) {
          if(new File(getDir("dex", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE), SECONDARY_DEX_NAME).exists()) {
            mToastButtonLoadDex.setEnabled(true);
          } else {
            mToastButtonLoadDex.setEnabled(false);
          }
          if(new File(getDir("native", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE), SO_NAME).exists()) {
            mToastButtonLoadSo.setEnabled(true);
          } else {
            mToastButtonLoadSo.setEnabled(false);
          }
        } else {
          if(new File(getDir("dex", Context.MODE_PRIVATE), SECONDARY_DEX_NAME).exists()) {
            mToastButtonLoadDex.setEnabled(true);
          } else {
            mToastButtonLoadDex.setEnabled(false);
          }
          if(new File(getDir("native", Context.MODE_PRIVATE), SO_NAME).exists()) {
            mToastButtonLoadSo.setEnabled(true);
          } else {
            mToastButtonLoadSo.setEnabled(false);
          }
        }
    }

    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                DeleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean autorun = sharedPref.getBoolean("autorun", false);
        if (autorun) {
            Log.d("custom-class-loader", "autorun activated");
            dex_url = sharedPref.getString("DEX_url", "");
            new DownloadFileFromURL(false, true).execute(dex_url);
            nativeLib_url = sharedPref.getString("JNI_url", "");
            String abi = android.os.Build.CPU_ABI;
            if (nativeLib_url.endsWith("/")) {
                nativeLib_url = nativeLib_url + abi + "/libhello-jni.so";
            } else {
                nativeLib_url = nativeLib_url + "/" + abi + "/libhello-jni.so";
            }
            new DownloadFileFromURL(true, true).execute(nativeLib_url);
        }

        boolean autorun_local = sharedPref.getBoolean("autorun_local", false);
        if (autorun_local) {
                loadNativeLocal(MainActivity.this);
                PrintFromNativeLocal();
        }

        mToastButtonDEX = (Button) findViewById(R.id.toast_buttonDEX);
        mToastButtonLoadDex = (Button) findViewById(R.id.toast_button_load_DEX);
        mToastButtonSO = (Button) findViewById(R.id.toast_buttonSO);
        mToastButtonLoadSo = (Button) findViewById(R.id.toast_button_load_SO);
        mToastButtonLoadSoLocal = (Button) findViewById(R.id.toast_button_load_SO_Local);
        display = getWindowManager().getDefaultDisplay();

        Button cleanup = (Button) findViewById(R.id.cleanup);

        cleanup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Clear files
                DeleteRecursive(getDir("native", Context.MODE_PRIVATE));
                DeleteRecursive(getDir("dex", Context.MODE_PRIVATE));
                DeleteRecursive(getDir("outdex", Context.MODE_PRIVATE));
                refreshButtons();
            }
        });

        display = getWindowManager().getDefaultDisplay();

        refreshButtons();

        mToastButtonDEX.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // update URL from prefs
                dex_url = sharedPref.getString("DEX_url", "");
                new DownloadFileFromURL(false).execute(dex_url);
            }
        });

        mToastButtonSO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // update URL from prefs
                nativeLib_url = sharedPref.getString("JNI_url", "");
                String abi = android.os.Build.CPU_ABI;
                if (nativeLib_url.endsWith("/")) {
                    nativeLib_url = nativeLib_url + abi + "/libhello-jni.so";
                } else {
                    nativeLib_url = nativeLib_url + "/" + abi + "/libhello-jni.so";
                }
                new DownloadFileFromURL(true).execute(nativeLib_url);
            }
        });

        mToastButtonLoadDex.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (insecureFilePermissions) {
                  loadDex(MainActivity.this, new File(getDir("dex", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE), SECONDARY_DEX_NAME));
                } else {
                  loadDex(MainActivity.this, new File(getDir("dex", Context.MODE_PRIVATE), SECONDARY_DEX_NAME));
                }
            }
        });

        mToastButtonLoadSo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (insecureFilePermissions) {
                  loadNative(MainActivity.this, new File(getDir("native", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE), SO_NAME));
                } else {
                  loadNative(MainActivity.this, new File(getDir("native", Context.MODE_PRIVATE), SO_NAME));
                }
                PrintFromNative();
            }
        });
        mToastButtonLoadSoLocal.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                loadNativeLocal(MainActivity.this);
                PrintFromNativeLocal();
            }
        });

    }

    private class DownloadFileFromURL extends AsyncTask<String, String, String> {
        private ProgressDialog pDialog;
        private Context ctx = getApplicationContext();
        private boolean isNative;

        private String fname;
        private File nativePath;
        private boolean autorun = false;

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        DownloadFileFromURL(boolean Native, boolean autorun){
            this.isNative = Native;
            this.autorun = autorun;
        }

        DownloadFileFromURL(boolean Native) {
            this.isNative = Native;
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected synchronized String doInBackground(String... f_url) {

            int count;

            Log.e("BEFORE Starting ","download!");

            try {
                Log.e("custom-class-loader", "URL: " + f_url[0]);
                URL url = new URL(f_url[0]);
                String urlpath = url.getPath();
                Log.e("custom-class-loader", "URLpath: " + urlpath);

                if (urlpath.endsWith(".so")) {
                    fname = SO_NAME;
                } else if (urlpath.endsWith(".jar")) {
                    fname = SECONDARY_DEX_NAME;
                } else {
                    Log.e("MainActivity", "Skipping file download because file extension is not .so or .jar");
                    return null;
                }

                Log.e("MainActivity:","fname is:" + fname);
                insecureFilePermissions = sharedPref.getBoolean("insecure_perms", true);
                if (isNative) {
                    if (insecureFilePermissions) {
                      nativePath = new File(getDir("native", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE),fname.toString());
                    } else {
                      nativePath = new File(getDir("native", Context.MODE_PRIVATE),fname.toString());
                    }
                }
                else {
                    if (insecureFilePermissions) {
                      nativePath = new File(getDir("dex", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE),fname.toString());
                    } else {
                      nativePath = new File(getDir("dex", Context.MODE_PRIVATE),fname.toString());
                    }
                }

                Log.e("MainActivity", "native path is: " + nativePath.toString());

                Log.e("MainActivity", "connecting to:" + url.toString());

                insecureTLS = sharedPref.getBoolean("insecure_tls", false);

                SSLContext sslContext = null;
                try {
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustAllCerts, null);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                HttpsURLConnection connection2 = null;
                if (connection instanceof HttpsURLConnection) {
                    connection2 = (HttpsURLConnection) connection;
                }
                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) { return true; }
                };

                if (insecureTLS && connection2 != null) {
                    Log.e("MainActivity", "Setting insecure SSLContext and HostnameVerifier");
                    connection2.setSSLSocketFactory(sslContext.getSocketFactory());
                    connection2.setHostnameVerifier(hv);
                }

                int len;
                InputStream input;

                if (connection2 != null) {
                    connection2.connect();
                    len = connection2.getContentLength();
                    input = connection2.getInputStream();
                } else {
                    connection.connect();
                    len = connection.getContentLength();
                    input = connection.getInputStream();
                }

                OutputStream output = new BufferedOutputStream(new FileOutputStream(nativePath));

                byte data[] = new byte[len];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                if (connection2 != null) {
                    connection2.disconnect();
                }
                else if (connection != null) {
                    connection.disconnect();
                }
                if (insecureFilePermissions) {
                    nativePath.setReadable(true, false);
                    nativePath.setWritable(true, false);
                }
                if (autorun && !isNative) {
                    loadDex(MainActivity.this, new File(getDir("dex", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE), SECONDARY_DEX_NAME));
                }
                if (autorun && isNative) {
                    loadNative(MainActivity.this, new File(getDir("native", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE), SO_NAME));
                    wait(1000L);
                    PrintFromNative();
                }

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            //pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task Dismiss the progress dialog
         * *
         */
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            //ctx.dismissDialog(progress_bar_type);
            //

            refreshButtons();
        }
    }



}

class PlasmaView extends View {
    private Bitmap mBitmap;
    private long mStartTime;

    /* implementend by libplasma.so */
    private static native void renderPlasma(Bitmap bitmap, long time_ms);

    public PlasmaView(Context context, int width, int height) {
        super(context);
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mStartTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawColor(0xFFCCCCCC);
        renderPlasma(mBitmap, System.currentTimeMillis() - mStartTime);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        // force a redraw, with a different time-based pattern.
        invalidate();
    }
}
