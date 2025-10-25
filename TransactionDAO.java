package dao;

import database.DatabaseConnection;
import models.Transaction;
import models.Transaction.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Transaction operations
 * Handles all database operations related to transactions
 */
public class TransactionDAO {
    
    /**
     * Create a new transaction record
     * 
     * @param transaction Transaction object to create
     * @return Generated transaction ID or -1 if failed
     */
    public int createTransaction(Transaction transaction) {
        String query = "INSERT INTO transactions (user_id, job_id, transaction_type, amount, " +
                      "balance_before, balance_after, description) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, transaction.getUserId());
            stmt.setObject(2, transaction.getJobId());
            stmt.setString(3, transaction.getTransactionType().name());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setDouble(5, transaction.getBalanceBefore());
            stmt.setDouble(6, transaction.getBalanceAfter());
            stmt.setString(7, transaction.getDescription());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating transaction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Get all transactions for a specific user
     * 
     * @param userId User ID
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByUserId(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT t.*, u.username FROM transactions t " +
                      "JOIN users u ON t.user_id = u.user_id " +
                      "WHERE t.user_id = ? ORDER BY t.transaction_date DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(extractTransactionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching user transactions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    /**
     * Get all transactions (for admin view)
     * 
     * @return List of all transactions
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT t.*, u.username FROM transactions t " +
                      "JOIN users u ON t.user_id = u.user_id " +
                      "ORDER BY t.transaction_date DESC LIMIT 1000";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(extractTransactionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching all transactions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    /**
     * Get transactions by type
     * 
     * @param transactionType Type of transaction
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByType(TransactionType transactionType) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT t.*, u.username FROM transactions t " +
                      "JOIN users u ON t.user_id = u.user_id " +
                      "WHERE t.transaction_type = ? ORDER BY t.transaction_date DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, transactionType.name());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(extractTransactionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching transactions by type: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    /**
     * Get transactions for a specific job
     * 
     * @param jobId Job ID
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByJobId(int jobId) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT t.*, u.username FROM transactions t " +
                      "JOIN users u ON t.user_id = u.user_id " +
                      "WHERE t.job_id = ? ORDER BY t.transaction_date DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, jobId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(extractTransactionFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching job transactions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    /**
     * Get total transaction amount by type for a user
     * 
     * @param userId User ID
     * @param transactionType Transaction type
     * @return Total amount
     */
    public double getTotalAmountByType(int userId, TransactionType transactionType) {
        String query = "SELECT COALESCE(SUM(amount), 0) AS total FROM transactions " +
                      "WHERE user_id = ? AND transaction_type = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, transactionType.name());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating total amount: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    /**
     * Extract Transaction object from ResultSet
     * 
     * @param rs ResultSet containing transaction data
     * @return Transaction object
     * @throws SQLException if error reading from ResultSet
     */
    private Transaction extractTransactionFromResultSet(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(rs.getInt("transaction_id"));
        transaction.setUserId(rs.getInt("user_id"));
        
        // Handle nullable job_id
        int jobId = rs.getInt("job_id");
        if (!rs.wasNull()) {
            transaction.setJobId(jobId);
        }
        
        transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setBalanceBefore(rs.getDouble("balance_before"));
        transaction.setBalanceAfter(rs.getDouble("balance_after"));
        transaction.setTransactionDate(rs.getTimestamp("transaction_date"));
        transaction.setDescription(rs.getString("description"));
        
        // Additional display field
        try {
            transaction.setUsername(rs.getString("username"));
        } catch (SQLException e) {
            // This field may not be present in all queries
        }
        
        return transaction;
    }
}
