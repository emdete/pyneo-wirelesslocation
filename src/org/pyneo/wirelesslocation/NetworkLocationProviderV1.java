package org.pyneo.wirelesslocation;

import internal.com.android.location.provider.LocationProvider;

import android.location.Criteria;
import android.location.Location;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;

public class NetworkLocationProviderV1 extends LocationProvider implements NetworkLocationProvider {
	private static final String TAG = "org.pyneo.wirelesslocation.NetworkLocationProviderV1";

	private long autoTime;
	private boolean autoUpdate;
	private boolean enabledByService = false;
	private boolean enabledBySetting = false;

	public NetworkLocationProviderV1() {
		autoUpdate = false;
		autoTime = Long.MAX_VALUE;
	}

	@Deprecated
	public NetworkLocationProviderV1(final boolean internal) {
		this();
	}

	@Override
	public synchronized void disable() {
		enabledByService = false;
	}

	@Override
	public synchronized void enable() {
		enabledByService = true;
	}

	@Override
	public boolean isActive() {
		return enabledBySetting && enabledByService;
	}

	@Override
	public void onAddListener(final int uid, final WorkSource ws) {
		if (MainService.DEBUG) Log.d(TAG, uid + " is listening as " + ws != null ? (ws + " (contents:" + ws.describeContents() + ")") : "[unknown WorkSource]");
	}

	@Override
	public void onDisable() {
		enabledBySetting = false;
	}

	@Override
	public void onEnable() {
		enabledBySetting = true;
	}

	@Override
	public void onEnableLocationTracking(final boolean enable) {
		autoUpdate = enable;
	}

	@Override
	public int onGetAccuracy() {
		return Criteria.ACCURACY_COARSE;
	}

	@Override
	public String onGetInternalState() {
		if (MainService.DEBUG) Log.w(TAG, "Internal State not yet implemented. The application may not work.");
		return "[INTERNAL STATE NOT IMPLEMENTED]";
	}

	@Override
	public int onGetPowerRequirement() {
		return Criteria.POWER_LOW;
	}

	@Override
	public int onGetStatus(final Bundle extras) {
		return android.location.LocationProvider.AVAILABLE;
	}

	@Override
	public long onGetStatusUpdateTime() {
		return SystemClock.elapsedRealtime();
	}

	@Override
	public boolean onHasMonetaryCost() {
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (MainService.DEBUG) Log.d(TAG, "Reporting: " + location);
		if (location != null) {
			reportLocation(location);
		}
	}

	@Override
	public boolean onMeetsCriteria(final Criteria criteria) {
		if (criteria.getAccuracy() == Criteria.ACCURACY_FINE) {
			return false;
		}
		if (criteria.isAltitudeRequired()) {
			return false;
		}
		if (criteria.isSpeedRequired()) {
			return false;
		}
		return true;
	}

	@Override
	public void onRemoveListener(final int uid, final WorkSource ws) {
	}

	@Override
	public boolean onRequiresCell() {
		return true;
	}

	@Override
	public boolean onRequiresNetwork() {
		return true;
	}

	@Override
	public boolean onRequiresSatellite() {
		return false;
	}

	@Override
	public boolean onSendExtraCommand(final String command, final Bundle extras) {
		return false;
	}

	@Override
	public void onSetMinTime(final long minTime, final WorkSource ws) {
		autoTime = minTime;
	}

	@Override
	public boolean onSupportsAltitude() {
		return false;
	}

	@Override
	public boolean onSupportsBearing() {
		return true;
	}

	@Override
	public boolean onSupportsSpeed() {
		return false;
	}

	@Override
	public void onUpdateLocation(final Location location) {
	}

	@Override
	public void onUpdateNetworkState(final int state, final NetworkInfo info) {
		if (MainService.DEBUG) Log.d(TAG, "onUpdateNetworkState: " + state + " (" + info + ")");
	}
}
