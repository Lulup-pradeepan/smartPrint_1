package models;

import java.sql.Timestamp;

/**
 * User model representing students, operators, and admins
 */
public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private UserType userType;
    private double walletBalance;
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private boolean isActive;
    
    public enum UserType {
        STUDENT, OPERATOR, ADMIN
    }
    
    // Constructors
    public User() {}
    
    public User(int userId, String username, String fullName, String email, 
                UserType userType, double walletBalance) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.userType = userType;
        this.walletBalance = walletBalance;
        this.isActive = true;
    }
    
    // Getters and Setters
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public UserType getUserType() {
        return userType;
    }
    
    public void setUserType(UserType userType) {
        this.userType = userType;
    }
    
    public double getWalletBalance() {
        return walletBalance;
    }
    
    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", userType=" + userType +
                ", walletBalance=" + walletBalance +
                '}';
    }
}
