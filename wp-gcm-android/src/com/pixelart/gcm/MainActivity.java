package com.pixelart.gcm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.content.pm.PackageManager;
import java.io.IOException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
*@author PixelartDev - http://pixelartdev.com
* 
* Copyright 2014 GPUv3
*/
public class MainActivity extends Activity {
	
	
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "YOUR ID";
	String URL = "YOUR URL";
    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    String regid;
	String responseBody;
	String condition = "0";
	String ms;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mDisplay = (TextView) findViewById(R.id.display);
		
		condition = getIntent().getStringExtra("alert");
		if(condition != null){
			ms = getIntent().getStringExtra("msg");
			alert();
		}else{}
		
        context = getApplicationContext();

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            if (regid.isEmpty()) {
                new registerInBackground().execute();
            }
        } else {
            Log.i("Px GCM", "No valid Google Play Services APK found.");
        }
    }
	
	private void alert() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("GCM Message");
		alertDialogBuilder
			.setMessage(ms)
			.setCancelable(false)
			.setPositiveButton("OK",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					dialog.cancel();
				}});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
													  PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i("Px GCM", "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}
	
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i("Px GCM", "Registration not found.");
			return "";
		}
	
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i("Px GCM", "App version changed.");
			return "";
		}
		return registrationId;
	}
	
	private SharedPreferences getGCMPreferences(Context context) {
		return getSharedPreferences(MainActivity.class.getSimpleName(),
									Context.MODE_PRIVATE);
	}
	
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
	
	private class registerInBackground extends AsyncTask<Void, Integer, String> {
		@Override
        protected String doInBackground(Void... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                msg = "Device registered \n registration ID=" + regid;
                sendRegistrationIdToBackend();
				
                storeRegistrationId(context, regid);
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            mDisplay.append(msg + "\n");
        }
	}
	
	private void sendRegistrationIdToBackend() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL+"/?regId="+regid);
		try {
			HttpResponse response = httpclient.execute(httppost);	
			responseBody = EntityUtils.toString(response.getEntity());

		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}
	
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGCMPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i("Px GCM", "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}
}
