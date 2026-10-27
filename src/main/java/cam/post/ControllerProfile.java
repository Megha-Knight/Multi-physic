package cam.post;

import java.util.Locale;

/**
 * Controller configuration profile specifying CNC syntax, coordinate conventions,
 * modal suppression, canned cycle capabilities, and safety blocks.
 * Modeled for ISO 6983 3-axis vertical machining center profiles.
 */
public class ControllerProfile {

    public enum UnitMode { METRIC_G21, INCH_G20 }
    public enum CoordinateMode { ABSOLUTE_G90, INCREMENTAL_G91 }
    public enum PlaneMode { XY_G17, XZ_G18, YZ_G19 }
    public enum RetractStrategy { G28_REFERENCE_RETURN, G53_MACHINE_COORDS, CLEARANCE_PLANE }

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
    private final RetractStrategy retractStrategy;
    private final String safeRetractCommand;
    private final String programEndCommand;
    private final boolean emitToolLengthComp;
    private final boolean emitToolLengthCancel;

    public ControllerProfile(String name, UnitMode unitMode, CoordinateMode coordinateMode,
                             PlaneMode planeMode, String defaultWcs, boolean modalSuppressionEnabled,
                             boolean lineNumbersEnabled, int lineNumberIncrement, int decimalPrecision,
                             boolean supportCannedCycles, RetractStrategy retractStrategy,
                             String safeRetractCommand, String programEndCommand,
                             boolean emitToolLengthComp, boolean emitToolLengthCancel) {
        this.name = (name != null) ? name : "ISO 6983 Standard 3-Axis VMC Profile";
        this.unitMode = (unitMode != null) ? unitMode : UnitMode.METRIC_G21;
        this.coordinateMode = (coordinateMode != null) ? coordinateMode : CoordinateMode.ABSOLUTE_G90;
        this.planeMode = (planeMode != null) ? planeMode : PlaneMode.XY_G17;
        this.defaultWcs = (defaultWcs != null && !defaultWcs.isBlank()) ? defaultWcs : "G54";
        this.modalSuppressionEnabled = modalSuppressionEnabled;
        this.lineNumbersEnabled = lineNumbersEnabled;
        this.lineNumberIncrement = Math.max(1, lineNumberIncrement);
        this.decimalPrecision = Math.max(0, Math.min(6, decimalPrecision));
        this.supportCannedCycles = supportCannedCycles;
        this.retractStrategy = (retractStrategy != null) ? retractStrategy : RetractStrategy.G28_REFERENCE_RETURN;
        this.safeRetractCommand = (safeRetractCommand != null) ? safeRetractCommand : "G91 G28 Z0.";
        this.programEndCommand = (programEndCommand != null) ? programEndCommand : "M30";
        this.emitToolLengthComp = emitToolLengthComp;
        this.emitToolLengthCancel = emitToolLengthCancel;
    }

    public static ControllerProfile createIso6983Standard() {
        return new ControllerProfile(
            "ISO 6983 3-Axis VMC Profile (Fanuc-Style Syntax)",
            UnitMode.METRIC_G21,
            CoordinateMode.ABSOLUTE_G90,
            PlaneMode.XY_G17,
            "G54",
            true,
            true,
            10,
            3,
            true,
            RetractStrategy.G28_REFERENCE_RETURN,
            "G91 G28 Z0.",
            "M30",
            true,
            true
        );
    }

    public static ControllerProfile createModernG53Profile() {
        return new ControllerProfile(
            "ISO 6983 Modern VMC Profile (G53 Retract)",
            UnitMode.METRIC_G21,
            CoordinateMode.ABSOLUTE_G90,
            PlaneMode.XY_G17,
            "G54",
            true,
            true,
            10,
            3,
            true,
            RetractStrategy.G53_MACHINE_COORDS,
            "G53 G00 Z0.",
            "M30",
            true,
            true
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
    public RetractStrategy getRetractStrategy(){ return retractStrategy; }
    public String getSafeRetractCommand()      { return safeRetractCommand; }
    public String getProgramEndCommand()        { return programEndCommand; }
    public boolean isEmitToolLengthComp()      { return emitToolLengthComp; }
    public boolean isEmitToolLengthCancel()    { return emitToolLengthCancel; }
}
