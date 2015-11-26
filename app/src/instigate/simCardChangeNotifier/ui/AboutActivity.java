package instigate.simCardChangeNotifier.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import instigate.simCardChangeNotifier.R;

public class AboutActivity extends ActionBarActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		TextView appVersion = (TextView) findViewById(R.id.appVersion);
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			appVersion.setText(getResources().getString(R.string.app_version, pInfo.versionName));
		} catch (NameNotFoundException e) {
			appVersion.setText("");
			e.printStackTrace();
		}
		Button appUrl = (Button) findViewById(R.id.appUrl);
		Button supportUrl = (Button) findViewById(R.id.supportEmail);
		Button rateUsUrl = (Button) findViewById(R.id.rateUsUrl);
		appUrl.setOnClickListener(this);
		supportUrl.setOnClickListener(this);
		rateUsUrl.setOnClickListener(this);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		String url = "";
		switch (v.getId()) {
		case R.id.appUrl:
			url = getResources().getString(R.string.app_url);
			break;
		case R.id.supportEmail:
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setType("text/plain");
			intent.setData(Uri.parse("mailto:info@instigatedesign.com"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			break;
		case R.id.rateUsUrl:
			url = getResources().getString(R.string.rate_us_url);
			break;
		default:
			break;
		}
		if (!url.equals("")) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
		} else {
			
		}
	}

}
