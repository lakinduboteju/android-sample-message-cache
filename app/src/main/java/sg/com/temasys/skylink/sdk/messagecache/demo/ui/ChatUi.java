package sg.com.temasys.skylink.sdk.messagecache.demo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import sg.com.temasys.skylink.sdk.messagecache.demo.ChatPresenter;
import sg.com.temasys.skylink.sdk.messagecache.demo.Constants;
import sg.com.temasys.skylink.sdk.messagecache.demo.R;

public class ChatUi implements ChatPresenter.SkylinkEvents {
    private ChatViewModel mChatViewModel;
    private View mChatView;

    public interface UserEvents {
        void onNewRoomSelected(String newRoomName);
        void onSendPressed(String message);
    }

    protected static class ChatViewModel extends ViewModel {
        protected final MutableLiveData<Boolean> mIsWaiting;
        protected final MutableLiveData<String[]> mRoomNames;
        protected final MutableLiveData<Integer> mSelectedRoomPosition;
        protected final MutableLiveData<String> mConnectionState;
        protected final MutableLiveData<String> mStatus;
        protected final MutableLiveData<List<String>> mPeers;
        protected final MutableLiveData<JSONArray> mStoredMessages;

        public ChatViewModel() {
            mIsWaiting = new MutableLiveData<>();
            mRoomNames = new MutableLiveData<>();
            mSelectedRoomPosition = new MutableLiveData<>();
            mConnectionState = new MutableLiveData<>();
            mStatus = new MutableLiveData<>();
            mPeers = new MutableLiveData<>();
            mStoredMessages = new MutableLiveData<>();
        }
    }

