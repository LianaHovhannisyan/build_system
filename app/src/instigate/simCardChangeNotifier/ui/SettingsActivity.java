package instigate.simCardChangeNotifier.ui;

/**
 * Created by Instigate Mobile on 11/25/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.listeners.CustomDeviceAdminReceiver;
import instigate.simCardChangeNotifier.logic.SharedData;
import android.R.anim;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.google.analytics.tracking.android.Log;


public class SettingsActivity extends PreferenceActivity {
	// identifies our request
	private final int ADMIN_RIGHTS_ACTIVATION_REQUEST = 47; 
	private ComponentName customDeviceAdmin;

	@SuppressLint("InlinedApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		customDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
		initializeAdminRightsPref();
		this.setTheme(android.R.style.Theme_DeviceDefault);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				switch (key) {
				case "unlock_password":
					if (SHOW_LOGS) {
						Log.i("LOG_I: (Preferences) " + " UNLOCK_PASSWORD_CHANGED: True");
					}
					SharedData.UNLOCK_PASSWORD_CHANGED = true;
					break;
				case "unhide_number":
					if (SHOW_LOGS) {
						Log.i("LOG_I: (Preferences) " + " UNHIDE_NUMBER_CHANGED: True");
					}
					SharedData.UNHIDE_NUMBER_CHANGED = true;
					break;				
				case "prefSmsInterval":
					if (SHOW_LOGS) {
						Log.i("LOG_I: (Preferences) " + " LOCATION_TIME_CHANGED: True");
					}
					SharedData.LOCATION_TIME_CHANGED = true;
					break;
				}
			}
		});
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ADMIN_RIGHTS_ACTIVATION_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				CheckBoxPreference adminRights = (CheckBoxPreference) findPreference("pref_admin_rights");
				CustomEditTextPreference unlockPassword = (CustomEditTextPreference) findPreference("unlock_password");
				adminRights.setChecked(true);
				unlockPassword.setEnabled(true);

				if (SHOW_LOGS) {
					Log.i("LOG_I: (main) " + "Administration enabled!");
				}
			} else {				
				if (SHOW_LOGS) {
					Log.i("LOG_I: (main) " + "Administration enable FAILED!");
				}
			}
			return;
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		Toolbar bar;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
			root.addView(bar, 0); // insert at top
		} else {
			ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
			ListView content = (ListView) root.getChildAt(0);

			root.removeAllViews();

			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);

			int height;
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
				height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
			} else {
				height = bar.getHeight();
			}

			content.setPadding(0, height, 0, 0);
			root.addView(content);
			root.addView(bar);
		}

		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void initializeAdminRightsPref() {
		CheckBoxPreference adminRights = (CheckBoxPreference) findPreference("pref_admin_rights");
		CustomEditTextPreference unlockPassword = (CustomEditTextPreference) findPreference("unlock_password");
		DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
		adminRights.setChecked(dpm.isAdminActive(customDeviceAdmin));
		unlockPassword.setEnabled(dpm.isAdminActive(customDeviceAdmin));
		adminRights.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				CheckBoxPreference adminRights = (CheckBoxPreference) preference;
				if (!adminRights.isChecked()) {
					Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
					intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, customDeviceAdmin);
					String explanation = getResources().getString(R.string.admin_rights_explanation);
					intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
					startActivityForResult(intent, ADMIN_RIGHTS_ACTIVATION_REQUEST);
					return false;
				}
				DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
				dpm.removeActiveAdmin(customDeviceAdmin);
				CustomEditTextPreference unlockPassword = (CustomEditTextPreference) findPreference("unlock_password");
				unlockPassword.setEnabled(false);
				return true;
			}
		});
	}
}