package main.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import main.Main;
import main.game.GameModel;
import main.database.SettingsDAO;
import main.database.HistoryDAO;
import main.database.SessionManager;
import main.exceptions.DatabaseException;
import main.exceptions.GameException;

public class SettingsPanel extends JPanel {

    // Color Palette (exactly as original)
    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(230, 230, 230);
    private static final Color TEXT_MAIN = new Color(30, 30, 30);
    private static final Color TEXT_MUTED = new Color(120, 120, 120);
    private static final Color TOGGLE_ON = new Color(24, 107, 121);
    private static final Color TOGGLE_OFF = new Color(200, 200, 200);

    private Main mainFrame;
    private GameModel model;

    // UI components
    private JPanel mainPanel;
    private JComboBox<String> comboGamemode;
    private JComboBox<String> comboBoard;
    private JComboBox<String> comboDifficulty;
    private JComboBox<String> comboWinningLogic;
    
    private CustomToggleSwitch toggleTimer;
    private CustomToggleSwitch toggleBoardInfo;
    private CustomToggleSwitch toggleCounter;
    
    private JLabel lblTimerState;
    private JLabel lblBoardInfoState;
    private JLabel lblCounterState;

    // Misc components
    private JComboBox<String> comboFirstPlayer;
    private JComboBox<String> comboTheme;
    private JComboBox<String> comboMusicTrack;
    
    private JPanel wrappedWinningLogicCard; // To dynamic show/hide
    private JPanel wrappedDifficultyCard;   // ★ NEW: wrapper for difficulty card

