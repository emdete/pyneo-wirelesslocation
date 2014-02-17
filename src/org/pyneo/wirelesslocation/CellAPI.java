package org.pyneo.wirelesslocation;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.security.MessageDigest;
import android.app.Activity;
import android.content.Context;
import android.location.LocationListener;
import android.telephony.NeighboringCellInfo;
import android.location.LocationManager;
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
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import org.json.JSONObject;
import java.math.BigInteger;

public class CellAPI {
	private static final String TAG = "org.pyneo.wirelesslocation.CellAPI";

	private static final String url = "https://cell.vodafone.com/loc";
	private static final String rid = "pyneo";
	private static final String secret = "aN3Peiv6";
	private static final Random random = new Random();

	static public Double[] retrieveLocation(Map<String,Object> map) {
		Double[] location = null;
		if (MainService.DEBUG) Log.d(TAG, "retrieveLocation: map=" + map);
		try {
			map = resolve(
				map.get("mcc").toString(),
				map.get("mnc").toString(),
				map.get("lac").toString(),
				map.get("rncid").toString(),
				map.get("cid").toString(),
				//map.get("uid").toString()
				"");
			int rcd = Integer.parseInt(map.get("rcd").toString());
			if (rcd < 2030) { // 2030: based on mcc
				final double lat = Double.parseDouble(map.get("lat").toString());
				final double lon = Double.parseDouble(map.get("lon").toString());
				double rad;
				final double altitude = 0.0;
				if (rcd == 2000) // rad included
					rad = Double.parseDouble(map.get("rad").toString());
				else
					rad = 5000.0;
				location = new Double[]{lat, lon, altitude, rad};
			}
			else {
				Log.e(TAG, "retrieveLocation: error=" + rcd);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "retrieveLocation: exception=" + e);
			if (MainService.DEBUG) Log.wtf(TAG, "retrieveLocation: exception=" + e, e);
		}
		if (MainService.DEBUG) Log.d(TAG, "retrieveLocation: location=" + location);
		return location;
	}

