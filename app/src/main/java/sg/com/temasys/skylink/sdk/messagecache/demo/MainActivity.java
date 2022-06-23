package sg.com.temasys.skylink.sdk.messagecache.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

public class MainActivity extends AppCompatActivity {


    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // Load selected App key and Room User setting details
            Config.loadSelectedAppKey(this);

            //init utils
            Utils utils = new Utils(this);

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container, ChatFragment.class, null)
                    .commit();
        }
    }
}