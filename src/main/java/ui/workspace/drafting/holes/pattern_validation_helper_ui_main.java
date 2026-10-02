package ui.workspace.drafting.holes;

import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

public final class pattern_validation_helper_ui_main {

    private pattern_validation_helper_ui_main() {}

    public static boolean isValidSeed(shape_item_ui_main shape, hole_feature_ui_main seed) {
        if (shape == null || seed == null || !seed.isValid()) return false;
        if (!shape.getId().equals(seed.getOwnerShapeId())) return false;
        if (seed.getId() != null && seed.getId().contains("-inst-")) return false;
        boolean inHoles = false;
        for (hole_feature_ui_main h : shape.getHoles()) {
            if (h.getId().equals(seed.getId())) { inHoles = true; break; }
        }
        if (!inHoles) return false;
        for (hole_pattern_ui_main p : shape.getPatterns()) {
            if (p.getId().equals(seed.getId())) return false;
        }
        return true;
    }

    public static boolean validateAgainstHost(shape_item_ui_main shape, hole_pattern_ui_main pattern,
                                              hole_feature_ui_main seed, StringBuilder errorOut) {
        if (!isValidSeed(shape, seed)) {
            if (errorOut != null) errorOut.append("Invalid or missing pattern seed hole.");
            return false;
        }
        if (pattern == null || !pattern.isValid(seed)) {
            if (errorOut != null) errorOut.append("Pattern definition is invalid.");
            return false;
        }
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, seed.getFaceKind());
        double outerR = seed.getOuterRadius();
        List<hole_pattern_ui_main.Pos2D> positions = pattern.getAllPositions(seed);
        for (hole_pattern_ui_main.Pos2D pos : positions) {
            if (!face_geometry_helper_ui_main.fitsWithinFace(info, pos.u(), pos.v(), outerR)) {
                if (errorOut != null) errorOut.append("Pattern instances exceed face bounds.");
                return false;
            }
        }
        if (pattern.hasOverlappingInstances(seed)) {
            if (errorOut != null) errorOut.append("Pattern instances overlap each other.");
            return false;
        }
        for (int i = 0; i < positions.size(); i++) {
            hole_pattern_ui_main.Pos2D p = positions.get(i);
            for (hole_feature_ui_main other : shape.getHoles()) {
                if (other.getId().equals(seed.getId())) {
                    if (i == 0) continue;
                }
                if (other.getFaceKind() == seed.getFaceKind()) {
                    double dist = Math.hypot(p.u() - other.getU(), p.v() - other.getV());
                    if (dist < outerR + other.getOuterRadius() - 1e-4) {
                        if (errorOut != null) errorOut.append("Pattern instance overlaps existing hole.");
                        return false;
                    }
                }
            }
            for (hole_pattern_ui_main otherPat : shape.getPatterns()) {
                if (otherPat.getId().equals(pattern.getId())) continue;
                hole_feature_ui_main otherSeed = null;
                for (hole_feature_ui_main h : shape.getHoles()) {
                    if (h.getId().equals(otherPat.getSeedHoleId())) { otherSeed = h; break; }
                }
                if (otherSeed == null) continue;
                for (hole_feature_ui_main inst : otherPat.generateDerivedHoles(otherSeed)) {
                    if (inst.getFaceKind() == seed.getFaceKind()) {
                        double dist = Math.hypot(p.u() - inst.getU(), p.v() - inst.getV());
                        if (dist < outerR + inst.getOuterRadius() - 1e-4) {
                            if (errorOut != null) errorOut.append("Pattern instance overlaps another pattern instance.");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
