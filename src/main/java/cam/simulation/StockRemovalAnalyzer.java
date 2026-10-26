package cam.simulation;

import cam.geometry.CamPoint2D;
import cam.geometry.CamPolygon2D;

/**
 * Analytical verification engine for stock removal simulation.
 * Calculates volumetric accuracy, identifies uncut residual islands, and detects gouges.
 */
public class StockRemovalAnalyzer {

    public record AnalysisResult(
        double theoreticalVolumeMm3,
        double simulatedVolumeMm3,
        double volumetricAccuracyPercent,
        double maxResidualDepthMm,
        double maxGougeDepthMm,
        boolean hasUncutIslands,
        boolean hasGouges
    ) {}

    private StockRemovalAnalyzer() {}

    public static AnalysisResult analyzePocketRoughing(StockRemovalGrid grid, CamPolygon2D pocketBoundary,
                                                       double targetDepth, double boundaryAllowance, double toleranceMm) {
        if (grid == null || pocketBoundary == null) {
            throw new IllegalArgumentException("Grid and pocket boundary cannot be null.");
        }

        double targetZ = grid.getInitialZ() - targetDepth;
        double theoreticalVol = 0.0;
        CamPolygon2D targetOffset = pocketBoundary.createInwardOffset(boundaryAllowance);
        if (targetOffset != null) {
            theoreticalVol = targetOffset.getArea() * targetDepth;
        } else {
            theoreticalVol = pocketBoundary.getArea() * targetDepth;
        }

        double simulatedVol = grid.getRemovedVolume();
        double maxResidual = 0.0;
        double maxGouge = 0.0;
        boolean hasUncutIslands = false;
        boolean hasGouges = false;

        // Verify every grid cell
        for (int iy = 0; iy < grid.getGridCountY(); iy++) {
            double y = grid.getCellY(iy);
            for (int ix = 0; ix < grid.getGridCountX(); ix++) {
                double x = grid.getCellX(ix);
                CamPoint2D pt = new CamPoint2D(x, y);
                double cellH = grid.getHeight(ix, iy);

                boolean insideNominal = (targetOffset != null) ? targetOffset.contains(pt) : pocketBoundary.contains(pt);

                if (insideNominal) {
                    // Inside target clearing region: should be cut down to targetZ
                    double residual = cellH - targetZ;
                    if (residual > toleranceMm) {
                        hasUncutIslands = true;
                        maxResidual = Math.max(maxResidual, residual);
                    }
                } else if (!pocketBoundary.contains(pt) && pocketBoundary.distanceToBoundary(pt) > toleranceMm) {
                    // Outside pocket boundary completely: should remain uncut at initialZ
                    double gouge = grid.getInitialZ() - cellH;
                    if (gouge > toleranceMm) {
                        hasGouges = true;
                        maxGouge = Math.max(maxGouge, gouge);
                    }
                }

                // Check depth over-cut anywhere
                if (cellH < targetZ - toleranceMm) {
                    hasGouges = true;
                    maxGouge = Math.max(maxGouge, targetZ - cellH);
                }
            }
        }

        double accuracy = (theoreticalVol > 1e-6) ? (simulatedVol / theoreticalVol) * 100.0 : 100.0;
        return new AnalysisResult(theoreticalVol, simulatedVol, accuracy, maxResidual, maxGouge, hasUncutIslands, hasGouges);
    }
}
