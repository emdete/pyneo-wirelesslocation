package org.pyneo.wirelesslocation.cellapi;

import org.pyneo.wirelesslocation.LocationCalculator;
import org.pyneo.wirelesslocation.MainService;
import org.pyneo.wirelesslocation.NetworkLocationProvider;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

public class WirelessLocationThread extends Thread {
	private static final String TAG = "org.pyneo.wirelesslocation.NetworkLocationThread";

	private LocationCalculator calculator;
	private long autoTime;
	private boolean autoUpdate;
	private boolean forceUpdate;
	private boolean enabled;
	private Location lastLocation;
	private NetworkLocationProvider locationProvider;
	private long lastTime;

	public WirelessLocationThread() {
		if (MainService.DEBUG) Log.d(TAG, "WirelessLocationThread:");
		autoUpdate = false;
		autoTime = Long.MAX_VALUE;
		lastTime = 0;
		enabled = true;
		forceUpdate = true;
	}

	public WirelessLocationThread(WirelessLocationThread oldThread) {
		this();
		if (MainService.DEBUG) Log.d(TAG, "WirelessLocationThread: oldThread");
		if (oldThread != null) {
			calculator = oldThread.calculator;
			lastLocation = oldThread.lastLocation;
			lastTime = oldThread.lastTime;
		}
	}

	public void disable() {
		if (MainService.DEBUG) Log.d(TAG, "disable:");
		enabled = false;
		synchronized (this) {
			notify();
		}
	}

	public long getLastTime() {
		return lastTime;
	}

	public void setLastTime(final long lastTime) {
		this.lastTime = lastTime;
	}

	public boolean isActive() {
		return enabled && autoUpdate && (autoTime < 60000);
	}

	@Override
	public void run() {
		if (MainService.DEBUG) Log.d(TAG, "run:");
		while (enabled) {
			boolean waited = false;
			try {
				synchronized (this) {
					if (!autoUpdate && !forceUpdate && enabled) {
						if (MainService.DEBUG) Log.d(TAG, "not active, waiting");
						wait();
						waited = true;
					}
				}
			}
			catch (final InterruptedException e) {
			}
			long wait;
			while ((wait = lastTime + autoTime - SystemClock.elapsedRealtime()) > 0 && autoUpdate && !forceUpdate && enabled) {
				final float w = wait / 1000F;
				if (MainService.DEBUG) Log.d(TAG, "run: lastTime=" + lastTime + ", autoTime=" + autoTime + ", currentTime=" + SystemClock.elapsedRealtime() + ", wait=" + w);
				try {
					synchronized (this) {
						wait(wait);
					}
					waited = true;
				}
				catch (final InterruptedException e) {
					break;
				}
			}

			if (MainService.DEBUG) Log.d(TAG, "run: waited=" + waited + ", lastTime=" + lastTime + ", autoTime=" + autoTime + ", currentTime=" + SystemClock.elapsedRealtime());
			if (!waited && enabled) {
				if (MainService.DEBUG) Log.w(TAG, "run: waiting 5s to prevent mass update");
				try {
					synchronized (this) {
						wait(5000);
					}
				}
				catch (final InterruptedException e) {
					continue;
				}
			}
			if ((autoUpdate || forceUpdate) && calculator != null && enabled) {
				if (forceUpdate) {
					if (MainService.DEBUG) Log.d(TAG, "run: Update forced because of new incoming request");
					forceUpdate = false;
				}
				lastTime = SystemClock.elapsedRealtime();
				if (locationProvider != null) {
					if (MainService.DEBUG) Log.d(TAG, "run: Now requesting");
					locationProvider.onLocationChanged(calculator.getCurrentLocation());
				}
			}
			else {
				if (MainService.DEBUG) Log.d(TAG, "run: we're not active (or not initialized yet) = do not track!");
			}
		}
		if (MainService.DEBUG) Log.d(TAG, "run: terminating");
	}

	public void setAuto(final boolean autoUpdate, final long autoTime) {
		if (MainService.DEBUG) Log.d(TAG, "setAuto autoTime=" + autoTime);
		synchronized (this) {
			if (autoTime < this.autoTime) {
				forceUpdate = true;
			}
			this.autoUpdate = autoUpdate;
			this.autoTime = autoTime;
			notify();
		}
	}

	public void setCalculator(LocationCalculator calculator) {
		this.calculator = calculator;
	}

	public void setLastLocation(final Location location) {
		lastLocation = location;
	}

	public void setLocationProvider(NetworkLocationProvider locationProvider) {
		this.locationProvider = locationProvider;
	}
}
