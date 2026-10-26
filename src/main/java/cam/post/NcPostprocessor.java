package cam.post;

import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.toolpath.Toolpath;

/**
 * Headless NC postprocessor interface translating CAM operations and toolpaths
 * into validated, controller-specific G-code programs.
 */
public interface NcPostprocessor {

    ControllerProfile getProfile();

    /**
     * Translates a single toolpath and tool into an NC program.
     */
    NcProgram postprocess(ManufacturingSetup setup, Toolpath toolpath, CamTool tool, int toolNumber);

    /**
     * Translates an entire manufacturing setup and its aggregated operations into an NC program.
     */
    NcProgram postprocess(ManufacturingSetup setup);
}
