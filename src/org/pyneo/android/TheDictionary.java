package org.pyneo.android;

import android.util.Log;

import org.json.simple.JSONObject;
import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;

import java.io.Writer;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TheDictionary implements JSONStreamAware, JSONAware, Map<String,Object> {
	private static final String TAG = TheDictionary.class.getName();
	private static boolean DEBUG = false;
	static { DEBUG = Log.isLoggable("org.pyneo.android", Log.DEBUG); }

	private Map<String,Object> map = new JSONObject();

	public TheDictionary() {
	}

	public TheDictionary(JSONObject map) {
		this.map = map;
	}

	@Override
	public void clear() {
		this.map.clear();
	}

	@Override
	public boolean containsKey(Object o) {
		return this.map.containsKey(o);
	}

	@Override
	public boolean containsValue(Object o) {
		return this.map.containsValue(o);
	}

	@Override
	public Set<Entry<String,Object>> entrySet() {
		return this.map.entrySet();
	}

	public Object pop(Object o) {
		Object r = this.map.get(o);
		this.map.remove(o);
		return r;
	}

	@Override
	public Object get(Object o) {
		return this.map.get(o);
	}

	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return this.map.keySet();
	}

	public Object put(String key, Object value) {
		return this.map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		if (map != null) {
			this.map.putAll(map);
		}
	}

	public void putAll(TheDictionary map) {
		if (map != null) {
			this.map.putAll(map.map);
		}
	}

	@Override
	public Object remove(Object o) {
		return this.map.remove(o);
	}

	@Override
	public int size() {
		return this.map.size();
	}

	public Collection<Object> values() {
		return this.map.values();
	}

	public boolean getBoolean(String key) {
		if (!this.map.containsKey(key))
			return false;
		return (Boolean)this.map.get(key);
	}

	public long getLong(String key) {
		if (!this.map.containsKey(key))
			return 0;
		return (Long)this.map.get(key);
	}

	public double getDouble(String key) {
		if (!this.map.containsKey(key))
			return Double.NaN;
		return (Double)this.map.get(key);
	}

	public String getString(String key) {
		if (!this.map.containsKey(key))
			return "";
		Object obj = this.map.get(key);
		return obj == null? null: obj.toString();
	}

	public String getIdent() {
		String ident = null;
		if (containsKey("ident")) {
			ident = getString("ident");
		}
		else {
			char type = getString("type").charAt(0);
			switch (type) {
			case '\0':
				// dummy null value, no ident
			break;
			case '1':
			case '2':
				if (containsKey("mcc") && containsKey("mnc") && containsKey("lac") && containsKey("cid")) {
					ident = getString("type") + ":" + getString("mcc") + "." + getString("mnc") + "." + getString("lac") + "." + getString("cid");
				}
			break;
			case '3':
				if (containsKey("mcc") && containsKey("mnc") && containsKey("lac") && containsKey("rncid") && containsKey("cid")) {
					ident = getString("type") + ":" + getString("mcc") + "." + getString("mnc") + "." + getString("lac") + "." + getString("rncid") + "." + getString("cid");
				}
			break;
			case '4':
				if (containsKey("mcc") && containsKey("mnc") && containsKey("tac") && containsKey("ci")) {
					ident = getString("type") + ":" + getString("mcc") + "." + getString("mnc") + "." + getString("tac") + "." + getString("ci");
				}
			break;
			case 'w':
				if (containsKey("bssid")) {
					ident = getString("type") + ":" + getString("bssid");
				}
			break;
			case 'm':
				ident = getString("type") + ":";
			break;
			default:
				Log.e(TAG, "getIdent: type=" + type);
			break;
			}
			put("ident", ident);
		}
		return ident;
	}

	public String toJSONString() {
		return ((JSONObject)this.map).toJSONString();
	}

	public String toString() {
		return this.map == null ? null : this.map.toString();
	}

	public void writeJSONString(Writer out) throws IOException {
		((JSONObject)this.map).writeJSONString(out);
	}
}
