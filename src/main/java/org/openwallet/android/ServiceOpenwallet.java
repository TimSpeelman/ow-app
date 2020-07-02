package org.openwallet.android;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.openwallet.android.R;
import org.openwallet.android.IRestApi;
import org.kivy.android.PythonService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceOpenwallet extends PythonService {

    // copied
    public static IRestApi createService(final String baseUrl, final String authToken) {

        OkHttpClient.Builder okHttp = new OkHttpClient.Builder()
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(new StethoInterceptor()) //DEBUG
                .retryOnConnectionFailure(true)
                .followSslRedirects(false)
                .followRedirects(false);

        return createService(baseUrl, authToken, okHttp);
    }

    // copied
    public static IRestApi createService(final String baseUrl, final String authToken, OkHttpClient.Builder okHttp) {

        Retrofit.Builder retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl);

        if (!TextUtils.isEmpty(authToken)) {

            okHttp.addInterceptor(chain -> {
                Request request = chain.request();
                Request newReq = request.newBuilder()
                        .addHeader("Authorization", String.format("token %s", authToken))
                        .build();
                return chain.proceed(newReq);
            });
        }
        retrofit.client(okHttp.build());

        return retrofit.build().create(IRestApi.class);
    }

    // copied
    public static IRestApi createService() {
        return IPV8Service.createService("http://127.0.0.1:8642", "");
    }


	/**
	 * Binder given to clients
	 */
    // private final IBinder mBinder = new ServiceOpenwalletBinder();

    

    

    public static void start(Context ctx/*, String pythonServiceArgument*/) {
        String argument = ctx.getFilesDir().getAbsolutePath() + "/app"; // ipv8 does not add /app
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
        intent.putExtra("pythonServiceArgument", "");
        intent.putExtra("serviceIconId", R.mipmap.ic_launcher);
        ctx.startService(intent);
    }

    public static void stop(Context ctx) {
        Intent intent = new Intent(ctx, ServiceOpenwallet.class);
        ctx.stopService(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // return mBinder;
        return null;
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    // public class ServiceOpenwalletBinder extends Binder {
    // 	ServiceOpenwallet getService() {
    //         // Return this instance of ServiceOpenwallet so clients can call public methods
    //         return ServiceOpenwallet.this;
    //     }
    // }
}