	static String generateMD5(String input) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("md5");
		return new BigInteger(1, digest.digest( input.getBytes("utf-8"))).toString(16).toLowerCase();
	}

	static Map<String,Object> decode(String line) throws Exception {
		StringTokenizer pairs = new StringTokenizer(line.trim(), "&");
		Map<String,Object> tx = new HashMap<String,Object>();
		while (pairs.hasMoreTokens()) {
			String pair = (String)pairs.nextToken();
			if (pair.length() > 0) {
				int index = pair.indexOf('=');
				if (index > 0) {
					String name = URLDecoder.decode(
						pair.substring(0, index), "UTF-8");
					String value = URLDecoder.decode(
						pair.substring(index+1), "UTF-8");
					tx.put(name, value);
				}
			}
		}
		return tx;
	}

	static String encode(Map<String,Object> map) throws Exception {
		StringBuffer res = new StringBuffer();
		for (String key : map.keySet()) {
			String value = map.get(key).toString();
			if (value != null)
				if (res.length() > 0)
					res.append('&');
				res
					.append(key)
					.append('=')
					.append(URLEncoder.encode(value, "utf-8"));
		}
		return res.toString();
	}

	static Map<String,Object> resolve(
		String mc,
		String mn,
		String la,
		String ri,
		String ci,
		String uid
		) throws Exception
	{
		String tim = Long.toString(System.currentTimeMillis() / 1000);
		String ver = "1";
		String correlation_id = null;
		// enable if under log4j: correlation_id = org.apache.log4j.MDC.get("correlation-id");
		if (correlation_id == null)
			correlation_id = Long.toString(random.nextLong());
		String aup = generateMD5(
			secret +
			// manually sorted:
			ci +
			la +
			mc +
			mn +
			ri +
			rid +
			tim +
			uid +
			ver +
			"");
		// create the parameter block:
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("mc", mc);
		map.put("mn", mn);
		map.put("ri", ri);
		map.put("la", la);
		map.put("ci", ci);
		if (MainService.DEBUG) Log.d(TAG, "resolve: parameter=" + map);
		java.net.URLConnection conn = new java.net.URL(url + "?" + encode(map)).openConnection();
		// fill in the meta parameter:
		conn.setRequestProperty("x-vf-csl-aup", aup);
		conn.setRequestProperty("x-vf-csl-rid", rid);
		conn.setRequestProperty("x-vf-csl-tim", tim);
		conn.setRequestProperty("x-vf-csl-uid", uid);
		conn.setRequestProperty("x-vf-csl-ver", ver);
		conn.setRequestProperty("x-correlation-id", correlation_id);
		if (MainService.DEBUG) Log.d(TAG, "resolve: meta=" + conn.getRequestProperties());
		java.io.InputStream input = conn.getInputStream();
		byte[] buffer = new byte[1024];
		String response = "";
		int amount = 0;
		while (amount != -1) {
			amount = input.read(buffer);
			if (amount > 0)
				response += new String(buffer, 0, amount);
		}
		Map<String,Object> ret = decode(response);
		if (MainService.DEBUG) Log.d(TAG, "resolve: response=" + ret);
		return ret;
	}

	static void toMap(Map<String,Object> map, int idx, CellIdentityCdma value) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("type", "cdma");
		map.put("basestationId", value.getBasestationId());
		map.put("latitude", value.getLatitude());
		map.put("longitude", value.getLongitude());
		map.put("networkId", value.getNetworkId());
		map.put("systemId", value.getSystemId());
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CellIdentityGsm value) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("type", "gsm");
		map.put("cid", value.getCid());
		map.put("lac", value.getLac());
		map.put("mcc", value.getMcc());
		map.put("mnc", value.getMnc());
		map.put("psc", value.getPsc());
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CellIdentityWcdma value) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("type", "wcdma");
		map.put("mcc", value.getMcc());
		map.put("mnc", value.getMnc());
		map.put("lac", value.getLac());
		map.put("psc", value.getPsc());
		map.put("cid", value.getCid());
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CellIdentityLte value) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("type", "lte");
		map.put("ci", value.getCi());
		map.put("mcc", value.getMcc());
		map.put("mnc", value.getMnc());
		map.put("pci", value.getPci());
		map.put("tac", value.getTac());
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CellInfo value) throws Exception {
		if (value == null) return;
		map.put("timeStamp", value.getTimeStamp());
		//map.put("registered", value.isRegistered());
		if (value instanceof CellInfoCdma) {
			toMap(map, idx, ((CellInfoCdma)value).getCellIdentity());
			// TODO: toMap(map, idx, ((CellInfoCdma)value).getCellSignalStrength());
		}
		else if (value instanceof CellInfoGsm) {
			toMap(map, idx, ((CellInfoGsm)value).getCellIdentity());
			// TODO: toMap(map, idx, ((CellInfoGsm)value).getCellSignalStrength());
		}
		else if (value instanceof CellInfoLte) {
			toMap(map, idx, ((CellInfoLte)value).getCellIdentity());
			// TODO: toMap(map, idx, ((CellInfoLte)value).getCellSignalStrength());
		}
		else if (value instanceof CellInfoWcdma) {
			toMap(map, idx, ((CellInfoWcdma)value).getCellIdentity());
			// TODO: toMap(map, idx, ((CellInfoWcdma)value).getCellSignalStrength());
		}
		else {
		}
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CellLocation value, int mcc, int mnc) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("mcc", mcc);
		map.put("mnc", mnc);
		if (value instanceof GsmCellLocation) {
			toMap(map, idx, ((GsmCellLocation)value), mcc, mnc);
		}
		else if (value instanceof CdmaCellLocation) {
			toMap(map, idx, ((CdmaCellLocation)value));
		}
		else {
			map.put("class", value.getClass().getName());
			map.put("string", value.toString());
		}
		return;
	}

	static void toMap(Map<String,Object> map, int idx, CdmaCellLocation value) throws Exception {
		if (value == null) return;
	}

	static void toMap(Map<String,Object> map, int idx, GsmCellLocation value, int mcc, int mnc) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		int i = value.getCid();
		if (i < 0x10000) {
			map.put("cid", i);
			map.put("type", "gsm");
		}
		else {
			map.put("rncid", i / 0x10000);
			map.put("cid", i % 0x10000);
			map.put("type", "wcdma");
		}
		map.put("lac", value.getLac());
		map.put("psc", value.getPsc());
		return;
	}

	static void toMap(Map<String,Object> map, int idx, NeighboringCellInfo value, int mcc, int mnc) throws Exception {
		if (value == null) return;
		String prefix = idx<0? "": Integer.toString(idx)+".";
		map.put("mcc", mcc);
		map.put("mnc", mnc);
		int i;
		i = value.getLac(); if (i != NeighboringCellInfo.UNKNOWN_CID) map.put("lac", i);
		i = value.getCid(); if (i != NeighboringCellInfo.UNKNOWN_CID) map.put("cid", i);
		i = value.getPsc(); if (i != NeighboringCellInfo.UNKNOWN_CID) map.put("psc", i);
		i = value.getRssi(); if (i != NeighboringCellInfo.UNKNOWN_CID) map.put("rssi", i);
		return;
	}

	static void toMap(Map<String,Object> map, List<NeighboringCellInfo> value, int mcc, int mnc) throws Exception {
		if (value == null) return;
		int i = 0;
		for (NeighboringCellInfo cellInfo : value) {
			toMap(map, i++, cellInfo, mcc, mnc);
		}
		return;
	}

	static void toMap(Map<String,Object> map, List<CellInfo> value, TelephonyManager telephonyManager) throws Exception {
		if (value == null) {
			// fallback:
			map.put("version", "pre-api17");
			int mcc = NeighboringCellInfo.UNKNOWN_CID;
			int mnc = NeighboringCellInfo.UNKNOWN_CID;
			String mccmnc = telephonyManager.getNetworkOperator();
			if (mccmnc != null && mccmnc.length() >= 5 && mccmnc.length() <= 6) {
				mcc = Integer.parseInt(mccmnc.substring(0, 3));
				mnc = Integer.parseInt(mccmnc.substring(3));
			}
			toMap(map, -1, telephonyManager.getCellLocation(), mcc, mnc);
			toMap(map, telephonyManager.getNeighboringCellInfo(), mcc, mnc);
		}
		else {
			try {
				int i = 0;
				for (CellInfo cellInfo: value) {
					if (cellInfo.isRegistered()) {
						toMap(map, i++, cellInfo);
					}
					else {
						toMap(map, -1, cellInfo);
					}
				}
			}
			catch (Exception e) {
				// I/dalvikvm(12043): Could not find method
				// android.telephony.CellIdentityWcdma.getMcc, referenced from method
				// com.vodafone.proerrrep.Engine.toMap

				// E/dalvikvm(12043): Could not find class 'android.telephony.CellInfoWcdma',
				// referenced from method com.vodafone.proerrrep.Engine.toMap
				map.put("error", e.getMessage());
			}
		}
		return;
	}

	static void toMap(Map<String,Object> map, TelephonyManager value) throws Exception {
		if (value == null) return;
		map.put("device_id", value.getDeviceId());
		map.put("device_software_version", value.getDeviceSoftwareVersion());
		map.put("network_operator", value.getNetworkOperator());
		map.put("network_type", value.getNetworkType());
		map.put("sim_operator", value.getSimOperator());
		map.put("subscriber_id", value.getSubscriberId());
		toMap(map, value.getAllCellInfo(), value);
		return;
	}
}