    public SettingsPanel(Main mainFrame, GameModel model) {
        this.mainFrame = mainFrame;
        this.model = model;

        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        // --- TOP: Left-aligned modern header view ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBackground(BG_COLOR);
        topPanel.setBorder(new EmptyBorder(25, 40, 10, 40));
        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_MAIN);
        topPanel.add(titleLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- MAIN: Scrollable content container ---
        mainPanel = new JPanel();
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 40, 25, 40));

        // SECTION 1: General Settings
        addSectionLabel(mainPanel, "General");
        
        JPanel gamemodeCard = createDropdownCard("gamemode", "Gamemode", new String[]{"Singleplayer", "Multiplayer"});
        mainPanel.add(gamemodeCard);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel boardCard = createDropdownCard("board", "Board", new String[]{"3x3", "4x4", "5x5", "6x6"});
        mainPanel.add(boardCard);
        mainPanel.add(Box.createVerticalStrut(8));

        // Nested sub-items directly under Board
        JPanel difficultyCard = createDropdownCard("difficulty", "Difficulty", getDifficultyOptions());
        wrappedDifficultyCard = createNestedWrapper(difficultyCard, 8); // ★ STORE WRAPPER
        mainPanel.add(wrappedDifficultyCard);

        JPanel winningLogicCard = createDropdownCard("winning", "Winning Logic", new String[]{"Default", "3x3 Rules"});
        wrappedWinningLogicCard = createNestedWrapper(winningLogicCard, 0);
        mainPanel.add(wrappedWinningLogicCard);
        mainPanel.add(Box.createVerticalStrut(25));

        // SECTION 2: Match Info
        addSectionLabel(mainPanel, "Match Info");
        
        JPanel timerCard = createToggleCard("timer", "Match Timer", "Keep track of how long the match takes");
        mainPanel.add(timerCard);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel boardInfoCard = createToggleCard("boardInfo", "Board Info", "Displays number of spots taken");
        mainPanel.add(boardInfoCard);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel counterCard = createToggleCard("counter", "Player Counter", "Human or bot wins count");
        mainPanel.add(counterCard);
        mainPanel.add(Box.createVerticalStrut(25));

        // SECTION 3: Misc
        addSectionLabel(mainPanel, "Misc");

        mainPanel.add(createButtonCard("history", "Game History", "Open", e -> showHistoryDialog()));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createDropdownCard("firstPlayer", "First Player", new String[]{"X", "O"}));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createMusicCard());
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createButtonCard("customise", "Customise X / O", "Edit", e -> customiseXO()));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createDropdownCard("theme", "Theme", new String[]{"Light", "Dark", "Blue", "Green"}));
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createButtonCard("clear", "Clear History", "Clear", e -> clearHistory()));
        mainPanel.add(Box.createVerticalStrut(25));

        // FOOTER: Operation Action Buttons (Strict Left Alignment)
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonContainer.setBackground(BG_COLOR);
        buttonContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonContainer.setMaximumSize(new Dimension(520, 45));

        buttonContainer.add(createResetButton());
        buttonContainer.add(Box.createHorizontalStrut(15));
        buttonContainer.add(createSaveButton());
        mainPanel.add(buttonContainer);

        // Frame overflow management
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // Dropdown actions tracking
        comboBoard.addActionListener(e -> {
            updateDifficultyOptions();
            updateWinningLogicVisibility();
        });

        // ★ NEW: Gamemode listener to show/hide difficulty
        comboGamemode.addActionListener(e -> updateDifficultyVisibility());

        // Initialize state fields
        loadSettingsFromDatabase();
        updateWinningLogicVisibility();
    }

    private void addSectionLabel(JPanel parent, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(12));
    }

    private JPanel createNestedWrapper(JPanel card, int bottomSpace) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 35, bottomSpace, 0)); // 35px Left indent nesting
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(520, 68 + bottomSpace));
        return wrapper;
    }

    // ---------- UI Custom Card Creation Helpers ----------

    private JPanel createBaseCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 12, 12));
                g2.setColor(CARD_BORDER);
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(520, 68));
        card.setPreferredSize(new Dimension(520, 68));
        card.setMinimumSize(new Dimension(520, 68));
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(0, 15, 0, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel createDropdownCard(String iconType, String titleText, String[] comboOptions) {
        JPanel card = createBaseCard();
        GridBagConstraints gbc = new GridBagConstraints();

        CustomIcon iconLabel = new CustomIcon(iconType);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 15);
        card.add(iconLabel, gbc);

        JLabel nameLabel = new JLabel(titleText);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nameLabel.setForeground(TEXT_MAIN);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(nameLabel, gbc);

        JComboBox<String> comboBox = new JComboBox<>(comboOptions);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setFocusable(false);
        comboBox.setPreferredSize(new Dimension(135, 32));
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 8);
        card.add(comboBox, gbc);

        CustomIcon expandArrow = new CustomIcon("chevron");
        gbc.gridx = 3;
        gbc.insets = new Insets(0, 5, 0, 5);
        card.add(expandArrow, gbc);

        if (iconType.equals("gamemode")) comboGamemode = comboBox;
        else if (iconType.equals("board")) comboBoard = comboBox;
        else if (iconType.equals("difficulty")) comboDifficulty = comboBox;
        else if (iconType.equals("winning")) comboWinningLogic = comboBox;
        else if (iconType.equals("firstPlayer")) comboFirstPlayer = comboBox;
        else if (iconType.equals("theme")) comboTheme = comboBox;

        return card;
    }

    private JPanel createToggleCard(String iconType, String titleText, String subTitleText) {
        JPanel card = createBaseCard();
        GridBagConstraints gbc = new GridBagConstraints();

        CustomIcon iconLabel = new CustomIcon(iconType);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 15);
        card.add(iconLabel, gbc);

        JLabel nameLabel = new JLabel(titleText);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nameLabel.setForeground(TEXT_MAIN);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        card.add(nameLabel, gbc);

        JLabel descriptionLabel = new JLabel(subTitleText);
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descriptionLabel.setForeground(TEXT_MUTED);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        card.add(descriptionLabel, gbc);

        JPanel toggleContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        toggleContainer.setOpaque(false);

        CustomToggleSwitch toggle = new CustomToggleSwitch();
        JLabel stateLabel = new JLabel("On");
        stateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stateLabel.setForeground(TEXT_MAIN);

        toggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                stateLabel.setText(toggle.isActivated() ? "On" : "Off");
            }
        });

        toggleContainer.add(toggle);
        toggleContainer.add(stateLabel);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 5);
        card.add(toggleContainer, gbc);

        if (iconType.equals("timer")) {
            toggleTimer = toggle;
            lblTimerState = stateLabel;
        } else if (iconType.equals("boardInfo")) {
            toggleBoardInfo = toggle;
            lblBoardInfoState = stateLabel;
        } else if (iconType.equals("counter")) {
            toggleCounter = toggle;
            lblCounterState = stateLabel;
        }

        return card;
    }

    private JPanel createButtonCard(String iconType, String titleText, String buttonText, java.awt.event.ActionListener action) {
        JPanel card = createBaseCard();
        GridBagConstraints gbc = new GridBagConstraints();

        CustomIcon iconLabel = new CustomIcon(iconType);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 15);
        card.add(iconLabel, gbc);

        JLabel nameLabel = new JLabel(titleText);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nameLabel.setForeground(TEXT_MAIN);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(nameLabel, gbc);

        JButton actionButton = new JButton(buttonText);
        actionButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        actionButton.setBackground(Color.WHITE);
        actionButton.setForeground(TEXT_MAIN);
        actionButton.setFocusPainted(false);
        actionButton.setPreferredSize(new Dimension(85, 32));
        actionButton.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
        actionButton.addActionListener(action);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 5);
        card.add(actionButton, gbc);

        return card;
    }

    private JPanel createMusicCard() {
        JPanel card = createBaseCard();
        GridBagConstraints gbc = new GridBagConstraints();

        CustomIcon iconLabel = new CustomIcon("music");
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 15);
        card.add(iconLabel, gbc);

        JLabel nameLabel = new JLabel("Music");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nameLabel.setForeground(TEXT_MAIN);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(nameLabel, gbc);

        comboMusicTrack = new JComboBox<>(new String[]{"Default (background.wav)", "Retro Synth.wav", "Chill Lo-Fi.wav"});
        comboMusicTrack.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboMusicTrack.setBackground(Color.WHITE);
        comboMusicTrack.setFocusable(false);
        comboMusicTrack.setPreferredSize(new Dimension(135, 32));
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 8);
        card.add(comboMusicTrack, gbc);

        JButton btnAddMusic = new JButton("Add");
        btnAddMusic.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAddMusic.setBackground(Color.WHITE);
        btnAddMusic.setForeground(TEXT_MAIN);
        btnAddMusic.setFocusPainted(false);
        btnAddMusic.setPreferredSize(new Dimension(55, 32));
        btnAddMusic.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
        btnAddMusic.addActionListener(e -> addMusicFile());
        
        gbc.gridx = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 5);
        card.add(btnAddMusic, gbc);

        return card;
    }

    // ---------- Operations & Event Implementations ----------

    private String[] getDifficultyOptions() {
        return new String[]{"Easy", "Medium", "Hard", "Impossible"};
    }

    private void updateDifficultyOptions() {
        if (comboBoard == null || comboDifficulty == null) return;
        String board = (String) comboBoard.getSelectedItem();
        if (board == null) return;
        int size = Integer.parseInt(board.substring(0, 1));
        String[] difficulties;
        if (size == 3) {
            difficulties = new String[]{"Easy", "Medium", "Hard", "Impossible"};
        } else {
            difficulties = new String[]{"Easy", "Medium", "Hard"};
        }
        
        String current = (String) comboDifficulty.getSelectedItem();
        comboDifficulty.setModel(new DefaultComboBoxModel<>(difficulties));
        if (current != null) {
            for (String d : difficulties) {
                if (d.equals(current)) {
                    comboDifficulty.setSelectedItem(d);
                    break;
                }
            }
        }
    }

    private void updateWinningLogicVisibility() {
        if (comboBoard == null || wrappedWinningLogicCard == null) return;
        String board = (String) comboBoard.getSelectedItem();
        if (board != null) {
            int size = Integer.parseInt(board.substring(0, 1));
            // Visible ONLY when 4x4, 5x5, or 6x6 is chosen
            boolean show = (size > 3);
            wrappedWinningLogicCard.setVisible(show);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // ★ NEW: Show/hide difficulty based on gamemode
    private void updateDifficultyVisibility() {
        if (comboGamemode == null || wrappedDifficultyCard == null) return;
        String mode = (String) comboGamemode.getSelectedItem();
        boolean show = "Singleplayer".equals(mode);
        wrappedDifficultyCard.setVisible(show);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void showHistoryDialog() {
        JOptionPane.showMessageDialog(this, "Game history log viewer loaded.", "Game History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void customiseXO() {
        JOptionPane.showMessageDialog(this, "Customise token assets editor window loaded.", "Customise Tokens", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addMusicFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio files", "wav", "mp3"));
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String name = file.getName();
            comboMusicTrack.addItem(name);
            comboMusicTrack.setSelectedItem(name);
        }
    }

    private void clearHistory() {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete ALL game history? This cannot be undone.", "Clear History", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                HistoryDAO.clearAllHistory();
                JOptionPane.showMessageDialog(this, "History cleared successfully.");
            } catch (DatabaseException ex) {
                JOptionPane.showMessageDialog(this, "Error clearing history: " + ex.getMessage());
            }
        }
    }

    private JButton createResetButton() {
        JButton resetBtn = new JButton("Reset to default") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_MAIN);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = 22, cy = 18;
                g2.drawArc(cx - 7, cy - 7, 14, 14, 35, 135);
                g2.drawArc(cx - 7, cy - 7, 14, 14, 215, 140);
                g2.drawPolyline(new int[]{cx + 1, cx + 6, cx + 5}, new int[]{cy - 4, cy - 4, cy - 9}, 3);
                g2.drawPolyline(new int[]{cx - 1, cx - 6, cx - 5}, new int[]{cy + 4, cy + 4, cy + 9}, 3);
                g2.dispose();
            }
        };
        resetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resetBtn.setBackground(Color.WHITE);
        resetBtn.setForeground(TEXT_MAIN);
        resetBtn.setFocusPainted(false);
        resetBtn.setPreferredSize(new Dimension(180, 38));
        resetBtn.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
        resetBtn.setMargin(new Insets(0, 26, 0, 0));
        resetBtn.addActionListener(e -> performDefaultReset());
        return resetBtn;
    }

    private JButton createSaveButton() {
        JButton saveBtn = new JButton("Save & Back");
        saveBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        saveBtn.setBackground(Color.WHITE);
        saveBtn.setForeground(new Color(24, 107, 121));
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(180, 38));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(24, 107, 121), 1, true));
        saveBtn.addActionListener(e -> performPersistentSave());
        return saveBtn;
    }

    // ---------- Backend Persistent Logic Sync ----------

    private void loadSettingsFromDatabase() {
        try {
            SettingsDAO.loadSettings(model);
            comboGamemode.setSelectedItem(model.getGameMode());
            
            int size = model.getBoardSize();
            comboBoard.setSelectedItem(size + "x" + size);
            
            toggleTimer.setActivated(model.isMatchTimerEnabled());
            lblTimerState.setText(model.isMatchTimerEnabled() ? "On" : "Off");
            toggleBoardInfo.setActivated(model.isBoardInfoEnabled());
            lblBoardInfoState.setText(model.isBoardInfoEnabled() ? "On" : "Off");
            toggleCounter.setActivated(model.isPlayerCounterEnabled());
            lblCounterState.setText(model.isPlayerCounterEnabled() ? "On" : "Off");
            
            updateDifficultyOptions();
            if (comboDifficulty != null) comboDifficulty.setSelectedItem(model.getDifficulty());
            if (comboWinningLogic != null) comboWinningLogic.setSelectedItem(model.getWinningLogic());
            if (comboFirstPlayer != null) comboFirstPlayer.setSelectedItem(model.getFirstPlayer());
            if (comboTheme != null) comboTheme.setSelectedItem(model.getTheme());
            if (comboMusicTrack != null) comboMusicTrack.setSelectedItem(model.getMusicTrack());

            // ★ Update difficulty visibility after loading gamemode
            updateDifficultyVisibility();

        } catch (GameException e) {
            System.err.println(e.toString());
            JOptionPane.showMessageDialog(this,
                    "Database server connection is offline. Defaulting to safe offline memory structures.",
                    "Persistence Layer Notice", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void performPersistentSave() {
        // Check if there is an active session and warn the user
        try {
            SessionManager.SessionSummary active = SessionManager.findActiveSession();
            if (active != null) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "You have an ongoing game.\nChanging settings will start a new game and your current progress will be lost.\nContinue?",
                    "Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return; // cancel save
                }
            }
        } catch (DatabaseException e) {
            // If DB is offline, proceed without warning
            System.err.println("Could not check active session: " + e.getMessage());
        }

        String selectedMode = (String) comboGamemode.getSelectedItem();
        String sizeStr = (String) comboBoard.getSelectedItem();
        int selectedSize = Integer.parseInt(sizeStr.substring(0, 1));

        model.setGameMode(selectedMode);
        model.setBoardSize(selectedSize);
        model.setMatchTimerEnabled(toggleTimer.isActivated());
        model.setBoardInfoEnabled(toggleBoardInfo.isActivated());
        model.setPlayerCounterEnabled(toggleCounter.isActivated());
        
        if (comboDifficulty != null) model.setDifficulty((String) comboDifficulty.getSelectedItem());
        if (comboWinningLogic != null) model.setWinningLogic((String) comboWinningLogic.getSelectedItem());
        if (comboFirstPlayer != null) model.setFirstPlayer((String) comboFirstPlayer.getSelectedItem());
        if (comboTheme != null) model.setTheme((String) comboTheme.getSelectedItem());
        if (comboMusicTrack != null) model.setMusicTrack((String) comboMusicTrack.getSelectedItem());

        try {
            SettingsDAO.saveSettings(model);
            JOptionPane.showMessageDialog(this, "Settings persistently synchronized inside DB schema configuration.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (GameException e) {
            System.err.println(e.toString());
            JOptionPane.showMessageDialog(this, "SQL Server unavailable. Selection applied to runtime cache memory state successfully.", "Offline Memory Cache Mode", JOptionPane.WARNING_MESSAGE);
        } finally {
            mainFrame.getGamePanel().synchronizeControllerSettings();
            mainFrame.switchToPanel("WelcomePanel");
        }
    }

    private void performDefaultReset() {
        System.out.println(">>> performDefaultReset() called");
        new Exception("Stack trace for performDefaultReset").printStackTrace(System.out);
        comboGamemode.setSelectedIndex(0);          
        comboBoard.setSelectedIndex(0);             
        if (comboDifficulty != null) comboDifficulty.setSelectedIndex(1); // Medium
        if (comboWinningLogic != null) comboWinningLogic.setSelectedIndex(0); 
        if (comboFirstPlayer != null) comboFirstPlayer.setSelectedIndex(0);
        if (comboTheme != null) comboTheme.setSelectedIndex(0);
        if (comboMusicTrack != null) comboMusicTrack.setSelectedIndex(0);

        toggleTimer.setActivated(true);
        lblTimerState.setText("On");
        toggleBoardInfo.setActivated(true);
        lblBoardInfoState.setText("On");
        toggleCounter.setActivated(true);
        lblCounterState.setText("On");

        model.setGameMode("Singleplayer");
        model.setBoardSize(3);
        model.setMatchTimerEnabled(true);
        model.setBoardInfoEnabled(true);
        model.setPlayerCounterEnabled(true);
        model.setDifficulty("Medium");
        model.setWinningLogic("Default");
        model.setFirstPlayer("X");
        model.setTheme("Light");
        model.setMusicTrack("Default (background.wav)");

        // ★ Update difficulty visibility after reset
        updateDifficultyVisibility();

        try {
            SettingsDAO.saveSettings(model);
        } catch (GameException e) {
            System.err.println("Warning: Database save blocked on reset step: " + e.getMessage());
        } finally {
            mainFrame.getGamePanel().synchronizeControllerSettings();
            JOptionPane.showMessageDialog(this, "Application runtime states reverted to defaults.", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ---------- Custom Inner Layout Shapes Drawing Engine ----------

    private class CustomIcon extends JComponent {
        private final String type;

        public CustomIcon(String type) {
            this.type = type;
            if (type.equals("chevron")) {
                setPreferredSize(new Dimension(12, 12));
            } else {
                setPreferredSize(new Dimension(24, 24));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_MAIN);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case "chevron":
                    g2.setColor(TEXT_MUTED);
                    g2.drawPolyline(new int[]{2, 6, 10}, new int[]{4, 8, 4}, 3);
                    break;
                case "gamemode":
                    g2.drawRoundRect(2, 5, 20, 14, 7, 7);
                    g2.drawLine(4, 12, 8, 12);
                    g2.drawLine(6, 10, 6, 14);
                    g2.fillOval(14, 13, 3, 3);
                    g2.fillOval(17, 9, 3, 3);
                    break;
                case "board":
                    g2.drawRect(2, 2, 20, 20);
                    g2.drawLine(2, 9, 22, 9);
                    g2.drawLine(2, 16, 22, 16);
                    g2.drawLine(9, 2, 9, 22);
                    g2.drawLine(16, 2, 16, 22);
                    break;
                case "difficulty":
                    g2.fillRect(3, 15, 4, 7);
                    g2.fillRect(10, 10, 4, 12);
                    g2.fillRect(17, 5, 4, 17);
                    break;
                case "winning":
                    g2.drawRoundRect(5, 4, 14, 10, 4, 4);
                    g2.drawLine(12, 14, 12, 19);
                    g2.drawLine(8, 19, 16, 19);
                    break;
                case "timer":
                    g2.drawOval(2, 2, 20, 20);
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(12, 12, 12, 7);
                    g2.drawLine(12, 12, 16, 12);
                    break;
                case "boardInfo":
                    g2.drawRoundRect(6, 7, 13, 13, 3, 3);
                    g2.setColor(CARD_BG);
                    g2.fillRoundRect(2, 3, 13, 13, 3, 3);
                    g2.setColor(TEXT_MAIN);
                    g2.drawRoundRect(2, 3, 13, 13, 3, 3);
                    break;
                case "counter":
                    g2.drawRoundRect(2, 2, 20, 20, 5, 5);
                    g2.drawOval(9, 6, 6, 6);
                    g2.drawArc(5, 14, 14, 10, 0, 180);
                    break;
                case "firstPlayer":
                    g2.drawOval(9, 3, 6, 6);
                    g2.drawArc(4, 11, 16, 10, 0, 180);
                    break;
                case "theme":
                    g2.drawOval(2, 2, 20, 20);
                    g2.fillArc(2, 2, 20, 20, 90, 180);
                    break;
                case "music":
                    g2.fillOval(4, 14, 6, 5);
                    g2.drawLine(10, 16, 10, 4);
                    g2.drawLine(10, 4, 18, 6);
                    g2.fillOval(12, 16, 6, 5);
                    g2.drawLine(18, 18, 18, 6);
                    break;
                case "history":
                    g2.drawRoundRect(4, 2, 16, 20, 3, 3);
                    g2.drawLine(8, 7, 16, 7);
                    g2.drawLine(8, 12, 16, 12);
                    g2.drawLine(8, 17, 13, 17);
                    break;
                case "customise":
                    g2.drawOval(3, 3, 18, 18);
                    g2.fillOval(7, 7, 3, 3);
                    g2.fillOval(14, 7, 3, 3);
                    g2.fillOval(7, 14, 3, 3);
                    break;
                case "clear":
                    g2.drawRect(5, 6, 14, 15);
                    g2.drawLine(2, 6, 22, 6);
                    g2.drawRect(9, 2, 6, 4);
                    g2.drawLine(9, 11, 9, 17);
                    g2.drawLine(12, 11, 12, 17);
                    g2.drawLine(15, 11, 15, 17);
                    break;
            }
            g2.dispose();
        }
    }

    private class CustomToggleSwitch extends JComponent {
        private boolean activated = true;

        public CustomToggleSwitch() {
            setPreferredSize(new Dimension(45, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    activated = !activated;
                    repaint();
                }
            });
        }

        public boolean isActivated() { return activated; }

        public void setActivated(boolean activated) {
            this.activated = activated;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(activated ? TOGGLE_ON : TOGGLE_OFF);
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, getHeight() - 4, getHeight() - 4);
            g2.setColor(Color.WHITE);
            int knobSize = getHeight() - 8;
            int xLocation = activated ? (getWidth() - knobSize - 4) : 4;
            g2.fillOval(xLocation, 4, knobSize, knobSize);
            g2.dispose();
        }
    }
}