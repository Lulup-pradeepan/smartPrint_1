package tests;

import dao.UserDAO;
import dao.PrintJobDAO;
import dao.TransactionDAO;
import models.User;
import models.PrintJob;
import models.Transaction;
import models.User.UserType;
import models.PrintJob.PaymentType;
import services.PaymentService;

/**
 * Test Suite for Smart Print Queue Management System
 * Manual testing scenarios for core functionalities
 */
public class TestSuite {
    
    private UserDAO userDAO;
    private PrintJobDAO printJobDAO;
    private TransactionDAO transactionDAO;
    private PaymentService paymentService;
    
    public TestSuite() {
        this.userDAO = new UserDAO();
        this.printJobDAO = new PrintJobDAO();
        this.transactionDAO = new TransactionDAO();
        this.paymentService = new PaymentService();
    }
    
    /**
     * Test 1: User Authentication
     */
    public void testUserAuthentication() {
        System.out.println("\n=== Test 1: User Authentication ===");
        
        // Test valid student login
        User student = userDAO.authenticate("student1", "student123");
        if (student != null && student.getUserType() == UserType.STUDENT) {
            System.out.println("✓ Valid student login successful");
        } else {
            System.out.println("✗ Valid student login failed");
        }
        
        // Test valid operator login
        User operator = userDAO.authenticate("operator1", "operator123");
        if (operator != null && operator.getUserType() == UserType.OPERATOR) {
            System.out.println("✓ Valid operator login successful");
        } else {
            System.out.println("✗ Valid operator login failed");
        }
        
        // Test invalid login
        User invalid = userDAO.authenticate("invalid", "wrong");
        if (invalid == null) {
            System.out.println("✓ Invalid login correctly rejected");
        } else {
            System.out.println("✗ Invalid login incorrectly accepted");
        }
    }
    
    /**
     * Test 2: Wallet Operations
     */
    public void testWalletOperations() {
        System.out.println("\n=== Test 2: Wallet Operations ===");
        
        User student = userDAO.authenticate("student1", "student123");
        if (student == null) {
            System.out.println("✗ Cannot test wallet - user not found");
            return;
        }
        
        double initialBalance = student.getWalletBalance();
        System.out.println("Initial balance: ₹" + String.format("%.2f", initialBalance));
        
        // Test wallet recharge
        double rechargeAmount = 50.0;
        boolean rechargeSuccess = paymentService.rechargeWallet(student.getUserId(), rechargeAmount);
        
        if (rechargeSuccess) {
            double newBalance = userDAO.getWalletBalance(student.getUserId());
            if (Math.abs(newBalance - (initialBalance + rechargeAmount)) < 0.01) {
                System.out.println("✓ Wallet recharge successful: ₹" + String.format("%.2f", newBalance));
            } else {
                System.out.println("✗ Wallet balance mismatch after recharge");
            }
        } else {
            System.out.println("✗ Wallet recharge failed");
        }
        
        // Test invalid recharge
        boolean invalidRecharge = paymentService.rechargeWallet(student.getUserId(), -10.0);
        if (!invalidRecharge) {
            System.out.println("✓ Invalid recharge amount correctly rejected");
        } else {
            System.out.println("✗ Invalid recharge amount incorrectly accepted");
        }
    }
    
    /**
     * Test 3: Print Job Submission
     */
    public void testPrintJobSubmission() {
        System.out.println("\n=== Test 3: Print Job Submission ===");
        
        User student = userDAO.authenticate("student1", "student123");
        if (student == null) {
            System.out.println("✗ Cannot test job submission - user not found");
            return;
        }
        
        // Test prepaid job submission
        double cost = PaymentService.calculatePrintCost(10, 2);
        System.out.println("Calculated cost for 10 pages × 2 copies: ₹" + String.format("%.2f", cost));
        
        if (cost == 40.0) {
            System.out.println("✓ Cost calculation correct");
        } else {
            System.out.println("✗ Cost calculation incorrect");
        }
        
        // Create print job
        PrintJob job = new PrintJob(
            student.getUserId(),
            "Test Document",
            10,
            2,
            cost,
            PaymentType.PREPAID
        );
        
        int jobId = printJobDAO.createPrintJob(job);
        
        if (jobId > 0) {
            System.out.println("✓ Print job created successfully: Job ID " + jobId);
            
            // Process payment
            boolean paymentSuccess = paymentService.processPayment(student.getUserId(), jobId, cost);
            if (paymentSuccess) {
                System.out.println("✓ Payment processed successfully");
            } else {
                System.out.println("✗ Payment processing failed");
            }
        } else {
            System.out.println("✗ Print job creation failed");
        }
    }
    
