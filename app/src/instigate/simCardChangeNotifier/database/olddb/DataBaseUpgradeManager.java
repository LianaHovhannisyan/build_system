package instigate.simCardChangeNotifier.database.olddb;

/**
 * Created by Instigate Mobile on 11/6/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.analytics.tracking.android.Log;

/**
 * Backup all old data when application is updated.
 */
public class DataBaseUpgradeManager extends SQLiteOpenHelper {

	private Context context;
	private SQLiteDatabase dataBase;
	private static final int DATABASE_VERSION = 1;

	public DataBaseUpgradeManager(Context context, String dbName) {
		super(context, dbName, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
	}

	//
	// Returns data from the "tableName" table's "columnName" column.
	//
	public String[] getData(String tableName, String columnName) {
		String[] data;
		try {
			dataBase = this.getReadableDatabase();
			String query = "SELECT " + columnName + " FROM " + tableName;
			Cursor c = dataBase.rawQuery(query, null);
			data = new String[c.getCount()];
			if (SHOW_LOGS) {
				Log.i("LOG_I: (DataBaseUpgradeManager) Old Data Count: " + c.getCount());
			}
			int i = 0;
			while (c.moveToNext()) {
				data[i] = c.getString(0).toString();
				i++;
			}
		} catch (SQLiteException e) {
			data = null;
			if (SHOW_LOGS) {
				Log.i("LOG_I: (DataBaseUpgradeManager) Exception SQL returning existing old data " + "returned Null! : "
						+ e);
			}
			e.printStackTrace();
		}
		return data;
	}

	//
	// Removes the database with "dbName" name.
	//
	public boolean removeDB(String dbName) {
		return context.deleteDatabase(dbName);
	}
}
