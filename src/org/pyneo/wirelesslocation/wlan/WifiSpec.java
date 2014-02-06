package org.pyneo.wirelesslocation.wlan;

import org.pyneo.wirelesslocation.PropSpec;

public class WifiSpec implements PropSpec {
	private final MacAddress mac;
	private final String ssid;
	private int channel;
	private int frequency;
	private int signal;

	public WifiSpec(MacAddress mac, String ssid) {
		this.mac = mac;
		this.ssid = ssid;
	}

	public WifiSpec(MacAddress mac, String ssid, int channel) {
		this.mac = mac;
		this.ssid = ssid;
		this.channel = channel;
	}

	public WifiSpec(MacAddress mac, String ssid, int frequency, int signal) {
		this.mac = mac;
		this.ssid = ssid;
		this.frequency = frequency;
		this.setSignal(signal);
	}

	@Override
	public byte[] getIdentBlob() {
		byte[] bytes = new byte[10];
		bytes[0] = 'w';
		bytes[1] = 'i';
		bytes[2] = 'f';
		bytes[3] = 'i';
		for(int i = 0; i < 6; ++i) {
			bytes[i+4] = (byte) mac.getBytes()[i];
		}
		return bytes;
	}

	public MacAddress getMac() {
		return mac;
	}

	public int getSignal() {
		return signal;
	}

	public void setSignal(int signal) {
		this.signal = signal;
	}

	@Override
	public String toString() {
		return "WifiSpec{" + "mac=" + mac + ", ssid=" + ssid + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WifiSpec wifiSpec = (WifiSpec) o;

		if (mac != null ? !mac.equals(wifiSpec.mac) : wifiSpec.mac != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return mac != null ? mac.hashCode() : 0;
	}
}
