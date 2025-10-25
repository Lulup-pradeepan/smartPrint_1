-- Smart Print Queue Management System Database Schema
-- MySQL Database Schema

-- Drop existing tables if they exist
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS print_jobs;
DROP TABLE IF EXISTS users;

-- Users table (for both students and operators)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    user_type ENUM('STUDENT', 'OPERATOR', 'ADMIN') NOT NULL,
    wallet_balance DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Print Jobs table
CREATE TABLE print_jobs (
    job_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_content MEDIUMBLOB,
    document_path VARCHAR(512),
    page_count INT NOT NULL CHECK (page_count > 0),
    num_copies INT NOT NULL CHECK (num_copies > 0),
    total_cost DECIMAL(10, 2) NOT NULL,
    job_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') DEFAULT 'UNPAID',
    payment_type ENUM('PREPAID', 'POSTPAID') NOT NULL,
    queue_position INT,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    operator_id INT NULL,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_job_status (job_status),
    INDEX idx_user_id (user_id),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_queue_position (queue_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Transactions table
CREATE TABLE transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    job_id INT NULL,
    transaction_type ENUM('WALLET_RECHARGE', 'PAYMENT', 'REFUND') NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    balance_before DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES print_jobs(job_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_transaction_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default users (passwords are hashed in application, here using plain text for demo)
INSERT INTO users (username, password, full_name, email, user_type, wallet_balance) VALUES
('student1', 'student123', 'John Doe', 'john.doe@college.edu', 'STUDENT', 100.00),
('student2', 'student123', 'Jane Smith', 'jane.smith@college.edu', 'STUDENT', 50.00),
('operator1', 'operator123', 'Mike Wilson', 'mike.wilson@college.edu', 'OPERATOR', 0.00),
('admin1', 'admin123', 'Admin User', 'admin@college.edu', 'ADMIN', 0.00);

-- Create a view for queue status
CREATE OR REPLACE VIEW queue_status_view AS
SELECT 
    pj.job_id,
    pj.user_id,
    u.username,
    u.full_name,
    pj.document_name,
    pj.page_count,
    pj.num_copies,
    pj.total_cost,
    pj.job_status,
    pj.payment_status,
    pj.payment_type,
    pj.submitted_at,
    pj.started_at,
    pj.completed_at,
    (SELECT COUNT(*) FROM print_jobs pj2 
     WHERE pj2.job_status IN ('PENDING', 'PROCESSING') 
     AND pj2.submitted_at < pj.submitted_at) + 1 AS queue_position
FROM print_jobs pj
JOIN users u ON pj.user_id = u.user_id
WHERE pj.job_status IN ('PENDING', 'PROCESSING')
ORDER BY pj.submitted_at ASC;

-- Stored procedure to calculate print cost (₹2 per page)
DELIMITER //
CREATE PROCEDURE calculate_print_cost(
    IN p_page_count INT,
    IN p_num_copies INT,
    OUT p_total_cost DECIMAL(10, 2)
)
BEGIN
    SET p_total_cost = p_page_count * p_num_copies * 2.00;
END //
DELIMITER ;

-- Trigger to update queue positions
DELIMITER //
CREATE TRIGGER after_job_status_update
AFTER UPDATE ON print_jobs
FOR EACH ROW
BEGIN
    IF OLD.job_status != NEW.job_status AND NEW.job_status IN ('COMPLETED', 'CANCELLED') THEN
        -- Recalculate queue positions for pending jobs
        UPDATE print_jobs 
        SET queue_position = (
            SELECT COUNT(*) FROM print_jobs pj2 
            WHERE pj2.job_status IN ('PENDING', 'PROCESSING') 
            AND pj2.submitted_at < print_jobs.submitted_at
        ) + 1
        WHERE job_status IN ('PENDING', 'PROCESSING');
    END IF;
END //
DELIMITER ;
