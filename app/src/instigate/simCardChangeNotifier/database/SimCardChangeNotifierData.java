package instigate.simCardChangeNotifier.database;

import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.OWNER_NAME;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_INSERT_TIME;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_PHONE_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_SERIAL_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.SIM_TABLE;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.USER_PHONE_NUMBER;
import static instigate.simCardChangeNotifier.database.SimCardChangeNotifierDataInterface.USER_TABLE;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

/** 
 * Class for creating database (SQLite) and two tables for User and Sim Card
 * information store.
 */

public class SimCardChangeNotifierData extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "simCardChangeNotifier.db";
    private static final int DATABASE_VERSION = 1;

    public SimCardChangeNotifierData(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Table for store/add Active sim cards serial number and phone number.
        sqLiteDatabase.execSQL("CREATE TABLE " + SIM_TABLE + " ( "
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + SIM_PHONE_NUMBER + " TEXT NOT NULL, "
                + SIM_SERIAL_NUMBER + " TEXT NOT NULL UNIQUE, " + SIM_INSERT_TIME
                + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP " + ");");

        // Table for store Specified User name and User's number.
        sqLiteDatabase.execSQL("CREATE TABLE " + USER_TABLE + " ( "
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + OWNER_NAME + " TEXT NOT NULL UNIQUE, "
                + USER_PHONE_NUMBER + " TEXT NOT NULL UNIQUE " + ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SIM_TABLE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + USER_TABLE);
        onCreate(sqLiteDatabase);
    }
}
