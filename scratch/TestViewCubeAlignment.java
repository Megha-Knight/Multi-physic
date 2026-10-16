package scratch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TestViewCubeAlignment.java
 * Rigorous mathematical and JavaFX transform verification of ViewCube rotation composition.
 * Verifies that cubePivot.getTransforms().setAll(cubeRx, cubeRy) faithfully mirrors
 * the main viewport's nested camera transform: cameraYGroup(Ry) -> cameraXGroup(Rx) -> camera(Translate).
 */
public class TestViewCubeAlignment {

    private static final Map<String, Point3D> FACE_NORMALS = new LinkedHashMap<>();
    static {
        FACE_NORMALS.put("FRONT",  new Point3D( 0,  0,  1)); // Z = +1
        FACE_NORMALS.put("BACK",   new Point3D( 0,  0, -1)); // Z = -1
        FACE_NORMALS.put("TOP",    new Point3D( 0, -1,  0)); // Y = -1 (sky)
        FACE_NORMALS.put("BOTTOM", new Point3D( 0,  1,  0)); // Y = +1 (ground)
        FACE_NORMALS.put("RIGHT",  new Point3D( 1,  0,  0)); // X = +1
        FACE_NORMALS.put("LEFT",   new Point3D(-1,  0,  0)); // X = -1
    }

    public record AlignmentResult(
        double pitch,
        double yaw,
        String mainFace,
        double mainDot,
        String oldCubeFace,
        double oldDot,
        String newCubeFace,
        double newDot
    ) {}

    public static AlignmentResult evaluate(double pitch, double yaw) {
        // 1 & 2. Main Viewport Camera position:
        // C_pos = Ry(yaw) * Rx(pitch) * (0, -30, -400)
        Rotate mainRx = new Rotate(pitch, Rotate.X_AXIS);
        Rotate mainRy = new Rotate(yaw, Rotate.Y_AXIS);
        Point3D camLocalPos = new Point3D(0, -30, -400);
        Point3D camWorldPos = mainRy.transform(mainRx.transform(camLocalPos));

        // Line-of-sight vector from world origin to camera
        Point3D eyeVec = camWorldPos.normalize();
        String mainFace = null;
        double maxMainDot = -Double.MAX_VALUE;
        for (var entry : FACE_NORMALS.entrySet()) {
            double dot = entry.getValue().dotProduct(eyeVec);
            if (dot > maxMainDot) {
                maxMainDot = dot;
                mainFace = entry.getKey();
            }
        }

        // 3. ViewCube calculation:
        // ViewCube camera is at (0, 0, -102) looking along +Z towards (0, 0, 0).
        // The vector from the ViewCube origin towards the ViewCube camera is (0, 0, -1).
        Point3D vcCamDir = new Point3D(0, 0, -1);

        Rotate cubeRx = new Rotate(-pitch, Rotate.X_AXIS);
        Rotate cubeRy = new Rotate(-yaw, Rotate.Y_AXIS);

        // Old incorrect composition: addAll(cubeRy, cubeRx)
        Group oldPivot = new Group();
        oldPivot.getTransforms().addAll(cubeRy, cubeRx);
        Transform oldTrans = oldPivot.getLocalToParentTransform();

        String oldCubeFace = null;
        double maxOldDot = -Double.MAX_VALUE;
        for (var entry : FACE_NORMALS.entrySet()) {
            Point3D transformedNormal = oldTrans.deltaTransform(entry.getValue());
            double dot = transformedNormal.dotProduct(vcCamDir);
            if (dot > maxOldDot) {
                maxOldDot = dot;
                oldCubeFace = entry.getKey();
            }
        }

        // Corrected composition: setAll(cubeRx, cubeRy)
        Group newPivot = new Group();
        newPivot.getTransforms().setAll(cubeRx, cubeRy);
        Transform newTrans = newPivot.getLocalToParentTransform();

        String newCubeFace = null;
        double maxNewDot = -Double.MAX_VALUE;
        for (var entry : FACE_NORMALS.entrySet()) {
            Point3D transformedNormal = newTrans.deltaTransform(entry.getValue());
            double dot = transformedNormal.dotProduct(vcCamDir);
            if (dot > maxNewDot) {
                maxNewDot = dot;
                newCubeFace = entry.getKey();
            }
        }

        return new AlignmentResult(pitch, yaw, mainFace, maxMainDot, oldCubeFace, maxOldDot, newCubeFace, maxNewDot);
    }

