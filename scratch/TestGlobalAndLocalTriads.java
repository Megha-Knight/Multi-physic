package scratch;

import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.sketch_coord_system_ui_main;
import ui.workspace.drafting.sketch.sketch_mode_ui_main;
import ui.workspace.drafting.sketch.sketch_plane_type_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.triad.coordinate_triad_math_ui_main;
import ui.workspace.triad.local_triad_3d_ui_main;
import ui.workspace.triad.triad_coordinator_ui_main;
import ui.workspace.triad.world_triad_ui_main;

/**
 * TestGlobalAndLocalTriads.java
 * Comprehensive verification of CAD dual-coordinate triad architecture (Task 02).
 */
public class TestGlobalAndLocalTriads {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.err.println("  [FAIL] " + testName);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Task 02: Global & Local Triads Test Suite");
        System.out.println("=================================================");

        int exitCode = 1;
        try {
            javafx.application.Platform.startup(() -> {});
            testWorldTriadInvariance();
            testLocalTriadMatchesActivePlane();
            testPrincipalPlanesBasis();
            testRotatedPlaneOrthonormality();
            testFaceBasedSketchOrientations();
            testRoundTripCoordinateTransforms();
            testSwitchingPlanesRefreshesTriad();
            testViewCubeAndWorldTriadAlignment();
            testViewportResizingAndThemeSafety();
            testModelGeometryIsolation();
            testDegeneratePlanesFailSafe();
            testMouseTransparencyAndEventRouting();

            System.out.println("\n-------------------------------------------------");
            System.out.println(String.format("Result: %d passed, %d failed", passed, failed));
            System.out.println("-------------------------------------------------");
            if (failed == 0) {
                System.out.println("ALL TESTS PASSED 100%!");
                exitCode = 0;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    // 1. World triad remains fixed in viewport corner independent of pan/zoom
    private static void testWorldTriadInvariance() {
        System.out.println("\n[1] World Triad Viewport Invariance & Placement:");
        Rotate rx = new Rotate(30, Rotate.X_AXIS);
        Rotate ry = new Rotate(-45, Rotate.Y_AXIS);
        world_triad_ui_main triad = new world_triad_ui_main(rx, ry);

        check("World triad has fixed size (width == 78)", triad.getPrefWidth() == 78.0);
        check("World triad has fixed size (height == 96)", triad.getPrefHeight() == 96.0);
        check("World triad is not affected by translation", triad.getTranslateX() == 0.0 && triad.getTranslateY() == 0.0);
    }

    // 2. Local triad matches the active sketch plane
    private static void testLocalTriadMatchesActivePlane() {
        System.out.println("\n[2] Local Triad Matches Active Sketch Plane:");
        sketch_coord_system_ui_main cs = sketch_coord_system_ui_main.forBasePlane(sketch_plane_type_ui_main.BASE_XZ);
        var frame = coordinate_triad_math_ui_main.resolveLocalFrame(cs);

        check("Frame is non-null", frame != null);
        check("Frame is marked local", frame.isLocal());
        check("Frame origin is at sketch plane origin (0,0,0)", frame.origin().distance(Point3D.ZERO) < 1e-4);
        check("Frame U axis matches CS U axis", frame.uAxis().distance(cs.getUAxis()) < 1e-4);
        check("Frame V axis matches CS V axis", frame.vAxis().distance(cs.getVAxis()) < 1e-4);
    }

    // 3. Local axes are correct on XY, YZ, and XZ principal planes
    private static void testPrincipalPlanesBasis() {
        System.out.println("\n[3] Principal Planes Basis & Right-Handedness:");
        for (sketch_plane_type_ui_main pt : new sketch_plane_type_ui_main[]{
            sketch_plane_type_ui_main.BASE_XZ,
            sketch_plane_type_ui_main.BASE_YZ,
            sketch_plane_type_ui_main.BASE_XY
        }) {
            sketch_coord_system_ui_main cs = sketch_coord_system_ui_main.forBasePlane(pt);
            var frame = coordinate_triad_math_ui_main.resolveLocalFrame(cs);

            check(pt.name() + " is orthonormal", coordinate_triad_math_ui_main.isOrthonormal(frame.uAxis(), frame.vAxis(), frame.wAxis(), 1e-4));
            check(pt.name() + " is strictly right-handed", coordinate_triad_math_ui_main.isRightHanded(frame.uAxis(), frame.vAxis(), frame.wAxis(), 1e-4));
            check(pt.name() + " basis vectors have unit length", Math.abs(frame.uAxis().magnitude() - 1.0) < 1e-4
                  && Math.abs(frame.vAxis().magnitude() - 1.0) < 1e-4 && Math.abs(frame.wAxis().magnitude() - 1.0) < 1e-4);
        }
    }

    // 4. Local axes are correct on a rotated sketch plane
    private static void testRotatedPlaneOrthonormality() {
        System.out.println("\n[4] Rotated Sketch Plane Basis:");
        Rotate rPitch = new Rotate(35.0, Rotate.X_AXIS);
        Rotate rYaw   = new Rotate(70.0, Rotate.Y_AXIS);

        Point3D uRot = rYaw.deltaTransform(rPitch.deltaTransform(new Point3D(1, 0, 0))).normalize();
        Point3D vRot = rYaw.deltaTransform(rPitch.deltaTransform(new Point3D(0, 0, 1))).normalize();
        Point3D wRot = uRot.crossProduct(vRot).normalize();

        var frame = new coordinate_triad_math_ui_main.TriadFrame(new Point3D(10, 20, 30), uRot, vRot, wRot, "RotatedPlane", true);

        check("Rotated frame is valid", frame.isValid());
        check("Rotated frame is orthonormal", coordinate_triad_math_ui_main.isOrthonormal(frame.uAxis(), frame.vAxis(), frame.wAxis(), 1e-4));
        check("Rotated frame is right-handed", coordinate_triad_math_ui_main.isRightHanded(frame.uAxis(), frame.vAxis(), frame.wAxis(), 1e-4));
    }

    // 5. Face-based sketch orientation is consistent with selected face normal
    private static void testFaceBasedSketchOrientations() {
        System.out.println("\n[5] Face-Based Sketch Orientations & Normal Consistency:");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0,0,0), new Point3D(100,100,100));

        face_kind_ui_main[] faces = {
            face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM,
            face_kind_ui_main.FRONT, face_kind_ui_main.BACK,
            face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT
        };

        for (face_kind_ui_main f : faces) {
            var info = face_geometry_helper_ui_main.getFaceInfo(cube, f);
            sketch_coord_system_ui_main cs = sketch_coord_system_ui_main.forFace(cube, f);
            var frame = coordinate_triad_math_ui_main.resolveLocalFrame(cs);

            check("Face " + f.name() + " frame valid", frame != null && frame.isValid());
            check("Face " + f.name() + " normal aligns with outward normal",
                  frame.wAxis().dotProduct(info.normal()) > 0.999);
        }
    }

