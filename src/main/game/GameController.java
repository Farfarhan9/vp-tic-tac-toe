package main.game;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import main.ui.GameBoard;
import main.game.mode.GameMode;
import main.game.mode.SinglePlayerMode;
import main.game.mode.MultiPlayerMode;
import main.game.bot.BotAI;
import main.game.bot.BotFactory;
import main.database.SessionManager;
import main.exceptions.DatabaseException;

/**
 * GameController – v2 update.
 *
 * Changes:
 *   • Bot selection now uses BotFactory (Feature 4 – board-specific difficulty).
 *   • Records every move to the database (Feature 2).
 *   • Notifies game completion so SessionManager can close the session (Feature 2).
 *   • Exposes stopBotThread() so GamePanel can cancel a pending bot turn when
 *     the Back button is pressed (Feature 6 – timer / session fix).
 */
public class GameController implements ActionListener {

    private final GameModel  gameModel;
    private final GameBoard  gameBoard;
    private final JLabel     lblStatusText;
    private final JLabel     lblMovesTracked;
    private final JLabel     lblHumanScoreboard;
    private final JLabel     lblBotScoreboard;

    private GameMode executionMode;
    private BotAI    opponentIntelligence;

    /** Callback invoked when the game ends (win, loss, or draw). */
    private Runnable gameOverCallback;

    /** Flag to abort an in‑flight bot thread when Back is pressed. */
    private volatile boolean botAborted = false;
    /** Reference to the running bot thread so we can interrupt it. */
    private volatile Thread  currentBotThread = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public GameController(GameModel model, GameBoard board, JLabel lblStatus,
                          JLabel lblMoves, JLabel lblHuman, JLabel lblBot) {
        this.gameModel          = model;
        this.gameBoard          = board;
        this.lblStatusText      = lblStatus;
        this.lblMovesTracked    = lblMoves;
        this.lblHumanScoreboard = lblHuman;
        this.lblBotScoreboard   = lblBot;

        refreshBotFromModel();
        synchronizeActiveGameMode();
    }

