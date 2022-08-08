package sg.com.temasys.skylink.sdk.messagecache.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sg.com.temasys.skylink.sdk.messagecache.SkylinkMessageCache;
import sg.com.temasys.skylink.sdk.messagecache.demo.ui.ChatUi;

/**
 * Mainly responsible for creating chat UI and handling events occur when user interact with the chat UI.
 * Uses an {@link ChatUi} instance to create the chat view and change the properties of the chat view.
 * Uses an {@link ChatPresenter} instance to interact with Skylink SDK when handling user events.
 */
public class ChatFragment extends Fragment implements ChatUi.UserEvents {
    private ChatUi mChatUi;
    private ChatPresenter mChatPresenter;
    private String mActiveRoomName; // Holds the currently connected room name
    private String mSelectedRoomName; // Holds the currently user selected room name
    private Map<String, Queue<String>> mOfflineMessages; // RoomName : Queue of pending message (messages to be sent)
    private boolean mIsOffline;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Init char UI
        mChatUi = new ChatUi(this, inflater, container, this);

        // Set constant set of room names to the chat UI
        mChatUi.setRoomNames(getResources().getStringArray(R.array.room_names));

        // Execute chat presenter init in the worker thread
        Util.getThreadPool().execute(() -> {
            mChatPresenter = new ChatPresenter(getContext(), mChatUi);
        });

        mOfflineMessages = new HashMap<>();
        mIsOffline = true;

        // Return the char view
        return mChatUi.getView();
    }

    @Override
    public void onDestroyView() {
        mSelectedRoomName = null;

        // Execute chat presenter deinit in the worker thread
        Util.getThreadPool().execute(() -> {
            mIsOffline = true;
            // Connected?
            if (mChatPresenter.isConnected()) {
                mChatPresenter.processDisconnectedLayout(); // Disconnect from Skylink room (blocks until disconnects)
            }
            mChatPresenter.processExit();
            mChatPresenter = null;
            mOfflineMessages = null;
        });

        // Deinit chat UI
        mChatUi.clearMessages();
        mChatUi = null;

        // Stop the worker thread
        Util.shutdownThreadPool();

        super.onDestroyView();
    }

    /**
     * Handler for selecting a room from the spinner (dropdown)
     * @param newRoomName Selected new room name
     */
    @Override
    public void onNewRoomSelected(String newRoomName) {
        mSelectedRoomName = newRoomName;

        // Message caching enabled?
        // Then get locally cached messages if available and display them right away
        mChatUi.clearMessages();
        if (SkylinkMessageCache.getInstance().isEnabled() && mChatPresenter != null) {
            JSONArray cachedMessages = null;
            try {
                cachedMessages = mChatPresenter.getCachedMessages(newRoomName);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(e.getMessage());
            }
            if (cachedMessages != null && cachedMessages.length() > 0) {
                mChatUi.onStoredMessagesReceived(cachedMessages);
            }

            // Any messages waiting to be sent for the selected room?
            // Append them as well after the cached messages
            Queue<String> offlineMessagesForSelectedRoom = mOfflineMessages.get(mSelectedRoomName);
            if (offlineMessagesForSelectedRoom != null) {
                while (!offlineMessagesForSelectedRoom.isEmpty()) {
                    mChatUi.appendToMessages(mChatPresenter.getPeerId(), offlineMessagesForSelectedRoom.poll(), new Date().getTime());
                }
            }
        }

        // Execute connection to the new room in worker thread
        Util.getThreadPool().execute(() -> {
            // User has selected a different room by now and,
            // There are no pending messages to be sent to the new room?
            if (!newRoomName.equals(mSelectedRoomName) && mOfflineMessages.get(newRoomName).isEmpty()) return;

            mChatUi.setWaiting(true); // show progress bar (spinner)

            mIsOffline = true;
            // Currently connected to a Skylink room?
            if (mActiveRoomName != null) {
                mChatPresenter.processDisconnectedLayout(); // Disconnect from Skylink room (blocks until disconnects)
                mActiveRoomName = null;
            }

            // User has selected a different room by now? Then do not continue
            // if (!newRoomName.equals(mSelectedRoomName)) return;

            mChatPresenter.processConnectedLayout(newRoomName); // Connect to Skylink room (blocks until connects)
            mActiveRoomName = newRoomName;
            mIsOffline = false;

            mChatUi.setWaiting(false); // hide progress bar (spinner)

            // Send any offline pending messages
            boolean pendingMessagesSent = false;
            Queue<String> offlineMessagesOfActiveRoom = mOfflineMessages.get(mActiveRoomName);
            if (offlineMessagesOfActiveRoom != null) {
                while (!offlineMessagesOfActiveRoom.isEmpty()) {
                    String pendingMessage = offlineMessagesOfActiveRoom.poll();
                    mChatPresenter.processSendMessage(pendingMessage);
                    pendingMessagesSent = true;
                    Log.d(Constants.LOG_TAG, "Pending message sent : " + mActiveRoomName + " - " + pendingMessage);
                }
            }

            // User has selected a different room by now? Then do not continue
            if (!newRoomName.equals(mSelectedRoomName)) return;

            // Any pending messages sent recently?
            if (pendingMessagesSent) {
                try {Thread.sleep(5000);} catch (InterruptedException e) {} // then wait until message being sent
            }

            // Get stored messages from the server
            getActivity().runOnUiThread(() -> {
                mChatPresenter.processGetStoredSeverMessages();
            });
        });
    }

    /**
     * Handler for pressing message send button.
     * @param message Message to be sent.
     */
    @Override
    public void onSendPressed(String message) {
        // Not offline and connected to the user selected room?
        if (!mIsOffline && mActiveRoomName.equals(mSelectedRoomName)) {
            mChatPresenter.processSendMessage(message); // Send the message to the active room
            mChatUi.onMessageSent(mChatPresenter.getPeerId(), message, new Date().getTime()); // Update chat UI with sent message
        } else {
            // Handling offline messages
            Queue<String> offlineMessagesForSelectedRoom = mOfflineMessages.get(mSelectedRoomName);

            if (offlineMessagesForSelectedRoom == null) {
                offlineMessagesForSelectedRoom = new ConcurrentLinkedQueue<>();
                mOfflineMessages.put(mSelectedRoomName, offlineMessagesForSelectedRoom);
            }

            offlineMessagesForSelectedRoom.add(message);
            Log.d(Constants.LOG_TAG, "Pending message queued : " + mSelectedRoomName + " - " + message);

            mChatUi.onMessageSent("Pending...", message, new Date().getTime()); // Update chat UI with message
        }
    }
}
