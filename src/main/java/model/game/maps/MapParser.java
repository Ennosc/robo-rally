package model.game.maps;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import model.game.board.Direction;
import model.game.board.tiles.*;
import network.messages.lobby3.GameStartedMessage;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapParser {

    private String fileName;
    private JsonReader reader;
    private List<List<List<Tile>>> board;

    /**
     * Creates a MapParser instance from an InputStream.
     * It parses the JSON and creates the internal board representation.
     */
    public MapParser(InputStream inputStream) {
        Gson gson = new Gson();
        try {
            this.reader = new JsonReader(new InputStreamReader(inputStream));
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            JsonObject mapObject = json.getAsJsonObject("messageBody");
            board = createBoard(mapObject);
        } catch (Exception e) {
            throw new RuntimeException("Error reading map JSON", e);
        }
    }

    /**
     * Returns the content of the selected map as a JSON string.
     *
     * @param filename The path to the JSON file.
     * @return The map data as a JSON string.
     */
    public String getSelectedMapAsJSON(String filename) {
        Gson gson = new Gson();
        try {
            this.reader = new JsonReader(new FileReader(filename));
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            return gson.toJson(json);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the map data from the provided JSON object and constructs the game board.
     *
     * Each row is a JSON array; each column in a row is itself a JSON array containing one
     * or more tile objects.
     *
     * @param mapObject the JSON object containing the map data under the "gameMap" field
     * @return a nested list representing the board: rows → columns → list of Tile objects
     */
    private List<List<List<Tile>>> createBoard(JsonObject mapObject) {
        List<List<List<Tile>>> rowsList = new ArrayList<>();

        JsonArray rows = mapObject.getAsJsonArray("gameMap");
        for (JsonElement rowElement : rows) {
            JsonArray cols = rowElement.getAsJsonArray();
            List<List<Tile>> colsList = new ArrayList<>();
            for (JsonElement colElement : cols) {
                List<Tile> tilesList = new ArrayList<>();
                if (!colElement.isJsonNull()) {
                    JsonArray tiles = colElement.getAsJsonArray();
                    // Loop through every object (tile) in the cell.
                    for (JsonElement tileElement : tiles) {
                        Tile tile = mapJsonToTiles(tileElement);
                        if (tile != null) {
                            tilesList.add(tile);
                        }
                    }
                }

                for (Tile tile : tilesList) {
                    if (tile instanceof LaserTile) {
                        LaserTile laserTile = (LaserTile) tile;
                        Direction laserOrientation = laserTile.getDirection();
                        for (Tile otherTile : tilesList) {
                            if (otherTile instanceof WallTile) {
                                WallTile wallTile = (WallTile) otherTile;
                                if (wallTile.getWall(laserOrientation.invert())) {
                                    laserTile.setStartingLaser(true);
                                    break;
                                }
                            }
                        }
                    }
                }
                colsList.add(tilesList);
            }
            rowsList.add(colsList);
        }
        return rowsList;
    }

    /**
     * Converts a JSON element into the corresponding Tile object.
     * This method safely checks for optional fields such as "isOnBoard" and "orientations"
     * so that the new JSON format (with multiple objects per cell) is accepted.
     *
     * @param tileElement the JSON element representing a single tile
     * @return a Tile object corresponding to the JSON data, or null if the type is unknown
     */
    private Tile mapJsonToTiles(JsonElement tileElement) {
        JsonObject object = tileElement.getAsJsonObject();

        // "type" must be present
        String type = object.get("type").getAsString();

        // Get "isOnBoard" if present, else use a default (empty string)
        String isOnBoard = object.has("isOnBoard") ? object.get("isOnBoard").getAsString() : "";

        // Get "orientations" safely (if absent, our helper returns null)
        ArrayList<String> orientations = getOrientations(object.get("orientations"));
        if (orientations == null) {
            orientations = new ArrayList<>();
        }

        // Default direction values
        Direction direction = Direction.RIGHT;
        String directionString = "default";

        switch (type) {
            case "Empty":
                return new EmptyTile(isOnBoard);

            case "CheckPoint":
                int cpCount = object.has("count") ? object.get("count").getAsInt() : 0;
                return new CheckpointTile(isOnBoard, cpCount);

            case "EnergySpace":
                int energyCount = object.has("count") ? object.get("count").getAsInt() : 0;
                return new EnergySpaceTile(isOnBoard, energyCount);

            case "Gear":
                for (String orientation : orientations) {
                    if ("clockwise".equals(orientation)) {
                        directionString = "clockwise";
                    } else if ("counterclockwise".equals(orientation)) {
                        directionString = "counterclockwise";
                    }
                }
                return new GearTile(isOnBoard, directionString);

            case "Pit":
                return new PitTile(isOnBoard);

            case "Antenna":
                for (String orientation : orientations) {
                    switch (orientation) {
                        case "top":
                            direction = Direction.TOP;
                            break;
                        case "right":
                            direction = Direction.RIGHT;
                            break;
                        case "bottom":
                            direction = Direction.BOTTOM;
                            break;
                        case "left":
                            direction = Direction.LEFT;
                            break;
                    }
                }
                return new AntennaTile(isOnBoard, direction);

            case "ConveyorBelt":
                // The first orientation is for outflow
                if (!orientations.isEmpty()) {
                    switch (orientations.get(0)) {
                        case "top":
                            direction = Direction.TOP;
                            break;
                        case "right":
                            direction = Direction.RIGHT;
                            break;
                        case "bottom":
                            direction = Direction.BOTTOM;
                            break;
                        case "left":
                            direction = Direction.LEFT;
                            break;
                    }
                }
                // Remaining orientations, if any, are inflow directions.
                List<Direction> inflowDirections = new ArrayList<>();
                for (int i = 1; i < orientations.size(); i++) {
                    String orient = orientations.get(i);
                    switch (orient) {
                        case "top":
                            inflowDirections.add(Direction.TOP);
                            break;
                        case "right":
                            inflowDirections.add(Direction.RIGHT);
                            break;
                        case "bottom":
                            inflowDirections.add(Direction.BOTTOM);
                            break;
                        case "left":
                            inflowDirections.add(Direction.LEFT);
                            break;
                    }
                }
                int speed = object.has("speed") ? object.get("speed").getAsInt() : 1;
                return new ConveyorBeltTile(isOnBoard, direction, inflowDirections, speed);

            case "Laser":
                for (String orientation : orientations) {
                    switch (orientation) {
                        case "top":
                            direction = Direction.TOP;
                            break;
                        case "right":
                            direction = Direction.RIGHT;
                            break;
                        case "bottom":
                            direction = Direction.BOTTOM;
                            break;
                        case "left":
                            direction = Direction.LEFT;
                            break;
                    }
                }
                int laserCount = object.has("count") ? object.get("count").getAsInt() : 1;
                return new LaserTile(isOnBoard, direction, laserCount);

            case "PushPanel":
                for (String orientation : orientations) {
                    switch (orientation) {
                        case "top":
                            direction = Direction.TOP;
                            break;
                        case "right":
                            direction = Direction.RIGHT;
                            break;
                        case "bottom":
                            direction = Direction.BOTTOM;
                            break;
                        case "left":
                            direction = Direction.LEFT;
                            break;
                    }
                }
                return new PushPanelTile(isOnBoard, direction, getRegisters(object.get("registers")));

            case "RestartPoint":
                for (String orientation : orientations) {
                    switch (orientation) {
                        case "top":
                            direction = Direction.TOP;
                            break;
                        case "right":
                            direction = Direction.RIGHT;
                            break;
                        case "bottom":
                            direction = Direction.BOTTOM;
                            break;
                        case "left":
                            direction = Direction.LEFT;
                            break;
                    }
                }
                return new RebootTokenTile(isOnBoard, direction);

            case "StartPoint":
                return new StartPointTile(isOnBoard);

            case "Wall":
                // Create a list for wall sides: index 0=top, 1=right, 2=bottom, 3=left.
                List<Boolean> wallDirections = new ArrayList<>(Arrays.asList(false, false, false, false));
                for (String orientation : orientations) {
                    switch (orientation) {
                        case "top":
                            wallDirections.set(0, true);
                            break;
                        case "right":
                            wallDirections.set(1, true);
                            break;
                        case "bottom":
                            wallDirections.set(2, true);
                            break;
                        case "left":
                            wallDirections.set(3, true);
                            break;
                    }
                }
                return new WallTile(isOnBoard, wallDirections);

            default:
                // Unrecognized type
                return null;
        }
    }

    /**
     * Helper method to safely extract orientations from a JSON element.
     *
     * @param element the JSON element which should be an array of strings
     * @return an ArrayList of orientation strings, or null if not present
     */
    private ArrayList<String> getOrientations(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        ArrayList<String> orientations = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement el : array) {
            orientations.add(el.getAsString());
        }
        return orientations;
    }

    /**
     * Helper method to extract registers from a JSON element.
     *
     * @param element the JSON element representing an array of integers
     * @return an ArrayList of registers, or null if not present
     */
    public ArrayList<Integer> getRegisters(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        ArrayList<Integer> registers = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement el : array) {
            registers.add(el.getAsInt());
        }
        return registers;
    }

    public List<List<List<Tile>>> getBoard() {
        return board;
    }

    /**
     * Converts the internal Tile board into the Field format required by GameStartedMessage.
     *
     * This conversion extracts the tile name, board location, speed, orientations, registers,
     * and count (for lasers and checkpoints) and packages them into a Field.
     *
     * @param tileBoard the board represented as a nested List of Tile objects
     * @return the board represented as a nested List of GameStartedMessage.Field objects
     */
    public List<List<List<GameStartedMessage.Field>>> convertTilesToFields(List<List<List<Tile>>> tileBoard) {
        List<List<List<GameStartedMessage.Field>>> fieldBoard = new ArrayList<>();

        for (int x = 0; x < tileBoard.size(); x++) {
            List<List<GameStartedMessage.Field>> colList = new ArrayList<>();
            for (int y = 0; y < tileBoard.get(x).size(); y++) {
                List<Tile> tilesAtPosition = tileBoard.get(x).get(y);
                List<GameStartedMessage.Field> fieldsAtPosition = new ArrayList<>();

                for (Tile tile : tilesAtPosition) {
                    // Extract properties from the Tile and convert to a Field
                    String type = tile.getName();
                    String isOnBoard = tile.getIsOnBoard();
                    int speed = 0;
                    List<String> orientations = null;
                    List<Integer> registers = null;
                    int count = 0;

                    if (tile instanceof CheckpointTile) {
                        count = ((CheckpointTile) tile).getCheckpointNumber();
                    } else if (tile instanceof EnergySpaceTile) {
                        count = ((EnergySpaceTile) tile).getCount();
                    } else if (tile instanceof LaserTile) {
                        count = ((LaserTile) tile).getDamage();
                        orientations = new ArrayList<>();
                        LaserTile l = (LaserTile) tile;
                        switch (l.getDirection()) {
                            case TOP:
                                orientations.add("top");
                                break;
                            case RIGHT:
                                orientations.add("right");
                                break;
                            case BOTTOM:
                                orientations.add("bottom");
                                break;
                            case LEFT:
                                orientations.add("left");
                                break;
                        }
                    } else if (tile instanceof ConveyorBeltTile) {
                        speed = ((ConveyorBeltTile) tile).getSpeed();
                        orientations = new ArrayList<>();
                        ConveyorBeltTile c = (ConveyorBeltTile) tile;
                        if (c.getOutflowDirection().equals(Direction.TOP)) orientations.add("top");
                        if (c.getOutflowDirection().equals(Direction.RIGHT)) orientations.add("right");
                        if (c.getOutflowDirection().equals(Direction.BOTTOM)) orientations.add("bottom");
                        if (c.getOutflowDirection().equals(Direction.LEFT)) orientations.add("left");
                        if (!c.getInflowDirections().isEmpty() && c.getInflowDirections().get(0).equals(Direction.TOP)) orientations.add("top");
                        if (!c.getInflowDirections().isEmpty() && c.getInflowDirections().get(0).equals(Direction.RIGHT)) orientations.add("right");
                        if (!c.getInflowDirections().isEmpty() && c.getInflowDirections().get(0).equals(Direction.BOTTOM)) orientations.add("bottom");
                        if (!c.getInflowDirections().isEmpty() && c.getInflowDirections().get(0).equals(Direction.LEFT)) orientations.add("left");
                        if (c.getInflowDirections().size() > 1 && c.getInflowDirections().get(1).equals(Direction.TOP)) orientations.add("top");
                        if (c.getInflowDirections().size() > 1 && c.getInflowDirections().get(1).equals(Direction.RIGHT)) orientations.add("right");
                        if (c.getInflowDirections().size() > 1 && c.getInflowDirections().get(1).equals(Direction.BOTTOM)) orientations.add("bottom");
                        if (c.getInflowDirections().size() > 1 && c.getInflowDirections().get(1).equals(Direction.LEFT)) orientations.add("left");
                    } else if (tile instanceof PushPanelTile) {
                        registers = ((PushPanelTile) tile).getActivationRegisters();
                        orientations = new ArrayList<>();
                        PushPanelTile p = (PushPanelTile) tile;
                        if (p.getPushDirection().equals(Direction.TOP)) orientations.add("top");
                        if (p.getPushDirection().equals(Direction.RIGHT)) orientations.add("right");
                        if (p.getPushDirection().equals(Direction.BOTTOM)) orientations.add("bottom");
                        if (p.getPushDirection().equals(Direction.LEFT)) orientations.add("left");
                    } else if (tile instanceof GearTile) {
                        orientations = List.of(((GearTile) tile).getDirection());
                    } else if (tile instanceof AntennaTile) {
                        orientations = List.of(((AntennaTile) tile).getDirection().toLowercaseString());
                    } else if (tile instanceof RebootTokenTile) {
                        orientations = new ArrayList<>();
                        RebootTokenTile r = (RebootTokenTile) tile;
                        if (r.getDirection().equals(Direction.TOP)) orientations.add("top");
                        if (r.getDirection().equals(Direction.RIGHT)) orientations.add("right");
                        if (r.getDirection().equals(Direction.BOTTOM)) orientations.add("bottom");
                        if (r.getDirection().equals(Direction.LEFT)) orientations.add("left");
                    } else if (tile instanceof WallTile) {
                        orientations = new ArrayList<>();
                        WallTile w = (WallTile) tile;
                        if (w.getWall(Direction.TOP)) orientations.add("top");
                        if (w.getWall(Direction.RIGHT)) orientations.add("right");
                        if (w.getWall(Direction.BOTTOM)) orientations.add("bottom");
                        if (w.getWall(Direction.LEFT)) orientations.add("left");
                    } else if (tile instanceof PitTile) {
                        // No extra properties for Pit
                    } else if (tile instanceof StartPointTile) {
                        // No extra properties for StartPoint
                    }

                    GameStartedMessage.Field field = new GameStartedMessage.Field(
                            type,
                            isOnBoard,
                            speed,
                            orientations == null ? null : orientations,
                            registers == null ? null : registers,
                            count
                    );
                    fieldsAtPosition.add(field);
                }
                colList.add(fieldsAtPosition);
            }
            fieldBoard.add(colList);
        }
        return fieldBoard;
    }
}