package com.example.personaldashboard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.personaldashboard.R;
import com.example.personaldashboard.adapters.ActivityAdapter;
import com.example.personaldashboard.database.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private TextView todayDateText, tasksProgressText, completedTasksText, pendingTasksText;
    private RecyclerView nearestScheduleRecyclerView;
    private AppDatabase database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        database = AppDatabase.getInstance(getContext());

        todayDateText = view.findViewById(R.id.today_date);
        tasksProgressText = view.findViewById(R.id.tasks_progress);
        completedTasksText = view.findViewById(R.id.completed_tasks);
        pendingTasksText = view.findViewById(R.id.pending_tasks);
        nearestScheduleRecyclerView = view.findViewById(R.id.nearest_schedule_recycler);

        nearestScheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        // Set today's date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        todayDateText.setText(sdf.format(new Date()));

        // Load tasks summary in background thread
        new Thread(() -> {
            List<Task> allTasks = database.taskDao().getAllTasks();
            List<Task> completedTasks = database.taskDao().getCompletedTasks();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tasksProgressText.setText("Total Tasks: " + allTasks.size());
                    completedTasksText.setText("Completed: " + completedTasks.size());
                    pendingTasksText.setText("Pending: " + (allTasks.size() - completedTasks.size()));
                });
            }

            // Load today's activities
            Calendar calendar = Calendar.getInstance();
            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.getTime()).toUpperCase();
            List<DailyActivity> todayActivities = database.dailyActivityDao().getActivitiesForDay(today);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ActivityAdapter adapter = new ActivityAdapter(todayActivities);
                    nearestScheduleRecyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }
}
