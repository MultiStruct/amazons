package com.codingame.game;

import com.codingame.game.gota.Action;
import com.codingame.game.gota.Board;
import com.codingame.game.gota.Cell;
import com.codingame.game.gota.Viewer;
import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.endscreen.EndScreenModule;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Random;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphics;
    @Inject private ToggleModule toggleModule;
    @Inject private EndScreenModule endScreenModule;

    Board board;
    Viewer viewer;
    int gamesPlayed = 0;
    int currentPlayer = 0;
    int turnsSinceReset = 0;
    boolean reset = false;
    final int MAX_GAMES = 2;
    String lastAction = "null";
    Random rand;
    int boardSize = 8;

    @Override
    public void init() {
        boardSize = 8;
        rand = new Random(gameManager.getSeed());
        gameManager.setMaxTurns(200);
        board = new Board(boardSize, gameManager.getSeed());
        viewer = new Viewer(graphics, board, gameManager, toggleModule);
        gameManager.setFirstTurnMaxTime(1000);
    }

    @Override
    public void gameTurn(int turn) {
        if (reset) {
            viewer.reset(board);
            reset = false;
            currentPlayer = 0;
            lastAction = "null";
            turnsSinceReset = 0;
            return;
        }

        if (turnsSinceReset < 2) {
            gameManager.setTurnMaxTime(1000);
        } else {
            gameManager.setTurnMaxTime(100);
        }
        ++turnsSinceReset;

        ArrayList<Action> actions = board.getLegalActions(currentPlayer);

        Player player = gameManager.getPlayer(gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1);

        try {
            int count = 0;
            for (Action act : actions) {
                count += act.arrows.size();
            }
            sendInputs(count);
            player.execute();

            String[] outputs = player.getOutputs().get(0).split("MSG|msg");
            outputs[0] = outputs[0].replaceAll("\\s", "").toLowerCase();

            if (outputs.length > 1) {
                outputs[1] = outputs[1].replaceFirst("MSG\\s","");
                outputs[1] = outputs[1].substring(0, Math.min(outputs[1].length(), 16));
                viewer.playerUIS[player.getIndex()].msg.setText(outputs[1]);
            } else {
                viewer.playerUIS[player.getIndex()].msg.setText("");
            }

            boolean found = false;

            if(outputs[0].equals("random")) {
                found = true;
                int a = rand.nextInt(actions.size());
                int b = rand.nextInt(actions.get(a).arrows.size());

                lastAction = actions.get(a).toString() + actions.get(a).arrows.get(b).toString();

                viewer.applyAction(actions.get(a).unit, actions.get(a).target, actions.get(a).arrows.get(b));
                board.applyAction(actions.get(a).unit, actions.get(a).target, actions.get(a).arrows.get(b));

            } else {
                for (Action action : actions) {
                    String s = action.toString();
                    if (outputs[0].startsWith(s)) {
                        for (Cell arrow : action.arrows) {
                            if ((s + arrow.toString()).equals(outputs[0])) {
                                found = true;
                                lastAction = action.toString() + arrow.toString();

                                viewer.applyAction(action.unit, action.target, arrow);
                                board.applyAction(action.unit, action.target, arrow);
                                break;
                            }
                        }
                    }
                    if (found)
                        break;
                }
            }

            if(!found) {
                throw new InvalidAction(String.format("Action was not valid!"));
            }

            viewer.playerUIS[player.getIndex()].action.setText(lastAction);
            graphics.commitEntityState(0, viewer.playerUIS[player.getIndex()].action, viewer.playerUIS[player.getIndex()].msg);

        } catch (AbstractPlayer.TimeoutException e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " did not output in time!"));
            player.deactivate(player.getNicknameToken() + " timeout.");
            player.setScore(-1);
            gameManager.endGame();
            return;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | InvalidAction e) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " made an invalid action!"));
            player.deactivate(player.getNicknameToken() + " made an invalid action.");
            player.setScore(-1);
            gameManager.endGame();
            return;
        }

        if(board.hasPlayerLost(currentPlayer ^ 1)) {
            gameManager.addToGameSummary(player.getNicknameToken() + " won this round.");
            gameManager.addTooltip(player, player.getNicknameToken() + " won this round");
            gameManager.getPlayer(gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1).setScore(gameManager.getPlayer(gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1).getScore() + 1);
            viewer.playerUIS[gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1].score.setText(Integer.toString(gameManager.getPlayer(gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1).getScore()));
            board = new Board(boardSize, gameManager.getSeed());
            gamesPlayed++;
            reset = true;
        }

        currentPlayer ^= 1;

        if (gamesPlayed == MAX_GAMES) {
            gameManager.endGame();
        }
    }

    void sendInputs(int actions) {
        Player player = gameManager.getPlayer(gamesPlayed % 2 == 0 ? currentPlayer : currentPlayer ^ 1);

        if (gamesPlayed == 0 && turnsSinceReset < 3) {
            player.sendInputLine(Integer.toString(boardSize));
        }

        // Color
        player.sendInputLine(currentPlayer == 0 ? "w" : "b");

        // Board
        for(int y = 0; y < board.getHEIGHT(); ++y) {
            String s = "";
            for (int x = 0; x < board.getWIDTH(); ++x) {
                Cell cell = board.cells[board.getHEIGHT() - y - 1][x];
                if (cell.wall) {
                    s += "-";
                } else if (cell.unit != null) {
                    s += cell.unit.owner == 0 ? 'w' : 'b';
                } else {
                    s += ".";
                }
            }
            player.sendInputLine(s);
        }

        // Last action
        player.sendInputLine(lastAction);
        player.sendInputLine(Integer.toString(actions));
    }

    @Override
    public void onEnd() {
        int[] scores = { gameManager.getPlayer(0).getScore(), gameManager.getPlayer(1).getScore() };
        String[] text = new String[2];
        if(scores[0] > scores[1]) {
            text[0] = "Won";
            text[1] = "Lost";
            gameManager.addTooltip(gameManager.getPlayer(0), gameManager.getPlayer(0).getNicknameToken() + " won the match");
        } else if(scores[1] > scores[0]) {
            text[0] = "Lost";
            text[1] = "Won";
            gameManager.addTooltip(gameManager.getPlayer(1), gameManager.getPlayer(1).getNicknameToken() + " won the match");
        } else {
            text[0] = "Draw";
            text[1] = "Draw";
        }
        endScreenModule.setScores(scores, text);
    }
}
