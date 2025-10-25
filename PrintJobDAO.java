package dao;

import database.DatabaseConnection;
import models.PrintJob;
import models.PrintJob.JobStatus;
import models.PrintJob.PaymentStatus;
import models.PrintJob.PaymentType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for PrintJob operations
 * Handles all database operations related to print jobs
 */
public class PrintJobDAO {
    
    /**
     * Create a new print job
     * Thread-safe implementation with proper queue position assignment
     * 
     * @param job PrintJob object to create
     * @return Generated job ID or -1 if failed
     */
    public synchronized int createPrintJob(PrintJob job) {
        String query = "INSERT INTO print_jobs (user_id, document_name, document_content, document_path, " +
                      "page_count, num_copies, total_cost, job_status, payment_status, payment_type, queue_position) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                      "(SELECT COALESCE(MAX(queue_position), 0) + 1 FROM print_jobs pj WHERE pj.job_status = 'PENDING'))";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, job.getUserId());
            stmt.setString(2, job.getDocumentName());
            stmt.setBytes(3, job.getDocumentContent());
            stmt.setString(4, job.getDocumentPath());
            stmt.setInt(5, job.getPageCount());
            stmt.setInt(6, job.getNumCopies());
            stmt.setDouble(7, job.getTotalCost());
            stmt.setString(8, JobStatus.PENDING.name());
            stmt.setString(9, job.getPaymentType() == PaymentType.PREPAID ? 
                         PaymentStatus.PAID.name() : PaymentStatus.UNPAID.name());
            stmt.setString(10, job.getPaymentType().name());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating print job: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Get print job by ID
     * 
     * @param jobId Job ID
     * @return PrintJob object or null if not found
     */
    public PrintJob getJobById(int jobId) {
        String query = "SELECT pj.*, u.username, u.full_name FROM print_jobs pj " +
                      "JOIN users u ON pj.user_id = u.user_id WHERE pj.job_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, jobId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return extractPrintJobFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching print job: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get all print jobs for a specific user
     * 
     * @param userId User ID
     * @return List of print jobs
     */
    public List<PrintJob> getJobsByUserId(int userId) {
        List<PrintJob> jobs = new ArrayList<>();
        String query = "SELECT pj.*, u.username, u.full_name FROM print_jobs pj " +
                      "JOIN users u ON pj.user_id = u.user_id " +
                      "WHERE pj.user_id = ? ORDER BY pj.submitted_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                jobs.add(extractPrintJobFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching user jobs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return jobs;
    }
    
    /**
     * Get all pending and processing jobs (queue view)
     * Ordered by submission time (FCFS)
     * 
     * @return List of print jobs in queue
     */
    public List<PrintJob> getQueueJobs() {
        List<PrintJob> jobs = new ArrayList<>();
        String query = "SELECT pj.*, u.username, u.full_name, " +
                      "(SELECT COUNT(*) FROM print_jobs pj2 " +
                      " WHERE pj2.job_status IN ('PENDING', 'PROCESSING') " +
                      " AND pj2.submitted_at < pj.submitted_at) + 1 AS queue_position " +
                      "FROM print_jobs pj " +
                      "JOIN users u ON pj.user_id = u.user_id " +
                      "WHERE pj.job_status IN ('PENDING', 'PROCESSING') " +
                      "ORDER BY pj.submitted_at ASC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PrintJob job = extractPrintJobFromResultSet(rs);
                job.setQueuePosition(rs.getInt("queue_position"));
                jobs.add(job);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching queue jobs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return jobs;
    }
    
    /**
     * Get all completed jobs
     * 
     * @return List of completed print jobs
     */
    public List<PrintJob> getCompletedJobs() {
        List<PrintJob> jobs = new ArrayList<>();
        String query = "SELECT pj.*, u.username, u.full_name FROM print_jobs pj " +
                      "JOIN users u ON pj.user_id = u.user_id " +
                      "WHERE pj.job_status = 'COMPLETED' " +
                      "ORDER BY pj.completed_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                jobs.add(extractPrintJobFromResultSet(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching completed jobs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return jobs;
    }
    
    /**
     * Update job status
     * Thread-safe implementation with proper timestamp updates
     * 
     * @param jobId Job ID
     * @param newStatus New job status
     * @param operatorId Operator performing the update
     * @return true if successful, false otherwise
     */
    public synchronized boolean updateJobStatus(int jobId, JobStatus newStatus, Integer operatorId) {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Build query based on new status
            String query;
            if (newStatus == JobStatus.PROCESSING) {
                query = "UPDATE print_jobs SET job_status = ?, started_at = CURRENT_TIMESTAMP, " +
                       "operator_id = ? WHERE job_id = ?";
            } else if (newStatus == JobStatus.COMPLETED) {
                query = "UPDATE print_jobs SET job_status = ?, completed_at = CURRENT_TIMESTAMP, " +
                       "operator_id = ? WHERE job_id = ?";
            } else {
                query = "UPDATE print_jobs SET job_status = ?, operator_id = ? WHERE job_id = ?";
            }
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus.name());
            stmt.setObject(2, operatorId);
            stmt.setInt(3, jobId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating job status: " + e.getMessage());
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
     * Update payment status
     * 
     * @param jobId Job ID
     * @param paymentStatus New payment status
     * @return true if successful, false otherwise
     */
    public boolean updatePaymentStatus(int jobId, PaymentStatus paymentStatus) {
        String query = "UPDATE print_jobs SET payment_status = ? WHERE job_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, paymentStatus.name());
            stmt.setInt(2, jobId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payment status: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get queue position for a specific job
     * 
     * @param jobId Job ID
     * @return Queue position or -1 if not in queue
     */
    public int getQueuePosition(int jobId) {
        String query = "SELECT COUNT(*) + 1 AS position FROM print_jobs pj1, print_jobs pj2 " +
                      "WHERE pj1.job_id = ? AND pj2.job_status IN ('PENDING', 'PROCESSING') " +
                      "AND pj2.submitted_at < pj1.submitted_at";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, jobId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("position");
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching queue position: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Get pending jobs for a user (for queue status display)
     * 
     * @param userId User ID
     * @return List of pending/processing jobs
     */
    public List<PrintJob> getPendingJobsByUserId(int userId) {
        List<PrintJob> jobs = new ArrayList<>();
        String query = "SELECT pj.*, u.username, u.full_name, " +
                      "(SELECT COUNT(*) FROM print_jobs pj2 " +
                      " WHERE pj2.job_status IN ('PENDING', 'PROCESSING') " +
                      " AND pj2.submitted_at < pj.submitted_at) + 1 AS queue_position " +
                      "FROM print_jobs pj " +
                      "JOIN users u ON pj.user_id = u.user_id " +
                      "WHERE pj.user_id = ? AND pj.job_status IN ('PENDING', 'PROCESSING') " +
                      "ORDER BY pj.submitted_at ASC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PrintJob job = extractPrintJobFromResultSet(rs);
                job.setQueuePosition(rs.getInt("queue_position"));
                jobs.add(job);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching pending jobs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return jobs;
    }
    
    /**
     * Extract PrintJob object from ResultSet
     * 
     * @param rs ResultSet containing print job data
     * @return PrintJob object
     * @throws SQLException if error reading from ResultSet
     */
    private PrintJob extractPrintJobFromResultSet(ResultSet rs) throws SQLException {
        PrintJob job = new PrintJob();
        job.setJobId(rs.getInt("job_id"));
        job.setUserId(rs.getInt("user_id"));
        job.setDocumentName(rs.getString("document_name"));
        job.setPageCount(rs.getInt("page_count"));
        job.setNumCopies(rs.getInt("num_copies"));
        job.setTotalCost(rs.getDouble("total_cost"));
        job.setJobStatus(JobStatus.valueOf(rs.getString("job_status")));
        job.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        job.setPaymentType(PaymentType.valueOf(rs.getString("payment_type")));
        job.setSubmittedAt(rs.getTimestamp("submitted_at"));
        job.setStartedAt(rs.getTimestamp("started_at"));
        job.setCompletedAt(rs.getTimestamp("completed_at"));
        
        // Handle nullable operator_id
        int operatorId = rs.getInt("operator_id");
        if (!rs.wasNull()) {
            job.setOperatorId(operatorId);
        }
        
        job.setNotes(rs.getString("notes"));
        
        // Additional display fields
        try {
            job.setUsername(rs.getString("username"));
            job.setFullName(rs.getString("full_name"));
        } catch (SQLException e) {
            // These fields may not be present in all queries
        }
        
        return job;
    }
}
