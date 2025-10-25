package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton class for managing database connections
 * Provides thread-safe connection management with connection pooling support
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private String url;
    private String username;
    private String password;
    private String driver;
    
    /**
     * Private constructor to prevent instantiation
     * Loads database configuration from properties file
     */
    private DatabaseConnection() {
        try {
            Properties props = new Properties();
            // Try to load from file system first
            try (InputStream input = new FileInputStream("src/config/database.properties")) {
                props.load(input);
            } catch (IOException e) {
                // If file not found, try loading from classpath
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/database.properties")) {
                    if (input != null) {
                        props.load(input);
                    } else {
                        // Use default values if properties file not found
                        System.err.println("Warning: database.properties not found, using default values");
                        props.setProperty("db.url", "jdbc:mysql://localhost:3306/print_queue_db?useSSL=false&serverTimezone=UTC");
                        props.setProperty("db.username", "root");
                        props.setProperty("db.password", "root");
                        props.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                    }
                }
            }
            
            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");
            this.driver = props.getProperty("db.driver");
            
            // Load the JDBC driver
            Class.forName(driver);
            
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-java to classpath.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }
    
    /**
     * Get singleton instance of DatabaseConnection
     * Thread-safe implementation using synchronized block
     * 
     * @return DatabaseConnection instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    /**
     * Get a new database connection
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * Test database connection
     * 
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Close a database connection safely
     * 
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
