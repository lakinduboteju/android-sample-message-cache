package sg.com.temasys.skylink.sdk.messagecache.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import sg.com.temasys.skylink.sdk.messagecache.demo.R;

public class SpinnerAdapter extends ArrayAdapter {
    public SpinnerAdapter(@NonNull Context context, @NonNull Object[] objects) {
        super(context, R.layout.spinner_item, objects);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View layout = ( (Activity) getContext() ).getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
        TextView spinnerItem = layout.findViewById(R.id.spinner_item);

        String roomName = (String) this.getItem(position);
        spinnerItem.setText(roomName);

        return layout;
    }
}