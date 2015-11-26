package instigate.simCardChangeNotifier.ui;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.database.olddb.OldDataBaseInterface;
import instigate.simCardChangeNotifier.logic.CustomEmailManager;
import instigate.simCardChangeNotifier.logic.CustomSmsManager;
import instigate.simCardChangeNotifier.logic.SharedData;
import instigate.simCardChangeNotifier.logic.SimData;
import instigate.simCardChangeNotifier.logic.UserData;

@SuppressLint("NewApi")
public class MainActivity extends ActionBarActivity implements OldDataBaseInterface, OnClickListener {

	// private static boolean adminRights = false;
	private TelephonyManager mTelephoneManager;
	private InputMethodManager inputMethodManager;
	private EditText trustedUserPhoneNumber;
	private EditText userEmailField;
	private EditText userField;
	private static Button testButton;
	private Button settingsButton;
	private Button saveButton;
	private Button infoButton;
	private MainActivity mContext;
	private boolean checkKillAplication;
	private boolean checkChangeMainField;
	private boolean checkOldversion;
	private ListView listview;
	private AlertDialog.Builder alertDialogBuilder;
	// Old DataBase Path
	private String dbPath = "";
	private final int TRUSTED_NUMBER_MIN_LENGHT = 5;
	private CustomSmsManager smsManager;
	private String mTrustedOwner;
	private String mTrustedNumber;
	private final int RESULT_SETTINGS = 1;
	public static final String PREFS_NAME = "CustomPrefsFile";
	private Toast mToast;
	private MainActivityLogic helperLogic;
	private BroadcastReceiver updateRequestReceiver;
	private Spinner countrySpinner;
	private ArrayList<Country> countries;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		setContentView(R.layout.main);
		initializeCountrySpinner();
		createComponents();
		initActionBar();
		listview = (ListView) findViewById(R.id.simCardsList);
		// Custom SMS Manager for sending sms.
		smsManager = new CustomSmsManager(this);
		helperLogic.addActiveSimNumberToDB();
		// check old version application in device.
		boolean installed = helperLogic.appInstalledOrNot("instigate.MainActivity");
		if (installed) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getText(R.string.old_version_warning_title))
					.setMessage(getResources().getText(R.string.old_version_warning_text)).setCancelable(false)
					.setPositiveButton(getResources().getText(R.string.ok_button),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Uri packageURI = Uri.parse("package:" + "instigate.MainActivity");
									Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
									startActivity(uninstallIntent);
									dialog.dismiss();
									helperLogic.getUserData().deleteAllUsers();
									helperLogic.getSimData().deleteAllSimData();
									checkOldversion = false;
								}
							});
			builder.create().show();
		}
		updateRequestReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				helperLogic.addActiveSimNumberToDB();
				helperLogic.createUserInfoRows(listview);
			}

		};

	}

	private void initializeCountrySpinner() {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		Set<String> supportedCountryCodes = phoneUtil.getSupportedRegions();
		countries = new ArrayList<Country>(supportedCountryCodes.size() + 1);
		for (String s : supportedCountryCodes) {
			// Creating an instance.
			Country countryTemp = new Country();
			// Filling country ISO codes.
			countryTemp.setIsoCode(s);
			// Filling country names.
			Locale countryLocale = new Locale("", s);
			if (countryLocale.getDisplayCountry().compareToIgnoreCase(s) == 0 || !isCurrentLanguageSupported()) {
				countryTemp.setName(countryLocale.getDisplayCountry(Locale.ENGLISH));
			} else {
				countryTemp.setName(countryLocale.getDisplayCountry(Locale.getDefault()));
			}

			// Filling phone codes.
			countryTemp.setPhoneCode("+" + phoneUtil.getCountryCodeForRegion(s));
			// Filling flag recourses id's
			countryTemp.setFlag(getResources().getIdentifier("flag_" + s.toLowerCase(Locale.ENGLISH), "drawable",
					getPackageName()));

			countries.add(countryTemp);
		}
		// Sorting the countries by name
		Collections.sort(countries);
		Country countryOther = new Country();
		countryOther = new Country();
		countryOther.setIsoCode("");
		countryOther.setName(getResources().getString(R.string.other_country));
		countryOther.setPhoneCode("");
		countryOther.setFlag(0);
		countries.add(countryOther);
		countrySpinner = (Spinner) findViewById(R.id.spinner1);
		countrySpinner.setAdapter(new countryListAdapter(this, R.layout.country_row, countries));
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String savedCountryIsoCode = pref.getString("prefCountryIsoCode", "PREF_NOT_FOUND");
		if (savedCountryIsoCode.equals("PREF_NOT_FOUND")) {
			trustedUserPhoneNumber = (EditText) findViewById(R.id.userPhoneField);
			trustedUserPhoneNumber.setText("");
			countrySpinner.setSelection(getCountryIndexByIsoCode(tm.getNetworkCountryIso()));
		} else {
			countrySpinner.setSelection(getCountryIndexByIsoCode(savedCountryIsoCode));
		}
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
	// Returns the index of given country in countries array.
	//
	private int getCountryIndexByIsoCode(String isoCode) {
		if (countries == null)
			return 0;
		int i;
		for (i = 0; i < countries.size(); i++) {
			if (countries.get(i).getIsoCode().compareToIgnoreCase(isoCode) == 0) {
				break;
			}
		}
		if (i == countries.size()) {
			return countries.size() - 1;
		} else {
			return i;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (SHOW_LOGS) {
			Log.i("LOG_I: (main) " + "On Resume");
		}
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String savedCountryIsoCode = pref.getString("prefCountryIsoCode", "PREF_NOT_FOUND");
		if (savedCountryIsoCode.equals("PREF_NOT_FOUND")) {
			trustedUserPhoneNumber = (EditText) findViewById(R.id.userPhoneField);
			trustedUserPhoneNumber.setText("");
			countrySpinner.setSelection(getCountryIndexByIsoCode(tm.getNetworkCountryIso()));
		} else {
			countrySpinner.setSelection(getCountryIndexByIsoCode(savedCountryIsoCode));
		}
		// get DB sim serial number and show for view
		if (checkKillAplication) {
			helperLogic.createUserInfoRows(listview);
			checkKillAplication = false;
		}
		// if old version have in phone have to exit application.
		if (!checkOldversion && helperLogic.appInstalledOrNot("instigate.MainActivity")) {
			finish();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.testNumber:
			helperLogic.trackerButtonCategory("Testing SMS");
			if (checkFields()) {
				smsManager.sendSmsValidation(userField.getText().toString(), mTrustedNumber);
				testButton.setClickable(false);
				testButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.message, 0, 0, 0);
			}
			break;
		case R.id.settingButton:
			helperLogic.trackerButtonCategory("Settings");
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;
		case R.id.saveButton:
			helperLogic.trackerButtonCategory("Save");
			// Initialize Device Policy Manager service and our receiver class
			// Activate device administration
			prepareToSave();
			break;
		case R.id.infoButton:
			helperLogic.trackerButtonCategory("Help (About)");
			startActivity(new Intent(this, InfoActivity.class));
			break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// google analytic
		EasyTracker.getInstance(this).activityStart(this);
		helperLogic.startGoogleAnalytics();
		LocalBroadcastManager.getInstance(this).registerReceiver((updateRequestReceiver),
				new IntentFilter(SharedData.UPDATE_REQUEST));
	}

	@Override
	protected void onStop() {
		super.onStop();
		// google analytic
		EasyTracker.getInstance(this).activityStop(this);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(updateRequestReceiver);
		// Saving country selection.
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Editor prefsEditor = pref.edit();
		prefsEditor.putString("prefCountryIsoCode",
				countries.get(countrySpinner.getSelectedItemPosition()).getIsoCode());
		prefsEditor.commit();
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mToast != null) {
			mToast.cancel();
		}
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	protected void initActionBar() {
		ActionBar bar = getSupportActionBar();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.background_down, getTheme()));
		} else {
			bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.background_down));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			helperLogic.trackerButtonCategory("Settings (Menu)");
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;
		case R.id.menu_info:
			startActivity(new Intent(this, InfoActivity.class));
			break;
		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.menu_quit:
			helperLogic.trackerButtonCategory("Quit (Menu)");
			android.os.Process.killProcess(android.os.Process.myPid());
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyNode, KeyEvent event) {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (main) " + "On Key Down");
		}
		if (event.getAction() == KeyEvent.KEYCODE_BACK) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		return super.onKeyDown(keyNode, event);
	}

	/**
	 * Called when startActivityForResult() call is completed. The result of
	 * activation could be success of failure, mostly depending on user
	 * accepting this app's request to administer the device.
	 */

	void notifyDualSimCase() {
		SimData sd = new SimData(this);
		if (sd.getSimCount() == 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.alert_title_dual_sim));
			builder.setMessage(getResources().getString(R.string.alert_massage_dual_sim));
			builder.setPositiveButton(getResources().getText(R.string.continue_button),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							helperLogic.hideApplication();

						}
					});

			builder.setNegativeButton(getResources().getText(R.string.cancel_button),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
						}
					});

			builder.create().show();
		}
	}

	//
	// Saves configurations and hides application.
	//
	public void prepareToSave() {
		// Don't allow save without registered SIM
		if (helperLogic.getSimData().getSimSerialNumbers().length > 0) {
			// Checks fields and stores full phone number(with country code) in
			// mTrustedNumber global variable
			if (checkFields()) {
				helperLogic.saveInfoInDatabase(userField.getText().toString(), mTrustedNumber,
						userEmailField.getText().toString());
				helperLogic.showHideIconMessage();
			}
		} else {
			showToast(R.string.toast_no_sim_inserted, true);
		}
	}

	//
	// Checks the "Owner Name" and "Trusted Number" fields content. Returns true
	// if everything is OK, in other cases - false.
	//
	//
	private boolean checkFields() {
		String ownerName = userField.getText().toString();
		String trustedNumber = trustedUserPhoneNumber.getText().toString();
		if (ownerName.trim().length() == 0) {
			// "Owner Name" field is empty
			showToast(R.string.toast_on_owner_name, false);
			return false;
		}
		if (trustedNumber.trim().length() == 0) {
			// "Trusted Number" field is empty
			showToast(R.string.toast_on_trusted_number, true);
			return false;
		}

		if (!checkEmailField(userEmailField.getText().toString())) {
			showToast(R.string.toast_on_invalid_email, false);
			return false;
		}
		if (!checkTrustedNumberField()) {
			return false;
		}
		return true;
	}

	public final static boolean checkEmailField(CharSequence target) {
		if (TextUtils.isEmpty(target)) {
			return true;
		} else {
			return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
		}
	}

	//
	// Checks the trusted field and stores full phone number in mTrustedNumber
	// global variable.
	//
	private boolean checkTrustedNumberField() {
		String trustedNumber = trustedUserPhoneNumber.getText().toString();
		trustedNumber = trustedNumber.trim();
		if (!trustedNumber.matches("^[+0-9(][0-9 ()-]+$")) {
			// Entered number is not phone number.
			showToast(R.string.toast_on_incorrect_trusted_number, false);
			return false;
		}
		if (trustedNumber.length() < TRUSTED_NUMBER_MIN_LENGHT) {
			// "Trusted Number" field content is too short
			showToast(R.string.toast_on_short_trusted_number, false);
			return false;
		}

		String selectedCountryCode = ((Country) countrySpinner.getSelectedItem()).getPhoneCode();
		// If selected "Other" from country list
		if (selectedCountryCode.equals("")) {
			mTrustedNumber = trustedUserPhoneNumber.getText().toString();
			return true;
		}
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		String number = selectedCountryCode + trustedUserPhoneNumber.getText().toString();
		PhoneNumber numberProto;
		try {
			numberProto = phoneUtil.parse(number, null);
			String countryIsoCode = ((Country) countrySpinner.getSelectedItem()).getIsoCode();
			mTrustedNumber = "+" + numberProto.getCountryCode() + numberProto.getNationalNumber();
			// If selected "Other" from country spinner
			if (countryIsoCode.equals("")) {
				if (phoneUtil.isValidNumber(numberProto)) {
					return true;
				} else {
					showToast(R.string.toast_on_incorrect_trusted_number, true);
					return false;
				}
			} else {
				if (phoneUtil.isValidNumberForRegion(numberProto, countryIsoCode)) {
					return true;
				} else {
					showToast(R.string.toast_on_incorrect_trusted_number, true);
					return false;
				}
			}
		} catch (NumberParseException e) {
			showToast(R.string.toast_on_incorrect_trusted_number, true);
			System.err.println("NumberParseException was thrown: " + e.toString());
			return false;
		}
	}

	//
	// Shows toast with text from set resId of String resource.
	// If duratinLong is set true duration will be long, else short.
	//
	private void showToast(int resId, boolean durationLong) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = Toast.makeText(this, getResources().getString(resId), Toast.LENGTH_SHORT);
		mToast.show();
	}

	//
	// Initializes class members.
	//
	private void initialize() {
		helperLogic = new MainActivityLogic(this);
		mContext = this;
		checkKillAplication = true;
		checkChangeMainField = true;
		checkOldversion = true;
		alertDialogBuilder = new AlertDialog.Builder(this);
		helperLogic.setSimData(new SimData(this));

		// Check Old Database
		if (checkDataBase(SIM_SERIAL_DATABASE)) {
			helperLogic.transferSimData();
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) Old DataBase exist");
			}
		}
		// Owner list and trusted Numbers.
		if (checkDataBase(OWNER_NAME_DATABASE) && checkDataBase(PHONE_NUMBER_DATABASE)) {
			alertDialogBuilder.setMessage(this.getResources().getString(R.string.alert_massage_old_data))
					.setCancelable(false).setPositiveButton(getResources().getText(R.string.no_button),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									helperLogic.trackerButtonCategory("Old Data Save: NO");
									helperLogic.transferAllData(false);
									dialog.cancel();
								}
							});
			alertDialogBuilder.setNegativeButton(getResources().getText(R.string.yes_button),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							helperLogic.trackerButtonCategory("Old Data Save: YES");
							helperLogic.transferAllData(true);
						}
					});
			AlertDialog alert = alertDialogBuilder.create();
			alert.show();
		} else {
			if (SHOW_LOGS) {
				Log.i("LOG_I: (main) No Old DataBase");
			}
		}
		mTelephoneManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// Getting Sim Serial Number and Phone Number
		helperLogic.getSimSerialNumber();
		helperLogic.getSimPhoneNumber();
		helperLogic.initializePreferences();
	}

	//
	// Checks that the database with set name exists or not.
	//
	private boolean checkDataBase(String dbName) {
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			dbPath = getApplicationContext().getApplicationInfo().dataDir + "/databases/";
		} else {
			dbPath = getDatabasePath(dbName).getParent() + "/";
		}
		File dbFile = new File(dbPath + dbName);
		return dbFile.exists();
	}

	//
	// Creates UI components and configures them.
	//
	private void createComponents() {
		userField = (EditText) findViewById(R.id.userField);
		trustedUserPhoneNumber = (EditText) findViewById(R.id.userPhoneField);
		testButton = (Button) findViewById(R.id.testNumber);
		saveButton = (Button) findViewById(R.id.saveButton);
		infoButton = (Button) findViewById(R.id.infoButton);
		settingsButton = (Button) findViewById(R.id.settingButton);
		userEmailField = (EditText) findViewById(R.id.userEmailField);
		// Text Edit
		trustedUserPhoneNumber.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (!trustedUserPhoneNumber.getText().toString().contentEquals("+") && checkChangeMainField == true
						&& trustedUserPhoneNumber.getText().length() == 1) {
					checkChangeMainField = false;
				}

				if (trustedUserPhoneNumber.getText().length() == 0) {
					checkChangeMainField = true;
				}
			}
		});
		testButton.setOnClickListener(this);
		settingsButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		infoButton.setOnClickListener(this);
		trustedUserPhoneNumber.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				inputMethodManager.hideSoftInputFromWindow(trustedUserPhoneNumber.getWindowToken(), 0);
				return false;
			}
		});
		UserData userData = new UserData(this);
		mTrustedOwner = userData.getUserNames()[0];
		mTrustedNumber = userData.getUserPhoneNumbers()[0];

		if (!mTrustedOwner.equals("")) {
			userField.setText(mTrustedOwner);
		}
		String selectedCountryIsoCode = ((Country) countrySpinner.getSelectedItem()).getIsoCode();
		// If selected Other from country list
		if (selectedCountryIsoCode.equals("")) {
			trustedUserPhoneNumber.setText(mTrustedNumber);
		} else {
			if (!mTrustedNumber.equals("")) {
				PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
				try {
					PhoneNumber numberProto = phoneUtil.parse(mTrustedNumber, null);
					trustedUserPhoneNumber.setText(numberProto.getNationalNumber() + "");
				} catch (NumberParseException e) {
					e.printStackTrace();
					trustedUserPhoneNumber.setText("");
				}
			}
		}
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		userEmailField.setText(pref.getString("prefEmailField", ""));
	}

	//
	// Initializes "Test" button with set title.
	//
	public static void initTestButton(boolean succes) {
		if (succes) {
			testButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.message_sent, 0, 0, 0);
		} else {
			testButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.message_clicked, 0, 0, 0);
		}
		testButton.setClickable(true);
	} 

	//
	// Returns true if there is SIM card inserted.
	//
	public boolean isSimInserted() {
		assert (null != mTelephoneManager);
		return (mTelephoneManager.getSimState() == TelephonyManager.SIM_STATE_READY);
	}

	public class countryListAdapter extends ArrayAdapter<Country> {
		public countryListAdapter(Context context, int textViewResourceId, ArrayList<Country> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			CountryViewHolder holder = null;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.country_dropdown, parent, false);
				holder = new CountryViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.country_name_with_code);
				holder.flag = (ImageView) convertView.findViewById(R.id.country_flag);
				convertView.setTag(holder);
			} else {
				holder = (CountryViewHolder) convertView.getTag();
			}
			if (position == countries.size() - 1) {
				holder.name.setText(countries.get(position).getName());
				holder.flag.setImageResource(android.R.color.transparent);
			} else {
				holder.name.setText(
						countries.get(position).getName() + " (" + countries.get(position).getPhoneCode() + ")");
				// ImageView countryFlag = (ImageView)
				// countryRow.findViewById(R.id.country_flag);
				holder.flag.setImageResource(countries.get(position).getFlag());
			}
			return convertView;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CountryViewHolder holder = null;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.country_row, parent, false);
				holder = new CountryViewHolder();
				holder.code = (TextView) convertView.findViewById(R.id.country_code);
				holder.flag = (ImageView) convertView.findViewById(R.id.country_flag);
				convertView.setTag(holder);
			} else {
				holder = (CountryViewHolder) convertView.getTag();
				holder.flag.setImageResource(android.R.color.transparent);
			}
			if (position == countries.size() - 1) {
				holder.code.setText(countries.get(position).getName());
			} else {
				holder.code.setText(countries.get(position).getPhoneCode());
				holder.flag.setImageResource(countries.get(position).getFlag());
			}
			return convertView;
		}

	}

	static class CountryViewHolder {
		ImageView flag;
		TextView code;
		TextView name;
	}
}
