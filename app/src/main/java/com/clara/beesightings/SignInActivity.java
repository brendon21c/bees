package com.clara.beesightings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/** If the user is not signed in, they will see this Activity first.
 * If the user is authenticated, this Activity will verify and then
 * open the app's main Activity, no sign-in needed */

public class SignInActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

	private static final int REQUEST_CODE_SIGN_IN = 12345;
	protected static final String FIREBASE_USER_ID_PREF_KEY = "Firebase user id";
	protected static final String USERS_PREFS = "User_preferences";
	private GoogleApiClient mGoogleApiClient;
	private FirebaseAuth mFirebaseAuth;
	private FirebaseAuth.AuthStateListener mAuthStateListener;

	private static final String TAG = "SIGN IN ACTIVITY";


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);

		//Two steps. User signs in with Google, and then exchange Google token for Firebase token
		mFirebaseAuth = FirebaseAuth.getInstance();

		//Use Google Sign In to request the user data required by this app. Let's request basic data, the default.
		//plus the user's email, although we aren't going to use it here.
		//If other info was needed, you'd chain on methods like requestProfile() before building.
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestEmail()
				.requestIdToken(getString(R.string.default_web_client_id))   //This String seems to exist already - did Firebase create it?
				.build();

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.enableAutoManage(this /* A FragmentActivity */, this /* An OnConnectionFailedListener */)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();

		mAuthStateListener = new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
				Log.d(TAG, "onAuthStateChanged for user: " + firebaseAuth.getCurrentUser());
				FirebaseUser user = firebaseAuth.getCurrentUser();
				SignInActivity.this.authStateChanged(user);
			}
		};

		SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
		signInButton.setOnClickListener(this);


	}


	@Override
	public void onClick(View view) {

		if (view.getId() == R.id.sign_in_button) {
			signIn();
		}
	}


	//This launches an Activity where the user can sign into their Google account, or even create a new account.
	private void signIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
	}


	//This deals with the result from the above method. Pass results to handleSignIn method.

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_SIGN_IN) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			handleSignIn(result);
		}

	}


	// This deals with the user signing into their Google account. This checks for success or failure.
	// If success, credentials will be returned, that can be used to sign into Firebase on the user's behalf.
	private void handleSignIn(GoogleSignInResult result) {
		Log.d(TAG, "handleSignIn for result " + result.getSignInAccount());
		if (result.isSuccess()) {
			//yay. Now need to use these credentials to authenticate to FireBase.
			Log.d(TAG, "Google sign in success");
			GoogleSignInAccount account = result.getSignInAccount();
			firebaseAuthWithGoogleCreds(account);
		} else {
			Log.e(TAG, "Google sign in failed");
			//This will fail if user has no internet connection OR your haven't enabled Google auth in Firebase console
			//and probably other reasons too. Check the log for the error message.
			Toast.makeText(this, "Google sign in failed", Toast.LENGTH_LONG).show();
		}
	}


	// This uses the credentials returned from a successful sign in to a Google account to authenticate to Firebase
	// Notice that this method doesn't do anything else with the results of Authentication - that's handled by an AuthStateListener, below
	private void firebaseAuthWithGoogleCreds(GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
		Log.d(TAG, "firebase auth attempt with creds " + credential);

		//Attempt to sign in to Firebase with the google credentials. The onCompleteListener is used for logging success or failure.
		mFirebaseAuth.signInWithCredential(credential)
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							Log.d(TAG, "firebase auth success");
						} else {
							Log.d(TAG, "firebase auth fail");
						}
					}
				});
	}



	//Add and remove listeners for auth state as this activity stops and starts
	//The mAuthStateChangedListener is what will permit the user to continue with the app once authenticated.

	@Override
	public void onStart(){
		super.onStart();
		mFirebaseAuth.addAuthStateListener(mAuthStateListener);
	}

	@Override
	public void onStop() {
		super.onStop();
		mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
	}


	// This method is called if user signs in or signs out. It is also called when the app is launched.
	// So if the user has already authenticated, and their session has not timed out, they will not be
	// prompted to authenticate again. Instead, this will launch BeeSightingsReportActivity.

	private void authStateChanged(FirebaseUser user) {
		if (user == null) {
			Log.d(TAG, "user is signed out");
		} else {
			Log.d(TAG, "user has signed in");

			//Save the user id in shared prefs
			Log.d(TAG, "The user id is = " + user.getUid() + " " +user.toString());

			SharedPreferences.Editor prefEditor = getSharedPreferences(USERS_PREFS, MODE_PRIVATE).edit();
			prefEditor.putString(FIREBASE_USER_ID_PREF_KEY, user.getUid());
			prefEditor.apply();

			//And then boot up the app by starting the BeeSightingsReportActivity
			Intent startBeeSightings = new Intent(this, BeeSightingReportActivity.class);
			startActivity(startBeeSightings);
		}
	}



	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		//todo - handle this in a more useful way for the user
		Toast.makeText(this, "Connection to Google failed", Toast.LENGTH_LONG).show();
		Log.d(TAG, "CONNECTION FAILED" + connectionResult.getErrorMessage() + connectionResult.getErrorCode());

	}
}
