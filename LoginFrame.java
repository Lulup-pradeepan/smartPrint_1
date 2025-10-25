package ui;

import dao.UserDAO;
import models.User;
import models.User.UserType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Login frame for user authentication
 * Supports both student and operator login
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> userTypeCombo;
    private JButton loginButton;
    private JButton exitButton;
    private UserDAO userDAO;
    
    public LoginFrame() {
        this.userDAO = new UserDAO();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Smart Print Queue - Login");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                Color color1 = new Color(41, 128, 185);
                Color color2 = new Color(109, 213, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Login panel
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(Color.WHITE);
        loginPanel.setLayout(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        
        GridBagConstraints loginGbc = new GridBagConstraints();
        loginGbc.insets = new Insets(8, 8, 8, 8);
        loginGbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("Smart Print Queue");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(41, 128, 185));
        loginGbc.gridx = 0;
        loginGbc.gridy = 0;
        loginGbc.gridwidth = 2;
        loginGbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(titleLabel, loginGbc);
        
        JLabel subtitleLabel = new JLabel("Management System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        loginGbc.gridy = 1;
        loginPanel.add(subtitleLabel, loginGbc);
        
        // Username
        loginGbc.gridwidth = 1;
        loginGbc.gridy = 2;
        loginGbc.anchor = GridBagConstraints.WEST;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(usernameLabel, loginGbc);
        
        loginGbc.gridx = 1;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        loginPanel.add(usernameField, loginGbc);
        
        // Password
        loginGbc.gridx = 0;
        loginGbc.gridy = 3;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(passwordLabel, loginGbc);
        
        loginGbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        loginPanel.add(passwordField, loginGbc);
        
        // User Type
        loginGbc.gridx = 0;
        loginGbc.gridy = 4;
        JLabel userTypeLabel = new JLabel("Login As:");
        userTypeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(userTypeLabel, loginGbc);
        
        loginGbc.gridx = 1;
        String[] userTypes = {"Student", "Operator", "Admin"};
        userTypeCombo = new JComboBox<>(userTypes);
        userTypeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(userTypeCombo, loginGbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.BLACK);
        
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setBackground(new Color(46, 204, 113));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(new LoginActionListener());
        
        exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setBackground(new Color(231, 76, 60));
        exitButton.setForeground(Color.BLACK);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        
        loginGbc.gridx = 0;
        loginGbc.gridy = 5;
        loginGbc.gridwidth = 2;
        loginGbc.insets = new Insets(15, 8, 8, 8);
        loginPanel.add(buttonPanel, loginGbc);
        
        // Add login panel to main panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(loginPanel, gbc);
        
        // Add Enter key listener
        passwordField.addActionListener(new LoginActionListener());
        
        add(mainPanel);
    }
    
    private class LoginActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String selectedType = (String) userTypeCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                    "Please enter username and password",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Authenticate user
            User user = userDAO.authenticate(username, password);
            
            if (user == null) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                    "Invalid username or password",
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                return;
            }
            
            // Check user type matches selection
            UserType expectedType = UserType.valueOf(selectedType.toUpperCase());
            if (user.getUserType() != expectedType) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                    "Invalid user type. Please select the correct login type.",
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Open appropriate portal
            dispose();
            
            if (user.getUserType() == UserType.STUDENT) {
                SwingUtilities.invokeLater(() -> {
                    StudentPortal portal = new StudentPortal(user);
                    portal.setVisible(true);
                });
            } else if (user.getUserType() == UserType.OPERATOR || user.getUserType() == UserType.ADMIN) {
                SwingUtilities.invokeLater(() -> {
                    OperatorPortal portal = new OperatorPortal(user);
                    portal.setVisible(true);
                });
            }
        }
    }
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
