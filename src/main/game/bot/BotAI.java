package main.game.bot;

import main.game.GameModel;
import java.awt.Point;

public interface BotAI {
    Point determineOptimalMove(GameModel model);
}