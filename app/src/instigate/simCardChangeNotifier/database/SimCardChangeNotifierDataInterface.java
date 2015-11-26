package instigate.simCardChangeNotifier.database;

/**
 * Created by Instigate Mobile on 10/24/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

import android.provider.BaseColumns;

/**
 * Following interface contain table and column names for Sim card and User
 * info.
 */
public interface SimCardChangeNotifierDataInterface extends BaseColumns {

	// Tables.
	public static final String SIM_TABLE = "tb_Sim";
	public static final String USER_TABLE = "tb_UserName";

	// Columns.
	public static final String SIM_SERIAL_NUMBER = "simSerialNumbers";
	public static final String SIM_PHONE_NUMBER = "simPhoneNumbers";
	public static final String SIM_INSERT_TIME = "simTimeStamp";

	public static final String OWNER_NAME = "userNames";
	public static final String USER_PHONE_NUMBER = "userPhoneNumbers";
}
