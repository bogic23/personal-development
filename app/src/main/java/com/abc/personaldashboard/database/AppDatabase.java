package com.abc.personaldashboard.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.database.Cursor;

@Database(entities = {DailyActivity.class, Task.class, CalendarEvent.class, UserProfile.class, Wallet.class, FinanceTransaction.class}, version = 7, exportSchema = false)
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
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addFinanceSharingColumns(database);
        }
    };
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addFinanceSharingColumns(database);
        }
    };
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addFinanceSharingColumns(database);
            rebuildWalletsTable(database);
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    private static void addFinanceSharingColumns(@NonNull SupportSQLiteDatabase database) {
        addColumnIfMissing(database, "wallets", "ownerId", "TEXT");
        addColumnIfMissing(database, "wallets", "shared", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(database, "wallets", "sharedCanEditBalance", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(database, "wallets", "sharedWith", "TEXT");
        addColumnIfMissing(database, "wallets", "sharedWithEmail", "TEXT");
        addColumnIfMissing(database, "finance_transactions", "walletOwnerId", "TEXT");
    }

    private static void addColumnIfMissing(@NonNull SupportSQLiteDatabase database, String tableName, String columnName, String columnDefinition) {
        if (hasColumn(database, tableName, columnName)) {
            return;
        }

        database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
    }

    private static boolean hasColumn(@NonNull SupportSQLiteDatabase database, String tableName, String columnName) {
        Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)");
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    private static void rebuildWalletsTable(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS wallets_new "
                + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`remoteId` TEXT, "
                + "`ownerId` TEXT, "
                + "`name` TEXT, "
                + "`balance` REAL NOT NULL, "
                + "`createdAt` TEXT, "
                + "`shared` INTEGER NOT NULL, "
                + "`sharedCanEditBalance` INTEGER NOT NULL, "
                + "`sharedWith` TEXT, "
                + "`sharedWithEmail` TEXT)");
        database.execSQL("INSERT INTO wallets_new "
                + "(`id`, `remoteId`, `ownerId`, `name`, `balance`, `createdAt`, `shared`, `sharedCanEditBalance`, `sharedWith`, `sharedWithEmail`) "
                + "SELECT `id`, `remoteId`, "
                + ownerIdSelect(database) + ", "
                + "`name`, `balance`, `createdAt`, "
                + "COALESCE(`shared`, 0), "
                + "COALESCE(`sharedCanEditBalance`, 0), "
                + "`sharedWith`, `sharedWithEmail` "
                + "FROM wallets");
        database.execSQL("DROP TABLE wallets");
        database.execSQL("ALTER TABLE wallets_new RENAME TO wallets");
    }

    private static String ownerIdSelect(@NonNull SupportSQLiteDatabase database) {
        if (hasColumn(database, "wallets", "ownerUid")) {
            return "COALESCE(`ownerId`, `ownerUid`)";
        }

        return "`ownerId`";
    }
}
