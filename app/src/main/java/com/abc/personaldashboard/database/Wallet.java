package com.abc.personaldashboard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallets")
public class Wallet {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String remoteId;
    private String ownerId;
    private String name;
    private double balance;
    private String createdAt;
    private boolean shared;
    private boolean sharedCanEditBalance;
    private String sharedWith;
    private String sharedWithEmail;

    public Wallet(String name, double balance) {
        this.name = name;
        this.balance = balance;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }

    public boolean isSharedCanEditBalance() { return sharedCanEditBalance; }
    public void setSharedCanEditBalance(boolean sharedCanEditBalance) { this.sharedCanEditBalance = sharedCanEditBalance; }

    public String getSharedWith() { return sharedWith; }
    public void setSharedWith(String sharedWith) { this.sharedWith = sharedWith; }

    public String getSharedWithEmail() { return sharedWithEmail; }
    public void setSharedWithEmail(String sharedWithEmail) { this.sharedWithEmail = sharedWithEmail; }
}
