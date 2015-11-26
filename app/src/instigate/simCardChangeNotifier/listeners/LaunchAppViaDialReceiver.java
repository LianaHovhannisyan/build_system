package instigate.simCardChangeNotifier.listeners;


import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;
import java.util.HashMap;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.ui.MainActivity;

/**
 * The class reactivates the Application after by dialing the specified number.
 */
public class LaunchAppViaDialReceiver extends BroadcastReceiver {
    private Tracker tracker;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null){
            return;
        }        
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (phoneNumber==null) return;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String unHideNumber = settings.getString("unhide_number", ""); 
        if (phoneNumber.equals(unHideNumber)) {
            setResultData(null);
            tracker = GoogleAnalytics.getInstance(context).getTracker(
                    context.getResources().getString(R.string.google_tracking_id));
            sendReceiver();
            // Making app visible in the applications menu. 
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, MainActivity.class);
            packageManager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            // Opening MainActivity. 
            Intent appIntent = new Intent(context, MainActivity.class);
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent);
        }
    }
    //
    // Sending data to Google Analytics.
    //
    private void sendReceiver() {
    	if (DEBUG) {
    		return;
    	}
        HashMap<String, String> hitParameters = new HashMap<String, String>();
        hitParameters.put(Fields.HIT_TYPE, "appview");
        hitParameters.put(Fields.SCREEN_NAME, "Return Application Via Dial Receiver");
        tracker.send(hitParameters);
        tracker.send(MapBuilder.createEvent("Dial Manager", "Application Icon shown", null, null)
                .build());
        tracker.set(Fields.SCREEN_NAME, null);
    }
}
