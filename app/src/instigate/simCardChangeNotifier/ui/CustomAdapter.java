package instigate.simCardChangeNotifier.ui;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.R;
import instigate.simCardChangeNotifier.logic.SimData;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Log;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/** 
 * The class is adopting each row in the List View(Registered Serial Number's
 * List).
 */
public class CustomAdapter extends ArrayAdapter<String> {
    private SimData simData;
    private final Context context;
    private ArrayList<String> values;
    private View rowView;
    private ImageButton deleteImageButton;
    private Tracker tracker;
    private String activeSimSerial = "";
    private AlertDialog.Builder alertDialogBuilder;

    public CustomAdapter(Context context, ArrayList<String> values) {
        super(context, R.layout.row_layout, values);
        this.simData = new SimData(context);
        this.context = context;
        this.values = values;
        tracker = GoogleAnalytics.getInstance(context).getTracker(
                context.getResources().getString(R.string.google_tracking_id));
        activeSimSerial = getActiveSimSerial();
        alertDialogBuilder = new AlertDialog.Builder(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (SHOW_LOGS) {
            Log.i("LOG_I: (CustomAdapter) " + "Get View ");
        }
        // For update DataBase row in every getView calling.
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rowView = inflater.inflate(R.layout.row_layout, parent, false);
        if (position < getCount()) {
            // Checks for null items.
            String item = values.get(position);
            if (item != null) {
                TextView textView = (TextView) rowView.findViewById(R.id.label);
                TextView textViewSim = (TextView) rowView.findViewById(R.id.labelsim);
                // This button is designed for deleting specified SIM card serial number from database.
                deleteImageButton = (ImageButton) rowView.findViewById(R.id.icon);
                textViewSim.setText("SIM: " + (position + 1) + " ");
                textView.setText(" ID: " + values.get(position));
                textView.setTextColor(Color.parseColor("#96890D"));
                deleteImageButton.setTag(position);
                deleteImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int id = Integer.parseInt(v.getTag().toString());
                        deleteSerial(id);
                    }
                });
                if (values.get(position).equals(activeSimSerial)) {
                    textView.setTextColor(Color.parseColor("#FEF9FF"));
                    textView.setTypeface(null, Typeface.BOLD);
                }
            } else {
                View v = new View(context);
                v.setLayoutParams(new AbsListView.LayoutParams(0, 0));
                return v;
            }
        }
        return rowView;
    }

    //
    // Returns the array of registered SIM card's serial numbers.
    //
    public String[] getValues() {
        String[] values = simData.getSimSerialNumbers();
        return values;
    }

    //
    // Returns current SIM card's serial number.
    //
    public String getActiveSimSerial() {
        TelephonyManager mTelephoneManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String str = mTelephoneManager.getSimSerialNumber();
        String simSerial;
        if (str != null && !str.equals("")) {
            simSerial = str;
        } else {
            simSerial = "empty";
        }
        return simSerial;
    }

    //
    // Shows the confirm dialog and deletes specified SIM serial number from
    // database in case of user's confirm.
    //
    private void deleteSerial(int serial_id) {
        final int id = serial_id;
        if (simData.getSimSerialNumbers().length > 1) {
            alertDialogBuilder
                    .setMessage(context.getResources().getString(R.string.alert_dialog_massage))
                    .setCancelable(true)
                    .setPositiveButton(context.getResources().getText(R.string.no_button), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int idClick) {
                            tracker.send(MapBuilder.createEvent("UI Buttons",
                                    "Delete Serial: Canceled", null, null).build());
                            dialog.cancel();
                        }
                    });
            alertDialogBuilder.setNegativeButton(context.getResources().getText(R.string.yes_button), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int idClick) {
                    tracker.send(MapBuilder.createEvent("UI Buttons", "Delete Serial: Confirmed",
                            null, null).build());
                    if (SHOW_LOGS) {
                        Log.i("LOG_I: (CustomAdapter) " + "values.get(id)" + " Delete Status: "
                                + "simData.deleteSimSerialNumber(values.get(id))");
                    }
                    values.get(id);
                    simData.deleteSimSerialNumber(values.get(id));
                    values.remove(id);
                    notifyDataSetChanged();
                }
            });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else {
            if (SHOW_LOGS) {
                Log.i("LOG_I: (CustomAdapter) " + " only 1. ");
            }
            Toast.makeText(context,
                    context.getResources().getString(R.string.error_deleting_massage),
                    Toast.LENGTH_LONG).show();
        }
    }
}
