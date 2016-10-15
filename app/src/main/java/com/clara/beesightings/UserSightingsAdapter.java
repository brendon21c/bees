package com.clara.beesightings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by admin on 10/15/16.
 */

public class UserSightingsAdapter extends ArrayAdapter<BeeSighting> {

	private int layoutResource;
	private Context context;

	public UserSightingsAdapter(Context context, int resource) {
		super(context, resource);
		this.context = context;
		this.layoutResource = resource;
	}

	@Override
	public View getView(int postition, View convertView, ViewGroup parent) {

		BeeSighting sighting = getItem(postition);

		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(layoutResource, parent, false);
		}

		TextView listNumberTV = (TextView) convertView.findViewById(R.id.list_number);
		TextView listDescriptionTV = (TextView) convertView.findViewById(R.id.list_description);
		TextView listDateTV = (TextView) convertView.findViewById(R.id.list_date);

		listNumberTV.setText(sighting.getNumber() + "");
		listDescriptionTV.setText(sighting.getLocationDescription());
		listDateTV.setText(sighting.getFormattedDate());

		return convertView;

	}


}
