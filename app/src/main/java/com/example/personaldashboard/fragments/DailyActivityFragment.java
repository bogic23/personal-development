package com.example.personaldashboard.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.personaldashboard.R;
import com.example.personaldashboard.adapters.ActivityAdapter;
import com.example.personaldashboard.database.*;
import java.util.List;

public class DailyActivityFragment extends Fragment {
    private Spinner daySpinner;
    private EditText activityNameInput, timeStartInput, timeEndInput;
    private Button addButton;
    private RecyclerView activitiesRecyclerView;
    private AppDatabase database;
    private ActivityAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_activity, container, false);

        database = AppDatabase.getInstance(getContext());

        daySpinner = view.findViewById(R.id.day_spinner);
        activityNameInput = view.findViewById(R.id.activity_name);
        timeStartInput = view.findViewById(R.id.time_start);
        timeEndInput = view.findViewById(R.id.time_end);
        addButton = view.findViewById(R.id.add_button);
        activitiesRecyclerView = view.findViewById(R.id.activities_recycler);

        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up day spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.days_of_week, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(spinnerAdapter);

        // Time pickers
        timeStartInput.setOnClickListener(v -> showTimePicker(timeStartInput));
        timeEndInput.setOnClickListener(v -> showTimePicker(timeEndInput));

        // Day spinner listener
        daySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadActivities();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add button
        addButton.setOnClickListener(v -> addActivity());

        loadActivities();

        return view;
    }

    private void showTimePicker(EditText editText) {
        TimePickerDialog dialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    editText.setText(time);
                }, 9, 0, true);
        dialog.show();
    }

    private void addActivity() {
        String name = activityNameInput.getText().toString().trim();
        String timeStart = timeStartInput.getText().toString().trim();
        String timeEnd = timeEndInput.getText().toString().trim();
        String day = daySpinner.getSelectedItem().toString().toUpperCase();

        if (name.isEmpty() || timeStart.isEmpty() || timeEnd.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        DailyActivity activity = new DailyActivity(day, name, timeStart, timeEnd);

        new Thread(() -> {
            database.dailyActivityDao().insert(activity);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadActivities();
                    activityNameInput.setText("");
                    timeStartInput.setText("");
                    timeEndInput.setText("");
                    Toast.makeText(getContext(), "Activity added!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadActivities() {
        String day = daySpinner.getSelectedItem().toString().toUpperCase();
        new Thread(() -> {
            List<DailyActivity> activities = database.dailyActivityDao().getActivitiesForDay(day);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new ActivityAdapter(activities);
                    activitiesRecyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }
}
