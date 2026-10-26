package cam.post;

import java.util.Locale;

/**
 * Controller configuration profile specifying CNC syntax, coordinate conventions,
 * modal suppression, canned cycle capabilities, and safety blocks.
 */
public class ControllerProfile {

    public enum UnitMode { METRIC_G21, INCH_G20 }
    public enum CoordinateMode { ABSOLUTE_G90, INCREMENTAL_G91 }
    public enum PlaneMode { XY_G17, XZ_G18, YZ_G19 }

    private final String name;
    private final UnitMode unitMode;
    private final CoordinateMode coordinateMode;
    private final PlaneMode planeMode;
    private final String defaultWcs;
    private final boolean modalSuppressionEnabled;
    private final boolean lineNumbersEnabled;
    private final int lineNumberIncrement;
    private final int decimalPrecision;
    private final boolean supportCannedCycles;
    private final String safeRetractCommand;
    private final String programEndCommand;

    public ControllerProfile(String name, UnitMode unitMode, CoordinateMode coordinateMode,
                             PlaneMode planeMode, String defaultWcs, boolean modalSuppressionEnabled,
                             boolean lineNumbersEnabled, int lineNumberIncrement, int decimalPrecision,
                             boolean supportCannedCycles, String safeRetractCommand, String programEndCommand) {
        this.name = (name != null) ? name : "ISO 6983 Standard";
        this.unitMode = (unitMode != null) ? unitMode : UnitMode.METRIC_G21;
        this.coordinateMode = (coordinateMode != null) ? coordinateMode : CoordinateMode.ABSOLUTE_G90;
        this.planeMode = (planeMode != null) ? planeMode : PlaneMode.XY_G17;
        this.defaultWcs = (defaultWcs != null && !defaultWcs.isBlank()) ? defaultWcs : "G54";
        this.modalSuppressionEnabled = modalSuppressionEnabled;
        this.lineNumbersEnabled = lineNumbersEnabled;
        this.lineNumberIncrement = Math.max(1, lineNumberIncrement);
        this.decimalPrecision = Math.max(0, Math.min(6, decimalPrecision));
        this.supportCannedCycles = supportCannedCycles;
        this.safeRetractCommand = (safeRetractCommand != null) ? safeRetractCommand : "G91 G28 Z0.";
        this.programEndCommand = (programEndCommand != null) ? programEndCommand : "M30";
    }

    public static ControllerProfile createIso6983Standard() {
        return new ControllerProfile(
            "ISO 6983 Standard Mill (Fanuc Compatible)",
            UnitMode.METRIC_G21,
            CoordinateMode.ABSOLUTE_G90,
            PlaneMode.XY_G17,
            "G54",
            true,
            true,
            10,
            3,
            true,
            "G91 G28 Z0.",
            "M30"
        );
    }

    public String formatNumber(double val) {
        return String.format(Locale.US, "%." + decimalPrecision + "f", val);
    }

    public String getName()                    { return name; }
    public UnitMode getUnitMode()              { return unitMode; }
    public CoordinateMode getCoordinateMode()  { return coordinateMode; }
    public PlaneMode getPlaneMode()            { return planeMode; }
    public String getDefaultWcs()              { return defaultWcs; }
    public boolean isModalSuppressionEnabled() { return modalSuppressionEnabled; }
    public boolean isLineNumbersEnabled()      { return lineNumbersEnabled; }
    public int getLineNumberIncrement()        { return lineNumberIncrement; }
    public int getDecimalPrecision()           { return decimalPrecision; }
    public boolean supportsCannedCycles()      { return supportCannedCycles; }
    public String getSafeRetractCommand()      { return safeRetractCommand; }
    public String getProgramEndCommand()        { return programEndCommand; }
}
