package instigate.simCardChangeNotifier.database.olddb;

/**
 * Created by Instigate Mobile on 11/7/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

/**
 * Old DataBase structure.
 */
public interface OldDataBaseInterface {
    // Serial Number.
    public static final String SIM_SERIAL_DATABASE = "simSerialNumber.db";
    public static final String SIM_SERIAL_TABLE = "tb_SimSerialNumbers";
    public static final String SIM_SERIAL_COLUMN = "simSerialNumbers";
    // Owner name.
    public static final String OWNER_NAME_DATABASE = "username.db";
    public static final String OWNER_NAME_TABLE = "tb_UserName";
    public static final String OWNER_NAME_COLUMN = "userName";
    // Trusted Number.
    public static final String PHONE_NUMBER_DATABASE = "phoneNumbers.db";
    public static final String PHONE_NUMBER_TABLE = "tb_PhoneNumbers";
    public static final String PHONE_NUMBER_COLUMN = "phoneNumber";
}
