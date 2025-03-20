package viewmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import helpers.RobotModel;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import model.game.board.Direction;
import model.game.board.Position;
import model.server_client.BaseClient;
import model.server_client.Client;
import network.interpreters.ClientJsonInterpreter;
import network.messages.lobby3.GameStartedMessage;
import viewmodel.tilesGUI.RobotRenderer;
import viewmodel.tilesGUI.TileImageMapper;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Parses and renders the game map by processing the provided game data and
 * populating the UI with tiles.
 */
public class MapParser {
    private static int checkpointNumber = 0;
    private static Direction restartPointOrientation = Direction.TOP;
    private static Logger logger;

    public static void setLogger(Logger log) {
        logger = log;
    }

    /**
     * Renders the game board by populating the provided GridPane with visual elements
     * based on the gameMap. It adjusts cell sizes to fit within the specified
     * VBox dimensions and overlays robot representations from the client.
     *
     * @param gameMap  The game map parsed from the GameStartedMessage.
     * @param client   The Client instance.
     * @param gridPane The grid pane used to display the map.
     */
    public static void renderGameMap(List<List<List<GameStartedMessage.Field>>> gameMap, Client client, GridPane gridPane) {
        int rows = gameMap.size();
        int columns = gameMap.getFirst().size();

        gridPane.getChildren().clear();

        for (int x = 0; x < rows; x++) {
            List<List<GameStartedMessage.Field>> row = gameMap.get(x);
            for (int y = 0; y < columns; y++) {
                List<GameStartedMessage.Field> cellFields = row.get(y);
                if (cellFields == null) continue;

                // Create one StackPane for this cell
                StackPane cellPane = new StackPane();
                cellPane.setStyle("-fx-background-color: #f0f0f0;");
                for (GameStartedMessage.Field field : cellFields) {
                    if (field == null) continue;

                    if ("Laser".equals(field.getType())) {
                        continue;
                    }

                    if ("Gear".equals(field.getType())) {
                        String imagePath = TileImageMapper.getImagePath("Gear", fieldToJsonObject(field));
                        Image image = loadImage(imagePath);
                        ImageView gearView = new ImageView(image);
                        gearView.setPreserveRatio(true);
                        gearView.setSmooth(true);
                        gearView.getStyleClass().add("gearTile");
                        String gearRotationDirection = field.getOrientations().getFirst();
                        gearView.setUserData(gearRotationDirection);
                        cellPane.getChildren().add(gearView);
                    }


                    // Build the tile’s image path
                    String imagePath = TileImageMapper.getImagePath(field.getType(), fieldToJsonObject(field));
                    Image tileImage = loadImage(imagePath);
                    ImageView tileView = new ImageView(tileImage);
                    tileView.setPreserveRatio(true);
                    tileView.setSmooth(true);
                    if ("PushPanel".equals(field.getType())) {
                        boolean is135 = false;
                        if (field.getRegisters() != null) {
                            for (Integer reg : field.getRegisters()) {
                                if (reg == 1 || reg == 3 || reg == 5) {
                                    is135 = true;
                                    break;
                                }
                            }
                        }
                        String pushPanelID = is135 ? "front--PushPanel135" : "front--PushPanel24";
                        tileView.setId(pushPanelID);
                    } else if ("EnergySpace".equals(field.getType())) {
                        tileView.setId("front--EnergySpace");
                    } else if ("Wall".equals(field.getType()) || "Antenna".equals(field.getType())) {
                        tileView.setId("front--" + field.getType());
                    }

                    if ("CheckPoint".equals(field.getType())) {
                        checkpointNumber++;
                        tileView.setId("checkpoint--");
                    }

                    if (field.getOrientations() != null && !field.getOrientations().isEmpty()) {
                        applyRotation(tileView, Direction.fromString(field.getOrientations().get(0)));
                    }

                    cellPane.getChildren().add(tileView);

                    if ("StartPoint".equals(field.getType())) {
                        Button startButton = new Button("Start");
                        // x,y are the board coordinates
                        int finalX = x;
                        int finalY = y;
                        startButton.setOnAction(_ -> handleStartPointClick(client, finalX, finalY));
                        startButton.setPrefSize(10, 10);
                        startButton.setStyle(
                                "-fx-background-color: transparent;" +
                                "-fx-border-color: transparent;" +
                                "-fx-text-fill: rgba(0,0,0,0);"
                        );
                        cellPane.getChildren().add(startButton);
                    }
                }
                gridPane.add(cellPane, x, y);
            }
        }

        Platform.runLater(() -> bringSpecialTilesToFront(gameMap, gridPane));
        Platform.runLater(() -> renderLasers(gameMap, gridPane));
        //client.getGameDataBridge().setGameMap(gameMap);
    }

