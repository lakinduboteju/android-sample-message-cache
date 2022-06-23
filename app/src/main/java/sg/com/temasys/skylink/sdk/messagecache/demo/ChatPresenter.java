package sg.com.temasys.skylink.sdk.messagecache.demo;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import sg.com.temasys.skylink.sdk.messagecache.SkylinkMessageCache;
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatContract;
import sg.com.temasys.skylink.sdk.sampleapp.chat.MESSAGE_FORMAT;
import sg.com.temasys.skylink.sdk.sampleapp.chat.MESSAGE_TYPE;
import sg.com.temasys.skylink.sdk.sampleapp.service.ChatService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.MessageModel;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer;

/**
 * Responsible for communicating with the CharService.
 */
public class ChatPresenter extends BasePresenter implements ChatContract.Presenter {
    private Context mContext;
    private ChatService mChatService;
    private SkylinkEvents mSkylinkEventsCallback;
    private boolean mIsConnected;
    private Set<SkylinkPeer> mRemotePeers;
    private String mRoomName;

    private CountDownLatch mConnectionStateChangeLatch;

    public interface SkylinkEvents {
        void onConnectionStateChanged(Constants.ConnectionStates newConnectionState, String roomName);
        void onRemotePeersChanged(List<String> newPeerList);
        void onStoredMessagesReceived(JSONArray storedMessages);
        void onRemoteMessageReceived(String senderId, String message, Long timestamp);
        void onMessageSendingFailed();
    }

    public ChatPresenter(Context context, SkylinkEvents callback) {
        mContext = context;
        mChatService = new ChatService(context);
        mChatService.setPresenter(this);
        mSkylinkEventsCallback = callback;
        mIsConnected = false;
        mRemotePeers = new HashSet<>();

        mConnectionStateChangeLatch = null;
    }

    // ChatContract.Presenter implementation

    /**
     * Connects to the given Skylink Room.
     * This is the blocking call. Should be executed on a worker thread.
     * @param roomName Skylink Room name
     */
    @Override
    public void processConnectedLayout(String roomName) {
        //start to connect to room when entering room
        //if not being connected, then connect
        if (mConnectionStateChangeLatch == null && !mChatService.isConnectingOrConnected()) {
            mConnectionStateChangeLatch = new CountDownLatch(1);

            mRoomName = roomName;

            //connect to room on Skylink connection
            mChatService.connectToRoom(sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.CONFIG_TYPE.CHAT, mRoomName);
            //after connected to skylink SDK, UI will be updated later on processRoomConnected

            mSkylinkEventsCallback.onConnectionStateChanged(Constants.ConnectionStates.CONNECTING, mRoomName);

            // Wait for connection state change
            try { mConnectionStateChangeLatch.await(); } catch (InterruptedException e) { e.printStackTrace(); }
            mConnectionStateChangeLatch = null;
        }
    }

    /**
     * Disconnects from the currently connected Skylink Room.
     */
    public void processDisconnectedLayout() {
        if (mConnectionStateChangeLatch == null) {
            mConnectionStateChangeLatch = new CountDownLatch(1);

            //process disconnect from room
            mChatService.disconnectFromRoom();
            //after disconnected from skylink SDK, UI will be updated latter on processRoomDisconnected

            mSkylinkEventsCallback.onConnectionStateChanged(Constants.ConnectionStates.DISCONNECTING, mRoomName);

            // Wait for connection state change
            try { mConnectionStateChangeLatch.await(); } catch (InterruptedException e) { e.printStackTrace(); }
            mConnectionStateChangeLatch = null;
        }
    }

    /**
     * Is connected to a Skylink Room?
     * @return True if connected to a Skylink Room
     */
    boolean isConnected() { return mIsConnected; }

    /**
     * Gets local peer ID.
     * @return Local peer ID
     */
    String getPeerId() { return mChatService.getPeerId(); }

    /**
     * Gets cached messages of the given room.
     * @param roomName Skylink Room name
     * @return JSON array of cached messages of the room
     * @throws Exception If cannot make proper connection to the local cache.
     */
    JSONArray getCachedMessages(String roomName) throws Exception {
        return SkylinkMessageCache.getInstance().getReadableSession(roomName).getCachedMessages();
    }

    /**
     * Completely disposes Skylink connection.
     */
    @Override
    public void processExit() {
        // need to call disposeLocalMedia to clear all local media objects as disconnectFromRoom no longer dispose local media
        mChatService.disposeLocalMedia();
    }

    /**
     * Gets stored messages from the Skylink server.
     */
    @Override
    public void processGetStoredSeverMessages() {
        mChatService.getStoredMessages();
    }

