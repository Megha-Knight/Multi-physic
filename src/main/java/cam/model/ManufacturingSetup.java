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
    private final java.util.Map<String, MachineCoordinateSystem> workOffsets = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, cam.tool.CamTool> toolTable = new java.util.LinkedHashMap<>();

    public ManufacturingSetup(String id, String name, String targetPartId,
                              StockDimensions stock, MachineCoordinateSystem mcs, SafetyPlane safetyPlane) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Manufacturing Setup 1";
        this.targetPartId = (targetPartId != null) ? targetPartId : "";
        this.stock = stock;
        this.mcs = (mcs != null) ? mcs : new MachineCoordinateSystem("G54", CamPoint3D.ZERO);
        this.safetyPlane = (safetyPlane != null) ? safetyPlane : new SafetyPlane(25.0, 5.0);
        this.workOffsets.put(this.mcs.getName().toUpperCase(java.util.Locale.US), this.mcs);
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
    public void setMcs(MachineCoordinateSystem m) {
        this.mcs = m;
        if (m != null) this.workOffsets.put(m.getName().toUpperCase(java.util.Locale.US), m);
    }
    public SafetyPlane getSafetyPlane()         { return safetyPlane; }
    public void setSafetyPlane(SafetyPlane sp)  { this.safetyPlane = sp; }

    public MachineCoordinateSystem getCoordinateSystem(String name) {
        if (name == null) return null;
        String key = name.toUpperCase(java.util.Locale.US);
        if (workOffsets.containsKey(key)) return workOffsets.get(key);
        if (mcs != null && key.equals(mcs.getName().toUpperCase(java.util.Locale.US))) return mcs;
        return null;
    }

    public void setCoordinateSystem(String name, MachineCoordinateSystem cs) {
        if (name != null && cs != null) workOffsets.put(name.toUpperCase(java.util.Locale.US), cs);
    }

    public void setTool(int toolNumber, cam.tool.CamTool tool) {
        if (tool != null) toolTable.put(toolNumber, tool);
    }

    public cam.tool.CamTool findTool(Integer toolNumber) {
        if (toolNumber == null) return null;
        if (toolTable.containsKey(toolNumber)) return toolTable.get(toolNumber);
        for (CamOperation op : operations) {
            if (op.getTool() != null) return op.getTool();
        }
        return null;
    }

    public double resolveToolRadius(Integer toolNumber, double fallbackRadius) {
        cam.tool.CamTool t = findTool(toolNumber);
        return (t != null) ? t.getRadius() : fallbackRadius;
    }

    public List<CamOperation> getOperations()   { return Collections.unmodifiableList(operations); }
    public void addOperation(CamOperation op)   { if (op != null) operations.add(op); }
    public void removeOperation(String opId)    { operations.removeIf(op -> op.getId().equals(opId)); }
    public void clearOperations()               { operations.clear(); }

    private StockDimensions fixtureBounds = null;
    public StockDimensions getFixtureBounds()   { return fixtureBounds; }
    public void setFixtureBounds(StockDimensions fb) { this.fixtureBounds = fb; }
    public boolean hasFixtureGeometry()         { return fixtureBounds != null; }
}