    /**
     * Adjusts the sizes of the cells within a GridPane based on the specified
     * number of columns, rows, and the dimensions of the window. Each cell is resized
     * to ensure a uniform appearance, and any ImageView within a StackPane
     * child is adjusted to fit the new cell size.
     *
     * @param gridPane     the GridPane whose cell sizes are to be adjusted
     * @param cols         the number of columns in the grid
     * @param rows         the number of rows in the grid
     * @param windowWidth  the total width of the window
     * @param windowHeight the total height of the window
     */
    public static void adjustCellSizes(GridPane gridPane, int cols, int rows, double windowWidth, double windowHeight) {
        logger.info("Entering adjustCellSizes - Window Width: " + windowWidth + ", Window Height: " + windowHeight);

        double availableWidth = windowWidth * 0.8;
        double availableHeight = windowHeight * 0.8;
        logger.info("Available Width: " + availableWidth + ", Available Height: " + availableHeight);

        double cellWidth = availableWidth / cols;
        double cellHeight = availableHeight / rows;
        logger.info("Calculated cellWidth: " + cellWidth + ", cellHeight: " + cellHeight);

        double cellSize = Math.min(cellWidth, cellHeight);
        logger.info("Final cellSize: " + cellSize);

        for (var node : gridPane.getChildren()) {
            if (node instanceof StackPane cell) {
                for (var child : cell.getChildren()) {
                    if (child instanceof ImageView imageView) {
                        imageView.setFitWidth(cellSize);
                        imageView.setFitHeight(cellSize);
                    }
                }
            }
        }
    }

    /**
     * Renders the game board with robots by placing them onto the specified GridPane.
     * It retrieves robot information from the client and uses the RobotRenderer
     * to display each robot on the board. Additionally, it creates and stores RobotModel
     * instances for each robot in the client's internal map.
     *
     * @param gridPane the GridPane representing the game board where robots will be rendered
     * @param client   the client containing robot data and methods for managing robot models
     */
    public static void renderGameWithRobots(GridPane gridPane, Client client) {
        // Retrieve the mappings

        Map<Integer, Integer> clientIDtoRobotNumber = client.getGameDataBridge().getClientIDToFigure();
        HashMap<String, Integer> robotNameToNumber = client.getRobotNameToNumberMap();

        for (Map.Entry<Integer, Integer> entry : clientIDtoRobotNumber.entrySet()) {
            Integer clientID = entry.getKey();
            Integer robotNumber = entry.getValue();
            // Find the robot name corresponding to the robot number
            String robotName = robotNameToNumber.entrySet().stream()
                                                .filter(e -> e.getValue().equals(robotNumber))
                                                .map(Map.Entry::getKey)
                                                .findFirst()
                                                .orElse("Default");

            RobotModel robotModel = new RobotModel(0, 0, Direction.TOP, robotName);
            client.getGameDataBridge().addRobotModelToMap(clientID, robotModel);
            client.getGameDataBridge().setEnergy(clientID, 5);
            RobotRenderer.renderRobot(gridPane, robotModel);
        }
    }

