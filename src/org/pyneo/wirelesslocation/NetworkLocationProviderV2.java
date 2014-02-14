package org.pyneo.wirelesslocation;

import android.annotation.TargetApi;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import internal.com.android.location.provider.LocationProviderBase;
import internal.com.android.location.provider.LocationRequestUnbundled;
import internal.com.android.location.provider.ProviderPropertiesUnbundled;
import internal.com.android.location.provider.ProviderRequestUnbundled;
import java.lang.reflect.Method;

@TargetApi(17)
public class NetworkLocationProviderV2 extends LocationProviderBase implements NetworkLocationProvider {
	private static final String TAG = "org.pyneo.wirelesslocation.NetworkLocationProviderV2";

	private boolean enabledByService = false;
	private boolean enabledBySetting = true;

	public NetworkLocationProviderV2() {
		super(TAG, ProviderPropertiesUnbundled.create(
			true, // requiresNetwork
			false, // requiresSatellite
			true, // requiresCell
			false, // hasMonetaryCost
			false, // supportsAltitude
			false, // supportsSpeed
			false, // supportsBearing
			Criteria.POWER_LOW, // powerRequirement
			Criteria.ACCURACY_FINE)); // accuracy
		if (MainService.DEBUG) Log.d(TAG, "NetworkLocationProviderV2:");
	}

	@Deprecated
	public NetworkLocationProviderV2(final boolean internal) {
		this();
		if (MainService.DEBUG) Log.d(TAG, "NetworkLocationProviderV2:");
	}

	@Override
	public synchronized void disable() {
		if (MainService.DEBUG) Log.d(TAG, "disable:");
		enabledByService = false;
	}

	@Override
	public synchronized void enable() {
		if (MainService.DEBUG) Log.d(TAG, "enable:");
		enabledByService = true;
	}

	@Override
	public boolean isActive() {
		if (MainService.DEBUG) Log.d(TAG, "isActive:");
		return enabledByService && enabledBySetting;
	}

	@Override
	public synchronized void onDisable() {
		if (MainService.DEBUG) Log.d(TAG, "onDisable:");
		enabledBySetting = false;
	}

	@Override
	public synchronized void onEnable() {
		if (MainService.DEBUG) Log.d(TAG, "onEnable:");
		enabledBySetting = true;
	}

	@Override
	public int onGetStatus(final Bundle arg0) {
		if (MainService.DEBUG) Log.d(TAG, "onGetStatus:");
		return android.location.LocationProvider.AVAILABLE;
	}

	@Override
	public long onGetStatusUpdateTime() {
		if (MainService.DEBUG) Log.d(TAG, "onGetStatusUpdateTime:");
		return SystemClock.elapsedRealtime();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (MainService.DEBUG) Log.d(TAG, "onLocationChanged: location=" + location);
		if (location != null) {
			if (MainService.DEBUG) Log.d(TAG, "Reporting: " + location);
			reportLocation(location);
		}
	}

	@Override
	public void onSetRequest(final ProviderRequestUnbundled requests, final WorkSource ws) {
		if (MainService.DEBUG) Log.d(TAG, "onSetRequest: requests=" + requests + ", ws=" + ws);
		long autoTime = Long.MAX_VALUE;
		boolean autoUpdate = false;
		for (final LocationRequestUnbundled request : requests.getLocationRequests()) {
			if (MainService.DEBUG) Log.d(TAG, "onSetRequest: request=" + request);
			if (request.getInterval() < autoTime) {
				autoTime = request.getInterval();
			}
			autoUpdate = true;
		}
		if (autoTime < 1000) {
			autoTime = 1000;
		}
	}

	static public Location dummy(double lat, double lon, double acc) {
		Location location = new Location("network");
		location.setProvider(NetworkLocationProvider.IDENTIFIER);
		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAccuracy((float)acc);
		location.setSpeed(0.0f);
		location.setBearing(0.0f);
		location.setAltitude(1.0);
		//location.setElapsedRealtimeNanos(SystemClock.elapsedRealtime());
		location.setTime(System.currentTimeMillis());
		androidLocationLocationMakeComplete(location);
		androidLocationLocationSetExtraLocation(location, new Location(location));
		return location;
	}

	private static void androidLocationLocationMakeComplete(Location location) {
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
}
