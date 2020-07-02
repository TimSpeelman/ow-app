package org.openwallet.android;

import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import org.kivy.android.PythonService;
import android.util.Log;

public class ServiceOpenwallet extends PythonService {
	/**
	 * Binder given to clients
	 */
    private final IBinder mBinder = new ServiceOpenwalletBinder();

    

    

    public static void start(Context ctx, String pythonServiceArgument) {
        Log.d("ServiceOpenWallet", "start()");
        String argument = ctx.getFilesDir().getAbsolutePath() + "/app";
        Intent intent = new Intent(ctx, ServiceOpenwallet.class);
        intent.putExtra("androidPrivate", argument);
        intent.putExtra("androidArgument", argument);
        intent.putExtra("serviceEntrypoint", "OpenWallet.py");
        intent.putExtra("serviceTitle", "Openwallet");
        intent.putExtra("serviceDescription", "http://127.0.0.1:8642");
        intent.putExtra("pythonName", "OpenWallet");
        intent.putExtra("pythonHome", argument);
        intent.putExtra("androidUnpack", argument);
        intent.putExtra("pythonPath", argument + ":" + argument + "/lib");
        intent.putExtra("pythonServiceArgument", pythonServiceArgument);
        ctx.startService(intent);
    }

    public static void stop(Context ctx) {
        Log.d("ServiceOpenWallet", "stop()");      
        Intent intent = new Intent(ctx, ServiceOpenwallet.class);
        ctx.stopService(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class ServiceOpenwalletBinder extends Binder {
    	ServiceOpenwallet getService() {
            Log.d("ServiceOpenWallet", "getService()");
            // Return this instance of ServiceOpenwallet so clients can call public methods
            return ServiceOpenwallet.this;
        }
    }
}