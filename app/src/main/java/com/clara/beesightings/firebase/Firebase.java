package com.clara.beesightings.firebase;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/** Handle Firebase interaction */


//TODO be able to remove listeners


public class Firebase {


	public interface CompleteListener {
		void onFirebaseComplete(String actionTag, boolean success);
	}


	public interface SightingsUpdatedListener {
		void sightingsUpdated(ArrayList<BeeSighting> s);
	}


	FirebaseDatabase database;

	Query mostRecentQuery;
	ValueEventListener mostRecentListener;

	private final String TAG = "FIREBASE INTERACTION";


	public Firebase() {
		database = FirebaseDatabase.getInstance();
	}


	public void addSighting(BeeSighting bee, final CompleteListener listener, final String tag) {

		DatabaseReference ref = database.getReference();
		DatabaseReference newChild = ref.push();
		newChild.setValue(bee, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				notifyListener(databaseError, listener, tag);
			}
		});

	}

	public void updateSighting(BeeSighting sighting, final CompleteListener listener, final String tag) {

		Log.d(TAG, "Update sighting " + sighting);
		DatabaseReference ref = database.getReference().child(sighting.getFirebaseKey());
		Log.d(TAG, "in update, reference to update is " + ref);
		ref.setValue(sighting, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				notifyListener(databaseError, listener, tag);
			}
		});
	}


	public void deleteSighting(BeeSighting sighting, final CompleteListener listener, final String tag) {

		Log.d(TAG, "Deleting item" + sighting);
		database.getReference().child(sighting.getFirebaseKey()).removeValue(new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				notifyListener(databaseError, listener, tag);
			}
		});

	}


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



	private void notifyListener(DatabaseError databaseError, CompleteListener listener,  String tag) {

		//If there's a listener, notify it of success or error
		if (listener != null) {

			if (databaseError == null) {
				//Success
				Log.d(TAG, "completed successfully");
				listener.onFirebaseComplete(tag, true);
			}

			else {
				//error :(
				Log.e(TAG, "failed", databaseError.toException());
				listener.onFirebaseComplete(tag, false);

			}
		}
	}


}
