/****************************************************************************
Copyright (c) 2008-2010 Ricardo Quesada
Copyright (c) 2010-2012 cocos2d-x.org
Copyright (c) 2011      Zynga Inc.
Copyright (c) 2013-2014 Chukong Technologies Inc.
 
http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
****************************************************************************/
package org.cocos2dx.cpp;

import org.cocos2dx.GPforAndroid.R;
import org.cocos2dx.lib.Cocos2dxActivity;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.Plus.PlusOptions;
import com.google.android.gms.plus.model.people.Person;
 

public class AppActivity extends Cocos2dxActivity implements OnClickListener,
ConnectionCallbacks, OnConnectionFailedListener
{
	   private static final int PICK_MEDIA_REQUEST_CODE = 8;
	   private static final int SHARE_MEDIA_REQUEST_CODE = 9;
	   private static final int SIGN_IN_REQUEST_CODE = 10;
	   private static final int ERROR_DIALOG_REQUEST_CODE = 11;
	 
	   // layout for showing user control buttons
	   private LinearLayout userOptionsLayout;
	   // layout for showing signed in user info
	   private LinearLayout userInfoLayout;
	 
	   private ImageView userProfilePic;
	   private TextView userName;
	   private TextView userEmail;
	   private TextView userLocation;
	   private TextView userTagLine;
	   private TextView userAboutMe;
	   private TextView userBirthday;
	 
	   Button signOutButton;
	   Button userInfoButton;
	   Button sharePostButton;
	   Button shareMediaButton;
	   Button revokeAccessButton;
	 
	   // For communicating with Google APIs
	   private GoogleApiClient mGoogleApiClient;
	   private boolean mSignInClicked;
	   private boolean mIntentInProgress;
	   // contains all possible error codes for when a client fails to connect to
	   // Google Play services
	   private ConnectionResult mConnectionResult;
	   @Override
	   protected void onCreate(Bundle savedInstanceState)
	   {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.main_activity);
	 
	      findViewById(R.id.sign_in_button).setOnClickListener(this);
	 
	      // Initializing google plus api client
	      mGoogleApiClient = buildGoogleAPIClient();
	   }
	 
	   /**
	    * API to return GoogleApiClient Make sure to create new after revoking
	    * access or for first time sign in
	    *
	    * @return
	    */
	   private GoogleApiClient buildGoogleAPIClient()
	   {
	      return new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
	            .addOnConnectionFailedListener(this)
	            .addApi(Plus.API, PlusOptions.builder().build())
	            .addScope(Plus.SCOPE_PLUS_LOGIN).build();
	   }
	   
	   @Override
	   protected void onStart() {
	      super.onStart();
	      // make sure to initiate connection
	      mGoogleApiClient.connect();
	   }
	 
	   @Override
	   protected void onStop() {
	      super.onStop();
	      // disconnect api if it is connected
	      if (mGoogleApiClient.isConnected())
	         mGoogleApiClient.disconnect();
	   }
	 
	   /**
	    * Handle Button onCLick Events based upon their view ID
	    */
	   @Override
	   public void onClick(View v) 
	   {   
	     processSignIn();
	   }
	   /**
	    * API to handler sign in of user If error occurs while connecting process
	    * it in processSignInError() api
	    */
	   private void processSignIn() {
	 
	      if (!mGoogleApiClient.isConnecting()) {
	         processSignInError();
	         mSignInClicked = true;
	      }
	 
	   }
	 
	   /**
	    * API to process sign in error Handle error based on ConnectionResult
	    */
	   private void processSignInError() {
	      if (mConnectionResult != null && mConnectionResult.hasResolution()) {
	         try {
	            mIntentInProgress = true;
	            mConnectionResult.startResolutionForResult(this,
	                  SIGN_IN_REQUEST_CODE);
	         } catch (SendIntentException e) {
	            mIntentInProgress = false;
	            mGoogleApiClient.connect();
	         }
	      }
	   }
	   /**
	    * Callback for GoogleApiClient connection failure
	    */
	   @Override
	   public void onConnectionFailed(ConnectionResult result) {
	      if (!result.hasResolution()) {
	         GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
	               ERROR_DIALOG_REQUEST_CODE).show();
	         return;
	      }
	      if (!mIntentInProgress) {
	         mConnectionResult = result;
	 
	         if (mSignInClicked) {
	            processSignInError();
	         }
	      }
	 
	   }
	 
	   /**
	    * Callback for GoogleApiClient connection success
	    */
	   @Override
	   public void onConnected(Bundle connectionHint) {
	      mSignInClicked = false;
	      processUserInfo();
	      Toast.makeText(getApplicationContext(), "Signed In Successfully",
	            Toast.LENGTH_LONG).show();
	   }
	   private void processUserInfo() {
		      Person signedInUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
		      if (signedInUser != null) 
		      {
		    	  String userID = signedInUser.getId();
		    	  Log.d("GOOGLE PLUS","USER ID: " + userID);
		         if (signedInUser.hasDisplayName()) {
		            String userName = signedInUser.getDisplayName();
		            Log.d("GOOGLE PLUS","Name: " + userName);
		         } 
		         if (signedInUser.hasImage()) {
		            String userProfilePicUrl = signedInUser.getImage().getUrl();
		            Log.d("GOOGLE PLUS","IMAGE URL: "+userProfilePicUrl);
		         } 
		      }
		   }
	   /**
	    * Callback for suspension of current connection
	    */
	   @Override
	   public void onConnectionSuspended(int cause) {
	      mGoogleApiClient.connect();
	 
	   }
	   @Override
	   protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	   {
	      if (requestCode == SIGN_IN_REQUEST_CODE) 
	      {
	         if (resultCode != RESULT_OK)
	         {
	            mSignInClicked = false;
	         }
	 
	         mIntentInProgress = false;
	 
	         if (!mGoogleApiClient.isConnecting())
	         {
	            mGoogleApiClient.connect();
	         }
	      } 
	      else if (requestCode == PICK_MEDIA_REQUEST_CODE) 
	      {
	         // If picking media is success, create share post using
	         // PlusShare.Builder
	         if (resultCode == RESULT_OK)
	         {
	            Uri selectedImage = data.getData();
	            ContentResolver cr = this.getContentResolver();
	            String mime = cr.getType(selectedImage);
	 
	            PlusShare.Builder share = new PlusShare.Builder(this);
	            share.setText("Hello from AndroidSRC.net");
	            share.addStream(selectedImage);
	            share.setType(mime);
	            startActivityForResult(share.getIntent(),
	                  SHARE_MEDIA_REQUEST_CODE);
	         }
	      }
	   }
	 
}
