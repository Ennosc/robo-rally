package model.game.cards;

import model.game.Game;
import model.game.board.robots.Robot;


/**
 * Enumeration of programming card types.
 * <p>
 * Each programming card type defines its effect on a robot when played.
 * </p>
 */
public enum ProgrammingCardType implements CardType{
    MOVE_1("MoveI") {
        @Override
        public void applyEffect(Robot robot) {
        robot.moveRobot(1);
        }
    },
    MOVE_2("MoveII") {
        @Override
        public void applyEffect(Robot robot) {
            robot.moveRobot(2);
        }
    },
    MOVE_3("MoveIII") {
        @Override
        public void applyEffect(Robot robot) {
            robot.moveRobot(3);
        }
    },
    TURN_RIGHT("TurnRight") {
        @Override
        public void applyEffect(Robot robot) {
            robot.rotateRobot("clockwise");
        }
    },
    TURN_LEFT("TurnLeft") {
        @Override
        public void applyEffect(Robot robot) {
            robot.rotateRobot("counterclockwise");
        }
    },
    BACK_UP("BackUp") {
        @Override
        public void applyEffect(Robot robot) {
            robot.moveRobot(-1);
        }
    },
    U_TURN("UTurn") {
        @Override
        public void applyEffect(Robot robot) {
            robot.uTurn();
        }
    },
    POWER_UP("PowerUp") {
        @Override
        public void applyEffect(Robot robot) {
            if(robot.getPlayer()!=null){
                robot.getPlayer().addEnergyCubes(1);
                Game.getInstance().notifyEnergyValues(robot.getPlayer(), "PowerUp");
            }
        }
    },
    AGAIN("Again") {
        @Override
        public void applyEffect(Robot robot) {
        }
    };

    private final String name;

    ProgrammingCardType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
