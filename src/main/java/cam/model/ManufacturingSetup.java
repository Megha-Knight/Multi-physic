package cam.model;

import cam.operation.CamOperation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Master Manufacturing Setup model aggregating finished part reference,
 * raw stock dimensions, work coordinate system (MCS), and safety plane.
 */
public class ManufacturingSetup {

    private final String id;
    private String name;
    private final String targetPartId;
    private StockDimensions stock;
    private MachineCoordinateSystem mcs;
    private SafetyPlane safetyPlane;
    private final List<CamOperation> operations = new ArrayList<>();

    public ManufacturingSetup(String id, String name, String targetPartId,
                              StockDimensions stock, MachineCoordinateSystem mcs, SafetyPlane safetyPlane) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Manufacturing Setup 1";
        this.targetPartId = (targetPartId != null) ? targetPartId : "";
        this.stock = stock;
        this.mcs = (mcs != null) ? mcs : new MachineCoordinateSystem("G54", CamPoint3D.ZERO);
        this.safetyPlane = (safetyPlane != null) ? safetyPlane : new SafetyPlane(25.0, 5.0);
    }

    public static ManufacturingSetup createDefault(String partId, double w, double d, double h) {
        StockDimensions stock = StockDimensions.fromFinishedPart(-w*0.5, -d*0.5, -h, w*0.5, d*0.5, 0.0, 5.0, 2.0, 5.0);
        SafetyPlane safety = SafetyPlane.fromStockTop(stock.getTopZ(), 20.0, 5.0);
        MachineCoordinateSystem mcs = new MachineCoordinateSystem("G54", new CamPoint3D(0, 0, stock.getTopZ()));
        return new ManufacturingSetup(null, "Setup 1", partId, stock, mcs, safety);
    }

    public String getId()                       { return id; }
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public String getTargetPartId()             { return targetPartId; }
    public StockDimensions getStock()           { return stock; }
    public void setStock(StockDimensions s)     { this.stock = s; }
    public MachineCoordinateSystem getMcs()     { return mcs; }
    public void setMcs(MachineCoordinateSystem m) { this.mcs = m; }
    public SafetyPlane getSafetyPlane()         { return safetyPlane; }
    public void setSafetyPlane(SafetyPlane sp)  { this.safetyPlane = sp; }

    public List<CamOperation> getOperations()   { return Collections.unmodifiableList(operations); }
    public void addOperation(CamOperation op)   { if (op != null) operations.add(op); }
    public void removeOperation(String opId)    { operations.removeIf(op -> op.getId().equals(opId)); }
    public void clearOperations()               { operations.clear(); }

    private StockDimensions fixtureBounds = null;
    public StockDimensions getFixtureBounds()   { return fixtureBounds; }
    public void setFixtureBounds(StockDimensions fb) { this.fixtureBounds = fb; }
    public boolean hasFixtureGeometry()         { return fixtureBounds != null; }
}
