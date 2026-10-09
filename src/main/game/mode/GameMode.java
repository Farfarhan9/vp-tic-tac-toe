package main.game.mode;

import main.game.GameModel;

public interface GameMode {
    String getModeIdentifier();
    String getPlayerTwoName();
    void processTurnTransition(GameModel model);
    String getRuntimeStatusText(GameModel model);
}