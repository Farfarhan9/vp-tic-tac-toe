package main.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import main.Main;
import main.database.SessionManager;
import main.exceptions.DatabaseException;
import main.game.GameController;
import main.game.GameModel;

public class GamePanel extends JPanel {

    // ------ Color palette ------
    private static final Color BG_MAIN = new Color(243, 243, 243);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_MAIN = new Color(50, 50, 50);
    private static final Color TEXT_MUTED = new Color(120, 120, 120);
    private static final Color BORDER_COLOR = new Color(225, 225, 225);

    // ------ UI components ------
    private Main mainFrame;
    private GameModel model;
    private GameBoard board;
    private GameController controller;

    private JLabel lblStatus;
    private JLabel lblTimerValue;
    private JLabel lblMoveValue;
    private JLabel lblHumanScoreValue;
    private JLabel lblBotScoreValue;

    private JButton btnAction;
    private JButton btnBack;
    private JButton btnMute;
    private boolean isMuted = false;
    private Thread timerThread;
    private int secondsElapsed = 0;
    private MusicPlayer audioSystemEngine;

    // ★ NEW: flag to track if the timer thread is running
    private volatile boolean timerRunning = false;

    private JPanel infoPanel;

    public GamePanel(Main mainFrame, GameModel model) {
        this.mainFrame = mainFrame;
        this.model = model;

        setBackground(BG_MAIN);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // 1. Top Section: Status Label
        lblStatus = new JLabel("Your Turn", SwingConstants.CENTER);
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblStatus.setForeground(TEXT_MAIN);
        add(lblStatus, BorderLayout.NORTH);

        // 2. Center Container: Info Panel + Game Board
        JPanel centerContainer = new JPanel(new BorderLayout(15, 20));
        centerContainer.setBackground(BG_MAIN);
        centerContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // ---- Info Panel ----
        infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        infoPanel.setBackground(CARD_BG);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Timer
        CustomIcon timerIcon = new CustomIcon("timer");
        lblTimerValue = new JLabel("0");
        lblTimerValue.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblTimerValue.setForeground(TEXT_MAIN);
        infoPanel.add(createStatItem(timerIcon, lblTimerValue));

        // Moves (Board Info)
        CustomIcon movesIcon = new CustomIcon("moves");
        lblMoveValue = new JLabel("0");
        lblMoveValue.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblMoveValue.setForeground(TEXT_MAIN);
        infoPanel.add(createStatItem(movesIcon, lblMoveValue));

        // Human Score (Player Counter)
        CustomIcon humanIcon = new CustomIcon("counter");
        lblHumanScoreValue = new JLabel("0");
        lblHumanScoreValue.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblHumanScoreValue.setForeground(TEXT_MAIN);
        infoPanel.add(createStatItem(humanIcon, lblHumanScoreValue));

        // Bot Score (Robot)
        CustomIcon botIcon = new CustomIcon("robot");
        lblBotScoreValue = new JLabel("0");
        lblBotScoreValue.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblBotScoreValue.setForeground(TEXT_MAIN);
        infoPanel.add(createStatItem(botIcon, lblBotScoreValue));

        centerContainer.add(infoPanel, BorderLayout.NORTH);

        // ---- Game Board ----
        initGame();
        centerContainer.add(board, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        // 3. Bottom Control Row
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(BG_MAIN);

        btnAction = new JButton("Play again");
        btnAction.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btnAction.setBackground(Color.WHITE);
        btnAction.setForeground(TEXT_MAIN);
        btnAction.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        btnAction.setPreferredSize(new Dimension(130, 40));
        btnAction.setFocusPainted(false);
        btnAction.setVisible(false);
        btnAction.addActionListener(e -> {
            if (controller != null) {
                controller.resetGameController();
                btnAction.setVisible(false);
                resetTimer();
                lblStatus.setText("Your Turn");
                if (!timerRunning) startTimer();
            }
        });
        bottomPanel.add(btnAction, BorderLayout.CENTER);

        btnBack = new JButton("◀  Menu");
        btnBack.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnBack.setBackground(Color.WHITE);
        btnBack.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        btnBack.setPreferredSize(new Dimension(90, 40));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> {
            saveCurrentDuration();   // ★ NEW: save timer before going back
            stopTimer();
            mainFrame.switchToPanel("WelcomePanel");
        });
        bottomPanel.add(btnBack, BorderLayout.WEST);

        btnMute = new JButton("🔊");
        btnMute.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btnMute.setBackground(Color.WHITE);
        btnMute.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        btnMute.setPreferredSize(new Dimension(50, 40));
        btnMute.setFocusPainted(false);
        btnMute.addActionListener(e -> {
            audioSystemEngine.toggleMute();
            isMuted = !isMuted;
            btnMute.setText(isMuted ? "🔇" : "🔊");
        });
        bottomPanel.add(btnMute, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // ------ Audio ------
        audioSystemEngine = MusicPlayer.getInstance("src/resources/sounds/background.wav");

        updateInfoPanelVisibility();
        if (!timerRunning) startTimer();
    }
    
    /**
     * Saves the current elapsed timer value to the database for the active session.
     * Called when the user presses Back.
     */
    private void saveCurrentDuration() {
        int gameId = model.getCurrentGameId();
        if (gameId < 0) return;
        try {
            SessionManager.saveGameDuration(gameId, secondsElapsed);
        } catch (DatabaseException e) {
            System.err.println("[GamePanel] Failed to save timer: " + e.getMessage());
        }
    }

    private JPanel createStatItem(JComponent icon, JLabel valueLabel) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        item.setAlignmentY(Component.CENTER_ALIGNMENT);
        item.add(icon);
        item.add(valueLabel);
        return item;
    }

