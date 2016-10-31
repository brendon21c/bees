package com.clara.beesightings;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.clara.beesightings.firebase.BeeSighting;

/* For editing a user's sighting report */

public class EditSightingDialog extends DialogFragment {

	private static final String SIGHTING = "bee_sighting";


	// newInstance - sets up the current values for a sighting.
	// Also provide the listPosition for convenience reporting which item has been edited.
	public static EditSightingDialog newInstance(BeeSighting toEdit) {
		EditSightingDialog dialog = new EditSightingDialog();
		Bundle args = new Bundle();
		args.putParcelable(SIGHTING, toEdit);

		dialog.setArguments(args);
		return dialog;
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof SightingUpdatedListener) {
			mListener = (SightingUpdatedListener) activity;
		} else {
			throw new RuntimeException("To show this dialog, the hosting Activity must implement SightingUpdatedListener");
		}
	}

	private SightingUpdatedListener mListener;

	interface SightingUpdatedListener {
		void sightingUpdated(BeeSighting updated);
	}


	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {

		final BeeSighting sighting = getArguments().getParcelable(SIGHTING);

		final String description = sighting.getLocationDescription();
		final int number = sighting.getNumber();

		//TODO verify the above have values

		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.edit_sighting, null);

		final EditText numberET = (EditText) view.findViewById(R.id.edit_dialog_number);
		final EditText descriptionET = (EditText) view.findViewById(R.id.edit_dialog_description);

		numberET.setText(Integer.toString(number));
		descriptionET.setText(description);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setView(view)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

						String newDescription = descriptionET.getText().toString();
						try {
							//Validate that the data is an Integer number
							int newNumber = Integer.parseInt(numberET.getText().toString().trim());

							sighting.setLocationDescription(newDescription);
							sighting.setNumber(newNumber);

							mListener.sightingUpdated(sighting);

						} catch (NumberFormatException e) {

							Toast.makeText(getActivity(), "Enter a number", Toast.LENGTH_LONG).show();
						}
					}
				});

		return builder.create();

	}

}
