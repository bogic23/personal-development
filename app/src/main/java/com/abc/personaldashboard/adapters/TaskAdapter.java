package com.abc.personaldashboard.adapters;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.AppDatabase;
import com.abc.personaldashboard.database.Task;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.taskName.setText(task.getTaskName());
        holder.taskDescription.setText(task.getDescription());
        holder.taskProgress.setText("Progress: " + task.getProgress() + "%");
        holder.taskDueDate.setText("Due: " + task.getDueDate());
        holder.progressBar.setProgress(task.getProgress());

        holder.completeButton.setOnClickListener(v -> {
            task.setCompleted(true);
            task.setProgress(100);
            new Thread(() -> {
                AppDatabase.getInstance(holder.itemView.getContext())
                        .taskDao().update(task);
                holder.itemView.post(() -> notifyDataSetChanged());
            }).start();
        });

        holder.updateProgressButton.setOnClickListener(v -> {
            showProgressDialog(holder.itemView, task);
        });
    }

    private void showProgressDialog(View view, Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Update Progress");

        final EditText input = new EditText(view.getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter progress (0-100)");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                int progress = Integer.parseInt(input.getText().toString());
                if (progress >= 0 && progress <= 100) {
                    task.setProgress(progress);
                    if (progress == 100) {
                        task.setCompleted(true);
                    }
                    new Thread(() -> {
                        AppDatabase.getInstance(view.getContext())
                                .taskDao().update(task);
                        view.post(() -> notifyDataSetChanged());
                    }).start();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(view.getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskDescription, taskProgress, taskDueDate;
        ProgressBar progressBar;
        Button completeButton, updateProgressButton;

        ViewHolder(View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskProgress = itemView.findViewById(R.id.task_progress);
            taskDueDate = itemView.findViewById(R.id.task_due_date);
            progressBar = itemView.findViewById(R.id.progress_bar);
            completeButton = itemView.findViewById(R.id.complete_button);
            updateProgressButton = itemView.findViewById(R.id.update_progress_button);
        }
    }
}