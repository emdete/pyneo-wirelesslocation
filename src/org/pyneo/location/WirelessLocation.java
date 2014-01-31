package org.pyneo.location;

import android.os.IBinder;
import android.app.Service;
import android.util.Log;
import android.os.Bundle;
import android.content.Intent;
import com.android.location.provider.LocationProviderBase;

public class WirelessLocation extends Service
{
	private static final String TAG = "org.pyneo.location.WirelessLocation";

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate:");
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.d(TAG, "onBind: intent=" + intent);
		if (intent == null) {
			return null;
		}
		String action = intent.getAction();
		if (action == null) {
			return null;
		}
		if (
			// action.equalsIgnoreCase("com.google.android.location.NetworkLocationProvider") ||
			// action.equalsIgnoreCase("com.android.location.service.NetworkLocationProvider") ||
			action.equalsIgnoreCase("com.android.location.service.v2.NetworkLocationProvider") ||
			// action.equalsIgnoreCase("com.android.location.service.v3.NetworkLocationProvider") ||
			// action.equalsIgnoreCase("com.google.android.location.internal.ANDROID_NLP") ||
			false
		) {
			Log.d(TAG, "onBind: action=" + action);
			return null;
		}
		Log.e(TAG, "onBind: action=" + action);
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy:");
	}
}
