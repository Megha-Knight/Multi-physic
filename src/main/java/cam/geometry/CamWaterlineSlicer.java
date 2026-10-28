package cam.geometry;

import cam.model.CamPoint3D;
import java.util.*;

/**
 * Deterministic constant-Z waterline slicing and contour assembly engine for 3D surfaces.
 * Slices triangulated meshes at specified Z levels, deduplicates segments, and reconstructs
 * topological closed loops and open boundary chains with bounded execution limits.
 */
public final class CamWaterlineSlicer {

    public static final double TOLERANCE = 1e-5;
    public static final int MAX_SEGMENTS = 50000;
    public static final int MAX_CONTOURS = 5000;

    private CamWaterlineSlicer() {}

    private record RawSeg(CamPoint2D p1, CamPoint2D p2) {
        public double length() { return p1.distanceTo(p2); }
        public boolean matches(RawSeg o) {
            return (p1.distanceTo(o.p1) < TOLERANCE && p2.distanceTo(o.p2) < TOLERANCE) ||
                   (p1.distanceTo(o.p2) < TOLERANCE && p2.distanceTo(o.p1) < TOLERANCE);
        }
    }

    public static List<CamContour2D> slice(CamTriangulatedSurface surface, double z) {
        if (surface == null || surface.getTriangleCount() == 0 || !Double.isFinite(z)) {
            return Collections.emptyList();
        }
        if (z < surface.getMinZ() - 1e-9 || z > surface.getMaxZ() + 1e-9) {
            return Collections.emptyList();
        }

        List<RawSeg> segments = new ArrayList<>();
        for (CamTriangle3D tri : surface.getTriangles()) {
            CamPoint3D[] pts = tri.intersectZPlane(z);
            if (pts.length == 2) {
                CamPoint2D p1 = new CamPoint2D(pts[0].x(), pts[0].y());
                CamPoint2D p2 = new CamPoint2D(pts[1].x(), pts[1].y());
                if (p1.distanceTo(p2) >= TOLERANCE) {
                    RawSeg seg = new RawSeg(p1, p2);
                    boolean isDup = false;
                    for (RawSeg existing : segments) {
                        if (existing.matches(seg)) { isDup = true; break; }
                    }
                    if (!isDup) {
                        segments.add(seg);
                        if (segments.size() > MAX_SEGMENTS) {
                            throw new IllegalStateException("Waterline slice exceeded maximum segment limit: " + MAX_SEGMENTS);
                        }
                    }
                }
            }
        }
        if (segments.isEmpty()) return Collections.emptyList();

        return chainSegments(segments);
    }

    private static List<CamContour2D> chainSegments(List<RawSeg> segments) {
        List<CamContour2D> contours = new ArrayList<>();
        boolean[] visited = new boolean[segments.size()];

        for (int i = 0; i < segments.size(); i++) {
            if (visited[i]) continue;
            visited[i] = true;

            List<CamPoint2D> chain = new ArrayList<>();
            chain.add(segments.get(i).p1());
            chain.add(segments.get(i).p2());

            boolean closed = false;
            boolean extended = true;

            while (extended && !closed) {
                extended = false;
                CamPoint2D tip = chain.get(chain.size() - 1);
                if (tip.distanceTo(chain.get(0)) < TOLERANCE && chain.size() > 2) {
                    closed = true;
                    break;
                }
                for (int j = 0; j < segments.size(); j++) {
                    if (visited[j]) continue;
                    RawSeg s = segments.get(j);
                    if (tip.distanceTo(s.p1()) < TOLERANCE) {
                        visited[j] = true;
                        chain.add(s.p2());
                        extended = true;
                        break;
                    } else if (tip.distanceTo(s.p2()) < TOLERANCE) {
                        visited[j] = true;
                        chain.add(s.p1());
                        extended = true;
                        break;
                    }
                }
            }

            if (!closed) {
                extended = true;
                while (extended) {
                    extended = false;
                    CamPoint2D head = chain.get(0);
                    for (int j = 0; j < segments.size(); j++) {
                        if (visited[j]) continue;
                        RawSeg s = segments.get(j);
                        if (head.distanceTo(s.p2()) < TOLERANCE) {
                            visited[j] = true;
                            chain.add(0, s.p1());
                            extended = true;
                            break;
                        } else if (head.distanceTo(s.p1()) < TOLERANCE) {
                            visited[j] = true;
                            chain.add(0, s.p2());
                            extended = true;
                            break;
                        }
                    }
                }
                if (chain.get(chain.size() - 1).distanceTo(chain.get(0)) < TOLERANCE && chain.size() > 2) {
                    closed = true;
                }
            }

            CamContour2D contour = buildContour(chain, closed);
            if (contour != null) {
                contours.add(contour);
                if (contours.size() > MAX_CONTOURS) {
                    throw new IllegalStateException("Waterline slice exceeded maximum contour limit: " + MAX_CONTOURS);
                }
            }
        }
        return contours;
    }

    private static CamContour2D buildContour(List<CamPoint2D> chain, boolean closed) {
        List<CamPoint2D> clean = new ArrayList<>();
        for (CamPoint2D pt : chain) {
            if (clean.isEmpty() || clean.get(clean.size() - 1).distanceTo(pt) >= TOLERANCE) {
                clean.add(pt);
            }
        }
        if (closed && clean.size() > 1 && clean.get(0).distanceTo(clean.get(clean.size() - 1)) < TOLERANCE) {
            clean.remove(clean.size() - 1);
        }

        List<CamPoint2D> simplified = simplifyCollinear(clean, closed);
        if (closed && simplified.size() >= 3) {
            try {
                return CamContour2D.closed(simplified);
            } catch (Exception ex) {
                try {
                    return CamContour2D.open(simplified);
                } catch (Exception ignored) {}
            }
        } else if (!closed && simplified.size() >= 2) {
            try {
                return CamContour2D.open(simplified);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static List<CamPoint2D> simplifyCollinear(List<CamPoint2D> pts, boolean closed) {
        if (pts.size() <= 2) return pts;
        List<CamPoint2D> res = new ArrayList<>(pts);
        boolean changed = true;
        while (changed && res.size() > (closed ? 3 : 2)) {
            changed = false;
            int n = res.size();
            for (int i = 0; i < (closed ? n : n - 2); i++) {
                CamPoint2D p1 = res.get(i), p2 = res.get((i + 1) % n), p3 = res.get((i + 2) % n);
                double cross = (p2.x() - p1.x()) * (p3.y() - p2.y()) - (p2.y() - p1.y()) * (p3.x() - p2.x());
                double d13 = p1.distanceTo(p3);
                if (Math.abs(cross) < 1e-5 && Math.abs(p1.distanceTo(p2) + p2.distanceTo(p3) - d13) < 1e-4) {
                    res.remove((i + 1) % n);
                    changed = true;
                    break;
                }
            }
        }
        return res;
    }
}
