package viewmodel.tilesGUI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps tile types to their corresponding image paths and determines
 * the appropriate image for a tile based on its attributes.
 */
public class TileImageMapper {
    private static final Map<String, String> tileImageMap = new HashMap<>();

    static {
        tileImageMap.put("Empty", "/images/general/tiles/Empty.png");
        tileImageMap.put("Default", "/images/general/tiles/Default.png");
        tileImageMap.put("ConveyorBelt_Green", "/images/general/tiles/ConveyorBelt_Green.png");
        tileImageMap.put("ConveyorBelt_Green_Left_Up", "/images/general/tiles/ConveyorBelt_Green_Left_Up.png");
        tileImageMap.put("ConveyorBelt_Green_Right_Up", "/images/general/tiles/ConveyorBelt_Green_Right_Up.png");
        tileImageMap.put("ConveyorBelt_Green_Left_Right", "/images/general/tiles/ConveyorBelt_Green_Left_Right.png");
        tileImageMap.put("ConveyorBelt_Green_Straight_Left", "/images/general/tiles/ConveyorBelt_Green_Straight_Left.png");
        tileImageMap.put("ConveyorBelt_Green_Straight_Right", "/images/general/tiles/ConveyorBelt_Green_Straight_Right.png");
        tileImageMap.put("ConveyorBelt_Blue", "/images/general/tiles/ConveyorBelt_Blue.png");
        tileImageMap.put("ConveyorBelt_Blue_Left_Up", "/images/general/tiles/ConveyorBelt_Blue_Left_Up.png");
        tileImageMap.put("ConveyorBelt_Blue_Right_Up", "/images/general/tiles/ConveyorBelt_Blue_Right_Up.png");
        tileImageMap.put("ConveyorBelt_Blue_Left_Right", "/images/general/tiles/ConveyorBelt_Blue_Left_Right.png");
        tileImageMap.put("ConveyorBelt_Blue_Straight_Left", "/images/general/tiles/ConveyorBelt_Blue_Straight_Left.png");
        tileImageMap.put("ConveyorBelt_Blue_Straight_Right", "/images/general/tiles/ConveyorBelt_Blue_Straight_Right.png");
        tileImageMap.put("PushPanel135", "/images/general/tiles/PushPanel135.png");
        tileImageMap.put("PushPanel24", "/images/general/tiles/PushPanel24.png");
        tileImageMap.put("WallTop", "/images/general/tiles/WallTop.png");
        tileImageMap.put("WallTopRight", "/images/general/tiles/WallTopRight.png");
        tileImageMap.put("WallTopLeft", "/images/general/tiles/WallTopLeft.png");
        tileImageMap.put("Laser1", "/images/general/tiles/Laser1.png");
        tileImageMap.put("Laser2", "/images/general/tiles/Laser2.png");
        tileImageMap.put("Laser3", "/images/general/tiles/Laser3.png");
        tileImageMap.put("Antenna", "/images/general/tiles/Antenna.png");
        tileImageMap.put("Pit", "/images/general/tiles/Pit.png");
        tileImageMap.put("GearClockwise", "/images/general/tiles/GearClockwise.png");
        tileImageMap.put("GearCounterclockwise", "/images/general/tiles/GearCounterclockwise.png");
        tileImageMap.put("Energy-Space0", "/images/general/tiles/Energy-Space0.png");
        tileImageMap.put("Energy-Space1", "/images/general/tiles/Energy-Space1.png");
        tileImageMap.put("StartPoint", "/images/general/tiles/StartPoint.png");
        tileImageMap.put("RestartPoint", "/images/general/tiles/Restart.png");
        tileImageMap.put("CheckPoint1", "/images/general/tiles/CheckPoint1.png");
        tileImageMap.put("CheckPoint2", "/images/general/tiles/CheckPoint2.png");
        tileImageMap.put("CheckPoint3", "/images/general/tiles/CheckPoint3.png");
        tileImageMap.put("CheckPoint4", "/images/general/tiles/CheckPoint4.png");
        tileImageMap.put("CheckPoint5", "/images/general/tiles/CheckPoint5.png");
        tileImageMap.put("CheckPoint6", "/images/general/tiles/CheckPoint6.png");
    }
    /**
     * Determines the appropriate image path based on tile type and attributes.
     *
     * @param type        The type of the tile (e.g., "ConveyorBelt").
     * @param tileObject  The JsonObject representing the tile.
     * @return The image path as a string.
     */
    public static String getImagePath(String type, JsonObject tileObject) {
        ArrayList<String> orientations = getOrientations(tileObject.get("orientations"));
        switch (type) {
            case "Empty":
                return tileImageMap.get("Empty");
            case "ConveyorBelt":
                int speed = tileObject.get("speed").getAsInt();
                int outflow = directionToNumber(orientations.get(0));
                List<Integer> inflows = new ArrayList<>();
                for (int i = 1; i < orientations.size(); i++) {
                    inflows.add(directionToNumber(orientations.get(i)));
                }
                String conveyorType = determineConveyorType(outflow, inflows);
                // Build the image String based on speed and conveyor type
                String imageKey = "";
                if (speed == 1) {
                    imageKey = "ConveyorBelt_Green" + conveyorType;
                } else if (speed == 2) {
                    imageKey = "ConveyorBelt_Blue" + conveyorType;
                }
                return tileImageMap.get(imageKey);
            case "PushPanel":
                JsonArray registersArray = tileObject.getAsJsonArray("registers");
                int registers = registersArray.get(0).getAsInt();
                if (registers == 1) {
                    return tileImageMap.get("PushPanel135");
                } else if (registers == 2) {
                    return tileImageMap.get("PushPanel24");
                }
                break;
            case "Laser":
                int countLaser = tileObject.get("count").getAsInt();
                if (countLaser == 1) {
                    return tileImageMap.get("Laser1");
                }else if (countLaser == 2){
                    return tileImageMap.get("Laser2");
                }else if (countLaser == 3){
                    return tileImageMap.get("Laser3");
                }
                break;
            case "Wall":
                if(orientations.size() < 2){
                    return tileImageMap.get("WallTop");
                }else if(orientations.get(0).equals("top") && orientations.get(1).equals("left")){
                    return tileImageMap.get("WallTopLeft");
                } else {
                    return tileImageMap.get("WallTopRight");
                }
            case "EnergySpace":
                int countEnergy = tileObject.get("count").getAsInt();
                if (countEnergy == 0) {
                    return tileImageMap.get("Energy-Space0");
                }else if (countEnergy == 1){
                    return tileImageMap.get("Energy-Space1");
                }
                break;
            case "Gear":
                String orientation = orientations.get(0);
                if (orientation.equals("clockwise")) {
                    return tileImageMap.get("GearClockwise");
                }else if (orientation.equals("counterclockwise")){
                    return tileImageMap.get("GearCounterclockwise");
                }
                break;
            case "Antenna":
                return tileImageMap.get("Antenna");
            case "Pit":
                return tileImageMap.get("Pit");
            case "RestartPoint":
                return tileImageMap.get("RestartPoint");
            case "CheckPoint":
                int countCheckPoint = tileObject.get("count").getAsInt();
                return switch (countCheckPoint) {
                    case 1 -> tileImageMap.get("CheckPoint1");
                    case 2 -> tileImageMap.get("CheckPoint2");
                    case 3 -> tileImageMap.get("CheckPoint3");
                    case 4 -> tileImageMap.get("CheckPoint4");
                    case 5 -> tileImageMap.get("CheckPoint5");
                    case 6 -> tileImageMap.get("CheckPoint6");
                    default -> throw new IllegalStateException("Unexpected value: " + countCheckPoint);
                };
            case "StartPoint":
                return tileImageMap.get("StartPoint");
            default:
                return tileImageMap.getOrDefault(type, "/images/general/tiles/Default.png");
        }
        return "/images/general/tiles/Default.png";
    }

