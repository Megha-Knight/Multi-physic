package cam.geometry;

/**
 * Directional offset side and boundary classification for 2D CAM contours.
 * Explicitly separates machining side intent from polygon winding.
 */
public enum ContourSide {
    LEFT,
    RIGHT,
    INSIDE,
    OUTSIDE;

    public boolean isClosedOnly() {
        return this == INSIDE || this == OUTSIDE;
    }

    /**
     * Resolves high-level machining side intent into left/right offset relative to travel direction.
     * @param isClosed true if contour is a closed loop enclosing an area
     * @param isCcw true if closed contour winding is counter-clockwise
     * @return resolved LEFT or RIGHT offset side
     */
    public ContourSide resolveTravelSide(boolean isClosed, boolean isCcw) {
        if (!isClosed && isClosedOnly()) {
            throw new IllegalArgumentException("Machining side " + this + " is only applicable to closed boundaries.");
        }
        if (!isClosed) return this;

        if (this == INSIDE) {
            return isCcw ? LEFT : RIGHT;
        } else if (this == OUTSIDE) {
            return isCcw ? RIGHT : LEFT;
        }
        return this;
    }
}
