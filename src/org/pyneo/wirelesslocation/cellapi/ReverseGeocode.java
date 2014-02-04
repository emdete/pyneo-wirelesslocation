package org.pyneo.wirelesslocation.cellapi;

import android.content.Context;
import android.location.Address;
import android.net.ConnectivityManager;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;
import org.pyneo.wirelesslocation.DataSource;

public class ReverseGeocode implements DataSource {
	private static final String TAG = "org.pyneo.wirelesslocation.cellapi.ReverseGeocode";
	private static final String NAME = "CellApi ReverseGeocode";
	private static final String DESCRIPTION = "Reverse geocode using the online service by vfnet.";
	private static final String COPYRIGHT = "© pyneo 2014";
	private final Context context;
	private final ConnectivityManager connectivityManager;

	public ReverseGeocode(Context context) {
		this.context = context;
		this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public boolean isSourceAvailable() {
		return (connectivityManager.getActiveNetworkInfo() != null) &&
			   connectivityManager.getActiveNetworkInfo().isAvailable() &&
			   connectivityManager.getActiveNetworkInfo().isConnected();
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getCopyright() {
		return COPYRIGHT;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	public List<Address> getFromLocation(double latitude, double longitude, String sourcePackage, Locale locale) {
		Log.i(TAG, "getFromLocation:");
		return null; // TODO:
	}

	public List<Address> getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, String sourcePackage, Locale locale) {
		Log.i(TAG, "getFromLocationName:");
		return null; //TODO: Implement
	}
}
