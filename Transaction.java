package models;

import java.sql.Timestamp;

/**
 * Transaction model representing payment and wallet transactions
 */
public class Transaction {
    private int transactionId;
    private int userId;
    private Integer jobId;
    private TransactionType transactionType;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private Timestamp transactionDate;
    private String description;
    
    // Additional fields for display
    private String username;
    
    public enum TransactionType {
        WALLET_RECHARGE, PAYMENT, REFUND
    }
    
    // Constructors
    public Transaction() {}
    
    public Transaction(int userId, TransactionType transactionType, double amount, 
                      double balanceBefore, double balanceAfter, String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }
    
    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public Integer getJobId() {
        return jobId;
    }
    
    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
    
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public double getBalanceBefore() {
        return balanceBefore;
    }
    
    public void setBalanceBefore(double balanceBefore) {
        this.balanceBefore = balanceBefore;
    }
    
    public double getBalanceAfter() {
        return balanceAfter;
    }
    
    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
    
    public Timestamp getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", userId=" + userId +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