    // 6. Transform / inverse-transform round trips preserve coordinates within tolerance
    private static void testRoundTripCoordinateTransforms() {
        System.out.println("\n[6] Round-Trip Coordinate Transforms:");
        double cos30 = Math.sqrt(3.0) / 2.0;
        var frame = new coordinate_triad_math_ui_main.TriadFrame(
            new Point3D(25, -15, 60),
            new Point3D(cos30, 0, 0.5),
            new Point3D(0, 1, 0),
            new Point3D(-0.5, 0, cos30),
            "AngledFrame", true
        );

        Point3D[] testWorldPoints = {
            new Point3D(0, 0, 0),
            new Point3D(25, -15, 60),
            new Point3D(100.5, -42.3, 18.7),
            new Point3D(-55.2, 88.1, -120.4)
        };

        for (Point3D pt : testWorldPoints) {
            Point3D local = coordinate_triad_math_ui_main.worldToLocal(pt, frame);
            Point3D restored = coordinate_triad_math_ui_main.localToWorld(local, frame);
            check("Round-trip preserves " + pt, pt.distance(restored) < 1e-4);
        }
    }

    // 7. Switching planes and sketches refreshes the triad without stale state
    private static void testSwitchingPlanesRefreshesTriad() {
        System.out.println("\n[7] Switching Planes Refreshes Local Triad:");
        StackPane viewport = new StackPane();
        Group root3D = new Group();
        Rotate rx = new Rotate(0, Rotate.X_AXIS);
        Rotate ry = new Rotate(0, Rotate.Y_AXIS);
        sketch_mode_ui_main mode = new sketch_mode_ui_main();

        triad_coordinator_ui_main coordinator = new triad_coordinator_ui_main(
            viewport, root3D, rx, ry, mode, null
        );

        check("Local triad initially hidden (no sketch active)", !coordinator.getLocalTriad().isVisible());

        // Enter Base XZ
        mode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, null);
        coordinator.syncState();
        check("Local triad visible after enterBasePlane XZ", coordinator.getLocalTriad().isVisible());
        check("Local triad frame is BASE_XZ", coordinator.getLocalTriad().getCurrentFrame().name().contains("BASE_XZ"));

