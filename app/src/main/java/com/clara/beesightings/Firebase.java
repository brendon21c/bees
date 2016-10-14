package com.clara.beesightings;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**Handle Firebase interaction*/

public class Firebase {

	FirebaseDatabase database;

	Firebase() {
		database = FirebaseDatabase.getInstance();

	}

	void addSighting(BeeSighting bee) {

		DatabaseReference ref = database.getReference();
		DatabaseReference newChild = ref.push();
		newChild.setValue(bee);

	}

	public void getMostRecentSightings(final SightingsUpdatedListener listener, int number) {

		Query query = database.getReference().orderByKey().limitToLast(number);

		query.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				ArrayList<BeeSighting> s = new ArrayList<>();
				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					s.add(ds.getValue(BeeSighting.class));
				}
				listener.sightingsUpdated(s);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});

	}


	public interface SightingsUpdatedListener {
		void sightingsUpdated(ArrayList<BeeSighting> s);
	}
}
