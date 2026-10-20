package backend.native_bridge;

import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class astra_native_bridge_ui_main {
    private static final astra_native_bridge_ui_main INSTANCE = new astra_native_bridge_ui_main();
    private boolean available = false;
    private String loadError = null;
    private MethodHandle mhGetVer, mhGetVerStr, mhMag, mhRayTri, mhRayMeshNearest, mhXform, mhLastErr;

    private astra_native_bridge_ui_main() { init(); }

    public static astra_native_bridge_ui_main getInstance() { return INSTANCE; }
    public boolean isAvailable() { return available; }
    public String getLoadError() { return loadError; }

    private void init() {
        try {
            Path dllPath = resolveLibraryPath();
            if (dllPath == null || !Files.exists(dllPath)) {
                loadError = "Native dynamic library not found";
                return;
            }
            SymbolLookup lookup = SymbolLookup.libraryLookup(dllPath, Arena.ofAuto());
            Linker linker = Linker.nativeLinker();

            mhGetVer = linker.downcallHandle(lookup.find("astra_get_version").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT));
            mhGetVerStr = linker.downcallHandle(lookup.find("astra_get_version_string").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS));
            mhMag = linker.downcallHandle(lookup.find("astra_vector_magnitude").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS));
            mhRayTri = linker.downcallHandle(lookup.find("astra_ray_triangle_intersect").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            lookup.find("astra_ray_mesh_intersect_nearest_f32").ifPresent(sym -> {
                mhRayMeshNearest = linker.downcallHandle(sym,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    Linker.Option.critical(true));
            });
            mhXform = linker.downcallHandle(lookup.find("astra_transform_point_4x4").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhLastErr = linker.downcallHandle(lookup.find("astra_get_last_error").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            available = true;
        } catch (Throwable t) {
            loadError = t.getMessage();
            available = false;
        }
    }

    private Path resolveLibraryPath() {
        String prop = System.getProperty("astra.native.path");
        if (prop != null && Files.exists(Paths.get(prop))) return Paths.get(prop);
        Path[] candidates = new Path[]{
            Paths.get("native/build/bin/astra_native.dll"),
            Paths.get("target/classes/native/astra_native.dll"),
            Paths.get("../native/build/bin/astra_native.dll")
        };
        for (Path c : candidates) {
            Path abs = c.toAbsolutePath();
            if (Files.exists(abs)) return abs;
        }
        try (InputStream is = getClass().getResourceAsStream("/native/astra_native.dll")) {
            if (is != null) {
                Path temp = Files.createTempFile("astra_native_", ".dll");
                temp.toFile().deleteOnExit();
                Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
                return temp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public int getVersion() {
        ensureLoaded();
        try { return (int) mhGetVer.invokeExact(); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public String getVersionString() {
        ensureLoaded();
        try {
            MemorySegment addr = (MemorySegment) mhGetVerStr.invokeExact();
            return addr.reinterpret(256).getString(0);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public double vectorMagnitude(double x, double y, double z) {
        ensureLoaded();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(ValueLayout.JAVA_DOUBLE);
            int rc = (int) mhMag.invokeExact(x, y, z, out);
            if (rc != 0) throw new RuntimeException("astra_vector_magnitude failed: " + getLastError());
            return out.get(ValueLayout.JAVA_DOUBLE, 0);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public double rayTriangleIntersect(double[] orig, double[] dir, double[] v0, double[] v1, double[] v2, double[] outBary) {
        ensureLoaded();
        if (orig == null || dir == null || v0 == null || v1 == null || v2 == null) throw new IllegalArgumentException("Array args cannot be null");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mOrig = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, orig);
            MemorySegment mDir = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, dir);
            MemorySegment mV0 = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, v0);
            MemorySegment mV1 = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, v1);
            MemorySegment mV2 = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, v2);
            MemorySegment mOutT = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment mOutU = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment mOutV = arena.allocate(ValueLayout.JAVA_DOUBLE);

            int rc = (int) mhRayTri.invokeExact(mOrig, mDir, mV0, mV1, mV2, mOutT, mOutU, mOutV);
            if (rc != 0) throw new RuntimeException("astra_ray_triangle_intersect failed: " + getLastError());
            if (outBary != null && outBary.length >= 2) {
                outBary[0] = mOutU.get(ValueLayout.JAVA_DOUBLE, 0);
                outBary[1] = mOutV.get(ValueLayout.JAVA_DOUBLE, 0);
            }
            return mOutT.get(ValueLayout.JAVA_DOUBLE, 0);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public double[] transformPoint4x4(double[] pt, double[] mat) {
        ensureLoaded();
        if (pt == null || mat == null || pt.length < 3 || mat.length < 16) throw new IllegalArgumentException("Invalid input dimensions");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mPt = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, pt);
            MemorySegment mMat = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, mat);
            MemorySegment mOut = arena.allocate(ValueLayout.JAVA_DOUBLE, 3);
            int rc = (int) mhXform.invokeExact(mPt, mMat, mOut);
            if (rc != 0) throw new RuntimeException("astra_transform_point_4x4 failed: " + getLastError());
            return new double[]{ mOut.getAtIndex(ValueLayout.JAVA_DOUBLE, 0), mOut.getAtIndex(ValueLayout.JAVA_DOUBLE, 1), mOut.getAtIndex(ValueLayout.JAVA_DOUBLE, 2) };
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public int rayMeshIntersectNearestF32(double[] orig, double[] dir, float[] points, int[] faces, int stride, double[] outHitInfo) {
        ensureLoaded();
        if (orig == null || dir == null || points == null || faces == null || mhRayMeshNearest == null) {
            throw new IllegalArgumentException("Invalid null arguments for rayMeshIntersectNearestF32");
        }
        if (orig.length < 3 || dir.length < 3) throw new IllegalArgumentException("Ray must have at least 3 elements");
        if (!Double.isFinite(orig[0]) || !Double.isFinite(orig[1]) || !Double.isFinite(orig[2]) ||
            !Double.isFinite(dir[0]) || !Double.isFinite(dir[1]) || !Double.isFinite(dir[2])) {
            throw new IllegalArgumentException("Ray elements must be finite");
        }
        if (stride != 3 && stride != 6) throw new IllegalArgumentException("Unsupported stride: " + stride);
        if (points.length % 3 != 0) throw new IllegalArgumentException("Point coordinates must be multiples of 3");
        if (points.length == 0 || faces.length < stride) {
            if (outHitInfo != null && outHitInfo.length > 0) outHitInfo[0] = -1.0;
            return -1;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mOrig = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, orig);
            MemorySegment mDir = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, dir);
            MemorySegment mPts = MemorySegment.ofArray(points);
            MemorySegment mFaces = MemorySegment.ofArray(faces);
            MemorySegment mOutIdx = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment mOutT = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment mOutU = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment mOutV = arena.allocate(ValueLayout.JAVA_DOUBLE);

            int rc = (int) mhRayMeshNearest.invokeExact(mOrig, mDir, mPts, points.length, mFaces, faces.length, stride, mOutIdx, mOutT, mOutU, mOutV);
            if (rc != 0) throw new RuntimeException("astra_ray_mesh_intersect_nearest_f32 failed: " + getLastError());
            int hitIdx = mOutIdx.get(ValueLayout.JAVA_INT, 0);
            if (outHitInfo != null && outHitInfo.length > 0) {
                outHitInfo[0] = mOutT.get(ValueLayout.JAVA_DOUBLE, 0);
                if (outHitInfo.length > 1) outHitInfo[1] = mOutU.get(ValueLayout.JAVA_DOUBLE, 0);
                if (outHitInfo.length > 2) outHitInfo[2] = mOutV.get(ValueLayout.JAVA_DOUBLE, 0);
            }
            return hitIdx;
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public String getLastError() {
        if (!available || mhLastErr == null) return "Bridge not loaded";
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(256);
            mhLastErr.invokeExact(buf, 256);
            return buf.getString(0);
        } catch (Throwable t) { return "Error querying message"; }
    }

    private void ensureLoaded() {
        if (!available) throw new IllegalStateException("Native backend not available: " + loadError);
    }
}
