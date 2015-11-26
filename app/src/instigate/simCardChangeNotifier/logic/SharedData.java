package instigate.simCardChangeNotifier.logic;

/**
 * Created by Instigate Mobile on 11/15/13.
 * Copyright (c) 2015 Instigate Mobile. All rights reserved.
 */

/**
 * This class contains shared data which can use all classes of this
 * application.
 */
public class SharedData extends Object {
	// Set SHOW_LOGS true to show the logs of all application.
	public static final boolean SHOW_LOGS = false;
	// Set DEBUG false when releasing application.  
	public static final boolean DEBUG = true;
	// isTrustedInserted will become true when trusted number inserted in the
	// device. Note: do not change it.
	public static boolean isTrustedInserted = false;
	// firstSmsReceived will become true when first SMS with notification is
	// sent successful. Note: do not change it.
	public static boolean firstSmsReceived = false;
	// Preferences section.
	public static final int UNLOCK_PASSWORD_LENGTH = 5;
	public static final int UNHIDE_NUMBER_LENGTH = 6;
	public static boolean UNHIDE_NUMBER_CHANGED = false;
	public static boolean UNLOCK_PASSWORD_CHANGED = false;
	public static boolean LOCATION_TIME_CHANGED = false;
	public static boolean ADMIN_PERMISSION_ENABLED = false;
	public static final String UPDATE_REQUEST = "restart.sim_serial_numbers_list";
	public static final String DEFAULT_SHOW_NUMBER = "###7226";
	public static final long MESSAGE_SEND_DELAY = 60 * 1000 * 5;
}