package com.example.personaldashboard.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DailyActivityDao {
    @Insert
    void insert(DailyActivity activity);

    @Update
    void update(DailyActivity activity);

    @Delete
    void delete(DailyActivity activity);

    @Query("SELECT * FROM daily_activities WHERE dayOfWeek = :day ORDER BY timeStart ASC")
    List<DailyActivity> getActivitiesForDay(String day);

    @Query("SELECT * FROM daily_activities")
    List<DailyActivity> getAllActivities();

    @Query("SELECT * FROM daily_activities WHERE dayOfWeek = :day AND isCompleted = 1")
    List<DailyActivity> getCompletedActivitiesForDay(String day);
}