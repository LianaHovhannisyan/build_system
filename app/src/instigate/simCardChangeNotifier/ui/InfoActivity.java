package instigate.simCardChangeNotifier.ui;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.R.id;
import instigate.simCardChangeNotifier.R.layout;
import instigate.simCardChangeNotifier.R.menu;

public class InfoActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		TextView infoTextView = (TextView) findViewById(R.id.infoTextView);		
		infoTextView.setMovementMethod(new ScrollingMovementMethod());
		String infoText = getResources().getString(R.string.about_text);
		infoTextView.setText(Html.fromHtml(infoText));
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
}