    /**
     * Loads an image from the given path.
     *
     * @param imagePath The path to the image resource.
     * @return The Image object, or null if loading fails.
     */
    private static Image loadImage(String imagePath) {
        try (InputStream is = ClientJsonInterpreter.class.getResourceAsStream(imagePath)) {
            if (is != null) {
                return new Image(is);
            } else {
                logger.severe("Image not found: " + imagePath);
                return new Image(
                        Objects.requireNonNull(
                                ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Default.png")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Image(Objects.requireNonNull(
                    ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Default.png")));
        }
    }

    /**
     * Rotates the ImageView based on the specified orientation.
     *
     * @param imageView   The ImageView to rotate.
     * @param orientation The orientation string (e.g., "top", "right", "bottom", "left").
     */
    private static void applyRotation(ImageView imageView, Direction orientation) {
        switch (orientation) {
            case TOP:
                imageView.setRotate(0); // No rotation needed
                break;
            case RIGHT:
                imageView.setRotate(90);
                break;
            case BOTTOM:
                imageView.setRotate(180);
                break;
            case LEFT:
                imageView.setRotate(270);
                break;
            default:
                break; // No rotation for unrecognized orientations
        }
    }

    /**
     * Updates the image of an Energy-Space tile when a player lands on it.
     *
     * @param gridPane The GridPane representing the game board.
     * @param x        The x-coordinate of the tile.
     * @param y        The y-coordinate of the tile.
     */
    public static void updateEnergySpace(GridPane gridPane, int x, int y) {
        Image newImage = new Image(Objects.requireNonNull(MapParser.class.getResourceAsStream(
                "/images/general/tiles/Energy-Space0.png")));
        // Find the StackPane at the specified coordinates
        StackPane cellPane = getCellPaneAt(gridPane, y, x);
        if (cellPane != null) {
            for (Node node : cellPane.getChildren()) {
                if (node instanceof ImageView imageView) {
                    if ("front--EnergySpace".equals(imageView.getId())) {
                        imageView.setImage(newImage);
                        imageView.setId("EnergySpaceUsed");
                        break;
                    }
                }
            }
        }
    }

    /**
     * Helper method to get the StackPane at a specific GridPane coordinate.
     */
    private static StackPane getCellPaneAt(GridPane gridPane, int row, int col) {
        for (Node node : gridPane.getChildren()) {
            Integer nodeRow = GridPane.getRowIndex(node);
            Integer nodeCol = GridPane.getColumnIndex(node);
            if (nodeRow == null) nodeRow = 0;
            if (nodeCol == null) nodeCol = 0;
            if (nodeRow == row && nodeCol == col) {
                return (StackPane) node;
            }
        }
        return null;
    }

    /**
     * Converts a Field object to its corresponding JsonObject.
     *
     * @param field The Field object.
     * @return The JsonObject representing the field.
     */
    private static JsonObject fieldToJsonObject(GameStartedMessage.Field field) {
        JsonObject json = new JsonObject();
        json.addProperty("type", field.getType());
        json.addProperty("isOnBoard", field.getIsOnBoard());

        switch (field.getType()) {
            case "ConveyorBelt":
                if (field.getSpeed() != null) {
                    json.addProperty("speed", field.getSpeed());
                }
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "PushPanel":
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }

                if (field.getRegisters() != null) {
                    JsonArray registersArray = new JsonArray();
                    for (Integer register : field.getRegisters()) {
                        registersArray.add(register);
                    }
                    json.add("registers", registersArray);
                }
                break;
            case "Gear":
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "Wall":
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "Laser":
                if (field.getCount() != null) {
                    json.addProperty("count", field.getCount());
                }

                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "Pit":
                break;
            case "Antenna":
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "EnergySpace":
                if (field.getCount() != null) {
                    json.addProperty("count", field.getCount());
                }
                break;
            case "CheckPoint":
                if (field.getCount() != null) {
                    json.addProperty("count", field.getCount());
                }
                break;
            case "RestartPoint":
                if (field.getOrientations() != null) {
                    JsonArray orientationsArray = new JsonArray();
                    for (String orientation : field.getOrientations()) {
                        orientationsArray.add(orientation);
                        restartPointOrientation = Direction.fromString(field.getOrientations().getFirst());
                        logger.info("RestartPoint orientation: " + restartPointOrientation);
                    }
                    json.add("orientations", orientationsArray);
                }
                break;
            case "StartPoint":
                break;
            default:
                break;
        }
        return json;
    }

    private static void handleStartPointClick(Client client, int x, int y) {
        logger.info("Starting point selected at: (" + x + ", " + y + ")");
        client.sendSetStartingPoint(x, y); // Notify server of the selected starting point
    }

    /**
     * Removes a starting point button from the specified coordinates on the game board.
     * If the provided client ID matches the current client, all buttons on the board
     * will be disabled.
     *
     * @param takenX   The x-coordinate of the starting point to be removed.
     * @param takenY   The y-coordinate of the starting point to be removed.
     * @param client   The client instance that holds the game board reference.
     * @param clientID The ID of the client.
     */
    public static void removeStartingPoint(int takenX, int takenY, BaseClient client, int clientID) {
        // Locate the cell at the given coordinates+
        GridPane gameBoard = client.getGameDataBridge().getGameMapGridPane();
        for (Node node : gameBoard.getChildren()) {
            if (GridPane.getRowIndex(node) == takenX && GridPane.getColumnIndex(node) == takenY) {
                if (node instanceof StackPane cellPane) {
                    // Remove the button from the StackPane
                    cellPane.getChildren().removeIf(child -> child instanceof Button);
                }
            }
        }

        // Disable all buttons if the clientID matches
        if (clientID == client.getClientID()) {
            for (Node node : gameBoard.getChildren()) {
                if (node instanceof StackPane cellPane) {
                    cellPane.getChildren().forEach(child -> {
                        if (child instanceof Button button) {
                            button.setDisable(true);
                        }
                    });
                }
            }
        }
    }


    private static void renderLasers(List<List<List<GameStartedMessage.Field>>> gameMap, GridPane gridPane) {
        int rows = gameMap.size();
        int columns = gameMap.getFirst().size();

        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < columns; y++) {
                List<GameStartedMessage.Field> cellFields = gameMap.get(x).get(y);
                if (cellFields == null) continue;

                for (GameStartedMessage.Field field : cellFields) {
                    if ("Laser".equals(field.getType())) {
                        // Render laser based on its orientation and position
                        Direction orientation = Direction.fromString(field.getOrientations().getFirst());
                        renderLaser(gridPane, x, y, orientation, gameMap);
                    }
                }
            }
        }
    }

