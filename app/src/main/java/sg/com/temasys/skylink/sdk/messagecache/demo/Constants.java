package sg.com.temasys.skylink.sdk.messagecache.demo;

public interface Constants {
    String LOG_TAG = "shylinkchatdemo";

    enum ConnectionStates {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        FAILED,
    }

    String MSG_SENDER_ID = "senderId";
    String MSG_DATA = "data";
    String MSG_TIMESTAMP = "timeStamp";
}
