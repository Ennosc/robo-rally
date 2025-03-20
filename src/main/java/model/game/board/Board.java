package model.game.board;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import model.game.Game;
import model.game.Player;
import model.game.board.robots.Robot;
import model.game.board.tiles.*;
import model.game.cards.DamageCardType;
import model.game.cards.UpgradeCardType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents the game board. The board holds a 3D structure of tiles, tracks robot positions,
 * and provides methods to move robots and activate tile effects (e.g. conveyors, push panels, gears, lasers).
 */
public class Board {

    private final int rows;
    private final int cols;
    private final ConcurrentHashMap<Robot, int[]> robotPositions;
    List<List<List<Tile>>> map; // Organized as rows -> columns -> list of tiles

    private AntennaTile antenna;
    private final List<ConveyorBeltTile> greenConveyorBelts;
    private final List<ConveyorBeltTile> blueConveyorBelts;
    private final List<PushPanelTile> pushPanels;
    private final List<GearTile> gears;
    private final List<EnergySpaceTile> energySpaces;
    private final List<CheckpointTile> checkpoints;
    private final List<Position> laserStartingPositions;
    private Logger logger;

    /**
     * Constructs a board with the specified number of rows and columns.
     *
     * @param rows the number of rows on the board
     * @param cols the number of columns on the board
     */
    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.robotPositions = new ConcurrentHashMap<>();
        this.map = new ArrayList<>();
        this.laserStartingPositions = new ArrayList<>();

        for (int x = 0; x < rows; x++) {
            List<List<Tile>> row = new ArrayList<>();
            for (int y = 0; y < cols; y++) {
                row.add(new ArrayList<>());
            }
            map.add(row);
        }

