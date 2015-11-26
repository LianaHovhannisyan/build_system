package instigate.simCardChangeNotifier.listeners;

/**
 * Created by Instigate Mobile on 10/31/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import instigate.simCardChangeNotifier.logic.SharedData;

import com.google.analytics.tracking.android.Log;

/**
 * This is the component that is responsible for actual device administration.
 * It becomes the receiver when a policy is applied. It is important that we
 * subclass DeviceAdminReceiver class here and to implement its only required
 * method onEnabled().
 */
public class CustomDeviceAdminReceiver extends DeviceAdminReceiver {

	/**
	 * Called when this application is approved to be a device administrator.
	 */
	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
		SharedData.ADMIN_PERMISSION_ENABLED = true;
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomDeviceAdminReceiver) " + " onEnabled");
		}
	}

	/**
	 * Called when this application is no longer the device administrator.
	 */
	@Override
	public void onDisabled(Context context, Intent intent) {
		super.onDisabled(context, intent);
		SharedData.ADMIN_PERMISSION_ENABLED = false;
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomDeviceAdminReceiver) " + "onDisabled");
		}
	}

	@Override
	public void onPasswordChanged(Context context, Intent intent) {
		super.onPasswordChanged(context, intent);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomDeviceAdminReceiver) " + "onPasswordChanged");
		}
	}

	@Override
	public void onPasswordFailed(Context context, Intent intent) {
		super.onPasswordFailed(context, intent);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomDeviceAdminReceiver) " + "onPasswordFailed");
		}
	}

	@Override
	public CharSequence onDisableRequested(Context context, Intent intent) {
		return super.onDisableRequested(context, intent);
	}

	@Override
	public void onPasswordSucceeded(Context context, Intent intent) {
		super.onPasswordSucceeded(context, intent);
		if (SHOW_LOGS) {
			Log.i("LOG_I: (CustomDeviceAdminReceiver) " + "onPasswordSucceeded");
		}
	}

}