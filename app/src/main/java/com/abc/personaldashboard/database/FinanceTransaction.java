package com.abc.personaldashboard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "finance_transactions")
public class FinanceTransaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String remoteId;
    private int walletId;
    private String walletRemoteId;
    private String walletOwnerId;
    private String type;
    private double amount;
    private String note;
    private String createdAt;

    public FinanceTransaction(int walletId, String type, double amount, String note) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public int getWalletId() { return walletId; }
    public void setWalletId(int walletId) { this.walletId = walletId; }

    public String getWalletRemoteId() { return walletRemoteId; }
    public void setWalletRemoteId(String walletRemoteId) { this.walletRemoteId = walletRemoteId; }

    public String getWalletOwnerId() { return walletOwnerId; }
    public void setWalletOwnerId(String walletOwnerId) { this.walletOwnerId = walletOwnerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