    // ------ CustomIcon (unchanged) ------
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
                case "timer":
                    g2.drawOval(2, 2, 20, 20);
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(12, 12, 12, 7);
                    g2.drawLine(12, 12, 16, 12);
                    break;
                case "moves":
                    g2.drawRoundRect(6, 7, 13, 13, 3, 3);
                    g2.setColor(CARD_BG);
                    g2.fillRoundRect(4, 5, 13, 13, 3, 3);
                    g2.setColor(TEXT_MAIN);
                    g2.drawRoundRect(4, 5, 13, 13, 3, 3);
                    break;
                case "counter":
                    g2.drawOval(8, 2, 8, 8);
                    g2.drawArc(2, 7, 20, 14, 180, 180);
                    g2.drawLine(2, 13, 22, 13);
                    break;
                case "robot":
                    g2.drawLine(12, 0, 12, 2);
                    g2.drawRoundRect(4, 2, 16, 7, 2, 2);
                    g2.fillOval(8, 5, 2, 2);
                    g2.fillOval(14, 5, 2, 2);
                    g2.drawArc(2, 7, 20, 14, 180, 180);
                    g2.drawLine(2, 13, 22, 13);
                    break;
                case "board":
                    g2.drawRect(2, 2, 20, 20);
                    g2.drawLine(2, 9, 22, 9);
                    g2.drawLine(2, 16, 22, 16);
                    g2.drawLine(9, 2, 9, 22);
                    g2.drawLine(16, 2, 16, 22);
                    break;
            }
            g2.dispose();
        }
    }

    // ------ Board Management ------
    private void initGame() {
        int size = model.getBoardSize();
        board = new GameBoard(size, e -> {
            if (controller != null) {
                controller.actionPerformed(e);
                updateGameOverButton();
            }
        });
        controller = new GameController(model, board, lblStatus, lblMoveValue, lblHumanScoreValue, lblBotScoreValue);
        controller.setGameOverCallback(this::updateGameOverButton);
    }

    private void updateGameOverButton() {
        if (model.isGameOver()) {
            String status = lblStatus.getText();
            if (status.equals("You won!")) {
                btnAction.setText("Play again");
            } else if (status.equals("You lost!")) {
                btnAction.setText("Try again");
            } else {
                btnAction.setText("Play again");
            }
            btnAction.setVisible(true);
            btnAction.revalidate();
            btnAction.repaint();
            this.revalidate();
            this.repaint();
        } else {
            btnAction.setVisible(false);
        }
    }

    private void recreateBoard() {
        Component centerContainer = getComponent(1);
        if (centerContainer instanceof JPanel) {
            JPanel container = (JPanel) centerContainer;
            Component oldBoard = ((BorderLayout) container.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (oldBoard != null) container.remove(oldBoard);
        }
        initGame();
        if (centerContainer instanceof JPanel) {
            ((JPanel) centerContainer).add(board, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        if (controller != null) controller.resetGameController();
        btnAction.setVisible(false);
        resetTimer();
        lblStatus.setText("Your Turn");
    }

    // ------ Visibility toggles ------
    private void updateInfoPanelVisibility() {
        Component[] comps = infoPanel.getComponents();
        boolean showTimer = model.isMatchTimerEnabled();
        boolean showMoves = model.isBoardInfoEnabled();
        boolean showHuman = model.isPlayerCounterEnabled();
        boolean showBot = model.isPlayerCounterEnabled();
        if (comps.length >= 4) {
            comps[0].setVisible(showTimer);
            comps[1].setVisible(showMoves);
            comps[2].setVisible(showHuman);
            comps[3].setVisible(showBot);
        }
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    // ------ Timer management ------
    private void startTimer() {
        if (timerRunning) return;
        timerRunning = true;
        timerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && timerRunning) {
                    Thread.sleep(1000);
                    if (!model.isGameOver() && mainFrame.getContentPane().isVisible()) {
                        secondsElapsed++;
                        if (model.isMatchTimerEnabled()) {
                            SwingUtilities.invokeLater(() -> lblTimerValue.setText(String.valueOf(secondsElapsed)));
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                timerRunning = false;
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    public void stopTimer() {
        timerRunning = false;
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    private void resetTimer() {
        secondsElapsed = 0;
        SwingUtilities.invokeLater(() -> lblTimerValue.setText("0"));
    }

    public void setTimerSeconds(int seconds) {
        this.secondsElapsed = seconds;
        SwingUtilities.invokeLater(() -> lblTimerValue.setText(String.valueOf(seconds)));
        if (!timerRunning) startTimer();
    }

    // ------ Public methods ------
    public void refreshBoardLayout() {
        if (controller != null) {
            controller.resetGameController();
            btnAction.setVisible(false);
            resetTimer();
            lblStatus.setText("Your Turn");
            updateInfoPanelVisibility();
            if (!timerRunning) startTimer();
        }
    }

    public void synchronizeControllerSettings() {
        if (controller != null) {
            recreateBoard();
            updateInfoPanelVisibility();
        }
    }

    /**
     * Refreshes the board UI from the current model state WITHOUT resetting the model.
     * Used when restoring a saved game session.
     */
    public void refreshBoardOnly() {
        int size = model.getBoardSize();
        if (board == null || board.getBoardSize() != size) {
            // Recreate board if size mismatch
            Component centerContainer = getComponent(1);
            if (centerContainer instanceof JPanel) {
                JPanel container = (JPanel) centerContainer;
                if (board != null) container.remove(board);
                board = new GameBoard(size, e -> {
                    if (controller != null) {
                        controller.actionPerformed(e);
                        updateGameOverButton();
                    }
                });
                container.add(board, BorderLayout.CENTER);
                container.revalidate();
                container.repaint();
            }
            // Recreate controller with the new board
            controller = new GameController(model, board, lblStatus, lblMoveValue, lblHumanScoreValue, lblBotScoreValue);
            controller.setGameOverCallback(this::updateGameOverButton);
        }

        // Update each cell from the model
        String[][] state = model.getBoardState();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                String mark = state[r][c];
                if (!mark.isEmpty()) {
                    board.updateCell(r, c, mark);
                } else {
                    board.clearCell(r, c);
                }
            }
        }

        // Update labels
        lblMoveValue.setText(String.valueOf(model.getMoveCount()));
        lblHumanScoreValue.setText(String.valueOf(model.getHumanWins()));
        lblBotScoreValue.setText(String.valueOf(model.getBotWins()));

        // Update status
        if (model.isGameOver()) {
            lblStatus.setText("Game Over");
            btnAction.setVisible(true);
            btnAction.setText("Play again");
        } else {
            lblStatus.setText(model.getCurrentPlayer().equals(model.getPlayer1Symbol()) ? "Your Turn" : "Waiting...");
            btnAction.setVisible(false);
        }
        updateInfoPanelVisibility();
    }
    
}