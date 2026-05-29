package com.example.personaldashboard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String taskName;
    private String description;
    private int progress; // 0-100
    private String dueDate;
    private boolean isCompleted;
    private String createdAt;

    public Task(String taskName, String description, String dueDate) {
        this.taskName = taskName;
        this.description = description;
        this.progress = 0;
        this.dueDate = dueDate;
        this.isCompleted = false;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}