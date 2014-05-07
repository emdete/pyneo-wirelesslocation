package org.pyneo.wirelesslocation;

import android.annotation.TargetApi;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.content.Context;
import android.os.WorkSource;
import android.util.Log;
import internal.com.android.location.provider.LocationProviderBase;
import internal.com.android.location.provider.LocationRequestUnbundled;
import internal.com.android.location.provider.ProviderPropertiesUnbundled;
import internal.com.android.location.provider.ProviderRequestUnbundled;

@TargetApi(17)
public class NetworkLocationProviderV2 extends LocationProviderBase implements NetworkLocationProvider {
	private static final String TAG = NetworkLocationProviderV2.class.getName();
	private static final boolean DEBUG = false;
	static { DEBUG = Log.isLoggable("org.pyneo.android", Log.DEBUG); }

	private boolean enabledByService = false;
	private boolean enabledBySetting = true;
	private WirelessEnvListener wirelessEnvListener;

	public NetworkLocationProviderV2(Context context) {
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
		if (DEBUG) Log.d(TAG, "NetworkLocationProviderV2:");
		wirelessEnvListener = new WirelessEnvListener(context, this);
	}

	@Override
	public synchronized void disable() {
		if (DEBUG) Log.d(TAG, "disable:");
		enabledByService = false;
	}

	@Override
	public synchronized void enable() {
		if (DEBUG) Log.d(TAG, "enable:");
		enabledByService = true;
	}

	@Override
	public boolean isActive() {
		if (DEBUG) Log.d(TAG, "isActive:");
		return enabledByService && enabledBySetting;
	}

	@Override
	public synchronized void onDisable() {
		if (DEBUG) Log.d(TAG, "onDisable:");
		enabledBySetting = false;
	}

	@Override
	public synchronized void onEnable() {
		if (DEBUG) Log.d(TAG, "onEnable:");
		enabledBySetting = true;
	}

	@Override
	public int onGetStatus(final Bundle arg0) {
		if (DEBUG) Log.d(TAG, "onGetStatus:");
		return android.location.LocationProvider.AVAILABLE;
	}

	@Override
	public long onGetStatusUpdateTime() {
		if (DEBUG) Log.d(TAG, "onGetStatusUpdateTime:");
		return SystemClock.elapsedRealtime();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (DEBUG) Log.d(TAG, "onLocationChanged:");
		if (location != null) {
			if (DEBUG) Log.d(TAG, "onLocationChanged: " + location);
			reportLocation(location);
		}
	}

	@Override
	public void onSetRequest(final ProviderRequestUnbundled requests, final WorkSource ws) {
		try {
			if (DEBUG) Log.d(TAG, "onSetRequest: requests=" + requests + ", ws=" + ws);
			wirelessEnvListener.disable();
			long autoTime = Long.MAX_VALUE;
			boolean autoUpdate = false;
			for (final LocationRequestUnbundled request : requests.getLocationRequests()) {
				if (DEBUG) Log.d(TAG, "onSetRequest: request=" + request);
				if (autoTime > request.getInterval()) {
					autoTime = request.getInterval();
				}
				autoUpdate = true;
			}
			if (autoUpdate) {
				if (autoTime < 1500) {
					autoTime = 1500;
				}
				wirelessEnvListener.setTime(autoTime);
				wirelessEnvListener.enable();
			}
			else {
				wirelessEnvListener.disable();
			}
		}
		catch (Exception e) {
			Log.e(TAG, "onSetRequest: e=" + e, e);
		}
	}
}
