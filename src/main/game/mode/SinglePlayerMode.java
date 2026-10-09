package main.game.mode;

import main.game.GameModel;

public class SinglePlayerMode implements GameMode {
    @Override
    public String getModeIdentifier() { return "Singleplayer"; }

    @Override
    public String getPlayerTwoName() { return "Bot"; }

    @Override
    public void processTurnTransition(GameModel model) {
        model.switchPlayer();
    }

    @Override
    public String getRuntimeStatusText(GameModel model) {
        if (model.getCurrentPlayer().equals(model.getPlayer1Symbol())) {
            return "Your Turn";
        } else {
            return "Bot thinking...";
        }
    }
}