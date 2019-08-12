package com.rohos.logon1;

public class MqttConstants {

    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 3476803;

    public static class ACTION {
        public static final String MAIN_ACTION = "test.action.main";
        public static final String START_ACTION = "test.action.start";
        public static final String STOP_ACTION = "test.action.stop";
    }

    public static class STATE_SERVICE {
        public static final int CONNECTED = 10;
        public static final int NOT_CONNECTED = 0;
    }
}
