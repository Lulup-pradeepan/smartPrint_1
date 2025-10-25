package dao;

import database.DatabaseConnection;
import models.User;
import models.User.UserType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations
 * Handles all database operations related to users
 */
public class UserDAO {
    
    /**
     * Authenticate user with username and password
     * 
     * @param username User's username
     * @param password User's password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticate(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = extractUserFromResultSet(rs);
                
                // Update last login
                updateLastLogin(user.getUserId());
                
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User object or null if not found
     */
    public User getUserById(int userId) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get all users by type
     * 
     * @param userType Type of users to retrieve
     * @return List of users
     */
    public List<User> getUsersByType(UserType userType) {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE user_type = ? AND is_active = TRUE ORDER BY full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userType.name());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    /**
     * Update user's wallet balance
     * Thread-safe implementation using database transaction
     * 
     * @param userId User ID
     * @param amount Amount to add (positive) or deduct (negative)
     * @return true if successful, false otherwise
     */
    public synchronized boolean updateWalletBalance(int userId, double amount) {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Get current balance with row lock
            String selectQuery = "SELECT wallet_balance FROM users WHERE user_id = ? FOR UPDATE";
            PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, userId);
            ResultSet rs = selectStmt.executeQuery();
            
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            
            double currentBalance = rs.getDouble("wallet_balance");
            double newBalance = currentBalance + amount;
            
            // Check for negative balance
            if (newBalance < 0) {
                conn.rollback();
                return false;
            }
            
            // Update balance
            String updateQuery = "UPDATE users SET wallet_balance = ? WHERE user_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setDouble(1, newBalance);
            updateStmt.setInt(2, userId);
            
            int rowsAffected = updateStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating wallet balance: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Get current wallet balance
     * 
     * @param userId User ID
     * @return Current balance or -1 if error
     */
    public double getWalletBalance(int userId) {
        String query = "SELECT wallet_balance FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("wallet_balance");
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching wallet balance: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Create a new user
     * 
     * @param user User object to create
     * @return Generated user ID or -1 if failed
     */
    public int createUser(User user) {
        String query = "INSERT INTO users (username, password, full_name, email, user_type, wallet_balance) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getUserType().name());
            stmt.setDouble(6, user.getWalletBalance());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Update last login timestamp
     * 
     * @param userId User ID
     */
    private void updateLastLogin(int userId) {
        String query = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }
    
    /**
     * Extract User object from ResultSet
     * 
     * @param rs ResultSet containing user data
     * @return User object
     * @throws SQLException if error reading from ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setUserType(UserType.valueOf(rs.getString("user_type")));
        user.setWalletBalance(rs.getDouble("wallet_balance"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
