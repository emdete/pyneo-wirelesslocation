package org.pyneo.wirelesslocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.pyneo.wirelesslocation.cellapi.LocationSpec;
import org.pyneo.wirelesslocation.cellular.CellSpec;
import org.pyneo.wirelesslocation.cellular.CellSpecRetriever;
import org.pyneo.wirelesslocation.wlan.WifiSpec;
import org.pyneo.wirelesslocation.wlan.WifiSpecRetriever;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class LocationCalculator {
	private static final String TAG = "org.pyneo.wirelesslocation.LocationCalculator";

	public static final int MAX_WIFI_RADIUS = 500;
	private final LocationDatabase locationDatabase;
	private final LocationRetriever locationRetriever;
	private final CellSpecRetriever cellSpecRetriever;
	private final WifiSpecRetriever wifiSpecRetriever;

	public LocationCalculator(LocationDatabase locationDatabase, LocationRetriever locationRetriever, CellSpecRetriever cellSpecRetriever, WifiSpecRetriever wifiSpecRetriever) {
		Log.d(TAG, "LocationRetriever:");
		this.locationDatabase = locationDatabase;
		this.locationRetriever = locationRetriever;
		this.cellSpecRetriever = cellSpecRetriever;
		this.wifiSpecRetriever = wifiSpecRetriever;
	}

	private static <T extends PropSpec> Collection<Collection<LocationSpec<T>>> divideInClasses( Collection<LocationSpec<T>> locationSpecs, double accuracy) {
		Log.d(TAG, "divideInClasses:");
		Collection<Collection<LocationSpec<T>>> classes = new ArrayList<Collection<LocationSpec<T>>>();
		for (LocationSpec<T> locationSpec : locationSpecs) {
			boolean used = false;
			for (Collection<LocationSpec<T>> locClass : classes) {
				if (locationCompatibleWithClass(locationSpec, locClass, accuracy)) {
					locClass.add(locationSpec);
					used = true;
				}
			}
			if (!used) {
				Collection<LocationSpec<T>> locClass = new ArrayList<LocationSpec<T>>();
				locClass.add(locationSpec);
				classes.add(locClass);
			}
		}
		return classes;
	}

	private static <T extends PropSpec> boolean locationCompatibleWithClass(LocationSpec<T> locationSpec, Collection<LocationSpec<T>> locClass, double accuracy) {
		Log.d(TAG, "locationCompatibleWithClass:");
		for (LocationSpec<T> spec : locClass) {
			if ((locationSpec.distanceTo(spec) - locationSpec.getAccuracy() - spec.getAccuracy() - accuracy) < 0) {
				return true;
			}
		}
		return false;
	}

	private static <T extends PropSpec> boolean locationCompatibleWithClass(Location location, Collection<LocationSpec<T>> locClass) {
		Log.d(TAG, "locationCompatibleWithClass:");
		for (LocationSpec<T> spec : locClass) {
			if (MainService.DEBUG) Log.d(TAG, "location: " + location + ", spec: " + spec + " => dist:" + spec.distanceTo(location) + "m");
			if ((spec.distanceTo(location) - location.getAccuracy() - spec.getAccuracy()) < 0) {
				return true;
			}
		}
		return false;
	}

	private <T extends PropSpec> Location getAverageLocation(Collection<LocationSpec<T>> locationSpecs) {
		Log.d(TAG, "getAverageLocation:");
		// TODO: This is a stupid way to do this, we could do better by using the signal strength and triangulation
		double latSum = 0, lonSum = 0, accSum = 0;
		for (LocationSpec<T> locationSpec : locationSpecs) {
			latSum += locationSpec.getLatitude();
			lonSum += locationSpec.getLongitude();
			accSum += locationSpec.getAccuracy();
		}
		Location location = new Location("network");
		location.setAccuracy((float) (accSum / locationSpecs.size()));
		location.setLatitude(latSum / locationSpecs.size());
		location.setLongitude(lonSum / locationSpecs.size());
		Log.d(TAG, "getAverageLocation: location=" + location);
		return location;
	}

	public Location getCurrentCellLocation() {
		Log.d(TAG, "getCurrentCellLocation:");
		Location location = null;
		Collection<LocationSpec<CellSpec>> cellLocationSpecs = getLocation(getCurrentCells());
		if (cellLocationSpecs != null && !cellLocationSpecs.isEmpty()) {
			location = getAverageLocation(cellLocationSpecs);
			Bundle b = new Bundle();
			b.putString("networkLocationType", "cell");
			location.setExtras(b);
		}
		Log.d(TAG, "getCurrentCellLocation: location=" + location);
		return location;
	}

	private Collection<CellSpec> getCurrentCells() {
		Log.d(TAG, "getCurrentCells:");
		Collection<CellSpec> ret = cellSpecRetriever.retrieveCellSpecs();
		Log.d(TAG, "getCurrentCells: ret=" + ret);
		return ret;
	}

	public Location getCurrentLocation() {
		Log.d(TAG, "getCurrentLocation:");
		Location cellLocation = getCurrentCellLocation();
		Location wifiLocation = getCurrentWifiLocation(cellLocation);
		if (wifiLocation != null) {
			Log.d(TAG, "getCurrentLocation: location=" + wifiLocation);
			return wifiLocation;
		}
		Log.d(TAG, "getCurrentLocation: location=" + cellLocation);
		return cellLocation;
	}

	public Location getCurrentWifiLocation(Location cellLocation) {
		Log.d(TAG, "getCurrentWifiLocation:");
		Collection<LocationSpec<WifiSpec>> wifiLocationSpecs = getLocation(getCurrentWifis());
		if (wifiLocationSpecs.isEmpty() || ((cellLocation == null) && (wifiLocationSpecs.size() < 2))) {
			return null;
		}
		Location location = null;
		if (cellLocation == null) {
			List<Collection<LocationSpec<WifiSpec>>> classes = new ArrayList<Collection<LocationSpec<WifiSpec>>>(divideInClasses(wifiLocationSpecs, MAX_WIFI_RADIUS));
			Collections.sort(classes, CollectionSizeComparator.INSTANCE);
			location = getAverageLocation(classes.get(0));
		} else {
			List<Collection<LocationSpec<WifiSpec>>> classes = new ArrayList<Collection<LocationSpec<WifiSpec>>>(divideInClasses(wifiLocationSpecs, cellLocation.getAccuracy()));
			Collections.sort(classes, CollectionSizeComparator.INSTANCE);
			for (Collection<LocationSpec<WifiSpec>> locClass : classes) {
				if (MainService.DEBUG) Log.d(TAG, "Test location class with "+locClass.size()+" entries");
				if (locationCompatibleWithClass(cellLocation, locClass)) {
					if (MainService.DEBUG) Log.d(TAG, "Location class matches, using its average");
					location = getAverageLocation(locClass);
					break;
				}
			}
		}
		if (location != null) {
			Bundle b = new Bundle();
			b.putString("networkLocationType", "wifi");
			location.setExtras(b);
		}
		Log.d(TAG, "getCurrentWifiLocation: location=" + location);
		return location;
	}

	private Collection<WifiSpec> getCurrentWifis() {
		Log.d(TAG, "getCurrentWifis:");
		return wifiSpecRetriever.retrieveWifiSpecs();
	}

	private <T extends PropSpec> Collection<LocationSpec<T>> getLocation(Collection<T> specs) {
		Log.d(TAG, "getLocation:");
		Collection<LocationSpec<T>> locationSpecs = new HashSet<LocationSpec<T>>();
		for (T spec : specs) {
			LocationSpec<T> locationSpec = locationDatabase.get(spec);
			if (locationSpec == null) {
				locationRetriever.queueLocationRetrieval(spec);
			} else if (!locationSpec.isUndefined()){
				locationSpecs.add(locationSpec);
			}
		}
		Log.d(TAG, "getLocation: locationSpecs=" + locationSpecs);
		return locationSpecs;
	}

	public static class CollectionSizeComparator implements Comparator<Collection<LocationSpec<WifiSpec>>> {
		public static CollectionSizeComparator INSTANCE = new CollectionSizeComparator();

		@Override
		public int compare(Collection<LocationSpec<WifiSpec>> left, Collection<LocationSpec<WifiSpec>> right) {
			Log.d(TAG, "compare:");
			return (left.size() < right.size()) ? -1 : ((left.size() > right.size()) ? 1 : 0);
		}
	}
}

