package sg.com.temasys.skylink.sdk.messagecache.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

import java.util.Date;

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
    private String mActiveRoomName; // Holds the user selected room name

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

        // Return the char view
        return mChatUi.getView();
    }

    @Override
    public void onDestroyView() {
        // Execute chat presenter deinit in the worker thread
        Util.getThreadPool().execute(() -> {
            // Connected?
            if (mChatPresenter.isConnected()) {
                mChatPresenter.processDisconnectedLayout(); // Disconnect from Skylink room (blocks until disconnects)
            }
            mChatPresenter.processExit();
            mChatPresenter = null;
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
        mChatUi.clearMessages();

        // Message caching enabled?
        // Then get locally cached messages if available and display them right away
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
        }

        // Execute in worker thread
        Util.getThreadPool().execute(() -> {
            mChatUi.setWaiting(true);

            // Already connected to a Skylink room?
            if (mActiveRoomName != null) {
                mChatPresenter.processDisconnectedLayout(); // Disconnect from Skylink room (blocks until disconnects)
            }
            mActiveRoomName = newRoomName;
            mChatPresenter.processConnectedLayout(mActiveRoomName); // Connect to Skylink room (blocks until connects)

            // User has not closed the app?
            // Then get stored messages from the server
            if (mChatUi != null && mChatPresenter != null) {
                mChatUi.setWaiting(false);

                // Connected? Then get stored messages from server
                if (mChatPresenter.isConnected()) {
                    getActivity().runOnUiThread(() -> {
                        mChatPresenter.processGetStoredSeverMessages();
                    });
                }
            }
        });
    }

    /**
     * Handler for pressing message send button.
     * @param message Message to be sent.
     */
    @Override
    public void onSendPressed(String message) {
        mChatPresenter.processSendMessage(message); // Send the message
        mChatUi.onMessageSent(mChatPresenter.getPeerId(), message, new Date().getTime()); // Update chat UI with sent message
    }
}