    public static void main(String[] args) {
        System.out.println("=== ViewCube Rotation Composition & Face Alignment Test ===");

        int passed = 0;
        int total = 0;

        // Acceptance Criterion 1: Exact counterexample pitch=50°, yaw=170°
        // Must demonstrate that Old composition fails (TOP) and New composition passes (BOTTOM)
        total++;
        AlignmentResult ce = evaluate(50.0, 170.0);
        System.out.println(String.format("1. Counterexample Check (pitch=%.1f°, yaw=%.1f°):", ce.pitch(), ce.yaw()));
        System.out.println(String.format("   Main Viewport Visible Face: %s (dot=%.4f)", ce.mainFace(), ce.mainDot()));
        System.out.println(String.format("   Old Composition (Ry, Rx):   %s (dot=%.4f)", ce.oldCubeFace(), ce.oldDot()));
        System.out.println(String.format("   New Composition (Rx, Ry):   %s (dot=%.4f)", ce.newCubeFace(), ce.newDot()));

        if ("BOTTOM".equals(ce.mainFace()) && "TOP".equals(ce.oldCubeFace()) && "BOTTOM".equals(ce.newCubeFace())) {
            passed++;
            System.out.println("   [PASS] Proven: Old composition fails with TOP, New composition correctly resolves to BOTTOM!");
        } else {
            System.err.println("   [FAIL] Counterexample acceptance criterion not met!");
        }

        // Acceptance Criterion 2: Additional non-degenerate pitch/yaw combinations across all quadrants
        double[][] matrix = new double[][]{
            // {pitch, yaw, expectedDescription}
            {  70.0,  150.0 }, // Quadrant 2 steep: old gives TOP, new gives BOTTOM
            { -70.0,  150.0 }, // Quadrant 2 negative pitch: old gives BOTTOM, new gives TOP
            {  20.0,   45.0 }, // Quadrant 1: old gives LEFT, new gives BACK
            { -20.0,   45.0 }, // Quadrant 1 negative pitch: old gives LEFT, new gives BACK
            {  20.0,  -45.0 }, // Quadrant 4: old gives RIGHT, new gives BACK
            { -20.0,  -45.0 }, // Quadrant 4 negative pitch: old gives RIGHT, new gives BACK
            {  60.0,    0.0 }, // Pure positive pitch -> BOTTOM
            { -60.0,    0.0 }, // Pure negative pitch -> TOP
            {  10.0,  180.0 }, // Pure 180 yaw -> FRONT
            {  10.0,    0.0 }, // Pure 0 yaw -> BACK
            {  10.0,  -90.0 }, // Pure -90 yaw -> RIGHT
            {  10.0,   90.0 }, // Pure +90 yaw -> LEFT
            {  20.0,  135.0 }, // Front-Left quadrant -> LEFT
            {  20.0, -135.0 }, // Front-Right quadrant -> RIGHT
            { -89.9,    0.0 }, // Top Snap View
            {  89.9,    0.0 }  // Bottom Snap View
        };

        System.out.println("\n2. Multi-Quadrant Non-Degenerate Angle Matrix:");
        for (double[] tc : matrix) {
            total++;
            double p = tc[0], y = tc[1];
            AlignmentResult r = evaluate(p, y);
            boolean ok = r.newCubeFace().equals(r.mainFace());
            if (ok) {
                passed++;
                System.out.println(String.format("   [PASS] (pitch=%6.1f°, yaw=%6.1f°) -> %-6s (mainDot=%.3f, cubeDot=%.3f)",
                    p, y, r.mainFace(), r.mainDot(), r.newDot()));
            } else {
                System.err.println(String.format("   [FAIL] (pitch=%6.1f°, yaw=%6.1f°) -> Expected %s, got %s",
                    p, y, r.mainFace(), r.newCubeFace()));
            }
        }

        System.out.println(String.format("\nResult: %d/%d ViewCube alignment tests passed.", passed, total));
        if (passed != total) {
            System.exit(1);
        }
    }
}
