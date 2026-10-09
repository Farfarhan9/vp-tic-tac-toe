package main.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import main.Main;
import main.database.SessionManager;
import main.exceptions.DatabaseException;

public class WelcomePanel extends JPanel implements ActionListener {

    private Main mainFrame;
    private JButton btnPlay;
    private JButton btnSettings;
    private JButton btnMusicToggle;
    private boolean isMuted = false;
    private MusicPlayer musicPlayer;

    public WelcomePanel(Main mainFrame) {
        this.mainFrame = mainFrame;

        setBackground(new Color(245, 245, 245));
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Center: Title + Buttons ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 40, 15, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel lblTitle = new JLabel("Tic-Tac-Toe", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 32));
        lblTitle.setForeground(new Color(30, 30, 30));
        gbc.gridy = 0;
        gbc.weighty = 0.3;
        centerPanel.add(lblTitle, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.setLayout(new GridLayout(2, 1, 0, 15));

        Font buttonFont = new Font("SansSerif", Font.PLAIN, 18);
        Color btnBgColor = Color.WHITE;
        Color btnTextColor = new Color(40, 40, 40);
        LineBorder btnBorder = new LineBorder(new Color(210, 210, 210), 1, true);

        btnPlay = new JButton("▶  Play");
        btnPlay.setFont(buttonFont);
        btnPlay.setBackground(btnBgColor);
        btnPlay.setForeground(btnTextColor);
        btnPlay.setBorder(btnBorder);
        btnPlay.setFocusPainted(false);
        btnPlay.setPreferredSize(new Dimension(220, 50));
        btnPlay.addActionListener(this);
        buttonPanel.add(btnPlay);

        btnSettings = new JButton("⚙  Settings");
        btnSettings.setFont(buttonFont);
        btnSettings.setBackground(btnBgColor);
        btnSettings.setForeground(btnTextColor);
        btnSettings.setBorder(btnBorder);
        btnSettings.setFocusPainted(false);
        btnSettings.setPreferredSize(new Dimension(220, 50));
        btnSettings.addActionListener(e -> mainFrame.switchToPanel("SettingsPanel"));
        buttonPanel.add(btnSettings);

        gbc.gridy = 1;
        gbc.weighty = 0.7;
        gbc.anchor = GridBagConstraints.NORTH;
        centerPanel.add(buttonPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // --- Bottom-right: Music toggle ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(245, 245, 245));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));

        btnMusicToggle = new JButton("🔊");
        btnMusicToggle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        btnMusicToggle.setBackground(Color.WHITE);
        btnMusicToggle.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        btnMusicToggle.setFocusPainted(false);
        btnMusicToggle.setPreferredSize(new Dimension(50, 40));
        btnMusicToggle.addActionListener(e -> {
            musicPlayer.toggleMute();
            isMuted = musicPlayer.isMuted();
            btnMusicToggle.setText(isMuted ? "🔇" : "🔊");
        });
        bottomPanel.add(btnMusicToggle);

        add(bottomPanel, BorderLayout.SOUTH);

        musicPlayer = MusicPlayer.getInstance("src/resources/sounds/background.wav");
        isMuted = musicPlayer.isMuted();
        btnMusicToggle.setText(isMuted ? "🔇" : "🔊");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	
        if (e.getSource() == btnPlay) {
            try {
                // Check for an active session
            	System.out.println("[WelcomePanel] Checking for active session...");
                SessionManager.SessionSummary active = SessionManager.findActiveSession();
                System.out.println("[WelcomePanel] Active session found: " + (active != null));
                if (active != null) {
                    int choice = JOptionPane.showOptionDialog(this,
                            "You have an unfinished game.\nDo you want to continue it?",
                            "Continue Game",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{"Continue", "Start New"},
                            "Continue");

                    if (choice == JOptionPane.YES_OPTION) {
                        // Continue: tell Main to restore the session
                        mainFrame.continueGame(active);
                        return;
                    } else {
                        // Start new: abandon the old session first
                        SessionManager.abandonSession(active.sessionId);
                    }
                }
                // No active session, or user chose Start New
                mainFrame.startNewGame();
            } catch (DatabaseException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Database error: " + ex.getMessage() + "\nStarting a new game.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                // Fallback: start fresh
                mainFrame.startNewGame();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Unexpected error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}