package com.rohos.logon1.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.NotificationsActivity;
import com.rohos.logon1.R;
import com.rohos.logon1.ui.RemoveNotifyDialog;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {

    ArrayList<String[]> mNotifyList = null;
    AuthRecordsDb mDb = null;
    NotificationsAdapter mNotifyAdapter = null;
    NotificationsActivity mActivity = null;
    LayoutInflater mInflater = null;
    ListView mListView = null;
    Handler mHandler = null;
    Handler.Callback mCallback = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppLog.log("onCreate: " + System.currentTimeMillis());

        mActivity = (NotificationsActivity)getActivity();
        mInflater = mActivity.getLayoutInflater();
        mDb = new AuthRecordsDb(getActivity().getApplicationContext());
        mNotifyList = mDb.getNotifications();
        mNotifyAdapter = new NotificationsAdapter(mActivity, R.layout.notify_row_view, mNotifyList);

        initCallback();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //AppLog.log("onCreateView: " + System.currentTimeMillis());

        View view = inflater.inflate(R.layout.fragment_notify, container, false);
        mListView = view.findViewById(R.id.notify_list);
        mListView.setAdapter(mNotifyAdapter);

        Button removeAll = view.findViewById(R.id.remove_all);
        removeAll.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Message msg = Message.obtain(mHandler);
                RemoveNotifyDialog dialog = new RemoveNotifyDialog(msg);
                dialog.show(mActivity.getSupportFragmentManager(), "remove_notify");
            }
        });

        return view;
    }

    private void initCallback(){
        mCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                mDb.deleteAllNotifications();
                //mNotifyAdapter = new NotificationsAdapter(mActivity, R.layout.notify_row_view, new ArrayList<>());
                mNotifyAdapter.clear();
                mListView.setAdapter(mNotifyAdapter);
                mNotifyAdapter.notifyDataSetChanged();

                return false;
            }
        };

        mHandler = new Handler(mCallback);
    }

    //******* Inner classes *****************************************************************
    private class NotificationsAdapter extends ArrayAdapter {
        public NotificationsAdapter(Context context, int rowId, ArrayList<String[]> items){
            super(context, rowId, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String[] notify = (String[])getItem(position);

            View row;
            if(convertView != null){
                row = convertView;
            }else{
                row = mInflater.inflate(R.layout.notify_row_view, null);
            }
            TextView user = row.findViewById(R.id.n_user);
            TextView pcName = row.findViewById(R.id.n_pcname);
            TextView text = row.findViewById(R.id.n_text);
            user.setText(notify[0]);
            pcName.setText(notify[1]);
            text.setText(notify[2]);

            return row;
        }
    }
}
