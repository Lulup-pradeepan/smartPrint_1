import database.DatabaseConnection;
import ui.LoginFrame;

import javax.swing.*;

/**
 * Main application entry point
 * Smart Print Queue Management System
 * 
 * @author College Print Management Team
 * @version 1.0
 */
public class Main {
    
    public static void main(String[] args) {
        // Test database connection on startup
        System.out.println("===========================================");
        System.out.println("Smart Print Queue Management System");
        System.out.println("===========================================");
        System.out.println("Testing database connection...");
        
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        if (dbConnection.testConnection()) {
            System.out.println("✓ Database connection successful!");
            System.out.println("===========================================");
            
            // Set look and feel to system default
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Warning: Could not set system look and feel");
                e.printStackTrace();
            }
            
            // Launch login frame on Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            
        } else {
            System.err.println("✗ Database connection failed!");
            System.err.println("Please check the following:");
            System.err.println("1. MySQL server is running");
            System.err.println("2. Database 'print_queue_db' exists");
            System.err.println("3. Database credentials in database.properties are correct");
            System.err.println("4. MySQL JDBC driver is in classpath");
            System.err.println("===========================================");
            
            // Show error dialog
            JOptionPane.showMessageDialog(null,
                "Failed to connect to database.\n" +
                "Please ensure MySQL is running and database is configured correctly.\n" +
                "Check console for details.",
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE);
            
            System.exit(1);
        }
    }
}