    private static ArrayList<String> getOrientations(JsonElement element){
        if(element == null){
            return null;
        }
        ArrayList<String> orientations = new ArrayList<>();
        JsonArray array = (JsonArray) element;
        for(JsonElement el : array){
            orientations.add(el.getAsString());
        }
        return orientations;
    }

    private static int directionToNumber(String direction) {
        switch (direction) {
            case "top":    return 0;
            case "right":  return 1;
            case "bottom": return 2;
            case "left":   return 3;
            default:       return 0;
        }
    }

    private static String determineConveyorType(int outflow, List<Integer> inflows) {
        if (inflows.size() == 1) {
            int inflow = inflows.get(0);
            if (inflow == outflow) {
                return "";
            } else if ((inflow + 1) % 4 == outflow) {
                return "_Left_Up";
            } else if ((inflow + 3) % 4 == outflow) {
                return "_Right_Up";
            }
        } else if (inflows.size() == 2) {
            if (inflows.contains((outflow + 1) % 4) && inflows.contains((outflow + 3) % 4)) {
                return "_Left_Right";
            } else if (inflows.contains((outflow + 1) % 4)) {
                return "_Straight_Right";
            } else if (inflows.contains((outflow + 3) % 4)) {
                return "_Straight_Left";
            }
        }
        return "";
    }

}