package cam.operation;

import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.toolpath.Toolpath;

/**
 * Base interface for all validated CAM machining operations.
 */
public interface CamOperation {

    String getId();
    String getName();
    CamOperationType getOperationType();
    CamTool getTool();

    /**
     * Validates operation parameters against the manufacturing setup, stock, and tool.
     * @param setup current manufacturing setup
     * @return null if valid; diagnostic error string if invalid
     */
    String validate(ManufacturingSetup setup);

    /**
     * Deterministically generates the linear CNC toolpath.
     * @param setup current manufacturing setup
     * @return calculated Toolpath
     * @throws IllegalStateException if the operation is invalid
     */
    Toolpath generateToolpath(ManufacturingSetup setup);
}
