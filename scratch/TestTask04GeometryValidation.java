package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ui.File_Types.document_serializer_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.edge_picker_ui_main;
import ui.workspace.drafting.features.feature_regeneration_engine_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.*;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestTask04GeometryValidation {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean cond, String msg) {
        testsTotal++;
        if (cond) {
            testsPassed++;
            System.out.println("  [PASS] " + msg);
        } else {
            System.err.println("  [FAIL] " + msg);
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  TASK 04 SKETCH & COMPUTATIONAL GEOMETRY SUITE  ");
        System.out.println("=================================================");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }

        try {
            testSketchPrimitivesAndEditing();
            testSnappingAccuracyAndPriorities();
            testPrincipalPlanesAndFaceBasisOrthonormality();
            testCoordinateTransformsAndInverses();
            testRaycastFaceAndEdgeSelection();
            testSolidModelingAndFeatureRegeneration();
            testProfileAnalysisAndSelfIntersectionValidation();
            testInteractiveToolStateResetAndCancellation();
            testDocumentPersistenceRoundTrip();

            System.out.println("\n-------------------------------------------------");
            System.out.printf("ALL TESTS PASSED: %d / %d assertions passed cleanly.%n", testsPassed, testsTotal);
            System.out.println("-------------------------------------------------");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\nTEST SUITE FAILED with error:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    // 1. Creating and editing sketch entities (Line, Circle, Arc, Rect, CenterRect)
    private static void testSketchPrimitivesAndEditing() {
        System.out.println("\n[1] Testing Sketch Primitives Creation & Editing...");

        // Line
        sketch_line_ui_main line = new sketch_line_ui_main("l1", 0, 0, 10, 0);
        assertTrue(line.isValid(), "Line with length 10 is valid");
        assertTrue(Math.abs(line.getLength() - 10.0) < 1e-4, "Line length is 10.0");
        assertTrue(line.getMidpoint().equalsWithTolerance(new sketch_point_2d_ui_main(5, 0)), "Line midpoint is (5,0)");
        line.translate(2, 3);
        assertTrue(line.getStart().equalsWithTolerance(new sketch_point_2d_ui_main(2, 3)), "Line translated start is (2,3)");
        assertTrue(line.getEnd().equalsWithTolerance(new sketch_point_2d_ui_main(12, 3)), "Line translated end is (12,3)");

        // Circle
        sketch_circle_ui_main circ = new sketch_circle_ui_main("c1", 10, 20, 5);
        assertTrue(circ.isValid(), "Circle with radius 5 is valid");
        assertTrue(circ.containsPoint(new sketch_point_2d_ui_main(15, 20), 1e-3), "Circle boundary contains point (15,20)");
        assertTrue(circ.isInside(new sketch_point_2d_ui_main(11, 21)), "Point (11,21) is inside circle");
        circ.translate(-5, -5);
        assertTrue(circ.getCenter().equalsWithTolerance(new sketch_point_2d_ui_main(5, 15)), "Circle center translated to (5,15)");

        // Arc
        sketch_arc_ui_main arc = new sketch_arc_ui_main("a1", new sketch_point_2d_ui_main(0, 0), 10, 0, 90);
        assertTrue(arc.isValid(), "90-degree Arc is valid");
        assertTrue(Math.abs(arc.getAngularSpan() - 90.0) < 1e-4, "Arc angular span is 90 degrees");
        assertTrue(arc.getStartPoint().equalsWithTolerance(new sketch_point_2d_ui_main(10, 0)), "Arc start point is (10,0)");
        assertTrue(arc.getEndPoint().equalsWithTolerance(new sketch_point_2d_ui_main(0, 10)), "Arc end point is (0,10)");

        // Rectangle
        sketch_rect_ui_main rect = new sketch_rect_ui_main("r1", 0, 0, 20, 10);
        assertTrue(rect.isValid(), "Rectangle 20x10 is valid");
        assertTrue(Math.abs(rect.getWidth() - 20.0) < 1e-4, "Rect width is 20.0");
        assertTrue(Math.abs(rect.getHeight() - 10.0) < 1e-4, "Rect height is 10.0");
        assertTrue(rect.getCenter().equalsWithTolerance(new sketch_point_2d_ui_main(10, 5)), "Rect center is (10,5)");

        // Degenerate geometry checks
        sketch_line_ui_main degenLine = new sketch_line_ui_main("dl", 5, 5, 5, 5);
        assertTrue(!degenLine.isValid(), "Zero-length line is invalid");
        sketch_circle_ui_main degenCirc = new sketch_circle_ui_main("dc", 0, 0, 0);
        assertTrue(!degenCirc.isValid() || degenCirc.getRadius() >= 1e-4, "Zero-radius circle is rejected or clamped");
        sketch_rect_ui_main degenRect = new sketch_rect_ui_main("dr", 10, 10, 10, 20);
        assertTrue(!degenRect.isValid(), "Zero-width rectangle is invalid");
    }

    // 2. Snapping accuracy and geometric references
    private static void testSnappingAccuracyAndPriorities() {
        System.out.println("\n[2] Testing Snapping Accuracy & Inferences...");
        List<sketch_entity_ui_main> entities = new ArrayList<>();
        sketch_line_ui_main l = new sketch_line_ui_main("l1", 0, 0, 50, 0);
        sketch_circle_ui_main c = new sketch_circle_ui_main("c1", 50, 50, 20);
        entities.add(l);
        entities.add(c);

        // Snap to Origin
        var snapOrig = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(0.5, 0.2), entities, 5.0);
        assertTrue(snapOrig != null && snapOrig.type() == sketch_snap_type_ui_main.ORIGIN,
                   "Cursor near (0,0) snaps to ORIGIN");

        // Snap to Line Endpoint
        var snapEnd = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(49.2, 0.3), entities, 5.0);
        assertTrue(snapEnd != null && snapEnd.type() == sketch_snap_type_ui_main.ENDPOINT,
                   "Cursor near line end snaps to ENDPOINT");

        // Snap to Line Midpoint
        var snapMid = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(25.1, 0.4), entities, 5.0);
        assertTrue(snapMid != null && snapMid.type() == sketch_snap_type_ui_main.MIDPOINT,
                   "Cursor near (25,0) snaps to MIDPOINT");

        // Snap to Circle Center
        var snapCenter = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(50.3, 49.8), entities, 5.0);
        assertTrue(snapCenter != null && snapCenter.type() == sketch_snap_type_ui_main.CENTER,
                   "Cursor near (50,50) snaps to CENTER");

        // Snap outside tolerance returns null
        var snapFar = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(200, 200), entities, 5.0);
        assertTrue(snapFar == null, "Cursor far away does not snap");
    }

    // 3. Principal planes and planar face bases orthonormality
    private static void testPrincipalPlanesAndFaceBasisOrthonormality() {
        System.out.println("\n[3] Testing Principal Planes & Face Basis Orthonormality...");
        for (sketch_plane_type_ui_main pt : new sketch_plane_type_ui_main[]{
            sketch_plane_type_ui_main.BASE_XZ,
            sketch_plane_type_ui_main.BASE_YZ,
            sketch_plane_type_ui_main.BASE_XY
        }) {
            sketch_coord_system_ui_main cs = sketch_coord_system_ui_main.forBasePlane(pt);
            Point3D u = cs.getUAxis(), v = cs.getVAxis(), n = cs.getNormal();
            assertTrue(Math.abs(u.magnitude() - 1.0) < 1e-4, pt.name() + " U has unit length");
            assertTrue(Math.abs(v.magnitude() - 1.0) < 1e-4, pt.name() + " V has unit length");
            assertTrue(Math.abs(n.magnitude() - 1.0) < 1e-4, pt.name() + " Normal has unit length");
            assertTrue(Math.abs(u.dotProduct(v)) < 1e-4, pt.name() + " U is perpendicular to V");
            assertTrue(Math.abs(u.dotProduct(n)) < 1e-4, pt.name() + " U is perpendicular to Normal");
            assertTrue(Math.abs(v.dotProduct(n)) < 1e-4, pt.name() + " V is perpendicular to Normal");

            Point3D cross = u.crossProduct(v);
            assertTrue(cross.distance(n) < 1e-4, pt.name() + " is strictly right-handed (U x V == Normal)");
        }

        // Test Face coordinate systems on a 3D Cuboid
        shape_item_ui_main cuboid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-20, -10, -30), new Point3D(20, 10, 30));

        for (face_kind_ui_main fk : new face_kind_ui_main[]{
            face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM,
            face_kind_ui_main.FRONT, face_kind_ui_main.BACK,
            face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT
        }) {
            sketch_coord_system_ui_main fcs = sketch_coord_system_ui_main.forFace(cuboid, fk);
            Point3D u = fcs.getUAxis(), v = fcs.getVAxis(), n = fcs.getNormal();
            assertTrue(Math.abs(u.dotProduct(v)) < 1e-4, "Face " + fk.name() + " U is perpendicular to V");
            assertTrue(Math.abs(u.dotProduct(n)) < 1e-4, "Face " + fk.name() + " U is perpendicular to Normal");
            assertTrue(Math.abs(v.dotProduct(n)) < 1e-4, "Face " + fk.name() + " V is perpendicular to Normal");
            var frame = ui.workspace.triad.coordinate_triad_math_ui_main.resolveLocalFrame(fcs);
            assertTrue(frame != null && frame.isValid(), "Face " + fk.name() + " resolves to a valid right-handed 3D frame");

            // Verify outward normals for cuboid faces
            switch (fk) {
                case TOP -> assertTrue(n.distance(new Point3D(0, -1, 0)) < 1e-4, "TOP normal points outward (0,-1,0)");
                case BOTTOM -> assertTrue(n.distance(new Point3D(0, 1, 0)) < 1e-4, "BOTTOM normal points outward (0,1,0)");
                case FRONT -> assertTrue(n.distance(new Point3D(0, 0, 1)) < 1e-4, "FRONT normal points outward (0,0,1)");
                case BACK -> assertTrue(n.distance(new Point3D(0, 0, -1)) < 1e-4, "BACK normal points outward (0,0,-1)");
                case LEFT -> assertTrue(n.distance(new Point3D(-1, 0, 0)) < 1e-4, "LEFT normal points outward (-1,0,0)");
                case RIGHT -> assertTrue(n.distance(new Point3D(1, 0, 0)) < 1e-4, "RIGHT normal points outward (1,0,0)");
            }
        }
    }

    // 4. Coordinate transforms and inverse transforms
    private static void testCoordinateTransformsAndInverses() {
        System.out.println("\n[4] Testing Coordinate Transforms & Inverse Mappings...");
        sketch_coord_system_ui_main cs = sketch_coord_system_ui_main.forBasePlane(sketch_plane_type_ui_main.BASE_XZ);

        sketch_point_2d_ui_main[] testPoints = {
            new sketch_point_2d_ui_main(0, 0),
            new sketch_point_2d_ui_main(35.5, -42.8),
            new sketch_point_2d_ui_main(-120.0, 85.25),
            new sketch_point_2d_ui_main(500.0, 1000.0)
        };

        for (sketch_point_2d_ui_main p2d : testPoints) {
            Point3D worldPt = cs.toWorldPoint(p2d);
            sketch_point_2d_ui_main roundTrip = cs.toSketchPoint(worldPt);
            assertTrue(p2d.equalsWithTolerance(roundTrip, 1e-5),
                       "Round-trip 2D -> 3D -> 2D preserves point: " + p2d);
        }

        // Ray-Plane Intersection
        // Ray starting at (0, -100, 0) pointing down (0, 1, 0) hitting XZ plane at Y=0
        double[] rayHit = {0, -100, 0, 0, 1, 0};
        Point3D hit = sketch_canvas_mapper_ui_main.intersectPlane(rayHit, Point3D.ZERO, new Point3D(0, -1, 0));
        assertTrue(hit != null && hit.distance(Point3D.ZERO) < 1e-4, "Ray intersects plane directly at origin");

        // Ray parallel to plane
        double[] rayParallel = {0, -100, 0, 1, 0, 0};
        Point3D parallelHit = sketch_canvas_mapper_ui_main.intersectPlane(rayParallel, Point3D.ZERO, new Point3D(0, -1, 0));
        assertTrue(parallelHit == null, "Parallel ray returns null");

        // Ray pointing away from plane (t < 0)
        double[] rayBehind = {0, -100, 0, 0, -1, 0};
        Point3D behindHit = sketch_canvas_mapper_ui_main.intersectPlane(rayBehind, Point3D.ZERO, new Point3D(0, -1, 0));
        assertTrue(behindHit == null, "Ray pointing away returns null");
    }

    // 5. Selecting faces and edges using raycasting
    private static void testRaycastFaceAndEdgeSelection() {
        System.out.println("\n[5] Testing Face & Edge Raycast Selection...");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE,
            new Point3D(-10, -10, -10), new Point3D(10, 10, 10));

        // Ray pointing at TOP face from above: origin (0, -50, 0), dir (0, 1, 0)
        double[] rayTop = {0, -50, 0, 0, 1, 0};
        face_reference_ui_main faceTop = face_picker_ui_main.pickFace(rayTop, cube);
        assertTrue(faceTop != null, "Top face is picked by vertical ray");
        assertTrue(faceTop.getFaceKind() == face_kind_ui_main.TOP, "Picked face is TOP");

        // Ray pointing at FRONT face: origin (0, 0, -50), dir (0, 0, 1)
        double[] rayFront = {0, 0, -50, 0, 0, 1};
        face_reference_ui_main faceFront = face_picker_ui_main.pickFace(rayFront, cube);
        assertTrue(faceFront != null, "Front face is picked by forward ray");
        assertTrue(faceFront.getFaceKind() == face_kind_ui_main.BACK || faceFront.getFaceKind() == face_kind_ui_main.FRONT,
                   "Picked face matches along Z axis");

        // Edge picking: ray close to top edge
        double[] rayEdge = {0, -10.1, -10.1, 0, 1, 1};
        topology_edge_ui_main edge = edge_picker_ui_main.pickEdge(rayEdge, cube);
        assertTrue(edge != null, "Edge picked near cube boundary");
    }

    // 6. Solid modeling and dependent feature regeneration
    private static void testSolidModelingAndFeatureRegeneration() {
        System.out.println("\n[6] Testing Solid Modeling & Feature Regeneration...");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-50, -25, -50), new Point3D(50, 25, 50));

        // Add a Hole feature to TOP face
        hole_feature_ui_main hole = new hole_feature_ui_main(
            "h1", host.getId(), face_kind_ui_main.TOP,
            0.0, 0.0, 12.0, 20.0, false
        );
        host.addHole(hole);
        assertTrue(host.hasHoles(), "Host has holes");
        assertTrue(host.getAllEffectiveHoles().size() == 1, "Host has 1 effective hole");

        // Add a Chamfer to an edge
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main(
            "ch1", host.getId(), "C:T_F", 3.0
        );
        host.addChamfer(chamfer);
        assertTrue(host.getChamfers().size() == 1, "Host has 1 chamfer");

        // Verify topology reflects solid and features
        topology_body_ui_main topo = host.getTopology();
        assertTrue(topo != null, "Topology body is generated");
        assertTrue(topo.getFace(face_kind_ui_main.TOP) != null, "TOP face present in topology");

        // Cascade feature regeneration
        List<shape_item_ui_main> shapes = List.of(host);
        feature_regeneration_engine_ui_main.regenerateAll(shapes);
        assertTrue(host.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID,
                   "Host remains valid after complete cascade regeneration");
    }

    // 7. Profile analysis, self-intersection, and boundary validation
    private static void testProfileAnalysisAndSelfIntersectionValidation() {
        System.out.println("\n[7] Testing Profile Analysis & Self-Intersection Detection...");

        // A. Valid closed rectangle
        List<sketch_entity_ui_main> rectLoop = List.of(
            new sketch_line_ui_main("l1", 0, 0, 40, 0),
            new sketch_line_ui_main("l2", 40, 0, 40, 30),
            new sketch_line_ui_main("l3", 40, 30, 0, 30),
            new sketch_line_ui_main("l4", 0, 30, 0, 0)
        );
        var resRect = sketch_profile_analyzer_ui_main.analyze(rectLoop);
        assertTrue(resRect.isValid(), "Closed rectangular profile is valid and extrudable");
        assertTrue(resRect.type() == sketch_profile_type_ui_main.CLOSED, "Profile type is CLOSED");

        // B. Open line chain (missing 4th side)
        List<sketch_entity_ui_main> openLoop = List.of(
            new sketch_line_ui_main("l1", 0, 0, 40, 0),
            new sketch_line_ui_main("l2", 40, 0, 40, 30),
            new sketch_line_ui_main("l3", 40, 30, 0, 30)
        );
        var resOpen = sketch_profile_analyzer_ui_main.analyze(openLoop);
        assertTrue(!resOpen.isValid(), "Open line chain is not extrudable");
        assertTrue(resOpen.type() == sketch_profile_type_ui_main.OPEN, "Open profile correctly flagged as OPEN");

        // C. Self-intersecting bowtie quadrilateral
        // (0,0) -> (40,30) -> (40,0) -> (0,30) -> (0,0)
        List<sketch_entity_ui_main> bowtieLoop = List.of(
            new sketch_line_ui_main("b1", 0, 0, 40, 30),
            new sketch_line_ui_main("b2", 40, 30, 40, 0),
            new sketch_line_ui_main("b3", 40, 0, 0, 30),
            new sketch_line_ui_main("b4", 0, 30, 0, 0)
        );
        var resBowtie = sketch_profile_analyzer_ui_main.analyze(bowtieLoop);
        assertTrue(!resBowtie.isValid(), "Bowtie profile is invalid");
        assertTrue(resBowtie.type() == sketch_profile_type_ui_main.SELF_INTERSECTING,
                   "Bowtie profile is correctly flagged as SELF_INTERSECTING");

        // D. Outer rectangle with inner circle hole
        List<sketch_entity_ui_main> outerWithInner = new ArrayList<>(rectLoop);
        outerWithInner.add(new sketch_circle_ui_main("c_inner", 20, 15, 5));
        var resHole = sketch_profile_analyzer_ui_main.analyze(outerWithInner);
        assertTrue(resHole.isValid(), "Profile with inner hole is extrudable");
        assertTrue(resHole.type() == sketch_profile_type_ui_main.MULTIPLE_LOOPS,
                   "Profile with inner hole has MULTIPLE_LOOPS");
        assertTrue(resHole.innerLoops().size() == 1, "Exactly 1 inner hole loop detected");
    }

    // 8. Interactive tool state reset, cancellation, and key events
    private static void testInteractiveToolStateResetAndCancellation() {
        System.out.println("\n[8] Testing Tool State Reset & Cancellation...");
        sketch_mode_ui_main mode = new sketch_mode_ui_main();
        mode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, null);
        assertTrue(mode.isActive(), "Sketch mode active");

        sketch_controller_ui_main ctrl = new sketch_controller_ui_main(mode, () -> mode.exit(true, null), () -> mode.exit(false, null));
        ctrl.setTool(sketch_tool_type_ui_main.LINE);
        assertTrue(ctrl.getActiveTool() == sketch_tool_type_ui_main.LINE, "Active tool is LINE");
        assertTrue(ctrl.getState() == sketch_controller_ui_main.State.IDLE, "State is IDLE");

        // Switching tools cancels active state
        ctrl.setTool(sketch_tool_type_ui_main.CIRCLE);
        assertTrue(ctrl.getActiveTool() == sketch_tool_type_ui_main.CIRCLE, "Active tool switched to CIRCLE");
        assertTrue(ctrl.getState() == sketch_controller_ui_main.State.IDLE, "State reset to IDLE on tool switch");

        // ESCAPE key cancels active drawing
        KeyEvent escEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
        boolean handledEsc = ctrl.handleKeyPressed(escEvent, null);
        assertTrue(handledEsc, "ESCAPE key handled by sketch controller");
        assertTrue(ctrl.getState() == sketch_controller_ui_main.State.IDLE, "State remains IDLE after ESCAPE");

        mode.exit(false, null);
        assertTrue(!mode.isActive(), "Sketch mode exited cleanly");
    }

    // 9. Document persistence round-trip for feature sketches
    private static void testDocumentPersistenceRoundTrip() throws Exception {
        System.out.println("\n[9] Testing Document Save & Reload Persistence...");
        File tempDoc = File.createTempFile("test_geom_persistence_", ".nd");
        tempDoc.deleteOnExit();

        shape_item_ui_main shape = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-30, -15, -30), new Point3D(30, 15, 30));

        // Attach sketch with lines, circle, and center rectangle
        sketch_feature_ui_main sketch = new sketch_feature_ui_main(
            "sk_test_1", shape.getId(), "Base Features",
            sketch_plane_type_ui_main.BASE_XZ, null
        );
        sketch.addEntity(new sketch_line_ui_main("line_base", 0, 0, 50, 0));
        sketch.addEntity(new sketch_circle_ui_main("circ_pin", 25, 25, 6.0));

        // Add Center Rect with 2 construction diagonals
        sketch_rect_ui_main cr = new sketch_rect_ui_main("cr_pocket", -10, -10, 10, 10);
        sketch_line_ui_main cd1 = new sketch_line_ui_main("cd1", -10, -10, 10, 10);
        cd1.setConstruction(true);
        sketch_line_ui_main cd2 = new sketch_line_ui_main("cd2", -10, 10, 10, -10);
        cd2.setConstruction(true);
        sketch.addEntity(cr);
        sketch.addEntity(cd1);
        sketch.addEntity(cd2);

        // Add a constraint
        sketch_constraint_ui_main constr = new sketch_constraint_ui_main(
            "c_len", sketch_constraint_type_ui_main.DISTANCE, List.of("line_base"), 50.0
        );
        sketch.addConstraint(constr);

        shape.addSketch(sketch);

        // Save
        boolean saved = document_serializer_ui_main.saveToFile(tempDoc, List.of(shape));
        assertTrue(saved, "Document successfully saved to .nd format");

        // Reload
        List<shape_item_ui_main> reloaded = document_serializer_ui_main.loadFromFile(tempDoc);
        assertTrue(reloaded.size() == 1, "Exactly 1 shape reloaded from document");
        shape_item_ui_main reloadedShape = reloaded.get(0);
        assertTrue(reloadedShape.hasSketches(), "Reloaded shape has sketches");
        assertTrue(reloadedShape.getSketches().size() == 1, "Reloaded shape has 1 sketch");

        sketch_feature_ui_main reloadedSketch = reloadedShape.getSketches().get(0);
        assertTrue(reloadedSketch.getName().equals("Base Features"), "Sketch name preserved");
        assertTrue(reloadedSketch.getEntities().size() == 5, "All 5 sketch entities restored");

        // Verify construction geometry flags restored
        int constructionCount = 0;
        for (sketch_entity_ui_main ent : reloadedSketch.getEntities()) {
            if (ent.isConstruction()) constructionCount++;
        }
        assertTrue(constructionCount == 2, "2 diagonal construction lines restored with construction=true");

        // Verify constraints restored
        assertTrue(reloadedSketch.getConstraints().size() == 1, "1 constraint restored");
        assertTrue(reloadedSketch.getConstraints().get(0).getType() == sketch_constraint_type_ui_main.DISTANCE,
                   "Restored constraint type is DISTANCE");
    }
}
