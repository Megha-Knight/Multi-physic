import backend.native_bridge.astra_native_bridge_ui_main;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * End-to-end Integration Test for Astra Native Backend via Java 25 FFM
 */
public class TestJavaNativeIntegration {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("ASTRA NATIVE BACKEND: JAVA 25 END-TO-END INTEGRATION TEST");
        System.out.println("================================================================================");

        astra_native_bridge_ui_main bridge = astra_native_bridge_ui_main.getInstance();

        // 1. Availability check
        System.out.println("\n[TEST 1: Dynamic Library Load & Availability]");
        System.out.println("  Bridge isAvailable: " + bridge.isAvailable());
        if (!bridge.isAvailable()) {
            System.err.println("  FAILURE: Load error = " + bridge.getLoadError());
            System.exit(1);
        }
        assert bridge.isAvailable() : "Bridge must be available";
        System.out.println("  -> PASS: astra_native.dll loaded successfully into Java process.");

        // 2. Version Queries
        System.out.println("\n[TEST 2: Native Version Handshake]");
        int ver = bridge.getVersion();
        String verStr = bridge.getVersionString();
        System.out.printf("  Native Packed Version: 0x%08X\n", ver);
        System.out.printf("  Native Version String: \"%s\"\n", verStr);
        assert ver == 0x000100 : "Version mismatch, expected 0x000100";
        assert verStr.contains("Astra Native Backend") : "Version string invalid";
        System.out.println("  -> PASS: Version handshake verified.");

        // 3. Vector Magnitude (Euclidean Math)
        System.out.println("\n[TEST 3: Vector Magnitude Calculation]");
        double mag1 = bridge.vectorMagnitude(3.0, 4.0, 12.0);
        System.out.printf("  vectorMagnitude(3.0, 4.0, 12.0) = %.6f (expected: 13.000000)\n", mag1);
        assert Math.abs(mag1 - 13.0) < 1e-9 : "Magnitude mismatch";

        double magZero = bridge.vectorMagnitude(0.0, 0.0, 0.0);
        assert Math.abs(magZero) < 1e-9 : "Zero magnitude mismatch";
        System.out.println("  -> PASS: Numerical vector math verified.");

        // 4. Ray-Triangle Intersection (Computational Geometry)
        System.out.println("\n[TEST 4: 3D Ray-Triangle Intersection (Möller–Trumbore)]");
        // Triangle at Z=0 in XY plane: (0,0,0) -> (10,0,0) -> (0,10,0)
        double[] v0 = {0.0, 0.0, 0.0};
        double[] v1 = {10.0, 0.0, 0.0};
        double[] v2 = {0.0, 10.0, 0.0};

        // Case A: Direct Hit
        double[] rayOrig = {2.0, 2.0, 5.0};
        double[] rayDir  = {0.0, 0.0, -1.0};
        double[] bary = new double[2];
        double tHit = bridge.rayTriangleIntersect(rayOrig, rayDir, v0, v1, v2, bary);
        System.out.printf("  Ray Hit:  t = %.6f (expected 5.0), u = %.4f, v = %.4f\n", tHit, bary[0], bary[1]);
        assert Math.abs(tHit - 5.0) < 1e-7 : "Ray hit distance mismatch";
        assert Math.abs(bary[0] - 0.2) < 1e-7 : "Barycentric u mismatch";
        assert Math.abs(bary[1] - 0.2) < 1e-7 : "Barycentric v mismatch";

        // Case B: Direct Miss (outside triangle boundary)
        double[] rayMissOrig = {20.0, 20.0, 5.0};
        double tMiss = bridge.rayTriangleIntersect(rayMissOrig, rayDir, v0, v1, v2, null);
        System.out.printf("  Ray Miss: t = %.6f (expected < 0.0)\n", tMiss);
        assert tMiss < 0.0 : "Miss ray should return negative t";

        // Case C: Parallel Ray
        double[] rayParDir = {1.0, 0.0, 0.0};
        double tPar = bridge.rayTriangleIntersect(rayOrig, rayParDir, v0, v1, v2, null);
        assert tPar < 0.0 : "Parallel ray should return negative t";
        System.out.println("  -> PASS: Computational geometry raycasting verified.");

        // 5. Affine 4x4 Point Transformation
        System.out.println("\n[TEST 5: 4x4 Affine Point Transformation]");
        double[] pt = {1.0, 2.0, 3.0};
        double[] mat = {
            1.0, 0.0, 0.0, 10.0,
            0.0, 1.0, 0.0, 20.0,
            0.0, 0.0, 1.0, 30.0,
            0.0, 0.0, 0.0,  1.0
        };
        double[] xformed = bridge.transformPoint4x4(pt, mat);
        System.out.printf("  Transform (1, 2, 3) + Translate(10, 20, 30) = (%.1f, %.1f, %.1f)\n",
            xformed[0], xformed[1], xformed[2]);
        assert Math.abs(xformed[0] - 11.0) < 1e-9;
        assert Math.abs(xformed[1] - 22.0) < 1e-9;
        assert Math.abs(xformed[2] - 33.0) < 1e-9;
        System.out.println("  -> PASS: Affine transform verified.");

        // 6. Safe Error Handling
        System.out.println("\n[TEST 6: Invalid Input & Native Error Handling]");
        try {
            bridge.rayTriangleIntersect(null, rayDir, v0, v1, v2, null);
            assert false : "Null input should throw exception";
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught expected Java exception for null pointer: " + e.getMessage());
        }

        try {
            bridge.transformPoint4x4(new double[]{1.0}, mat);
            assert false : "Mismatched dimensions should throw exception";
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught expected dimension validation exception: " + e.getMessage());
        }
        System.out.println("  -> PASS: Error boundaries prevent JVM crashes.");

        // 7. Multi-Threaded Stress Test
        System.out.println("\n[TEST 7: Multi-Threaded Concurrent Execution]");
        int numThreads = 8;
        int itersPerThread = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final double shift = t * 1.5;
            exec.submit(() -> {
                try {
                    for (int i = 0; i < itersPerThread; i++) {
                        double d = bridge.vectorMagnitude(3.0 + shift, 4.0, 0.0);
                        double exp = Math.hypot(3.0 + shift, 4.0);
                        if (Math.abs(d - exp) > 1e-7) return;
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        exec.shutdown();
        System.out.printf("  Threads Completed: %d / %d (Total invocations: %d)\n",
            successCount.get(), numThreads, numThreads * itersPerThread);
        assert successCount.get() == numThreads : "Multi-threaded test failed";
        System.out.println("  -> PASS: Thread-safe native execution confirmed.");

        System.out.println("\n================================================================================");
        System.out.println(">>> ALL 7 JAVA-TO-NATIVE INTEGRATION TESTS PASSED (100% SUCCESS) <<<");
        System.out.println("================================================================================");
    }
}
