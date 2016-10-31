package com.clara.beesightings.firebase;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



//@IgnoreExtraProperties
public class BeeSighting implements Parcelable {

	private double latitude;
	private double longitude;
	private int number;
	private String locationDescription;
	private Date sightingDate;
	private String userId;
	private String firebaseKey;


	private static String pattern = "MMM d, yyyy";
	private static SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());

	//This constructor is required for Firebase, empty constructor required
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


	//Constructor required for Parcelable interface
	//Read all the fields back in the same order as written
	public BeeSighting(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
		number = in.readInt();
		locationDescription = in.readString();
		userId = in.readString();
		firebaseKey = in.readString();
	}

	public static final Creator<BeeSighting> CREATOR = new Creator<BeeSighting>() {
		@Override
		public BeeSighting createFromParcel(Parcel in) {
			return new BeeSighting(in);
		}

		@Override
		public BeeSighting[] newArray(int size) {
			return new BeeSighting[size];
		}
	};

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
	public void setLocation(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();
	}

	@Exclude
	public LatLng getLatLng() {
		return new LatLng(latitude, longitude);
	}

	@Exclude
	public String getMarkerText() {
		return getFormattedDate() + ": " + locationDescription;
	}

	@Exclude
	public String getFormattedDate() {
		String formattedDate = formatter.format(sightingDate);
		return formattedDate;
	}

	@Exclude
	public String getMarkerTitle() {
		String bees = (number == 1) ? " bee" : " bees";
		return number + bees;
	}

	@Exclude
	public String getFirebaseKey() {
		return firebaseKey;
	}

	@Exclude
	public void setFirebaseKey(String firebaseKey) {
		this.firebaseKey = firebaseKey;
	}


	/** These are used by firebase - the fields to put / fetch from the database **/

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

	//Required for the Parcelable interface, so that BeeSighting objects can be put in Bundles

	// A really useful website for generating the Parcelable code for an particular class: http://www.parcelabler.com/

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		/* Fields to save...
	private double latitude;
	private double longitude;
	private int number;
	private String locationDescription;
	private Date sightingDate;
	private String userId;
	private String firebaseKey;

	*/
		parcel.writeDouble(latitude);
		parcel.writeDouble(longitude);
		parcel.writeInt(number);
		parcel.writeString(locationDescription);
		parcel.writeSerializable(sightingDate);
		parcel.writeString(userId);
		parcel.writeString(firebaseKey);
	}



}
