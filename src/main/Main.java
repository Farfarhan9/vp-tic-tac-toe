package main;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import main.ui.WelcomePanel;
import main.ui.GamePanel;
import main.ui.SettingsPanel;
import main.game.GameModel;
import main.database.DatabaseConnection;
import main.database.SettingsDAO;
import main.database.SessionManager;
import main.exceptions.GameException;
import main.exceptions.DatabaseException;

public class Main extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GameModel model;
    private GamePanel gamePanel;
    private SettingsPanel settingsPanel;

    public Main() {
        setTitle("Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 780);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(245, 245, 245));

        model = new GameModel();
        loadSettingsIntoModel();

        welcomePanelInitialized();

        add(mainPanel);
        cardLayout.show(mainPanel, "WelcomePanel");
    }

    private void loadSettingsIntoModel() {
        try {
            SettingsDAO.loadSettings(model);
        } catch (GameException e) {
            System.err.println("Could not load settings: " + e.getMessage());
        }
    }

    private void welcomePanelInitialized() {
        try {
            WelcomePanel welcomePanel = new WelcomePanel(this);
            gamePanel = new GamePanel(this, model);
            settingsPanel = new SettingsPanel(this, model);

            mainPanel.add(welcomePanel, "WelcomePanel");
            mainPanel.add(gamePanel, "GamePanel");
            mainPanel.add(settingsPanel, "SettingsPanel");
        } catch (NullPointerException e) {
            System.err.println("Fatal Interface Generation Breakdown: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                "A critical interface render framework exception occurred. Shutting down.",
                "Fatal System Failure",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void switchToPanel(String panelName) {
        try {
            cardLayout.show(mainPanel, panelName);
        } catch (IllegalArgumentException e) {
            System.err.println("Attempted invalid CardLayout component redirection to tag identity: " + panelName);
        }
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    // ---- Session management ----

    public void startNewGame() {
        try {
            model.resetGame();
            model.resetScores();

            String player1Name = "Player 1";
            String player2Name = model.getGameMode().equals("Singleplayer") ? "Bot" : "Player 2";
            int gameId = SessionManager.createNewSession(
                model.getBoardSize(),
                model.getGameMode(),
                model.getDifficulty(),
                model.getWinningLogic(),
                model.getFirstPlayer(),
                player1Name,
                player2Name
            );
            model.setCurrentGameId(gameId);

            if (gamePanel != null) {
                gamePanel.refreshBoardLayout();
                gamePanel.setTimerSeconds(0);
            }
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(this,
                "Could not create new game session: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        switchToPanel("GamePanel");
    }

    public void continueGame(SessionManager.SessionSummary summary) {
        try {
            // 1. Restore session settings and win counters
            model.setBoardSize(summary.boardSize);
            model.setGameMode(summary.gamemode);
            model.setDifficulty(summary.difficulty);
            model.setWinningLogic(summary.winningLogic);
            model.setFirstPlayer(summary.firstPlayer);
            model.setHumanWins(summary.humanWins);
            model.setBotWins(summary.botWins);
            model.setDraws(summary.draws);

            // 2. Determine whether to resume an unfinished game or start a new one
            int gameId;
            int savedDuration = 0;

            if (summary.gameResult != null) {
                // Last game is finished → start a new game in the same session
                System.out.println("[Main] Last game finished, creating new game in session " + summary.sessionId);
                gameId = SessionManager.createNewGame(summary.sessionId);
                model.setCurrentGameId(gameId);
                model.resetGame(); // fresh board
                savedDuration = 0;
            } else {
                // Unfinished game → resume it
                System.out.println("[Main] Resuming unfinished game " + summary.gameId + " in session " + summary.sessionId);
                gameId = summary.gameId;
                model.setCurrentGameId(gameId);

                // Load moves from DB
                List<SessionManager.MoveRecord> moves = SessionManager.loadMoves(gameId);
                System.out.println("[Main] Loaded " + moves.size() + " moves");

                // Reset board and apply moves
                model.resetGame(); // clears board, sets currentPlayer to firstPlayer
                String p1 = model.getPlayer1Symbol();
                String p2 = model.getPlayer2Symbol();

                for (SessionManager.MoveRecord move : moves) {
                    String symbol = move.player.equals("player1") || move.player.equals(p1) ? p1 : p2;
                    model.setBoardCell(move.row, move.col, symbol);
                    model.incrementMoveCount();
                }

                // Set the current player correctly based on move count
                int moveCount = moves.size();
                model.setCurrentPlayer((moveCount % 2 == 0) ? p1 : p2);

                // Retrieve saved timer duration
                savedDuration = SessionManager.loadGameDuration(gameId);
                System.out.println("[Main] Restored timer: " + savedDuration + "s");
            }

            // 3. Refresh the UI with the restored state
            if (gamePanel != null) {
                // Ensure the board size matches before refresh
                gamePanel.refreshBoardOnly();      // updates board and labels
                gamePanel.setTimerSeconds(savedDuration);
                System.out.println("[Main] UI refreshed, timer set to " + savedDuration);
            }

        } catch (DatabaseException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Could not restore game session: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            startNewGame();
        }
        switchToPanel("GamePanel");
    }

    public static void main(String[] args) {
        try {
            DatabaseConnection.testConnection();
        } catch (GameException e) {
            System.err.println(e.toString());
            JOptionPane.showMessageDialog(null,
                "MySQL service appears offline on local environment.\nApplication features will operate in safe local memory state.",
                "Database Environment Offline",
                JOptionPane.WARNING_MESSAGE);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                Main frame = new Main();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Fatal structural failure inside Event Dispatch Thread instantiation frame loop: " + e.getMessage());
            }
        });
    }
}