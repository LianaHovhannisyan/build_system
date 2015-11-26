/**
 * 
 */
package instigate.simCardChangeNotifier.ui;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;

/**
 * Created by Instigate Mobile on 03/20/15.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.database.olddb.DataBaseUpgradeManager;
import instigate.simCardChangeNotifier.database.olddb.OldDataBaseInterface;
import instigate.simCardChangeNotifier.listeners.CustomDeviceAdminReceiver;
import instigate.simCardChangeNotifier.logic.CustomSmsManager;
import instigate.simCardChangeNotifier.logic.SharedData;
import instigate.simCardChangeNotifier.logic.SimData;
import instigate.simCardChangeNotifier.logic.UserData;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/**
 * This class is designed to help "MainActivity" class. This class contain
 * functions and data which need "MainActivity" class.
 */
public class MainActivityLogic implements OldDataBaseInterface {

	private String mSimSerialNumber = "";
	private String mSimPhoneNumber = "";
	private SimData simData;
	private CustomAdapter adapter;
	private UserData userData;
	private Tracker tracker;
	private Context context;
	private String mTrustedNumber;

	public MainActivityLogic(Context ct) {
		context = ct;
		initialize();
	}

	//
	// Sends Button events to Google Analytics.
	//
	void trackerButtonCategory(String buttonName) {
		if (DEBUG) {
			return;
		}
		tracker.send(MapBuilder.createEvent("UI Buttons", buttonName, null, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

	//
	// Sends info to Google Analytics about Trusted Number was entered with
	// country code or not.
	//
	void trackerCountryCode(String trustedNumber) {
		if (DEBUG) {
			return;
		}
		boolean isEnteredWithCode;
		if (trustedNumber.startsWith("+") || trustedNumber.startsWith("00")) {
			isEnteredWithCode = true;
		} else {
			isEnteredWithCode = false;
		}
		tracker.send(MapBuilder.createEvent("Prefereces", "Trusted Number",
				(isEnteredWithCode ? "With" : "Without") + " Country Code", null).build());
		tracker.set(Fields.SCREEN_NAME, null);

	}

	//
	// Sends Preferences changes info to Google Analytics.
	//
	void trackerPreferences() {
		if (DEBUG) {
			return;
		}
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		long time = Integer.parseInt(pref.getString("prefSmsInterval", "120"));

		tracker.send(MapBuilder.createEvent("Prefereces", "Location Interval",
				SharedData.LOCATION_TIME_CHANGED ? "Changed to " + time : "Not Changed", time).build());
		tracker.set(Fields.SCREEN_NAME, null);

		tracker.send(MapBuilder.createEvent("Prefereces", "Unhide Number",
				SharedData.UNHIDE_NUMBER_CHANGED ? "Changed" : "Not Changed", null).build());
		tracker.set(Fields.SCREEN_NAME, null);

		tracker.send(MapBuilder.createEvent("Prefereces", "Unlock Password",
				SharedData.UNLOCK_PASSWORD_CHANGED ? "Changed" : "Not Changed", null).build());
		tracker.set(Fields.SCREEN_NAME, null);
		tracker.send(MapBuilder.createEvent("Prefereces", "Admin Rights",
				SharedData.ADMIN_PERMISSION_ENABLED ? "Enabled" : "Not Enabled", null).build());
	}

	//
	// Sends Sim cards count info to Google Analytics.
	//
	void trackerSimCount() {
		if (DEBUG) {
			return;
		}
		String simCount = String.valueOf(simData.getSimCount());
		tracker.send(MapBuilder.createEvent("Prefereces", "Count of Sim Cards", simCount, null).build());
		tracker.set(Fields.SCREEN_NAME, null);

	}

	//
	// Send a screen view to Google Analytics by setting a map of parameter
	// values on the tracker and calling send.
	//
	void startGoogleAnalytics() {
		if (DEBUG) {
			return;
		}
		tracker = GoogleAnalytics.getInstance(context)
				.getTracker(context.getResources().getString(R.string.google_tracking_id));
	}

	//
	// Adds current SIM card's serial number to database.
	//
	public void addActiveSimNumberToDB() {
		if (getSimSerialNumber().equals("empty")) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) " + "Sim Card is : " + getSimSerialNumber());
			}
		} else if (simData.addSimData(getSimPhoneNumber(), getSimSerialNumber()) > 0) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) " + "Sim Card Serial number: " + getSimSerialNumber());
			}
		} else {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) " + "Sim Card already exists: " + getSimSerialNumber());
			}
		}

	}

	//
	// Creates user information rows and adds them to "listview"
	//
	public void createUserInfoRows(ListView listview) {
		String[] simSerialNumbers = getSimData().getSimSerialNumbers();
		if (SHOW_LOGS) {
			Log.i("LOG_I: (main) " + " Creating Adapter");
		}
		float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35,
				context.getResources().getDisplayMetrics());
		float sumPixels = 0;
		final ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < simSerialNumbers.length; ++i) {
			list.add(simSerialNumbers[i]);
			sumPixels += pixels;
		}
		adapter = new CustomAdapter(context, list);
		listview.setAdapter(adapter);
		setListViewHeight(listview, sumPixels);
	}

	//
	// Sets height of "listView"(SIM card's serial numbers list).
	//
	private void setListViewHeight(ListView listview, float height) {
		int list_height = getListViewHeight(listview);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (main) " + " ListView Height:" + list_height + " PX: " + height);
		}
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) listview.getLayoutParams();
		lp.height = (int) height;
		listview.setLayoutParams(lp);
	}

	//
	// Gets height of "listView"(SIM card's serial numbers list).
	//
	private int getListViewHeight(ListView listview) {
		ListAdapter adapter = listview.getAdapter();
		int listViewHeight = 0;
		listview.measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		listViewHeight = listview.getMeasuredHeight() * adapter.getCount()
				+ (adapter.getCount() * listview.getDividerHeight());
		return listViewHeight;
	}

	//
	// Return true if application is installed.
	//
	boolean appInstalledOrNot(String uri) {
		PackageManager pm = context.getPackageManager();
		boolean app_installed = false;
		try {
			pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
			app_installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			app_installed = false;
		}
		return app_installed;
	}

	//
	// Initializes class members.
	//
	private void initialize() {
		userData = new UserData(context);
	}

	//
	// Saves application configuration in database, tracks information to Google
	// Analytics.
	//
	public void saveInfoInDatabase(final String userName, final String trustedUserNumber, final String email) {
		getUserData().deleteAllUsers();
		getUserData().addUserData(userName, trustedUserNumber);
		mTrustedNumber = trustedUserNumber;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		Editor prefsEditor = pref.edit();
		prefsEditor.putString("prefEmailField", email);
		prefsEditor.commit();
		trackerPreferences();
		trackerSimCount();
		trackerCountryCode(trustedUserNumber);
	}

	//
	// Shows message which informs that the icon of the application will
	// disappear from your menu.
	//
	public void showHideIconMessage() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String unhideNumber = pref.getString("unhide_number", "");
		builder.setTitle(context.getResources().getString(R.string.massage_hide_icon_title))
				.setMessage(context.getResources().getString(R.string.massage_hide_icon, unhideNumber))
				.setCancelable(false).setPositiveButton(context.getResources().getText(R.string.ok_button),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showSendSmsOnSaveDialog();
							}
						});
		builder.create().show();
	}

	void showSendSmsOnSaveDialog() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		dialogBuilder.setTitle(context.getResources().getString(R.string.alert_title_send_sms_on_save));
		dialogBuilder.setMessage(context.getResources().getString(R.string.alert_massage_send_sms_on_save));
		dialogBuilder.setCancelable(false);
		dialogBuilder.setPositiveButton(context.getResources().getString(R.string.send_sms_button),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						CustomSmsManager smsManager = new CustomSmsManager(context);
						smsManager.sendSmsOnSave(mTrustedNumber);
						hideApplication();
					}
				});
		dialogBuilder.setNegativeButton(context.getResources().getString(R.string.dont_send_sms_button),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						hideApplication();
					}

				});
		if (!getSimSerialNumber().equals("empty")) {
			dialogBuilder.create().show();
		} else {
			hideApplication();
		}
	}

	//
	// Disables application and hides it.
	//
	void hideApplication() {
		// hide application icon
		PackageManager pm = context.getPackageManager();
		Activity activity = (Activity) context;
		pm.setComponentEnabledSetting(activity.getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		simData.close();
		userData.close();
		activity.finish();
	}

	//
	// Transfers old "SIM_SERIAL_DATABASE" database to new database.
	//
	String transferSimData() {
		String status = "";
		// DataBaseUpgradeManager is for manipulation with old database.
		// Sim Serial Numbers.
		DataBaseUpgradeManager oldDataBaseSim = new DataBaseUpgradeManager(context, SIM_SERIAL_DATABASE);
		String[] oldDataSim = oldDataBaseSim.getData(SIM_SERIAL_TABLE, SIM_SERIAL_COLUMN);
		for (int i = 0; i < oldDataSim.length; i++) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) adding sim serial from old DataBase : " + oldDataSim[i]);
			}
			getSimData().addSimData("empty", oldDataSim[i]);
		}
		if (oldDataBaseSim.removeDB(SIM_SERIAL_DATABASE)) {
			status += "Old Sim Serial Numbers Imported successfully\n";
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) DataBase Removed: " + SIM_SERIAL_DATABASE);
			}
		} else {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) DataBase Removing Failed: " + SIM_SERIAL_DATABASE);
			}
		}
		return status;
	}

	//
	// Transfers old "OWNER_NAME_DATABASE" and "PHONE_NUMBER_DATABASE" database
	// to new database.
	//
	boolean transferAllData(boolean transfer) {
		DataBaseUpgradeManager oldDataBaseOwner = new DataBaseUpgradeManager(context, OWNER_NAME_DATABASE);
		DataBaseUpgradeManager oldDataBaseTrustedNumber = new DataBaseUpgradeManager(context, PHONE_NUMBER_DATABASE);
		if (transfer) {
			// DataBaseUpgradeManager is for manipulation with old database.
			String[] oldDataOwner = oldDataBaseOwner.getData(OWNER_NAME_TABLE, OWNER_NAME_COLUMN);
			String[] oldDataTrustedNumber = oldDataBaseTrustedNumber.getData(PHONE_NUMBER_TABLE, PHONE_NUMBER_COLUMN);
			for (int i = 0; i < oldDataOwner.length; i++) {
				if (SHOW_LOGS) {
					Log.i("LOG_I: (main) adding Owner and trusted number: " + oldDataOwner[i] + " "
							+ oldDataTrustedNumber[i]);
				}
				if (getUserData().addUserData(oldDataOwner[i], oldDataTrustedNumber[i]) > 0) {
					if (SHOW_LOGS) {
						Log.i("LOG_I: (main) saving: " + oldDataOwner[i] + " : " + oldDataTrustedNumber[i]);
					}
					hideApplication();
				}
			}
		}
		if (oldDataBaseOwner.removeDB(OWNER_NAME_DATABASE)
				&& oldDataBaseTrustedNumber.removeDB(PHONE_NUMBER_DATABASE)) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) DataBase Removed: " + OWNER_NAME_DATABASE + " and " + PHONE_NUMBER_DATABASE);
			}
		} else {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) DataBase Removing Failed: " + OWNER_NAME_DATABASE + " and "
						+ PHONE_NUMBER_DATABASE);
			}
		}
		return true;
	}

	//
	// Gets "mSimSerialNumber".
	//
	String getSimSerialNumber() {
		TelephonyManager mTelephoneManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String str = mTelephoneManager.getSimSerialNumber();
		if (str != null && !str.equals("")) {
			mSimSerialNumber = str;
		} else {
			mSimSerialNumber = "empty";
		}
		return mSimSerialNumber;

	}

	//
	// Gets SIM phone number
	//
	String getSimPhoneNumber() {
		TelephonyManager mTelephoneManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = mTelephoneManager.getLine1Number();
		if (phoneNumber != null && !phoneNumber.equals("")) {
			mSimPhoneNumber = phoneNumber;
		} else {
			mSimPhoneNumber = "empty";
		}
		return mSimPhoneNumber;
	}

	//
	// Sets "simData".
	//
	void setSimData(SimData simData) {
		this.simData = simData;
	}

	//
	// Gets "simData".
	//
	SimData getSimData() {
		return simData;
	}

	//
	// Gets "userData".
	//
	UserData getUserData() {
		return userData;
	}

	//
	// Initializes application preferences with random values.
	//
	public void initializePreferences() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String unhideNumber = pref.getString("unhide_number", SharedData.DEFAULT_SHOW_NUMBER);
		pref.edit().putString("unhide_number", unhideNumber).commit();

		String unlockPassword = pref.getString("unlock_password", "");
		if (unlockPassword.equals("")) {
			unlockPassword = randomString(SharedData.UNLOCK_PASSWORD_LENGTH);
			pref.edit().putString("unlock_password", unlockPassword).commit();
		}
	}

	//
	// Generates password with set length and random symbols.
	//
	public String randomString(int length) {
		Random rand = new Random();
		String pattern = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(pattern.charAt(rand.nextInt(pattern.length())));
		return sb.toString();
	}

}
