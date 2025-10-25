package ui;

import dao.PrintJobDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import models.PrintJob;
import models.PrintJob.PaymentType;
import models.Transaction;
import models.User;
import services.PaymentService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Student Portal - Main interface for students
 * Features: Submit print jobs, view queue status, manage wallet
 */
public class StudentPortal extends JFrame {
    private User currentUser;
    private UserDAO userDAO;
    private PrintJobDAO printJobDAO;
    private TransactionDAO transactionDAO;
    private PaymentService paymentService;
    
    private JLabel walletBalanceLabel;
    private JTabbedPane tabbedPane;
    private Timer refreshTimer;
    
    public StudentPortal(User user) {
        this.currentUser = user;
        this.userDAO = new UserDAO();
        this.printJobDAO = new PrintJobDAO();
        this.transactionDAO = new TransactionDAO();
        this.paymentService = new PaymentService();
        
        initializeUI();
        startAutoRefresh();
    }
    
    private void initializeUI() {
        setTitle("Student Portal - " + currentUser.getFullName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(236, 240, 241));
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        tabbedPane.addTab("Submit Print Job", createSubmitJobPanel());
        tabbedPane.addTab("Queue Status", createQueueStatusPanel());
        tabbedPane.addTab("My Jobs", createMyJobsPanel());
        tabbedPane.addTab("Wallet & Transactions", createWalletPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Left side - Welcome message
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.BLACK);
        leftPanel.add(welcomeLabel);
        
        // Right side - Wallet balance and logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);
        
        JLabel walletLabel = new JLabel("Wallet Balance:");
        walletLabel.setFont(new Font("Arial", Font.BOLD, 16));
        walletLabel.setForeground(Color.BLACK);
        rightPanel.add(walletLabel);
        
        walletBalanceLabel = new JLabel("₹" + String.format("%.2f", currentUser.getWalletBalance()));
        walletBalanceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        walletBalanceLabel.setForeground(new Color(46, 204, 113));
        rightPanel.add(walletBalanceLabel);
        
        JButton refreshButton = new JButton("⟳");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 18));
        refreshButton.setBackground(new Color(52, 152, 219));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        rightPanel.add(refreshButton);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        logoutButton.setBackground(new Color(231, 76, 60));
        logoutButton.setForeground(Color.BLACK);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logout());
        rightPanel.add(logoutButton);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private File selectedFile;
    
    private boolean validateFile(File file) {
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(this,
                "File does not exist.",
                "File Access Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!file.canRead()) {
            JOptionPane.showMessageDialog(this,
                "Cannot read the file. Please check file permissions.",
                "File Access Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        long fileSize = file.length();
        if (fileSize == 0) {
            JOptionPane.showMessageDialog(this,
                "File is empty.",
                "Invalid File",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (fileSize > 20 * 1024 * 1024) { // 20MB limit
            JOptionPane.showMessageDialog(this,
                String.format("File size (%.2f MB) exceeds 20MB limit.", fileSize / (1024.0 * 1024.0)),
                "File Too Large",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        boolean isValidExtension = false;
        String validType = "";
        
        switch (extension) {
            case "pdf":
                isValidExtension = true;
                validType = "PDF";
                break;
            case "doc":
            case "docx":
                isValidExtension = true;
                validType = "Word Document";
                break;
            case "txt":
                isValidExtension = true;
                validType = "Text Document";
                break;
        }
        
        if (!isValidExtension) {
            JOptionPane.showMessageDialog(this,
                "Invalid file type: ." + extension + "\nPlease select PDF, DOC, DOCX, or TXT files only.",
                "Invalid File Type",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        
        return true;
    }
    
    private JPanel createSubmitJobPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(41, 128, 185)); // Nice blue color
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("Submit New Print Job");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Document Selection
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel docLabel = new JLabel("Select Document:");
        docLabel.setFont(new Font("Arial", Font.BOLD, 14));
        docLabel.setForeground(Color.BLACK);
        panel.add(docLabel, gbc);
        
        gbc.gridx = 1;
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.setBackground(Color.WHITE);
        
        JTextField docNameField = new JTextField(20);
        docNameField.setFont(new Font("Arial", Font.PLAIN, 14));
        docNameField.setEditable(false);
        docNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        filePanel.add(docNameField, BorderLayout.CENTER);
        
        JButton browseButton = new JButton("Choose File");
        browseButton.setFont(new Font("Arial", Font.BOLD, 12));
        browseButton.setBackground(new Color(52, 152, 219));
        browseButton.setForeground(Color.BLACK);
        browseButton.setFocusPainted(false);
        browseButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Document to Print");
        browseButton.addActionListener(e -> {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Document Files (*.pdf, *.doc, *.docx, *.txt)", 
                "pdf", "doc", "docx", "txt"
            ));
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    if (validateFile(file)) {
                        selectedFile = file;
                        docNameField.setText(selectedFile.getName());
                        docNameField.setToolTipText(selectedFile.getAbsolutePath());
                    } else {
                        selectedFile = null;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                        "Error accessing file: " + ex.getMessage() + "\nPlease try again.",
                        "File Access Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    selectedFile = null;
                    docNameField.setText("");
                    docNameField.setToolTipText("");
                }
            }
        });
        
        filePanel.add(browseButton, BorderLayout.EAST);
        panel.add(filePanel, gbc);
        
        // Page Count
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel pageLabel = new JLabel("Number of Pages:");
        pageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        pageLabel.setForeground(Color.BLACK);
        panel.add(pageLabel, gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel pageModel = new SpinnerNumberModel(1, 1, 1000, 1);
        JSpinner pageSpinner = new JSpinner(pageModel);
        pageSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(pageSpinner, gbc);
        
        // Number of Copies
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel copiesLabel = new JLabel("Number of Copies:");
        copiesLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        copiesLabel.setForeground(Color.BLACK);
        panel.add(copiesLabel, gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel copiesModel = new SpinnerNumberModel(1, 1, 100, 1);
        JSpinner copiesSpinner = new JSpinner(copiesModel);
        copiesSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(copiesSpinner, gbc);
        
        // Cost Display
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel costLabel = new JLabel("Total Cost:");
        costLabel.setFont(new Font("Arial", Font.BOLD, 14));
        costLabel.setForeground(Color.BLACK);
        panel.add(costLabel, gbc);
        
        gbc.gridx = 1;
        JLabel costValueLabel = new JLabel("₹0.00");
        costValueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        costValueLabel.setForeground(Color.BLACK);
        panel.add(costValueLabel, gbc);
        
        // Update cost on spinner change
        Runnable updateCost = () -> {
            int pages = (Integer) pageSpinner.getValue();
            int copies = (Integer) copiesSpinner.getValue();
            double cost = PaymentService.calculatePrintCost(pages, copies);
            costValueLabel.setText("₹" + String.format("%.2f", cost));
        };
        pageSpinner.addChangeListener(e -> updateCost.run());
        copiesSpinner.addChangeListener(e -> updateCost.run());
        
        // Payment Type
        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel paymentLabel = new JLabel("Payment Type:");
        paymentLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        paymentLabel.setForeground(Color.BLACK);
        panel.add(paymentLabel, gbc);
        
        gbc.gridx = 1;
        JPanel paymentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paymentPanel.setBackground(Color.WHITE);
        JRadioButton prepaidRadio = new JRadioButton("Pre-paid (Pay Now)");
        prepaidRadio.setFont(new Font("Arial", Font.PLAIN, 14));
        prepaidRadio.setBackground(Color.WHITE);
        prepaidRadio.setSelected(true);
        JRadioButton postpaidRadio = new JRadioButton("Post-paid (Pay After)");
        postpaidRadio.setFont(new Font("Arial", Font.PLAIN, 14));
        postpaidRadio.setBackground(Color.WHITE);
        ButtonGroup paymentGroup = new ButtonGroup();
        paymentGroup.add(prepaidRadio);
        paymentGroup.add(postpaidRadio);
        paymentPanel.add(prepaidRadio);
        paymentPanel.add(postpaidRadio);
        panel.add(paymentPanel, gbc);
        
        // Submit Button
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton submitButton = new JButton("Submit Print Job");
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        submitButton.setBackground(new Color(46, 204, 113));
        submitButton.setForeground(Color.BLACK);
        submitButton.setFocusPainted(false);
        submitButton.setBorder(BorderFactory.createEmptyBorder(12, 40, 12, 40));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> {
            String docName = docNameField.getText().trim();
            int pages = (Integer) pageSpinner.getValue();
            int copies = (Integer) copiesSpinner.getValue();
            PaymentType paymentType = prepaidRadio.isSelected() ? PaymentType.PREPAID : PaymentType.POSTPAID;
            
            submitPrintJob(docName, pages, copies, paymentType);
            
            // Reset form
            docNameField.setText("");
            pageSpinner.setValue(1);
            copiesSpinner.setValue(1);
            prepaidRadio.setSelected(true);
        });
        panel.add(submitButton, gbc);
        
        return panel;
    }
    
    private JPanel createQueueStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel("My Print Jobs in Queue");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(41, 128, 185));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Job ID", "Document", "Pages", "Copies", "Cost", "Status", "Queue Position", "Submitted At"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setForeground(Color.BLACK);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 240));
        table.getTableHeader().setForeground(Color.BLACK);
        
        // Center align cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load data
        loadQueueData(model);
        
        return panel;
    }
    
    private JPanel createMyJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel("My Print Jobs");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(142, 68, 173));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // File Selection
        JLabel fileLabel = new JLabel("Select Document:");
        fileLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(fileLabel, gbc);

        JTextField filePathField = new JTextField(30);
        filePathField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 0;
        formPanel.add(filePathField, gbc);

        JButton browseButton = new JButton("Browse");
        browseButton.setFont(new Font("Arial", Font.BOLD, 12));
        browseButton.setBackground(new Color(52, 152, 219));
        browseButton.setForeground(Color.WHITE);
        browseButton.setFocusPainted(false);
        gbc.gridx = 2;
        gbc.gridy = 0;
        formPanel.add(browseButton, gbc);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        browseButton.addActionListener(e -> {
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        // Table
        String[] columns = {"Job ID", "Document", "Pages", "Copies", "Cost", "Status", "Payment", "Submitted At"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setForeground(Color.BLACK);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(52, 152, 219));
        table.getTableHeader().setForeground(Color.BLACK);
        
        // Center align cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load data
        loadMyJobsData(model);
        
        return panel;
    }
    
    private JPanel createWalletPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Top panel - Wallet info and recharge
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("Wallet Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        topPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel balanceLabel = new JLabel("Current Balance:");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(balanceLabel, gbc);
        
        gbc.gridx = 1;
        JLabel balanceValue = new JLabel("₹" + String.format("%.2f", currentUser.getWalletBalance()));
        balanceValue.setFont(new Font("Arial", Font.BOLD, 18));
        balanceValue.setForeground(new Color(46, 204, 113));
        topPanel.add(balanceValue, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel rechargeLabel = new JLabel("Recharge Amount:");
        rechargeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(rechargeLabel, gbc);
        
        gbc.gridx = 1;
        JTextField rechargeField = new JTextField(15);
        rechargeField.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(rechargeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton rechargeButton = new JButton("Recharge Wallet");
        rechargeButton.setFont(new Font("Arial", Font.BOLD, 14));
        rechargeButton.setBackground(new Color(46, 204, 113));
        rechargeButton.setForeground(Color.BLACK);
        rechargeButton.setFocusPainted(false);
        rechargeButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        rechargeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rechargeButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(rechargeField.getText().trim());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid amount", "Invalid Amount", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                if (paymentService.rechargeWallet(currentUser.getUserId(), amount)) {
                    JOptionPane.showMessageDialog(this, "Wallet recharged successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    rechargeField.setText("");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to recharge wallet", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        });
        topPanel.add(rechargeButton, gbc);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Transaction history table
        JLabel transLabel = new JLabel("Transaction History");
        transLabel.setFont(new Font("Arial", Font.BOLD, 18));
        transLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        String[] columns = {"Transaction ID", "Type", "Amount", "Balance After", "Date", "Description"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(52, 152, 219));
        table.getTableHeader().setForeground(Color.BLACK);
        table.setForeground(Color.BLACK);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(transLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        // Load transaction data
        loadTransactionData(model);
        
        return panel;
    }
    
    private void submitPrintJob(String docName, int pages, int copies, PaymentType paymentType) {
        if (docName.isEmpty() || selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a document to print", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate file again before submission
        if (!selectedFile.exists() || !selectedFile.canRead()) {
            JOptionPane.showMessageDialog(this, 
                "Cannot access the selected file. Please choose the file again.", 
                "File Access Error", 
                JOptionPane.ERROR_MESSAGE);
            selectedFile = null;
            return;
        }
        
        // Verify file size again before reading
        if (selectedFile.length() > 20 * 1024 * 1024) { // 20MB limit
            JOptionPane.showMessageDialog(this,
                "File size exceeds 20MB limit.",
                "File Too Large",
                JOptionPane.ERROR_MESSAGE
            );
            selectedFile = null;
            return;
        }
        
        double cost = PaymentService.calculatePrintCost(pages, copies);
        
        // Check balance for prepaid
        if (paymentType == PaymentType.PREPAID) {
            if (!paymentService.hasSufficientBalance(currentUser.getUserId(), cost)) {
                JOptionPane.showMessageDialog(this,
                    "Insufficient wallet balance. Please recharge your wallet.\nRequired: ₹" + String.format("%.2f", cost),
                    "Insufficient Balance",
                    JOptionPane.WARNING_MESSAGE);
                tabbedPane.setSelectedIndex(3); // Switch to wallet tab
                return;
            }
        }
        
                // Create print job with file content
        PrintJob job = new PrintJob(currentUser.getUserId(), docName, pages, copies, cost, paymentType);
        int jobId;
        try {
            // Read file content with buffer and proper size handling
            byte[] fileContent;
            long fileSize = selectedFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new Exception("File is too large to process");
            }
            
            try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(
                    new java.io.FileInputStream(selectedFile))) {
                fileContent = new byte[(int) fileSize];
                int bytesRead = bis.read(fileContent);
                
                if (bytesRead != fileSize) {
                    throw new Exception("Could not read entire file");
                }
            }
            
            if (fileContent == null || fileContent.length == 0) {
                throw new Exception("Failed to read file content");
            }
            
            // Set file content and path in the job
            job.setDocumentContent(fileContent);
            job.setDocumentPath(selectedFile.getName()); // Store just the filename instead of full path
            
            // Create the job in database
            jobId = printJobDAO.createPrintJob(job);
        } catch (Exception ex) {
            String errorMessage = "Error processing file: ";
            if (ex instanceof java.io.IOException) {
                errorMessage += "Could not read the file. Please ensure the file is not in use.";
            } else {
                errorMessage += ex.getMessage();
            }
            JOptionPane.showMessageDialog(this,
                errorMessage + "\nPlease try again.",
                "File Upload Error",
                JOptionPane.ERROR_MESSAGE);
            selectedFile = null;
            return;
        }
        
        if (jobId > 0) {
            // Process payment if prepaid
            if (paymentType == PaymentType.PREPAID) {
                if (paymentService.processPayment(currentUser.getUserId(), jobId, cost)) {
                    JOptionPane.showMessageDialog(this,
                        "Print job submitted successfully!\nJob ID: " + jobId + "\nPayment processed.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Job created but payment failed. Please contact support.",
                        "Payment Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Print job submitted successfully!\nJob ID: " + jobId + "\nPayment will be collected after printing.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            refreshData();
            selectedFile = null; // Clear the selected file
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to submit print job. Please try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            selectedFile = null; // Clear the selected file on error
        }
    }
    
    private void loadQueueData(DefaultTableModel model) {
        model.setRowCount(0);
        List<PrintJob> jobs = printJobDAO.getPendingJobsByUserId(currentUser.getUserId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        
        for (PrintJob job : jobs) {
            model.addRow(new Object[]{
                job.getJobId(),
                job.getDocumentName(),
                job.getPageCount(),
                job.getNumCopies(),
                "₹" + String.format("%.2f", job.getTotalCost()),
                job.getJobStatus(),
                job.getQueuePosition(),
                sdf.format(job.getSubmittedAt())
            });
        }
    }
    
    private void loadMyJobsData(DefaultTableModel model) {
        model.setRowCount(0);
        List<PrintJob> jobs = printJobDAO.getJobsByUserId(currentUser.getUserId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        
        for (PrintJob job : jobs) {
            model.addRow(new Object[]{
                job.getJobId(),
                job.getDocumentName(),
                job.getPageCount(),
                job.getNumCopies(),
                "₹" + String.format("%.2f", job.getTotalCost()),
                job.getJobStatus(),
                job.getPaymentStatus(),
                sdf.format(job.getSubmittedAt())
            });
        }
    }
    
    private void loadTransactionData(DefaultTableModel model) {
        model.setRowCount(0);
        List<Transaction> transactions = transactionDAO.getTransactionsByUserId(currentUser.getUserId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        
        for (Transaction trans : transactions) {
            model.addRow(new Object[]{
                trans.getTransactionId(),
                trans.getTransactionType(),
                "₹" + String.format("%.2f", trans.getAmount()),
                "₹" + String.format("%.2f", trans.getBalanceAfter()),
                sdf.format(trans.getTransactionDate()),
                trans.getDescription()
            });
        }
    }
    
    private void refreshData() {
        // Refresh user data
        currentUser = userDAO.getUserById(currentUser.getUserId());
        walletBalanceLabel.setText("₹" + String.format("%.2f", currentUser.getWalletBalance()));
        
        // Refresh all tabs
        int selectedIndex = tabbedPane.getSelectedIndex();
        
        tabbedPane.removeAll();
        tabbedPane.addTab("Submit Print Job", createSubmitJobPanel());
        tabbedPane.addTab("Queue Status", createQueueStatusPanel());
        tabbedPane.addTab("My Jobs", createMyJobsPanel());
        tabbedPane.addTab("Wallet & Transactions", createWalletPanel());
        
        tabbedPane.setSelectedIndex(selectedIndex);
    }
    
    private void startAutoRefresh() {
        // Auto-refresh every 10 seconds
        refreshTimer = new Timer(10000, e -> {
            if (tabbedPane.getSelectedIndex() == 1) { // Queue Status tab
                refreshData();
            }
        });
        refreshTimer.start();
    }
    
    private void logout() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
