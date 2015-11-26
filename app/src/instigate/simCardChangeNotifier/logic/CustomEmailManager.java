package instigate.simCardChangeNotifier.logic;

import static instigate.simCardChangeNotifier.logic.SharedData.DEBUG;
import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.mailing.CustomEmailSender;

public class CustomEmailManager extends Service {

	private Context context;
	private Tracker tracker;
	private String owner;
	private String trustedNumber;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public CustomEmailManager(Context context, String owner, String trustedNumber) {
		this.context = context;
		this.owner = owner;
		this.trustedNumber = trustedNumber;
		tracker = GoogleAnalytics.getInstance(context)
				.getTracker(context.getResources().getString(R.string.google_tracking_id));
		GoogleAnalytics.getInstance(context).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
	}

	//
	// Sends notification email which informs that SIM card is changed and also
	// includes the list of remote commands.
	//
	public void sendEmailNotification() {
		trackEmailType("Send", "Notification (with password)");
		// Getting unlock password.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String password = settings.getString("unlock_password", "");
		// Getting message content from resources.
		String contentText = context.getResources().getString(R.string.notification_email, owner, trustedNumber,
				password);
		sendMail(contentText);
	}

	private String getEmailTemplate(String content) {
		String messageText = "";
		// Getting Logo URL to show in Email.
		// Getting Application Name.
		String applicationName = context.getResources().getString(R.string.app_name);
		// Getting website button's URL and text
		String websiteTitle = context.getResources().getString(R.string.website_title);
		String websiteUrl = context.getResources().getString(R.string.app_url);
		// Getting rateUs button's URL and text
		String rateUsTitle = context.getResources().getString(R.string.rate_us_title);
		String rateUsUrl = context.getResources().getString(R.string.rate_us_url);
		// Getting company Name and website.
		String companyName = context.getResources().getString(R.string.company_name);
		String companyUrl = context.getResources().getString(R.string.developer_url);
		String appSupport = context.getResources().getString(R.string.support_email);
		String logoPicture = context.getResources().getString(R.string.email_logo_id);
		messageText = context.getResources().getString(R.string.email_template, logoPicture, applicationName,
				trustedNumber, content, websiteUrl, websiteTitle, rateUsUrl, rateUsTitle, companyName, companyUrl,
				appSupport);
		return messageText;
	}

	public void sendMail(String contentText) {
		// TODO add comment
		try {
			String emailLogoId = context.getResources().getString(R.string.email_logo_id);
			File f = new File(context.getFilesDir(), emailLogoId);
			if (!f.exists()) {
				InputStream inputStream = context.getResources().openRawResource(R.drawable.icon);
				OutputStream out = new java.io.FileOutputStream(f);
				byte buf[] = new byte[1024];
				int len;
				while ((len = inputStream.read(buf)) > 0)
					out.write(buf, 0, len);
				out.close();
				inputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		Set<String> recipients = new HashSet<>(1);
		String savedRecipient = pref.getString("prefEmailField", "");
		if (!savedRecipient.equals("")) {
			recipients.add(savedRecipient);
			String messageText = getEmailTemplate(contentText);
			new CustomEmailSender(context, recipients, "Sim Card Change Notifier", messageText).execute();
		}
	}

	//
	// Sends email with GPS coordinates and address of device (if available).
	//
	public void sendEmailWithGPSCoordinates(Double latitude, Double longitude) {
		String contentText;
		String address = getAddress(latitude, longitude);
		if (!address.equals("")) {
			trackEmailType("Send", "With GPS Coordinates and Address");
			contentText = context.getResources().getString(R.string.gps_coordinates_email, owner, address, latitude,
					longitude);
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomEmailManager) " + "GPS Coordinates email: " + contentText);
			}
		} else {
			trackEmailType("Send", "With GPS Coordinates");
			contentText = context.getResources().getString(R.string.gps_location_email, owner, latitude, longitude);
			if (SHOW_LOGS) {
				Log.i("LOG_I: (CustomEmailManager) " + "GPS Location email: " + contentText);
			}
		}
		sendMail(contentText);
	}

	//
	// Returns the address of given coordinates or empty string if address is
	// not available.
	//
	private String getAddress(double lati, double longi) {
		String currentAddress = "";
		Geocoder geocoder;
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
				Log.i("LOG_I: (CustomEmailManager) Exception get password (geocoder)" + e);
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
	// Sends data to Google Analytics.
	//
	private void trackEmailType(String category, String type) {
		if (DEBUG) {
			return;
		}
		HashMap<String, String> hitParameters = new HashMap<String, String>();
		hitParameters.put(Fields.HIT_TYPE, "appview");
		hitParameters.put(Fields.SCREEN_NAME, "Custom Email Manager");
		tracker.send(hitParameters);
		tracker.send(MapBuilder.createEvent("Email", category, type, null).build());
		tracker.set(Fields.SCREEN_NAME, null);
	}

}
