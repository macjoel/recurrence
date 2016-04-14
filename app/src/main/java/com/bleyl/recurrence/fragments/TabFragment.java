package com.bleyl.recurrence.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bleyl.recurrence.database.DatabaseHelper;
import com.bleyl.recurrence.models.Reminder;
import com.bleyl.recurrence.R;
import com.bleyl.recurrence.adapters.ReminderAdapter;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TabFragment extends Fragment {

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.empty_text) TextView emptyText;
    @Bind(R.id.empty_view) LinearLayout linearLayout;
    @Bind(R.id.empty_icon) ImageView imageView;

    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderList;
    private int remindersType;
    private boolean startAnimation = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tabs, container, false);
        if (savedInstanceState == null) startAnimation = true;
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        ButterKnife.bind(this, view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        remindersType = this.getArguments().getInt("TYPE");
        if (remindersType == Reminder.INACTIVE) {
            emptyText.setText(R.string.no_inactive);
            imageView.setImageResource(R.drawable.ic_notifications_off_black_empty);
        }

        reminderList = getListData();
        reminderAdapter = new ReminderAdapter(getActivity(), R.layout.item_notification_list, reminderList);
        recyclerView.setAdapter(reminderAdapter);

        if (reminderAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
        } else {
            if (startAnimation)
                runStartAnimation(recyclerView, layoutManager);
        }
    }

    public List<Reminder> getListData() {
        DatabaseHelper database = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        List<Reminder> reminderList = database.getNotificationList(remindersType);
        database.close();
        return reminderList;
    }

    public void updateList() {
        reminderList.clear();
        reminderList.addAll(getListData());
        reminderAdapter.notifyDataSetChanged();

        if (reminderAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
        }
    }

    public void runStartAnimation(final RecyclerView recyclerView, final LinearLayoutManager layoutManager) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();

                for (int i = start; i <= end; i++) {
                    View view = recyclerView.findViewHolderForAdapterPosition(i).itemView;
                    view.setAlpha(0);

                    PropertyValuesHolder slide = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 400, 0);
                    PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0, 1);
                    ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, slide, alpha);
                    animator.setDuration(400);
                    animator.setStartDelay(i * 80);
                    animator.setInterpolator(new DecelerateInterpolator());
                    animator.start();
                }
                recyclerView.setAlpha(1);

            }
        }, 50);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver, new IntentFilter("BROADCAST_REFRESH"));
        updateList();
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
        super.onPause();
    }
}