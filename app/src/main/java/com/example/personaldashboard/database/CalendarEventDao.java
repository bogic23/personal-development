package com.example.personaldashboard.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface CalendarEventDao {
    @Insert
    void insert(CalendarEvent event);

    @Update
    void update(CalendarEvent event);

    @Delete
    void delete(CalendarEvent event);

    @Query("SELECT * FROM calendar_events WHERE eventDate = :date ORDER BY eventTime ASC")
    List<CalendarEvent> getEventsForDate(String date);

    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC, eventTime ASC")
    List<CalendarEvent> getAllEvents();
}