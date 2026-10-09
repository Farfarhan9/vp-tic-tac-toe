package main.game.bot;

/**
 * BotFactory – Feature 4: board-specific difficulty selection.
 *
 * 3×3:
 *   Easy       → RandomBot
 *   Medium     → SmartBot
 *   Hard       → SmartBot  (with internal strategic enhancements via SmartBot)
 *   Impossible → MinimaxBot (perfect play)
 *
 * 4×4, 5×5, 6×6:
 *   Basic        → RandomBot
 *   Intermediate → SmartBot
 *   Advanced     → HeuristicBot (depth-limited minimax + heuristic)
 */
public class BotFactory {

    public static BotAI createBot(int boardSize, String difficulty) {

        // ★ LOG 1: Shows what board size and difficulty string are passed in
        System.out.println("BotFactory: boardSize=" + boardSize + ", difficulty=" + difficulty);

        if (difficulty == null) difficulty = "Medium";

        if (boardSize == 3) {
            switch (difficulty) {
                case "Easy":       return new RandomBot();
                case "Hard":       return new SmartBot();
                case "Impossible": return new MinimaxBot();
                default:           return new SmartBot();     // Medium
            }
        } else {
            // 4×4, 5×5, 6×6
            switch (difficulty) {
                case "Basic":        return new RandomBot();
                case "Advanced":     return new HeuristicBot();
                default:             return new SmartBot();   // Intermediate
            }
        }
    }

    /**
     * Returns the difficulty options appropriate for the given board size.
     * These strings are displayed in the Settings panel dropdown.
     */
    public static String[] getDifficultyOptions(int boardSize) {
        if (boardSize == 3) {
            return new String[]{"Easy", "Medium", "Hard", "Impossible"};
        } else {
            return new String[]{"Basic", "Intermediate", "Advanced"};
        }
    }
}