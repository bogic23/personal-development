package com.abc.personaldashboard.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface FinanceTransactionDao {
    @Insert
    void insert(FinanceTransaction transaction);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM finance_transactions WHERE type = :type")
    double getTotalByType(String type);

    @Query("DELETE FROM finance_transactions WHERE walletId = :walletId")
    void deleteByWalletId(int walletId);

    @Query("DELETE FROM finance_transactions")
    void deleteAll();
}
