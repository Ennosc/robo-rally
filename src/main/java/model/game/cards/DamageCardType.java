package model.game.cards;

import model.game.Game;
import model.game.Player;
import model.game.board.Board;
import model.game.board.robots.Robot;

import java.util.List;

/**
 * Enumeration of damage card types.
 * <p>
 * Each damage card type defines its own effect when applied to a robot.
 * </p>
 */
public enum DamageCardType implements CardType{

    /**
     * Spam damage card.
     * <p>
     * When applied, it draws a new card from the player's programming deck and replaces the card
     * in the same register.
     * </p>
     */
    SPAM("Spam"){
        @Override
        public void applyEffect(Robot robot) {
            Card newCard = robot.getPlayer().getProgrammingDeck().drawCard();
            if (newCard == null) {
                Game.getInstance().resetProgrammingDeck(robot.getPlayer());
                newCard = robot.getPlayer().getProgrammingDeck().drawCard();
            }
            int currentRegister = -1;
            for (int i = 0; i < robot.getPlayer().getRobotMat().getRegisters().size(); i++) {
                Card card = robot.getPlayer().getRobotMat().getRegisters().get(i);
                if (card.type() == this) {
                    currentRegister = i;
                    break;
                }
            }
            robot.getPlayer().getRobotMat().setRegisters(newCard, currentRegister);
            Game.getInstance().notifyReplaceCard(currentRegister, newCard.type().getName(), robot.getPlayer().getPlayerId());
            Game.getInstance().playCard(robot.getPlayer(), newCard.type().getName());
        }
    },

    /**
     * Worm damage card.
     * <p>
     * When applied, it prepares the robot for reboot by using the board's reboot procedure.
     * </p>
     */
    WORM("Worm"){
        @Override
        public void applyEffect(Robot robot) {
            Board board = Game.getInstance().getBoard();
            board.prepareReboot(robot, board.isOnBoard(robot));

        }
    },

    /**
     * Trojan damage card.
     * <p>
     * When applied, it causes the player to draw two Spam damage cards.
     * </p>
     */
    TROJAN("Trojan"){
        @Override
        public void applyEffect(Robot robot) {
            Game.getInstance().drawDamageCard(robot.getPlayer(), SPAM, 2);
        }
    },

    /**
     * Virus damage card.
     * <p>
     * When applied, it causes all robots in the radius of the affected robot to draw one Spam damage card.
     * </p>
     */
    VIRUS("Virus"){
        @Override
        public void applyEffect(Robot robot) {
            List<Robot> damagedRobots = Game.getInstance().getBoard().getRobotsInRadius(robot);
            if (!damagedRobots.isEmpty()) {
                for (Robot damagedRobot : damagedRobots) {
                    Player damagedPlayer = Game.getInstance().getPlayerByRobot(damagedRobot);
                    if (damagedPlayer != null) {
                        Game.getInstance().drawDamageCard(damagedPlayer, SPAM, 1);
                    }
                }
            }
        }
    };


    private final String name;

    DamageCardType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
