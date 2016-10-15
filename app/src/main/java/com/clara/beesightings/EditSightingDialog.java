package com.clara.beesightings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by admin on 10/14/16.
 */

public class EditSightingDialog extends DialogFragment {

	private static final String NUMBER = "number";
	private static final String DESCRIPTION = "description";
	private static final String POSITION = "list_position";


	public static EditSightingDialog newInstance(BeeSighting toEdit, int listPosition) {
		EditSightingDialog dialog = new EditSightingDialog();
		Bundle args = new Bundle();
		args.putInt(NUMBER, toEdit.getNumber());
		args.putInt(POSITION, listPosition);
		args.putString(DESCRIPTION, toEdit.getLocationDescription());

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
		void sightingUpdated(int position, int number, String description);
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final String description = getArguments().getString(DESCRIPTION, null);
		int number = getArguments().getInt(NUMBER, -1);
		final int position = getArguments().getInt(POSITION, -1);

		//TODO verify these above have values

		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.edit_sighting, null);

		final EditText numberET = (EditText) view.findViewById(R.id.edit_dialog_number);
		final EditText descriptionET = (EditText) view.findViewById(R.id.edit_dialog_description);

		numberET.setText(number + "");   //Hack to convert number to string. Otherwise the int is assumed to be thie ID of a resource
		descriptionET.setText(description);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setView(view)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

						String newDescription = descriptionET.getText().toString();
						try {
							int newNumber = Integer.parseInt(numberET.getText().toString().trim());
							mListener.sightingUpdated(position, newNumber, newDescription);
						} catch (NumberFormatException e) {
							Toast.makeText(getActivity(), "Enter a number", Toast.LENGTH_LONG).show();
						}
					}
				});

		return builder.create();

	}

}
