package org.pyneo.wirelesslocation.wlan;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.pyneo.wirelesslocation.MainService;

import java.util.ArrayList;
import java.util.Collection;

public class WifiSpecRetriever {
	private static final String TAG = "org.pyneo.wirelesslocation.platform.WifiSpecRetriever";
	private final WifiManager wifiManager;

	public WifiSpecRetriever(WifiManager wifiManager) {
		this.wifiManager = wifiManager;
	}

	public WifiSpecRetriever(Context context) {
		this((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
	}

	public Collection<WifiSpec> retrieveWifiSpecs() {
		Collection<WifiSpec> wifiSpecs = new ArrayList<WifiSpec>();
		if (wifiManager == null) {
			return wifiSpecs;
		}
		Collection<ScanResult> scanResults = wifiManager.getScanResults();
		if (scanResults == null) {
			return wifiSpecs;
		}
		for (ScanResult scanResult : scanResults) {
			MacAddress macAddress = MacAddress.parse(scanResult.BSSID);
			String ssid = scanResult.SSID;
			int frequency = scanResult.frequency;
			int level = scanResult.level;
			wifiSpecs.add(new WifiSpec(macAddress, ssid, frequency, level));
		}
		if (MainService.DEBUG) {
			Log.d(TAG, "Found " + wifiSpecs.size() + " Wifis");
			for (Object wifiSpec: wifiSpecs) {
				Log.d(TAG, "Found " + wifiSpec);
			}
		}
		return wifiSpecs;
	}
}
