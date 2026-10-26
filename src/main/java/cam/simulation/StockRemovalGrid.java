package cam.simulation;

import cam.model.StockDimensions;

/**
 * Headless discrete Z-heightfield/dexel model for 2.5D/3-axis CAM material removal.
 * Tracks remaining material, volumetric removal, and spatial grid bounds.
 */
public class StockRemovalGrid {

    private final double minX, minY, maxX, maxY;
    private final double initialZ, bottomZ;
    private final double resolutionMm;
    private final int nx, ny;
    private final double[] heights;

    public StockRemovalGrid(StockDimensions stock, double resolutionMm) {
        if (stock == null) throw new IllegalArgumentException("StockDimensions cannot be null.");
        if (resolutionMm <= 1e-4) throw new IllegalArgumentException("Resolution must be strictly positive.");

        this.minX = stock.getMin().x();
        this.minY = stock.getMin().y();
        this.maxX = stock.getMax().x();
        this.maxY = stock.getMax().y();
        this.initialZ = stock.getTopZ();
        this.bottomZ = stock.getBottomZ();
        this.resolutionMm = resolutionMm;

        this.nx = (int) Math.ceil((maxX - minX) / resolutionMm) + 1;
        this.ny = (int) Math.ceil((maxY - minY) / resolutionMm) + 1;
        this.heights = new double[nx * ny];

        for (int i = 0; i < heights.length; i++) {
            heights[i] = initialZ;
        }
    }

    public double getResolutionMm() { return resolutionMm; }
    public double getMinX()          { return minX; }
    public double getMinY()          { return minY; }
    public double getMaxX()          { return maxX; }
    public double getMaxY()          { return maxY; }
    public double getInitialZ()      { return initialZ; }
    public double getBottomZ()       { return bottomZ; }
    public int getGridCountX()       { return nx; }
    public int getGridCountY()       { return ny; }
    public int getTotalCells()       { return nx * ny; }

    public double getCellX(int ix) { return minX + ix * resolutionMm; }
    public double getCellY(int iy) { return minY + iy * resolutionMm; }

    public int getIndexX(double x) {
        if (x < minX || x > maxX) return -1;
        return (int) Math.round((x - minX) / resolutionMm);
    }

    public int getIndexY(double y) {
        if (y < minY || y > maxY) return -1;
        return (int) Math.round((y - minY) / resolutionMm);
    }

    public double getHeight(int ix, int iy) {
        if (ix < 0 || ix >= nx || iy < 0 || iy >= ny) return Double.NaN;
        return heights[iy * nx + ix];
    }

    public double sampleHeight(double x, double y) {
        int ix = getIndexX(x);
        int iy = getIndexY(y);
        return getHeight(ix, iy);
    }

    public void cutAt(int ix, int iy, double cutZ) {
        if (ix < 0 || ix >= nx || iy < 0 || iy >= ny) return;
        int idx = iy * nx + ix;
        if (cutZ < heights[idx]) {
            heights[idx] = Math.max(bottomZ, cutZ);
        }
    }

    public double getCellArea() {
        return ((maxX - minX) * (maxY - minY)) / (double) (nx * ny);
    }

    public double getRemovedVolume() {
        double vol = 0;
        for (double h : heights) {
            vol += (initialZ - h);
        }
        return vol * getCellArea();
    }

    public double getRemainingVolume() {
        double vol = 0;
        for (double h : heights) {
            vol += (h - bottomZ);
        }
        return vol * getCellArea();
    }

    public double getInitialVolume() {
        return (maxX - minX) * (maxY - minY) * (initialZ - bottomZ);
    }
}
