package com.abc.personaldashboard.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DailyActivity.class, Task.class, CalendarEvent.class, UserProfile.class, Wallet.class, FinanceTransaction.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS daily_activities_new "
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`activityDate` TEXT, "
                    + "`activityName` TEXT, "
                    + "`timeStart` TEXT, "
                    + "`timeEnd` TEXT, "
                    + "`isCompleted` INTEGER NOT NULL)");
            database.execSQL("INSERT INTO daily_activities_new "
                    + "(`id`, `activityDate`, `activityName`, `timeStart`, `timeEnd`, `isCompleted`) "
                    + "SELECT `id`, date('now', 'localtime'), `activityName`, `timeStart`, `timeEnd`, `isCompleted` "
                    + "FROM daily_activities");
            database.execSQL("DROP TABLE daily_activities");
            database.execSQL("ALTER TABLE daily_activities_new RENAME TO daily_activities");
        }
    };

    public abstract DailyActivityDao dailyActivityDao();
    public abstract TaskDao taskDao();
    public abstract CalendarEventDao calendarEventDao();
    public abstract UserProfileDao userProfileDao();
    public abstract WalletDao walletDao();
    public abstract FinanceTransactionDao financeTransactionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "personal_dashboard_db")
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
