package org.pyneo.wirelesslocation.cellapi;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.pyneo.wirelesslocation.cellular.CellSpec;
import org.pyneo.wirelesslocation.MainService;

public class WirelessIdLocation implements LocationSource<CellSpec> {
	private static final String TAG = "org.pyneo.wirelesslocation.cellapi.WirelessIdLocation";
	private static final String NAME = "CellApi WirelessIdLocation";
	private static final String DESCRIPTION = "Read cell locations cellapi";
	private static final String COPYRIGHT = "© pyneo 2014";

	static final private java.util.Random random = new java.util.Random();
	static final private String url = "https://cell.vodafone.com/loc";
	static final private String rid = "pyneo";
	static final private String secret = "aN3Peiv6";

	public WirelessIdLocation() {
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getCopyright() {
		return COPYRIGHT;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isSourceAvailable() {
		return true;
	}

	@Override
	public Collection<LocationSpec<CellSpec>> retrieveLocation(Collection<CellSpec> specs) {
		List<LocationSpec<CellSpec>> locationSpecs = new ArrayList<LocationSpec<CellSpec>>();
		for (CellSpec spec : specs) {
			try {
				if (MainService.DEBUG) {
					Log.i(TAG, "checking for " + spec);
				}
				java.util.Map<String,String> map = resolve(
					Integer.toString(spec.getMcc()),
					Integer.toString(spec.getMnc()),
					Integer.toString(spec.getLac()),
					Integer.toString(0),
					Integer.toString(spec.getCid()),
					"-");
				int rcd = Integer.parseInt(map.get("rcd"));
				if (rcd < 2030) { // 2030: based on mcc
					double lat = Double.parseDouble(map.get("lat"));
					double lon = Double.parseDouble(map.get("lon"));
					double rad;
					if (rcd == 2000) // include rad
						rad = Double.parseDouble(map.get("rad"));
					else
						rad = 5000.0;
					locationSpecs.add(new LocationSpec<CellSpec>(spec,
						lat,
						lon,
						0.0,
						rad
						));
				}
				else {
					Log.e(TAG, "retrieveLocation: error=" + rcd);
				}
			}
			catch (Exception e) {
				Log.e(TAG, "retrieveLocation: exception=" + e);
			}
		}
		return locationSpecs;
	}

	static String generateMD5(String input) throws Exception {
		java.security.MessageDigest digest = java.security.MessageDigest.getInstance("md5");
		return new java.math.BigInteger(1, digest.digest(
			input.getBytes("utf-8"))).toString(16).toLowerCase();
	}

	static java.util.Map<String,String> decode(String line) throws Exception {
		java.util.StringTokenizer pairs = new java.util.StringTokenizer(line.trim(), "&");
		java.util.Map<String,String> tx = new java.util.HashMap();
		while (pairs.hasMoreTokens()) {
			String pair = (String)pairs.nextToken();
			if (pair.length() > 0) {
				int index = pair.indexOf('=');
				if (index > 0) {
					String name = java.net.URLDecoder.decode(
						pair.substring(0, index), "UTF-8");
					String value = java.net.URLDecoder.decode(
						pair.substring(index+1), "UTF-8");
					tx.put(name, value);
				}
			}
		}
		return tx;
	}

	static String encode(java.util.Map<String,String> map) throws Exception {
		StringBuffer res = new StringBuffer();
		for (String key : map.keySet()) {
			String value = map.get(key);
			if (value != null)
				if (res.length() > 0)
					res.append('&');
				res
					.append(key)
					.append('=')
					.append(java.net.URLEncoder.encode(value, "utf-8"));
		}
		return res.toString();
	}

	static java.util.Map<String,String> resolve(
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
		java.util.Map<String,String> map = new java.util.HashMap<String,String>();
		map.put("mc", mc);
		map.put("mn", mn);
		map.put("ri", ri);
		map.put("la", la);
		map.put("ci", ci);
		Log.d(TAG, "parameter=" + map);
		java.net.URLConnection conn = new java.net.URL(url + "?" + encode(map)).openConnection();
		// fill in the meta parameter:
		conn.setRequestProperty("x-vf-csl-aup", aup);
		conn.setRequestProperty("x-vf-csl-rid", rid);
		conn.setRequestProperty("x-vf-csl-tim", tim);
		conn.setRequestProperty("x-vf-csl-uid", uid);
		conn.setRequestProperty("x-vf-csl-ver", ver);
		conn.setRequestProperty("x-correlation-id", correlation_id);
		Log.d(TAG, "meta=" + conn.getRequestProperties());
		java.io.InputStream input = conn.getInputStream();
		byte[] buffer = new byte[1024];
		String response = "";
		int amount = 0;
		while (amount != -1) {
			amount = input.read(buffer);
			if (amount > 0)
				response += new String(buffer, 0, amount);
		}
		java.util.Map<String,String> ret = decode(response);
		Log.d(TAG, "response=" + ret);
		return ret;
	}
}
