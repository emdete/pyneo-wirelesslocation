package org.pyneo.wirelesslocation;

import java.util.ArrayList;
import java.util.List;

import org.pyneo.wirelesslocation.MainService;

import android.location.Address;
import android.location.GeocoderParams;
import android.util.Log;

public class GeocodeProvider extends internal.com.android.location.provider.GeocodeProvider {
	private static final String TAG = GeocodeProvider.class.getName();
	private static final boolean DEBUG = MainService.DEBUG;

	private static final String UNKNOWN_RESULT_ERROR = "unknown";

	void disable() {
	}

	void enable() {
	}

	@Override
	public String onGetFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
		if (DEBUG) Log.d(TAG, "onGetFromLocation: " + latitude + "/" + longitude);
		return UNKNOWN_RESULT_ERROR;
	}

	@Override
	public String onGetFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
		if (DEBUG) Log.d(TAG, "onGetFromLocationName: " + locationName);
		return UNKNOWN_RESULT_ERROR;
	}
}
