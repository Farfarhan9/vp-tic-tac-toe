package main.game.bot;

import main.game.GameModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomBot implements BotAI {
    private final Random randomGenerator = new Random();

    @Override
    public Point determineOptimalMove(GameModel model) {
        int dimensions = model.getBoardSize();
        String[][] matrixState = model.getBoardState();
        List<Point> legalOptions = new ArrayList<>();

        for (int row = 0; row < dimensions; row++) {
            for (int col = 0; col < dimensions; col++) {
                if (matrixState[row][col].equals("")) {
                    legalOptions.add(new Point(row, col));
                }
            }
        }

        if (legalOptions.isEmpty()) return null;
        return legalOptions.get(randomGenerator.nextInt(legalOptions.size()));
    }
}