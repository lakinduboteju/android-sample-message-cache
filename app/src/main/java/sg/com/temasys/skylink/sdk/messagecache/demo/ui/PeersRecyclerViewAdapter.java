package sg.com.temasys.skylink.sdk.messagecache.demo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import sg.com.temasys.skylink.sdk.messagecache.demo.R;

public class PeersRecyclerViewAdapter extends RecyclerView.Adapter<PeersRecyclerViewAdapter.ItemViewHolder> {
    private Context mContext;
    private List<String> mPeers;

    protected static class ItemViewHolder extends RecyclerView.ViewHolder {
        protected final TextView textView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.peer_textview);
        }
    }

    public PeersRecyclerViewAdapter(Context context, List<String> peers) {
        mContext = context;
        mPeers = peers;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeersRecyclerViewAdapter.ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.peers_recyclerview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.textView.setText( Character.toString( mPeers.get(position).charAt(0) ) );
    }

    @Override
    public int getItemCount() {
        return mPeers.size();
    }
}
