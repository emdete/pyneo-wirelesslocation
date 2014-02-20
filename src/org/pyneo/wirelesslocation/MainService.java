package org.pyneo.wirelesslocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class MainService extends Service {
	private static final String TAG = "org.pyneo.wirelesslocation.MainService";

	public static final boolean DEBUG;
	static {
		DEBUG = Log.isLoggable("nlp", Log.DEBUG);
	}

	private static Context context;
	private NetworkLocationProvider nlprovider;
	private GeocodeProvider geoprovider;

	public MainService() {
		if (DEBUG) Log.d(TAG, "MainService:");
	}

	public static Context getContext() {
		if (DEBUG) Log.d(TAG, "getContext:");
		return context;
	}

	public Location getCurrentLocation() {
		if (DEBUG) Log.d(TAG, "getCurrentLocation:");
		return null;
	}

	public boolean isActive() {
		if (DEBUG) Log.d(TAG, "isActive:");
		return nlprovider.isActive();
	}

	private static int androidProviderSettingsGlobalGetInt(ContentResolver contentResolver, String name, int defaultValue) {
		if (DEBUG) Log.d(TAG, "androidProviderSettingsGlobalGetInt:");
		try {
			Class<?> clazz = Class.forName("android.provider.Settings$Global");
			Method getInt = clazz.getDeclaredMethod("getInt", ContentResolver.class, String.class, int.class);
			return (Integer) getInt.invoke(null, contentResolver, name, defaultValue);
		} catch (Exception e) {
			Log.w("android.provider.Settings.Global.getInt", e);
			return defaultValue;
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		if (DEBUG) Log.d(TAG, "onBind: intent=" + intent);
		IBinder ret = null;
		if (intent != null) {
			String action = intent.getAction();
			if (action != null) {
				if (
					action.equalsIgnoreCase("com.google.android.location.NetworkLocationProvider") ||
					action.equalsIgnoreCase("com.android.location.service.NetworkLocationProvider") ||
					action.equalsIgnoreCase("com.android.location.service.v2.NetworkLocationProvider") ||
					action.equalsIgnoreCase("com.android.location.service.v3.NetworkLocationProvider")) {
					ret = nlprovider.getBinder();
				} else if (
					action.equalsIgnoreCase("com.google.android.location.GeocodeProvider") ||
					action.equalsIgnoreCase("com.android.location.service.GeocodeProvider")) {
					ret = geoprovider.getBinder();
				} else if (
					action.equalsIgnoreCase("com.google.android.location.internal.ANDROID_NLP")) {
					ret = nlprovider.getBinder();
				} else {
					Log.w(TAG, "onBind: unknown intent");
				}
			}
		}
		if (DEBUG) Log.d(TAG, "onBind: ret=" + ret);
		return ret;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.d(TAG, "onCreate:");
		context = this;
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
			nlprovider = new NetworkLocationProviderV1();
		} else {
			nlprovider = new NetworkLocationProviderV2(context);
		}
		geoprovider = new GeocodeProvider();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.d(TAG, "onDestroy:");
		geoprovider.disable();
		geoprovider = null;
		nlprovider.disable();
		nlprovider = null;
	}
}
