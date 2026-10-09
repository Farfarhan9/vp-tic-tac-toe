package main.game.bot;

import java.awt.Point;
import main.game.GameModel;

/**
 * MinimaxBot – "Impossible" difficulty for 3×3 only (Feature 4).
 */
public class MinimaxBot implements BotAI {

    private String botSymbol;
    private String humanSymbol;

    @Override
    public Point determineOptimalMove(GameModel model) {
        // ★ LOG 2: Confirms that MinimaxBot is actually being invoked
        System.out.println("MinimaxBot.determineOptimalMove() called for board size " 
                           + model.getBoardSize());

        botSymbol   = model.getPlayer2Symbol();
        humanSymbol = model.getPlayer1Symbol();

        int boardSize = model.getBoardSize();
        String[][] board = copyBoard(model.getBoardState(), boardSize);

        int bestScore = Integer.MIN_VALUE;
        Point bestMove = null;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (board[r][c].isEmpty()) {
                    board[r][c] = botSymbol;
                    int score = minimax(board, boardSize, 0, false,
                                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                    board[r][c] = "";
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove  = new Point(r, c);
                    }
                }
            }
        }
        return bestMove;
    }

    private int minimax(String[][] board, int size, int depth,
                        boolean isMaximising, int alpha, int beta) {

        int result = evaluate(board, size);
        if (result != 0) return result;
        if (isDraw(board, size)) return 0;

        if (isMaximising) {
            int best = Integer.MIN_VALUE;
            outer:
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board[r][c].isEmpty()) {
                        board[r][c] = botSymbol;
                        int score = minimax(board, size, depth + 1, false, alpha, beta);
                        board[r][c] = "";
                        best  = Math.max(best, score);
                        alpha = Math.max(alpha, best);
                        if (beta <= alpha) break outer;
                    }
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            outer:
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board[r][c].isEmpty()) {
                        board[r][c] = humanSymbol;
                        int score = minimax(board, size, depth + 1, true, alpha, beta);
                        board[r][c] = "";
                        best = Math.min(best, score);
                        beta = Math.min(beta, best);
                        if (beta <= alpha) break outer;
                    }
                }
            }
            return best;
        }
    }

    private int evaluate(String[][] board, int size) {
        if (hasWon(board, size, botSymbol))   return 10;
        if (hasWon(board, size, humanSymbol)) return -10;
        return 0;
    }

    private boolean hasWon(String[][] board, int size, String sym) {
        for (int i = 0; i < size; i++) {
            boolean rowWin = true, colWin = true;
            for (int j = 0; j < size; j++) {
                if (!board[i][j].equals(sym)) rowWin = false;
                if (!board[j][i].equals(sym)) colWin = false;
            }
            if (rowWin || colWin) return true;
        }
        boolean d1 = true, d2 = true;
        for (int i = 0; i < size; i++) {
            if (!board[i][i].equals(sym))           d1 = false;
            if (!board[i][size - 1 - i].equals(sym)) d2 = false;
        }
        return d1 || d2;
    }

    private boolean isDraw(String[][] board, int size) {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c].isEmpty()) return false;
        return true;
    }

    private String[][] copyBoard(String[][] src, int size) {
        String[][] copy = new String[size][size];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                copy[r][c] = src[r][c];
        return copy;
    }
}