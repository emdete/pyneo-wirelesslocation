package org.pyneo.wirelesslocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.Runnable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class WirelessEnvListener extends PhoneStateListener implements Runnable {
	private static final String TAG = WirelessEnvListener.class.getName();

	ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	long intervalSecs = 5000;
	TelephonyManager telephonyManager;
	NetworkLocationProvider networkLocationProvider;

	WirelessEnvListener(Context context, NetworkLocationProvider networkLocationProvider) {
		this.telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		this.networkLocationProvider = networkLocationProvider;
	}

	void setTime(long intervalSecs) {
		this.intervalSecs = intervalSecs;
	}

	void enable() {
		if (MainService.DEBUG) Log.d(TAG, "enable:");
		executor.scheduleAtFixedRate(this, intervalSecs / 2, intervalSecs, TimeUnit.MILLISECONDS);
	}

	void disable() {
		if (MainService.DEBUG) Log.d(TAG, "disable:");
		executor.shutdownNow();
		executor = new ScheduledThreadPoolExecutor(1);
	}

	@Override
	public void onCellInfoChanged(List<CellInfo> cellInfos) {
		if (MainService.DEBUG) Log.d(TAG, "onCellInfoChanged: cellInfos=" + cellInfos);
		try {
			final Map<String,Object> map = new HashMap<String,Object>();
			CellAPI.toMap(map, cellInfos, telephonyManager);
			if (MainService.DEBUG) Log.d(TAG, "onCellInfoChanged: map=" + map);
			telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
			executor.execute(new Runnable() {
				public void run() {
					Double[] location = CellAPI.retrieveLocation(map);
					if (MainService.DEBUG) Log.d(TAG, "onCellInfoChanged: loc=" + Arrays.toString(location));
					if (location != null) {
						networkLocationProvider.onLocationChanged(dummy(location[0], location[1], location[2], location[3]));
					}
				}
			});
		}
		catch (Exception e) {
			Log.e(TAG, "e=" + e);
		}
	}

	static public Location dummy(double lat, double lon, double alt, double acc) {
		Location location = new Location("network");
		location.setProvider(NetworkLocationProvider.IDENTIFIER);
		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAltitude(alt);
		location.setAccuracy((float)acc);
		location.setSpeed(0.0f);
		location.setBearing(0.0f);
		//location.setElapsedRealtimeNanos(SystemClock.elapsedRealtime());
		location.setTime(System.currentTimeMillis());
		androidLocationLocationMakeComplete(location);
		androidLocationLocationSetExtraLocation(location, new Location(location));
		return location;
	}

	static void androidLocationLocationMakeComplete(Location location) {
		if (MainService.DEBUG) Log.d(TAG, "androidLocationLocationMakeComplete:");
		try {
			Class<?> clazz = Class.forName("android.location.Location");
			Method makeComplete = clazz.getDeclaredMethod("makeComplete");
			makeComplete.invoke(location);
		} catch (Exception e) {
			Log.w(TAG, "androidLocationLocationMakeComplete: " + e);
		}
	}

	public static void androidLocationLocationSetExtraLocation(Location location, Location value) {
		try {
			Class<?> clazz = Class.forName("android.location.Location");
			Method setExtraLocation = clazz.getDeclaredMethod("setExtraLocation", String.class, Location.class);
			setExtraLocation.invoke(location, "noGPSLocation", value);
		} catch (Exception e) {
			Log.w("android.location.Location.setExtraLocation", e);
		}
	}

	public void run() {
		if (MainService.DEBUG) Log.d(TAG, "run:");
		try {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_SERVICE_STATE);
			CellLocation.requestLocationUpdate();
		}
		finally {
		}
	}
}
