package main.game.mode;

import main.game.GameModel;

public class MultiPlayerMode implements GameMode {
    @Override
    public String getModeIdentifier() { return "Multiplayer"; }

    @Override
    public String getPlayerTwoName() { return "Player 2"; }

    @Override
    public void processTurnTransition(GameModel model) {
        model.switchPlayer();
    }

    @Override
    public String getRuntimeStatusText(GameModel model) {
        if (model.getCurrentPlayer().equals(model.getPlayer1Symbol())) {
            return "Player 1 Turn";
        } else {
            return "Player 2 Turn";
        }
    }
}