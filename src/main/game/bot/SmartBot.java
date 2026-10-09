package main.game.bot;

import main.game.GameModel;
import java.awt.Point;

public class SmartBot implements BotAI {
    private final RandomBot baselineEngine = new RandomBot();

    @Override
    public Point determineOptimalMove(GameModel model) {
        int dimensions = model.getBoardSize();
        String[][] dynamicState = model.getBoardState();
        String botSymbol = model.getPlayer2Symbol();
        String humanSymbol = model.getPlayer1Symbol();

        // Heuristic Layer 1: Evaluate if a single placement yields victory for the bot
        for (int r = 0; r < dimensions; r++) {
            for (int c = 0; c < dimensions; c++) {
                if (dynamicState[r][c].equals("")) {
                    dynamicState[r][c] = botSymbol;
                    boolean instantWin = (model.checkWinCoordinates() != null);
                    dynamicState[r][c] = ""; 
                    if (instantWin) return new Point(r, c);
                }
            }
        }

        // Heuristic Layer 2: Evaluate if human possesses immediate win threats and block them
        for (int r = 0; r < dimensions; r++) {
            for (int c = 0; c < dimensions; c++) {
                if (dynamicState[r][c].equals("")) {
                    // Inject human mark inside grid to calculate threat pathing
                    dynamicState[r][c] = humanSymbol;
                    boolean blockRequired = (model.checkWinCoordinates() != null);
                    dynamicState[r][c] = ""; 
                    if (blockRequired) return new Point(r, c);
                }
            }
        }

        // Heuristic Layer 3: Execute standard coordinate search patterns if no immediate triggers fire
        return baselineEngine.determineOptimalMove(model);
    }
}