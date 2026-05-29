package com.abc.personaldashboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.AppDatabase;
import com.abc.personaldashboard.database.DailyActivity;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    private List<DailyActivity> activities;
    private boolean canCheckActivities;
    private ActivityActionListener listener;

    public ActivityAdapter(List<DailyActivity> activities, boolean canCheckActivities, ActivityActionListener listener) {
        this.activities = activities;
        this.canCheckActivities = canCheckActivities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyActivity activity = activities.get(position);
        holder.activityName.setText(activity.getActivityName());
        holder.activityTime.setText(activity.getTimeStart() + " - " + activity.getTimeEnd());
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(activity.isCompleted());
        holder.checkbox.setEnabled(canCheckActivities);

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            activity.setCompleted(isChecked);
            new Thread(() -> {
                AppDatabase.getInstance(holder.itemView.getContext())
                        .dailyActivityDao().update(activity);
            }).start();
        });
        holder.editButton.setOnClickListener(view -> listener.onEdit(activity));
        holder.deleteButton.setOnClickListener(view -> listener.onDelete(activity));
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityName, activityTime;
        CheckBox checkbox;
        Button editButton, deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            activityName = itemView.findViewById(R.id.activity_name);
            activityTime = itemView.findViewById(R.id.activity_time);
            checkbox = itemView.findViewById(R.id.activity_checkbox);
            editButton = itemView.findViewById(R.id.edit_activity_button);
            deleteButton = itemView.findViewById(R.id.delete_activity_button);
        }
    }

    public interface ActivityActionListener {
        void onEdit(DailyActivity activity);
        void onDelete(DailyActivity activity);
    }
}