    private static void renderLaser(GridPane gridPane, int startX, int startY,
                                    Direction orientation,
                                    List<List<List<GameStartedMessage.Field>>> gameMap) {
        StackPane firstCellPane;
        List<GameStartedMessage.Field> firstTileFields = gameMap.get(startX).get(startY);
        for (GameStartedMessage.Field field : firstTileFields) {
            firstCellPane = getCellPaneAt(gridPane, startY, startX);
            if ("Wall".equals(field.getType())
                && field.getOrientations() != null
                && field.getOrientations().contains(orientation.toLowercaseString())) {
                return;
            } else if ("Wall".equals(field.getType())
                       && field.getOrientations() != null
                       && field.getOrientations().contains(orientation.invert().toLowercaseString())) {
                if (firstCellPane == null) {
                    return;
                }
                addLaserStartInactiveToCell(firstCellPane, orientation.invert());
                return;
            }
        }
    }

    private static ImageView addLaserToCell(StackPane cellPane, Direction orientation, String image) {
        String path = "/images/general/tiles/";
        Image laserImage = new Image(
                Objects.requireNonNull(ClientJsonInterpreter.class.getResourceAsStream(path + image + ".png")));
        ImageView laserView = new ImageView(laserImage);
        // Rotate laser image
        switch (orientation) {
            case TOP -> laserView.setRotate(0);
            case BOTTOM -> laserView.setRotate(180);
            case LEFT -> laserView.setRotate(270);
            case RIGHT -> laserView.setRotate(90);
        }
        double cellSize = 0;
        if (!cellPane.getChildren().isEmpty() && cellPane.getChildren().getFirst() instanceof ImageView imageView) {
            cellSize = imageView.getFitWidth();
        }
        laserView.setFitWidth(cellSize);
        laserView.setFitHeight(cellSize);
        laserView.toFront();
        laserView.setPreserveRatio(true);
        cellPane.getChildren().add(laserView);
        return laserView;
    }