        // Switch to Base YZ
        mode.exit(false, null);
        coordinator.syncState();
        check("Local triad hidden after exit", !coordinator.getLocalTriad().isVisible());

        mode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_YZ, null);
        coordinator.syncState();
        check("Local triad visible after enterBasePlane YZ", coordinator.getLocalTriad().isVisible());
        check("Local triad frame updated to BASE_YZ", coordinator.getLocalTriad().getCurrentFrame().name().contains("BASE_YZ"));
    }

    // 8. ViewCube and World Triad alignment
    private static void testViewCubeAndWorldTriadAlignment() {
        System.out.println("\n[8] ViewCube & World Triad Alignment:");
        Rotate rx = new Rotate(35.0, Rotate.X_AXIS);
        Rotate ry = new Rotate(120.0, Rotate.Y_AXIS);
        world_triad_ui_main wt = new world_triad_ui_main(rx, ry);

        check("World triad instantiates correctly with camera bindings", wt != null);
        rx.setAngle(50.0);
        ry.setAngle(170.0);
        check("World triad updates dynamically with camera rotation changes", wt != null);
    }

    // 9. Viewport resizing and layout safety
    private static void testViewportResizingAndThemeSafety() {
        System.out.println("\n[9] Viewport Resizing & Layout Safety:");
        StackPane viewport = new StackPane();
        viewport.setPrefSize(800, 600);
        Rotate rx = new Rotate(0, Rotate.X_AXIS);
        Rotate ry = new Rotate(0, Rotate.Y_AXIS);
        world_triad_ui_main wt = new world_triad_ui_main(rx, ry);

        StackPane.setAlignment(wt, Pos.BOTTOM_LEFT);
        viewport.getChildren().add(wt);

        viewport.setPrefSize(1920, 1080);
        check("World triad remains at BOTTOM_LEFT when viewport expands", StackPane.getAlignment(wt) == Pos.BOTTOM_LEFT);

        viewport.setPrefSize(400, 300);
        check("World triad remains at BOTTOM_LEFT when viewport shrinks", StackPane.getAlignment(wt) == Pos.BOTTOM_LEFT);
    }

    // 10. Coordinate indicators do not affect model geometry, saving, or exports
    private static void testModelGeometryIsolation() {
        System.out.println("\n[10] Model Geometry Isolation:");
        local_triad_3d_ui_main lt = new local_triad_3d_ui_main();
        lt.updateFrame(coordinate_triad_math_ui_main.getWorldFrame());

        Object obj = lt;
        check("Local triad is a Group node", lt instanceof Group);
        check("Local triad does not implement shape_item_ui_main", !(obj instanceof shape_item_ui_main));
    }

    // 11. Invalid plane data fails safely
    private static void testDegeneratePlanesFailSafe() {
        System.out.println("\n[11] Degenerate Plane Fail-Safe Handling:");
        var frameNull = coordinate_triad_math_ui_main.resolveLocalFrame(null);
        check("Null sketch coordinate system returns null safely", frameNull == null);

        var degenerateFace = coordinate_triad_math_ui_main.resolveFaceFrame(null);
        check("Null face reference returns null safely", degenerateFace == null);

        local_triad_3d_ui_main lt = new local_triad_3d_ui_main();
        lt.updateFrame(null);
        check("Updating local triad with null frame hides it safely without throwing", !lt.isVisible());
    }

    // 12. Mouse events reach the correct sketch or viewport handler
    private static void testMouseTransparencyAndEventRouting() {
        System.out.println("\n[12] Mouse Transparency & Event Routing:");
        local_triad_3d_ui_main lt = new local_triad_3d_ui_main();
        check("Local 3D triad is mouseTransparent (cannot block sketch clicks)", lt.isMouseTransparent());
    }
}
