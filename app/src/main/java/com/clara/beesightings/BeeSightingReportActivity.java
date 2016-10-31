package com.clara.beesightings;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.clara.beesightings.firebase.BeeSighting;
import com.clara.beesightings.firebase.Firebase;


public class BeeSightingReportActivity extends AppCompatActivity {


	//TODO Location. Request network, GPS, or both?
	//Is timeout behavior bug in emulator or something wrong with code? If emulator is sent a location update it works. Times out otherwise.
	//Should notify user on timeout. Do thread properly - in Handler/Looper

	//TODO cancel background tasks as needed

	//TODO once user clicks submit, hold off on submitting more
	//reports until the one submitted has completed or failed


	private static final String TAG = "BEE SIGHTING ACTIVITY";
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
	private static final long GPS_TIMEOUT = 10 * 1000;   //10 seconds. Probably longer in practice.
	EditText mBeeNumberET;
	EditText mBeeLocationDescriptionET;
	Button mSubmitReportButton;
	Button mThisUserReportsListButton;
	Button mThisUserReportsMapButton;
	Button mAllUserReportMapButton;

	BeeSighting mCurrentSighting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bee_sighting_report);

		mBeeLocationDescriptionET = (EditText) findViewById(R.id.bee_location_et);
		mBeeNumberET = (EditText) findViewById(R.id.bee_number_et);

		mSubmitReportButton = (Button) findViewById(R.id.bee_report_submit_button);
		mThisUserReportsListButton = (Button) findViewById(R.id.this_users_reports_list_button);
		mThisUserReportsMapButton = (Button) findViewById(R.id.this_users_reports_map_button);
		mAllUserReportMapButton = (Button) findViewById(R.id.map_all_users_reports_button);

		mSubmitReportButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				submitReport();
			}
		});

		mThisUserReportsListButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				thisUserSightings();
			}
		});


		mThisUserReportsMapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				mapSightings(true);   /* onlyThisUser = true */
			}
		});


		mAllUserReportMapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mapSightings(false);   /* onlyThisUsers = false = show everyone */
			}
		});
	}




	private void submitReport() {

		String beeLoc = mBeeLocationDescriptionET.getText().toString();
		String beeNumStr = mBeeNumberET.getText().toString();

		if (beeLoc.length() == 0 || !beeNumStr.matches("^\\d+$"))  {
			Toast.makeText(this, "Please enter both a numer and description", Toast.LENGTH_LONG).show();
			return;
		}

		int beeNum = Integer.parseInt(mBeeNumberET.getText().toString());

		mCurrentSighting = new BeeSighting(beeNum, beeLoc);

		String userId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something is borked");
		Log.d(TAG, "userid from prefs = " +userId);
		mCurrentSighting.setUserId(userId);


		//TODO get location in callbacks. Pause app - prevent more submissions - until location obtained or not.
		getLocation();
	}



	private boolean haveLocationPermission() {
		/*Starting in API 23, permission requesting is different. Make request 23-style using the compat library */
		return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
	}

	private void requestLocationPermission() {
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
	}

	//This is the callback for ActivityCompat.requestPermissions
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		//Arrays are empty if request denied.
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
				//yay
				getLocation();
			}

			else {
				//denied! Snackbar would be nicer here.
				//TODO allow user to indicate location on a map. Useful for reporting sightings after the fact.
				Toast.makeText(this, "Need location to report sightings", Toast.LENGTH_LONG).show();
			}

		}
	}


	private boolean isLocationEnabled() {
		// StackOverflow: http://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled

		//KitKat and up - read the settings
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			try {
				int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
				return locationMode != Settings.Secure.LOCATION_MODE_OFF;
			} catch (Settings.SettingNotFoundException se) {
				Log.e(TAG, "Can't check settings for location", se);
				return false;
			}
			//Pre-kitkat (API 19), we'd like to deal with 15 and up
		} else {
			String locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED );
			return !TextUtils.isEmpty(locationProviders);   //If not empty, location is on
		}

	}

	private void getLocation() {

		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		if (!isLocationEnabled()) {
			sendUserToSettings();
			return;
		}

		if (!haveLocationPermission()) {
			requestLocationPermission();
			return;
		}

		try {

			Log.d(TAG, "Requesting single location");

			final LocationListener singleLocationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					//As soon as have location, notify
					Log.d(TAG, "Location available " + location.toString());
					notifyLocationAvailable(location);

					try {
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

			//request a location update

			//todo replace with Handler. Thread is bad(?) timeout for location not found.

			Thread timeoutThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(GPS_TIMEOUT);
						//stop location request
						locationManager.removeUpdates(singleLocationListener);  //does this break if listener is already removed?
						Log.d(TAG, "Location manager single location request timed out after " + GPS_TIMEOUT + " ms");
						//TODO notify user etc. Can't modify UI from this thread though.
						notifyLocationTimeout();
					} catch (InterruptedException e) {
						//whatever
					} catch (SecurityException se) {
						Log.e(TAG, "Remove updates permission exception", se);
					}
				}
			});

			//A race! (This is probably not how you do this)
			locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleLocationListener, null);
			locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleLocationListener, null);

			timeoutThread.start();


		} catch (SecurityException se) {
			Log.e(TAG, "Security error trying to access location. Sure you checked permissions correctly? ", se);
		}
	}


	//Called from thread so can't modify UI. TODO notify. TODO why is it timing out?
	private void notifyLocationTimeout() {
		//Toast.makeText(this, "Location request timed out", Toast.LENGTH_SHORT).show();
	}

	private void sendUserToSettings() {
		//Stack overflow http://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled
		//Show dialog, with positive button
		AlertDialog dialog = new AlertDialog.Builder(this)
				.setMessage("Location not enabled")
				.setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						Intent openSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(openSettings);
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						Toast.makeText(BeeSightingReportActivity.this, "Location needs to be enabled to report sightings", Toast.LENGTH_SHORT).show();
					}
				}).create();

		dialog.show();


	}

	private void notifyLocationAvailable(Location location) {
		//Now can save bee sighting

		mCurrentSighting.setLocation(location);

		Log.d(TAG, "About to save this sighting: " + mCurrentSighting);

		Firebase fb = new Firebase();
		fb.addSighting(mCurrentSighting);
		clearCurrentSighting();

	}

	private void clearCurrentSighting() {

		mCurrentSighting = null;
		mBeeLocationDescriptionET.getText().clear();
		mBeeNumberET.getText().clear();
		Toast.makeText(this, "Sending report to Firebase - thank you!", Toast.LENGTH_LONG).show();

	}


	private void thisUserSightings() {

		Intent thisUserSightings = new Intent(this, ManageSightingsActivity.class);
		startActivity(thisUserSightings);

	}

	private void mapSightings(boolean onlyThisUser) {

		Intent allSightings = new Intent(this, MapActivity.class);
		allSightings.putExtra(MapActivity.USER_SIGHTINGS_ONLY, onlyThisUser);
		startActivity(allSightings);

	}


}
