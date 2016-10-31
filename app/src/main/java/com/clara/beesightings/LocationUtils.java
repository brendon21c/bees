package com.clara.beesightings;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * Handles obtaining the current location, if available


 TODO TEST on devices and under various conditions

 FIXME Start app in emulator API 24. Turn off location, then future location
 requests time out; until the Emulator is sent a new mock location.
 This behavior was not observed on real device API 21 - should test on others.

 TODO Should notify user on timeout? This would happen if a real device had location enabled and permission was granted, but had a poor GPS signal

*/


public class LocationUtils {


	private static final String TAG = "LOCATION UTIlS";

	private static final long GPS_TIMEOUT = 10 * 1000;   //10 seconds. todo investigate the best timeout period

	interface LocationResultListener {
		void notifyLocationResult(Location location);
	}


	private boolean locationFound = false;

	void getLocation(Context context, final LocationResultListener listener) {

		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);


		try {

			Log.d(TAG, "Requesting single location");

			final LocationListener singleLocationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					//As soon as have location, notify
					Log.d(TAG, "Location available " + location.toString());
					listener.notifyLocationResult(location);

					try {
						locationFound = true;
						locationManager.removeUpdates(this);
					} catch (SecurityException se) {
						Log.e(TAG, "Location manager listener permission error removing location listener", se);
					}
				}

				@Override
				public void onStatusChanged(String s, int i, Bundle bundle) {
					Log.d(TAG, "Location manager listener status changed" + s + i + bundle);
				}

				@Override
				public void onProviderEnabled(String s) {
					Log.d(TAG, "Location manager listener provider enabled");
				}

				@Override
				public void onProviderDisabled(String s) {
					Log.d(TAG, "Location manager listener provider disabled");
				}
			};

			//Background thread to implement timeout period, so device doesn't wait forever for location
			//location is battery-intensive. If location not available after timeout period, cancel listeners.
			//User can try again if they want.

			Thread timeoutThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {

						Thread.sleep(GPS_TIMEOUT);    //Wait for the timeout. Remove location listener at end of timeout.

						if (locationFound) {
							//A location was found within the timeout period, and has been handled by onLocationChanged. No action required
						}

						else {
							//A location was not found. Log
							Log.d(TAG, "Location manager single location request timed out after " + GPS_TIMEOUT + " ms");
							listener.notifyLocationResult(null);   //Indicate no location found
						}

						locationManager.removeUpdates(singleLocationListener);  //Remove listener

					} catch (InterruptedException e) {
						//whatever - this may be thrown by the Thread.sleep call
					} catch (SecurityException se) {
						Log.e(TAG, "Remove updates permission exception", se);
					}
				}
			});

			//A race! (This is probably not how you do this). Whichever one gets the location first will be the location used, the other will be cancelled
			locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleLocationListener, null);
			locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleLocationListener, null);

			timeoutThread.start();


		} catch (SecurityException se) {
			Log.e(TAG, "Security error trying to access location. Sure you checked permissions correctly? ", se);
		}
	}




	 boolean isLocationEnabled(Context context) {

		// StackOverflow: http://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled

		//KitKat and up - read the settings
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			try {
				int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

				boolean enabled = (locationMode != Settings.Secure.LOCATION_MODE_OFF);
				Log.d(TAG, "KitKat+ check, location is enabled: " + enabled);
				return enabled;

			} catch (Settings.SettingNotFoundException se) {
				Log.e(TAG, "Can't check settings for location", se);
				return false;
			}
			//Pre-kitkat (API 19), we'd like to deal with 15 and up
		} else {
			String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED );
			boolean enabled = !TextUtils.isEmpty(locationProviders);   //If not empty, location is on
			Log.d(TAG, "Pre-Kitkat check, location is enabled: " + enabled);
			return enabled;

		}

	}


}
