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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;


/** Todo show a map with markers for bee reports  - add flag for this user OR for latests */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, Firebase.SightingsUpdatedListener{

	private TextView mMapLabel;
	private int numberOfSightings = 100;
	private String mMapTitle;

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

		if (getIntent().getExtras().getBoolean(USER_SIGHTINGS_ONLY)) {

			String userId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something has gone wrong here");
			fb.getUserSightings(this, userId);
			mMapTitle = "Showing your reported sightings";

		}
		else {

			fb.getMostRecentSightings(this, numberOfSightings);   // can't get everything... request the most recent
			mMapTitle = "Showing the " + numberOfSightings + " most recent sightings";

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


	public void sightingsUpdated(ArrayList<BeeSighting> sightings)  {

		//update map once map is ready and Firebase data is available

		if (mapReady && map != null) {

			for (BeeSighting s : sightings) {
				LatLng position = s.getLatLng();
				String markerTitle = s.getMarkerTitle();
				String markerText = s.getMarkerText();
				map.addMarker(new MarkerOptions()
						.position(position)
						.title(markerTitle)
						.snippet(markerText)
				);
			}

			//Set the camera to center on the most recent sighting. Zoom out so map shows most of the world.

			if (sightings.size() >= 1) {
				BeeSighting mostRecent = sightings.get(sightings.size()-1);
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(mostRecent.getLatLng(), map.getMinZoomLevel());
				map.moveCamera(update);
				mMapLabel.setText(mMapTitle);
			}

			else {
				mMapLabel.setText("No sightings found");
			}


		} else {
			//Save these sightings until map is ready
			sightingsAwaitingMapReady = sightings;
		}

	}


}
