package org.pyneo.wirelesslocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.pyneo.wirelesslocation.cellapi.GeocodeProvider;
import org.pyneo.wirelesslocation.cellapi.LocationSource;
import org.pyneo.wirelesslocation.cellapi.ReverseGeocode;
import org.pyneo.wirelesslocation.cellapi.WirelessIdLocation;
import org.pyneo.wirelesslocation.cellular.CellSpec;
import org.pyneo.wirelesslocation.cellular.CellSpecRetriever;
import org.pyneo.wirelesslocation.wlan.WifiSpec;
import org.pyneo.wirelesslocation.wlan.WifiSpecRetriever;

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
		DEBUG = true; // Log.isLoggable("nlp", Log.DEBUG);
	}

	private static Context context;
	private LocationCalculator locationCalculator;
	private LocationRetriever locationRetriever;
	private GeocodeProvider geoprovider;
	private org.pyneo.wirelesslocation.NetworkLocationProvider nlprovider;
	private WifiManager wifiManager;
	private BroadcastReceiver airplaneModeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateProviderStateOnAirplaneMode();
		}
	};

	public MainService() {
		if (DEBUG) Log.d(TAG, "MainService:");
	}

	public static Context getContext() {
		if (DEBUG) Log.d(TAG, "getContext:");
		return context;
	}

	public Location getCurrentLocation() {
		if (DEBUG) Log.d(TAG, "getCurrentLocation:");
		return locationCalculator.getCurrentLocation();
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
				if (action.equalsIgnoreCase("com.google.android.location.NetworkLocationProvider")
						|| action.equalsIgnoreCase("com.android.location.service.NetworkLocationProvider")
						|| action.equalsIgnoreCase("com.android.location.service.v2.NetworkLocationProvider")
						|| action.equalsIgnoreCase("com.android.location.service.v3.NetworkLocationProvider")) {
					ret = nlprovider.getBinder();
				} else if (action.equalsIgnoreCase("com.google.android.location.GeocodeProvider")
						|| action.equalsIgnoreCase("com.android.location.service.GeocodeProvider")) {
					ret = geoprovider.getBinder();
				} else if (action.equalsIgnoreCase("com.google.android.location.internal.ANDROID_NLP")) {
					Log.w(TAG, "somebody wants internal stuff o.O");
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
		wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
			nlprovider = new NetworkLocationProviderV1();
		} else {
			nlprovider = new NetworkLocationProviderV2();
		}
		geoprovider = new GeocodeProvider();
		GeocodeDatabase geocodeDatabase = new GeocodeDatabase();
		geoprovider.setGeocodeDatabase(geocodeDatabase);
		WifiSpecRetriever wifiSpecRetriever = new WifiSpecRetriever(context);
		CellSpecRetriever cellSpecRetriever = new CellSpecRetriever(context);
		//
		LocationDatabase locationDatabase = new LocationDatabase(context);
		locationRetriever = new LocationRetriever(locationDatabase);
		locationCalculator = new LocationCalculator(locationDatabase, locationRetriever, cellSpecRetriever, wifiSpecRetriever);
		nlprovider.setCalculator(locationCalculator);
		// wifi:
		List<LocationSource<WifiSpec>> wifiSources = new ArrayList<LocationSource<WifiSpec>>();
		locationRetriever.setWifiLocationSources(wifiSources);
		// gsm
		List<LocationSource<CellSpec>> cellSources = new ArrayList<LocationSource<CellSpec>>();
		cellSources.add(new WirelessIdLocation());
		locationRetriever.setCellLocationSources(cellSources);
		// start
		locationRetriever.start();
		//
		List<ReverseGeocode> geocodeSources = new ArrayList<ReverseGeocode>();
		geocodeSources.add(new ReverseGeocode(context));
		geoprovider.setSources(geocodeSources);
		//
		registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
		updateProviderStateOnAirplaneMode();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.d(TAG, "onDestroy:");
		unregisterReceiver(airplaneModeReceiver);
		geoprovider = null;
		nlprovider.disable();
		locationCalculator = null;
		locationRetriever.stop();
		locationRetriever = null;
		nlprovider = null;
		wifiManager = null;
	}

	public void updateProviderStateOnAirplaneMode() {
		boolean airplane = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN ?
			Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) :
			androidProviderSettingsGlobalGetInt(getContentResolver(), "airplane_mode_on", 0)) != 0;
		boolean wifi = wifiManager.isWifiEnabled();
		if (DEBUG) Log.d(TAG, "updateProviderStateOnAirplaneMode: airplane:" + airplane + ", wifi:" + wifi);
		if (airplane && !wifi) {
			if (DEBUG) Log.d(TAG, "updateProviderStateOnAirplaneMode: disable nlprovider");
			nlprovider.disable();
		} else {
			if (DEBUG) Log.d(TAG, "updateProviderStateOnAirplaneMode: enable nlprovider");
			nlprovider.enable();
		}
	}
}
