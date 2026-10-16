package scratch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TestViewCubeAlignment.java
 *
 * Mathematical and JavaFX transform verification of ViewCube rotation composition.
 *
 * Reference Models:
 * 1. Optical Orientation Model (Primary Reference):
 *    The ViewCube is an orientation indicator bound to rx and ry (it does not translate).
 *    Its reference viewing vector is the camera's optical line-of-sight:
 *       V_optical = Ry(yaw) * Rx(pitch) * (0, 0, -1)^T
 * 2. Translated Camera Position Model (Diagnostic Reference):
 *    The vector from world origin to the camera's translated world position:
 *       V_pos = normalize(Ry(yaw) * Rx(pitch) * (0, -30, -400)^T)
 *
 * Verifies that cubePivot.getTransforms().setAll(cubeRx, cubeRy) faithfully implements
 * (Ry * Rx)^(-1) = Rx(-pitch) * Ry(-yaw) and correctly aligns with both models.
 */
public class TestViewCubeAlignment {

    public static final Map<String, Point3D> FACE_NORMALS = new LinkedHashMap<>();
    static {
        FACE_NORMALS.put("FRONT",  new Point3D( 0,  0,  1)); // Z = +1
        FACE_NORMALS.put("BACK",   new Point3D( 0,  0, -1)); // Z = -1
        FACE_NORMALS.put("TOP",    new Point3D( 0, -1,  0)); // Y = -1 (sky)
        FACE_NORMALS.put("BOTTOM", new Point3D( 0,  1,  0)); // Y = +1 (ground)
        FACE_NORMALS.put("RIGHT",  new Point3D( 1,  0,  0)); // X = +1
        FACE_NORMALS.put("LEFT",   new Point3D(-1,  0,  0)); // X = -1
    }

    public record FaceMatch(String face, double dot) {}

    public static FaceMatch findDominantFace(Point3D viewVec) {
        String bestFace = null;
        double maxDot = -Double.MAX_VALUE;
        for (var entry : FACE_NORMALS.entrySet()) {
            double dot = entry.getValue().dotProduct(viewVec);
            if (dot > maxDot) {
                maxDot = dot;
                bestFace = entry.getKey();
            }
        }
        return new FaceMatch(bestFace, maxDot);
    }

    public static FaceMatch findViewCubeFace(Transform trans) {
        Point3D vcCamDir = new Point3D(0, 0, -1);
        String bestFace = null;
        double maxDot = -Double.MAX_VALUE;
        for (var entry : FACE_NORMALS.entrySet()) {
            Point3D transformedNormal = trans.deltaTransform(entry.getValue());
            double dot = transformedNormal.dotProduct(vcCamDir);
            if (dot > maxDot) {
                maxDot = dot;
                bestFace = entry.getKey();
            }
        }
        return new FaceMatch(bestFace, maxDot);
    }

