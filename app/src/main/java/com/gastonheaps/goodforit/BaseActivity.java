package com.gastonheaps.goodforit;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.gastonheaps.goodforit.ui.ProgressDialogFragment;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * BaseActivity class is used as a base class for all activities in the app
 * It implements GoogleApiClient callbacks to enable "Logout" in all activities
 * and defines variables that are being shared across all activities
 */
public class BaseActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    protected FirebaseAuth mAuth;
    protected FirebaseAuth.AuthStateListener mAuthListener;
    protected DatabaseReference mFirebaseRef;

    protected GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Setup the Google API object to allow Google logins */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        /**
         * Build a GoogleApiClient with access to the Google Sign-In API and the
         * options specified by gso.
         */

        /* Setup the Google API object to allow Google+ logins */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        /**
         * Getting mProvider and mEncodedEmail from SharedPreferences
         */
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(BaseActivity.this);
        /* Get mEncodedEmail and mProvider from SharedPreferences, use null as default value */
        mEncodedEmail = sp.getString(Constants.KEY_ENCODED_EMAIL, null);
        mProvider = sp.getString(Constants.KEY_PROVIDER, null);


        if (!((this instanceof LoginActivity) || (this instanceof CreateAccountActivity))) {
            mFirebaseRef = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        // User is signed in
                        Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    } else {
                        // User is signed out
                        Log.d(TAG, "onAuthStateChanged:signed_out");
                    }
                    // [START_EXCLUDE]
                    updateUI(user);
                    // [END_EXCLUDE]
                }
            };
            mFirebaseRef.au
        }

        @Override
        public void onDestroy () {
            super.onDestroy();
        /* Cleanup the AuthStateListener */
            if (!((this instanceof LoginActivity) || (this instanceof CreateAccountActivity))) {
                mFirebaseRef.removeAuthStateListener(mAuthListener);
            }

        }

        @Override
        public void onSaveInstanceState (Bundle outState){
            super.onSaveInstanceState(outState);
        }

        @Override
        public boolean onCreateOptionsMenu (Menu menu){
        /* Inflate the menu; this adds items to the action bar if it is present. */
            getMenuInflater().inflate(R.menu.menu_base, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            int id = item.getItemId();

            if (id == android.R.id.home) {
                super.onBackPressed();
                return true;
            }

            if (id == R.id.action_logout) {
                logout();
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        protected void initializeBackground (LinearLayout linearLayout){

            /**
             * Set different background image for landscape and portrait layouts
             */
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                linearLayout.setBackgroundResource(R.drawable.background_loginscreen_land);
            } else {
                linearLayout.setBackgroundResource(R.drawable.background_loginscreen);
            }
        }

        /**
         * Logs out the user from their current session and starts LoginActivity.
         * Also disconnects the mGoogleApiClient if connected and provider is Google
         */
        protected void logout () {

        /* Logout if mProvider is not null */
            if (mProvider != null) {
                mFirebaseRef.unauth();

                if (mProvider.equals(Constants.GOOGLE_PROVIDER)) {

                /* Logout from Google+ */
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    //nothing
                                }
                            });
                }
            }
        }

        private void takeUserToLoginScreenOnUnAuth () {
        /* Move user to LoginActivity, and remove the backstack */
            Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        @Override
        public void onConnectionFailed (ConnectionResult connectionResult){
        }
    }