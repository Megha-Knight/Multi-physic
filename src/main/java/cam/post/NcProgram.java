package cam.post;

import java.util.Collections;
import java.util.List;

/**
 * Output NC program container holding ordered G-code blocks,
 * program identification, and emission statistics.
 */
public class NcProgram {

    private final String programName;
    private final List<String> blocks;
    private final String controllerProfileName;
    private final int totalBlocks;
    private final int toolChangeCount;

    public NcProgram(String programName, List<String> blocks, String controllerProfileName, int toolChangeCount) {
        this.programName = (programName != null && !programName.isBlank()) ? programName : "PROG0001";
        this.blocks = (blocks != null) ? List.copyOf(blocks) : Collections.emptyList();
        this.controllerProfileName = controllerProfileName;
        this.totalBlocks = this.blocks.size();
        this.toolChangeCount = toolChangeCount;
    }

    public String getProgramName()             { return programName; }
    public List<String> getBlocks()            { return blocks; }
    public String getControllerProfileName()   { return controllerProfileName; }
    public int getTotalBlocks()                { return totalBlocks; }
    public int getToolChangeCount()            { return toolChangeCount; }

    public String toGcodeText() {
        return String.join(System.lineSeparator(), blocks);
    }
}
