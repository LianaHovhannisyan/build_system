package instigate.simCardChangeNotifier.services;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;

/**
 * Created by Instigate Mobile on 11/1/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.listeners.CustomDeviceAdminReceiver;
import instigate.simCardChangeNotifier.logic.CustomSmsManager;
import instigate.simCardChangeNotifier.logic.SharedData;

import java.util.HashMap;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/**
 * Service for Lock device with SMS.
 */
public class SetPasswordService extends Service {

	private DevicePolicyManager devicePolicyManager;
	private ComponentName customDeviceAdmin;
	private String password;
	private CustomSmsManager smsManager;
	private String trustedNumber;
	private String owner;
	private Tracker tracker;
	private Intent intent;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.intent = intent;
		tracker = GoogleAnalytics.getInstance(this)
				.getTracker(this.getResources().getString(R.string.google_tracking_id));
		// Initialize Device Policy Manager service and our receiver class.
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		customDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
		smsManager = new CustomSmsManager(this);
		String action = intent.getStringExtra("ACTION");
		if(action !=null){
		if (action.equals("block")) {
			setPassword();
		}}
		return Service.START_NOT_STICKY;
	}

	//
	// Sends data to Google Analytics.
	//
	private void sendType(String type) {
		if (DEBUG) {
    		return;
    	}
		HashMap<String, String> hitParameters = new HashMap<String, String>();
		hitParameters.put(Fields.HIT_TYPE, "appview");
		hitParameters.put(Fields.SCREEN_NAME, "Block Device");
		tracker.send(hitParameters);
		tracker.send(MapBuilder.createEvent("Admin Manager", "Block Device", type, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

	//
	// Block device by setting lock password.
	//
	private void setPassword() {
		password = intent.getStringExtra("PASSWORD");
		trustedNumber = intent.getStringExtra("NUMBER");
		owner = intent.getStringExtra("OWNER");
		if (SHOW_LOGS)
			Log.i("LOG_I: (SetPasswordService) Blocking Device with new Password: " + password);
		try {
			devicePolicyManager.setPasswordQuality(customDeviceAdmin, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
			devicePolicyManager.setPasswordMinimumLength(customDeviceAdmin, SharedData.UNLOCK_PASSWORD_LENGTH);
			devicePolicyManager.resetPassword(password, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
			devicePolicyManager.lockNow();
			sendType("New Password Set");
			smsManager.sendSmsWithNewPassword(trustedNumber, owner, password);
		} catch (SecurityException e) {
			// SecurityException will be thrown if app has now administrator
			// rights.
			sendType("No Admin Right");
			smsManager.sendSmsNoAdminRight(owner, trustedNumber);
			if (SHOW_LOGS)
				Log.i("LOG_I: (SetPasswordService)" + "in else SecurityException (No Admin Right) : " + e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}