    /**
     * Sends stored message to the connected Skylink Room.
     * @param message Message to be sent
     */
    @Override
    public void processSendMessage(String message) {
        mChatService.sendServerMessage(null, message);
    }

    @Override
    public List<MessageModel> processGetChatCollection() {
        return null;
    }
    @Override
    public SkylinkPeer processGetPeerByIndex(int index) {
        return null;
    }
    @Override
    public int processGetCurrentSelectedPeer() {
        return 0;
    }
    @Override
    public void processSelectRemotePeer(int index) {}
    @Override
    public void processSelectMessageType(MESSAGE_TYPE message_type) {}
    @Override
    public void processSelectMessageFormat(MESSAGE_FORMAT formatMsg) {}
    @Override
    public void processAddEncryption(String enryptionKey, String encryptionValue) {}
    @Override
    public String processGetEncryptionValueFromKey(String encryptionKey) {
        return null;
    }
    @Override
    public void processStoreMessageSet(boolean isChecked) {}
    @Override
    public void processSelectSecretKey(String secretKey) {}
    @Override
    public void processDeleteEncryption(String enryptionKey, String encryptionValue) {}

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------

    /**
     * Handler of Skylink Room connection.
     */
    @Override
    public void processRoomConnected(boolean isSuccessful) {
        if (isSuccessful) {
            mSkylinkEventsCallback.onConnectionStateChanged(Constants.ConnectionStates.CONNECTED, mRoomName);
            mIsConnected = true;

            String encryptSecretKey = mContext.getResources().getString(R.string.encrypt_secret_key);
            String encryptSecretValue = mContext.getResources().getString(R.string.encrypt_secret_value);

            mChatService.setEncryptedMap(Collections.singletonList(encryptSecretKey), Collections.singletonList(encryptSecretValue));

            mChatService.setSelectedEncryptedSecret(encryptSecretKey);

            mChatService.setStoreMessage(true);
        } else {
            mSkylinkEventsCallback.onConnectionStateChanged(Constants.ConnectionStates.FAILED, mRoomName);
            mIsConnected = false;
        }
        if (mConnectionStateChangeLatch != null) mConnectionStateChangeLatch.countDown();
    }

    /**
     * Handler of Skylink Room disconnection.
     */
    @Override
    public void processRoomDisconnected() {
        mSkylinkEventsCallback.onConnectionStateChanged(Constants.ConnectionStates.DISCONNECTED, mRoomName);
        mIsConnected = false;
        if (mConnectionStateChangeLatch != null) mConnectionStateChangeLatch.countDown();
    }

    /**
     * Handler of remote peer connected to the currently connected Skylink Room.
     * @param newPeer Object contains new remote peer information
     */
    @Override
    public void processRemotePeerConnected(SkylinkPeer newPeer) {
        mRemotePeers.add(newPeer);
        onRemotePeersChanged();
    }

    /**
     * Handler of remote peer disconnection from the current Skylink Room.
     * @param remotePeer Object contains leaving remote peer information
     * @param removeIndex
     */
    @Override
    public void processRemotePeerDisconnected(SkylinkPeer remotePeer, int removeIndex) {
        mRemotePeers.remove(remotePeer);
        onRemotePeersChanged();
    }

    /**
     * Handler of receiving stored message history from the Skylink server.
     * @param storedMessages JSON array of stored messages from the server.
     */
    @Override
    public void processStoredMessagesResult(JSONArray storedMessages) {
        if (storedMessages != null) {
            mSkylinkEventsCallback.onStoredMessagesReceived(storedMessages);
        }
    }

    /**
     * Handler of receiving server message from a remote peer.
     * @param remotePeerId Message sender's peer ID
     * @param message      Message object
     * @param isPrivate    Is a private message?
     * @param timestamp    Timestamp of sent message
     */
    @Override
    public void processServerMessageReceived(String remotePeerId, Object message, boolean isPrivate, Long timestamp) {
        mSkylinkEventsCallback.onRemoteMessageReceived(remotePeerId, (String) message, timestamp);
    }

    /**
     * Message sending failure handler
     */
    @Override
    public void processMessageSendFailed() {
        mSkylinkEventsCallback.onMessageSendingFailed();
    }

    /**
     * Remote peer change handler
     */
    private void onRemotePeersChanged() {
        List<String> newRemotePeerIds = new ArrayList<>();
        for (SkylinkPeer p : mRemotePeers) {
            newRemotePeerIds.add(p.getPeerId());
        }
        mSkylinkEventsCallback.onRemotePeersChanged(newRemotePeerIds);
    }
}
