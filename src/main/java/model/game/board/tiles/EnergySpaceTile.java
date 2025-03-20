package model.game.board.tiles;

import model.game.Game;
import model.game.board.robots.Robot;

/**
 * Represents an energy space tile.
 */
public class EnergySpaceTile extends Tile {
    private int count;
    Game game = Game.getInstance();

    /**
     * Constructs an EnergySpaceTile with the specified initial energy count.
     *
     * @param isOnBoard         a flag indicating whether the tile is on the board.
     * @param energyCubesLeft the initial number of energy cubes available on the tile.
     */
    public EnergySpaceTile(String isOnBoard, int energyCubesLeft) {
        super("EnergySpace",isOnBoard);
        this.count = energyCubesLeft;
    }

    /**
     * Activates the energy space tile.
     * <p>
     * The tile adds one energy cube to the robot's player, decrements its count, and notifies the game.
     * </p>
     *
     * @param robot the robot activating the tile.
     */
    @Override
    public void activate(Robot robot) {
        if(count>=1){
           robot.getPlayer().addEnergyCubes(1);
            count--;
            int playerEnergyCubes = robot.getPlayer().getEnergyCube();
            game.notifyEnergyValues(robot.getPlayer(), "EnergySpace");
            game.notifyAnimation("EnergySpace");
        }
    }

    public int getCount() {
        return count;
    }
}
