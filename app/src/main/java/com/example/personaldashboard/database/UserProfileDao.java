package com.example.personaldashboard.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserProfileDao {
    @Insert
    void insert(UserProfile userProfile);

    @Update
    void update(UserProfile userProfile);

    @Query("SELECT * FROM user_profile LIMIT 1")
    UserProfile getProfile();

    @Query("SELECT * FROM user_profile WHERE name = :name AND password = :password LIMIT 1")
    UserProfile login(String name, String password);
}