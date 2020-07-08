
package org.kivy.android;

import java.net.Socket;
import java.net.InetSocketAddress;

import android.os.SystemClock;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import android.os.Build; 
import android.app.*;
import android.content.*;
import android.view.*;
import android.view.SurfaceView;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.widget.ImageView;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.widget.AbsoluteLayout;

import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.kivy.android.PythonUtil;

import org.renpy.android.ResourceManager;
import org.renpy.android.AssetExtract;

// <IPV8APP>
import org.openwallet.android.R;
import android.Manifest;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.webkit.PermissionRequest;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import java.lang.Runnable;
import org.openwallet.android.ServiceOpenwallet;
// </IPV8APP>
import android.content.res.AssetManager;


public class PythonActivity extends Activity {

    // <IPV8MAIN>
    private Class<?> mClss;
    private WebView mWebView;
    private JavaScriptInterface JSInterface;
    private static final int ZBAR_CAMERA_PERMISSION = 1;
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 110;
    private static final String url = "http://localhost:8642/app?port=8642";
    // </IPV8MAIN>

    // This activity is modified from a mixture of the SDLActivity and
    // PythonActivity in the SDL2 bootstrap, but removing all the SDL2
    // specifics.

    private static final String TAG = "PythonActivity";

    public static PythonActivity mActivity = null;

    /** If shared libraries (e.g. the native application) could not be loaded. */
    public static boolean mBrokenLibraries;

    protected static Thread mPythonThread;

    private ResourceManager resourceManager = null;
    private Bundle mMetaData = null;
    private PowerManager.WakeLock mWakeLock = null;

    // <IPV8APP>

    protected void ipv8AppOnCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        // // Write permissions on sdcard?
        // if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        //     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        // } else {
        //     // startService();
        //     Log.w("ipv8AppOnCreate","skipping startService()");
        // }

        mWebView = (WebView) findViewById(R.id.activity_main_webview);

        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Set JavascriptInterface
        // JSInterface = new JavaScriptInterface(this);
        // mWebView.addJavascriptInterface(JSInterface, "android");
        mWebView.setWebContentsDebuggingEnabled(true);

        // Load the GUI
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Show custom error page
                if (view != null){
                    String htmlData ="<html><body><div align=\"center\" >Please wait while loading backend..</div></body>";
                    view.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
                }

