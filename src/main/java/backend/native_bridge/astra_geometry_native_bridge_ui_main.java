package backend.native_bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;

public final class astra_geometry_native_bridge_ui_main {
    private static final astra_geometry_native_bridge_ui_main INSTANCE = new astra_geometry_native_bridge_ui_main();
    private boolean available = false;
    private String loadError = null;
    private MethodHandle mhAbiVer, mhLastErr, mhModelCreate, mhModelRelease, mhEvaluateFeature, mhResultRelease, mhResultRevision, mhResultTessellation;

    private astra_geometry_native_bridge_ui_main() { init(); }

    public static astra_geometry_native_bridge_ui_main getInstance() { return INSTANCE; }
    public boolean isAvailable() { return available; }
    public String getLoadError() { return loadError; }

    private void init() {
        try {
            Path dllPath = astra_native_bridge_ui_main.getInstance().resolveLibraryPath();
            if (dllPath == null || !Files.exists(dllPath)) {
                loadError = "Native dynamic library not found";
                return;
            }
            SymbolLookup lookup = SymbolLookup.libraryLookup(dllPath, Arena.ofAuto());
            Linker linker = Linker.nativeLinker();

            mhAbiVer = linker.downcallHandle(lookup.find("astra_geometry_abi_version").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT));
            mhLastErr = linker.downcallHandle(lookup.find("astra_geometry_last_error").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            mhModelCreate = linker.downcallHandle(lookup.find("astra_geom_model_create").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            mhModelRelease = linker.downcallHandle(lookup.find("astra_geom_model_release").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            mhEvaluateFeature = linker.downcallHandle(lookup.find("astra_geom_evaluate_feature").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhResultRelease = linker.downcallHandle(lookup.find("astra_geom_result_release").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            mhResultRevision = linker.downcallHandle(lookup.find("astra_geom_result_revision").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhResultTessellation = linker.downcallHandle(lookup.find("astra_geom_result_tessellation").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            available = true;
        } catch (Throwable t) {
            loadError = t.getMessage();
            available = false;
        }
    }

    private void ensureAvailable() {
        if (!available) throw new UnsupportedOperationException("Geometry kernel unavailable: " + loadError);
    }

    public int getAbiVersion() {
        ensureAvailable();
        try { return (int) mhAbiVer.invokeExact(); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public String getLastError() {
        if (!available) return loadError;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(512);
            MemorySegment req = arena.allocate(ValueLayout.JAVA_INT);
            int rc = (int) mhLastErr.invokeExact(buf, 512, req);
            return rc == 0 ? buf.getString(0) : "Failed to retrieve last error (code " + rc + ")";
        } catch (Throwable t) { return "Error invoking astra_geometry_last_error: " + t.getMessage(); }
    }

    public GeometryModel createModel() {
        ensureAvailable();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.ADDRESS);
            int rc = (int) mhModelCreate.invokeExact(ptr);
            if (rc != 0) throw new RuntimeException("astra_geom_model_create failed (code " + rc + "): " + getLastError());
            return new GeometryModel(ptr.get(ValueLayout.ADDRESS, 0));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public GeometryResult evaluateFeature(GeometryModel model, MemorySegment request) {
        ensureAvailable();
        if (model == null || !model.isValid()) throw new IllegalArgumentException("Invalid geometry model handle");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outRes = arena.allocate(ValueLayout.ADDRESS);
            int rc = (int) mhEvaluateFeature.invokeExact(model.handle, request, outRes);
            if (rc != 0) throw new UnsupportedOperationException("evaluateFeature failed (code " + rc + "): " + getLastError());
            return new GeometryResult(outRes.get(ValueLayout.ADDRESS, 0), 1L);
        } catch (UnsupportedOperationException e) { throw e; }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static final class GeometryModel implements AutoCloseable {
        private MemorySegment handle;
        private boolean closed = false;
        public GeometryModel(MemorySegment h) { this.handle = h; }
        public boolean isValid() { return !closed && handle != null && !handle.equals(MemorySegment.NULL); }
        public MemorySegment getHandle() { return handle; }
        @Override public synchronized void close() {
            if (!closed && isValid()) {
                closed = true;
                try { INSTANCE.mhModelRelease.invokeExact(handle); } catch (Throwable ignored) {}
                handle = MemorySegment.NULL;
            }
        }
    }

    public static final class GeometryResult implements AutoCloseable {
        private MemorySegment handle;
        private final long revision;
        private boolean closed = false;
        public GeometryResult(MemorySegment h, long rev) { this.handle = h; this.revision = rev; }
        public long getRevision() { return revision; }
        public boolean isValid() { return !closed && handle != null && !handle.equals(MemorySegment.NULL); }
        @Override public synchronized void close() {
            if (!closed && isValid()) {
                closed = true;
                try { INSTANCE.mhResultRelease.invokeExact(handle); } catch (Throwable ignored) {}
                handle = MemorySegment.NULL;
            }
        }
    }
}
