package com.clara.beesightings;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.clara.beesightings.firebase.BeeSighting;
import com.clara.beesightings.firebase.Firebase;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;


/** Show a map with markers for bee reports. Markers are color coded for this user, another color for everyone one else. */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, Firebase.SightingsUpdatedListener{

	private TextView mMapLabel;
	private int maxNumberOfSightings = 100;

	private float mUserColor = BitmapDescriptorFactory.HUE_BLUE;
	private float mEveryoneElseColor = BitmapDescriptorFactory.HUE_YELLOW;   //Change to whatever colors are preferred

	private String mUserId;

	private boolean mapReady = false;    //Can take a little time to get the map loaded and ready. Keep track of when it is ready, so know when can draw markers on it.

	private GoogleMap map;

	private ArrayList<BeeSighting> sightingsAwaitingMapReady;  //If Firebase provides data before map is ready, store them here while awaiting map.

	private boolean updateCurrentMap = false;    //When updates happen, should the whole map be redrawn in the inital position?

	static final String USER_SIGHTINGS_ONLY = "com.clara.beesightings.user_sightings_only";
	private boolean mUserSightingsOnly = false;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapLabel = (TextView) findViewById(R.id.map_label);
		mMapLabel.setText(R.string.loading_map_msg);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);     //Gets map ready in background. onMapReady is called once done

		Firebase fb = new Firebase();

		//Show the user's sightings, or latest from everyone - which may include some of users ?

		//Need the user's ID to color-code the markers
		mUserId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something has gone wrong here");
		mUserSightingsOnly = getIntent().getExtras().getBoolean(USER_SIGHTINGS_ONLY);

		// Request sightings from firebase. This also happens asynchronously and sightingsUpdated
		// method will be called when they are available. sightingsUpdated method is also called if Firebase data is updated
		if (mUserSightingsOnly) {
			fb.getUserSightings(this, mUserId);
		}
		else {
			fb.getMostRecentSightings(this, maxNumberOfSightings);   // can't get everything... request the most recent
		}
	}


	@Override
	public void onMapReady(GoogleMap googleMap) {

		//Do map setup.

		mapReady = true;
		map = googleMap;
		map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);  // Roads and contours

		//Typically, you'll add map markers here. But for this app, the data to create the markers comes from Firebase, and
		//they may not be available. If there is data from firebase, it will be stored in the sightingsAgaitingMapReady ArrayList.
		//So if there are sightings already available, call sightingsUpdated to draw them on the map.
		if (sightingsAwaitingMapReady != null) {
			sightingsUpdated(sightingsAwaitingMapReady);
			sightingsAwaitingMapReady = null;
		}

	}


	public void sightingsUpdated(ArrayList<BeeSighting> sightings)  {

		//Draw markers on map if map is ready AND Firebase data is available

		if (mapReady && map != null) {

			//Make a note of the camera position....
			CameraPosition cp = map.getCameraPosition();
			//remove any old markers. This also resets the camera to initial position, which is why the CameraPosition is saved.
			map.clear();

			if (!updateCurrentMap) {
				//Move camera to most recent sighting
				BeeSighting mostRecent = sightings.get(sightings.size() - 1);
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mostRecent.getLatLng(), map.getMinZoomLevel());
				map.moveCamera(update);

			} else {
				// restore camera to the position it was at
				map.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
			}


			//Add markers
			for (BeeSighting s : sightings) {

				//Set color to this user color, or everyone else color.
				float color = s.getUserId().equals(mUserId) ? mUserColor : mEveryoneElseColor;

				LatLng position = s.getLatLng();
				String markerTitle = s.getMarkerTitle();
				String markerText = s.getMarkerText();

				//See docs for more marker options.
				map.addMarker(new MarkerOptions()
						.position(position)
						.title(markerTitle)
						.snippet(markerText)
						.icon(BitmapDescriptorFactory.defaultMarker(color))
				);
			}

			//Set label
			if (sightings.size() >= 1) {
				String title = String.format(getString(R.string.showing_d_sightings), sightings.size());
				mMapLabel.setText(title);
			}

			else {
				mMapLabel.setText(R.string.no_sightings_found);
			}

			//Once the map has been drawn once, don't move to initial zoom level
			updateCurrentMap = true;


		}

		//Else, the map isn't ready. Save the sightings in sightingAwaitingMapReady. When the map is ready, it will check to see if
		//there are any sightings waiting to be drawn, and then call this method again to draw the markers.

		else {
			//Save these sightings until map is ready
			sightingsAwaitingMapReady = sightings;
		}

	}

}