    // Add LaserStart
    private static void addLaserStartInactiveToCell(StackPane cellPane, Direction orientation) {
        String imageName = "LaserStartInactive";
        ImageView laserView = addLaserToCell(cellPane, orientation, imageName);
        laserView.setId(imageName);
        laserView.setUserData(orientation);
    }

    /**
     * Fires lasers from predefined wall positions on the game board.
     *
     * @param dataBridge The data bridge containing the game state and laser positions.
     * @param gridPane   The grid where the lasers should be rendered.
     * @param client     The client executing the laser firing action.
     */
    public static void fireWallLasers(GameDataBridge dataBridge, GridPane gridPane, Client client) {
        logger.info("Firing lasers");
        Map<Position, Direction> wallLaserPositions = dataBridge.getWallLaserMap();

        // For each cached wall-laser position, fire the beam.
        for (Map.Entry<Position, Direction> entry : wallLaserPositions.entrySet()) {
            Position pos = entry.getKey();
            Direction laserDir = entry.getValue();
            logger.info("Firing wall laser from " + pos + " in direction " + laserDir);
            renderLaserWithTimeout("wall-" + pos, gridPane, pos.x(), pos.y(), laserDir, client,
                                   dataBridge.getGameMap(), true);
        }
    }

    private static void updateTerminalLaserView(ImageView view, Direction orientation, String terminalImageName, String idSuffix) {
        Image terminalImage = loadImage("/images/general/tiles/" + terminalImageName + ".png");
        view.setImage(terminalImage);

        switch (orientation) {
            case TOP -> view.setRotate(0);
            case BOTTOM -> view.setRotate(180);
            case LEFT -> view.setRotate(270);
            case RIGHT -> view.setRotate(90);
        }

        if (!view.getParent().getChildrenUnmodifiable().isEmpty() &&
            view.getParent().getChildrenUnmodifiable().getFirst() instanceof ImageView) {
            double cellSize = ((ImageView) view.getParent().getChildrenUnmodifiable().getFirst()).getFitWidth();
            view.setFitWidth(cellSize);
            view.setFitHeight(cellSize);
        }
        view.setId("laser-" + idSuffix);
    }

    /**
     * Renders lasers fired by robots on the game board.
     * This method retrieves the robot positions and directions, then calls the laser rendering function.
     *
     * @param gameMap  The game board map containing all game elements.
     * @param gridPane The grid where the robot lasers should be rendered.
     * @param client   The client controlling the robots.
     */
    public static void renderRobotLasers(List<List<List<GameStartedMessage.Field>>> gameMap, GridPane gridPane, Client client) {
        for (Map.Entry<Integer, RobotModel> entry : client.getGameDataBridge().getClientIDToRoboModelToRobotModel()
                                                          .entrySet()) {
            Integer clientId = entry.getKey();
            RobotModel robotModel = entry.getValue();

            if (!client.getGameDataBridge().getRebootedClientIDs().contains(clientId)) {
                int startX = robotModel.getX();
                int startY = robotModel.getY();
                Direction direction = robotModel.getDirection();

                logger.info("Robotlaser test: " + startX + "/" + startY);

                renderLaserWithTimeout(robotModel.getName() + "-robot", gridPane, startX, startY, direction, client,
                                       gameMap, false);


                Map<Integer, List<String>> upgradeMap = client.getGameDataBridge().getClientIDToActivatedUpgradesMap();


                if (upgradeMap.containsKey(clientId)) {
                    List<String> upgrades = upgradeMap.get(clientId);
                    logger.info("Upgrades for client " + clientId + ": " + upgrades);
                    if (upgrades != null && upgrades.contains("RearLaser")) {
                        Direction directionUpgrade = robotModel.getDirection();
                        logger.info("Player " + clientId + " has RearLaser.");

                        // Render the backwards laser for this robot
                        renderLaserWithTimeout(robotModel.getName() + "-rear", gridPane,
                                               startX, startY, directionUpgrade.invert(), client, gameMap, false);
                    }
                }
            }
        }
    }

