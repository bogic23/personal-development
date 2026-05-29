package com.example.personaldashboard.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.personaldashboard.R;
import com.example.personaldashboard.adapters.TaskAdapter;
import com.example.personaldashboard.database.*;
import java.util.List;

public class TasksFragment extends Fragment {
    private EditText taskNameInput, taskDescriptionInput, taskDueDateInput;
    private Button addTaskButton;
    private RecyclerView tasksRecyclerView;
    private AppDatabase database;
    private TaskAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        database = AppDatabase.getInstance(getContext());

        taskNameInput = view.findViewById(R.id.task_name);
        taskDescriptionInput = view.findViewById(R.id.task_description);
        taskDueDateInput = view.findViewById(R.id.task_due_date);
        addTaskButton = view.findViewById(R.id.add_task_button);
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler);

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        addTaskButton.setOnClickListener(v -> addTask());

        loadTasks();

        return view;
    }

    private void addTask() {
        String name = taskNameInput.getText().toString().trim();
        String description = taskDescriptionInput.getText().toString().trim();
        String dueDate = taskDueDateInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter task name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty()) {
            dueDate = "No due date";
        }

        Task task = new Task(name, description, dueDate);

        new Thread(() -> {
            database.taskDao().insert(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadTasks();
                    taskNameInput.setText("");
                    taskDescriptionInput.setText("");
                    taskDueDateInput.setText("");
                    Toast.makeText(getContext(), "Task added!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadTasks() {
        new Thread(() -> {
            List<Task> tasks = database.taskDao().getAllTasks();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new TaskAdapter(tasks);
                    tasksRecyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }
}
