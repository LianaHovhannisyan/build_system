package instigate.simCardChangeNotifier.listeners;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;

import java.util.HashMap;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.logic.CustomSmsManager;
import instigate.simCardChangeNotifier.logic.SharedData;
import instigate.simCardChangeNotifier.logic.SimData;
import instigate.simCardChangeNotifier.logic.UserData;
import instigate.simCardChangeNotifier.services.SetPasswordService;
import instigate.simCardChangeNotifier.services.SimCardCheckerService;
import instigate.simCardChangeNotifier.ui.MainActivity;

/**
 * The class processed the received sms of the user and based on the context and
 * performs the corresponding action.
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {

	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private UserData userData;
	private SimData simData;
	private String savedPhoneNumber = "";
	private String savedUserName = "";
	private Context context;
	private CustomSmsManager smsManager;
	private PackageManager pm;
	private Tracker tracker;
	private Intent intent;
	private String senderNumber;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		this.intent = intent;
		pm = context.getPackageManager();
		if (getApplicationState()) {
			initializer();
			SmsMessage[] messages = checkNumberGetSms();
			if (messages != null) {
				if (messages.length > 0) {
					parseSmsContent(messages);
				}
			}
		}
	}

	private boolean getApplicationState() {
		if (PackageManager.COMPONENT_ENABLED_STATE_DISABLED == (pm
				.getComponentEnabledSetting(new ComponentName(context.getApplicationContext(), MainActivity.class)))) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) Status: " + "DISABLED");
			}
			return true;
		} else if (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT == (pm
				.getComponentEnabledSetting(new ComponentName(context.getApplicationContext(), MainActivity.class)))) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) Status: " + "DEFAULT");
			}
			return false;
		} else {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) Status: " + "ENABLED (unexpected)");
			}
			return false;
		}
	}
    //
	// Initializes data.  
	//
	private void initializer() {
		userData = new UserData(context.getApplicationContext());
		simData = new SimData(context.getApplicationContext());
		smsManager = new CustomSmsManager(context.getApplicationContext());
		savedPhoneNumber = userData.getUserPhoneNumbers()[0];
		savedUserName = userData.getUserNames()[0];
	}

	//
	// Returns the array of received messages or null if cannot get messages.
	//
	private SmsMessage[] checkNumberGetSms() {
		SmsMessage[] messages = null;
		if (!savedPhoneNumber.equals("") && intent.getAction().equals(SMS_RECEIVED) && intent.getExtras() != null) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) Saved Trusted Number: " + savedPhoneNumber);
			}
			Bundle bundle = intent.getExtras();
			Object[] pdus = (Object[]) bundle.get("pdus");
			messages = new SmsMessage[pdus.length];
			for (int i = 0; i < pdus.length; i++) {
				messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
			}

		}
		return messages;
	}

	//
	// Removes spaces from SMS before and after text and calls
	// "compareSmsContains" if sender of SMS was trusted number.
	//
	private void parseSmsContent(SmsMessage[] messages) {
		String smsContent = messages[0].getMessageBody();
		// Deletes spaces before and after SMS command.
		smsContent = smsContent.trim();
		senderNumber = messages[0].getOriginatingAddress();

		if (SHOW_LOGS) {
			Log.i("LOG_I: (SMSBroadcastReceiver) SMS Content: " + smsContent);
		}
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SMSBroadcastReceiver) Sender Number: " + senderNumber);
		}
		if (!savedPhoneNumber.substring(1).equals("")) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) (substring(1).equals(\"\")) " + smsContent);
			}
			String phoneNumWithoutFirstChar = savedPhoneNumber.substring(1);
			if (senderNumber.endsWith(phoneNumWithoutFirstChar)) {
				initTracker();
				compareSmsContains(smsContent);
			}
		}
	}
    //
	// Tracker used for sending data to Google Analytics.
	//
	private void initTracker() {
		if (DEBUG) {
    		return;
    	}
		tracker = GoogleAnalytics.getInstance(context)
				.getTracker(context.getResources().getString(R.string.google_tracking_id));
	}
	
	//
	// Calls corresponding methods depends on content of the SMS.
	//
	@SuppressLint("DefaultLocale")
	private void compareSmsContains(String smsLowerCase) {

		if (CustomSmsManager.SmsNotificationText == null) {
			CustomSmsManager.SmsNotificationText = "";
		}
		if (CustomSmsManager.SmsLocationText == null) {
			CustomSmsManager.SmsLocationText = "";
		}

		if (SharedData.isTrustedInserted) {
			return;
			// Checks case when trusted number is inserted into owners device.
		} else if (CustomSmsManager.SmsNotificationText.toLowerCase().startsWith(smsLowerCase)
				|| CustomSmsManager.SmsLocationText.toLowerCase().startsWith(smsLowerCase)) {
			SharedData.isTrustedInserted = true;
			sendReceiver("Trusted Number Inserted");
			registerSimSerial();
		} else if (smsLowerCase.compareToIgnoreCase(context.getResources().getString(R.string.add_sim_serial)) == 0) {
			registerSimSerial();
		} else if (smsLowerCase.compareToIgnoreCase(context.getResources().getString(R.string.block_device)) == 0) {
			blockDevice();
		} else
			if (smsLowerCase.compareToIgnoreCase(context.getResources().getString(R.string.reset_show_number)) == 0) {
			resetShowNumber();
		}
	}

	//
	// Resets "Show Number" to it's default.
	//
	private void resetShowNumber() {
		sendReceiver("Reset Show Number");
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SMSBroadcastReceiver) Reset Show Number " + savedUserName + "\'s Phone");
		}
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (pref.edit().putString("unhide_number", SharedData.DEFAULT_SHOW_NUMBER).commit()) {
			smsManager.sendSmsUnhideNumberResetSuccess(savedUserName, senderNumber);
		} else {
			smsManager.sendSmsUnhideNumberResetFail(senderNumber);
		}
	}

	//
	// Registers SIM card as owner's SIM card.
	//
	private void registerSimSerial() {
		sendReceiver("Add Sim Serial");
		if (simData.addSimData("empty", getSimSerialNumber(context)) > 0) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) New SIM card is registered in " + savedUserName + "\'s Phone");
			}
			smsManager.sendSmsSimRegistration(savedUserName, senderNumber);

			// As new SIM card is registered the "SimCardCheckerService" service
			// must be stops. "SimCardCheckerService" service must work only
			// when SIM card is not registered.
			Intent stopIntent = new Intent(context, SimCardCheckerService.class);
			context.stopService(stopIntent);

		} else if (simData.similarSerialNumber(getSimSerialNumber(context))) {
			sendReceiver("Sim already registered");
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) This sim card is already registered");
			}
			smsManager.sendSmsSimAlreadyRegistered(savedUserName, senderNumber);
		} else {
			sendReceiver("Unknown Action");
			if (SHOW_LOGS) {
				Log.i("LOG_I: (SMSBroadcastReceiver) Unknown Error");
			}
			smsManager.sendSmsUnknownError(senderNumber);
		}
	}

	//
	// Blocks device by setting new password.
	//
	private void blockDevice() {
		sendReceiver("Block device");
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String password = settings.getString("unlock_password", "");
		// Use this to start and trigger a service.
		Intent setPassword = new Intent(context, SetPasswordService.class);
		// Potentially add data to the intent.
		setPassword.putExtra("ACTION", "block");
		setPassword.putExtra("PASSWORD", password);
		setPassword.putExtra("NUMBER", senderNumber);
		setPassword.putExtra("OWNER", savedUserName);
		context.startService(setPassword);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (SMSBroadcastReceiver) Unlock Password: " + password);
		}
	}

	//
	// Sends data to Google Analytics.
	//
	private void sendReceiver(String actionName) {
		if (DEBUG) {
    		return;
    	}
		HashMap<String, String> hitParameters = new HashMap<String, String>();
		hitParameters.put(Fields.HIT_TYPE, "appview");
		hitParameters.put(Fields.SCREEN_NAME, "SMS Broadcast Receiver");
		tracker.send(hitParameters);
		tracker.send(MapBuilder.createEvent("SMS", "Receive", actionName, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

	//
	// Returns current SIM card serial number.
	//
	private String getSimSerialNumber(Context context) {
		TelephonyManager mTelephoneManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String str = mTelephoneManager.getSimSerialNumber();
		return str;
	}
}