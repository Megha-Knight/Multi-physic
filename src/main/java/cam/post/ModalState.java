package cam.post;

/**
 * Tracks the active modal state of the CNC controller during postprocessing.
 */
public class ModalState {

    private String activeMotionGroup;
    private String activeWcs;
    private double currentX = Double.NaN;
    private double currentY = Double.NaN;
    private double currentZ = Double.NaN;
    private double activeFeedRate = -1.0;
    private double activeSpindleRpm = -1.0;
    private boolean spindleRunning = false;
    private int activeToolNumber = -1;
    private boolean toolLengthCompActive = false;
    private int activeHRegister = -1;
    private boolean setupExecuted = false;

    public void reset() {
        activeMotionGroup = null;
        activeWcs = null;
        currentX = Double.NaN;
        currentY = Double.NaN;
        currentZ = Double.NaN;
        activeFeedRate = -1.0;
        activeSpindleRpm = -1.0;
        spindleRunning = false;
        activeToolNumber = -1;
        toolLengthCompActive = false;
        activeHRegister = -1;
        setupExecuted = false;
    }

    public String getActiveMotionGroup()       { return activeMotionGroup; }
    public void setActiveMotionGroup(String m) { this.activeMotionGroup = m; }
    public String getActiveWcs()               { return activeWcs; }
    public void setActiveWcs(String wcs)       { this.activeWcs = wcs; }
    public double getCurrentX()                { return currentX; }
    public double getCurrentY()                { return currentY; }
    public double getCurrentZ()                { return currentZ; }

    public void updatePosition(double x, double y, double z) {
        this.currentX = x;
        this.currentY = y;
        this.currentZ = z;
    }

    public double getActiveFeedRate()          { return activeFeedRate; }
    public void setActiveFeedRate(double f)    { this.activeFeedRate = f; }
    public double getActiveSpindleRpm()        { return activeSpindleRpm; }
    public void setActiveSpindleRpm(double s)  { this.activeSpindleRpm = s; }
    public boolean isSpindleRunning()          { return spindleRunning; }
    public void setSpindleRunning(boolean r)   { this.spindleRunning = r; }
    public int getActiveToolNumber()           { return activeToolNumber; }
    public void setActiveToolNumber(int t)     { this.activeToolNumber = t; }
    public boolean isToolLengthCompActive()    { return toolLengthCompActive; }
    public void setToolLengthCompActive(boolean c) { this.toolLengthCompActive = c; }
    public int getActiveHRegister()            { return activeHRegister; }
    public void setActiveHRegister(int h)      { this.activeHRegister = h; }
    public boolean isSetupExecuted()           { return setupExecuted; }
    public void setSetupExecuted(boolean s)    { this.setupExecuted = s; }

    public boolean isCannedCycleActive() {
        return activeMotionGroup != null && activeMotionGroup.startsWith("G8") && !"G80".equals(activeMotionGroup);
    }
}
