package com.abc.personaldashboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingEventAdapter extends RecyclerView.Adapter<UpcomingEventAdapter.ViewHolder> {
    private final List<CalendarEvent> events;

    public UpcomingEventAdapter(List<CalendarEvent> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        holder.eventName.setText(event.getEventName());

        String formattedDate = formatDate(event.getEventDate());
        String formattedTime = formatTime(event.getEventTime());
        holder.eventTime.setText(formattedDate + " - " + formattedTime);

        String location = event.getLocation();
        if (location == null || location.trim().isEmpty()) {
            holder.eventLocation.setVisibility(View.GONE);
        } else {
            holder.eventLocation.setVisibility(View.VISIBLE);
            holder.eventLocation.setText(location);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = input.parse(dateStr);
            if (date == null) {
                return dateStr;
            }
            SimpleDateFormat output = new SimpleDateFormat("yyyy/MMMM/dd", Locale.getDefault());
            return output.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return "All day";
        }
        // Try parsing as HH:mm:ss first, then HH:mm
        String[] patterns = {"HH:mm:ss", "HH:mm"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.ENGLISH);
                Date time = input.parse(timeStr);
                if (time != null) {
                    SimpleDateFormat output = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    return output.format(time);
                }
            } catch (Exception ignored) {
            }
        }
        // If parsing fails, return original
        return timeStr;
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName;
        TextView eventTime;
        TextView eventLocation;

        ViewHolder(View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.upcoming_event_name);
            eventTime = itemView.findViewById(R.id.upcoming_event_time);
            eventLocation = itemView.findViewById(R.id.upcoming_event_location);
        }
    }
}
