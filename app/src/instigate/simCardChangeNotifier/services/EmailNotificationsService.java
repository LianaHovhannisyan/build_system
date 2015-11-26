package instigate.simCardChangeNotifier.services;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;

/**
 * Created by Instigate Mobile 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.logic.CustomEmailManager;
import instigate.simCardChangeNotifier.logic.CustomSmsManager;
import instigate.simCardChangeNotifier.logic.SharedData;
import instigate.simCardChangeNotifier.logic.SimData;
import instigate.simCardChangeNotifier.logic.UserData;
import instigate.simCardChangeNotifier.ui.MainActivity;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/**
 * Starts background service, checks if the active SIM Card Serial Number is
 * registered in the Application's registered numbers Database. If the number is
 * not found in the Database, Application sends SMSes with active phone number
 * and GPS coordinates (in case of possibility) to trusted person.
 */
public class EmailNotificationsService extends Service {

	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	private CustomEmailManager mEmailManager;
	private Context context;
	private Timer mLocationTimer;
	private String mTrustedOwner;
	private String mTrustedNumber;
	private PowerManager pm;
	private PowerManager.WakeLock wl;

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		// This is required for keeping application running when the device is
		// locked.
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		if (wl != null) {
			wl.acquire();
		}
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) Starting");
		}
		UserData userData = new UserData(this);
		mTrustedOwner = userData.getUserNames()[0];
		mTrustedNumber = userData.getUserPhoneNumbers()[0];
		mEmailManager = new CustomEmailManager(this, mTrustedOwner, mTrustedNumber);
		handleServiceStateChanges();
	}

	//
	// Handles SIM card Service state changes and when SIM card is ready checks
	// its status in terms of application logic.
	//
	private void handleServiceStateChanges() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) handleServiceStateChanges()");
		}
		final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(new PhoneStateListener() {
			boolean isFirstCall = true;

			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
					if (isFirstCall) {
						checkStatus();
					}
					isFirstCall = false;
				}
			}
		}, PhoneStateListener.LISTEN_SERVICE_STATE);
	}

	//
	// Checks weather SIM card is registered or not. If SIM card is not
	// registered sent SMS notification and initializes "LocationListener".
	//
	private void checkStatus() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) checkStatus()");
		}
		if (!checkSimCard() || !getApplicationState()) {
			// This is case when inserted SIM card is registered or application
			// is not activated. In this case "SimCardCheckerService" service
			// must be stopped.
			stopSelf();
		}
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);		
		sendNotificationEmailWithDelay(SharedData.MESSAGE_SEND_DELAY);
	}

	

	//
	// Removes location updates from "mLocationListener".
	//
	private void stopLocationUpdate() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) Stop Location Update.");
		}
		if (mLocationListener != null && mLocationManager != null) {
			mLocationManager.removeUpdates(mLocationListener);
		}
	}

	//
	// initializes "mLocationListener" and implements "onLocationChanged"
	// function.
	//
	private void initializeLocationListener() {
		mLocationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (SHOW_LOGS) {
					Log.i("LOG_I: (SimCardCheckService ->" + " LocationListener) onLocationChanged");
				}
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
				if (getApplicationState() && checkSimCard()) {
					if (latitude != 0 && longitude != 0) {
						android.util.Log.d("MYTAG", "Sending GPS email  :))))");

						mEmailManager.sendEmailWithGPSCoordinates(latitude, longitude);
					}
				}
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}
		};	
	
			PackageManager pm = getPackageManager();
			if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
				int time = Integer.parseInt(pref.getString("prefSmsInterval", "120"));
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60 * time, 0,
						mLocationListener);
			}
		}
	

	//
	// Sends data to Google Analytics.
	//
	private void sendAction(String action) {
		if (DEBUG) {
			return;
		}
		Tracker tracker = GoogleAnalytics.getInstance(this)
				.getTracker(this.getResources().getString(R.string.google_tracking_id));
		GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
		HashMap<String, String> hitParameters = new HashMap<String, String>();
		hitParameters.put(Fields.HIT_TYPE, "appview");
		hitParameters.put(Fields.SCREEN_NAME, "Sim Card Checker Service");
		tracker.send(hitParameters);
		tracker.send(MapBuilder.createEvent("Sim Checker", action, null, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

	//
	// Returns true if application is hidden and activated.
	//
	private boolean getApplicationState() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService)" + " getApplicationState() ");
		}
		PackageManager pm = getApplicationContext().getPackageManager();
		if (PackageManager.COMPONENT_ENABLED_STATE_DISABLED == (pm
				.getComponentEnabledSetting(new ComponentName(getApplicationContext(), MainActivity.class)))) {
			return true;
		} else if (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT == (pm
				.getComponentEnabledSetting(new ComponentName(getApplicationContext(), MainActivity.class)))) {
			return false;
		} else {
			return false;
		}
	}

	//
	// Checks whether current SIM card is registered as owners SIM card, or not.
	// Returns true if SIM card was not registered.
	//
	private boolean checkSimCard() {
		String mSimSerialNumber = getSimSerialNumber();
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) Sim Serial Number: " + mSimSerialNumber);
		}
		if (mSimSerialNumber == null) {
			return false;
		}
		if (mSimSerialNumber.equals("")) {
			return false;
		}

		SimData simData = new SimData(this);
		if (!simData.similarSerialNumber(mSimSerialNumber)) {
			return true;
		}
		return false;
	}

	//
	// Returns current SIM card serial number.
	//
	private String getSimSerialNumber() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService)" + " getSimSerialNumber() ");
		}
		TelephonyManager mTelephoneManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String str = mTelephoneManager.getSimSerialNumber();
		return str;
	}

	private void sendNotificationEmailWithDelay(long sendDelay) {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) sending sms with password. ");
		}		
		final Handler handler = new Handler();
		Timer sendNotificationEmailTimer = new Timer();
		sendNotificationEmailTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						mEmailManager.sendEmailNotification();
						initializeLocationListener();						
					}
				});				
			}
		}, sendDelay);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!getApplicationState()) {
			// This is case when application is not activated. In this case
			// "SimCardCheckerService" service must be stopped.
			stopSelf();
		} else if (!checkSimCard()) {
			// This is case when inserted SIM card is registered. In this case
			// "SimCardCheckerService" service must be stopped.
			sendAction("Registered Sim");
			stopSelf();
		} else {
			sendAction("Unregistered Sim");
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService)" + " onDestroy() ");
		}
		stopLocationUpdate();
		if (mLocationTimer != null)
			mLocationTimer.cancel();
		if (wl != null) {
			// This method releases CPU resources.
			wl.release();
		}
	}
}
