package instigate.simCardChangeNotifier.logic;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_INSERT_TIME;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_PHONE_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_SERIAL_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_TABLE;
import static instigate.simCardChangeNotifier.logic.SharedData.SHOW_LOGS;
import instigate.simCardChangeNotifier.database.SimCardChangeNotifierData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.analytics.tracking.android.Log;

/**
 * This class is a model that contains the the SIM card data to be saved in the
 * database and show in the user interface.
 */
public class SimData {
    public static SimCardChangeNotifierData simCardChangeNotifierData;
    public static SQLiteDatabase dbRead, dbWrite;
    
    public SimData(Context Context) {
        simCardChangeNotifierData = new SimCardChangeNotifierData(Context);
        dbWrite = simCardChangeNotifierData.getWritableDatabase();
        dbRead = simCardChangeNotifierData.getReadableDatabase();
    }
    
    //
    // Adds new SIM data with given parameters to database.
    //
    public long addSimData(String simPhoneNumber, String simSerialNumber) {
        ContentValues values = new ContentValues();
        values.put(SIM_PHONE_NUMBER, simPhoneNumber);
        values.put(SIM_SERIAL_NUMBER, simSerialNumber);
        return dbWrite.insert(SIM_TABLE, null, values);
    }
    
    //
    // Returns SIM serial number from database or "No SIM Serial Number" if doesn't found in database.
    //
    public String getDate(String mSimSerialNumber) {
        String query = "SELECT " + SIM_INSERT_TIME + " FROM " + SIM_TABLE + " WHERE "
                + SIM_SERIAL_NUMBER + " = \"" + mSimSerialNumber + "\"";
        Cursor cursor = dbRead.rawQuery(query, null);
        if (cursor.getCount() > 0) {
            cursor.moveToNext();
            // Only one SIM serial number (UNIQUE!).
            return cursor.getString(0).toString();
        }
        return "No SIM Serial Number";
    }

    //
    // Returns true if "mSimSerialNumber" number exists in owner's registered
    // SIM card's list.
    //
    public boolean similarSerialNumber(String mSimSerialNumber) {
        String query = "SELECT " + SIM_SERIAL_NUMBER + " FROM " + SIM_TABLE + " WHERE "
                + SIM_SERIAL_NUMBER + " like \"" + mSimSerialNumber + "\"";
        Cursor cursor = dbRead.rawQuery(query, null);
        if (SHOW_LOGS) {
            Log.i("LOG_I: (SimData) Found Similar Numbers: " + cursor.getCount()
                    + "  For Serial:  " + mSimSerialNumber);
        }
        if (cursor.getCount() > 0) {
            return true;
        }
        return false;
    }

    //
    // Returns the array of registered SIM cards serial numbers.
    //
    public String[] getSimSerialNumbers() {
        String serialNumber;
        String query = "SELECT " + SIM_SERIAL_NUMBER + " FROM " + SIM_TABLE;
        Cursor cursor = dbWrite.rawQuery(query, null);
        String[] serialNumbers = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            serialNumber = cursor.getString(0).toString();
            serialNumbers[i] = serialNumber;
            i++;
        }
        return serialNumbers;
    }
    public int getSimCount() {
        return getSimSerialNumbers().length;
    }
    
    //
    // Deletes the given serial number from the registered SIM cards.
    //
    public boolean deleteSimSerialNumber(String serialNumber) {
        return dbWrite.delete(SIM_TABLE, SIM_SERIAL_NUMBER + "= \"" + serialNumber + "\"", null) > 0;
    }

    //
    // Closes the database of SimData.
    //
    public void close() {
        if (SHOW_LOGS) {
            Log.i("LOG_I: (SimData)  Closing DataBase ");
        }
        simCardChangeNotifierData.close();
    }
    
    //
    // Deletes all registered SIM cards.
    //
    public boolean deleteAllSimData() {
        return dbWrite.delete(SIM_TABLE, null, null) > 0;
    }

}
