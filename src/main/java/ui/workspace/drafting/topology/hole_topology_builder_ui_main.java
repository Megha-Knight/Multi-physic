package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.holes.hole_intersection_helper_ui_main;
import java.util.ArrayList;
import java.util.List;

/**
 * hole_topology_builder_ui_main.java
 * Generates deterministic topology-aware derived faces for CAD bodies with holes.
 */
public final class hole_topology_builder_ui_main {

    private hole_topology_builder_ui_main() {}

    public static void populateDerivedFaces(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;
        List<hole_feature_ui_main> holes = shape.getAllEffectiveHoles();
        if (holes == null || holes.isEmpty()) return;

        for (topology_face_ui_main baseFace : body.getFaces()) {
            face_kind_ui_main kind = baseFace.getFaceKind();
            List<hole_feature_ui_main> faceHoles = getHolesOnFace(holes, kind);
            List<hole_feature_ui_main> exitHoles = getThroughHolesExitingFace(holes, kind);

            if (faceHoles.isEmpty() && exitHoles.isEmpty()) continue;

            validateHoles(shape, body, faceHoles);

            topology_derived_face_ui_main remaining = buildRemainingFace(body, kind, baseFace, faceHoles, exitHoles);
            body.addDerivedFace(remaining);

            for (hole_feature_ui_main h : faceHoles) {
                if (h.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID) continue;
                buildHoleRegions(body, shape, kind, h);
            }
        }
    }

    private static void validateHoles(shape_item_ui_main shape, topology_body_ui_main body, List<hole_feature_ui_main> holes) {
        for (hole_feature_ui_main h : holes) {
            boundary_relationship_ui_main rel = edge_machining_analyzer_ui_main.analyzeHole(shape, body, h);
            h.setBoundaryRelationship(rel);
            if (rel != null && !rel.isValid()) {
                h.setState(ui.workspace.drafting.features.feature_state_ui_main.INVALID);
                h.setDiagnosticMessage(rel.getDiagnosticMessage());
            }
        }
    }

    private static List<hole_feature_ui_main> getHolesOnFace(List<hole_feature_ui_main> holes, face_kind_ui_main kind) {
        List<hole_feature_ui_main> list = new ArrayList<>();
        for (hole_feature_ui_main h : holes) if (h.getFaceKind() == kind) list.add(h);
        return list;
    }

    private static List<hole_feature_ui_main> getThroughHolesExitingFace(List<hole_feature_ui_main> holes, face_kind_ui_main exitKind) {
        List<hole_feature_ui_main> list = new ArrayList<>();
        for (hole_feature_ui_main h : holes) {
            if (h.isThroughAll() && hole_intersection_helper_ui_main.isOpposingFace(h.getFaceKind(), exitKind)) {
                list.add(h);
            }
        }
        return list;
    }

    private static topology_derived_face_ui_main buildRemainingFace(topology_body_ui_main body, face_kind_ui_main kind,
                                                                    topology_face_ui_main baseFace,
                                                                    List<hole_feature_ui_main> entryHoles,
                                                                    List<hole_feature_ui_main> exitHoles) {
        String id = body.getId() + ":F:" + kind.name() + ":REMAINING";
        topology_derived_face_ui_main rem = new topology_derived_face_ui_main(
            body.getId(), id, "Remaining " + kind.getLabel(), kind, null,
            hole_region_kind_ui_main.REMAINING_FACE, baseFace.getWidth(), baseFace.getHeight(), 0.0, 0.0, new Point3D(0, 0, 0)
        );
        rem.addLoop(topology_boundary_loop_ui_main.createRectangular("OUTER", baseFace.getWidth(), baseFace.getHeight()));

        for (hole_feature_ui_main h : entryHoles) {
            if (h.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                rem.addLoop(topology_boundary_loop_ui_main.createCircular("IN_HOLE_" + h.getId(), false, new Point3D(h.getU(), h.getV(), 0), h.getRadius()));
            }
        }
        for (hole_feature_ui_main h : exitHoles) {
            if (h.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                rem.addLoop(topology_boundary_loop_ui_main.createCircular("EXIT_HOLE_" + h.getId(), false, new Point3D(h.getU(), h.getV(), 0), h.getRadius()));
            }
        }
        reattachAppearance(rem);
        return rem;
    }

    private static void buildHoleRegions(topology_body_ui_main body, shape_item_ui_main shape, face_kind_ui_main kind, hole_feature_ui_main h) {
        double thick = getSolidThickness(shape, kind);
        Point3D loc = new Point3D(h.getU(), h.getV(), 0);

        if (h.isThroughAll()) {
            String wallId = body.getId() + ":F:" + kind.name() + ":BORE_WALL:" + h.getId();
            topology_derived_face_ui_main bore = new topology_derived_face_ui_main(
                body.getId(), wallId, "Bore Wall (" + h.getName() + ")", kind, h.getId(),
                hole_region_kind_ui_main.BORE_WALL, h.getDiameter() * Math.PI, thick, thick, h.getRadius(), loc
            );
            reattachAppearance(bore);
            body.addDerivedFace(bore);
        } else {
            String wallId = body.getId() + ":F:" + kind.name() + ":HOLE_WALL:" + h.getId();
            topology_derived_face_ui_main wall = new topology_derived_face_ui_main(
                body.getId(), wallId, "Hole Wall (" + h.getName() + ")", kind, h.getId(),
                hole_region_kind_ui_main.HOLE_WALL, h.getDiameter() * Math.PI, h.getDepth(), h.getDepth(), h.getRadius(), loc
            );
            reattachAppearance(wall);
            body.addDerivedFace(wall);

            String floorId = body.getId() + ":F:" + kind.name() + ":HOLE_FLOOR:" + h.getId();
            topology_derived_face_ui_main floor = new topology_derived_face_ui_main(
                body.getId(), floorId, "Hole Floor (" + h.getName() + ")", kind, h.getId(),
                hole_region_kind_ui_main.HOLE_FLOOR, h.getDiameter(), h.getDiameter(), h.getDepth(), h.getRadius(),
                new Point3D(h.getU(), h.getV(), -h.getDepth())
            );
            floor.addLoop(topology_boundary_loop_ui_main.createCircular("FLOOR_LOOP", true, loc, h.getRadius()));
            reattachAppearance(floor);
            body.addDerivedFace(floor);
        }
    }

    private static double getSolidThickness(shape_item_ui_main s, face_kind_ui_main kind) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        if (s.getType() == ui.workspace.shapes.basic_shapes_ui_main.CYLINDER || s.getType() == ui.workspace.shapes.basic_shapes_ui_main.CONE) {
            double r = Math.max(0.1, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
            double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
            return kind.isLateral() ? r * 2.0 : h;
        }
        double w = Math.abs(p2.getX() - p1.getX()), h = Math.abs(p2.getY() - p1.getY()), d = Math.abs(p2.getZ() - p1.getZ());
        if (h <= 0.1) h = Math.max(6.0, Math.min(w, d) * 0.5);
        return switch (kind) {
            case TOP, BOTTOM, TOP_CAP, BOTTOM_CAP, BASE_CAP -> h;
            case FRONT, BACK -> d;
            case LEFT, RIGHT -> w;
            default -> Math.max(w, Math.max(h, d));
        };
    }

    private static void reattachAppearance(topology_derived_face_ui_main df) {
        var app = shape_face_appearance_helper_ui_main.getAppearance(df.getBodyId(), df.getId());
        if (app != null) df.setAppearanceOverride(app);
    }
}
