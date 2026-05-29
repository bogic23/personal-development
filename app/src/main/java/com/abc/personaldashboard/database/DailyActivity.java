package com.abc.personaldashboard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_activities")
public class DailyActivity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String dayOfWeek; // SUNDAY, MONDAY, etc.
    private String activityName;
    private String timeStart;
    private String timeEnd;
    private boolean isCompleted;

    // Constructor
    public DailyActivity(String dayOfWeek, String activityName, String timeStart, String timeEnd) {
        this.dayOfWeek = dayOfWeek;
        this.activityName = activityName;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.isCompleted = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getTimeStart() { return timeStart; }
    public void setTimeStart(String timeStart) { this.timeStart = timeStart; }

    public String getTimeEnd() { return timeEnd; }
    public void setTimeEnd(String timeEnd) { this.timeEnd = timeEnd; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}