    /**
     * Test 4: Queue Management
     */
    public void testQueueManagement() {
        System.out.println("\n=== Test 4: Queue Management ===");
        
        // Get queue jobs
        var queueJobs = printJobDAO.getQueueJobs();
        System.out.println("Current queue size: " + queueJobs.size());
        
        if (!queueJobs.isEmpty()) {
            System.out.println("✓ Queue jobs retrieved");
            
            // Verify FCFS order
            boolean correctOrder = true;
            for (int i = 0; i < queueJobs.size() - 1; i++) {
                if (queueJobs.get(i).getSubmittedAt().after(queueJobs.get(i + 1).getSubmittedAt())) {
                    correctOrder = false;
                    break;
                }
            }
            
            if (correctOrder) {
                System.out.println("✓ Queue maintains FCFS order");
            } else {
                System.out.println("✗ Queue order incorrect");
            }
            
            // Display queue positions
            System.out.println("\nQueue positions:");
            for (PrintJob job : queueJobs) {
                System.out.println("  Position " + job.getQueuePosition() + ": Job #" + 
                                 job.getJobId() + " - " + job.getDocumentName());
            }
        } else {
            System.out.println("Queue is empty");
        }
    }
    
    /**
     * Test 5: Transaction Recording
     */
    public void testTransactionRecording() {
        System.out.println("\n=== Test 5: Transaction Recording ===");
        
        User student = userDAO.authenticate("student1", "student123");
        if (student == null) {
            System.out.println("✗ Cannot test transactions - user not found");
            return;
        }
        
        var transactions = transactionDAO.getTransactionsByUserId(student.getUserId());
        System.out.println("Total transactions for " + student.getUsername() + ": " + transactions.size());
        
        if (!transactions.isEmpty()) {
            System.out.println("✓ Transactions retrieved");
            
            // Display recent transactions
            System.out.println("\nRecent transactions:");
            int count = Math.min(5, transactions.size());
            for (int i = 0; i < count; i++) {
                Transaction trans = transactions.get(i);
                System.out.println("  " + trans.getTransactionType() + ": ₹" + 
                                 String.format("%.2f", trans.getAmount()) + 
                                 " - Balance: ₹" + String.format("%.2f", trans.getBalanceAfter()));
            }
        } else {
            System.out.println("No transactions found");
        }
    }
    
    /**
     * Test 6: Concurrent Operations
     */
    public void testConcurrentOperations() {
        System.out.println("\n=== Test 6: Concurrent Operations ===");
        
        User student = userDAO.authenticate("student1", "student123");
        if (student == null) {
            System.out.println("✗ Cannot test concurrent operations - user not found");
            return;
        }
        
        final int userId = student.getUserId();
        final double initialBalance = userDAO.getWalletBalance(userId);
        
        System.out.println("Testing concurrent wallet operations...");
        System.out.println("Initial balance: ₹" + String.format("%.2f", initialBalance));
        
        // Create multiple threads to test thread safety
        Thread[] threads = new Thread[5];
        final boolean[] results = new boolean[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = paymentService.rechargeWallet(userId, 10.0);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Check results
        int successCount = 0;
        for (boolean result : results) {
            if (result) successCount++;
        }
        
        double finalBalance = userDAO.getWalletBalance(userId);
        double expectedBalance = initialBalance + (successCount * 10.0);
        
        System.out.println("Successful operations: " + successCount + "/5");
        System.out.println("Final balance: ₹" + String.format("%.2f", finalBalance));
        System.out.println("Expected balance: ₹" + String.format("%.2f", expectedBalance));
        
        if (Math.abs(finalBalance - expectedBalance) < 0.01) {
            System.out.println("✓ Concurrent operations handled correctly");
        } else {
            System.out.println("✗ Concurrent operations resulted in incorrect balance");
        }
    }
    
    /**
     * Test 7: Data Integrity
     */
    public void testDataIntegrity() {
        System.out.println("\n=== Test 7: Data Integrity ===");
        
        // Test user data
        var students = userDAO.getUsersByType(UserType.STUDENT);
        System.out.println("Total students: " + students.size());
        
        boolean allValid = true;
        for (User student : students) {
            if (student.getWalletBalance() < 0) {
                System.out.println("✗ Negative balance found for user: " + student.getUsername());
                allValid = false;
            }
        }
        
        if (allValid) {
            System.out.println("✓ All wallet balances are valid");
        }
        
        // Test print job data
        var allJobs = printJobDAO.getQueueJobs();
        allValid = true;
        for (PrintJob job : allJobs) {
            if (job.getPageCount() <= 0 || job.getNumCopies() <= 0) {
                System.out.println("✗ Invalid job data found: Job #" + job.getJobId());
                allValid = false;
            }
        }
        
        if (allValid) {
            System.out.println("✓ All print job data is valid");
        }
    }
    
    /**
     * Run all tests
     */
    public void runAllTests() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Smart Print Queue Management System      ║");
        System.out.println("║  Test Suite Execution                     ║");
        System.out.println("╚════════════════════════════════════════════╝");
        
        testUserAuthentication();
        testWalletOperations();
        testPrintJobSubmission();
        testQueueManagement();
        testTransactionRecording();
        testConcurrentOperations();
        testDataIntegrity();
        
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Test Suite Completed                     ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
    }
    
    /**
     * Main method to run tests
     */
    public static void main(String[] args) {
        System.out.println("Initializing test suite...");
        
        // Test database connection first
        database.DatabaseConnection dbConn = database.DatabaseConnection.getInstance();
        if (!dbConn.testConnection()) {
            System.err.println("Database connection failed. Cannot run tests.");
            System.exit(1);
        }
        
        TestSuite testSuite = new TestSuite();
        testSuite.runAllTests();
    }
}
