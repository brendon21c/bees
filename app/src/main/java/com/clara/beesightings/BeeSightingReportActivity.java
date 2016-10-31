package com.clara.beesightings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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

public class BeeSightingReportActivity extends AppCompatActivity implements Firebase.CompleteListener, LocationUtils.LocationResultListener {

	private static final String TAG = "BEE SIGHTING ACTIVITY";
	private static final String ACTION_ADD_NEW = "add_new_sighting";
	EditText mBeeNumberET;
	EditText mBeeLocationDescriptionET;
	Button mSubmitReportButton;
	Button mThisUserReportsListButton;
	Button mThisUserReportsMapButton;
	Button mAllUserReportMapButton;

	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bee_sighting_report);

		checkLocationAvailable();    //Verify location is enabled, and this app has permission to use location services. Need to check this from an Activity (this is why it's not delegated to LocationUtils)
									// this app is not usable without GPS and permission, won't be able to submit bee sightings.
									//TODO Future version: if location is not available and/or permission not granted, user should be able to select location on map

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
				manageThisUserSightings();
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
				mapSightings(false);   /* onlyThisUser = false = show everyone */
			}
		});
	}


	private void manageThisUserSightings() {
		Intent thisUserSightings = new Intent(this, ManageSightingsActivity.class);
		startActivity(thisUserSightings);
	}

	private void mapSightings(boolean onlyThisUser) {
		Intent allSightings = new Intent(this, MapActivity.class);
		allSightings.putExtra(MapActivity.USER_SIGHTINGS_ONLY, onlyThisUser);
		startActivity(allSightings);
	}


	/* Read and validate data from user input fields.
	Request devices' location. Await location result */

	private void submitReport() {

		String beeLoc = mBeeLocationDescriptionET.getText().toString();
		String beeNumStr = mBeeNumberET.getText().toString();

		//Validate, if either failed show error and return.
		if (beeLoc.length() == 0 || !beeNumStr.matches("^\\d+$"))  {
			Toast.makeText(this, "Please enter both a number and description", Toast.LENGTH_LONG).show();
			return;
		}

		checkLocationAvailable();   //Just in case the user turned location off... This causes a Dialog to turn it on again

		//Location will be available (or not available) in callbacks.
		new LocationUtils().getLocation(this, this);

	}

	/* This is the callback from LocationUtils.getLocation() */

	@Override
	public void notifyLocationResult(Location location) {
		//Now can save bee sighting

		if (location == null) {

			//no location available - for example, because of poor GPS signal
			//TODO give user the opportunity to select location on map

			//This may be called from a background thread - the timeout thread in LocationUtils - and background threads are not allowed to modify the UI.
			//http://stackoverflow.com/questions/17379002/java-lang-runtimeexception-cant-create-handler-inside-thread-that-has-not-call

			//So to display a message, need to create a task to run on the UI thread, as follows,

			BeeSightingReportActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//Don't put anything time consuming in a runOnUIThread call, or start any other processes - this is suitable for quick messages or UI update tasks.
					Toast.makeText(BeeSightingReportActivity.this, "Device can't determine location", Toast.LENGTH_LONG).show();
				}
			});

		}

		else {

			//This code runs on the UI thread - called from the callback in LocationUtils, which belong to the UI thread.

			String beeLoc = mBeeLocationDescriptionET.getText().toString();
			int beeNum = Integer.parseInt(mBeeNumberET.getText().toString());

			BeeSighting currentSighting = new BeeSighting(beeNum, beeLoc);

			String userId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something is borked");
			Log.d(TAG, "userid from prefs = " +userId);
			currentSighting.setUserId(userId);

			currentSighting.setLocation(location);

			Log.d(TAG, "About to save this sighting: " + currentSighting);

			Firebase fb = new Firebase();
			fb.addSighting(currentSighting, this, ACTION_ADD_NEW);
			clearCurrentSightingInfo();
			Toast.makeText(this, "Sending report to Firebase...", Toast.LENGTH_LONG).show();
		}
	}


	//Clear the entry fields in the UI, ready for the next sighting to be entered
	private void clearCurrentSightingInfo() {
		mBeeLocationDescriptionET.getText().clear();
		mBeeNumberET.getText().clear();
	}



	/* This is the callback from Firebase - success or failure of submission */

	@Override
	public void onFirebaseComplete(String action, boolean success) {

		// This class only adds things to Firebase, so assume the action is add.
		// If there were other FB interactions and need to differentiate results, then check the value of Action to display appropriate message.
		if (success) {
			Toast.makeText(this, "Report received - thank you!", Toast.LENGTH_LONG).show();
		} else {
			//Failure - the user may have no internet connection, or it could be some error with the code.
			Toast.makeText(this, "An error occurred sending your report. Please check your internet connection?", Toast.LENGTH_LONG).show();

		}
	}


	/** The following methods handle checking if Location is enabled,
	 * and if this app has permission to use Location services? */

	private void checkLocationAvailable() {

		//If location is turned off on this device, send user to settings to turn Location on

		boolean locationEnabled = new LocationUtils().isLocationEnabled(this);
		boolean locationPermission = haveLocationPermission();

		Log.d(TAG, "location enabled?" + locationEnabled);
		Log.d(TAG, "location permission?" + locationPermission);


		if (!locationEnabled) {
			sendUserToSettings();

		}

		if (!locationPermission) {
			requestLocationPermission();
			return;
		}
	}

	//This is the callback for ActivityCompat.requestPermissions

	private boolean haveLocationPermission() {
		/*Starting in API 23, permission requesting is different. Make request 23-style using the compat library */
		return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
	}

	private void requestLocationPermission() {
		ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
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
				.setNegativeButton("DENY", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						Toast.makeText(BeeSightingReportActivity.this, "Location needs to be enabled to report sightings", Toast.LENGTH_SHORT).show();
					}
				}).create();

		dialog.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		//Arrays are empty if request denied.
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
				//yay
				new LocationUtils().getLocation(this, this);
			}

			else {
				//denied!
				//TODO allow user to indicate location on a map. Useful for reporting sightings after the fact.
				Toast.makeText(this, "Need location to report sightings", Toast.LENGTH_LONG).show();
			}

		}
	}

}
