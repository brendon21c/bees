package com.clara.beesightings;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;


/** Todo show a map with markers for bee reports  - add flag for this user OR for latests */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, Firebase.SightingsUpdatedListener{

	private TextView mMapLabel;
	private int numberOfSightings = 100;
	private String mMapTitle;

	float mUserColor = BitmapDescriptorFactory.HUE_BLUE;
	float mEveryoneElseColor = BitmapDescriptorFactory.HUE_YELLOW;   //Change to whatever colors are preferred

	String mUserId;

	static final String USER_SIGHTINGS_ONLY = "com.clara.beesightings.user_sightings_only";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapLabel = (TextView) findViewById(R.id.map_label);
		mMapLabel.setText("Loading map...");

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);     //onMapReady callback once done

		Firebase fb = new Firebase();

		//Show the user's sightings, or everyone's ?

		//Need the user's ID to color-code the markers
		mUserId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something has gone wrong here");
		fb.getUserSightings(this, mUserId);


		if (getIntent().getExtras().getBoolean(USER_SIGHTINGS_ONLY)) {
			mMapTitle = "Showing your reported sightings";
		}
		else {

			fb.getMostRecentSightings(this, numberOfSightings);   // can't get everything... request the most recent
			mMapTitle = "Showing the " + numberOfSightings + " most recent sightings";
			//FIXME what if we request 100 most recent but only 10 sightings in DB? This message is wrong
		}


	}

	boolean mapReady = false;

	GoogleMap map;

	ArrayList<BeeSighting> sightingsAwaitingMapReady;

	@Override
	public void onMapReady(GoogleMap googleMap) {

		//We may need to wait for Firebase before markers can be added.
		mapReady = true;
		map = googleMap;
		map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);  // Roads and contours

		if (sightingsAwaitingMapReady != null) {
			sightingsUpdated(sightingsAwaitingMapReady);
			sightingsAwaitingMapReady = null;
		}

	}


	boolean updateCurrentMap = false;

	public void sightingsUpdated(ArrayList<BeeSighting> sightings)  {

		//update map once map is ready and Firebase data is available

		if (mapReady && map != null) {

			//Make a note of the camera position....
			CameraPosition cp = map.getCameraPosition();
			//remove any old markers. This also resets the camera to initial position TODO is there a method that clears markers without moving the camera?
			map.clear();

			if (!updateCurrentMap) {
				//Move camera to most recent sighting
				BeeSighting mostRecent = sightings.get(sightings.size() - 1);
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mostRecent.getLatLng(), map.getMinZoomLevel());
				map.moveCamera(update);

			} else {
				// move camera to the position it was at
				map.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
			}


			//Add markers
			for (BeeSighting s : sightings) {

				float color = s.getUserId().equals(mUserId) ? mUserColor : mEveryoneElseColor;

				LatLng position = s.getLatLng();
				String markerTitle = s.getMarkerTitle();
				String markerText = s.getMarkerText();
				map.addMarker(new MarkerOptions()
						.position(position)
						.title(markerTitle)
						.snippet(markerText)
						.icon(BitmapDescriptorFactory.defaultMarker(color))
				);
			}

			//Set label
			if (sightings.size() >= 1) {
				mMapLabel.setText(mMapTitle);
			}

			else {
				mMapLabel.setText("No sightings found");
			}

			//Once the map has been drawn once, don't move to initial zoom level
			updateCurrentMap = true;


		} else {
			//Save these sightings until map is ready
			sightingsAwaitingMapReady = sightings;
		}

	}




}
