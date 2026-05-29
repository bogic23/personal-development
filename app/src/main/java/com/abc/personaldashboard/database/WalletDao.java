package com.abc.personaldashboard.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WalletDao {
    @Insert
    long insert(Wallet wallet);

    @Update
    void update(Wallet wallet);

    @Delete
    void delete(Wallet wallet);

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC")
    List<Wallet> getAllWallets();

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    Wallet getWalletById(int walletId);

    @Query("SELECT * FROM wallets WHERE remoteId = :remoteId LIMIT 1")
    Wallet getWalletByRemoteId(String remoteId);

    @Query("SELECT * FROM wallets WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    Wallet getWalletByName(String name);

    @Query("DELETE FROM wallets")
    void deleteAll();
}