    private static void renderLaserWithTimeout(String idSuffix, GridPane gridPane, int startX, int startY,
                                               Direction orientation, Client client,
                                               List<List<List<GameStartedMessage.Field>>> gameMap,
                                               boolean checkRobotOnStart) {

        // startX is the row index, startY is the column index.
        int currentX = startX;
        int currentY = startY;
        ImageView lastLaserView = null;

        List<GameStartedMessage.Field> cellFields = gameMap.get(currentX).get(currentY);
        for (GameStartedMessage.Field field : cellFields) {
            if ("Wall".equals(field.getType()) &&
                field.getOrientations().contains(orientation.toLowercaseString())) {
                scheduleOverlayRemoval(new ArrayList<>());
                return;
            }
        }
        // List to track all laser overlays we add so we can remove them later.
        List<ImageView> laserOverlays = new ArrayList<>();
        if (checkRobotOnStart) {
            StackPane startCellPane = getCellPaneAt(gridPane, startY, startX);
            boolean robotOnStart = false;
            for (RobotModel robot : client.getGameDataBridge().getClientIDToRoboModelToRobotModel().values()) {
                if (robot.getX() == startX && robot.getY() == startY) {
                    robotOnStart = true;
                    break;
                }
            }
            if (robotOnStart) {
                if (startCellPane != null) {
                    ImageView laserView = addLaserToCell(startCellPane, orientation.invert(), "LaserEndStart");
                    laserView.setId("laser-" + idSuffix);
                    laserOverlays.add(laserView);
                }
                scheduleOverlayRemoval(laserOverlays);
                return;
            } else {
                if (startCellPane != null) {
                    ImageView laserView = addLaserToCell(startCellPane, orientation.invert(), "LaserStart");
                    laserView.setId("laser-" + idSuffix);
                    laserOverlays.add(laserView);
                }
            }
        }

        while (true) {
            // x is row and y is col:
            switch (orientation) {
                case TOP:
                    currentY--;
                    break;    // row--
                case BOTTOM:
                    currentY++;
                    break;    // row++
                case LEFT:
                    currentX--;
                    break;    // col--
                case RIGHT:
                    currentX++;
                    break;    // col++
            }

            // Stop if out of bounds.
            if (currentX < 0 || currentY < 0 || currentX >= gameMap.size() || currentY >= gameMap.get(0).size()) {
                break;
            }

            // Get the current cell
            StackPane cellPane = getCellPaneAt(gridPane, currentY, currentX);
            if (cellPane == null) {
                break;
            }

            cellFields = gameMap.get(currentX).get(currentY);
            boolean blocked = false;
            for (GameStartedMessage.Field field : cellFields) {
                // Check if a wall with the inverted orientation is present.
                if ("Wall".equals(field.getType()) &&
                    field.getOrientations() != null &&
                    field.getOrientations().contains(orientation.invert().toLowercaseString())) {
                    blocked = true;
                    break;
                }
                if ("Antenna".equals(field.getType())) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                if (lastLaserView != null) {
                    updateTerminalLaserView(lastLaserView, orientation.invert(), "LaserEndWallNext", idSuffix);
                }
                scheduleOverlayRemoval(laserOverlays);
                return;
            }

            // Check for a robot in the current cell.
            for (RobotModel robot : client.getGameDataBridge().getClientIDToRoboModelToRobotModel().values()) {
                if (robot.getX() == currentX && robot.getY() == currentY) {
                    ImageView laserView = addLaserToCell(cellPane, orientation.invert(), "LaserEndRobot");
                    laserView.setId("laser-" + idSuffix);
                    laserOverlays.add(laserView);
                    scheduleOverlayRemoval(laserOverlays);
                    return;
                }
            }

            boolean sameOrientationWall = false;
            boolean laserPresent = false;
            for (GameStartedMessage.Field field : cellFields) {
                if ("Wall".equals(field.getType()) &&
                    field.getOrientations() != null &&
                    field.getOrientations().contains(orientation.toLowercaseString())) {
                    sameOrientationWall = true;
                }
                if ("Laser".equals(field.getType()) &&
                    field.getOrientations().contains(orientation.invert().toLowercaseString())) {
                    laserPresent = true;
                }
            }
            if (sameOrientationWall) {
                if (!laserPresent) {
                    ImageView laserView = addLaserToCell(cellPane, orientation.invert(), "LaserEndWall");
                    laserView.setId("laser-" + idSuffix);
                    laserOverlays.add(laserView);
                }
                scheduleOverlayRemoval(laserOverlays);
                return;
            }

            // Add a new overlay for the laser beam.
            lastLaserView = addRobotLaserToCell(cellPane, orientation, 1);
            if (lastLaserView != null) {
                lastLaserView.setId("laser-" + idSuffix);
                laserOverlays.add(lastLaserView);
            }
        }
        scheduleOverlayRemoval(laserOverlays);
    }

