package main.game.bot;

import java.awt.Point;
import main.game.GameModel;

/**
 * HeuristicBot – "Advanced" difficulty for 4×4, 5×5, 6×6 boards (Feature 4).
 *
 * Uses depth-limited minimax with alpha-beta pruning and a positional heuristic
 * to evaluate non-terminal states within the time/depth budget.
 *
 * depth limit:
 *   4×4 → 5 plies
 *   5×5 → 4 plies
 *   6×6 → 3 plies
 */
public class HeuristicBot implements BotAI {

    private String botSymbol;
    private String humanSymbol;
    private int    winLen;          // consecutive cells needed to win
    private int    boardSize;
    private int    maxDepth;

    @Override
    public Point determineOptimalMove(GameModel model) {
        botSymbol   = model.getPlayer2Symbol();
        humanSymbol = model.getPlayer1Symbol();
        boardSize   = model.getBoardSize();

        // Win length from winning logic setting
        winLen = model.getWinningLogic().equals("3x3 Rules") ? 3 : boardSize;

        // Depth budget by board size
        switch (boardSize) {
            case 4:  maxDepth = 5; break;
            case 5:  maxDepth = 4; break;
            default: maxDepth = 3; break;   // 6×6
        }

        String[][] board = copyBoard(model.getBoardState(), boardSize);

        int bestScore = Integer.MIN_VALUE;
        Point bestMove = null;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (board[r][c].isEmpty()) {
                    board[r][c] = botSymbol;
                    int score = minimax(board, 1, false,
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

    private int minimax(String[][] board, int depth,
                        boolean isMaximising, int alpha, int beta) {

        int terminal = evaluate(board);
        if (terminal != 0) return terminal;
        if (isDraw(board))  return 0;
        if (depth >= maxDepth) return heuristic(board);

        if (isMaximising) {
            int best = Integer.MIN_VALUE;
            outer:
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    if (board[r][c].isEmpty()) {
                        board[r][c] = botSymbol;
                        int s = minimax(board, depth + 1, false, alpha, beta);
                        board[r][c] = "";
                        best  = Math.max(best, s);
                        alpha = Math.max(alpha, best);
                        if (beta <= alpha) break outer;
                    }
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            outer:
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    if (board[r][c].isEmpty()) {
                        board[r][c] = humanSymbol;
                        int s = minimax(board, depth + 1, true, alpha, beta);
                        board[r][c] = "";
                        best = Math.min(best, s);
                        beta = Math.min(beta, best);
                        if (beta <= alpha) break outer;
                    }
                }
            }
            return best;
        }
    }

    /** +1000 bot wins, -1000 human wins, 0 otherwise (terminal check). */
    private int evaluate(String[][] board) {
        if (hasWon(board, botSymbol))   return 1000;
        if (hasWon(board, humanSymbol)) return -1000;
        return 0;
    }

    /**
     * Counts "open" runs of length k for each player.
     * Bot runs score positive; human runs score negative.
     */
    private int heuristic(String[][] board) {
        return scoreForPlayer(board, botSymbol) - scoreForPlayer(board, humanSymbol);
    }

    private int scoreForPlayer(String[][] board, String sym) {
        int score = 0;
        int[] dr = {0, 1, 1,  1};
        int[] dc = {1, 0, 1, -1};

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                for (int d = 0; d < 4; d++) {
                    int count = 0;
                    boolean open = true;
                    for (int k = 0; k < winLen; k++) {
                        int nr = r + k * dr[d];
                        int nc = c + k * dc[d];
                        if (nr < 0 || nr >= boardSize || nc < 0 || nc >= boardSize) {
                            open = false; break;
                        }
                        String cell = board[nr][nc];
                        if (cell.equals(sym))            count++;
                        else if (!cell.isEmpty())       { open = false; break; }
                    }
                    if (open && count > 0) score += (int) Math.pow(10, count);
                }
            }
        }
        return score;
    }

    private boolean hasWon(String[][] board, String sym) {
        int[] dr = {0, 1, 1,  1};
        int[] dc = {1, 0, 1, -1};
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                for (int d = 0; d < 4; d++) {
                    int count = 0;
                    for (int k = 0; k < winLen; k++) {
                        int nr = r + k * dr[d];
                        int nc = c + k * dc[d];
                        if (nr < 0 || nr >= boardSize || nc < 0 || nc >= boardSize) break;
                        if (board[nr][nc].equals(sym)) count++;
                        else break;
                    }
                    if (count == winLen) return true;
                }
            }
        }
        return false;
    }

    private boolean isDraw(String[][] board) {
        for (int r = 0; r < boardSize; r++)
            for (int c = 0; c < boardSize; c++)
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