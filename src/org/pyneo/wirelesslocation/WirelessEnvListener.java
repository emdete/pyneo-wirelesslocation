package org.pyneo.wirelesslocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.CellIdentityCdma;
import android.os.Build;
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
import org.pyneo.android.TheDictionary;
import org.pyneo.android.TheList;
import org.pyneo.android.cellapi.CellAPI2;
import org.pyneo.android.wirelessid.CellId;
import org.pyneo.android.wirelessid.CellIdPre17API;
import org.pyneo.android.wirelessid.Meta;
import org.pyneo.android.wirelessid.Satellite;
import org.pyneo.android.wirelessid.WifiId;

public class WirelessEnvListener extends PhoneStateListener implements Runnable {
	private static final String TAG = WirelessEnvListener.class.getName();
	private static boolean DEBUG = false;
	static { DEBUG = Log.isLoggable("org.pyneo.android", Log.DEBUG); }

	ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	long intervalSecs = 5000;
	TelephonyManager telephonyManager;
	NetworkLocationProvider networkLocationProvider;
	private TheDictionary meta_map = new TheDictionary();
	String last_ident;

	WirelessEnvListener(Context context, NetworkLocationProvider networkLocationProvider) {
		this.telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		this.networkLocationProvider = networkLocationProvider;
		try { Meta.fill(meta_map, telephonyManager); } catch (Exception e) { Log.e(TAG, e.getMessage(), e); }
	}

	void setTime(long intervalSecs) {
		this.intervalSecs = intervalSecs;
	}

	void enable() {
		if (DEBUG) Log.d(TAG, "enable:");
		executor.scheduleAtFixedRate(this, intervalSecs / 2, intervalSecs, TimeUnit.MILLISECONDS);
	}

	void disable() {
		if (DEBUG) Log.d(TAG, "disable:");
		executor.shutdownNow();
		executor = new ScheduledThreadPoolExecutor(1);
	}

	@Override
	public void onCellInfoChanged(List<CellInfo> cellInfos) {
		if (DEBUG) Log.d(TAG, "onCellInfoChanged: cellInfos=" + cellInfos);
		try {
			Iterable<TheDictionary> cell_ids = null;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (cellInfos == null) {
					cellInfos = telephonyManager.getAllCellInfo();
					if (DEBUG) Log.e(TAG, "onCellInfoChanged: from parameter cellInfos=" + cellInfos);
				}
				cell_ids = new CellId(cellInfos);
			}
			if (cellInfos == null) {
				if (DEBUG) Log.e(TAG, "onCellInfoChanged: fallback to Pre17, cellInfos=" + cellInfos);
				cell_ids = new CellIdPre17API(telephonyManager, telephonyManager.getCellLocation(), telephonyManager.getNeighboringCellInfo());
			}
			if (DEBUG) Log.d(TAG, "onCellInfoChanged: cell_ids=" + cell_ids);
			final TheList cellapi2_request = new TheList();
			boolean changed = false;
			if (cell_ids != null) {
				for (TheDictionary item: cell_ids) {
					String ident = item.getIdent();
					if (ident != null) { // check for complete id information
						cellapi2_request.add(item);
						if (item.getBoolean("registered") && ident.equals(last_ident)) {
							changed = true;
							last_ident = ident;
						}
					}
				}
			}
			telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
			if (changed) {
				executor.execute(new Runnable() {
					public void run() {
						try {
							Location location = constructLocation(CellAPI2.retrieveLocation(meta_map, cellapi2_request, "single"));
							if (DEBUG) Log.d(TAG, "onCellInfoChanged: location=" + location);
							if (location != null) {
								networkLocationProvider.onLocationChanged(location);
							}
						}
						catch (Exception e) {
							Log.e(TAG, e.getMessage(), e);
						}
					}
				});
			}
			else {
				if (DEBUG) Log.e(TAG, "onCellInfoChanged: no change in environment, no request done");
			}
		}
		catch (Exception e) {
			Log.e(TAG, "e=" + e);
		}
	}

	static public Location constructLocation(TheList cellapi2_response) {
		Location location = null;
		for (TheDictionary ident_location: cellapi2_response) {
			if (!ident_location.getString("type").equals("m")) {
				long rcd = ident_location.getLong("rcd");
				if (rcd / 1000 == 2) {
					double latitude = ident_location.getDouble("latitude");
					double longitude = ident_location.getDouble("longitude");
					double accuracy = 5000.0;
					if (rcd != 2010 && ident_location.containsKey("radius")) { // radius given
						accuracy = ident_location.getDouble("radius");
					}
					location = new Location("network");
					location.setProvider(NetworkLocationProvider.IDENTIFIER);
					location.setLatitude(latitude);
					location.setLongitude(longitude);
					location.setAccuracy((float)accuracy);
					//location.setAltitude(0.0);
					//location.setSpeed(0.0f);
					//location.setBearing(0.0f);
					//location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
					location.setTime(System.currentTimeMillis());
					androidLocationLocationMakeComplete(location);
					androidLocationLocationSetExtraLocation(location, new Location(location));
					break;
				}
			}
		}
		return location;
	}

	static void androidLocationLocationMakeComplete(Location location) {
		if (DEBUG) Log.d(TAG, "androidLocationLocationMakeComplete:");
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
		if (DEBUG) Log.d(TAG, "run:");
		try {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_SERVICE_STATE);
			CellLocation.requestLocationUpdate();
		}
		finally {
		}
	}
}
