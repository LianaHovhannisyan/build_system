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
public class SimCardCheckerService extends Service {

	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	private CustomSmsManager mSmsManager;
	private Timer mNotificationSMSTimer;
	private Timer mLocationSMSTimer;
	private String mTrustedOwner;
	private String mTrustedNumber;
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	private BroadcastReceiver smsResultReceiver;

	@Override
	public void onCreate() {
		super.onCreate();
		SharedData.firstSmsReceived = false;
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
		mSmsManager = new CustomSmsManager(this);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) SharedPref");
		}
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
		initializeSentSmsStatus();
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!checkSimCard() || !getApplicationState()) {
			// This is case when inserted SIM card is registered or application
			// is not activated. In this case "SimCardCheckerService" service
			// must be stopped.
			stopSelf();
		}
		UserData userData = new UserData(this);
		mTrustedOwner = userData.getUserNames()[0];
		mTrustedNumber = userData.getUserPhoneNumbers()[0];
		initializeLocationListener();
		// Tries to sent notification SMS after fixed(5 minutes) time (as in
		// some devices service becomes ready several minutes after SIM
		// loading) with set time(5 minutes) interval, when after detecting
		// unregistered SIM. Timer will stop in success.
		final Handler notificationHandler = new Handler();
		mNotificationSMSTimer = new Timer();
		TimerTask doAsynchronousNotificationTask = new TimerTask() {
			@Override
			public void run() {
				notificationHandler.post(new Runnable() {
					public void run() {
						try {
							if (SHOW_LOGS) {
								Log.i("LOG_I: (SimCardCheckService) sending sms with password. ");
							}
							if (checkSimCard() && getApplicationState()) {
								mSmsManager.sendSmsNotification(mTrustedOwner, mTrustedNumber);
							}

						} catch (Exception e) {
							if (SHOW_LOGS) {
								Log.i("LOG_I: (SimCardCheckService) Exception in Notification timer. \n"
										+ e.getMessage());
							}
						}
					}
				});
			}
		};
		mNotificationSMSTimer.schedule(doAsynchronousNotificationTask, SharedData.MESSAGE_SEND_DELAY, 1000 * 60 * 5);
	}

	//
	// Initializes "BroadcastReceiver" for listening result of sent SMS.
	//
	private void initializeSentSmsStatus() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) initializeSentSmsStatus()");
		}
		smsResultReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (getResultCode() == Activity.RESULT_OK) {
					// Checks for calling startLocationUpdate() only one time
					// when first SMS notification is sent.
					if (!SharedData.firstSmsReceived) {
						SharedData.firstSmsReceived = true;
						PackageManager pm = getPackageManager();
						if (!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {

							// This is case when the device on which the
							// application works does not have "Location"
							// feature. In this case "SimCardCheckerService"
							// service must be stopped.
							sendAction("Device doesn't have Location feature");
							stopSelf();
						}
						initializeLocationTimer();
						if (mNotificationSMSTimer != null) {
							mNotificationSMSTimer.cancel();
						}
					}
					unregisterReceiver(smsResultReceiver);
				}
			}
		};
		registerReceiver(smsResultReceiver, new IntentFilter("SMS_SENT"));
	}

	//
	// sets timer periodically to call "startLocationUpdate()".
	//
	private void initializeLocationTimer() {
		// This is for periodically and asynchronously call
		// startLocationUpdate() function.
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) initializeLocationTimer()");
		}
		final Handler handler = new Handler();
		mLocationSMSTimer = new Timer();
		TimerTask doAsynchronousTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						try {
							if (!checkSimCard() || !getApplicationState()) {
								// This is case when inserted SIM card is
								// registered or application
								// is not activated. In this case
								// "SimCardCheckerService" service
								// must be stopped.
								stopSelf();
							} else {
								startLocationUpdate();
							}
						} catch (Exception e) {
							if (SHOW_LOGS) {
								Log.i("LOG_I: (SimCardCheckService) Exception in Location timer. \n" + e.getMessage());
							}
						}
					}
				});
			}
		};
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		int time = Integer.parseInt(pref.getString("prefSmsInterval", "120"));
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) Time(Minutes): " + time);
		}
		mLocationSMSTimer.schedule(doAsynchronousTask, 1000 * 30, 1000 * 60 * time);
	}

	//
	// "startLocationUpdate" requests location updates from both
	// "NETWORK_PROVIDER" and "GPS_PROVIDER" with set minimum time interval.
	// "requestLocationUpdates" are listened by "mLocationListener"
	//
	private void startLocationUpdate() {
		// minimal time interval between location updates in milliseconds. As
		// updates can be received very fast, setting "minTime" calls
		// "onLocationChanged" function just one every "minTimeInterval"".
		final long minTimeInterval = 10000;
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SimCardCheckService) Start Location Update.");
		}
		if (mLocationListener != null && mLocationManager != null) {
			PackageManager pm = getPackageManager();
			if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeInterval, 0,
						mLocationListener);
			}
			try {
				Thread.sleep(minTimeInterval);
				if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)) {
					mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeInterval, 0,
							mLocationListener);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
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
				// when onLocationChanged() function is called checks SIM card
				// and sends SMS with GPS coordinates and stops location
				// updates.
				if (getApplicationState()) {
					if (latitude != 0 && longitude != 0) {
						mSmsManager.sendSmsWithGPSCoordinates(mTrustedOwner, mTrustedNumber, latitude, longitude);
						// stops location updates to avoid sent two SMSs in a
						// row.(One can be sent by GPS_PROVIDER and the other
						// one can be sent by NETWORK_PROVIDER). Location update
						// will be started in mLocationSMSTimer with set time
						// interval.
						stopLocationUpdate();
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
		if (mNotificationSMSTimer != null)
			mNotificationSMSTimer.cancel();
		if (mLocationSMSTimer != null)
			mLocationSMSTimer.cancel();
		if (wl != null) {
			// This method releases CPU resources.
			wl.release();
		}
	}
}
