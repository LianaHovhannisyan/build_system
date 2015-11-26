package instigate.simCardChangeNotifier.mailing;

import java.util.Set;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import instigate.simCardChangeNotifier.BuildConfig;

public class CustomEmailSender extends AsyncTask<Void, Void, Boolean> {

	private Set<String> messageRecipients;
	private String messageSubject;
	private String messageBody;
	private Context context;
	GMailSender sender = new GMailSender(context, "notification.sender.sccn@gmail.com", "InstigateMobile1");

	public CustomEmailSender(Context cnt, Set<String> recipients, String subject, String body) {
		messageRecipients = recipients;
		messageSubject = subject;
		messageBody = body;
		context = cnt;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (BuildConfig.DEBUG)
			Log.v(CustomEmailSender.class.getName(), "doInBackground()");
		try {
			for (String i : messageRecipients) {
				GMailSender sender = new GMailSender(context, "notification.sender.sccn@gmail.com", "InstigateMobile1");
				sender.sendMail(messageSubject, messageBody, "notification.sender.sccn@gmail.com", i);
				Log.d("SendMail", "Mail was sent");
			}
		} catch (Exception e) {
			Log.d("SendMail", e.getMessage(), e);
			e.printStackTrace();
			return false;
		}
		return true;
	}

};