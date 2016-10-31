package com.clara.beesightings;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.clara.beesightings.firebase.BeeSighting;
import com.clara.beesightings.firebase.Firebase;

import java.util.ArrayList;


/** A list of user's sightings, and ability to modify or delete
 * A local cache is maintained so even if user has no network connection, changes appear to be made, and they will be
 * synced with the Firebase server once connectivity is restored. */

public class ManageSightingsActivity extends AppCompatActivity implements Firebase.SightingsUpdatedListener, EditSightingDialog.SightingUpdatedListener, Firebase.CompleteListener {

	private static final String DELETE_SIGHTING = "manage_sightings_delete_sighting";
	private static final String UPDATE_SIGHTING = "manage_sightings_update_sighting";
	ListView mSightingList;
	TextView mInstructions;
	UserSightingsAdapter mAdapter;

	Firebase firebase;

	private static String TAG = "MANAGE SIGHTINGS";

	private final String DELETE = "delete";
	private final String UPDATE = "update";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_sightings);

		//Display loading message
		mInstructions = (TextView) findViewById(R.id.instructions_tv);
		mInstructions.setText(R.string.loading_sightings_msg);

		mSightingList = (ListView) findViewById(R.id.user_list);
		mAdapter = new UserSightingsAdapter(this, R.layout.user_sightings_list_element);
		mSightingList.setAdapter(mAdapter);

		firebase = new Firebase();

		//get userID
		String userId = getSharedPreferences(SignInActivity.USERS_PREFS, MODE_PRIVATE).getString(SignInActivity.FIREBASE_USER_ID_PREF_KEY, "something has gone wrong here");

		//Request this user's reported sightings
		firebase.getUserSightings(this, userId);

		addListListeners();

	}


	@Override
	public void sightingsUpdated(ArrayList<BeeSighting> s) {

		Log.d(TAG, s.toString());

		mAdapter.clear();
		mAdapter.addAll(s);
		mAdapter.notifyDataSetChanged();

		//Change TextView to instructions
		mInstructions.setText(R.string.tap_to_edit_long_press_to_delete);

	}


	private void addListListeners() {

		//Tap to edit....

		mSightingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

				//Display dialog with ability to edit number of bees and description
				BeeSighting toEdit = mAdapter.getItem(i);
				EditSightingDialog dialog = EditSightingDialog.newInstance(toEdit);
				dialog.show(getSupportFragmentManager(), "Edit Sighting Dialog");
				// The callback - sightingUpdated() sends the update to Firebase.
			}
		});


		// Long-press to delete

		mSightingList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

				final BeeSighting toDelete = mAdapter.getItem(i);

				AlertDialog.Builder builder = new AlertDialog.Builder(ManageSightingsActivity.this);
				builder.setMessage("Delete this sighting?")
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								//delete item
								//So this will cause a value event, which causes Firebase to call sightings updated, which will update the list.

								Log.d(TAG, "Deleting " + toDelete);
								//Provide a reference to this class to get a callback once the sighting has been updated, and a tag to differentiate different requests in the callback onFirebaseComplete
								firebase.deleteSighting(toDelete, ManageSightingsActivity.this, DELETE_SIGHTING);
								Toast.makeText(ManageSightingsActivity.this, "Sighting being deleted", Toast.LENGTH_LONG).show();

							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.create()
						.show();

				return false;
			}
		});

	}


	//Update Dialog callback

	@Override
	public void sightingUpdated(BeeSighting updated) {

		Log.d(TAG, "Sighting updated callback, updated sighting is " + updated);
		Toast.makeText(this, "Uploading sighting update...", Toast.LENGTH_LONG).show();

		//Provide a reference to this class to get a callback once the sighting has been updated, and a tag to differentiate different requests in the callback onFirebaseComplete
		firebase.updateSighting(updated, this, UPDATE_SIGHTING);

	}


	//Firebase success/failure callback

	@Override
	public void onFirebaseComplete(String actionTag, boolean success) {

		//If you wanted to do something on success or failure, this would be a good place.

		String actionMsg = "";

		switch (actionTag) {
			case UPDATE_SIGHTING: { actionMsg = "updated"; break; }
			case DELETE_SIGHTING: { actionMsg = "deleted"; break; }
		}

		String result = (success) ? "was successful" : "failed (check your internet connection?)";

		String userMsg = "Sighting " + actionMsg + " " + result;

		Toast.makeText(this, userMsg, Toast.LENGTH_LONG).show();

	}
}
