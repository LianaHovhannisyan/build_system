package instigate.simCardChangeNotifier.listeners;

/**
 * Created by Instigate Mobile on 12/20/14.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import instigate.simCardChangeNotifier.logic.SharedData;
import instigate.simCardChangeNotifier.services.EmailNotificationsService;
import instigate.simCardChangeNotifier.services.SimCardCheckerService;
import instigate.simCardChangeNotifier.ui.MainActivity;
import instigate.simCardChangeNotifier.ui.MainActivityLogic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * The class listens SIM card state changes and starts SimCardCheckerService. It
 * is designed for receiving the case when user changes SIM card of the mobile
 * phone without turning off the phone.
 */
public class SimStateChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}
		// Checks whether SIM card is ready.
		String extraKey = intent.getStringExtra("ss");
		if (extraKey != null && extraKey.equals("LOADED")) {
			PackageManager pm = context.getPackageManager();
			// Checks whether app is hidden and activated.
			if (PackageManager.COMPONENT_ENABLED_STATE_DISABLED != (pm
					.getComponentEnabledSetting(new ComponentName(context, MainActivity.class)))) {
				// Sends update request to MainActivity.
				LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(context);
				Intent updateIntent = new Intent(SharedData.UPDATE_REQUEST);
				updateIntent.putExtra(SharedData.UPDATE_REQUEST, "Update SIM serial numbers list");
				broadcaster.sendBroadcast(updateIntent);	
			}
			// Initializing each time when SIM card is changed and then in
			// SMSBroadcastReceivr checking case when trusted number is inserted
			// into owners device. In this case app send SMS to itself.
			SharedData.isTrustedInserted = false;
			// Checking whether SIM card is registered or no and sends
			// notification SMSes if needed.
			Intent startupIntent = new Intent(context, SimCardCheckerService.class);
			context.startService(startupIntent);
			Intent startEmailServiceIntent = new Intent(context, EmailNotificationsService.class);
			context.startService(startEmailServiceIntent);
		}
	}

}