    public static void main(String[] args) {
        System.out.println("=== ViewCube Rotation Composition & Optical Alignment Test ===");

        int passed = 0;
        int total = 0;

        // -------------------------------------------------------------
        // 1. Acceptance Criterion: The Reported Counterexample (50°, 170°)
        // -------------------------------------------------------------
        total++;
        double p0 = 50.0, y0 = 170.0;
        Rotate mainRx0 = new Rotate(p0, Rotate.X_AXIS);
        Rotate mainRy0 = new Rotate(y0, Rotate.Y_AXIS);

        Point3D optVec0 = mainRy0.transform(mainRx0.transform(new Point3D(0, 0, -1)));
        Point3D posVec0 = mainRy0.transform(mainRx0.transform(new Point3D(0, -30, -400))).normalize();

        FaceMatch optMatch0 = findDominantFace(optVec0);
        FaceMatch posMatch0 = findDominantFace(posVec0);

        // Old composition: addAll(cubeRy, cubeRx)
        Group oldPivot = new Group();
        oldPivot.getTransforms().addAll(new Rotate(-y0, Rotate.Y_AXIS), new Rotate(-p0, Rotate.X_AXIS));
        FaceMatch oldCubeMatch = findViewCubeFace(oldPivot.getLocalToParentTransform());

        // New composition: setAll(cubeRx, cubeRy)
        Group newPivot = new Group();
        newPivot.getTransforms().setAll(new Rotate(-p0, Rotate.X_AXIS), new Rotate(-y0, Rotate.Y_AXIS));
        FaceMatch newCubeMatch = findViewCubeFace(newPivot.getLocalToParentTransform());

        System.out.println(String.format("1. Counterexample Check (pitch=%.1f°, yaw=%.1f°):", p0, y0));
        System.out.println(String.format("   Optical Reference Face:   %s (dot=%.4f)", optMatch0.face(), optMatch0.dot()));
        System.out.println(String.format("   CamPosition Reference:    %s (dot=%.4f)", posMatch0.face(), posMatch0.dot()));
        System.out.println(String.format("   Old Composition (Ry, Rx): %s (dot=%.4f)", oldCubeMatch.face(), oldCubeMatch.dot()));
        System.out.println(String.format("   New Composition (Rx, Ry): %s (dot=%.4f)", newCubeMatch.face(), newCubeMatch.dot()));

        boolean cePass = "BOTTOM".equals(optMatch0.face())
                      && "BOTTOM".equals(posMatch0.face())
                      && "TOP".equals(oldCubeMatch.face())
                      && "BOTTOM".equals(newCubeMatch.face());

        if (cePass) {
            passed++;
            System.out.println("   [PASS] Proven: Old composition fails with TOP, New composition correctly resolves to BOTTOM!");
        } else {
            System.err.println("   [FAIL] Counterexample acceptance criterion not met!");
        }

        // -------------------------------------------------------------
        // 2. Canonical Snap Views (6 Primary Faces)
        // -------------------------------------------------------------
        double[][] canonicalCases = {
            {   0.0,   0.0 }, // BACK
            {   0.0, 180.0 }, // FRONT
            { -89.9,   0.0 }, // TOP
            {  89.9,   0.0 }, // BOTTOM
            {   0.0,  90.0 }, // LEFT
            {   0.0, -90.0 }  // RIGHT
        };

        System.out.println("\n2. Canonical Snap Views:");
        for (double[] tc : canonicalCases) {
            total++;
            double p = tc[0], y = tc[1];
            Rotate rx = new Rotate(p, Rotate.X_AXIS);
            Rotate ry = new Rotate(y, Rotate.Y_AXIS);
            Point3D optVec = ry.transform(rx.transform(new Point3D(0, 0, -1)));
            FaceMatch exp = findDominantFace(optVec);

            Group pivot = new Group();
            pivot.getTransforms().setAll(new Rotate(-p, Rotate.X_AXIS), new Rotate(-y, Rotate.Y_AXIS));
            FaceMatch act = findViewCubeFace(pivot.getLocalToParentTransform());

            if (exp.face().equals(act.face())) {
                passed++;
                System.out.println(String.format("   [PASS] (pitch=%5.1f°, yaw=%6.1f°) -> %-6s (dot=%.3f)", p, y, act.face(), act.dot()));
            } else {
                System.err.println(String.format("   [FAIL] (pitch=%5.1f°, yaw=%6.1f°) -> Expected %s, got %s", p, y, exp.face(), act.face()));
            }
        }

        // -------------------------------------------------------------
        // 3. Non-Degenerate Multi-Quadrant Matrix (All 4 Quadrants & Pitch Variations)
        // -------------------------------------------------------------
        double[][] generalCases = {
            {  30.0,   30.0 }, // Q1 Back-dominant
            {  30.0,   60.0 }, // Q1 Left-dominant
            {  30.0,  120.0 }, // Q2 Left-dominant
            {  30.0,  150.0 }, // Q2 Front-dominant
            {  30.0, -150.0 }, // Q3 Front-dominant
            {  30.0, -120.0 }, // Q3 Right-dominant
            {  30.0,  -60.0 }, // Q4 Right-dominant
            {  30.0,  -30.0 }, // Q4 Back-dominant
            { -30.0,   30.0 }, // Q1 Negative pitch
            { -30.0,  150.0 }, // Q2 Negative pitch
            { -30.0, -150.0 }, // Q3 Negative pitch
            { -30.0,  -30.0 }, // Q4 Negative pitch
            {  70.0,  150.0 }, // Steep positive pitch -> Bottom-dominant
            { -70.0,  150.0 }  // Steep negative pitch -> Top-dominant
        };

        System.out.println("\n3. Non-Degenerate Multi-Quadrant Matrix:");
        for (double[] tc : generalCases) {
            total++;
            double p = tc[0], y = tc[1];
            Rotate rx = new Rotate(p, Rotate.X_AXIS);
            Rotate ry = new Rotate(y, Rotate.Y_AXIS);
            Point3D optVec = ry.transform(rx.transform(new Point3D(0, 0, -1)));
            FaceMatch exp = findDominantFace(optVec);

            Group pivot = new Group();
            pivot.getTransforms().setAll(new Rotate(-p, Rotate.X_AXIS), new Rotate(-y, Rotate.Y_AXIS));
            FaceMatch act = findViewCubeFace(pivot.getLocalToParentTransform());

            if (exp.face().equals(act.face())) {
                passed++;
                System.out.println(String.format("   [PASS] (pitch=%5.1f°, yaw=%6.1f°) -> %-6s (optDot=%.3f, cubeDot=%.3f)",
                    p, y, act.face(), exp.dot(), act.dot()));
            } else {
                System.err.println(String.format("   [FAIL] (pitch=%5.1f°, yaw=%6.1f°) -> Expected %s, got %s",
                    p, y, exp.face(), act.face()));
            }
        }

        // -------------------------------------------------------------
        // 4. Diagonal Corner Verification (Yaw = ±45°, ±135°)
        // -------------------------------------------------------------
        // At 45° azimuth, two vertical faces are equidistant (|x| == |z|).
        // The ViewCube must identify one of the two sharing the edge within epsilon.
        double[][] diagonalCases = {
            { 20.0,   45.0, 1.0, 0.0 }, // BACK and RIGHT tied
            { 20.0,  135.0, 0.0, 1.0 }, // FRONT and LEFT tied
            { 20.0, -135.0, 0.0, 0.0 }, // FRONT and RIGHT tied
            { 20.0,  -45.0, 1.0, 1.0 }  // BACK and LEFT tied
        };

        System.out.println("\n4. Diagonal Boundary Verification (Equidistant Face Ties):");
        for (double[] tc : diagonalCases) {
            total++;
            double p = tc[0], y = tc[1];
            Group pivot = new Group();
            pivot.getTransforms().setAll(new Rotate(-p, Rotate.X_AXIS), new Rotate(-y, Rotate.Y_AXIS));
            FaceMatch act = findViewCubeFace(pivot.getLocalToParentTransform());

            Rotate rx = new Rotate(p, Rotate.X_AXIS);
            Rotate ry = new Rotate(y, Rotate.Y_AXIS);
            Point3D optVec = ry.transform(rx.transform(new Point3D(0, 0, -1)));

            // Compute top two face dots
            double dFront = FACE_NORMALS.get("FRONT").dotProduct(optVec);
            double dBack  = FACE_NORMALS.get("BACK").dotProduct(optVec);
            double dLeft  = FACE_NORMALS.get("LEFT").dotProduct(optVec);
            double dRight = FACE_NORMALS.get("RIGHT").dotProduct(optVec);

            double maxOpt = Math.max(Math.max(dFront, dBack), Math.max(dLeft, dRight));
            double actDot = FACE_NORMALS.get(act.face()).dotProduct(optVec);

            boolean isCoDominant = Math.abs(actDot - maxOpt) < 1e-6;
            if (isCoDominant) {
                passed++;
                System.out.println(String.format("   [PASS] (pitch=%5.1f°, yaw=%6.1f°) -> Co-dominant face %-6s (dot=%.4f, tie-gap=%.1e)",
                    p, y, act.face(), actDot, Math.abs(actDot - maxOpt)));
            } else {
                System.err.println(String.format("   [FAIL] (pitch=%5.1f°, yaw=%6.1f°) -> Face %s not co-dominant (gap=%.4f)",
                    p, y, act.face(), Math.abs(actDot - maxOpt)));
            }
        }

        System.out.println(String.format("\nResult: %d/%d ViewCube alignment tests passed.", passed, total));
        if (passed != total) {
            System.exit(1);
        }
    }
}
