package com.clara.beesightings;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**Handle Firebase interaction*/


//TODO be able to remove listeners


public class Firebase {

	FirebaseDatabase database;

	private final String TAG = "FIREBASE INTERACTION";

	Firebase() {
		database = FirebaseDatabase.getInstance();

	}

	void addSighting(BeeSighting bee) {

		DatabaseReference ref = database.getReference();
		DatabaseReference newChild = ref.push();
		newChild.setValue(bee);

	}



	Query mostRecentQuery;
	ValueEventListener mostRecentListener;

	public void getMostRecentSightings(final SightingsUpdatedListener listener, int number) {

		//TODO if this is already set up, it will replace the SightingsUpdatedListener with a new one.

		mostRecentQuery = database.getReference().orderByKey().limitToLast(number);
		mostRecentListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				ArrayList<BeeSighting> list = new ArrayList<>();

				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					BeeSighting sighting = (ds.getValue(BeeSighting.class));
					sighting.firebaseKey = ds.getKey();
					list.add(sighting);
				}
				listener.sightingsUpdated(list);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "get most recent sightings, onCancelled", databaseError.toException());

			}
		};

		mostRecentQuery.addValueEventListener(mostRecentListener);

	}

	public void removeMostRecentListener() {
		if (mostRecentQuery != null) {
			mostRecentQuery.removeEventListener(mostRecentListener);
		}
	}


	public void getUserSightings(final SightingsUpdatedListener listener, String userId) {

		Query query = database.getReference().equalTo(userId);
		query.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				ArrayList<BeeSighting> list = new ArrayList<>();
				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					BeeSighting sighting = (ds.getValue(BeeSighting.class));
					sighting.firebaseKey = ds.getKey();
					list.add(sighting);
				}
				listener.sightingsUpdated(list);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "get user sightings, onCancelled", databaseError.toException());
			}
		});

	}

	public void updateSighting(String key, int number, String description) {
		//TODO!
		Log.d(TAG, "Update sighting " + key + " " + number + " " + description);
	}


	public void deleteSighting(String key) {
		//TODO!
		Log.d(TAG, "Deleting key " + key);

		database.getReference().child(key).removeValue();   // ?

	}


	public interface SightingsUpdatedListener {
		void sightingsUpdated(ArrayList<BeeSighting> s);
	}
}
