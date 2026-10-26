package cam.model;

/**
 * Safety Clearance Plane and Retract Levels for collision-free transit in CAM.
 */
public class SafetyPlane {

    private final double clearanceZ;
    private final double retractZ;

    public SafetyPlane(double clearanceZ, double retractZ) {
        if (clearanceZ < retractZ) {
            throw new IllegalArgumentException("Clearance Z (" + clearanceZ + ") cannot be below Retract Z (" + retractZ + ").");
        }
        this.clearanceZ = clearanceZ;
        this.retractZ = retractZ;
    }

    public static SafetyPlane fromStockTop(double stockTopZ, double clearanceOffset, double retractOffset) {
        double ret = stockTopZ + Math.max(1.0, retractOffset);
        double clr = ret + Math.max(1.0, clearanceOffset);
        return new SafetyPlane(clr, ret);
    }

    public double getClearanceZ() { return clearanceZ; }
    public double getRetractZ()   { return retractZ; }

    public boolean isSafe(CamPoint3D pt) {
        if (pt == null) return false;
        return pt.z() >= retractZ - 1e-6;
    }
}
