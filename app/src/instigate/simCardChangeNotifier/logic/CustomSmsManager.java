package instigate.simCardChangeNotifier.logic;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;

/**
 * Created by Instigate Mobile on 11/4/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.mailing.CustomEmailSender;
import instigate.simCardChangeNotifier.ui.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

/**
 * Class is designed for sending/replying SMSes.
 */
public class CustomSmsManager extends Service {

	public static String SmsNotificationText = "";
	public static String SmsLocationText = "";
	private TextView text;
	private ImageView image;
	private Button dialog_button;
	private Dialog dialog;
	private Context context;
	private ArrayList<PendingIntent> listOfIntents;
	private int sendStatus = -1;
	private BroadcastReceiver smsResultReceiver;
	private ArrayList<String> parts;
	private SmsManager mSmsManager;
	private Tracker tracker;
	private Boolean showDialog = false;
	private Geocoder geocoder;

	public CustomSmsManager() {
	}

	public CustomSmsManager(Context context) {
		this.context = context;
		initializeSentSmsStatus();
		mSmsManager = SmsManager.getDefault();
		tracker = GoogleAnalytics.getInstance(context)
				.getTracker(context.getResources().getString(R.string.google_tracking_id));
		GoogleAnalytics.getInstance(context).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
	}

	//
	// Sends data to Google Analytics.
	//
	private void sendSmsType(String category, String type) {
		if (DEBUG) {
			return;
		}
		HashMap<String, String> hitParameters = new HashMap<String, String>();
		hitParameters.put(Fields.HIT_TYPE, "appview");
		hitParameters.put(Fields.SCREEN_NAME, "Custom SMS Manager");
		tracker.send(hitParameters);
		tracker.send(MapBuilder.createEvent("SMS", category, type, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

	//
	// Sends test SMS to validate number and get deliver status.
	//
	public int sendSmsValidation(String owner, String number) {
		sendSmsType("Send", "Validation");
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomSmsManager) sending tesuses-permissiont SMS:");
		}
		try {
			sendStatus = -1;
			String txt = context.getResources().getString(R.string.validation_sms, owner);
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) Validation Sms Context: " + txt);
			}
			parts = mSmsManager.divideMessage(txt);
			listOfIntents = new ArrayList<PendingIntent>();
			for (int i = 0; i < parts.size(); i++) {
				listOfIntents.add(resultOfSMSStatus());
			}
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) " + " Sending Validation SMS");
			}
			showDialog = true;
			mSmsManager.sendMultipartTextMessage(number, null, parts, listOfIntents, null);
		} catch (Exception e) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) " + "Exception: " + e);
			}
		}

		return sendStatus;
	}

	//
	// Sends notification SMS which informs that SIM card is changed and also
	// includes the list of remote commands.
	//
	public void sendSmsNotification(String owner, String number) {
		sendSmsType("Send", "Notification (with password)");
		String textSms = "";
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String password = settings.getString("unlock_password", "");
		textSms = context.getResources().getString(R.string.notification_sms, owner, password);
		SmsNotificationText = new String(textSms);
		replySms(number, textSms);
	}

	//
	// Sends notification SMS which informs that this number was included in
	// SCCN app.
	//
	public void sendSmsOnSave(String trustedNumber) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String unhideNumber = pref.getString("unhide_number", "");
		sendSmsType("Send", "On app settings saved");
		String textSms = context.getResources().getString(R.string.app_saved_sms, unhideNumber);
		replySms(trustedNumber, textSms);
	}

	//
	// Sends SMS with GPS coordinates and address of device (if available).
	//
	public void sendSmsWithGPSCoordinates(String owner, String number, Double latitude, Double longitude) {
		String textSms;
		String address = getAddress(latitude, longitude);
		if (!address.equals("")) {
			sendSmsType("Send", "With GPS Coordinates and Address");
			textSms = context.getResources().getString(R.string.gps_coordinates_sms, owner, address, latitude,
					longitude);
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) " + "GPS Coordinates SMS: " + textSms);
			}
		} else {
			sendSmsType("Send", "With GPS Coordinates");
			textSms = context.getResources().getString(R.string.gps_location_sms, owner, latitude, longitude);
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) " + "GPS Location SMS: " + textSms);
			}
		}
		SmsLocationText = new String(textSms);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that device was blocked successfully. Also the
	// SMS contains new password.
	//
	public void sendSmsWithNewPassword(String number, String owner, String password) {
		sendSmsType("Reply", "Blocked with Password");
		String textSms = context.getResources().getString(R.string.password_set_confirm_sms, owner, password);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that application has no administrator rights.
	//
	public void sendSmsNoAdminRight(String owner, String number) {
		sendSmsType("Reply", "Error. No Admin Right");
		String textSms = context.getResources().getString(R.string.admin_rights_missing_sms, owner);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that current SIM card was registered as owner's
	// SIM card.
	//
	public void sendSmsSimRegistration(String owner, String number) {
		sendSmsType("Reply", "Sim Registered remotely");
		String textSms = context.getResources().getString(R.string.new_sim_registered_sms, owner);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that current SIM card was already registered as
	// owner's SIM card.
	//
	public void sendSmsSimAlreadyRegistered(String owner, String number) {
		sendSmsType("Reply", "Error registering. Sim Already Registered");
		String textSms = context.getResources().getString(R.string.sim_registered_sms, owner);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that "Show Number" was successfully reset to its
	// default.
	//
	public void sendSmsUnhideNumberResetSuccess(String owner, String number) {
		sendSmsType("Reply", "Show Number was reseted to default remotely");
		String textSms = context.getResources().getString(R.string.show_number_reset_seccess, owner);
		replySms(number, textSms);
	}

	//
	// Sends SMS which informs that "Show Number" failed to reset.
	//
	public void sendSmsUnhideNumberResetFail(String number) {
		sendSmsType("Reply", "Error registering. Sim Already Registered");
		String textSms = context.getResources().getString(R.string.show_number_reset_fail);
		replySms(number, textSms);
	}

	//
	// Sends SMS about unknown error.
	//
	public void sendSmsUnknownError(String number) {
		sendSmsType("Reply", "Error. Registered Status Unknown");
		String textSms = context.getResources().getString(R.string.unknown_error_sms);
		;
		replySms(number, textSms);
	}

	//
	// Sends SMS to given number with given text.
	//
	private void replySms(String number, String smsContent) {
		try {
			parts = mSmsManager.divideMessage(smsContent);
			listOfIntents = new ArrayList<PendingIntent>();
			for (int i = 0; i < parts.size(); i++) {
				listOfIntents.add(resultOfSMSStatus());
			}
			showDialog = false;
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) " + " sending sms: " + number + " SMS Context: " + smsContent);
			}
			mSmsManager.sendMultipartTextMessage(number, null, parts, listOfIntents, null);
		} catch (Exception e) {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) Exception: Can't Reply Sms :" + e + " sms Text: " + smsContent);
			}
		}
	}

	//
	// Returns the address of given coordinates or empty string if address is
	// not available.
	//
	private String getAddress(double lati, double longi) {
		String currentAddress = "";
		try {
			if (isCurrentLanguageSupported()) {
				geocoder = new Geocoder(context);
			} else {
				geocoder = new Geocoder(context, Locale.ENGLISH);
			}
			List<Address> addresses = geocoder.getFromLocation(lati, longi, 1);
			if (addresses != null) {
				Address returnedAddress = addresses.get(0);
				StringBuilder strReturnedAddress = new StringBuilder(" ");
				for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
					strReturnedAddress.append(returnedAddress.getAddressLine(i)).append(" ");
				}
				currentAddress = strReturnedAddress.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomSmsManager) Exception get password (geocoder)" + e);
			}
		}
		return currentAddress;
	}
	
	//
	// Returns true is current language of OS Android is supported by
	// application.
	//
	private boolean isCurrentLanguageSupported() {
		String[] supportedLanguages = getResources().getStringArray(R.array.supported_languages);
		for (String l : supportedLanguages) {
			if (Locale.getDefault().getLanguage().equals(l)) {
				return true;
			}
		}
		return false;
	}

	//
	// Shows the dialog with result of the test SMS".
	//
	private void resultDialogOfSentSMS(boolean success, String dialogText, boolean showDialog) {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomSmsManager) " + " errorDialog");
		}
		if (showDialog) {
			AlertDialog.Builder alertDialogBuilder = new Builder(context);
			String title;
			int icon;
			if (success) {
				title = context.getResources().getString(R.string.test_sms_result_title_info);
				icon = R.drawable.success_icon;
			} else {
				title = context.getResources().getString(R.string.test_sms_result_title_error);
				icon = R.drawable.warning_icon;
			}
			alertDialogBuilder.setTitle(title);
			alertDialogBuilder.setIcon(icon);
			alertDialogBuilder.setMessage(dialogText);
			alertDialogBuilder.setCancelable(false);
			alertDialogBuilder.setPositiveButton(context.getResources().getString(R.string.ok_button),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alertDialogBuilder.create().show();
			MainActivity.initTestButton(success);
			this.showDialog = false;
		}
		return;
	}

	//
	// Registers "smsResultReceiver" to receive result of sent SMS.
	//
	private PendingIntent resultOfSMSStatus() {
		String SENT = "SMS_SENT";
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomSmsManager) " + " Pending Intent");
		}
		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT),
				PendingIntent.FLAG_CANCEL_CURRENT);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomSmsManager) " + " register Receiver");
		}
		context.registerReceiver(this.smsResultReceiver, new IntentFilter(SENT));
		return sentPI;
	}

	//
	// Initializes "smsResultReceiver" which will show corresponding dialog in
	// each case.
	//
	private void initializeSentSmsStatus() {
		smsResultReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String dialogText;
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: OK");
					}
					sendSmsType("Delivery Status", "OK");
					dialogText = context.getResources().getString(R.string.test_sms_result_ok);
					resultDialogOfSentSMS(true, dialogText, showDialog);
					sendStatus = 0;
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: ERROR GENERIC FAILURE");
					}
					sendSmsType("Delivery Status", "ERROR GENERIC FAILURE");
					dialogText = context.getResources().getString(R.string.test_sms_result_error_generic);
					resultDialogOfSentSMS(false, dialogText, showDialog);
					sendStatus = 1;
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: no Service");
					}
					sendSmsType("Delivery Status", "No Service");
					dialogText = context.getResources().getString(R.string.test_sms_result_error_no_service);
					resultDialogOfSentSMS(false, dialogText, showDialog);
					sendStatus = 2;
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: PDU");
					}
					sendSmsType("Delivery Status", "ERROR NULL PDU");
					dialogText = context.getResources().getString(R.string.test_sms_result_error_null_pdu);
					resultDialogOfSentSMS(false, dialogText, showDialog);
					sendStatus = 3;
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: Radio Off");
					}
					sendSmsType("Delivery Status", "ERROR RADIO OFF");
					dialogText = context.getResources().getString(R.string.test_sms_result_error_radio_off);
					resultDialogOfSentSMS(false, dialogText, showDialog);
					sendStatus = 4;
					break;
				default:
					sendSmsType("Delivery Status", "Unknown Error");
					if (SHOW_LOGS) {
						Log.i("LOG_I: (CustomSmsManager) " + "Delivery Status: Unknown");
					}
					dialogText = context.getResources().getString(R.string.test_sms_result_error_unknown);
					resultDialogOfSentSMS(false, dialogText, showDialog);
					sendStatus = 5;
					break;
				}
			}
		};
	}

	@Override
	public void onDestroy() {
		this.unregisterReceiver(this.smsResultReceiver);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