        this.greenConveyorBelts = new ArrayList<>();
        this.blueConveyorBelts = new ArrayList<>();
        this.pushPanels = new ArrayList<>();
        this.gears = new ArrayList<>();
        this.energySpaces = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
    }

    public void setLogger(Logger log) {
        this.logger = log;
    }

    /**
     * Initializes the board’s map and populates internal lists for special tiles.
     *
     * @param map the board’s map organized as rows, columns, and lists of tiles
     */
    public void initializeBoard(List<List<List<Tile>>> map) {
        this.map = map;
        populateTiles();
    }

    /**
     * Moves the given robot in the specified direction.
     * Checks boundaries, pits, antennas, walls, and other robots before updating its position.
     * If all checks pass, the robot is placed in the new cell and a movement notification is sent.
     *
     * @param robot the robot to move
     * @param directionOfMovement the direction in which to move the robot
     */
    public void moveRobot(Robot robot, Direction directionOfMovement) {
        if (robot == null) {
            logger.warning("Attempted to move a null robot!");
            return;
        }
        int[] currentPosition = robotPositions.get(robot);
        if (currentPosition == null) {
            logger.warning("No position found for robot " + robot.getPlayer().getName()+ robot.getName());
            return;
        }
        int currentRow = currentPosition[0];
        int currentCol = currentPosition[1];
        if(robot.getPlayer()!=null){
            logger.info(robot.getName() + " " + robot.getPlayer().getName()+" players robot is on: " +  "row " + currentRow+ " " +
                    "col" + currentCol);

        }

        int[] newPosition = calculateNewCoordinates(currentPosition[0], currentPosition[1], directionOfMovement);
        int newRow = newPosition[0];
        int newCol = newPosition[1];


        boolean noWallHere     = checkWallCurrentTile(currentRow, currentCol, directionOfMovement);
        boolean inBounds       = checkBoundaries(robot, newRow, newCol, currentRow, currentCol);
        boolean noWallNext     = checkWallNextTile(newRow, newCol, directionOfMovement);
        boolean noPit          = checkPit(robot, newRow, newCol);
        boolean noAntenna      = checkAntenna(robot, newRow, newCol);
        boolean noOtherRobot   = checkForAnotherRobot(robot, newRow, newCol, directionOfMovement);


        if (noWallHere && inBounds && noWallNext && noPit && noAntenna && noOtherRobot) {
            placeRobot(robot, newRow, newCol);
            if(robot.getPlayer()!=null){
                logger.info(String.format(
                        "%s%d (%s) successfully moved to (row=%d, col=%d).",
                        robot.getName(),
                        robot.getId(),
                        robot.getPlayer().getName(),
                        newRow,
                        newCol
                ));
            }
        } else {
            if(robot.getPlayer()!=null){
                logger.info(String.format(
                        "%s%d NOT moved. Checks: wallCurrent=%b, boundaries=%b, wallNext=%b, pit=%b, antenna=%b, otherRobot=%b",
                        robot.getName(),
                        robot.getId(),
                        noWallHere,
                        inBounds,
                        noWallNext,
                        noPit,
                        noAntenna,
                        noOtherRobot
                ));
            }
        }
    }


    private void populateTiles(){
        for(int row = 0; row <rows; row++){
            for(int col =0;col<cols;col++){
                for (Tile tile : map.get(row).get(col)){
                    if(tile instanceof ConveyorBeltTile){
                        ConveyorBeltTile conveyorBelt = (ConveyorBeltTile)  tile;
                        if(conveyorBelt.getSpeed()==1){
                            greenConveyorBelts.add(conveyorBelt);
                        } else if(conveyorBelt.getSpeed() == 2) {
                            blueConveyorBelts.add(conveyorBelt);
                        }
                    } else if (tile instanceof PushPanelTile) {
                        pushPanels.add((PushPanelTile) tile);
                    } else if (tile instanceof GearTile) {
                        gears.add((GearTile) tile);
                    } else if (tile instanceof EnergySpaceTile) {
                        energySpaces.add((EnergySpaceTile) tile);
                    } else if (tile instanceof CheckpointTile) {
                        checkpoints.add((CheckpointTile) tile);
                    } else if (tile instanceof AntennaTile) {
                        antenna = (AntennaTile) tile;
                    } else if (tile instanceof LaserTile) {
                        LaserTile laser = (LaserTile) tile;
                        if (laser.isStartingLaser()) {
                            laserStartingPositions.add(new Position(row, col));
                        }
                    }
                }
            }
        }
    }


    /**
     * Calculates the new board coordinates when moving from a given cell in a specified direction.
     *
     * @param currentRow the current row index
     * @param currentCol the current column index
     * @param directionOfMovement the direction to move
     * @return an array where index 0 is the new row and index 1 is the new column
     */
    public int[] calculateNewCoordinates(int currentRow, int currentCol, Direction directionOfMovement) {
        int rowChange = 0, colChange = 0;

        switch (directionOfMovement) {
            case TOP:
                colChange =  -1;
                break;
            case RIGHT:
                rowChange = 1;
                break;
            case BOTTOM:
                colChange = 1;
                break;
            case LEFT:
                rowChange = -1;
                break;
        }
        return new int[]{currentRow + rowChange, currentCol + colChange};
    }

    /**
     * Checks whether the new coordinates are within the board boundaries.
     * If not, the robot is rebooted.
     *
     * @param robot the robot being moved
     * @param newRow the proposed new row
     * @param newCol the proposed new column
     * @param currentRow the robots current row
     * @param currentCol the robots current column
     * @return true if the new coordinates are within bounds; false otherwise
     */
    private boolean checkBoundaries(Robot robot, int newRow, int newCol,int currentRow, int currentCol) {
        if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols) {
            //logger.info("Robot fell of the board!");
            String isOnBoard = map.get(currentRow).get(currentCol).getFirst().getIsOnBoard();
            prepareReboot(robot, isOnBoard);
            return false;
        }
        return true;
    }

    /**
     * Checks if the destination cell contains a pit.
     * If so, the robot is rebooted.
     *
     * @param robot the robot being moved
     * @param newRow the destination row
     * @param newCol the destination column
     * @return true if no pit is present; false if a pit is found
     */
    private boolean checkPit(Robot robot, int newRow, int newCol) {
        if (!isWithinBounds(newRow, newCol)) {
            if (robot.getPlayer() != null) {
                logger.info("out of bound " + newRow + " new row " + newCol + " newCol");
            }
            return true;
        }

        for (Tile tile : map.get(newRow).get(newCol)) {
            if (tile instanceof PitTile) {
                if (robot.getPlayer() != null) {
                    logger.info("Robot fell into a pit!");
                }
                String isOnBoard = map.get(newRow).get(newCol).getFirst().getIsOnBoard();
                prepareReboot(robot, isOnBoard);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the robot's new position contains an antenna.
     *
     * <p>This method iterates through all tiles at the robot's proposed new position
     * ({@code newRow}, {@code newCol}) to determine if it contains an {@code AntennaTile}.
     * @param newRow the proposed new row position for the robot
     * @param newCol the proposed new column position for the robot
     * @return {@code true} if the new position does not contain an antenna; {@code false} otherwise
     */
    private boolean checkAntenna(Robot robot, int newRow, int newCol) {
        if (!isWithinBounds(newRow, newCol)) {
            if (robot.getPlayer() != null) {
                //logger.info("out of bound " + newRow + " new row " + newCol + " newCol");
            }
            return true;
        }

        for (Tile tile : map.get(newRow).get(newCol)) {
            if (tile instanceof AntennaTile) {
                //logger.info("Robot blocked by an antenna!");
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the current tile for a wall blocking movement in the given direction.
     *
     * @param currentRow the current row of the robot
     * @param currentCol the current column of the robot
     * @param directionOfMovement the intended movement direction
     * @return true if no wall blocks movement; false otherwise
     */
    private boolean checkWallCurrentTile(int currentRow, int currentCol, Direction directionOfMovement) {
        for (Tile tile : map.get(currentRow).get(currentCol)) {
            if (tile instanceof WallTile && ((WallTile) tile).getWall(directionOfMovement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the destination tile for a wall that would block entry (using the inverted direction).
     *
     * @param newRow the destination row
     * @param newCol the destination column
     * @param directionOfMovement the intended movement direction
     * @return true if no blocking wall is found; false otherwise
     */
    private boolean checkWallNextTile(int newRow, int newCol, Direction directionOfMovement) {

        if (!isWithinBounds(newRow, newCol)) {
            //logger.info("out of bound " + newRow + " new row " + newCol + " newCol");
            return true;
        }

        Direction invertedDirection = directionOfMovement.invert();

        for (Tile tile : map.get(newRow).get(newCol)) {
            if (tile instanceof WallTile && ((WallTile) tile).getWall(invertedDirection)) {
                //logger.info("Robot blocked by a wall on the next tile!");
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether another robot occupies the destination cell.
     * If so, attempts to push that robot.
     *
     * @param movingRobot the robot trying to move
     * @param newRow the destination row
     * @param newCol the destination column
     * @param directionOfMovement the movement direction
     * @return true if the destination is free or push succeeds; false if blocked
     */
    private boolean checkForAnotherRobot(Robot movingRobot, int newRow, int newCol, Direction directionOfMovement) {
        if (!isWithinBounds(newRow, newCol)) {
            if (movingRobot.getPlayer() != null) {
                logger.info("out of bound " + newRow + " new row " + newCol + " newCol");
            }
            return true;
        }

        for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
            Robot otherRobot = entry.getKey();
            int[] otherPosition = entry.getValue();

            if(movingRobot == otherRobot){
                continue;
            }
            if (otherPosition[0] == newRow && otherPosition[1] == newCol) {
                logger.info(movingRobot.getPlayer().getName() + " " + movingRobot.getName() + " Robot pushes another robot!");

                // Try to push the other robot
                if (!attemptPush(otherRobot, directionOfMovement)) {
                    if (movingRobot.getPlayer() != null) {
                        logger.info("Push failed! Robot stays in place.");
                    }

                    return false; // Abort the move if push fails
                }
                return true; // Push succeeded, continue the move
            }
        }
        return true;

    }

    /**
     * Attempts to push the specified robot in the given direction.
     * Recursively pushes any blocking robots.
     *
     * @param robot the robot to push
     * @param directionOfMovement the direction to push the robot
     * @return true if the push succeeds; false otherwise
     */
    private boolean attemptPush(Robot robot, Direction directionOfMovement) {

        int[] currentPosition = robotPositions.get(robot);
        int currentRow = currentPosition[0];
        int currentCol = currentPosition[1];

        int[] newPosition = calculateNewCoordinates(currentPosition[0], currentPosition[1], directionOfMovement);
        int newRow = newPosition[0];
        int newCol = newPosition[1];


        // Check for walls on the current and next tiles
        if (!checkWallCurrentTile(currentRow, currentCol, directionOfMovement) || !checkWallNextTile(newRow, newCol, directionOfMovement)) {
            return false; //push fails
        }
        // Check for another robot at the new position
        for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
            int[] otherPosition = entry.getValue();
            if (otherPosition[0] == newRow && otherPosition[1] == newCol) {
                Robot otherRobot = entry.getKey();
                if (robot.getPlayer() != null) {
                    logger.info("Pushed another robot encounters another robot! Attempting to push...");
                }

                if (!attemptPush(otherRobot, directionOfMovement)) {
                    if (robot.getPlayer() != null) {
                        logger.info("Chain push failed! Robot cannot be pushed.");
                    }
                    return false; // Push fails if the chain fails
                }
                break; // Chain push succeeded, continue
            }
        }
        // Push the robot to the new position
        moveRobot(robot,directionOfMovement);
        // Maybe a boolean to mark being pushed?
        if (robot.getPlayer() != null) {
            logger.info("Pushed robot moved to (" + newRow + ", " + newCol + ")");
        }
        return true; // Push succeeds
    }

    /**
     * Activates the effects of all board tiles.
     *
     * @param currentRegister the current register
     */
    public void activateTiles(int currentRegister) {
        PauseTransition pt1 = new PauseTransition(Duration.seconds(0.2));
        pt1.setOnFinished(_ -> {
            logger.info("Activating blue and then green conveyors...");
            moveCheckpointsOnConveyorBelts();
            activateConveyorBelts();

            PauseTransition pt2 = new PauseTransition(Duration.seconds(0.4));
            pt2.setOnFinished(_ -> {
                logger.info("Activating push panels...");
                activatePushPanels(currentRegister);

                PauseTransition pt3 = new PauseTransition(Duration.seconds(0.4));
                pt3.setOnFinished(_ -> {
                    logger.info("Activating gears...");
                    activateGears();

                    PauseTransition pt4 = new PauseTransition(Duration.seconds(1));
                    pt4.setOnFinished(_ -> {
                        logger.info("Activating lasers...");
                        activateLasers();
                        logger.info("Post lasers");

                        PauseTransition pt5 = new PauseTransition(Duration.seconds(1));
                        pt5.setOnFinished(_ -> {
                            logger.info("Activating robot lasers...");
                            activateRobotLasers();

                            PauseTransition pt6 = new PauseTransition(Duration.seconds(0.5));
                            pt6.setOnFinished(_ -> {
                                logger.info("Activating energy spaces...");
                                activateEnergySpaces();

                                PauseTransition pt7 = new PauseTransition(Duration.seconds(0.5));
                                pt7.setOnFinished(_ -> {
                                    logger.info("Checking checkpoints...");
                                    activateCheckpoints();
                                });
                                pt7.play();
                            });
                            pt6.play();
                        });
                        pt5.play();
                    });
                    pt4.play();
                });
                pt3.play();
            });
            pt2.play();
        });
        pt1.play();
    }

    /**
     * Activates all gear tiles on the game board.
     */
    public void activateGears() {
        for (GearTile gear : gears) {
            Robot robot = getRobotOnTile(gear);
            if (robot != null) {
                gear.activate(robot);
            }
        }
    }

    /**
     * Activates all push panel tiles for the given register phase.
     * For each push panel with a robot, the panel’s push effect is triggered.
     *
     * @param currentRegister the register phase during which push panels activate
     */
    public void activatePushPanels(int currentRegister) {
        for (PushPanelTile pushPanel : pushPanels) {
            Robot robot = getRobotOnTile(pushPanel);
            if (robot != null) {
                pushPanel.activatePush(robot, currentRegister, this);
            }
        }
    }

    /**
     * Activates wall lasers. Fires a laser from each starting position in its set direction.
     */
    public void activateLasers() {
        Game.getInstance().notifyAnimation("WallShooting");
        for (Position pos : laserStartingPositions) {
            int row = pos.x();
            int col = pos.y();

            List<Tile> cellTiles = map.get(row).get(col);
            for (Tile tile : cellTiles) {
                if (tile instanceof LaserTile) {
                    LaserTile laser = (LaserTile) tile;
                    if (laser.isStartingLaser()) {
                        fireLaser(row, col, laser.getDirection(), laser.getDamage());
                        break;
                    }
                }
            }
        }
    }


    /**
     * Activates lasers fired by robots.
     * For each robot, fires a laser in its facing direction (and rear laser if applicable).
     */
    public void activateRobotLasers() {
        Game.getInstance().notifyAnimation("PlayerShooting");
        for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
            Robot robot = entry.getKey();
            int[] position = entry.getValue();
            int[] nextPosition = calculateNewCoordinates(position[0], position[1],robot.getDirection());
            if (robot.getIsRebooting()) {
                logger.info("Robot is rebooting. Can't fire laser.");
                continue; // Stop the laser
            }
            if (hasBlockingWall(position[0], position[1],robot.getDirection())) {
                logger.info("Laser blocked by wall on current tile at (" + position[0] + ", " + position[1] + ")");
                continue; // Stop the laser
            }

            boolean hasRearLaser = false;
            for (UpgradeCardType card : robot.getPlayer().getUpgradeCards()) {
                if (card == UpgradeCardType.REAR_LASER) {
                    hasRearLaser = true;
                }
            }
            if(hasRearLaser){
                int[] nextPositionRearLaser = calculateNewCoordinates(position[0], position[1],robot.getDirection().invert());
                fireLaser(nextPositionRearLaser[0], nextPositionRearLaser[1], robot.getDirection().invert(), 1);
            }
            fireLaser(nextPosition[0], nextPosition[1], robot.getDirection(), 1);
        }
    }

    /**
     * Fires a laser from the given starting coordinates in the specified direction.
     * The laser shoots until it hits a robot, is blocked by a wall, or goes out of bounds.
     * When a robot is hit, it takes the specified damage.
     *
     * @param startRow the row where the laser starts
     * @param startCol the column where the laser starts
     * @param direction the direction the laser travels
     * @param damage the damage to apply upon hitting a robot
     */
    private void fireLaser(int startRow, int startCol, Direction direction, int damage) {
        int row = startRow;
        int col = startCol;

        while (isWithinBounds(row, col)) {
            // Get all tiles at the current position
            List<Tile> tilesAtPosition = map.get(row).get(col);
            // Check for a robot on any of the tiles at this position
            for (Tile tile : tilesAtPosition) {
                Robot robot = getRobotOnTile(tile);
                if (robot != null) {
                    Player damagedPlayer = Game.getInstance().getPlayerByRobot(robot);
                    Game.getInstance().drawDamageCard(damagedPlayer, DamageCardType.SPAM, damage);
                    logger.info("Laser hit robot at (" + row + ", " + col + ")" + " " + robot.getName()  + " "  +  robot.getPlayer().getName() + " " + damagedPlayer.getName());
                    return; // Stop the laser
                }
            }
            // Check for a wall blocking the laser on the current tile
            if (hasBlockingWall(row, col, direction)) {
                logger.info("Laser blocked by wall on current tile at (" + row + ", " + col + ")");
                return; // Stop the laser
            }
            switch (direction) {
                case TOP:
                    col--;
                    break;
                case BOTTOM:
                    col++;
                    break;
                case LEFT:
                    row--;
                    break;
                case RIGHT:
                    row++;
                    break;
            }
            // Check for a wall blocking the laser from the next tile
            Direction invertedDirection = direction.invert();
            if (isWithinBounds(row, col) && hasBlockingWall(row, col, invertedDirection)) {
                logger.info("Laser blocked by wall on next tile at (" + row + ", " + col + ")");
                return; // Stop the laser
            }
        }
    }

    /**
     * Checks whether the given row and column are within the boards boundaries.
     *
     * @param row the row index to check
     * @param col the column index to check
     * @return true if the position is within bounds; false otherwise
     */
    public boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * Checks if there is a wall blocking movement or laser fire in the specified direction on a tile.
     * <p>
     * This method retrieves all tiles at the specified position ({@code row}, {@code col}) and performs the following steps:</p>
     * <ul>
     *     <li>Iterates through the list of tiles at the given position.</li>
     *     <li>Checks if any tile is an instance of {@code WallTile}.</li>
     *     <li>If the tile is a {@code WallTile}, calls {@code getWall(direction)} to determine if a wall blocks the specified direction.</li>
     * </ul>
     * <p>The method returns {@code true} if a blocking wall is found; otherwise, it returns {@code false}.</p>
     *
     * @param row the row position of the tile being checked
     * @param col the column position of the tile being checked
     * @param direction the direction to check for a blocking wall (e.g., {@code "up"}, {@code "down"}, {@code "left"}, {@code "right"})
     * @return {@code true} if a wall blocks the specified direction, {@code false} otherwise
     */
    private boolean hasBlockingWall(int row, int col, Direction direction) {
        List<Tile> tiles = map.get(row).get(col);
        for (Tile tile : tiles) {
            if (tile instanceof WallTile && ((WallTile) tile).getWall(direction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Activates all energy space tiles on the game board.
     * <p>This method assumes that all energy space tiles are stored in the list {@code energySpaces}.</p>
     * <p>
     * This method iterates through the list of energy space tiles and performs the following steps:</p>
     * <ul>
     *     <li>Checks if a robot is present on the current energy space tile using {@code getRobotOnTile}.</li>
     *     <li>If a robot is found, activates the energy space using {@code energySpace.activate(robot)}.</li>
     * </ul>
     */
    private void activateEnergySpaces() {
        for (EnergySpaceTile energySpace : energySpaces) {
            Robot robot = getRobotOnTile(energySpace);
            if (robot != null) {
                energySpace.activate(robot);
            }
        }
    }

    /**
     * Activates all checkpoint tiles on the game board.
     * <p>This method iterates through the list of checkpoint tiles and performs the following steps:</p>
     * <ul>
     *     <li>Checks if a robot is present on the current checkpoint tile using {@code getRobotOnTile}.</li>
     *     <li>If a robot is found, activates the checkpoint using {@code checkpoint.activate(robot)}.</li>
     * </ul>
     */
    private void activateCheckpoints() {
        for (CheckpointTile checkpoint : checkpoints) {
            Robot robot = getRobotOnTile(checkpoint);
            if (robot != null) {
                checkpoint.activate(robot);
            }
        }
    }

    /**
     * Retrieves the robot located on a specific tile.
     * <p>This method iterates through the {@code robotPositions} map and performs the following steps:</p>
     * <ul>
     *     <li>Retrieves the position of each robot as an array of coordinates ({@code int[]}).</li>
     *     <li>Gets the list of tiles at the robot's current position from the {@code map}.</li>
     *     <li>Checks if the specified {@code tile} is present in the list of tiles at the robot's position.</li>
     *     <li>If a match is found, returns the corresponding {@code Robot}.</li>
     * </ul>
     * <p>If no robot is found on the specified tile, the method returns {@code null}.</p>
     *
     * @param tile the tile to check for a robot
     * @return the {@code Robot} located on the specified tile, or {@code null} if no robot is found
     */
    private Robot getRobotOnTile(Tile tile) {
        if(tile == null){
            logger.info("ERROR Tile is null in getRobotOnTile()");
            return null;
        }
        for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
            int[] position = entry.getValue();
            List<Tile> tilesAtPosition = map.get(position[0]).get(position[1]);
            if (tilesAtPosition.contains(tile)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Updates the position of the given {@code robot} on the game board.
     * Replaces the robot's current position with the specified {@code row} and {@code col}.
     *
     * @param robot the robot to be placed
     * @param row the row index of the new position
     * @param col the column index of the new position
     */
    public void placeRobot(Robot robot, int row, int col) {
        if (robot.getPlayer() != null) {
            logger.info("Robot position updated to " + row + " " + col);
        }

        robotPositions.put(robot, new int[]{row, col});
        if (robot.getPlayer() != null) {
            Game.getInstance().notifyMovement(robot.getPlayer().getPlayerId(), row, col);
        }

    }

    /**
     * Determines which player has the highest priority based on their robot's distance
     * and angle relative to the antenna.
     *
     * @return a list of players sorted by priority (closest and best aligned come first)
     */
    public List<Player> determinePriority() {
        logger.info("board/determinepriority");
        int[] antennaPosition = new int[]{}; // Find the antenna's position
        Direction antennaDirection = null; // Get the antenna's direction
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                for (Tile tile : map.get(row).get(col)) {
                    if (tile instanceof AntennaTile) {
                        antennaPosition = new int[]{row, col};
                        antennaDirection = ((AntennaTile) tile).getDirection();
                        logger.info("AntennaDirection: " + antennaDirection);
                        logger.info("AntennaPosition: " + Arrays.toString(antennaPosition));
                        break;
                    }
                }
            }
        }
        // Create a list of robots with their positions
        logger.info("board/RobotPositions: "+ robotPositions);
        List<Map.Entry<Robot, int[]>> robotsWithDistances = new ArrayList<>(robotPositions.entrySet());
        // Sort robots by distance and angle
        for (int i = 0; i < robotsWithDistances.size(); i++) {
            for (int j = i + 1; j < robotsWithDistances.size(); j++) {
                Map.Entry<Robot, int[]> entry1 = robotsWithDistances.get(i);
                Map.Entry<Robot, int[]> entry2 = robotsWithDistances.get(j);

                int distance1 = calculateDistance(entry1.getValue(), antennaPosition);
                int distance2 = calculateDistance(entry2.getValue(), antennaPosition);

                if (distance1 > distance2 || (distance1 == distance2 &&
                        calculateAngle(entry1.getValue(), antennaPosition, antennaDirection) >
                                calculateAngle(entry2.getValue(), antennaPosition, antennaDirection))) {
                    // Swap positions if entry2 has higher priority than entry1
                    Map.Entry<Robot, int[]> temp = robotsWithDistances.get(i);
                    robotsWithDistances.set(i, robotsWithDistances.get(j));
                    robotsWithDistances.set(j, temp);
                }
            }
        }
        // Extract the sorted robots into a list of players
        List<Player> playerPriority = new ArrayList<>();
        for (Map.Entry<Robot, int[]> entry : robotsWithDistances) {
            Player player = entry.getKey().getPlayer();
            logger.info(player + " " + player.getName());
            playerPriority.add(player);
        }
        logger.info("board/playerPriority: "+ playerPriority);
        return playerPriority;
    }

    /**
     * Calculates the Manhattan distance between two positions on the game board.
     * <p>Manhattan distance is the sum of the absolute differences of their row and column indices.</p>
     * @param pos1 The first position as an array where {@code pos1[0]} is the row and {@code pos1[1]} is the column.
     * @param pos2 The second position as an array where {@code pos2[0]} is the row and {@code pos2[1]} is the column.
     * @return The Manhattan distance between {@code pos1} and {@code pos2}.
     */
    public int calculateDistance(int[] pos1, int[] pos2) {
        return Math.abs(pos1[0] - pos2[0]) + Math.abs(pos1[1] - pos2[1]);
    }

    /**
     * Calculates the adjusted angle between a robot's position and the antenna's position and direction.
     * <p>The angle is computed in radians, adjusted based on the antenna's outflow direction, and normalized to the range [0, 2π].</p>
     * @param robotPosition    The robot's current position as an array where {@code robotPosition[0]} is the row and {@code robotPosition[1]} is the column.
     * @param antennaPosition  The antenna's position as an array where {@code antennaPosition[0]} is the row and {@code antennaPosition[1]} is the column.
     * @param antennaDirection The direction in which the antenna is oriented (e.g., "up", "right", "down", "left").
     * @return The adjusted angle in radians between the robot and the antenna, normalized to the range [0, 2π].
     */
    private double calculateAngle(int[] robotPosition, int[] antennaPosition, Direction antennaDirection) {
        int dx = robotPosition[1] - antennaPosition[1]; // Column difference
        int dy = robotPosition[0] - antennaPosition[0]; // Row difference
        double angle = Math.atan2(dy, dx); // Calculate the angle in radians
        // Adjust the angle based on the antenna's starting direction
        double directionOffset = getDirectionOffset(antennaDirection);
        angle -= directionOffset;
        // Normalize the angle to range [0, 2π]
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Retrieves the angular offset in radians corresponding to a given direction.
     * <p>This offset is used to adjust angles based on the antenna's outflow direction.</p>
     * <ul>
     *     <li>{@code "up"}: {@code Math.PI / 2} (90 degrees)</li>
     *     <li>{@code "right"}: {@code 0} (0 degrees)</li>
     *     <li>{@code "down"}: {@code -Math.PI / 2} (-90 degrees)</li>
     *     <li>{@code "left"}: {@code Math.PI} (180 degrees)</li>
     * </ul>
     * @param direction The direction for which to retrieve the angular offset. Valid values are {@code "up"}, {@code "right"}, {@code "down"}, and {@code "left"}.
     * @return The angular offset in radians corresponding to the specified direction.
     * @throws IllegalArgumentException If the provided {@code direction} is not one of the valid options.
     */
    private double getDirectionOffset(Direction direction) {
        return switch (direction) {
            case TOP -> Math.PI / 2; // 90 degrees
            case RIGHT -> 0; // 0 degrees
            case BOTTOM -> -Math.PI / 2; // -90 degrees
            case LEFT -> Math.PI; // 180 degrees
        };
    }

    /**
     * Reboots the robot by locating a suitable RebootTokenTile matching the given board status.
     * If no appropriate tile is found or the tile is blocked, an alternative reboot location is sought.
     *
     * @param robot the robot to reboot
     * @param isOnBoard the board identifier used to match the reboot tile
     */
    public void prepareReboot(Robot robot, String isOnBoard){
        for(int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                for (Tile tile : map.get(row).get(col)) {
                    if (tile instanceof RebootTokenTile) {
                        String currentTileIsOnBoard = tile.getIsOnBoard();
                        if (currentTileIsOnBoard.equals(isOnBoard)) {
                            RebootTokenTile reboot = (RebootTokenTile) tile;
                            Direction direction = Direction.TOP;
                            if (reboot.getDirection() != null) {
                                direction = reboot.getDirection();
                            }
                            if(checkForAnotherRobot(robot, row, col,direction)){// Robots on Reboot Token get pushed off
                                placeRobot(robot, row, col);
                                if(robot.getPlayer()!=null){
                                    Game.getInstance().notifyReboot(robot.getPlayer().getPlayerId());
                                    Player rebootedPlayer = Game.getInstance().getPlayerByRobot(robot);
                                    Game.getInstance().drawDamageCard(rebootedPlayer, DamageCardType.SPAM, 2);
                                }
                                robot.reboot();
                                return;
                            }else{
                                searchForUnblockedReboot(robot);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if(isOnBoard.startsWith("Start")){
            prepareRebootStartPoint(robot);
            return;
        }
        searchForUnblockedReboot(robot);
    }

    /**
     * Searches for an alternative unblocked reboot tile and reboots the robot there.
     *
     * @param robot the robot to reboot
     */
    private void searchForUnblockedReboot(Robot robot){
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                for (Tile tile : map.get(row).get(col)) {
                    if (tile instanceof RebootTokenTile) {
                        RebootTokenTile reboot = (RebootTokenTile) tile;
                        Direction direction = Direction.TOP;
                        if (reboot.getDirection() != null) {
                            direction = reboot.getDirection();
                        }
                        if (checkForAnotherRobot(robot, row, col, direction)) {
                            placeRobot(robot, row, col);
                            if(robot.getPlayer()!=null){
                                Game.getInstance().notifyReboot(robot.getPlayer().getPlayerId());
                                Player rebootedPlayer = Game.getInstance().getPlayerByRobot(robot);
                                Game.getInstance().drawDamageCard(rebootedPlayer, DamageCardType.SPAM, 2);
                            }
                            robot.reboot();
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Reboots the robot at its starting point.
     *
     * @param robot the robot to reboot
     */
    private void prepareRebootStartPoint(Robot robot){
        int [] startingPointPosition = robot.getStartingPosition();
        int startRow = startingPointPosition[0];
        int startCol = startingPointPosition[1];
        if (!checkForAnotherRobot(robot, startRow, startCol, Direction.TOP)) {
            searchForUnblockedReboot(robot);
            return;
        }
        placeRobot(robot, startRow, startCol);
        if (robot.getPlayer() != null) {
            Game.getInstance().notifyReboot(robot.getPlayer().getPlayerId());
            Player rebootedPlayer = Game.getInstance().getPlayerByRobot(robot);
            Game.getInstance().drawDamageCard(rebootedPlayer, DamageCardType.SPAM, 2);
        }
        robot.reboot();
    }

    /**
     * Activates all conveyor belts in two phases (speed 2 then speed 1).
     * Moves robots along the belts and adjusts their orientation when switching directions.
     */
    public void activateConveyorBelts() {
        // Clone current robot positions
        ConcurrentHashMap<Robot, int[]> oldRobotPositions = new ConcurrentHashMap<>(robotPositions);
        Set<Robot> movedRobots = new HashSet<>();
        // Handle blue conveyor belts, then green conveyor belts
        for (int speed = 2; speed >= 1; speed--) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    for (Tile tile : map.get(row).get(col)) {
                        if (tile instanceof ConveyorBeltTile) {
                            ConveyorBeltTile conveyor = (ConveyorBeltTile) tile;
                            if (conveyor.getSpeed() == speed) {
                                Robot robot = getRobotOnTile(conveyor);
                                if (robot == null) {
                                    continue;
                                }
                                // Skip if the robot has already been moved
                                if (movedRobots.contains(robot)) {
                                    if (robot.getPlayer() != null) {
                                    }
                                    continue;
                                }
                                if (greenConveyorBelts.contains(conveyor)) {
                                    if(robot.getPlayer()!=null){
                                    }
                                    moveRobotOnConveyor(robot, conveyor);
                                    if(robot.getPlayer()!=null){
                                        Game.getInstance().notifyAnimation("GreenConveyorBelt");
                                    }
                                } else if (blueConveyorBelts.contains(conveyor)) {
                                    if(robot.getPlayer()!=null){
                                    }
                                    moveRobotOnConveyor(robot, conveyor);
                                    int[] currentPosition = robotPositions.get(robot);
                                    if (currentPosition == null) {
                                        if(robot.getPlayer()!=null){
                                            logger.info("Robot position is null for robot on conveyor at " +
                                                    "(" + row + ", " + col + ")");
                                        }
                                        continue;
                                    }
                                    for (Tile nextTile : map.get(currentPosition[0]).get(currentPosition[1])) {
                                        if (nextTile instanceof ConveyorBeltTile) {
                                            moveRobotOnConveyor(robot, (ConveyorBeltTile) nextTile);
                                            if(robot.getPlayer()!=null){
                                                Game.getInstance().notifyAnimation("BlueConveyorBelt");
                                            }
                                        }
                                    }
                                }
                                movedRobots.add(robot);
                            }
                        }
                    }
                }
            }
            movedRobots.clear();

            // Check if multiple robots occupy the same space
            handleRobotCollisions(oldRobotPositions);
            oldRobotPositions = new ConcurrentHashMap<>(robotPositions);
            for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
                Robot robot = entry.getKey();
                int[] position = entry.getValue();
                placeRobot(robot, position[0], position[1]);
            }
        }
    }


    /**
     * Moves the given robot along the specified conveyor belt tile.
     *
     * @param robot the robot on the conveyor
     * @param conveyor the conveyor belt tile determining movement direction and speed
     */
    private void moveRobotOnConveyor(Robot robot, ConveyorBeltTile conveyor) {
        // Determine the next position based on the conveyor's outflow direction
        if(robot == null){
            return;
        }
        int[] currentPosition = robotPositions.get(robot);
        int currentRow = currentPosition[0];
        int currentCol = currentPosition[1];

        // Calculate the new position for the pushed robot
        int[] newPosition = calculateNewCoordinates(currentPosition[0], currentPosition[1],
                conveyor.getOutflowDirection());
        int newRow = newPosition[0];
        int newCol = newPosition[1];

        if (!checkBoundaries(robot, newRow, newCol, currentRow, currentCol)
            || !checkPit(robot, newRow, newCol)
            || !checkWallCurrentTile(currentRow, currentCol, conveyor.getOutflowDirection())
            || !checkWallNextTile(currentRow, currentCol, conveyor.getOutflowDirection())) {
            return;
        }

        // Move the robot to the next position
        //placeRobot(robot, newRow, newCol);
        robotPositions.put(robot, new int[]{newRow, newCol});

        // Get the tile at the new position
        List<Tile> nextTiles = map.get(newRow).get(newCol);
        // Check for rotation on the conveyor belt
        for (Tile tile : nextTiles) {
            if (tile instanceof ConveyorBeltTile) {
                ConveyorBeltTile nextConveyor = (ConveyorBeltTile) tile;
                // Compare the outflow directions
                Direction fromOutflow = conveyor.getOutflowDirection();
                Direction toOutflow = nextConveyor.getOutflowDirection();
                if (!fromOutflow.equals(toOutflow)) {
                    handleRotationOnConveyor(fromOutflow, toOutflow, robot);
                }
                break;
            }
        }
    }

    /**
     * Rotates a robot when transitioning between conveyor belts with different directions.
     * The robot rotates clockwise if the difference is 1, or counterclockwise if the difference is 3.
     *
     * @param fromOutflow the current conveyor’s direction
     * @param toOutflow the next conveyor’s direction
     * @param robot the robot to rotate
     */
    private void handleRotationOnConveyor(Direction fromOutflow, Direction toOutflow, Robot robot) {
        List<Direction> directions = List.of(Direction.TOP, Direction.RIGHT, Direction.BOTTOM, Direction.LEFT);
        int fromIndex = directions.indexOf(fromOutflow);
        int toIndex = directions.indexOf(toOutflow);

        if (fromIndex == -1 || toIndex == -1) {
            if(robot.getPlayer()!=null){
                logger.warning("Invalid directions provided for rotation.");
            }
            return;
        }
        int diff = (toIndex - fromIndex + 4) % 4;
        if (diff == 1) {
            robot.rotateRobot("clockwise");
        } else if (diff == 3) {
            robot.rotateRobot("counterclockwise");
        }
    }

    /**
     * Checks for collisions between robots. If two robots share the same cell, they are reset
     * to their previous positions.
     *
     * @param oldRobotPositions a map of robots to their previous positions before conveyor movement
     */
    private void handleRobotCollisions(ConcurrentHashMap<Robot, int[]> oldRobotPositions) {
        List<Robot> robots = new ArrayList<>(robotPositions.keySet()); // Get a list of all robots
        // Check for collisions by comparing every pair of robots
        for (int i = 0; i < robots.size(); i++) {
            Robot robot1 = robots.get(i);
            int[] position1 = robotPositions.get(robot1);
            for (int j = i + 1; j < robots.size(); j++) { // Start from i+1 to avoid duplicate comparisons
                Robot robot2 = robots.get(j);
                int[] position2 = robotPositions.get(robot2);
                // Compare positions of the two robots
                if (position1[0] == position2[0] && position1[1] == position2[1]) {
                    // Reset both robots to their old positions
                    robotPositions.put(robot1, oldRobotPositions.get(robot1));
                    robotPositions.put(robot2, oldRobotPositions.get(robot2));
                }
            }
        }
    }

    public int getCheckpoints(){
        return checkpoints.size();
    }


    public String isOnBoard(Robot robot) {
        int[] currentPosition = robotPositions.get(robot);
        int row = currentPosition[0];
        int col = currentPosition[1];
        return map.get(row).get(col).getFirst().getIsOnBoard();
    }

    public List<Robot> getRobotsInRadius(Robot robot) {
        List<Robot> robotsInRadius = new ArrayList<>();

        int[] robotPosition = robotPositions.get(robot);
        int robotRow = robotPosition[0];
        int robotCol = robotPosition[1];

        for (Map.Entry<Robot, int[]> entry : robotPositions.entrySet()) {
            int[] position = entry.getValue();
            int row = position[0];
            int col = position[1];

            int distance = Math.abs(row - robotRow) + Math.abs(col - robotCol);

            if (distance <= 6) {
                robotsInRadius.add(entry.getKey());
            }
        }
        return robotsInRadius;
    }


    public List<List<List<Tile>>> getMap() {
        return map;
    }


    public Position getRobotPositions(Robot robot) {
       return new Position (robotPositions.get(robot)[0], robotPositions.get(robot)[1]);
    }

    public void clearRobotPositions() {
        this.robotPositions.clear();
    }

    public List<Tile> getTilesAtPosition(Position position) {
        if (!isWithinBounds(position.x(), position.y())) {
            return Collections.emptyList();
        }
        return map.get(position.x()).get(position.y());
    }

    public boolean isLaserTileAt(Position position) {
        List<Tile> tiles = getTilesAtPosition(position);
        for (Tile tile : tiles) {
            if (tile instanceof LaserTile) {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckpointTile(Position position) {
        List<Tile> tiles = getTilesAtPosition(position);
        for (Tile tile : tiles) {
            if (tile instanceof CheckpointTile) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnergySpaceTile(Position position) {
        List<Tile> tiles = getTilesAtPosition(position);
        for (Tile tile : tiles) {
            if (tile instanceof EnergySpaceTile) {
                return true;
            }
        }
        return false;
    }

    /**
     * Activates all conveyor belts in two phases.
     * Moves robots along the belts and adjusts their orientation when switching directions.
     */
    public void moveCheckpointsOnConveyorBelts() {
        List<CheckpointTile> movedCheckpoints = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                for (Tile tile : new ArrayList<>(map.get(row).get(col))) {
                    if (tile instanceof CheckpointTile) {
                        CheckpointTile checkpoint = (CheckpointTile) tile;
                        if (movedCheckpoints.contains(checkpoint)) {
                            continue;
                        }
                        ConveyorBeltTile conveyorBelt = null;
                        for (Tile tileConveyor : map.get(row).get(col)) {
                            if (tileConveyor instanceof ConveyorBeltTile) {
                                conveyorBelt = (ConveyorBeltTile) tileConveyor;
                                break;
                            }
                        }
                        if (conveyorBelt == null) {
                            continue;
                        }
                        int speed = conveyorBelt.getSpeed();
                        int[] currentPosition = {row, col};
                        Direction currentDirection = conveyorBelt.getOutflowDirection();
                        for (int i = 0; i < speed; i++) {
                            int[] newPosition = calculateNewCoordinates(currentPosition[0], currentPosition[1], currentDirection);
                            int newRow = newPosition[0];
                            int newCol = newPosition[1];
                            if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols) {
                                break;
                            }
                            // Remove from old position
                            map.get(currentPosition[0]).get(currentPosition[1]).remove(checkpoint);
                            map.get(newRow).get(newCol).add(checkpoint);
                            Game.getInstance().notifyCheckpointMoved(newRow, newCol, checkpoint.getCheckpointNumber());
                            currentPosition[0] = newRow;
                            currentPosition[1] = newCol;
                            ConveyorBeltTile nextConveyor = null;
                            for (Tile tileNext : map.get(newRow).get(newCol)) {
                                if (tileNext instanceof ConveyorBeltTile) {
                                    nextConveyor = (ConveyorBeltTile) tileNext;
                                    break;
                                }
                            }
                            if (nextConveyor != null) {
                                currentDirection = nextConveyor.getOutflowDirection();
                            } else {
                                break;
                            }
                        }
                        movedCheckpoints.add(checkpoint);
                    }
                }
            }
        }
    }
}