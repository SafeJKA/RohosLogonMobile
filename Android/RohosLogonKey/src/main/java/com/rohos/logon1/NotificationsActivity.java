package com.rohos.logon1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.rohos.logon1.fragments.NotificationsFragment;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, NotificationsFragment.class, null)
                .commit();

    }
}