    private static void scheduleOverlayRemoval(List<ImageView> laserOverlays) {
        PauseTransition pause = new PauseTransition(Duration.seconds(0.75));
        pause.setOnFinished(_ -> {
            for (ImageView iv : laserOverlays) {
                if (iv.getParent() instanceof StackPane stackPane) {
                    stackPane.getChildren().remove(iv);
                }
            }
        });
        pause.play();
    }
    /**
     * Updates the mapping of wall lasers in the game map.
     * <p>
     * This method scans the game map for laser positions that are blocked by walls.
     * If a laser faces a wall in the opposite direction,
     * its position and orientation are stored in a temporary map.
     * </p>
     *
     * @param gameMap    The game map.
     * @param dataBridge The instance to store the updated wall laser map.
     */
    public static void updateWallLaserMap(List<List<List<GameStartedMessage.Field>>> gameMap,
                                          GameDataBridge dataBridge) {
        // Run the expensive iteration off the UI thread.
        new Thread(() -> {
            int rows = gameMap.size();
            int columns = gameMap.getFirst().size();
            // A temporary map to store wall-laser positions.
            Map<Position, Direction> wallLaserPositions = new HashMap<>();

            for (int x = 0; x < rows; x++) {
                for (int y = 0; y < columns; y++) {
                    List<GameStartedMessage.Field> cellFields = gameMap.get(x).get(y);
                    if (cellFields == null) continue;

                    Direction laserOrientation = null;
                    // Look for a Laser field in the cell.
                    for (GameStartedMessage.Field field : cellFields) {
                        if ("Laser".equals(field.getType())) {
                            laserOrientation = Direction.fromString(field.getOrientations().getFirst());
                            break;
                        }
                    }
                    if (laserOrientation == null) continue;
                    boolean hasOppositeWall = false;
                    for (GameStartedMessage.Field field : cellFields) {
                        if ("Wall".equals(field.getType())
                            && field.getOrientations() != null
                            && field.getOrientations().contains(laserOrientation.invert().toLowercaseString())) {
                            hasOppositeWall = true;
                            break;
                        }
                    }
                    if (hasOppositeWall) {
                        wallLaserPositions.put(new Position(x, y), laserOrientation);
                    }
                }
            }
            Platform.runLater(() -> {
                dataBridge.setWallLaserMap(wallLaserPositions);
            });
        }).start();
    }

