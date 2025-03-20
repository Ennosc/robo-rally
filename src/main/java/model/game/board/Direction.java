package model.game.board;

public enum Direction {
    TOP, RIGHT, BOTTOM, LEFT;

    private static final Direction[] VALUES = values();
    private static final int SIZE = VALUES.length;

    /**
     * Rotates clockwise.
     *
     * @return the new Direction after a clockwise turn
     */
    public Direction turnClockwise() {
        return VALUES[(this.ordinal() + 1) % SIZE];
    }

    /**
     * Rotates counterclockwise.
     *
     * @return the new Direction after a counterclockwise turn
     */
    public Direction turnCounterClockwise() {
        return VALUES[(this.ordinal() - 1 + SIZE) % SIZE];
    }

    /**
     * Inverts the current direction, which results in a 180° turn.
     *
     * @return the opposite direction
     */
    public Direction invert() {
        return VALUES[(this.ordinal() + 2) % SIZE];
    }

    /**
     * Returns the direction's name in lowercase.
     *
     * @return the lowercase string representation of the direction
     */
    public String toLowercaseString() {
        return this.name().toLowerCase();
    }

    /**
     * Converts a string into a Direction.
     *
     * @param direction a string (ie. "top") representing a direction
     * @return the corresponding Direction
     * @throws IllegalArgumentException if the string is null or does not match a valid direction
     */
    public static Direction fromString(String direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction String cannot be null");
        }
        String lower = direction.trim().toLowerCase();
        return switch (lower) {
            case "top", "up" -> TOP;
            case "bottom", "down" -> BOTTOM;
            case "left" -> LEFT;
            case "right" -> RIGHT;
            case "clockwise" -> {
                yield RIGHT;
            }
            case "counterclockwise" -> {
                yield LEFT;
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };
    }

    /**
     * Changes the current direction according to the specified rotation.
     *
     * @param directionOfRotation a string indicating the rotation ("clockwise" or "counterclockwise")
     * @return the new direction after applying the rotation
     * @throws IllegalArgumentException for any non accounted for directions
     */
    public Direction rotate(String directionOfRotation) {
        if (directionOfRotation.equals("counterclockwise")) {
            return turnCounterClockwise();
        } else if (directionOfRotation.equals("clockwise")) {
            return turnClockwise();
        } else {
            throw new IllegalArgumentException("Invalid rotation: " + directionOfRotation);
        }
    }
}

