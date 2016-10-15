package com.clara.beesightings;


import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class BeeSighting {

	double latitude;
	double longitude;
	int number;
	String locationDescription;
	Date sightingDate;
	String userId;

	static String pattern = "MMM d, yyyy";
	static SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());

	//For firebase
	public BeeSighting() {}

	public BeeSighting(double latitude, double longitude, int number, String locationDescription, Date sightingDate) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.number = number;
		this.locationDescription = locationDescription;
		this.sightingDate = sightingDate;
	}


	public BeeSighting(double latitude, double longitude, int number, String locationDescription) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.number = number;
		this.locationDescription = locationDescription;
		this.sightingDate = new Date();
	}


	public BeeSighting(int number, String locationDescription) {
		this.number = number;
		this.locationDescription = locationDescription;
		this.sightingDate = new Date();
	}

	@Override
	public String toString() {
		return "BeeSighting{" +
				"latitude=" + latitude +
				", longitude=" + longitude +
				", number=" + number +
				", locationDescription='" + locationDescription + '\'' +
				", sightingDate=" + sightingDate +
				", userId='" + userId + '\'' +
				'}';
	}

	/** These methods are used by app code */

	@Exclude
	protected void setLocation(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();
	}

	@Exclude
	protected LatLng getLatLng() {
		return new LatLng(latitude, longitude);
	}

	@Exclude
	protected String getMarkerText() {
		String formattedDate = formatter.format(sightingDate);
		return formattedDate + ": " + locationDescription;
	}


	public String getMarkerTitle() {
		String bees = (number == 1) ? " bee" : " bees";
		return number + bees;
	}

	@Exclude
	String firebaseKey;



	/** These are used by  firebase - the fields to put / fetch from the database **/

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}


	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getLocationDescription() {
		return locationDescription;
	}

	public void setLocationDescription(String locationDescription) {
		this.locationDescription = locationDescription;
	}

	public Date getSightingDate() {
		return sightingDate;
	}

	public void setSightingDate(Date sightingDate) {
		this.sightingDate = sightingDate;
	}

}
