package com.clara.beesightings.firebase;

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

	public Firebase() {
		database = FirebaseDatabase.getInstance();

	}

	public void addSighting(BeeSighting bee) {

		DatabaseReference ref = database.getReference();
		DatabaseReference newChild = ref.push();
		newChild.setValue(bee);

	}

	Query mostRecentQuery;
	ValueEventListener mostRecentListener;

	public void getMostRecentSightings(final SightingsUpdatedListener listener, int number) {

		//TODO if this is already set up, it will replace the SightingsUpdatedListener with a new one.

		mostRecentQuery = database.getReference().limitToFirst(number);

		mostRecentListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				ArrayList<BeeSighting> list = new ArrayList<>();

				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					BeeSighting sighting = (ds.getValue(BeeSighting.class));
					sighting.setFirebaseKey(ds.getKey());
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

		Log.d(TAG, "Querying for sightings for this userid : " + userId);

		//Query starts with an orderByXXX method and then (optionally) use startAt, equalTo, endAt to filter.

		Query query = database.getReference().orderByChild("userId").equalTo(userId);

		query.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				ArrayList<BeeSighting> list = new ArrayList<>();
				Log.d(TAG, "All data:" + dataSnapshot);
				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					BeeSighting sighting = (ds.getValue(BeeSighting.class));
					sighting.setFirebaseKey(ds.getKey());
					list.add(sighting);
				}

				Log.d(TAG, "Results for this user : " + list);

				listener.sightingsUpdated(list);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "get user sightings, onCancelled", databaseError.toException());
			}
		});

	}

	public void updateSighting(BeeSighting sighting) {

		Log.d(TAG, "Update sighting " + sighting);
		DatabaseReference ref = database.getReference().child(sighting.getFirebaseKey());
		Log.d(TAG, "in update, reference to update is " + ref);
		ref.setValue(sighting);
	}


	public void deleteSighting(BeeSighting sighting) {

		Log.d(TAG, "Deleting item" + sighting);
		database.getReference().child(sighting.getFirebaseKey()).removeValue();

	}


	public interface SightingsUpdatedListener {
		void sightingsUpdated(ArrayList<BeeSighting> s);
	}
}
