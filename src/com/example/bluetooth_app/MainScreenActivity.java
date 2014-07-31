package com.example.bluetooth_app;

import android.support.v4.app.Fragment;

public class MainScreenActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new MainScreenFragment();
    }

}
