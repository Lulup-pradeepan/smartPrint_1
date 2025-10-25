package ui;

import dao.PrintJobDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import models.PrintJob;
import models.PrintJob.JobStatus;
import models.PrintJob.PaymentStatus;
import models.Transaction;
import models.Transaction.TransactionType;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import javax.swing.Timer;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Operator Portal - Main interface for print operators
 * This class provides functionality for operators to:
 * - View and manage the print queue
 * - Process print jobs
 * - Handle payments and transactions
 * - View completed jobs
 */
public class OperatorPortal extends JFrame {
    private User currentUser;
    private PrintJobDAO printJobDAO;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;
    private Timer refreshTimer;
    private JTabbedPane tabbedPane;
    private DefaultTableModel queueModel;
    private DefaultTableModel completedModel;
    private JPanel mainPanel;

    public OperatorPortal(User operator) {
        this.currentUser = operator;
        this.printJobDAO = new PrintJobDAO();
        this.transactionDAO = new TransactionDAO();
        this.userDAO = new UserDAO();
        
        initializeUI();
        refreshData();
        startAutoRefresh();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("Print Shop Management System");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(236, 240, 241));

        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Initialize tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.addTab("Print Queue", createQueuePanel());
        tabbedPane.addTab("Completed Jobs", createCompletedJobsPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(30000, e -> {
            SwingUtilities.invokeLater(() -> refreshData());
        });
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();
    }

    private void refreshData() {
        List<PrintJob> queueJobs = printJobDAO.getQueueJobs();
        updateQueueTable(queueJobs);
        
        List<PrintJob> completedJobs = printJobDAO.getCompletedJobs();
        updateCompletedTable(completedJobs);
    }