    public ChatUi(Fragment owner, LayoutInflater inflater, ViewGroup container, UserEvents callback) {
        mChatViewModel = new ViewModelProvider(owner).get(ChatViewModel.class);
        mChatView = inflater.inflate(R.layout.fragment_chat, container, false);

        // Bind waiting progressbar to the view model
        ProgressBar waitingProgressBar = mChatView.findViewById(R.id.waiting_progressbar);
        EditText messageEditText = mChatView.findViewById(R.id.message_edittext);
        Button sendButton = mChatView.findViewById(R.id.send_button);
        mChatViewModel.mIsWaiting.observe(owner, isWaiting -> {
            if (isWaiting) {
                waitingProgressBar.setVisibility(View.VISIBLE);
                // messageEditText.setVisibility(View.GONE);
                // sendButton.setVisibility(View.GONE);
            } else {
                waitingProgressBar.setVisibility(View.GONE);
                // messageEditText.setVisibility(View.VISIBLE);
                // sendButton.setVisibility(View.VISIBLE);
            }
        });

        // Initialize Room select spinner (dropdown) and bind it to the view model
        Spinner roomListSpinner = mChatView.findViewById(R.id.room_list_spinner);
        roomListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mChatViewModel.mSelectedRoomPosition.postValue(position);
                String newActiveRoomName = (String) adapterView.getAdapter().getItem(position);
                callback.onNewRoomSelected(newActiveRoomName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        mChatViewModel.mRoomNames.observe(owner, newRooms -> roomListSpinner.setAdapter(new SpinnerAdapter(owner.getContext(), newRooms)));
        mChatViewModel.mSelectedRoomPosition.observe(owner, newPosition -> roomListSpinner.setSelection(newPosition));

        // Bind connection state textview to the view model
        TextView connectionStateTextView = mChatView.findViewById(R.id.connection_textview);
        mChatViewModel.mConnectionState.observe(owner, newConnectionState -> connectionStateTextView.setText(newConnectionState));

        // Bind status textview to the view model
        TextView statusTextView = mChatView.findViewById(R.id.status_textview);
        mChatViewModel.mStatus.observe(owner, status -> statusTextView.setText(status));

        // Bind peers recyclerview to the view model
        RecyclerView peersRecyclerView = mChatView.findViewById(R.id.peers_recyclerview);
        peersRecyclerView.setLayoutManager(new LinearLayoutManager(owner.getContext()));
        mChatViewModel.mPeers.observe(owner, newPeerList -> peersRecyclerView.setAdapter(new PeersRecyclerViewAdapter(owner.getContext(), newPeerList)));

        // Bind messages recyclerview to the view model
        RecyclerView messagesRecyclerView = mChatView.findViewById(R.id.messages_recyclerview);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(owner.getContext()));
        mChatViewModel.mStoredMessages.observe(owner, storeMessages -> messagesRecyclerView.setAdapter(new MessagesRecyclerViewAdapter(owner.getContext(), storeMessages)));

        // Set send button on click listener
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            if (!message.isEmpty()) {
                callback.onSendPressed(message);
                messageEditText.setText(null);
            }
        });
    }

    public View getView() {
        return mChatView;
    }

    public void setRoomNames(String[] roomNames) {
        mChatViewModel.mRoomNames.postValue(roomNames);
    }

    public void setWaiting(boolean isWaiting) {
        mChatViewModel.mIsWaiting.postValue(isWaiting);
    }

    public void clearMessages() {
        mChatViewModel.mStoredMessages.postValue(null);
    }

    public void appendToMessages(String peerId, String message, Long timestamp) {
        JSONArray storedMessages = mChatViewModel.mStoredMessages.getValue();
        if (storedMessages == null) storedMessages = new JSONArray();
        JSONObject msg = new JSONObject();
        try {
            msg.put(Constants.MSG_SENDER_ID, peerId);
            msg.put(Constants.MSG_DATA, message);
            msg.put(Constants.MSG_TIMESTAMP, timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new AssertionError("Failed to create message JSONObject");
        }
        storedMessages.put(msg);
        mChatViewModel.mStoredMessages.postValue(storedMessages);
    }

    public void onMessageSent(String peerId, String message, Long timestamp) {
        appendToMessages(peerId, message, timestamp);
        mChatViewModel.mStatus.postValue("Message sent.");
    }

    public void onCachedMessages(JSONArray storedMessages) {
        mChatViewModel.mStoredMessages.postValue(storedMessages);
        mChatViewModel.mStatus.postValue("Received stored messages from local cache.");
    }

    @Override
    public void onConnectionStateChanged(Constants.ConnectionStates newConnectionState, String roomName) {
        switch (newConnectionState) {
            case CONNECTING:
                mChatViewModel.mConnectionState.postValue("Connecting...");
                mChatViewModel.mStatus.postValue("Trying to connecting to room : " + roomName);
                break;
            case CONNECTED:
                mChatViewModel.mConnectionState.postValue("Connected.");
                mChatViewModel.mStatus.postValue("Connected to room : " + roomName);
                break;
            case DISCONNECTING:
                mChatViewModel.mConnectionState.postValue("Disconnecting.");
                mChatViewModel.mStatus.postValue("Disconnecting from room : " + roomName);
                break;
            case DISCONNECTED:
                mChatViewModel.mConnectionState.postValue("Disconnected.");
                mChatViewModel.mStatus.postValue("Disconnected from room : " + roomName);
                break;
            case FAILED:
                mChatViewModel.mConnectionState.postValue("Connection Failed.");
                mChatViewModel.mStatus.postValue("Failed to connect to room : " + roomName);
                break;
        }
    }

    @Override
    public void onRemotePeersChanged(List<String> newPeerList) {
        mChatViewModel.mPeers.postValue(newPeerList);
        mChatViewModel.mStatus.postValue("Number of remote peers changed.");
    }

    @Override
    public void onStoredMessagesReceived(JSONArray storedMessages) {
        mChatViewModel.mStoredMessages.postValue(storedMessages);
        mChatViewModel.mStatus.postValue("Received stored messages from server.");
    }

    @Override
    public void onRemoteMessageReceived(String senderId, String message, Long timestamp) {
        appendToMessages(senderId, message, timestamp);
        mChatViewModel.mStatus.postValue("Received a message from a remote peer.");
    }

    @Override
    public void onMessageSendingFailed() {
        mChatViewModel.mStatus.postValue("Error : Failed to send the message.");
    }
}