                // Reload after 1s. Should happen only when IPv8 is still loading.
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.loadUrl(url);
                    }
                }, 1000);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.d("openwalletWebview", "requesting: " + url);
                return super.shouldInterceptRequest(view, url);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    request.grant(request.getResources());
                }
            }
        });

        if (savedInstanceState == null) {
            mWebView.loadUrl(url);
        }

    }
  
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_STORAGE_PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.w("onRequestPermissionsResult", "skipping startService()");
                        // startService();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
                    }
                }
            }
        }
        else if (requestCode == ZBAR_CAMERA_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // launchActivity(ScannerActivity.class);
                    } else {
                        // Don't retry
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    public class JavaScriptInterface {
        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void launchScanner()
        {
            // launchActivity(ScannerActivity.class);
        }
    }

    // </IPV8APP>


    public String getAppRoot() {
        String app_root =  getFilesDir().getAbsolutePath() + "/app";
        return app_root;
    }

    public String getEntryPoint(String search_dir) {
        /* Get the main file (.pyc|.pyo|.py) depending on if we
         * have a compiled version or not.
        */
        List<String> entryPoints = new ArrayList<String>();
        entryPoints.add("main.pyo");  // python 2 compiled files
        entryPoints.add("main.pyc");  // python 3 compiled files
		for (String value : entryPoints) {
            File mainFile = new File(search_dir + "/" + value);
            if (mainFile.exists()) {
                return value;
            }
        }
        return "main.py";
    }

    public static void initialize() {
        // The static nature of the singleton and Android quirkiness force us to initialize everything here
        // Otherwise, when exiting the app and returning to it, these variables *keep* their pre exit values
        mBrokenLibraries = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        File app_root_file = new File(getAppRoot());
        File myassets = new File(app_root_file.getAbsolutePath() + "/myassets").mkdirs();

        Log.v(TAG, "Copying assets to " + myassets.getAbsolutePath());
        this.copyAssets("", myassets.getAbsolutePath());

        Log.v(TAG, "My oncreate running");
        resourceManager = new ResourceManager(this);

        Log.v(TAG, "Ready to unpack");
        
        unpackData("private", app_root_file);

        listFiles(app_root_file.getAbsolutePath());

        Log.v(TAG, "About to do super onCreate");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Did super onCreate");

        this.mActivity = this;
        //this.showLoadingScreen();
        Log.v("Python", "Device: " + android.os.Build.DEVICE);
        Log.v("Python", "Model: " + android.os.Build.MODEL);

        //Log.v(TAG, "Ready to unpack");
        //new UnpackFilesTask().execute(getAppRoot());

        PythonActivity.initialize();

        // Load shared libraries
        String errorMsgBrokenLib = "";
        try {
            loadLibraries();
        } catch(UnsatisfiedLinkError e) {
            System.err.println(e.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e.getMessage();
        } catch(Exception e) {
            System.err.println(e.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e.getMessage();
        }

        if (mBrokenLibraries)
        {
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
            dlgAlert.setMessage("An error occurred while trying to load the application libraries. Please try again and/or reinstall."
                  + System.getProperty("line.separator")
                  + System.getProperty("line.separator")
                  + "Error: " + errorMsgBrokenLib);
            dlgAlert.setTitle("Python Error");
            dlgAlert.setPositiveButton("Exit",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close current activity
                        PythonActivity.mActivity.finish();
                    }
                });
           dlgAlert.setCancelable(false);
           dlgAlert.create().show();
       
           return;
        }

        // Set up the Python environment
        String app_root_dir = getAppRoot();
        String mFilesDirectory = mActivity.getFilesDir().getAbsolutePath();
        String entry_point = getEntryPoint(app_root_dir);

        Log.v(TAG, "Setting env vars for start.c and Python to use");
        PythonActivity.nativeSetenv("ANDROID_ENTRYPOINT", entry_point);
        PythonActivity.nativeSetenv("ANDROID_ARGUMENT", app_root_dir);
        PythonActivity.nativeSetenv("ANDROID_APP_PATH", app_root_dir);
        PythonActivity.nativeSetenv("ANDROID_PRIVATE", mFilesDirectory);
        PythonActivity.nativeSetenv("ANDROID_UNPACK", app_root_dir);
        PythonActivity.nativeSetenv("PYTHONHOME", app_root_dir);
        PythonActivity.nativeSetenv("PYTHONPATH", app_root_dir + ":" + app_root_dir + "/lib");
        PythonActivity.nativeSetenv("PYTHONOPTIMIZE", "2");


        listFiles(app_root_dir);
        listFiles(mFilesDirectory);
        listFiles("/data/app/org.openwallet.android-1");

        try {
            Log.v(TAG, "Access to our meta-data...");
            mActivity.mMetaData = mActivity.getPackageManager().getApplicationInfo(
                    mActivity.getPackageName(), PackageManager.GET_META_DATA).metaData;

            PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
            if ( mActivity.mMetaData.getInt("wakelock") == 1 ) {
                mActivity.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Screen On");
                mActivity.mWakeLock.acquire();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        final Thread pythonThread = new Thread(new PythonMain(), "PythonThread");
        PythonActivity.mPythonThread = pythonThread;
        pythonThread.start();

        // <IPV8APP>   
        ipv8AppOnCreate(savedInstanceState);
        // </IPV8APP>   

    }

    @Override
    public void onDestroy() {
        Log.i("Destroy", "end of app");
        super.onDestroy();

        // make sure all child threads (python_thread) are stopped
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void loadLibraries() {
        String app_root = new String(getAppRoot());
        File app_root_file = new File(app_root);
        PythonUtil.loadLibraries(app_root_file,
            new File(getApplicationInfo().nativeLibraryDir));
    }

    public void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File r : f.listFiles()) {
                recursiveDelete(r);
            }
        }
        f.delete();
    }

    /**
     * Show an error using a toast. (Only makes sense from non-UI
     * threads.)
     */
    public void toastError(final String msg) {

        final Activity thisActivity = this;

        runOnUiThread(new Runnable () {
            public void run() {
                Toast.makeText(thisActivity, msg, Toast.LENGTH_LONG).show();
            }
        });

        // Wait to show the error.
        synchronized (this) {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void unpackData(final String resource, File target) {

        Log.v(TAG, "UNPACKING!!! " + resource + " " + target.getName());

        // The version of data in memory and on disk.
        String data_version = resourceManager.getString(resource + "_version");
        String disk_version = null;

        Log.v(TAG, "Data version is " + data_version);

        // If no version, no unpacking is necessary.
        if (data_version == null) {
            return;
        }

        // Check the current disk version, if any.
        String filesDir = target.getAbsolutePath();
        String disk_version_fn = filesDir + "/" + resource + ".version";

        try {
            byte buf[] = new byte[64];
            InputStream is = new FileInputStream(disk_version_fn);
            int len = is.read(buf);
            disk_version = new String(buf, 0, len);
            is.close();
        } catch (Exception e) {
            disk_version = "";
        }

        // If the disk data is out of date, extract it and write the
        // version file.
        // if (! data_version.equals(disk_version)) {
        if (! data_version.equals(disk_version)) {
            Log.v(TAG, "Extracting " + resource + " assets.");

            recursiveDelete(target);
            target.mkdirs();

            AssetExtract ae = new AssetExtract(this);
            if (!ae.extractTar(resource + ".mp3", target.getAbsolutePath())) {
                toastError("Could not extract " + resource + " data.");
            }

            try {
                // Write .nomedia.
                new File(target, ".nomedia").createNewFile();

                // Write version file.
                FileOutputStream os = new FileOutputStream(disk_version_fn);
                os.write(data_version.getBytes());
                os.close();
            } catch (Exception e) {
                Log.w("python", e);
            }
        }
    }

    long lastBackClick = SystemClock.elapsedRealtime();
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        if (SystemClock.elapsedRealtime() - lastBackClick > 2000){
            lastBackClick = SystemClock.elapsedRealtime();
            Toast.makeText(this, "Click again to close the app",
            Toast.LENGTH_LONG).show();
            return true;
        }

        lastBackClick = SystemClock.elapsedRealtime();
        return super.onKeyDown(keyCode, event);
    }


    //----------------------------------------------------------------------------
    // Listener interface for onNewIntent
    //

    public interface NewIntentListener {
        void onNewIntent(Intent intent);
    }

    private List<NewIntentListener> newIntentListeners = null;

    public void registerNewIntentListener(NewIntentListener listener) {
        if ( this.newIntentListeners == null )
            this.newIntentListeners = Collections.synchronizedList(new ArrayList<NewIntentListener>());
        this.newIntentListeners.add(listener);
    }

    public void unregisterNewIntentListener(NewIntentListener listener) {
        if ( this.newIntentListeners == null )
            return;
        this.newIntentListeners.remove(listener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ( this.newIntentListeners == null )
            return;
        this.onResume();
        synchronized ( this.newIntentListeners ) {
            Iterator<NewIntentListener> iterator = this.newIntentListeners.iterator();
            while ( iterator.hasNext() ) {
                (iterator.next()).onNewIntent(intent);
            }
        }
    }

    //----------------------------------------------------------------------------
    // Listener interface for onActivityResult
    //

    public interface ActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    private List<ActivityResultListener> activityResultListeners = null;

    public void registerActivityResultListener(ActivityResultListener listener) {
        if ( this.activityResultListeners == null )
            this.activityResultListeners = Collections.synchronizedList(new ArrayList<ActivityResultListener>());
        this.activityResultListeners.add(listener);
    }

    public void unregisterActivityResultListener(ActivityResultListener listener) {
        if ( this.activityResultListeners == null )
            return;
        this.activityResultListeners.remove(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ( this.activityResultListeners == null )
            return;
        this.onResume();
        synchronized ( this.activityResultListeners ) {
            Iterator<ActivityResultListener> iterator = this.activityResultListeners.iterator();
            while ( iterator.hasNext() )
                (iterator.next()).onActivityResult(requestCode, resultCode, intent);
        }
    }

    public static void start_service(String serviceTitle, String serviceDescription,
                String pythonServiceArgument) {
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        String argument = PythonActivity.mActivity.getFilesDir().getAbsolutePath();
        String filesDirectory = argument;
        String app_root_dir = PythonActivity.mActivity.getAppRoot();
        String entry_point = PythonActivity.mActivity.getEntryPoint(app_root_dir + "/service");
        serviceIntent.putExtra("androidPrivate", argument);
        serviceIntent.putExtra("androidArgument", app_root_dir);
        serviceIntent.putExtra("serviceEntrypoint", "service/" + entry_point);
        serviceIntent.putExtra("pythonName", "python");
        serviceIntent.putExtra("pythonHome", app_root_dir);
        serviceIntent.putExtra("pythonPath", app_root_dir + ":" + app_root_dir + "/lib");
        serviceIntent.putExtra("serviceTitle", serviceTitle);
        serviceIntent.putExtra("serviceDescription", serviceDescription);
        serviceIntent.putExtra("pythonServiceArgument", pythonServiceArgument);
        PythonActivity.mActivity.startService(serviceIntent);
    }

    public static void stop_service() {
        Intent serviceIntent = new Intent(PythonActivity.mActivity, PythonService.class);
        PythonActivity.mActivity.stopService(serviceIntent);
    }


    public static native void nativeSetenv(String name, String value);
    public static native int nativeInit(Object arguments);



    public void _listFiles(String startDir) {
        File dir = new File(startDir);
        File[] files = dir.listFiles();

        if (files != null && files.length > 0) {
            for (File file : files) {
                // Check if the file is a directory
                if (file.isDirectory()) {
                    // We will not print the directory name, just use it as a new
                    // starting point to list files from
                    _listFiles(file.getAbsolutePath());
                } else {
                    // We can use .length() to get the file size
                    System.out.println(file.getAbsolutePath());
                }
            }
        }
    }

    protected void listFiles(String path) { 
        System.out.println("< FilesIn: " + path);
        _listFiles(path);
        System.out.println("< END FilesIn: " + path);
    }    

    private void copyAssets(String assetPath, String targetFolder) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list(assetPath);
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
            in = assetManager.open(filename);
            File outFile = new File(targetFolder, filename);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }     
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }  
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
        out.write(buffer, 0, read);
        }
    }
}


class PythonMain implements Runnable {
    @Override
    public void run() {
        PythonActivity.nativeInit(new String[0]);
    }
}
