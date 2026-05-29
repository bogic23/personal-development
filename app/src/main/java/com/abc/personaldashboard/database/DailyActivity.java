package com.abc.personaldashboard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_activities")
public class DailyActivity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String activityDate;
    private String activityName;
    private String timeStart;
    private String timeEnd;
    private boolean isCompleted;

    public DailyActivity(String activityDate, String activityName, String timeStart, String timeEnd) {
        this.activityDate = activityDate;
        this.activityName = activityName;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.isCompleted = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getActivityDate() { return activityDate; }
    public void setActivityDate(String activityDate) { this.activityDate = activityDate; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getTimeStart() { return timeStart; }
    public void setTimeStart(String timeStart) { this.timeStart = timeStart; }

    public String getTimeEnd() { return timeEnd; }
    public void setTimeEnd(String timeEnd) { this.timeEnd = timeEnd; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