    private static ImageView addRobotLaserToCell(StackPane cellPane, Direction orientation, int count) {
        Image laserImage = switch (count) {
            case 1 -> new Image(Objects.requireNonNull(
                    ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Laser1.png")));
            case 2 -> new Image(Objects.requireNonNull(
                    ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Laser2.png")));
            case 3 -> new Image(Objects.requireNonNull(
                    ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Laser3.png")));
            default -> new Image(Objects.requireNonNull(
                    ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Default.png")));
        };

        ImageView laserView = new ImageView(laserImage);
        laserView.setPreserveRatio(true);

        // Rotate laser image to match orientation
        switch (orientation) {
            case TOP -> laserView.setRotate(0);
            case BOTTOM -> laserView.setRotate(180);
            case LEFT -> laserView.setRotate(270);
            case RIGHT -> laserView.setRotate(90);
        }
        double cellSize = 0;
        if (!cellPane.getChildren().isEmpty() && cellPane.getChildren().getFirst() instanceof ImageView imageView) {
            cellSize = imageView.getFitWidth();
        }
        laserView.setFitWidth(cellSize);
        laserView.setFitHeight(cellSize);

        cellPane.getChildren().add(laserView);
        return laserView;
    }


    public int getCheckpointNumber() {
        return checkpointNumber;
    }

    public static Direction getRestartPointOrientation() {
        return restartPointOrientation;
    }

    private static void bringSpecialTilesToFront(List<List<List<GameStartedMessage.Field>>> gameMap,
                                                 GridPane gridPane) {
        int rows = gameMap.size();
        int columns = gameMap.get(0).size();
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < columns; y++) {
                StackPane cellPane = getCellPaneAt(gridPane, y, x);
                if (cellPane == null) {
                    continue;
                }
                List<ImageView> specialImages = new ArrayList<>();
                for (Node child : new ArrayList<>(cellPane.getChildren())) {
                    if (child instanceof ImageView iv) {
                        if (iv.getId() != null &&
                            ("front--Wall".equals(iv.getId()) || "front--Antenna".equals(iv.getId()))) {
                            specialImages.add(iv);
                        }
                    }
                }
                for (ImageView iv : specialImages) {
                    iv.toFront();
                }
            }
        }
    }
    /**
     * Activates animation for a given push panel type.
     *
     * @param dataBridge   The instance that holds game data, including the game board.
     * @param pushPanelType The type of push panel to animate.
     */
    public static void animatePushPanels(GameDataBridge dataBridge, String pushPanelType) {
        GridPane gridPane = dataBridge.getGameMapGridPane();
        for (Node node : gridPane.getChildren()) {
            if (node instanceof StackPane cellPane) {
                for (Node child : cellPane.getChildren()) {
                    if (child instanceof ImageView imageView) {
                        if (("front--" + pushPanelType).equals(imageView.getId())) {
                            Animation.popOpenPushPanel(imageView, pushPanelType);
                        }
                    }
                }
            }
        }
    }
    /**
     * Moves a checkpoint on the game board to a new position and updates
     * the grid pane and game map accordingly.
     *
     * @param gridPane   The grid pane representing the game board.
     * @param gameMap    The game map.
     * @param checkpointID The checkpoint ID.
     * @param newRow     The row index where the checkpoint should be moved.
     * @param newCol     The column index where the checkpoint should be moved.
     */
    public static void moveCheckpoints(GridPane gridPane, List<List<List<GameStartedMessage.Field>>> gameMap, int checkpointID, int newRow, int newCol) {
        int rows = gameMap.size();
        int columns = gameMap.get(0).size();
        int oldRow = 0;
        int oldCol = 0;
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < columns; y++) {
                List<GameStartedMessage.Field> cellFields = gameMap.get(x).get(y);
                if (cellFields == null) continue;
                for (GameStartedMessage.Field field : cellFields) {
                    if ("CheckPoint".equals(field.getType()) && field.getCount() == checkpointID) {
                        oldRow = x;
                        oldCol = y;
                        break;
                    }
                }
            }
        }
        StackPane cellPaneOld = getCellPaneAt(gridPane, oldCol, oldRow);
        ImageView checkpointView = null;
        for (Node node : cellPaneOld.getChildren()) {
            if (node instanceof ImageView imageView) {
                if (imageView.getId() != null && imageView.getId().startsWith("checkpoint-")) {
                    checkpointView = imageView;
                    break;
                }
            }
        }
        if(checkpointView != null){
            gameMap.get(oldRow).get(oldCol).removeIf(field -> "CheckPoint".equals(field.getType()) && field.getCount() == checkpointID);
            gameMap.get(newRow).get(newCol).add(new GameStartedMessage.Field("CheckPoint", null, 0, null, null, checkpointID));
            cellPaneOld.getChildren().remove(checkpointView);
            StackPane cellPaneNew = getCellPaneAt(gridPane, newCol, newRow);
            checkpointView.setId("checkpoint--");
            cellPaneNew.getChildren().add(checkpointView);
        }
    }
}
