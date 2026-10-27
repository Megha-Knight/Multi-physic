package cam.post.verifier;

/**
 * Reconstructed NC modal state machine maintained independently by the verifier
 * by interpreting sequential G-code blocks.
 */
public class NcInterpreterState {

    private boolean initialized = false;
    private String activeWcs = null;
    private String activePlane = null;
    private String activeDistanceMode = null;
    private String activeMotionGroup = null;

    private boolean spindleRunning = false;
    private double spindleRpm = 0.0;
    private double activeFeedRate = 0.0;

    private Integer activeTool = null;
    private Integer activeHRegister = null;
    private boolean toolLengthCompActive = false;
    private boolean cannedCycleActive = false;

    private double currentX = Double.NaN;
    private double currentY = Double.NaN;
    private double currentZ = Double.NaN;

    private boolean programTerminated = false;
    private int toolChangeCount = 0;
    private int totalMotionBlocks = 0;
    private int cuttingMotionBlocks = 0;
    private int rapidMotionBlocks = 0;

    private double minZ = Double.POSITIVE_INFINITY;
    private double maxZ = Double.NEGATIVE_INFINITY;
    private double minX = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    public void updatePosition(double x, double y, double z) {
        if (Double.isFinite(x)) {
            this.currentX = x;
            this.minX = Math.min(minX, x);
            this.maxX = Math.max(maxX, x);
        }
        if (Double.isFinite(y)) {
            this.currentY = y;
            this.minY = Math.min(minY, y);
            this.maxY = Math.max(maxY, y);
        }
        if (Double.isFinite(z)) {
            this.currentZ = z;
            this.minZ = Math.min(minZ, z);
            this.maxZ = Math.max(maxZ, z);
        }
    }

    public void recordMotion(boolean cutting) {
        totalMotionBlocks++;
        if (cutting) {
            cuttingMotionBlocks++;
        } else {
            rapidMotionBlocks++;
        }
    }

    public boolean isInitialized()                  { return initialized; }
    public void setInitialized(boolean init)        { this.initialized = init; }

    public String getActiveWcs()                     { return activeWcs; }
    public void setActiveWcs(String wcs)            { this.activeWcs = wcs; }

    public String getActivePlane()                   { return activePlane; }
    public void setActivePlane(String plane)        { this.activePlane = plane; }

    public String getActiveDistanceMode()            { return activeDistanceMode; }
    public void setActiveDistanceMode(String mode)  { this.activeDistanceMode = mode; }

    public String getActiveMotionGroup()             { return activeMotionGroup; }
    public void setActiveMotionGroup(String group)  { this.activeMotionGroup = group; }

    public boolean isSpindleRunning()               { return spindleRunning; }
    public void setSpindleRunning(boolean running)  { this.spindleRunning = running; }

    public double getSpindleRpm()                   { return spindleRpm; }
    public void setSpindleRpm(double rpm)           { this.spindleRpm = rpm; }

    public double getActiveFeedRate()               { return activeFeedRate; }
    public void setActiveFeedRate(double feed)      { this.activeFeedRate = feed; }

    public Integer getActiveTool()                  { return activeTool; }
    public void setActiveTool(Integer tool) {
        this.activeTool = tool;
        this.activeHRegister = null;
        this.toolLengthCompActive = false;
        this.toolChangeCount++;
    }

    public Integer getActiveHRegister()             { return activeHRegister; }
    public void setActiveHRegister(Integer h)       { this.activeHRegister = h; }

    public boolean isToolLengthCompActive()         { return toolLengthCompActive; }
    public void setToolLengthCompActive(boolean ac) { this.toolLengthCompActive = ac; }

    public boolean isCannedCycleActive()            { return cannedCycleActive; }
    public void setCannedCycleActive(boolean ac)    { this.cannedCycleActive = ac; }

    public double getCurrentX()                     { return currentX; }
    public double getCurrentY()                     { return currentY; }
    public double getCurrentZ()                     { return currentZ; }

    public boolean isProgramTerminated()            { return programTerminated; }
    public void setProgramTerminated(boolean term)  { this.programTerminated = term; }

    public int getToolChangeCount()                 { return toolChangeCount; }
    public int getTotalMotionBlocks()               { return totalMotionBlocks; }
    public int getCuttingMotionBlocks()             { return cuttingMotionBlocks; }
    public int getRapidMotionBlocks()               { return rapidMotionBlocks; }

    public double getMinZ()                         { return minZ; }
    public double getMaxZ()                         { return maxZ; }
    public double getMinX()                         { return minX; }
    public double getMaxX()                         { return maxX; }
    public double getMinY()                         { return minY; }
    public double getMaxY()                         { return maxY; }
}
