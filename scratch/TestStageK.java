import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.workspace.drafting.sketch.*;
import ui.workspace.drafting.faces.face_kind_ui_main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * TestStageK.java — Stage K: Interactive Sketch Mode & Viewport Drafting
 * Comprehensive headless test suite for sketch entities, constraints, snap, profile analysis,
 * solver, serialization, extrude bridge, and canvas mapping.
 */
public class TestStageK {

    static int pass = 0, fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { System.out.println("[PASS] " + name); pass++; }
        else       { System.out.println("[FAIL] " + name); fail++; }
    }
    static void assertClose(String name, double got, double expected, double tol) {
        check(name + " ≈ " + expected, Math.abs(got - expected) <= tol);
    }

    public static void main(String[] args) {
        Platform.startup(() -> {
            try { runAll(); }
            catch (Exception ex) { ex.printStackTrace(); System.exit(2); }
            finally {
                System.out.printf("%n===========================================%n");
                if (fail == 0) System.out.printf("ALL %d TESTS PASSED SUCCESSFULLY!%n===========================================%n", pass);
                else System.out.printf("FAILED: %d/%d passed.%n===========================================%n", pass, pass + fail);
                Platform.exit();
                System.exit(fail == 0 ? 0 : 1);
            }
        });
    }

    static void runAll() throws Exception {
        System.out.println("\n--- Stage K: Sketch Geometry ---");
        testPoint2D();
        testSketchLine();
        testSketchCircle();
        testSketchArc();
        testSketchRect();

        System.out.println("\n--- Stage K: Constraints ---");
        testConstraints();
        testConstraintEvaluator();

        System.out.println("\n--- Stage K: Solver ---");
        testSolver();

        System.out.println("\n--- Stage K: Snap System ---");
        testSnapSystem();

        System.out.println("\n--- Stage K: Hit Tester ---");
        testHitTester();

        System.out.println("\n--- Stage K: Profile Analyzer ---");
        testProfileAnalyzer();

        System.out.println("\n--- Stage K: Extrude Bridge ---");
        testExtrudeBridge();

        System.out.println("\n--- Stage K: Sketch Feature ---");
        testSketchFeature();

        System.out.println("\n--- Stage K: Coord System ---");
        testCoordSystem();

        System.out.println("\n--- Stage K: Serializer ---");
        testSerializer();

        System.out.println("\n--- Stage K: Canvas Mapper ---");
        testCanvasMapper();
    }

    // ============================================================
    // 1. POINT 2D
    // ============================================================
    static void testPoint2D() {
        var p = new sketch_point_2d_ui_main(3.0, 4.0);
        assertClose("Point2D.x", p.x(), 3.0, 1e-9);
        assertClose("Point2D.y", p.y(), 4.0, 1e-9);
        assertClose("Point2D.distance(origin)", p.distance(sketch_point_2d_ui_main.ZERO), 5.0, 1e-6);
        var mid = p.midpoint(sketch_point_2d_ui_main.ZERO);
        assertClose("Point2D.midpoint.x", mid.x(), 1.5, 1e-6);
        assertClose("Point2D.midpoint.y", mid.y(), 2.0, 1e-6);
        var normed = p.normalize();
        assertClose("Point2D.normalize length", Math.hypot(normed.x(), normed.y()), 1.0, 1e-6);
        assertClose("Point2D.dot", p.dot(new sketch_point_2d_ui_main(1, 0)), 3.0, 1e-9);
        assertClose("Point2D.cross", p.cross(new sketch_point_2d_ui_main(1, 0)), -4.0, 1e-9);
        check("Point2D.ZERO is zero", sketch_point_2d_ui_main.ZERO.x() == 0 && sketch_point_2d_ui_main.ZERO.y() == 0);
    }

    // ============================================================
    // 2. SKETCH LINE
    // ============================================================
    static void testSketchLine() {
        var l = new sketch_line_ui_main("L1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(4,0));
        check("Line.id", "L1".equals(l.getId()));
        assertClose("Line.length", l.getLength(), 4.0, 1e-6);
        var mid = l.getMidpoint();
        assertClose("Line.midpoint.x", mid.x(), 2.0, 1e-6);
        assertClose("Line.midpoint.y", mid.y(), 0.0, 1e-6);
        check("Line.type is LINE", l.getType() == sketch_geom_type_ui_main.LINE);
        check("Line.isValid", l.isValid());
        check("Line.not construction", !l.isConstruction());
        var dir = l.getDirection();
        assertClose("Line.direction.x", dir.x(), 1.0, 1e-6);
        assertClose("Line.direction.y", dir.y(), 0.0, 1e-6);

        // Vertical line
        var v = new sketch_line_ui_main("LV", new sketch_point_2d_ui_main(5,0), new sketch_point_2d_ui_main(5,10));
        assertClose("Vertical line length", v.getLength(), 10.0, 1e-6);
    }

    // ============================================================
    // 3. SKETCH CIRCLE
    // ============================================================
    static void testSketchCircle() {
        var c = new sketch_circle_ui_main("C1", new sketch_point_2d_ui_main(10, 5), 7.5);
        check("Circle.id", "C1".equals(c.getId()));
        assertClose("Circle.radius", c.getRadius(), 7.5, 1e-6);
        assertClose("Circle.center.x", c.getCenter().x(), 10.0, 1e-6);
        assertClose("Circle.diameter", c.getDiameter(), 15.0, 1e-6);
        check("Circle.type is CIRCLE", c.getType() == sketch_geom_type_ui_main.CIRCLE);
        check("Circle.isValid", c.isValid());
    }

    // ============================================================
    // 4. SKETCH ARC
    // ============================================================
    static void testSketchArc() {
        var arc = new sketch_arc_ui_main("A1", new sketch_point_2d_ui_main(0, 0), 10.0, 0.0, 90.0);
        check("Arc.id", "A1".equals(arc.getId()));
        assertClose("Arc.radius", arc.getRadius(), 10.0, 1e-6);
        var sp = arc.getStartPoint();
        assertClose("Arc.startPoint.x", sp.x(), 10.0, 1e-3);
        assertClose("Arc.startPoint.y", sp.y(), 0.0, 1e-3);
        var ep = arc.getEndPoint();
        assertClose("Arc.endPoint.x", ep.x(), 0.0, 1e-3);
        assertClose("Arc.endPoint.y", ep.y(), 10.0, 1e-3);
        check("Arc.type is ARC", arc.getType() == sketch_geom_type_ui_main.ARC);
    }

    // ============================================================
    // 5. SKETCH RECTANGLE
    // ============================================================
    static void testSketchRect() {
        var r = new sketch_rect_ui_main("R1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(6,4));
        check("Rect.id", "R1".equals(r.getId()));
        check("Rect.type is RECTANGLE", r.getType() == sketch_geom_type_ui_main.RECTANGLE);
        assertClose("Rect.width", r.getWidth(), 6.0, 1e-6);
        assertClose("Rect.height", r.getHeight(), 4.0, 1e-6);
        List<sketch_line_ui_main> lines = r.toLines();
        check("Rect.toLines has 4 segments", lines.size() == 4);
    }

    // ============================================================
    // 6. CONSTRAINTS
    // ============================================================
    static void testConstraints() {
        var hLine = new sketch_line_ui_main("H1", new sketch_point_2d_ui_main(0, 5), new sketch_point_2d_ui_main(10, 5));
        var c = new sketch_constraint_ui_main("CHOR", sketch_constraint_type_ui_main.HORIZONTAL, List.of("H1"), 0.0);
        check("Constraint.id", "CHOR".equals(c.getId()));
        check("Constraint.type HORIZONTAL", c.getType() == sketch_constraint_type_ui_main.HORIZONTAL);
        check("Constraint.isActive", c.isActive());
        check("Horizontal satisfied on horizontal line",
            sketch_constraint_evaluator_ui_main.isSatisfied(c, List.of(hLine)));

        var nonHLine = new sketch_line_ui_main("NH1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(3,4));
        check("Horizontal NOT satisfied on diagonal",
            !sketch_constraint_evaluator_ui_main.isSatisfied(c, List.of(nonHLine)));

        var vLine = new sketch_line_ui_main("V1", new sketch_point_2d_ui_main(3, 0), new sketch_point_2d_ui_main(3, 8));
        var cv = new sketch_constraint_ui_main("CVERT", sketch_constraint_type_ui_main.VERTICAL, List.of("V1"), 0.0);
        check("Vertical satisfied on vertical line",
            sketch_constraint_evaluator_ui_main.isSatisfied(cv, List.of(vLine)));
    }

    // ============================================================
    // 7. CONSTRAINT EVALUATOR - PARALLEL, PERPENDICULAR, EQUAL
    // ============================================================
    static void testConstraintEvaluator() {
        var l1 = new sketch_line_ui_main("PA", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(5,0));
        var l2 = new sketch_line_ui_main("PB", new sketch_point_2d_ui_main(0,3), new sketch_point_2d_ui_main(5,3));
        var cPar = new sketch_constraint_ui_main("CPAR", sketch_constraint_type_ui_main.PARALLEL, List.of("PA","PB"), 0.0);
        check("Parallel: two horizontal lines are parallel",
            sketch_constraint_evaluator_ui_main.isSatisfied(cPar, List.of(l1, l2)));

        var lV = new sketch_line_ui_main("PV", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(0,5));
        var cPerp = new sketch_constraint_ui_main("CPERP", sketch_constraint_type_ui_main.PERPENDICULAR, List.of("PA","PV"), 0.0);
        check("Perpendicular: horizontal + vertical are perpendicular",
            sketch_constraint_evaluator_ui_main.isSatisfied(cPerp, List.of(l1, lV)));

        var l3 = new sketch_line_ui_main("EA", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(5,0));
        var l4 = new sketch_line_ui_main("EB", new sketch_point_2d_ui_main(10,0), new sketch_point_2d_ui_main(15,0));
        var cEq = new sketch_constraint_ui_main("CEQ", sketch_constraint_type_ui_main.EQUAL, List.of("EA","EB"), 0.0);
        check("Equal: two lines of same length are equal",
            sketch_constraint_evaluator_ui_main.isSatisfied(cEq, List.of(l3, l4)));

        var c1 = new sketch_circle_ui_main("CA", new sketch_point_2d_ui_main(0,0), 8.0);
        var c2 = new sketch_circle_ui_main("CB", new sketch_point_2d_ui_main(20,0), 8.0);
        var cEqC = new sketch_constraint_ui_main("CEQC", sketch_constraint_type_ui_main.EQUAL, List.of("CA","CB"), 0.0);
        check("Equal: two circles of same radius are equal",
            sketch_constraint_evaluator_ui_main.isSatisfied(cEqC, List.of(c1, c2)));
    }

    // ============================================================
    // 8. SOLVER
    // ============================================================
    static void testSolver() {
        // Empty: under-constrained
        var res0 = sketch_solver_ui_main.solve(List.of(), List.of());
        check("Solver: empty geometry → UNDER_CONSTRAINED",
            res0.status() == sketch_constraint_status_ui_main.UNDER_CONSTRAINED);

        // Horizontal line with HORIZONTAL constraint
        var hl = new sketch_line_ui_main("SL1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,0));
        var cH = new sketch_constraint_ui_main("SCH", sketch_constraint_type_ui_main.HORIZONTAL, List.of("SL1"), 0.0);
        var res1 = sketch_solver_ui_main.solve(List.of(hl), List.of(cH));
        check("Solver: satisfied horizontal constraint → not INVALID", res1.status() != sketch_constraint_status_ui_main.INVALID);

        // Geometry without constraints
        var res2 = sketch_solver_ui_main.solve(List.of(hl), List.of());
        check("Solver: geometry, no constraints → UNDER_CONSTRAINED",
            res2.status() == sketch_constraint_status_ui_main.UNDER_CONSTRAINED);
    }

    // ============================================================
    // 9. SNAP SYSTEM
    // ============================================================
    static void testSnapSystem() {
        var line = new sketch_line_ui_main("SN1", new sketch_point_2d_ui_main(10,0), new sketch_point_2d_ui_main(30,0));
        var entities = List.<sketch_entity_ui_main>of(line);

        // Snap to endpoint
        var r1 = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(10.5, 0.3), entities, 2.0);
        check("Snap: near start → ENDPOINT", r1 != null && r1.type() == sketch_snap_type_ui_main.ENDPOINT);
        assertClose("Snap: endpoint x=10", r1.snapPoint().x(), 10.0, 1e-6);

        // Snap to midpoint
        var r2 = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(20.2, 0.1), entities, 2.0);
        check("Snap: near midpoint → MIDPOINT", r2 != null && r2.type() == sketch_snap_type_ui_main.MIDPOINT);
        assertClose("Snap: midpoint x=20", r2.snapPoint().x(), 20.0, 1e-6);

        // Snap to origin
        var r3 = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(0.2, 0.1), List.of(), 2.0);
        check("Snap: near origin → ORIGIN", r3 != null && r3.type() == sketch_snap_type_ui_main.ORIGIN);

        // No snap far away
        var r4 = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(100, 100), entities, 2.0);
        check("Snap: far away → null", r4 == null);

        // Snap to circle center
        var circ = new sketch_circle_ui_main("SC1", new sketch_point_2d_ui_main(50, 50), 10);
        var r5 = sketch_snap_system_ui_main.findBestSnap(new sketch_point_2d_ui_main(50.5, 50.3), List.of(circ), 2.0);
        check("Snap: near circle center → CENTER", r5 != null && r5.type() == sketch_snap_type_ui_main.CENTER);
    }

    // ============================================================
    // 10. HIT TESTER
    // ============================================================
    static void testHitTester() {
        var l = new sketch_line_ui_main("HT1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,0));
        double d1 = sketch_hit_tester_ui_main.distToEntity(new sketch_point_2d_ui_main(5, 0), l);
        assertClose("HitTester: on-line dist = 0", d1, 0.0, 1e-5);

        double d2 = sketch_hit_tester_ui_main.distToEntity(new sketch_point_2d_ui_main(5, 3), l);
        assertClose("HitTester: 3 above line dist = 3", d2, 3.0, 1e-4);

        var c = new sketch_circle_ui_main("HC1", new sketch_point_2d_ui_main(0,0), 5.0);
        double d3 = sketch_hit_tester_ui_main.distToEntity(new sketch_point_2d_ui_main(5,0), c);
        assertClose("HitTester: on circle perimeter = 0", d3, 0.0, 1e-5);
        double d4 = sketch_hit_tester_ui_main.distToEntity(new sketch_point_2d_ui_main(0,0), c);
        assertClose("HitTester: center to circle edge = radius", d4, 5.0, 1e-5);
    }

    // ============================================================
    // 11. PROFILE ANALYZER
    // ============================================================
    static void testProfileAnalyzer() {
        // Empty: invalid
        var r0 = sketch_profile_analyzer_ui_main.analyze(List.of());
        check("Profile: empty → INVALID", r0.type() == sketch_profile_type_ui_main.INVALID);

        // Single closed circle: CLOSED
        var circ = new sketch_circle_ui_main("PC1", new sketch_point_2d_ui_main(0,0), 10);
        var r1 = sketch_profile_analyzer_ui_main.analyze(List.of(circ));
        check("Profile: circle → CLOSED", r1.type() == sketch_profile_type_ui_main.CLOSED);
        check("Profile: circle has outer loop", r1.outerLoop() != null);

        // 4 lines forming a closed square (20x20)
        var l1 = new sketch_line_ui_main("SQ1", new sketch_point_2d_ui_main(0,0),  new sketch_point_2d_ui_main(20,0));
        var l2 = new sketch_line_ui_main("SQ2", new sketch_point_2d_ui_main(20,0), new sketch_point_2d_ui_main(20,20));
        var l3 = new sketch_line_ui_main("SQ3", new sketch_point_2d_ui_main(20,20),new sketch_point_2d_ui_main(0,20));
        var l4 = new sketch_line_ui_main("SQ4", new sketch_point_2d_ui_main(0,20), new sketch_point_2d_ui_main(0,0));
        var r2 = sketch_profile_analyzer_ui_main.analyze(List.of(l1, l2, l3, l4));
        check("Profile: closed square → CLOSED", r2.type() == sketch_profile_type_ui_main.CLOSED);
        check("Profile: square has outer loop", r2.outerLoop() != null);
        check("Profile: square valid for extrusion", r2.isValid());

        // Single open line: OPEN
        var openL = new sketch_line_ui_main("OL", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,10));
        var r3 = sketch_profile_analyzer_ui_main.analyze(List.of(openL));
        check("Profile: single open line → OPEN", r3.type() == sketch_profile_type_ui_main.OPEN);
        check("Profile: open not extrudable", !r3.isValid());

        // Rect entity → CLOSED
        var rect = new sketch_rect_ui_main("RE1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,8));
        var r4 = sketch_profile_analyzer_ui_main.analyze(List.of(rect));
        check("Profile: rect entity → CLOSED", r4.type() == sketch_profile_type_ui_main.CLOSED);
    }

    // ============================================================
    // 12. EXTRUDE BRIDGE
    // ============================================================
    static void testExtrudeBridge() {
        var sk = new sketch_feature_ui_main("SK_EXT", "SHP_EXT", "Extrude Sketch", sketch_plane_type_ui_main.BASE_XZ, null);
        sk.addEntity(new sketch_rect_ui_main("REC1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(20,10)));

        var grp = sketch_extrude_bridge_ui_main.buildExtrusion(sk, 15.0, false);
        check("ExtrudeBridge: extrusion group created", grp != null);
        check("ExtrudeBridge: extrusion has mesh children", !grp.getChildren().isEmpty());
    }

    // ============================================================
    // 13. SKETCH FEATURE
    // ============================================================
    static void testSketchFeature() {
        var sk = new sketch_feature_ui_main("SK1", "SHAPE1", "Sketch 1", sketch_plane_type_ui_main.BASE_XZ, null);
        check("SketchFeature.id", "SK1".equals(sk.getId()));
        check("SketchFeature.name", "Sketch 1".equals(sk.getName()));
        check("SketchFeature.planeType BASE_XZ", sk.getPlaneType() == sketch_plane_type_ui_main.BASE_XZ);
        check("SketchFeature: initially clean", sk.isValid());
        check("SketchFeature: initially null profile", sk.getProfileResult() == null);

        // Add a circle
        var circ = new sketch_circle_ui_main("FC1", new sketch_point_2d_ui_main(0,0), 15);
        sk.addEntity(circ);
        check("SketchFeature: after circle, CLOSED profile", sk.getProfileResult().type() == sketch_profile_type_ui_main.CLOSED);
        check("SketchFeature: 1 entity", sk.getEntities().size() == 1);
        check("SketchFeature: still valid", sk.isValid());

        // Get entity
        check("SketchFeature.getEntity", sk.getEntity("FC1") == circ);

        // Add constraint
        var c = new sketch_constraint_ui_main("CRA", sketch_constraint_type_ui_main.RADIUS, List.of("FC1"), 15.0);
        sk.addConstraint(c);
        check("SketchFeature: has 1 constraint", sk.getConstraints().size() == 1);

        // Remove entity
        sk.removeEntity("FC1");
        check("SketchFeature: entity removed", sk.getEntities().isEmpty());
        check("SketchFeature: related constraints removed", sk.getConstraints().isEmpty());

        // Copy
        var sk2 = new sketch_feature_ui_main("SK2", "SHAPE2", "Copy", sketch_plane_type_ui_main.BASE_XZ, null);
        var rectE = new sketch_rect_ui_main("RR1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,5));
        sk2.addEntity(rectE);
        var sk2Copy = sk2.copy();
        check("SketchFeature.copy: same id", sk2Copy.getId().equals(sk2.getId()));
        check("SketchFeature.copy: same entity count", sk2Copy.getEntities().size() == sk2.getEntities().size());

        // Signature
        String sig = sk2.getSignature();
        check("SketchFeature.signature non-null", sig != null && !sig.isEmpty());
        check("SketchFeature.signature contains id", sig.contains("SK2"));
    }

    // ============================================================
    // 14. COORD SYSTEM
    // ============================================================
    static void testCoordSystem() {
        var cs = new sketch_coord_system_ui_main(sketch_plane_type_ui_main.BASE_XZ, "S1", null);
        check("CoordSystem: normal non-null", cs.getNormal() != null);
        double normalMag = cs.getNormal().magnitude();
        assertClose("CoordSystem: normal is unit", normalMag, 1.0, 1e-6);

        var p2d = new sketch_point_2d_ui_main(3.0, 4.0);
        var world = cs.toWorldPoint(p2d);
        check("CoordSystem: toWorldPoint non-null", world != null);

        var back = cs.toSketchPoint(world);
        assertClose("CoordSystem: roundtrip x", back.x(), p2d.x(), 1e-4);
        assertClose("CoordSystem: roundtrip y", back.y(), p2d.y(), 1e-4);
    }

    // ============================================================
    // 15. SERIALIZER
    // ============================================================
    static void testSerializer() throws Exception {
        var sk = new sketch_feature_ui_main("SER1", "SHP1", "SerTest", sketch_plane_type_ui_main.BASE_XZ, face_kind_ui_main.TOP);
        sk.addEntity(new sketch_line_ui_main("SL1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(10,0)));
        sk.addEntity(new sketch_circle_ui_main("SC1", new sketch_point_2d_ui_main(5,5), 3.0));
        sk.addEntity(new sketch_rect_ui_main("SR1", new sketch_point_2d_ui_main(0,0), new sketch_point_2d_ui_main(8,6)));
        sk.addEntity(new sketch_arc_ui_main("SA1", new sketch_point_2d_ui_main(0,0), 5.0, 0.0, 180.0));
        sk.addConstraint(new sketch_constraint_ui_main("SCON1", sketch_constraint_type_ui_main.HORIZONTAL, List.of("SL1"), 0.0));

        Path tmp = Files.createTempFile("sketch_test", ".nd");
        try (PrintWriter pw = new PrintWriter(tmp.toFile())) {
            sketch_serializer_ui_main.writeSketch(pw, sk);
        }
        String content = Files.readString(tmp);
        check("Serializer: file non-empty", !content.isBlank());
        check("Serializer: has sketch header", content.contains("sketch:"));
        check("Serializer: has LINE entity", content.contains("LINE"));
        check("Serializer: has CIRCLE entity", content.contains("CIRCLE"));
        check("Serializer: has RECTANGLE entity", content.contains("RECTANGLE"));
        check("Serializer: has ARC entity", content.contains("ARC"));
        check("Serializer: has constraint", content.contains("sconstraint:"));
        check("Serializer: has constraint type HORIZONTAL", content.contains("HORIZONTAL"));
        check("Serializer: has face kind TOP", content.contains("TOP"));

        // Test parse
        String header = content.lines().findFirst().orElse("");
        var parsed = sketch_serializer_ui_main.parseSketchHeader(header);
        check("Serializer: parse returns non-null", parsed != null);
        check("Serializer: parsed id matches", parsed != null && "SER1".equals(parsed.getId()));
        check("Serializer: parsed name matches", parsed != null && "SerTest".equals(parsed.getName()));
        check("Serializer: parsed planeType BASE_XZ", parsed != null && parsed.getPlaneType() == sketch_plane_type_ui_main.BASE_XZ);
        check("Serializer: parsed faceKind TOP", parsed != null && parsed.getFaceKind() == face_kind_ui_main.TOP);

        Files.deleteIfExists(tmp);
    }

    // ============================================================
    // 16. CANVAS MAPPER
    // ============================================================
    static void testCanvasMapper() {
        var cs = sketch_coord_system_ui_main.forBasePlane(sketch_plane_type_ui_main.BASE_XZ);
        var pt = new sketch_point_2d_ui_main(10.0, 20.0);
        Point3D world = sketch_canvas_mapper_ui_main.sketchToWorld(pt, cs);
        check("CanvasMapper: sketchToWorld non-null", world != null);

        sketch_point_2d_ui_main mappedBack = sketch_canvas_mapper_ui_main.worldToSketch(world, cs);
        assertClose("CanvasMapper: roundtrip x", mappedBack.x(), pt.x(), 1e-4);
        assertClose("CanvasMapper: roundtrip y", mappedBack.y(), pt.y(), 1e-4);

        // Ray intersection test
        double[] ray = new double[] { 5.0, -10.0, 7.0, 0.0, 1.0, 0.0 }; // Ray pointing straight down in Y towards origin plane (y=0)
        Point3D hit = sketch_canvas_mapper_ui_main.intersectPlane(ray, cs.getOrigin(), cs.getNormal());
        check("CanvasMapper: intersectPlane finds hit", hit != null);
        if (hit != null) {
            assertClose("CanvasMapper: ray hit X", hit.getX(), 5.0, 1e-4);
            assertClose("CanvasMapper: ray hit Z", hit.getZ(), 7.0, 1e-4);
        }
    }
}