    /** (Re‑)creates the bot from the model's current difficulty + board size. */
    public void refreshBotFromModel() {
    	 System.out.println("refreshBotFromModel: difficulty=" + gameModel.getDifficulty() + ", boardSize=" + gameModel.getBoardSize());
        this.opponentIntelligence =
            BotFactory.createBot(gameModel.getBoardSize(), gameModel.getDifficulty());
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setGameOverCallback(Runnable callback) {
        this.gameOverCallback = callback;
    }

    // ── Mode sync ─────────────────────────────────────────────────────────────

    public void synchronizeActiveGameMode() {
        try {
            synchronized (gameModel) {
                if ("Singleplayer".equalsIgnoreCase(gameModel.getGameMode())) {
                    this.executionMode = new SinglePlayerMode();
                } else {
                    this.executionMode = new MultiPlayerMode();
                }
            }
        } catch (NullPointerException e) {
            System.err.println("GameModel mode state uninitialized. Defaulting to Singleplayer.");
            this.executionMode = new SinglePlayerMode();
        }
        updateInterfaceDisplay();
    }

    // ── ActionListener (cell clicks) ──────────────────────────────────────────

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (gameModel.isGameOver() || botAborted) return;

            if (!(e.getSource() instanceof javax.swing.JButton)) return;
            javax.swing.JButton btn = (javax.swing.JButton) e.getSource();

            Object rowProp = btn.getClientProperty("row");
            Object colProp = btn.getClientProperty("col");
            if (rowProp == null || colProp == null) return;

            processTurnMove((int) rowProp, (int) colProp);

            if (gameModel.getGameMode().equals("Singleplayer")
                    && !gameModel.isGameOver()
                    && gameModel.getCurrentPlayer().equals(gameModel.getPlayer2Symbol())) {

                gameBoard.freezeBoard();
                executeAsynchronousBotThread();
            }
        } catch (Exception ex) {
            System.err.println("UI click error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Turn processing ───────────────────────────────────────────────────────

    private synchronized void processTurnMove(int row, int col) {
        try {
            String mark = gameModel.getCurrentPlayer();

            if (gameModel.setMove(row, col)) {
                gameBoard.updateCell(row, col, mark);
                lblMovesTracked.setText(String.valueOf(gameModel.getMoveCount()));

                // Persist the move (Feature 2)
                persistMove(mark, row, col);

                int[][] winningLine = gameModel.checkWinCoordinates();
                if (winningLine != null) {
                    gameModel.setGameOver(true);
                    gameBoard.highlightWinningCells(winningLine);

                    String result;
                    if (mark.equals(gameModel.getPlayer1Symbol())) {
                        gameModel.incrementHumanWins();
                        lblStatusText.setText("You won!");
                        lblHumanScoreboard.setText(String.valueOf(gameModel.getHumanWins()));
                        result = "player1";
                    } else {
                        gameModel.incrementBotWins();
                        lblStatusText.setText(gameModel.getGameMode().equals("Singleplayer")
                                ? "You lost!" : "Player 2 Wins!");
                        lblBotScoreboard.setText(String.valueOf(gameModel.getBotWins()));
                        result = "player2";
                    }
                    persistGameOver(result);
                    fireGameOverCallback();
                    return;
                }

                if (gameModel.checkDraw()) {
                    gameModel.setGameOver(true);
                    gameModel.incrementDraws();
                    lblStatusText.setText("Draw!");
                    persistGameOver("draw");
                    fireGameOverCallback();
                    return;
                }

                executionMode.processTurnTransition(gameModel);
                updateInterfaceDisplay();
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Illegal grid coordinate: " + e.getMessage());
        }
    }

    // ── Bot thread ────────────────────────────────────────────────────────────

    private void executeAsynchronousBotThread() {
        botAborted = false;

        Thread botThread = new Thread(() -> {
            try {
                Thread.sleep(750);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }

            if (botAborted) return;

            Point target = null;
            try {
                synchronized (gameModel) {
                    target = opponentIntelligence.determineOptimalMove(gameModel);
                }
            } catch (NullPointerException | ArrayIndexOutOfBoundsException ex) {
                System.err.println("Bot calculation error: " + ex.getMessage());
            }

            if (botAborted) return;

            final Point finalTarget = target;
            SwingUtilities.invokeLater(() -> {
                if (botAborted) return;
                try {
                    synchronized (gameModel) {
                        String[][] state = gameModel.getBoardState();
                        int size = gameModel.getBoardSize();
                        gameBoard.initializeBoard();
                        for (int r = 0; r < size; r++)
                            for (int c = 0; c < size; c++)
                                if (!"".equals(state[r][c]))
                                    gameBoard.updateCell(r, c, state[r][c]);
                    }
                    if (finalTarget != null) processTurnMove(finalTarget.x, finalTarget.y);
                } catch (Exception ex) {
                    System.err.println("Bot UI sync error: " + ex.getMessage());
                }
            });
        }, "BotThread");

        botThread.setDaemon(true);
        currentBotThread = botThread;
        botThread.start();
    }

    /**
     * Aborts any pending bot move immediately.
     * Called by GamePanel when Back is pressed (Feature 6).
     */
    public void stopBotThread() {
        botAborted = true;
        Thread t = currentBotThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        currentBotThread = null;
    }

    // ── Display update ────────────────────────────────────────────────────────

    private void updateInterfaceDisplay() {
        if (executionMode != null) {
            lblStatusText.setText(executionMode.getRuntimeStatusText(gameModel));
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void resetGameController() {
        try {
            stopBotThread();
            botAborted = false;

            synchronized (gameModel) {
                gameModel.resetGame();
            }
            refreshBotFromModel();
            synchronizeActiveGameMode();
            gameBoard.updateBoardSize(gameModel.getBoardSize());
            lblMovesTracked.setText("0");
            updateInterfaceDisplay();
        } catch (Exception ex) {
            System.err.println("Controller reset error: " + ex.getMessage());
        }
    }

    // ── Database helpers ──────────────────────────────────────────────────────

    /** Records a move to the DB if a game session is active. */
    private void persistMove(String player, int row, int col) {
        int gameId = gameModel.getCurrentGameId();
        if (gameId < 0) return;   // no active session
        try {
            SessionManager.recordMove(gameId, gameModel.getMoveCount(), player, row, col);
        } catch (DatabaseException e) {
            System.err.println("[GameController] Move persist failed: " + e);
        }
    }

    /**
     * Marks the game as completed in the DB and stores the final win counters.
     */
    private void persistGameOver(String result) {
        int gameId = gameModel.getCurrentGameId();
        if (gameId < 0) return;
        try {
            // ★ UPDATED: pass the current win counters to SessionManager
            SessionManager.completeGame(
                gameId,
                result,
                0, // duration will be saved later by GamePanel
                gameModel.getMoveCount(),
                gameModel.getHumanWins(),
                gameModel.getBotWins(),
                gameModel.getDraws()
            );
        } catch (DatabaseException e) {
            System.err.println("[GameController] Game complete persist failed: " + e);
        }
    }

    private void fireGameOverCallback() {
        if (gameOverCallback != null) {
            SwingUtilities.invokeLater(gameOverCallback);
        }
    }
}