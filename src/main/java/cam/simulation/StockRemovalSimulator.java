package cam.simulation;

import cam.geometry.CamPoint2D;
import cam.geometry.CamSegment2D;
import cam.model.CamPoint3D;
import cam.tool.CamTool;
import cam.toolpath.Toolpath;
import cam.toolpath.ToolpathSegment;

/**
 * Headless simulation engine applying milling and drilling toolpaths
 * to a discrete StockRemovalGrid to compute material removal and remaining stock.
 */
public class StockRemovalSimulator {

    private final StockRemovalGrid grid;

    public StockRemovalSimulator(StockRemovalGrid grid) {
        if (grid == null) throw new IllegalArgumentException("StockRemovalGrid cannot be null.");
        this.grid = grid;
    }

    public StockRemovalGrid getGrid() { return grid; }

    public void simulateToolpath(Toolpath toolpath, CamTool tool) {
        if (toolpath == null || tool == null || toolpath.isEmpty()) return;

        double cutterRadius = tool.getRadius();
        boolean isDrill = tool.getType().isHolemaking();
        double tipAngle = tool.getTipAngleDegrees();
        double tanHalfAngle = Math.tan(Math.toRadians(tipAngle * 0.5));

        for (ToolpathSegment seg : toolpath.getSegments()) {
            if (!seg.motionType().isCutting()) continue;

            CamPoint3D pStart = seg.start();
            CamPoint3D pEnd = seg.end();

            double minX = Math.min(pStart.x(), pEnd.x()) - cutterRadius;
            double maxX = Math.max(pStart.x(), pEnd.x()) + cutterRadius;
            double minY = Math.min(pStart.y(), pEnd.y()) - cutterRadius;
            double maxY = Math.max(pStart.y(), pEnd.y()) + cutterRadius;

            int ixMin = Math.max(0, grid.getIndexX(minX));
            int ixMax = Math.min(grid.getGridCountX() - 1, grid.getIndexX(maxX));
            int iyMin = Math.max(0, grid.getIndexY(minY));
            int iyMax = Math.min(grid.getGridCountY() - 1, grid.getIndexY(maxY));

            CamSegment2D seg2d = new CamSegment2D(new CamPoint2D(pStart.x(), pStart.y()), new CamPoint2D(pEnd.x(), pEnd.y()));

            for (int iy = iyMin; iy <= iyMax; iy++) {
                double cy = grid.getCellY(iy);
                for (int ix = ixMin; ix <= ixMax; ix++) {
                    double cx = grid.getCellX(ix);
                    CamPoint2D pt = new CamPoint2D(cx, cy);

                    double dist = seg2d.distanceTo(pt);
                    if (dist <= cutterRadius + 1e-6) {
                        double cutZ = Math.min(pStart.z(), pEnd.z());

                        if (isDrill && tipAngle < 180.0 && tanHalfAngle > 1e-4) {
                            double distCenter = pEnd.distanceXY(new CamPoint3D(cx, cy, 0));
                            if (distCenter <= cutterRadius) {
                                double tipOffset = distCenter / tanHalfAngle;
                                cutZ = pEnd.z() + tipOffset;
                            }
                        }
                        grid.cutAt(ix, iy, cutZ);
                    }
                }
            }
        }
    }
}
