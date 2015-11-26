package instigate.simCardChangeNotifier.logic;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.OWNER_NAME;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.USER_PHONE_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.USER_TABLE;
import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.database.SimCardChangeNotifierData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.analytics.tracking.android.Log;

/**
 * This class is a model that contains the trusted user data to be saved in the
 * database
 */
public class UserData {
	public static SimCardChangeNotifierData simCardChangeNotifierData;
	public static SQLiteDatabase dbRead, dbWrite;

	public UserData(Context Context) {
		simCardChangeNotifierData = new SimCardChangeNotifierData(Context);
		dbWrite = simCardChangeNotifierData.getWritableDatabase();
		dbRead = simCardChangeNotifierData.getReadableDatabase();
	}

	//
	// Deletes the user data with given name. The function returns true in
	// success or false if the record wasn't found.
	//
	private boolean deleteUserData(String fieldName, String deletedData) {
		return dbWrite.delete(USER_TABLE, fieldName + " = \"" + deletedData + "\"", null) > 0;
	}

	//
	// Returns user data with given field name.
	//
	private String[] getUserData(String fieldName) {
		String userReturnData;
		String query = "SELECT " + fieldName + " FROM " + USER_TABLE;
		String[] userDataArray = null;
		try {
			Cursor cursor = dbWrite.rawQuery(query, null);
			int i = 0;
			if (cursor == null || cursor.getCount() <= 0) {
				userDataArray = new String[1];
				userDataArray[0] = "";
				return userDataArray;
			}
			userDataArray = new String[cursor.getCount()];
			while (cursor.moveToNext()) {
				userReturnData = cursor.getString(0).toString();
				userDataArray[i] = userReturnData;
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (userDataArray == null) {
			userDataArray = new String[1];
			userDataArray[0] = "";
		}

		return userDataArray;
	}

	//
	// Adds new user data in database with given name and phone number.
	//
	public long addUserData(String userName, String userPhoneNumber) {
		ContentValues values = new ContentValues();
		values.put(OWNER_NAME, userName);
		values.put(USER_PHONE_NUMBER, userPhoneNumber);
		return dbWrite.insert(USER_TABLE, null, values);
	}

	//
	// Returns user phone number from database.
	//
	public String[] getUserPhoneNumbers() {
		return getUserData(USER_PHONE_NUMBER);
	}

	//
	// Returns user name from database.
	//
	public String[] getUserNames() {
		return getUserData(OWNER_NAME);
	}

	//
	// Deletes user phone number from database.
	//
	public boolean deleteUserByPhoneNumber(String phoneNumber) {
		return deleteUserData(USER_PHONE_NUMBER, phoneNumber);
	}

	//
	// Deletes user name from database.
	//
	public boolean deleteUserByName(String userName) {
		return deleteUserData(OWNER_NAME, userName);
	}

	//
	// Deletes all users from database.
	//
	public boolean deleteAllUsers() {
		return dbWrite.delete(USER_TABLE, null, null) > 0;
	}

	//
	// Closes the database.
	//
	public void close() {
		if (SHOW_LOGS) {
			Log.i("LOG_I: (UserData)  Closing DataBase ");
		}
		simCardChangeNotifierData.close();
	}
}
