package com.abc.personaldashboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.AppDatabase;
import com.abc.personaldashboard.database.DailyActivity;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    private List<DailyActivity> activities;

    public ActivityAdapter(List<DailyActivity> activities) {
        this.activities = activities;
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
        holder.checkbox.setChecked(activity.isCompleted());

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.setCompleted(isChecked);
            new Thread(() -> {
                AppDatabase.getInstance(holder.itemView.getContext())
                        .dailyActivityDao().update(activity);
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityName, activityTime;
        CheckBox checkbox;

        ViewHolder(View itemView) {
            super(itemView);
            activityName = itemView.findViewById(R.id.activity_name);
            activityTime = itemView.findViewById(R.id.activity_time);
            checkbox = itemView.findViewById(R.id.activity_checkbox);
        }
    }
}