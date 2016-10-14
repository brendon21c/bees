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
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiActivity;
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
		//plus the user's email, although we aren't going to use it (it makes differentiating users easier in the firebase console
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

	private void authStateChanged(FirebaseUser user) {
		//This method is called if user signs in or signs out
		if (user == null) {
			Log.d(TAG, "user is signed out");
			//Toast.makeText(this, "Firebase: User signed OUT", Toast.LENGTH_LONG).show();
		} else {
			Log.d(TAG, "user has signed in");
			//Toast.makeText(this, "Firebase: User signed IN", Toast.LENGTH_LONG).show();
			//boot up the app

			//Save the user id in shared prefs

			Log.d(TAG, "The user id is = " + user.getUid() + " " +user.toString());

			SharedPreferences.Editor prefEditor = getSharedPreferences(USERS_PREFS, MODE_PRIVATE).edit();
			prefEditor.putString(FIREBASE_USER_ID_PREF_KEY, user.getUid());
			prefEditor.apply();

			Intent startBeeSightings = new Intent(this, BeeSightingReportActivity.class);
			startActivity(startBeeSightings);
		}
	}

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


	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		//TODO
		Log.d(TAG, "CONNECTION FAILED");

	}

	@Override
	public void onClick(View view) {

		if (view.getId() == R.id.sign_in_button) {
			signIn();
		}
	}

	private void signIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_SIGN_IN) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			handleSignIn(result);
		}

	}

	private void handleSignIn(GoogleSignInResult result) {
		Log.d(TAG, "handleSignIn for result " + result.getSignInAccount());
		if (result.isSuccess()) {
			//yay. Now need to use these credentials to authenticate to FireBase.
			Log.d(TAG, "Google sign in success");
			GoogleSignInAccount account = result.getSignInAccount();
			firebaseAuthWithGoogleCreds(account);
		} else {
			Log.e(TAG, "Google sign in failed");
			//This will fail if user has no internet connection
			Toast.makeText(this, "Google sign in failed - check your internet connection?", Toast.LENGTH_LONG).show();
		}
	}


	private void firebaseAuthWithGoogleCreds(GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
		Log.d(TAG, "firebase auth attempt with creds " + credential);

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
}
