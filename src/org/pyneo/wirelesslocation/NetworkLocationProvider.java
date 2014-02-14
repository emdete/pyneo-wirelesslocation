package org.pyneo.wirelesslocation;

import android.location.Location;
import android.os.IBinder;


public interface NetworkLocationProvider {
	final static String NETWORK_LOCATION_TYPE = "networkLocationType";
	final static String IDENTIFIER = "network";

	void disable();

	void enable();

	IBinder getBinder();

	boolean isActive();

	void onLocationChanged(Location paramLocation);
}
