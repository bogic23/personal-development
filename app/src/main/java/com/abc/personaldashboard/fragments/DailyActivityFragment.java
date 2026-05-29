package com.abc.personaldashboard.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.adapters.ActivityAdapter;
import com.abc.personaldashboard.database.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyActivityFragment extends Fragment {
    private EditText activityDateInput, activityNameInput, timeStartInput, timeEndInput;
    private Button addButton, cancelEditButton;
    private RecyclerView activitiesRecyclerView;
    private AppDatabase database;
    private ActivityAdapter adapter;
    private DailyActivity editingActivity;
    private String selectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.ENGLISH);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_activity, container, false);

        database = AppDatabase.getInstance(getContext());
        selectedDate = todayDate();

        activityDateInput = view.findViewById(R.id.activity_date);
        activityNameInput = view.findViewById(R.id.activity_name);
        timeStartInput = view.findViewById(R.id.time_start);
        timeEndInput = view.findViewById(R.id.time_end);
        addButton = view.findViewById(R.id.add_button);
        cancelEditButton = view.findViewById(R.id.cancel_edit_button);
        activitiesRecyclerView = view.findViewById(R.id.activities_recycler);

        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        activityDateInput.setText(getDisplayDate(selectedDate));
        activityDateInput.setOnClickListener(v -> showDatePicker());

        timeStartInput.setOnClickListener(v -> showTimePicker(timeStartInput));
        timeEndInput.setOnClickListener(v -> showTimePicker(timeEndInput));

        addButton.setOnClickListener(v -> saveActivity());
        cancelEditButton.setOnClickListener(v -> resetForm());

        loadActivities();

        return view;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (Exception ignored) {
        }

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, dayOfMonth);
                    selectedDate = dateFormat.format(pickedDate.getTime());
        activityDateInput.setText(getDisplayDate(selectedDate));
                    resetForm();
                    loadActivities();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker(EditText editText) {
        TimePickerDialog dialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    editText.setText(time);
                }, 9, 0, true);
        dialog.show();
    }

    private void saveActivity() {
        String name = activityNameInput.getText().toString().trim();
        String timeStart = timeStartInput.getText().toString().trim();
        String timeEnd = timeEndInput.getText().toString().trim();

        if (name.isEmpty() || timeStart.isEmpty() || timeEnd.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minutesFromTime(timeStart) >= minutesFromTime(timeEnd)) {
            Toast.makeText(getContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            List<DailyActivity> activities = database.dailyActivityDao().getActivitiesForDate(selectedDate);
            int editingId = editingActivity == null ? 0 : editingActivity.getId();
            if (hasOverlappingActivity(activities, timeStart, timeEnd, editingId)) {
                showToast("Activity time cannot overlap");
                return;
            }

            if (editingActivity == null) {
                database.dailyActivityDao().insert(new DailyActivity(selectedDate, name, timeStart, timeEnd));
            } else {
                editingActivity.setActivityDate(selectedDate);
                editingActivity.setActivityName(name);
                editingActivity.setTimeStart(timeStart);
                editingActivity.setTimeEnd(timeEnd);
                database.dailyActivityDao().update(editingActivity);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadActivities();
                    resetForm();
                    Toast.makeText(getContext(), editingId == 0 ? "Activity added!" : "Activity updated!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadActivities() {
        new Thread(() -> {
            List<DailyActivity> activities = database.dailyActivityDao().getActivitiesForDate(selectedDate);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new ActivityAdapter(activities, selectedDate.equals(todayDate()), new ActivityAdapter.ActivityActionListener() {
                        @Override
                        public void onEdit(DailyActivity activity) {
                            startEditing(activity);
                        }

                        @Override
                        public void onDelete(DailyActivity activity) {
                            confirmDelete(activity);
                        }
                    });
                    activitiesRecyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void startEditing(DailyActivity activity) {
        editingActivity = activity;
        activityNameInput.setText(activity.getActivityName());
        timeStartInput.setText(activity.getTimeStart());
        timeEndInput.setText(activity.getTimeEnd());
        addButton.setText("Update Activity");
        cancelEditButton.setVisibility(View.VISIBLE);
    }

    private void confirmDelete(DailyActivity activity) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete activity?")
                .setMessage("This removes " + activity.getActivityName() + " from " + selectedDate + ".")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteActivity(activity))
                .show();
    }

    private void deleteActivity(DailyActivity activity) {
        new Thread(() -> {
            database.dailyActivityDao().delete(activity);
            if (editingActivity != null && editingActivity.getId() == activity.getId()) {
                editingActivity = null;
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadActivities();
                    resetForm();
                    Toast.makeText(getContext(), "Activity deleted", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean hasOverlappingActivity(List<DailyActivity> activities, String timeStart, String timeEnd, int ignoredActivityId) {
        int newStart = minutesFromTime(timeStart);
        int newEnd = minutesFromTime(timeEnd);

        for (DailyActivity activity : activities) {
            if (activity.getId() == ignoredActivityId) {
                continue;
            }

            int existingStart = minutesFromTime(activity.getTimeStart());
            int existingEnd = minutesFromTime(activity.getTimeEnd());
            if (newStart < existingEnd && newEnd > existingStart) {
                return true;
            }
        }
        return false;
    }

    private int minutesFromTime(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void resetForm() {
        editingActivity = null;
        activityNameInput.setText("");
        timeStartInput.setText("");
        timeEndInput.setText("");
        addButton.setText("Add Activity");
        cancelEditButton.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    private String todayDate() {
        return dateFormat.format(new Date());
    }

    private String getDisplayDate(String isoDate) {
        try {
            return displayDateFormat.format(dateFormat.parse(isoDate));
        } catch (Exception e) {
            return isoDate;
        }
    }
}
