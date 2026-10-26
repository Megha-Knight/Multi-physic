package cam.model;

/**
 * Machine Coordinate System (MCS / Work Coordinate System, e.g., G54, G55).
 * Defines workpiece origin and orientation.
 */
public class MachineCoordinateSystem {

    private final String name;
    private final CamPoint3D origin;
    private final CamVector3D axisX;
    private final CamVector3D axisY;
    private final CamVector3D axisZ;

    public MachineCoordinateSystem(String name, CamPoint3D origin) {
        this(name, origin, CamVector3D.UNIT_X, CamVector3D.UNIT_Y, CamVector3D.UNIT_Z);
    }

    public MachineCoordinateSystem(String name, CamPoint3D origin,
                                   CamVector3D axisX, CamVector3D axisY, CamVector3D axisZ) {
        this.name = (name != null && !name.isBlank()) ? name : "G54";
        this.origin = (origin != null) ? origin : CamPoint3D.ZERO;
        this.axisX = (axisX != null) ? axisX.normalize() : CamVector3D.UNIT_X;
        this.axisY = (axisY != null) ? axisY.normalize() : CamVector3D.UNIT_Y;
        this.axisZ = (axisZ != null) ? axisZ.normalize() : CamVector3D.UNIT_Z;
    }

    public String getName()          { return name; }
    public CamPoint3D getOrigin()    { return origin; }
    public CamVector3D getAxisX()    { return axisX; }
    public CamVector3D getAxisY()    { return axisY; }
    public CamVector3D getAxisZ()    { return axisZ; }

    public CamPoint3D toWorkCoords(CamPoint3D worldPt) {
        if (worldPt == null) return CamPoint3D.ZERO;
        CamVector3D d = worldPt.subtract(origin);
        return new CamPoint3D(d.dot(axisX), d.dot(axisY), d.dot(axisZ));
    }

    public CamPoint3D toWorldCoords(CamPoint3D workPt) {
        if (workPt == null) return origin;
        return origin.add(axisX.multiply(workPt.x()))
                     .add(axisY.multiply(workPt.y()))
                     .add(axisZ.multiply(workPt.z()));
    }
}
