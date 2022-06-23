package sg.com.temasys.skylink.sdk.messagecache.demo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import sg.com.temasys.skylink.sdk.messagecache.demo.Constants;
import sg.com.temasys.skylink.sdk.messagecache.demo.R;

public class MessagesRecyclerViewAdapter extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.ItemViewHolder> {
    private Context mContext;
    private JSONArray mMessages;

    public MessagesRecyclerViewAdapter(Context context, JSONArray messages) {
        mContext = context;
        mMessages = messages;
    }

    protected static class ItemViewHolder extends RecyclerView.ViewHolder {
        protected final TextView senderIdTextView;
        protected final TextView messageTextView;
        protected final TextView timeTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            senderIdTextView = itemView.findViewById(R.id.senderid_textview);
            messageTextView = itemView.findViewById(R.id.message_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.messages_recyclerview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        try {
            JSONObject msg = (JSONObject) mMessages.get(position);
            holder.senderIdTextView.setText(msg.getString(Constants.MSG_SENDER_ID));
            holder.messageTextView.setText(msg.getString(Constants.MSG_DATA));
            holder.timeTextView.setText(new Date(msg.getLong(Constants.MSG_TIMESTAMP)).toString());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new AssertionError("Failed to read from message JSONObject");
        }
    }

    @Override
    public int getItemCount() {
        if (mMessages == null) return 0;
        return mMessages.length();
    }
}
