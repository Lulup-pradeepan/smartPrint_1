package services;

import dao.TransactionDAO;
import dao.UserDAO;
import models.Transaction;
import models.Transaction.TransactionType;

/**
 * Service class for handling payment operations
 * Provides business logic for wallet management and transactions
 */
public class PaymentService {
    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    
    public PaymentService() {
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();
    }
    
    /**
     * Recharge wallet with specified amount
     * Creates a transaction record and updates wallet balance
     * 
     * @param userId User ID
     * @param amount Amount to add
     * @return true if successful, false otherwise
     */
    public synchronized boolean rechargeWallet(int userId, double amount) {
        if (amount <= 0) {
            System.err.println("Invalid recharge amount: " + amount);
            return false;
        }
        
        // Get current balance
        double currentBalance = userDAO.getWalletBalance(userId);
        if (currentBalance < 0) {
            return false;
        }
        
        // Update wallet balance
        if (userDAO.updateWalletBalance(userId, amount)) {
            double newBalance = currentBalance + amount;
            
            // Create transaction record
            Transaction transaction = new Transaction(
                userId,
                TransactionType.WALLET_RECHARGE,
                amount,
                currentBalance,
                newBalance,
                "Wallet recharge of ₹" + String.format("%.2f", amount)
            );
            
            int transactionId = transactionDAO.createTransaction(transaction);
            return transactionId > 0;
        }
        
        return false;
    }
    
    /**
     * Process payment for a print job
     * Deducts amount from wallet and creates transaction record
     * 
     * @param userId User ID
     * @param jobId Job ID
     * @param amount Amount to deduct
     * @return true if successful, false otherwise
     */
    public synchronized boolean processPayment(int userId, int jobId, double amount) {
        if (amount <= 0) {
            System.err.println("Invalid payment amount: " + amount);
            return false;
        }
        
        // Get current balance
        double currentBalance = userDAO.getWalletBalance(userId);
        if (currentBalance < 0) {
            return false;
        }
        
        // Check if sufficient balance
        if (currentBalance < amount) {
            System.err.println("Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
            return false;
        }
        
        // Deduct amount from wallet
        if (userDAO.updateWalletBalance(userId, -amount)) {
            double newBalance = currentBalance - amount;
            
            // Create transaction record
            Transaction transaction = new Transaction(
                userId,
                TransactionType.PAYMENT,
                amount,
                currentBalance,
                newBalance,
                "Payment for print job #" + jobId
            );
            transaction.setJobId(jobId);
            
            int transactionId = transactionDAO.createTransaction(transaction);
            return transactionId > 0;
        }
        
        return false;
    }
    
    /**
     * Process refund for a cancelled job
     * Adds amount back to wallet and creates transaction record
     * 
     * @param userId User ID
     * @param jobId Job ID
     * @param amount Amount to refund
     * @return true if successful, false otherwise
     */
    public synchronized boolean processRefund(int userId, int jobId, double amount) {
        if (amount <= 0) {
            System.err.println("Invalid refund amount: " + amount);
            return false;
        }
        
        // Get current balance
        double currentBalance = userDAO.getWalletBalance(userId);
        if (currentBalance < 0) {
            return false;
        }
        
        // Add refund amount to wallet
        if (userDAO.updateWalletBalance(userId, amount)) {
            double newBalance = currentBalance + amount;
            
            // Create transaction record
            Transaction transaction = new Transaction(
                userId,
                TransactionType.REFUND,
                amount,
                currentBalance,
                newBalance,
                "Refund for cancelled job #" + jobId
            );
            transaction.setJobId(jobId);
            
            int transactionId = transactionDAO.createTransaction(transaction);
            return transactionId > 0;
        }
        
        return false;
    }
    
    /**
     * Check if user has sufficient balance for a payment
     * 
     * @param userId User ID
     * @param amount Required amount
     * @return true if sufficient balance, false otherwise
     */
    public boolean hasSufficientBalance(int userId, double amount) {
        double currentBalance = userDAO.getWalletBalance(userId);
        return currentBalance >= amount;
    }
    
    /**
     * Get current wallet balance
     * 
     * @param userId User ID
     * @return Current balance
     */
    public double getWalletBalance(int userId) {
        return userDAO.getWalletBalance(userId);
    }
    
    /**
     * Calculate print cost based on pages and copies
     * Cost: ₹2 per page
     * 
     * @param pageCount Number of pages
     * @param numCopies Number of copies
     * @return Total cost
     */
    public static double calculatePrintCost(int pageCount, int numCopies) {
        final double COST_PER_PAGE = 2.00;
        return pageCount * numCopies * COST_PER_PAGE;
    }
}