    private void logout() {
        refreshTimer.stop();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(142, 68, 173));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Create left panel for welcome message
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);

        // Create welcome label
        JLabel welcomeLabel = new JLabel("Operator Portal - " + currentUser.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);
        leftPanel.add(welcomeLabel);

        // Create right panel for buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        // Create refresh button
        JButton refreshButton = new JButton("⟳ Refresh");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshButton.setBackground(new Color(52, 152, 219));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> refreshData());
        rightPanel.add(refreshButton);

        // Create logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        logoutButton.setBackground(new Color(231, 76, 60));
        logoutButton.setForeground(Color.BLACK);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logout());
        rightPanel.add(logoutButton);

        // Add panels to header
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Print Queue Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(142, 68, 173));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Create table model
        String[] columns = {
            "Job ID", "Student", "Document", "Pages", "Copies", "Cost", 
            "Payment", "Status", "Submitted", "Actions"
        };

        queueModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table
        JTable queueTable = new JTable(queueModel);
        setupTable(queueTable);
        
        // Add status column renderer
        TableColumn statusColumn = queueTable.getColumnModel().getColumn(7);
        statusColumn.setCellRenderer(new StatusColumnRenderer());

        // Add payment status column renderer
        TableColumn paymentColumn = queueTable.getColumnModel().getColumn(6);
        paymentColumn.setCellRenderer(new PaymentStatusRenderer());

        // Add download button column
        TableColumn actionColumn = queueTable.getColumnModel().getColumn(9);
        actionColumn.setCellRenderer(new ButtonRenderer());
        actionColumn.setCellEditor(new ButtonEditor(queueTable));

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(queueTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        // Create action buttons
        JButton processButton = createActionButton("Process Selected", new Color(46, 204, 113));
        processButton.addActionListener(e -> processSelectedJob(queueTable));
        buttonPanel.add(processButton);

        JButton cancelButton = createActionButton("Cancel Selected", new Color(231, 76, 60));
        cancelButton.addActionListener(e -> cancelSelectedJob(queueTable));
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCompletedJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Completed Jobs");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(142, 68, 173));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Create table model
        String[] columns = {
            "Job ID", "Student", "Document", "Pages", "Copies", "Cost", 
            "Payment", "Status", "Completed"
        };

        completedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table
        JTable completedTable = new JTable(completedModel);
        setupTable(completedTable);
        
        // Add status column renderer
        TableColumn statusColumn = completedTable.getColumnModel().getColumn(7);
        statusColumn.setCellRenderer(new StatusColumnRenderer());

        // Add payment status column renderer
        TableColumn paymentColumn = completedTable.getColumnModel().getColumn(6);
        paymentColumn.setCellRenderer(new PaymentStatusRenderer());

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(completedTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(52, 73, 94));
        table.getTableHeader().setForeground(Color.BLACK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(189, 195, 199));
    }
    
    private void downloadPrintJobFile(int jobId) {
        PrintJob job = printJobDAO.getJobById(jobId);
        if (job == null || job.getDocumentContent() == null) {
            JOptionPane.showMessageDialog(this,
                "Could not retrieve the document content.",
                "Download Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Print Job File");
        fileChooser.setSelectedFile(new File(job.getDocumentName()));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Ensure file has proper extension
                String originalName = job.getDocumentName().toLowerCase();
                if (originalName.endsWith(".pdf") && !file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getPath() + ".pdf");
                } else if (originalName.endsWith(".doc") && !file.getName().toLowerCase().endsWith(".doc")) {
                    file = new File(file.getPath() + ".doc");
                } else if (originalName.endsWith(".docx") && !file.getName().toLowerCase().endsWith(".docx")) {
                    file = new File(file.getPath() + ".docx");
                } else if (originalName.endsWith(".txt") && !file.getName().toLowerCase().endsWith(".txt")) {
                    file = new File(file.getPath() + ".txt");
                }
                
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(job.getDocumentContent());
                    JOptionPane.showMessageDialog(this,
                        "File downloaded successfully!",
                        "Download Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error saving file: " + ex.getMessage(),
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void updateQueueTable(List<PrintJob> jobs) {
        queueModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (PrintJob job : jobs) {
            queueModel.addRow(new Object[]{
                job.getJobId(),
                job.getFullName(),
                job.getDocumentName(),
                job.getPageCount(),
                job.getNumCopies(),
                String.format("$%.2f", job.getTotalCost()),
                job.getPaymentStatus(),
                job.getJobStatus(),
                job.getSubmittedAt().toLocalDateTime().format(formatter),
                "Download"  // This will be replaced by our button renderer
            });
        }
    }

    private void updateCompletedTable(List<PrintJob> jobs) {
        completedModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (PrintJob job : jobs) {
            completedModel.addRow(new Object[]{
                job.getJobId(),
                job.getFullName(),
                job.getDocumentName(),
                job.getPageCount(),
                job.getNumCopies(),
                String.format("$%.2f", job.getTotalCost()),
                job.getPaymentStatus(),
                job.getJobStatus(),
                job.getCompletedAt().toLocalDateTime().format(formatter)
            });
        }
    }

    private void processSelectedJob(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a job to process.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int jobId = (int) table.getValueAt(selectedRow, 0);
        PrintJob job = printJobDAO.getJobById(jobId);
        
        if (job == null) {
            JOptionPane.showMessageDialog(this,
                "Could not find the selected job.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (job.getJobStatus() == JobStatus.COMPLETED || job.getJobStatus() == JobStatus.CANCELLED) {
            JOptionPane.showMessageDialog(this,
                "This job has already been " + job.getJobStatus().toString().toLowerCase() + ".",
                "Invalid Action",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (job.getPaymentStatus() == PaymentStatus.UNPAID) {
            JOptionPane.showMessageDialog(this,
                "Payment must be processed before printing.",
                "Payment Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to mark this job as completed?",
            "Confirm Process",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            printJobDAO.updateJobStatus(job.getJobId(), JobStatus.COMPLETED, currentUser.getUserId());
            refreshData();
            
            JOptionPane.showMessageDialog(this,
                "Job has been marked as completed.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void cancelSelectedJob(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a job to cancel.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int jobId = (int) table.getValueAt(selectedRow, 0);
        PrintJob job = printJobDAO.getJobById(jobId);
        
        if (job == null) {
            JOptionPane.showMessageDialog(this,
                "Could not find the selected job.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (job.getJobStatus() == JobStatus.COMPLETED || job.getJobStatus() == JobStatus.CANCELLED) {
            JOptionPane.showMessageDialog(this,
                "This job has already been " + job.getJobStatus().toString().toLowerCase() + ".",
                "Invalid Action",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to cancel this job?\nThis action cannot be undone.",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // If job was paid, create a refund transaction
            if (job.getPaymentStatus() == PaymentStatus.PAID) {
                // Get current user balance for the transaction
                User student = userDAO.getUserById(job.getUserId());
                double currentBalance = student.getWalletBalance();
                double refundAmount = job.getTotalCost();
                Transaction refund = new Transaction(
                    job.getUserId(),
                    TransactionType.REFUND,
                    refundAmount,
                    currentBalance,
                    currentBalance + refundAmount,
                    "Refund for cancelled job #" + job.getJobId()
                );
                transactionDAO.createTransaction(refund);
            }
            
            printJobDAO.updateJobStatus(job.getJobId(), JobStatus.CANCELLED, currentUser.getUserId());
            refreshData();
            
            JOptionPane.showMessageDialog(this,
                "Job has been cancelled and any payment has been refunded.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class StatusColumnRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof JobStatus) {
                JobStatus status = (JobStatus) value;
                setHorizontalAlignment(CENTER);
                
                switch (status) {
                    case PENDING:
                        setBackground(new Color(241, 196, 15));
                        setForeground(Color.BLACK);
                        break;
                    case PROCESSING:
                        setBackground(new Color(52, 152, 219));
                        setForeground(Color.WHITE);
                        break;
                    case COMPLETED:
                        setBackground(new Color(46, 204, 113));
                        setForeground(Color.WHITE);
                        break;
                    case CANCELLED:
                        setBackground(new Color(231, 76, 60));
                        setForeground(Color.WHITE);
                        break;
                }
            }
            
            return c;
        }
    }

    private class PaymentStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof PaymentStatus) {
                PaymentStatus status = (PaymentStatus) value;
                setHorizontalAlignment(CENTER);
                
                switch (status) {
                    case UNPAID:
                        setBackground(new Color(231, 76, 60));
                        setForeground(Color.WHITE);
                        break;
                    case PAID:
                        setBackground(new Color(46, 204, 113));
                        setForeground(Color.WHITE);
                        break;
                    case REFUNDED:
                        setBackground(new Color(142, 68, 173));
                        setForeground(Color.WHITE);
                        break;
                }
            }
            
            return c;
        }
    }
    
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Download");
            setBackground(new Color(52, 152, 219));
            setForeground(Color.WHITE);
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private int clickedRow;

        public ButtonEditor(JTable table) {
            super(new JCheckBox());
            this.table = table;
            button = new JButton();
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBackground(new Color(52, 152, 219));
            button.setForeground(Color.WHITE);
            
            button.addActionListener(e -> {
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            clickedRow = row;
            label = (value == null) ? "Download" : value.toString();
            isPushed = true;
            button.setText(label);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Execute download when cell editing is done
                int jobId = (int) table.getValueAt(clickedRow, 0);
                downloadPrintJobFile(jobId);
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
}
